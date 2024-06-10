package org.sensorhub.process.rapiscan;

import net.opengis.sensorml.v20.IOPropertyList;
import org.junit.Test;
import org.sensorhub.process.rapiscan.helpers.ProcessHelper;
import org.sensorhub.process.rapiscan.test.AlarmRecorder;
import org.vast.swe.SWEHelper;

public class TestProcessWriter {

    public TestProcessWriter() throws Exception {
        testGamepadPtzProcess();
    }

    @Test
    public void testGamepadPtzProcess() throws Exception
    {
        ProcessHelper processHelper = new ProcessHelper();
        SWEHelper fac = new SWEHelper();

        IOPropertyList outputs = new IOPropertyList();
        outputs.add("numGammaRecords",
                fac.createCount()
                        .id("numGammaRecords")
                        .label("Num Records")
                        .definition(SWEHelper.getPropertyUri("Quantity"))
                        .build());
        outputs.add("numNeutronRecords",
                fac.createCount()
                        .id("numNeutronRecords")
                        .label("Num Records")
                        .definition(SWEHelper.getPropertyUri("Quantity"))
                        .build());

        processHelper.addOutputList(outputs);

        processHelper.addDataSource("source0", "urn:osh:sensor:rapiscansensor001");

        AlarmRecorder process = new AlarmRecorder();
        process.getParameterList().getComponent(0).getData().setStringValue("29f2b677-95b1-4499-8e5b-459839ec3eb6");

        processHelper.addProcess("process0", process);

        processHelper.addConnection("components/source0/outputs/Occupancy/"
                ,"components/process0/inputs/occupancy");

        processHelper.addConnection("components/process0/outputs/neutronEntry/numNeutronRecords",
                "outputs/numNeutronRecords");
        processHelper.addConnection("components/process0/outputs/gammaEntry/numGammaRecords",
                "outputs/numGammaRecords");

        processHelper.writeXML(System.out);
    }

}
