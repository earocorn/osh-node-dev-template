package org.sensorhub.process.gamepadptz;

import net.opengis.swe.v20.*;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;

public class GamepadProcessChain extends ExecutableProcessImpl {
    public static final OSHProcessInfo INFO = new OSHProcessInfo("gamepadchain", "Gamepad Process Chain", null, GamepadProcessChain.class);

    // Inputs

    // Can use index or each gamepad has isPrimaryController value
//    private Count primaryControllerIndexInput;
    private DataArray gamepadsInput;

    // Outputs
    private Quantity dpadOutput;
    private Quantity xAxisOutput;
    private Quantity yAxisOutput;
    private Quantity leftOutput;
    private Quantity rightOutput;

    // vars
    DataBlock gamepadsDataBlock;
    DataBlock latestRecord;
    int numGamepads;
    float povValue;
    float xValue;
    float yValue;
    float leftValue;
    float rightValue;

    public GamepadProcessChain() {
        super(INFO);

        SWEHelper sweHelper = new SWEHelper();

        inputData.add("gamepads", gamepadsInput = sweHelper.createArray()
                .label("Gamepads")
                .description("List of connected gamepads.")
                .updatable(true)
                //.addSamplingTimeIsoUTC("sampleTime")
                .build());

        outputData.add("pov", dpadOutput = sweHelper.createQuantity()
                .label("Hat Switch")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        outputData.add("x", xAxisOutput = sweHelper.createQuantity()
                .label("x")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        outputData.add("y", yAxisOutput = sweHelper.createQuantity()
                .label("y")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        outputData.add("LeftThumb", leftOutput = sweHelper.createQuantity()
                .label("Left Thumb")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
        outputData.add("RightThumb", rightOutput = sweHelper.createQuantity()
                .label("Right Thumb")
                .uomUri(SWEConstants.UOM_UNITLESS)
                .build());
    }

    @Override
    public void init() throws ProcessException {
        super.init();
    }

    @Override
    public void execute() throws ProcessException {
        try {
            if (gamepadsInput.getComponentCount() > 0) {
                povValue = 0.0f;
                xValue = 0.0f;
                yValue = 0.0f;
                leftValue = 0.0f;
                rightValue = 0.0f;
                // parse gamepads, extract values for pov, x, y, LT, RT, set output values to primary controller's values
                if (latestRecord == null) {
                    gamepadsDataBlock = gamepadsInput.createDataBlock();
                } else {
                    gamepadsDataBlock = latestRecord.renew();
                }

                numGamepads = ((DataBlockMixed) gamepadsDataBlock).getUnderlyingObject().length;
                getLogger().debug("PROCESS: Number of gamepads = " + numGamepads);

                if (numGamepads > 0) {
                    for (int i = 0; i < numGamepads; i++) {
                        DataComponent gamepad = (DataComponent) ((DataBlockMixed) gamepadsDataBlock).getUnderlyingObject()[i];
                        if (gamepad.getComponent("isPrimaryController").getData().getBooleanValue()) {
                            povValue = gamepad.getComponent("pov").getData().getFloatValue();
                            xValue = gamepad.getComponent("x").getData().getFloatValue();
                            yValue = gamepad.getComponent("y").getData().getFloatValue();
                            leftValue = gamepad.getComponent("LeftThumb").getData().getFloatValue();
                            rightValue = gamepad.getComponent("RightThumb").getData().getFloatValue();
                        }
                    }

                }

                dpadOutput.getData().setFloatValue(povValue);
                xAxisOutput.getData().setFloatValue(xValue);
                yAxisOutput.getData().setFloatValue(yValue);
                leftOutput.getData().setFloatValue(leftValue);
                rightOutput.getData().setFloatValue(rightValue);

                latestRecord = gamepadsDataBlock;
            }
        } catch (Exception e) {
            reportError(e.getMessage());
            reportError("Error retrieving gamepad data");
        }
    }
}
