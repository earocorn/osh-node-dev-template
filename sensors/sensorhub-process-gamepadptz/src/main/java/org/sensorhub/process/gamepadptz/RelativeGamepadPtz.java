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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
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
    private Quantity xAxisInput;
    private Quantity yAxisInput;
    private Quantity dpadInput;
    private Quantity rightInput;
    private Quantity leftInput;
    float curXValue = 0;
    float curYValue = 0;
    private Quantity rPanOutput;
    private Quantity rTiltOutput;
    private Quantity rZoomOutput;
    private Quantity sensitivityOutput;
    float newPan = 0, newTilt = 0;
    float newZoom = 0;
    boolean isLeftPressed = false;
    boolean isRightPressed = false;

    public RelativeGamepadPtz()
    {
        super(INFO);

        SWEHelper sweHelper = new SWEHelper();

        // TODO: add different input to get current PTZ position

        // inputs
        inputData.add("pov", dpadInput = sweHelper.createQuantity()
                .label("Hat Switch")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        inputData.add("x", xAxisInput = sweHelper.createQuantity()
                .label("x")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        inputData.add("y", yAxisInput = sweHelper.createQuantity()
                .label("y")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        inputData.add("LeftThumb", leftInput = sweHelper.createQuantity()
                .label("Left Thumb")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        inputData.add("RightThumb", rightInput = sweHelper.createQuantity()
                .label("Right Thumb")
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
                .definition(SWEHelper.getPropertyUri("RelativeZoomFactor"))
                .label("Relative Zoom Factor")
                .uomCode("1")
                .build());

        // TODO: sense is temporary, should be its own field in gamepad output
        outputData.add("sensitivity", sensitivityOutput = sweHelper.createQuantity()
                .definition(SWEHelper.getPropertyUri("JoystickSensitivity"))
                .dataType(DataType.INT)
                .value(1)
                .label("Sensitivity")
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
        System.out.println("Executing rPTZ process");
        try {
            curXValue = xAxisInput.getData().getFloatValue();
            curYValue = yAxisInput.getData().getFloatValue();

            // Zoom in/out button isPressed values for HID and Wii controllers
            isLeftPressed = leftInput.getValue() == 1.0f;
            isRightPressed = rightInput.getValue() == 1.0f;

            // Sensitivity determined by dpadInput up and down on scale of 1-10
            if(dpadInput.getData().getFloatValue() == 0.25f) {
                if(sensitivityOutput.getData().getIntValue() < 10) {
                    sensitivityOutput.getData().setIntValue(sensitivityOutput.getData().getIntValue() + 1);
                }
            } else if(dpadInput.getData().getFloatValue() == 0.75f) {
                if(sensitivityOutput.getData().getIntValue() > 1) {
                    sensitivityOutput.getData().setIntValue(sensitivityOutput.getData().getIntValue() - 1);
                }
            }

            //          1
            newPan = curXValue * sensitivityOutput.getData().getIntValue() * 5;

            //         -1
            newTilt = -(curYValue * sensitivityOutput.getData().getIntValue() * 5);

            // Zoom in or out incrementally based on whether buttons are pressed
            if(isLeftPressed) {
                newZoom = -100 * sensitivityOutput.getData().getIntValue();
            } else if(isRightPressed) {
                newZoom = 100 * sensitivityOutput.getData().getIntValue();
            } else {
                newZoom = 0;
            }

            rPanOutput.getData().setFloatValue(newPan);
            rTiltOutput.getData().setFloatValue(newTilt);
            rZoomOutput.getData().setFloatValue(newZoom);
        } catch (Exception e) {
            reportError("Error computing PTZ position");
        }
    }
}