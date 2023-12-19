package com.sample.impl.sensor.gamepad;

import net.java.games.input.*;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.sensor.SensorException;
import org.vast.swe.SWEBuilders;

public class GamepadUtil {

    /**
     * The main gamepad controller
     */
    private Controller gamepad;

    /**
     * List of gamepad components associated with the main gamepad controller
     */
    private Component[] gamepadComponents;
    public GamepadUtil() throws SensorException {

        // Sample setup from https://jinput.github.io/jinput/
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

        for(Controller controller : controllers) {
            if(controller.getType() == Controller.Type.GAMEPAD) {
                gamepad = controller;
            }
        }

        if(gamepad == null) {
            throw new SensorException("Failed to fetch game controller!");
        }

        gamepadComponents = gamepad.getComponents();

    }

    /**
     * Poll the gamepad for updates, to populate data of each gamepad component
     */
    public void pollGamepad() {
        if(gamepad != null) {
            gamepad.poll();
        }
    }

    /**
     * Populate a DataRecord with the data of the gamepad outputs, assuming the DataRecord is set up in the order that the components are in
     *
     * @param dataBlock
     */
    public void populateGamepadOutput(DataBlock dataBlock) {
        for (int i = 0; i < gamepadComponents.length; i++) {
            dataBlock.setDoubleValue(i, gamepadComponents[i].getPollData());
        }
    }

    /**
     * Retrieve a list of the current gamepad's components
     *
     * @return gamepadComponents
     */
    public Component[] getGamepadComponents() {
        return gamepadComponents;
    }

    /**
     * Retrieve the current gamepad
     *
     * @return gamepad
     */
    public Controller getGamepad() {
        return gamepad;
    }

}
