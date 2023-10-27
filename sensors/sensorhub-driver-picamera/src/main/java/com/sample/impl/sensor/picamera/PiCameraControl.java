package com.sample.impl.sensor.picamera;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.command.ICommandReceiver;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

public class PiCameraControl extends AbstractSensorControl<PiCameraSensor> {

    DataRecord commandDataStruct;
    private static final String SENSOR_CONTROL_NAME = "PiCameraControl";

    protected PiCameraControl(PiCameraSensor parentSensor) {
        super(SENSOR_CONTROL_NAME, parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {

        // TODO: Initialize command data record

        SWEHelper sweFactory = new SWEHelper();
        commandDataStruct = sweFactory.createRecord()
                .name(getName())
                
                .build();

        return commandDataStruct;
    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException {
        return super.execCommand(cmdData);
    }
}
