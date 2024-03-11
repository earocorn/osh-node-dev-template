package com.sample.impl.sensor.wii.trials;

import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

public class WiiController {
    private static final String SENSOR_OUTPUT_NAME = "WII_REMOTE_CONTROLLER";
    private static final String SENSOR_OUTPUT_LABEL = "WII_SENSOR";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Driver for Wii remote outputting control inputs";
    private static final Logger logger = LoggerFactory.getLogger(WiiController.class);
    private DataRecord dataStruct;
    private DataEncoding dataEncoding;


    public WiiController(){

    }

    public void doInIt(){
        logger.debug("Initalizing wii controller");

        SWEHelper sweFactory = new SWEHelper();

//        dataStruct = sweFactory.createRecord()
//                .name(SENSOR_OUTPUT_NAME)
//                .label(SENSOR_OUTPUT_LABEL)
//                .description(SENSOR_OUTPUT_DESCRIPTION)
//                .addField("sampleTime", sweFactory.createTime()
//                        .asSamplingTimeIsoUTC()
//                        .label("Sample Time")
//                        .description("Time of data collection"))
//                .addField("Controller", sweFactory.createRecord()
//                        .addField())
//                .build();
    }

}
