/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.gamepad;

import com.alexalmanza.model.GamepadAxis;
import com.alexalmanza.observer.GamepadListener;
import com.alexalmanza.observer.GamepadObserver;
import com.alexalmanza.GamepadUtil;
import com.sample.impl.sensor.gamepad.helpers.GamepadHelper;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;
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

import java.io.File;

/**
 * Output specification and provider for {@link GamepadSensor}.
 *
 * @author your_name
 * @since date
 */
public class GamepadOutput extends AbstractSensorOutput<GamepadSensor> implements Runnable {

    private static final String SENSOR_OUTPUT_NAME = "GamepadOutput";
    private static final String SENSOR_OUTPUT_LABEL = "Gamepad";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Current gamepad output.";

    private static final Logger logger = LoggerFactory.getLogger(GamepadOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private Thread worker;
    private Controller gamepad = null;
    private Component[] gamepadComponents = null;
    private GamepadUtil gamepadUtil = null;
    private Event event;
    private GamepadObserver eventObserver;
    private String axisData;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    GamepadOutput(GamepadSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("GamepadOutput created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() throws SensorException {
        event = new Event();

        logger.info("Fetching resource...");
        logger.info("java.library.path = " + System.getProperty("java.library.path"));
        System.setProperty("java.library.path", new File("jiraw").getAbsolutePath());
        logger.info("java.library.path = " + System.getProperty("java.library.path"));

        logger.debug("Initializing GamepadOutput");

        // Sample setup from https://jinput.github.io/jinput/

        gamepadUtil = new GamepadUtil();
        gamepadComponents = gamepadUtil.getGamepadComponents();

        // Get an instance of SWE Factory suitable to build components
        GamepadHelper sweFactory = new GamepadHelper();

        dataStruct = sweFactory.newGamepadOutput(SENSOR_OUTPUT_NAME, gamepadUtil.getGamepad());

        dataStruct.setLabel(SENSOR_OUTPUT_LABEL);
        dataStruct.setDescription(SENSOR_OUTPUT_DESCRIPTION);

        axisData = "";

        // Instantiate the observer in the gamepad output setup
        eventObserver = GamepadObserver.getInstance();
        eventObserver.setEvent(event);

        // Two listeners defined with the only method being logging their component's name and data which is the float value
        GamepadListener aButtonEvent = (identifier, value) -> axisData = (identifier + " = " + event.getComponent().getPollData());
        GamepadListener axisEvent = (identifier, value) -> logger.info(identifier + " = " + gamepadUtil.getDirection(GamepadAxis.LEFT_JOYSTICK));
        GamepadListener povEvent = (identifier, value) -> logger.info(identifier + " = " + gamepadUtil.getDirection(GamepadAxis.D_PAD));
        GamepadListener zEvent = (identifier, value) -> logger.info(identifier+ " = " + gamepadUtil.getComponentValue(identifier) + "\nTrigger Left, Right: " + gamepadUtil.getTriggerPressure(true) + ", " + gamepadUtil.getTriggerPressure(false));

        // Button 0 is usually the Identifier for the primary button on gamepad which is usually the A or X button
        eventObserver.addListener(aButtonEvent, Component.Identifier.Button._0);
        // POV is the Identifier for the D-Pad component
        eventObserver.addListener(axisEvent, Component.Identifier.Axis.X);
        eventObserver.addListener(povEvent, Component.Identifier.Axis.POV);
        eventObserver.addListener(zEvent, Component.Identifier.Axis.Z);

        for(Component component : gamepadComponents) {
           logger.info(component.getIdentifier() + " deadzone: " + component.getDeadZone());
        }

//        dataStruct = sweFactory.createRecord()
//                .name(SENSOR_OUTPUT_NAME)
//                .label(SENSOR_OUTPUT_LABEL)
//                .description(SENSOR_OUTPUT_DESCRIPTION)
//                .addField("sampleTime", sweFactory.createTime()
//                        .asSamplingTimeIsoUTC()
//                        .label("Sample Time")
//                        .description("Time of data collection"))
//                .addField("gamepadData", sweFactory.createRecord()
//                        .addField("joystick", sweFactory.createRecord()
//                                .addField("y", sweFactory.createQuantity()
//                                        //TODO .definition() NEED TO ADD DEFINITIONS and possibly unit of measurement???
//                                        .label("Joystick Y-Axis"))
//                                .addField("x", sweFactory.createQuantity()
//                                        .label("Joystick X-Axis"))
//                                .addField("ry", sweFactory.createQuantity()
//                                        .label("Joystick Rotational Y"))
//                                .addField("rx", sweFactory.createQuantity()
//                                        .label("Joystick Rotational X"))
//                                .addField("dpad", sweFactory.createQuantity()
//                                        //TODO make it easier to get orientation possibly with up-down-left-right booleans
//                                        .label("D-Pad Orientation")
//                                        .addAllowedValues(0.0, 0.125, 0.250, 0.375, 0.500, 0.625, 0.750, 0.875, 1.0)
//                                        .value(0.0))
//                                .build())
//                        .addField("buttons", sweFactory.createRecord()
//                                .addField("a", sweFactory.createBoolean()
//                                        .label("A-Button Selected")
//                                        .value(false))
//                                .addField("b", sweFactory.createBoolean()
//                                        .label("B-Button Selected")
//                                        .value(false))
//                                .addField("x", sweFactory.createBoolean()
//                                        .label("X-Button Selected")
//                                        .value(false))
//                                .addField("y", sweFactory.createBoolean()
//                                        .label("Y-Button Selected")
//                                        .value(false))
//                                .addField("l1", sweFactory.createBoolean()
//                                        .label("Left1 Trigger-Button Selected")
//                                        .value(false))
//                                .addField("r1", sweFactory.createBoolean()
//                                        .label("Right1 Trigger-Button Selected")
//                                        .value(false))
//                                .addField("select", sweFactory.createBoolean()
//                                        .label("Select-Button Selected")
//                                        .value(false))
//                                .addField("start", sweFactory.createBoolean()
//                                        .label("Start-Button Selected")
//                                        .value(false))
//                                .addField("leftJoystick", sweFactory.createBoolean()
//                                        .label("Left Joystick Button Selected")
//                                        .value(false))
//                                .addField("rightJoystick", sweFactory.createBoolean()
//                                        .label("Right Joystick Button Selected")
//                                        .value(false))
//                                .addField("l2", sweFactory.createQuantity()
//                                        .label("Left2 Trigger Pressure"))
//                                .addField("r2", sweFactory.createQuantity()
//                                        .label("Right2 Trigger Pressure"))
//                                .build())
//                    .label("Output data from gamepad"))
//                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing GamepadOutput Complete");
    }

    /**
     * Begins processing data for output
     */
    public void doStart() {

        eventObserver.doStart();
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

        eventObserver.doStop();
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

                if(gamepad != null) {
                    // Poll the game controller for any updates
                    gamepad.poll();
                }

                dataBlock.setDoubleValue(0, timestamp);

                // Collective gamepad data, which is separated into 2 parts, joystick data and button data
                AbstractDataBlock gamepadData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[1];

                if(gamepadUtil != null) {
                    gamepadUtil.pollGamepad();
                }

                for(int i = 0; i < gamepadComponents.length; i++) {
                   gamepadData.setDoubleValue(i, gamepadComponents[i].getPollData());
                }

                AbstractDataBlock actionData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[2];
                actionData.setStringValue(axisData);

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

                eventHandler.publish(new DataEvent(latestRecordTime, GamepadOutput.this, dataBlock));

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
