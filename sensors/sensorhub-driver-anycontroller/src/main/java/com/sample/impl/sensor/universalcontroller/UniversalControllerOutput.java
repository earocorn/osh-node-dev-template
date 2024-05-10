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
import com.alexalmanza.models.ControllerType;
import com.sample.impl.sensor.universalcontroller.helpers.ControllerCyclingAction;
import com.sample.impl.sensor.universalcontroller.helpers.ControllerMappingPreset;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;

import java.lang.Boolean;

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

    // Stuff from config
    private int primaryControllerIndex;
    private long pollingRate;
    private ControllerLayerConfig controllerLayerConfig;
    private boolean hasSensitivity;

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

        primaryControllerIndex = parentSensor.getConfiguration().primaryControllerIndex;
        pollingRate = parentSensor.getConfiguration().pollingRate;
        controllerLayerConfig = parentSensor.getConfiguration().controllerLayerConfig;
        hasSensitivity = parentSensor.getConfiguration().hasSensitivity;

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
                String uriName = component.getName();
                if(uriName.equals("x") || uriName.equals("y") || uriName.equals("z")) {
                    uriName += "Axis";
                }
                uriName = uriName.replace(" ", "");
                controllerRecord.addField(component.getName().replace(" ", ""), sweFactory.createQuantity()
                        .value(component.getValue())
                        .definition(SWEHelper.getPropertyUri(uriName))
                        .addAllowedInterval(-1.0f, 1.0f));
            }

            controllerRecord.addField("isPrimaryController", sweFactory.createBoolean()
                    .label("Is Primary Controller")
                    .definition(SWEHelper.getPropertyUri("IsPrimaryController"))
                    .value(false));

            // TODO: make this better OR just do all sensitivity logic in process
            if(hasSensitivity) {
                controllerRecord.addField("sensitivity", sweFactory.createQuantity()
                        .label("Sensitivity")
                        .value(1)
                        .addAllowedInterval(1, 10)
                        .dataType(DataType.INT));
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
                        .definition(SWEHelper.getPropertyUri("PrimaryControllerIndex"))
                        .value(primaryControllerIndex)
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
            if(controller.getControllerData().getControllerType() == ControllerType.WIIMOTE) {
                parentSensor.cancelWiiMoteSearch();
            }
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
                Thread.sleep(pollingRate);

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

                // Go through controller mapping
                for (ControllerMappingPreset preset : controllerLayerConfig.presets) {
                    IController controller = parentSensor.allControllers.get(preset.controllerIndex);
                    int componentsForCombination = preset.componentNames.size();

                    if (preset.controllerCyclingAction.equals(ControllerCyclingAction.CYCLES_PRIMARY_CONTROLLER)) {
                        // TODO: only cycle to next controller if current controller is primary controller?
                        for (ControllerComponent controllerComponent : controller.getControllerData().getOutputs()) {
                            if (preset.componentNames.contains(controllerComponent.getName())) {
                                if (controllerComponent.getValue() == 1.0f) {
                                    componentsForCombination--;
                                }
                                if(componentsForCombination == 0) {
                                    primaryControllerIndex++;
                                    if (primaryControllerIndex >= parentSensor.allControllers.size()) {
                                        primaryControllerIndex = 0;
                                    }
                                }
                            }
                        }
                    }

                    if (preset.controllerCyclingAction.equals(ControllerCyclingAction.OVERRIDES_PRIMARY_CONTROLLER)) {
                        componentsForCombination = preset.componentNames.size();
                        for(ControllerComponent controllerComponent : controller.getControllerData().getOutputs()) {
                            if(preset.componentNames.contains(controllerComponent.getName())) {
                                if(controllerComponent.getValue() == 1.0f) {
                                    componentsForCombination--;
                                }
                                if(componentsForCombination == 0) {
                                    primaryControllerIndex = parentSensor.allControllers.indexOf(controller);
                                }
                            }
                        }
                    }
                }

                dataBlock.setIntValue(1, primaryControllerIndex);

                AbstractDataBlock gamepadsData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[2];

                for (int i = 0; i < parentSensor.allControllers.size(); i++) {
                    IController controller = parentSensor.allControllers.get(i);
                    AbstractDataBlock controllerDataBlock = ((DataBlockMixed) gamepadsData).getUnderlyingObject()[i];

                    for (int recordNum = 0; recordNum < controller.getControllerData().getOutputs().size(); recordNum++) {
                        controllerDataBlock.setDoubleValue(recordNum, controller.getControllerData().getOutputs().get(recordNum).getValue());
                    }
                    controllerDataBlock.setBooleanValue(controller.getControllerData().getOutputs().size(), i == primaryControllerIndex);
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
