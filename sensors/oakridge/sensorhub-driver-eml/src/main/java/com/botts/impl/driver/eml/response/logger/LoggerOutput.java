package com.botts.impl.driver.eml.response.logger;

import com.botts.impl.driver.eml.EMLDriver;
import com.botts.impl.driver.eml.SeverityLevel;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

public class LoggerOutput extends AbstractSensorOutput<EMLDriver> {

    private static final String SENSOR_OUTPUT_NAME = "LoggerOutput";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Response output from EML logger";

    private static final Logger logger = LoggerFactory.getLogger(LoggerOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;
    private DataBlock dataBlock;

    Thread worker;

    public void init() {
        SWEHelper fac = new SWEHelper();

        dataStruct = fac.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label("Logger Output")
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("LoggerOutput"))
                .addField("timestamp", fac.createTime()
                        .label("Timestamp")
                        .asSamplingTimeIsoUTC())
                .addField("severityLevel", fac.createCategory()
                        .label("Severity Level")
                        .addAllowedValues(SeverityLevel.class))
                .addField("origin", fac.createText()
                        .label("Origin")
                        .value(""))
                .addField("message", fac.createText()
                        .label("Message")
                        .value(""))
                .build();
    }

    public void onNewMessage(long timestamp, SeverityLevel severityLevel, String origin, String message) {
        if(latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        dataBlock.setLongValue(0, timestamp/1000);
        dataBlock.setStringValue(1, severityLevel.name());
        dataBlock.setStringValue(2, origin);
        dataBlock.setStringValue(3, message);

        eventHandler.publish(new DataEvent(timestamp, LoggerOutput.this, dataBlock));
    }

    protected LoggerOutput(EMLDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }
}
