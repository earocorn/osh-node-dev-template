package org.sensorhub.process.datasave.processes;

import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.impl.processing.OnDemandProcess;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEHelper;

public class TestDatasaveProcess extends OnDemandProcess {

    public TestDatasaveProcess()
    {
        super("TestSyncOnDemand", null);
    }


    public AbstractProcess initProcess()
    {
        var swe = new SWEHelper();

        // create input
        this.input = swe.createRecord()
                .name("input")
                .addField("x", swe.createQuantity().label("Input variable"))
                .build();

        // create output
        this.output = swe.createRecord()
                .name("output")
                .addField("y", swe.createQuantity().label("Output variable"))
                .build();

        // create params
        this.params = swe.createRecord()
                .name("params")
                .addField("a", swe.createQuantity().label("Gradient"))
                .addField("b", swe.createQuantity().label("Intercept"))
                .build();

        // create process description
        return new SMLHelper().createSimpleProcess()
                .name("TestSyncOnDemand")
                .uniqueID("urn:osh:process:test:lineareq")
                .build();
    }


    @Override
    public void execute()
    {
        var x = input.getData().getDoubleValue();
        var a = params.getData().getDoubleValue(0);
        var b = params.getData().getDoubleValue(1);
        output.getData().setDoubleValue(a*x+b);
    }
}
