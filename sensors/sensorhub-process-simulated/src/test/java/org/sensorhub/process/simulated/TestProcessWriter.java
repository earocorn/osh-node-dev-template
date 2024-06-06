package org.sensorhub.process.simulated;

import net.opengis.sensorml.v20.IOPropertyList;
import org.junit.Test;
import org.sensorhub.process.simulated.helpers.ProcessHelper;
import org.sensorhub.process.simulated.test.SimulatedDB;
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
        outputs.add("numRecords",
                fac.createCount()
                        .id("numRecords")
                        .label("Num Records")
                        .definition(SWEHelper.getPropertyUri("Quantity"))
                        .build());

        processHelper.addOutputList(outputs);

        processHelper.addDataSource("source0", "urn:osh:sensor:simulated");

        processHelper.addProcess("process0", new SimulatedDB());

        processHelper.addConnection("components/source0/outputs/sensorOutput/count"
                ,"components/process0/inputs/count");
        processHelper.addConnection("components/source0/outputs/sensorOutput/alarm"
                ,"components/process0/inputs/alarm");

        processHelper.addConnection("components/process0/outputs/entry/numRecords",
                "outputs/numRecords");

        processHelper.writeXML(System.out);
    }

}
