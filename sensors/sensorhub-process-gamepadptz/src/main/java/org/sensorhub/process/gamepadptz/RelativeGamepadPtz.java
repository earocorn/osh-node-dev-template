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
 *
 * </p>
 *
 * @author Alex Almanza
 * @date May 6, 2024
 */
public class RelativeGamepadPtz extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("gamepadPTZ", "Gamepad PTZ Process", null, RelativeGamepadPtz.class);
    private Quantity dpad;
    private Quantity zoomInHID;
    private Quantity zoomOutHID;
    private Quantity zoomReset;
    private Quantity rPanOutput;
    private Quantity rTiltOutput;
    private Quantity rZoomOutput;
    float newPan = 0, newTilt = 0;
    float newZoom = 0;
    double curDpadValue = 0;
    boolean isLeftPressed = false;
    boolean isRightPressed = false;
    boolean isMinusPressed = false;
    boolean isPlusPressed = false;
    boolean isBPressed = false;
    boolean isInOutMatched = false;

    public RelativeGamepadPtz()
    {
        super(INFO);

        SWEHelper sweHelper = new SWEHelper();

        // TODO: add different input to get current PTZ position

        // inputs
        inputData.add("pov", dpad = sweHelper.createQuantity()
                .label("Hat Switch")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        inputData.add("Left Thumb", zoomOutHID = sweHelper.createQuantity()
                .label("Left Thumb")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        inputData.add("Right Thumb", zoomInHID = sweHelper.createQuantity()
                .label("Right Thumb")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        inputData.add("B", zoomReset = sweHelper.createQuantity()
                .label("B")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());

        // outputs
        outputData.add("rpan", rPanOutput = sweHelper.createQuantity()
                .dataType(DataType.FLOAT)
                .definition(SWEHelper.getPropertyUri("RelativePan"))
                .label("Relative Pan")
                .uomCode("deg")
                .build());
        outputData.add("rtilt", rTiltOutput = sweHelper.createQuantity()
                .dataType(DataType.FLOAT)
                .definition(SWEHelper.getPropertyUri("RelativeTilt"))
                .label("Relative Tilt")
                .uomCode("deg")
                .build());
        outputData.add("rzoom", rZoomOutput = sweHelper.createQuantity()
                .dataType(DataType.FLOAT)
                .definition(SWEHelper.getPropertyUri("RelativeZoom"))
                .label("Relative Zoom")
                .uomCode("deg")
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