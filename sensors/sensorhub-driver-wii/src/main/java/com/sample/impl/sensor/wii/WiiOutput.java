/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.wii;

import javafx.util.Pair;
import motej.Mote;
import motej.event.AccelerometerEvent;
import motej.event.AccelerometerListener;
import motej.event.CoreButtonEvent;
import motej.event.CoreButtonListener;
import motej.request.ReportModeRequest;
import net.opengis.sensorml.v20.Event;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;


/**
 * Output specification and provider for {@link WiiDriver}.
 *
 * @author Kalyn Stricklin
 * @since date
 */
public class WiiOutput extends AbstractSensorOutput<WiiDriver> implements Runnable{
    private static final String SENSOR_OUTPUT_NAME = "WII_REMOTE_SENSOR";
    private static final String SENSOR_OUTPUT_LABEL = "WII_SENSOR";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Driver for Wii remote outputting control inputs";
    private static final Logger logger = LoggerFactory.getLogger(WiiOutput.class);
    private DataRecord dataStruct;
    private DataEncoding dataEncoding;
    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();
    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private Thread worker;
    boolean NONE, BUTTON_A, BUTTON_B, BUTTON_HOME, BUTTON_MINUS, BUTTON_ONE, BUTTON_PLUS, BUTTON_TWO, D_PAD_DOWN, D_PAD_LEFT, D_PAD_RIGHT, D_PAD_UP;
    public int motionX = 0;
    public int motionY = 0;
    public int motionZ = 0;
    Mote mote;


//    WiiObserver observer;
//    Event event;


    /**
     * Constructor
     *
     * @param parent Sensor driver providing this output
     */
    WiiOutput(WiiDriver parent) {
        super(SENSOR_OUTPUT_NAME, parent);

        logger.debug("Wii Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() {

        // wii remote observer
//        observer = WiiObserver.getInstance();


        logger.debug("Initializing Wii Output");

        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        // TODO: Create data record description
        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))

                .addField("Tilt_Accelerometer", sweFactory.createRecord()
                        .addField("xyz", sweFactory.createRecord()
                                .addField("X", sweFactory.createQuantity()
                                        .label("X-accel")
                                        .uom("deg")
                                )
                                .addField("Y", sweFactory.createQuantity()
                                        .label("Y-accel")
                                        .uom("deg")
                                )
                                .addField("Z", sweFactory.createQuantity()
                                        .label("Z-accel")
                                        .uom("deg")
                                )
                        )
                )
                .addField("Wii Button Data", sweFactory.createRecord()
                        .addField("Buttons",sweFactory.createRecord()
                            .addField("D_PAD_UP",sweFactory.createBoolean()
                                    .label("D-Pad Up Button")
                                    .value(false))
                            .addField("D_PAD_DOWN", sweFactory.createBoolean()
                                    .value(false)
                                    .label("D-Pad Down button"))
                            .addField("D_PAD_LEFT", sweFactory.createBoolean()
                                    .label("D-Pad left button")
                                    .value(false))
                            .addField("D_PAD_RIGHT", sweFactory.createBoolean()
                                    .label("D-Pad right")
                                    .value(false))
                            .addField("HOME", sweFactory.createBoolean()
                                    .value(false)
                                    .label("Home"))
                            .addField("1", sweFactory.createBoolean()
                                    .value(false)
                                    .label("1"))
                            .addField("2", sweFactory.createBoolean()
                                    .value(false)
                                    .label("2"))
                            .addField("A", sweFactory.createBoolean()
                                    .value(false)
                                    .label("A"))
                            .addField("B", sweFactory.createBoolean()
                                    .value(false)
                                    .label("B"))
                            .addField("PLUS", sweFactory.createBoolean()
                                    .value(false)
                                    .label("Plus"))
                            .addField("MINUS", sweFactory.createBoolean()
                                    .value(false)
                                    .label("Minus"))
                            .addField("NONE", sweFactory.createBoolean()
                                    .value(false)
                                    .label("none")
                            )
                        )
                )
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
        logger.debug("Initializing Wii Output Complete");
    }

    public void getAccelData(Mote mote) {
        mote.addAccelerometerListener(new AccelerometerListener<Mote>() {
            @Override
            public void accelerometerChanged(AccelerometerEvent accelerometerEvent) {
                motionX = accelerometerEvent.getX();
                motionY = accelerometerEvent.getY();
                motionZ = accelerometerEvent.getZ();
            }
        });

    }
    public void getButtonData(Mote mote){
        mote.addCoreButtonListener(new CoreButtonListener() {
            @Override
            public void buttonPressed(CoreButtonEvent coreButtonEvent) {
                BUTTON_A = coreButtonEvent.isButtonAPressed();
                BUTTON_B = coreButtonEvent.isButtonBPressed();
                BUTTON_MINUS = coreButtonEvent.isButtonMinusPressed();
                BUTTON_PLUS = coreButtonEvent.isButtonPlusPressed();
                BUTTON_HOME = coreButtonEvent.isButtonHomePressed();
                D_PAD_LEFT = coreButtonEvent.isDPadLeftPressed();
                D_PAD_RIGHT = coreButtonEvent.isDPadRightPressed();
                D_PAD_DOWN = coreButtonEvent.isDPadDownPressed();
                D_PAD_UP = coreButtonEvent.isDPadUpPressed();
                NONE = coreButtonEvent.isNoButtonPressed();
                BUTTON_TWO = coreButtonEvent.isButtonTwoPressed();
                BUTTON_ONE = coreButtonEvent.isButtonOnePressed();

            }
        });

    }


    /**
     * Begins processing data for output
     */
    public void doStart() {

        mote = PairWii.getInstance().mote;

        if (mote == null){
            logger.debug("mote is null.");
            logger.debug("finding wii remote again");
            mote = PairWii.getInstance().findMote();
        }

        else{
            System.out.println("getting button and accel data");
            getAccelData(mote);
            logger.debug( "accel data set");
            logger.debug("x val: "+ motionX);
            logger.debug("y val: "+ motionY);
            logger.debug("z val: "+ motionZ);

            getButtonData(mote);
            mote.setReportMode(ReportModeRequest.DATA_REPORT_0x31);

        }

        // Instantiate a new worker thread
        worker = new Thread(this, this.name);

        // TODO: Perform other startup
        logger.info("Starting worker thread: {}", worker.getName());

        // Start the worker thread
        worker.start();
    }


    /**
     * Terminates processing data for output
     */
    public void doStop() {

        synchronized (processingLock) {

            stopProcessing = true;
        }

        mote.disconnect();

        logger.debug("wii remote disconnected");
        // TODO: Perform other shutdown procedures
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {
        return worker.isAlive();
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

        long accumulator = 0;

        synchronized (histogramLock) {

            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {

                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    @Override
    public void run() {

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

            while (processSets) {

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }

                synchronized (histogramLock) {

                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                    // Set latest sampling time to now
                    lastSetTimeMillis = timingHistogram[setIndex];
                }

                ++setCount;

                double timestamp = System.currentTimeMillis() / 1000d;

                // TODO: Populate data block
                dataBlock.setDoubleValue(0, timestamp);

                //set underlying wii remote buttons and accel data
                AbstractDataBlock accelData= ((DataBlockMixed) dataBlock).getUnderlyingObject()[1];
                AbstractDataBlock wiiButtonData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[2];

                wiiButtonData.setBooleanValue(0, D_PAD_UP);
                wiiButtonData.setBooleanValue(1, D_PAD_DOWN);
                wiiButtonData.setBooleanValue(2, D_PAD_LEFT);
                wiiButtonData.setBooleanValue(3, D_PAD_RIGHT);
                wiiButtonData.setBooleanValue(4, BUTTON_HOME);
                wiiButtonData.setBooleanValue(5, BUTTON_ONE);
                wiiButtonData.setBooleanValue(6, BUTTON_TWO);
                wiiButtonData.setBooleanValue(7, BUTTON_A);
                wiiButtonData.setBooleanValue(8, BUTTON_B);
                wiiButtonData.setBooleanValue(9, BUTTON_PLUS);
                wiiButtonData.setBooleanValue(10, BUTTON_MINUS);
                wiiButtonData.setBooleanValue(11, NONE);



                //TODO: get it to update as the wii remote moves around
                //set accel data output
                accelData.setDoubleValue(0, motionX);
                accelData.setDoubleValue(1, motionY);
                accelData.setDoubleValue(2, motionZ);

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, WiiOutput.this, dataBlock));

                synchronized (processingLock) {

                    processSets = !stopProcessing;
                }
            }

        } catch (Exception e) {

            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);

        } finally {

            stopProcessing = false;

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }


}
