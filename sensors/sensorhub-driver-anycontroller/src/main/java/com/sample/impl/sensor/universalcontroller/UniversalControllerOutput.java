/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.universalcontroller;

import com.alexalmanza.controller.wii.identifiers.WiiIdentifier;
import com.alexalmanza.interfaces.IController;
import com.alexalmanza.models.ControllerComponent;
import com.sample.impl.sensor.universalcontroller.helpers.ControllerMappingPreset;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;

/**
 * Output specification and provider for {@link UniversalControllerSensor}.
 *
 * @author your_name
 * @since date
 */
public class UniversalControllerOutput extends AbstractSensorOutput<UniversalControllerSensor> implements Runnable {

    private static final String SENSOR_OUTPUT_NAME = "UniversalControllerOutput";
    private static final String SENSOR_OUTPUT_LABEL = "UniversalController";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Currently connected controller outputs.";

    private static final Logger logger = LoggerFactory.getLogger(UniversalControllerOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private Thread worker;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    UniversalControllerOutput(UniversalControllerSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("UniversalControllerOutput created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() throws SensorException {

        logger.debug("Initializing UniversalControllerOutput");

        // Get an instance of SWE Factory suitable to build components
//        GamepadHelper sweFactory = new GamepadHelper();
//
//        dataStruct = sweFactory.newGamepadOutput(SENSOR_OUTPUT_NAME, gamepadUtil.getGamepad());
//
//        dataStruct.setLabel(SENSOR_OUTPUT_LABEL);
//        dataStruct.setDescription(SENSOR_OUTPUT_DESCRIPTION);

        SWEHelper sweFactory = new SWEHelper();

        SWEBuilders.DataRecordBuilder recordBuilder;
        DataRecord controllersRecord;

        recordBuilder = sweFactory.createRecord()
                .name("gamepads")
                .label("Gamepads")
                .description("List of connected gamepads.");

        for(IController controller : parentSensor.allControllers) {
            SWEBuilders.DataRecordBuilder controllerRecord = sweFactory.createRecord()
                    // TODO: better identification
                    .name("gamepad" + parentSensor.allControllers.indexOf(controller))
                    .label(controller.getControllerData().getName())
                    .description("Auto-populated gamepad data");

            for (ControllerComponent component : controller.getControllerData().getOutputs()) {
                controllerRecord.addField(component.getName(), sweFactory.createQuantity()
                        .value(component.getValue())
                        .addAllowedInterval(-1.0f, 1.0f));
            }

            DataRecord builtControllerRecord = controllerRecord.build();

            recordBuilder.addField(builtControllerRecord.getName(), builtControllerRecord);
        }

        controllersRecord = recordBuilder.build();

        int[] controllerIndices = new int[parentSensor.allControllers.size()];
        for (int i = 0; i < parentSensor.allControllers.size(); i++) {
            controllerIndices[i] = i;
        }

        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("primaryControllerIndex", sweFactory.createCount()
                        .label("Primary Controller Index")
                        .description("Index of the primary controller in use")
                        .value(parentSensor.getConfiguration().primaryControllerIndex)
                        .addAllowedValues(controllerIndices))
                .addField(controllersRecord.getName(), controllersRecord)
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing UniversalControllerOutput Complete");
    }

    /**
     * Begins processing data for output
     */
    public void doStart() {

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

        // stop all controller observers and disconnect all controllers
        for(IController controller : parentSensor.allControllers) {
            controller.getObserver().doStop();
            controller.disconnect();
        }
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

                // adjust polling rate from config
                Thread.sleep(parentSensor.getConfiguration().pollingRate);

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

                dataBlock.setDoubleValue(0, timestamp);


                for (ControllerMappingPreset preset : parentSensor.getConfiguration().controllerLayerConfig.presets) {
                    IController controller = parentSensor.allControllers.get(preset.controllerIndex);
                    WiiIdentifier component = preset.component;

                    if (preset.cyclesPrimaryController) {
                        for (ControllerComponent controllerComponent : controller.getControllerData().getOutputs()) {
                            if (controllerComponent.getName().equals(component.getName())) {
                                if (controllerComponent.getValue() == 1.0f) {
                                    parentSensor.getConfiguration().primaryControllerIndex++;
                                    if (parentSensor.getConfiguration().primaryControllerIndex >= parentSensor.allControllers.size()) {
                                        parentSensor.getConfiguration().primaryControllerIndex = 0;
                                    }
                                }
                            }
                        }
                    }
                }

                dataBlock.setIntValue(1, parentSensor.getConfiguration().primaryControllerIndex);

                AbstractDataBlock gamepadsData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[2];

                for (int i = 0; i < parentSensor.allControllers.size(); i++) {
                    IController controller = parentSensor.allControllers.get(i);
                    AbstractDataBlock controllerDataBlock = ((DataBlockMixed) gamepadsData).getUnderlyingObject()[i];

                    for (int recordNum = 0; recordNum < controller.getControllerData().getOutputs().size(); recordNum++) {
                        controllerDataBlock.setDoubleValue(recordNum, controller.getControllerData().getOutputs().get(recordNum).getValue());
                    }

                }

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, UniversalControllerOutput.this, dataBlock));

                synchronized (processingLock) {

                    processSets = !stopProcessing;
                }
            }

        } catch (Exception e) {

            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);

        } finally {

            // Reset the flag so that when driver is restarted loop thread continues
            // until doStop called on the output again
            stopProcessing = false;

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}
