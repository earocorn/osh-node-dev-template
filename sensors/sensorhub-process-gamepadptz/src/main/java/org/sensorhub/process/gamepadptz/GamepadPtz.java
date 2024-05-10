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

import net.opengis.swe.v20.*;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEBuilders;
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
    private Quantity zoomInHID;
    private Quantity zoomOutHID;
    private Quantity zoomInWii;
    private Quantity zoomOutWii;
    private Quantity zoomReset;
    private DataRecord ptzOutput;
    private DataRecord ptzInput;

    float curPan = 0;
    float curTilt = 0;
    float curZoom = 0;
    float newPan = 0, newTilt = 0;
    float newZoom = 0;
    double curDpadValue = 0;
    boolean isLeftPressed = false;
    boolean isRightPressed = false;
    boolean isMinusPressed = false;
    boolean isPlusPressed = false;
    boolean isBPressed = false;
    boolean isInOutMatched = false;

    public GamepadPtz()
    {
        super(INFO);

        SWEHelper sweHelper = new SWEHelper();

        // TODO: add different input to get current PTZ position

        // inputs
        inputData.add("pov", dpad = sweHelper.createQuantity()
                .label("Hat Switch")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        inputData.add("LeftThumb", zoomOutHID = sweHelper.createQuantity()
                .label("Left Thumb")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        inputData.add("RightThumb", zoomInHID = sweHelper.createQuantity()
                .label("Right Thumb")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
//        inputData.add("Minus", zoomOutWii = sweHelper.createQuantity()
//                .label("Minus")
//                .uomUri(SWEConstants.UOM_UNITLESS)
//                .build());
//        inputData.add("Plus", zoomInWii = sweHelper.createQuantity()
//                .label("Plus")
//                .uomUri(SWEConstants.UOM_UNITLESS)
//                .build());
        inputData.add("B", zoomReset = sweHelper.createQuantity()
                .label("B")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());

        // outputs
        outputData.add("ptz", ptzOutput = sweHelper.createRecord()
                .addField("pan", sweHelper.createQuantity()
                        .definition(SWEHelper.getPropertyUri("PanAngle"))
                        .label("Pan")
                        .uomCode("deg")
                        .dataType(DataType.FLOAT)
                        //.value(ptzInput.getField("pan").getData().getFloatValue()))
                )
                .addField("tilt", sweHelper.createQuantity()
                        .definition(SWEHelper.getPropertyUri("TiltAngle"))
                        .label("Tilt")
                        .uomCode("deg")
                        .dataType(DataType.FLOAT)
                        //.value(ptzInput.getField("tilt").getData().getFloatValue()))
                )
                .addField("zoom", sweHelper.createQuantity()
                        .definition(SWEHelper.getPropertyUri("ZoomFactor"))
                        .label("Zoom Factor")
                        .uomCode("1")
                        .dataType(DataType.SHORT)
                        //.value(ptzInput.getField("zoom").getData().getShortValue()))
                )
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
            
            // Zoom in/out button isPressed values for HID and Wii controllers
            isLeftPressed = zoomOutHID.getValue() == 1.0f;
            isRightPressed = zoomInHID.getValue() == 1.0f;
//            isMinusPressed = zoomOutWii.getValue() == 1.0f;
//            isPlusPressed = zoomInWii.getValue() == 1.0f;
            
            // Zoom reset button isPressed values for HID and Wii controllers
            isBPressed = zoomReset.getValue() == 1.0f;

            if(curDpadValue != 0 || isLeftPressed || isRightPressed || isBPressed) {
                curPan = ptzOutput.getField("pan").getData().getFloatValue();
                curTilt = ptzOutput.getField("tilt").getData().getFloatValue();
                curZoom = ptzOutput.getField("zoom").getData().getShortValue();

                // Zoom in or out incrementally based on whether buttons are pressed
                if(isLeftPressed || isMinusPressed) {
                    newZoom -= 50;
                } else if(isRightPressed || isPlusPressed) {
                    newZoom += 50;
                }

                if(isBPressed) {
                    newZoom = 0;
                }
                
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
                    newPan = Math.max(newPan, -180.0f);
                }

                if(newTilt >= 180) {
                    newTilt = Math.min(newTilt, 180.0f);
                } else if(newTilt <= 0) {
                    newTilt = Math.max(newTilt, 0.0f);
                }
                
                if(newZoom >= 10909) {
                    newZoom = Math.min(newZoom, 10909);
                } else if(newZoom <= 0) {
                    newZoom = 0;
                }

                getLogger().debug("New PTZ = [{},{},{}]", newPan, newTilt, newZoom);

                ptzOutput.getData().setFloatValue(0, newPan);
                ptzOutput.getData().setFloatValue(1, newTilt);
                ptzOutput.getData().setFloatValue(2, (short) newZoom);
            }
        } catch (Exception e) {
            reportError("Error computing PTZ position");
        }
    }
}