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

import com.alexalmanza.GamepadInit;
import com.alexalmanza.GamepadObserver;
import com.alexalmanza.observer.GamepadListener;
import com.alexalmanza.observer.GamepadObserver;
import net.java.games.input.Component;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author your_name
 * @since date
 */
public class GamepadSensor extends AbstractSensorModule<GamepadConfig> {

    private static final Logger logger = LoggerFactory.getLogger(GamepadSensor.class);
    GamepadOutput output;
    GamepadObserver observer;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:", config.serialNumber);
        generateXmlID("GAMEPAD", config.serialNumber);

        // Create and initialize output
        output = new GamepadOutput(this);

        addOutput(output, false);

        output.doInit();

        observer = GamepadObserver.getInstance();

        // TODO: Perform other initialization
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != output) {

            // Allocate necessary resources and start outputs
            output.doStart();
        }



        // TODO: Perform other startup procedures
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != output) {

            output.doStop();
        }

        // TODO: Perform other shutdown procedures
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }
}
