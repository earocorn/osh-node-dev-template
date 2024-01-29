package com.sample.impl.sensor.simulated;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

import java.util.concurrent.CompletableFuture;
public class Control extends AbstractSensorControl<Sensor> {
    DataRecord commandDataStruct;

    protected Control(Sensor parentSensor) {
        super(parentSensor.getName(), parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException {
        boolean testCmd = false;

        try {

            DataRecord commandData = commandDataStruct.copy();

            commandData.setData(cmdData);

            DataComponent booleanComponent = commandData.getField("SampleBoolean");

            DataBlock booleanComponentData = booleanComponent.getData();

            testCmd = booleanComponentData.getBooleanValue();

            DataComponent textComponent = commandData.getField("SampleText");

            DataBlock textComponentData = textComponent.getData();

            String text = textComponentData.getStringValue();

            parentSensor.output.getLatestRecord().setStringValue(5, text);
            parentSensor.output.getLatestRecord().setBooleanValue(6, testCmd);

            getLogger().info("TEXT DATA FROM COMMAND: " + text);
            getLogger().info(commandData.getData().getStringValue());

        } catch (Exception e) {
            getLogger().error("failed to send command: " + e.getMessage());
        }
        return testCmd;
    }

    protected void init() {

        SWEHelper sweFactory = new SWEHelper();
        commandDataStruct = sweFactory.createRecord()
                .name(getName())
                .label("Simulated Sensor")
                .updatable(true)
                .description("Example command to test osh-js api")
                .addField("SampleBoolean",
                        sweFactory.createBoolean()
                                .name("SampleBoolean")
                                .description("Sample command to return t or f")
                                .value(false)
                                .build())
                .addField("SampleText",
                        sweFactory.createText()
                                .name("SampleText")
                                .description("Sample command to return text")
                                .value("Default")
                                .build())
                .build();

    }
}
