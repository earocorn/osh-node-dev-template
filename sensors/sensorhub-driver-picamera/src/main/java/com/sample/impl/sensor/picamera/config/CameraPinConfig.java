package com.sample.impl.sensor.picamera.config;

import org.sensorhub.api.config.DisplayInfo;

public class CameraPinConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "Tilt Servo Pin", desc = "GPIO pin to control Pi Camera tilt")
    public int pinConfig = 26;

}
