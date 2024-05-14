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
import com.alexalmanza.models.ControllerData;
import com.alexalmanza.models.ControllerType;
import com.sample.impl.sensor.universalcontroller.helpers.ControllerCyclingAction;
import com.sample.impl.sensor.universalcontroller.helpers.ControllerMappingPreset;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.*;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;

import java.lang.Boolean;
import java.util.ArrayList;
import java.util.List;

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

        SWEHelper sweFactory = new SWEHelper();

        int[] controllerIndices = new int[parentSensor.allControllers.size()];
        for (int i = 0; i < parentSensor.allControllers.size(); i++) {
            controllerIndices[i] = i;
        }

        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .updatable(true)
                .label(SENSOR_OUTPUT_LABEL)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
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
                .addField("numGamepads", sweFactory.createCount()
                        .label("Num Gamepads")
                        .description("Number of connected gamepads")
                        .definition(SWEHelper.getPropertyUri("GamepadCount"))
                        .id("numGamepads"))
                .addField("gamepads", sweFactory.createArray()
                        .name("gamepads")
                        .label("Gamepads")
                        .description("List of connected gamepads.")
                        .definition(SWEHelper.getPropertyUri("GamepadArray"))
                        .withVariableSize("numGamepads")
                        .withElement("gamepad", sweFactory.createRecord()
                                .label("Gamepad")
                                .description("Gamepad Data")
                                .definition(SWEHelper.getPropertyUri("Gamepad"))
                                .addField("gamepadName", sweFactory.createText()
                                        .label("Gamepad Name")
                                        .definition("GamepadName"))
                                .addField("isPrimaryController", sweFactory.createBoolean()
                                        .label("Is Primary Controller")
                                        .definition(SWEHelper.getPropertyUri("IsPrimaryController"))
                                        .value(false))
                                .addField("numComponents", sweFactory.createCount()
                                        .label("Num Components")
                                        .description("Number of button and axis components on gamepad")
                                        .definition(SWEHelper.getPropertyUri("NumGamepadComponents"))
                                        .id("numComponents")
                                        .build())
                                .addField("gamepadComponents", sweFactory.createArray()
                                        .name("gamepadComponents")
                                        .label("Gamepad Components")
                                        .description("Data of Connected Gamepad Components")
                                        .definition(SWEHelper.getPropertyUri("GamepadComponentArray"))
                                        .withVariableSize("numComponents")
                                        .withElement("component", sweFactory.createRecord()
                                                .name("component")
                                                .label("Component")
                                                .description("Gamepad Component (A button, B button, X axis, etc.)")
                                                .definition(SWEHelper.getPropertyUri("GamepadComponent"))
                                                .addField("componentName", sweFactory.createText()
                                                        .label("Component Name")
                                                        .description("Name of component")
                                                        .definition(SWEHelper.getPropertyUri("ComponentName"))
                                                        .value(""))
                                                .addField("componentValue", sweFactory.createQuantity()
                                                        .label("Component Value")
                                                        .description("Value of component")
                                                        .definition(SWEHelper.getPropertyUri("ComponentValue"))
                                                        .dataType(DataType.FLOAT)
                                                        .value(0.0f)
                                                        .addAllowedInterval(-1.0f, 1.0f)))
                                        .build())))
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

    /**
     * Updates the primary controller index based on controller mappings.
     */
    public void updatePrimaryControllerIndex() {
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

                updatePrimaryControllerIndex();

                double timestamp = System.currentTimeMillis() / 1000d;

                dataBlock.setDoubleValue(0, timestamp);
                dataBlock.setIntValue(1, primaryControllerIndex);
                dataBlock.setIntValue(2, parentSensor.allControllers.size());

                // debug stuff
                // List of gamepads is a DataBlockList
                logger.debug("datablock : {}", ((AbstractDataBlock[]) dataBlock.getUnderlyingObject())[3].getClass());
                // gamepad is a DataBlockMixed
                logger.debug("gamepad component : {}", dataStruct.getComponent("gamepads").getComponent("gamepad").createDataBlock().getClass());
                // gamepadComponents array is a DataBlockParallel
                logger.debug("components component : {}", dataStruct.getComponent("gamepads").getComponent("gamepad").getComponent("gamepadComponents").createDataBlock().getClass());
                // Component itself is a DataBlockTuple
                logger.debug("cloned component block : {}", dataStruct.getComponent("gamepads").getComponent("gamepad").getComponent("gamepadComponents").getComponent("component").clone().createDataBlock().getClass());

                DataArrayImpl gamepadArray = (DataArrayImpl) dataStruct.getComponent("gamepads");
                gamepadArray.updateSize();
                List<DataBlock> gamepadList = new ArrayList<>();

                for (int i = 0; i < parentSensor.allControllers.size(); i++) {
                    IController controller = parentSensor.allControllers.get(i);
                    DataBlock gamepadDataBlock = dataStruct
                            .getComponent("gamepads")
                            .getComponent("gamepad")
                            .createDataBlock();

                    // Set each element for underlying gamepad object
                    gamepadDataBlock.setStringValue(0, "gamepad" + i);
                    gamepadDataBlock.setBooleanValue(1, i == primaryControllerIndex);
                    gamepadDataBlock.setIntValue(2, controller.getControllerData().getOutputs().size());

                    DataArrayImpl componentArray = (DataArrayImpl) dataStruct
                            .getComponent("gamepads")
                            .getComponent("gamepad")
                            .getComponent("gamepadComponents");
                    componentArray.updateSize(controller.getControllerData().getOutputs().size());

                    List<DataBlock> componentList = new ArrayList<>();

                    // Set component data from controllerData outputs
                    for(int componentIndex = 0; componentIndex < controller.getControllerData().getOutputs().size(); componentIndex++) {
                        ControllerComponent componentData = controller.getControllerData().getOutputs().get(componentIndex);

                        DataBlock componentDataBlock = dataStruct
                                .getComponent("gamepads")
                                .getComponent("gamepad")
                                .getComponent("gamepadComponents")
                                .getComponent("component")
                                .createDataBlock();

                        componentDataBlock.setStringValue(0, componentData.getName());
                        componentDataBlock.setFloatValue(1, componentData.getValue());

                        componentList.add(componentDataBlock);
                    }
                    // Set this list of components for each gamepad
                    ((AbstractDataBlock[])(gamepadDataBlock.getUnderlyingObject()))[3].setUnderlyingObject(componentList);

                    gamepadList.add(gamepadDataBlock);
                }

                // Set the list of gamepads
                //logger.debug("gamepadsDatablock = {}", ((AbstractDataBlock[]) dataBlock.getUnderlyingObject())[3].getClass());
                ((AbstractDataBlock[])(dataBlock.getUnderlyingObject()))[3].setUnderlyingObject(gamepadList);

                dataBlock.updateAtomCount();

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
