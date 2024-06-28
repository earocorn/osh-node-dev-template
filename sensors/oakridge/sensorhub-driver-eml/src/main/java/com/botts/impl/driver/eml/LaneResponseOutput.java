package com.botts.impl.driver.eml;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

public class LaneResponseOutput extends AbstractSensorOutput<EMLDriver> {

    private static final String SENSOR_OUTPUT_NAME = "ControlResponseOutput";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Response output from EML control channel";

    private static final Logger logger = LoggerFactory.getLogger(LaneResponseOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;
    private DataBlock dataBlock;

    Thread worker;

    private void init() {
        SWEHelper fac = new SWEHelper();

    }

    protected LaneResponseOutput(EMLDriver parentSensor) {
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
