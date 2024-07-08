/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rapiscan;


import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Sensor driver for the ... providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Drew Botts
 * @since Oct 16, 2023
 */
public class RapiscanSensor extends AbstractSensorModule<RapiscanConfig> {

    MessageHandler messageHandler;

    private static final Logger logger = LoggerFactory.getLogger(RapiscanSensor.class);

    ICommProvider<?> commProvider;

    GammaOutput gammaOutput;

    NeutronOutput neutronOutput;

    OccupancyOutput occupancyOutput;

    LocationOutput locationOutput;

    TamperOutput tamperOutput;

    SpeedOutput speedOutput;

    InputStream msgIn;

    Timer t;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:rapiscan:", config.serialNumber);
        generateXmlID("Rapiscan", config.serialNumber);

        gammaOutput = new GammaOutput(this);
        addOutput(gammaOutput, false);
        gammaOutput.init();

        neutronOutput = new NeutronOutput(this);
        addOutput(neutronOutput, false);
        neutronOutput.init();

        occupancyOutput = new OccupancyOutput(this);
        addOutput(occupancyOutput, false);
        occupancyOutput.init();

        locationOutput = new LocationOutput(this);
        addOutput(locationOutput, false);
        locationOutput.init();

        tamperOutput = new TamperOutput(this);
        addOutput(tamperOutput, false);
        tamperOutput.init();

        speedOutput = new SpeedOutput(this);
        addOutput(speedOutput, false);
        speedOutput.init();



    }

    @Override
    protected void doStart() throws SensorHubException {

//        locationOutput.setLocationOuput(config.getLocation());
        setLocationRepeatTimer();

        // init comm provider
        if (commProvider == null) {

            // we need to recreate comm provider here because it can be changed by UI
            try {

                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                var moduleReg = getParentHub().getModuleRegistry();

                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);

                commProvider.start();

            } catch (Exception e) {

                commProvider = null;

                throw e;
            }
        }

        // connect to data stream
        try {

            msgIn = new BufferedInputStream(commProvider.getInputStream());

            messageHandler = new MessageHandler(msgIn, gammaOutput, neutronOutput, occupancyOutput, tamperOutput, speedOutput);

//            csvMsgRead.readMessages(msgIn, gammaOutput, neutronOutput, occupancyOutput);

        } catch (IOException e) {

            throw new SensorException("Error while initializing communications ", e);
        }
    }

    @Override
    public void doStop() throws SensorHubException {

        if (commProvider != null) {

            try {
                t.cancel();
                t.purge();
                commProvider.stop();

            } catch (Exception e) {

                logger.error("Uncaught exception attempting to stop comms module", e);

            } finally {

                commProvider = null;
            }
        }

        messageHandler.stopProcessing();
    }

    @Override
    public boolean isConnected() {
        if (commProvider == null) {

            return false;

        } else {

            return commProvider.isStarted();
        }
    }

    void setLocationRepeatTimer(){
        t = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                locationOutput.setLocationOuput(config.getLocation());
                System.out.println("location updated");
            }
        };
        t.scheduleAtFixedRate(tt,500,10000);

    }
}
