package org.sensorhub.process.gamepadptz;

import net.opengis.gml.v32.Reference;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.FeatureList;
import net.opengis.sensorml.v20.Link;
import net.opengis.sensorml.v20.SimpleProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.junit.Assert;
import org.junit.Test;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleClassFinder;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.processing.ProcessingManagerImpl;
import org.sensorhub.impl.processing.SMLProcessConfig;
import org.sensorhub.impl.processing.SMLProcessImpl;
import org.vast.data.DataStreamImpl;
import org.vast.sensorML.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class TestGamepadPtzProcess {

    GamepadPtz ptzProcess = new GamepadPtz();
    static ModuleRegistry registry;

    protected static void runProcess(IProcessModule<?> process) throws Exception
    {
        AtomicInteger counter = new AtomicInteger();

        for (IStreamingDataInterface output: process.getOutputs().values())
            output.registerListener(e -> {
                //System.out.println(e.getTimeStamp() + ": " + ((DataEvent)e).getRecords()[0].getAtomCount());
                counter.incrementAndGet();
            });

        process.start();

        long t0 = System.currentTimeMillis();
        while (counter.get() < 85-12+1)
        {
            if (System.currentTimeMillis() - t0 >= 100000L)
                Assert.fail("No data received before timeout");
            Thread.sleep(100);
        }

        System.out.println();
    }
    protected static IProcessModule<?> createSMLProcess(String smlUrl) throws Exception
    {
        SMLProcessConfig processCfg = new SMLProcessConfig();
        processCfg.autoStart = false;
        processCfg.name = "SensorML Process #1";
        processCfg.moduleClass = SMLProcessImpl.class.getCanonicalName();
        processCfg.sensorML = smlUrl;

        @SuppressWarnings("unchecked")
        IProcessModule<SMLProcessConfig> process = (IProcessModule<SMLProcessConfig>)registry.loadModule(processCfg);
        process.init();
        return process;
    }

    @Test
    public void testGamepadPtzProcess() throws Exception
    {
        GamepadPtz p = new GamepadPtz();
        p.init();

        SMLUtils smlHelper = new SMLUtils(SMLUtils.V2_0);

        SimpleProcessImpl simple = new SimpleProcessImpl();
        simple.setExecutableImpl(p);

        // serialize
        AggregateProcessImpl wp = new AggregateProcessImpl();
        smlHelper.makeProcessExecutable(wp, false);
        // set type
        wp.setTypeOf(simple.getTypeOf());
        wp.setUniqueIdentifier(UUID.randomUUID().toString());

        // components
        SimpleProcess gamepadSource = new SimpleProcessImpl();
        // gamepad source output uuid
        gamepadSource.setUniqueIdentifier("urn:osh:sensor:gamepad001");

        wp.addComponent("gamepadsource", gamepadSource);

        SimpleProcess ptzDestination = new SimpleProcessImpl();
        ptzDestination.setUniqueIdentifier("urn:axis:cam:00408CA0FF1C");

        wp.addComponent("axiscam", ptzDestination);

        // inputs and outputs
        wp.addOutput("ptz", p.getOutputList().getComponent(0));
        wp.addInput("pov", p.getInputList().getComponent(0));

        // connections
        LinkImpl link = new LinkImpl();

        link.setSource("components/gamepadsource/outputs/gamepadData/pov");
        link.setDestination("components/axiscam/outputs/ptz");
        wp.addConnection(link);

        smlHelper.writeProcess(System.out, wp, true);

        p.execute();
    }

}
