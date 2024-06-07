package com.botts.impl.sensor.rs350;

import com.botts.impl.sensor.rs350.messages.RS350Message;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class LocationOutput extends OutputBase {

    private static final String SENSOR_OUTPUT_NAME = "RS350 Location";

    private static final Logger logger = LoggerFactory.getLogger(LocationOutput.class);

    public LocationOutput(RS350Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug(SENSOR_OUTPUT_NAME + " output created");
    }

    @Override
    protected void init() {
        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Location")
                .definition(RADHelper.getRadUri("location-output"))
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("Sensor Location", radHelper.createLocationVectorLLA())
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");

    }

//    protected void parseData(RS350Message msg) {
//
//        if (latestRecord == null)
//            dataBlock = dataStruct.createDataBlock();
//        else
//            dataBlock = latestRecord.renew();
//
//        latestRecordTime = System.currentTimeMillis() / 1000;
//
//        dataBlock.setLongValue(0, msg.getRs350ForegroundMeasurement().getStartDateTime() / 1000);
//        dataBlock.setDoubleValue(1, msg.getRs350ForegroundMeasurement().getLat());
//        dataBlock.setDoubleValue(2, msg.getRs350ForegroundMeasurement().getLon());
//        dataBlock.setDoubleValue(3, msg.getRs350ForegroundMeasurement().getAlt());
//
//        eventHandler.publish(new DataEvent(latestRecordTime, LocationOutput.this, dataBlock));
//
//    }

    @Override
    public void onNewMessage(RS350Message message) {

        if (message.getRs350ForegroundMeasurement().getLat() != null) {

            createOrRenewDataBlock();

            latestRecordTime = System.currentTimeMillis() / 1000;

            dataBlock.setLongValue(0, message.getRs350ForegroundMeasurement().getStartDateTime() / 1000);
            dataBlock.setDoubleValue(1, message.getRs350ForegroundMeasurement().getLat());
            dataBlock.setDoubleValue(2, message.getRs350ForegroundMeasurement().getLon());
            dataBlock.setDoubleValue(3, message.getRs350ForegroundMeasurement().getAlt());

            eventHandler.publish(new DataEvent(latestRecordTime, LocationOutput.this, dataBlock));
        }
    }
}
