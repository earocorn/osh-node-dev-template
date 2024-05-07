package com.sample.impl.sensor.universalcontroller.helpers;

import com.alexalmanza.controller.wii.identifiers.WiiIdentifier;

public class ControllerMappingPreset {

    public String componentName;
    public int controllerIndex;

    // Options for controller mapping
    /**
     * This mapping will switch to next controller upon press of mapped button.
     */
    public boolean cyclesPrimaryController;

    /**
     * This mapping will set primary controller
     */
    public boolean overridesPrimaryController;

}
