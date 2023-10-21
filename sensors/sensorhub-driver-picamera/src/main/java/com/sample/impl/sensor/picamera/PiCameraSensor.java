/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.picamera;

import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author your_name
 * @since date
 */
public class PiCameraSensor extends AbstractSensorModule<PiCameraConfig> {

    private static final Logger logger = LoggerFactory.getLogger(com.sample.impl.sensor.picamera.PiCameraSensor.class);

    PiCameraOutput output;

    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if(!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("A camera attached to a servo for tilt functionality, " +
                        "hosted on a raspberry pi.");

                SMLHelper smlHelper = new SMLHelper();
                smlHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlHelper.identifiers.serialNumber(config.serialNumber))
                        .addClassifier(smlHelper.classifiers.sensorType("Pi Camera Sensor"));
                // add characteristics of image pixels?

            }

        }

    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:picamera", config.serialNumber);
        generateXmlID("PI_CAMERA", config.serialNumber);

        // Create and initialize output
        output = new PiCameraOutput(this);

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
