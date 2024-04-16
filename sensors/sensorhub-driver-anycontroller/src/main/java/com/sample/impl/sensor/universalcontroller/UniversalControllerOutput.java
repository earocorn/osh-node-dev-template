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

import com.alexalmanza.interfaces.IController;
import com.alexalmanza.models.ControllerComponent;
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
                .name("controllers")
                .label("Gamepads")
                .description("List of connected gamepads.");

        for(IController controller : parentSensor.allControllers) {
            SWEBuilders.DataRecordBuilder controllerRecord = sweFactory.createRecord()
                    // TODO: better identification
                    .name(controller.getControllerData().getName())
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

                dataBlock.setIntValue(1, parentSensor.getConfiguration().primaryControllerIndex);

                AbstractDataBlock gamepadsData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[2];

                for (int i = 0; i < parentSensor.allControllers.size(); i++) {
                    IController controller = parentSensor.allControllers.get(i);
                    AbstractDataBlock controllerDataBlock = ((DataBlockMixed) gamepadsData).getUnderlyingObject()[i];

                    for (int recordNum = 0; recordNum < controller.getControllerData().getOutputs().size(); recordNum++) {
                        controllerDataBlock.setDoubleValue(recordNum, controller.getControllerData().getOutputs().get(recordNum).getValue());
                    }

                }

//                AbstractDataBlock joystickData = ((DataBlockMixed) gamepadData).getUnderlyingObject()[0];
//                AbstractDataBlock buttonData = ((DataBlockMixed) gamepadData).getUnderlyingObject()[1];
//
//                // Orientation of joysticks and d-pad
//                joystickData.setDoubleValue(0, gamepad.getComponent(Component.Identifier.Axis.Y).getPollData());
//                joystickData.setDoubleValue(1, gamepad.getComponent(Component.Identifier.Axis.X).getPollData());
//                joystickData.setDoubleValue(2, gamepad.getComponent(Component.Identifier.Axis.RY).getPollData());
//                joystickData.setDoubleValue(3, gamepad.getComponent(Component.Identifier.Axis.RX).getPollData());
//                joystickData.setDoubleValue(4, gamepad.getComponent(Component.Identifier.Axis.POV).getPollData());
//
//                // Set boolean value for whether button is selected or not
//                buttonData.setBooleanValue(0, gamepad.getComponent(Component.Identifier.Button._0).getPollData() == 1.0);
//                buttonData.setBooleanValue(1, gamepad.getComponent(Component.Identifier.Button._1).getPollData() == 1.0);
//                buttonData.setBooleanValue(2, gamepad.getComponent(Component.Identifier.Button._2).getPollData() == 1.0);
//                buttonData.setBooleanValue(3, gamepad.getComponent(Component.Identifier.Button._3).getPollData() == 1.0);
//                buttonData.setBooleanValue(4, gamepad.getComponent(Component.Identifier.Button._4).getPollData() == 1.0);
//                buttonData.setBooleanValue(5, gamepad.getComponent(Component.Identifier.Button._5).getPollData() == 1.0);
//                buttonData.setBooleanValue(6, gamepad.getComponent(Component.Identifier.Button._6).getPollData() == 1.0);
//                buttonData.setBooleanValue(7, gamepad.getComponent(Component.Identifier.Button._7).getPollData() == 1.0);
//                buttonData.setBooleanValue(8, gamepad.getComponent(Component.Identifier.Button._8).getPollData() == 1.0);
//                buttonData.setBooleanValue(9, gamepad.getComponent(Component.Identifier.Button._9).getPollData() == 1.0);
//                // Left and right trigger pressure
//                buttonData.setDoubleValue(10, Math.max(gamepad.getComponent(Component.Identifier.Axis.Z).getPollData(), 0));
//                buttonData.setDoubleValue(11, Math.min(gamepad.getComponent(Component.Identifier.Axis.Z).getPollData(), 0));

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
