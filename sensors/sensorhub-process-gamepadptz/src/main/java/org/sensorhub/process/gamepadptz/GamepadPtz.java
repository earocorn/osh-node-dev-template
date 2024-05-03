/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.gamepadptz;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of a binary operation with respect to a parameter (addition,
 * soustraction, multiplication, division, power)
 * </p>
 *
 * @author Alexandre Robin & Gregoire Berthiau
 * @date Mar 7, 2007
 */
public class GamepadPtz extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("dpadPTZ", "DPad (POV) PTZ Process", null, GamepadPtz.class);
    private Quantity dpad;
    private DataRecord ptzOutput;

    float curPan = 0;
    float curTilt = 0;
    float newPan = 0, newTilt = 0;
    double curDpadValue = 0;

    public GamepadPtz()
    {
        super(INFO);

        SWEHelper sweHelper = new SWEHelper();

        // inputs
        inputData.add("pov", dpad = sweHelper.createQuantity()
                .label("Hat Switch")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());

        // outputs
        outputData.add("ptz", ptzOutput = sweHelper.createRecord()
                .addField("pan", sweHelper.createQuantity()
                        .definition(SWEHelper.getPropertyUri("PanAngle"))
                        .label("Pan")
                        .uomCode("deg")
                        .dataType(DataType.FLOAT))
                .addField("tilt", sweHelper.createQuantity()
                        .definition(SWEHelper.getPropertyUri("TiltAngle"))
                        .label("Tilt")
                        .uomCode("deg")
                        .dataType(DataType.FLOAT))
                .addField("zoom", sweHelper.createQuantity()
                        .definition(SWEHelper.getPropertyUri("ZoomFactor"))
                        .label("Zoom Factor")
                        .uomCode("1")
                        .value(0)
                        .dataType(DataType.SHORT))
                .build());
    }


    @Override
    public void init() throws ProcessException
    {
        super.init();
    }


    @Override
    public void execute() throws ProcessException
    {
        try {
            curDpadValue = dpad.getValue();

            if(curDpadValue != 0) {
                curPan = ptzOutput.getField("pan").getData().getFloatValue();
                curTilt = ptzOutput.getField("tilt").getData().getFloatValue();

                // D-Pad values arranged in 8 parts from UP_LEFT(0.125) to LEFT(1.0) in a clockwise sequence
                if (curDpadValue == 0.125f) {
                    newPan = curPan - 15;
                    newTilt = curTilt + 15;
                } else if (curDpadValue == 0.25f) {
                    newPan = curPan;
                    newTilt = curTilt + 15;
                } else if (curDpadValue == 0.375f) {
                    newPan = curPan + 15;
                    newTilt = curTilt + 15;
                } else if (curDpadValue == 0.5f) {
                    newPan = curPan + 15;
                    newTilt = curTilt;
                } else if (curDpadValue == 0.625f) {
                    newPan = curPan + 15;
                    newTilt = curTilt - 15;
                } else if (curDpadValue == 0.75f) {
                    newPan = curPan;
                    newTilt = curTilt - 15;
                } else if (curDpadValue == 0.875f) {
                    newPan = curPan - 15;
                    newTilt = curTilt - 15;
                } else if (curDpadValue == 1.0f) {
                    newPan = curPan - 15;
                    newTilt = curTilt;
                } else {
                    newPan = curPan;
                    newTilt = curTilt;
                }

                if(newPan >= 180) {
                    newPan = Math.min(newPan, 180.0f);
                } else if (newPan <= -180) {
                    newPan = Math.max(newPan, 180.0f);
                }

                if(newTilt >= 180) {
                    newTilt = Math.min(newTilt, 180.0f);
                } else if(newTilt <= 0) {
                    newTilt = Math.max(newTilt, 0.0f);
                }

                getLogger().debug("New Pan and Tilt = [{},{}]", newPan, newTilt);

                ptzOutput.getData().setDoubleValue(0, newPan);
                ptzOutput.getData().setDoubleValue(1, newTilt);
            }
        } catch (Exception e) {
            reportError("Error computing PTZ position");
        }
    }
}