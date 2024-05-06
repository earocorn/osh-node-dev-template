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

import com.alexalmanza.controller.wii.WiiMote;
import com.alexalmanza.controller.wii.WiiMoteConnection;
import com.alexalmanza.interfaces.IController;
import com.alexalmanza.interfaces.IControllerConnection;
import com.alexalmanza.models.ControllerType;
import com.alexalmanza.util.FindControllers;
import net.java.games.input.Event;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author your_name
 * @since date
 */
public class UniversalControllerSensor extends AbstractSensorModule<UniversalControllerConfig> {

    private static final Logger logger = LoggerFactory.getLogger(UniversalControllerSensor.class);
    UniversalControllerOutput output;
    ArrayList<IController> allControllers = new ArrayList<>();
    private FindControllers findControllers;
    private WiiMoteConnection wiiMoteConnection;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:", config.serialNumber);
        generateXmlID("UNIVERSAL_CONTROLLER", config.serialNumber);

        ControllerType[] typesArray = new ControllerType[config.controllerTypes.size()];

        for (int i = 0; i < config.controllerTypes.size(); i++) {
            ControllerType type = config.controllerTypes.get(i);
            typesArray[i] = type;
        }

        try {
            findControllers = new FindControllers(new Event(), typesArray);
            if(!findControllers.getControllers().isEmpty()) {
                allControllers = findControllers.getControllers();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        // Create and initialize output
        output = new UniversalControllerOutput(this);

        addOutput(output, false);

        output.doInit();

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

    public void cancelWiiMoteSearch() {
        wiiMoteConnection = (WiiMoteConnection) findControllers.getControllerConnection(ControllerType.WIIMOTE);
        wiiMoteConnection.cancelSearch();
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != output) {

            wiiMoteConnection = (WiiMoteConnection) findControllers.getControllerConnection(ControllerType.WIIMOTE);
            wiiMoteConnection.cancelSearch();

            for(IController controller : allControllers) {
                controller.disconnect();
            }

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
