package org.sensorhub.process.gamepadptz;

import net.opengis.swe.v20.*;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockMixed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;

import java.util.Objects;

public class GamepadProcessChain extends ExecutableProcessImpl {
    public static final OSHProcessInfo INFO = new OSHProcessInfo("gamepadchain", "Gamepad Process Chain", null, GamepadProcessChain.class);

    // Inputs

    // Can use index or each gamepad has isPrimaryController value
//    private Count primaryControllerIndexInput;
    private DataArray gamepadsInput;
    private Count gamepadCountInput;

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

        inputData.add("numGamepads", gamepadCountInput = createGamepadCount());
        inputData.add("gamepads", gamepadsInput = createGamepadArray());

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
        getLogger().debug("Completed constructor");
    }

    public Count createGamepadCount() {
        SWEHelper sweFactory = new SWEHelper();
        return sweFactory.createCount()
                .name("numGamepads")
                .label("Num Gamepads")
                .description("Number of connected gamepads")
                .definition(SWEHelper.getPropertyUri("GamepadCount"))
                .id("numGamepads").build();
    }

    public DataArray createGamepadArray() {
        SWEHelper sweFactory = new SWEHelper();
        return sweFactory.createArray()
                .name("gamepads")
                .label("Gamepads")
                .description("List of connected gamepads.")
                .definition(SWEHelper.getPropertyUri("GamepadArray"))
                .withVariableSize("numGamepads")
                .withElement("gamepad", sweFactory.createRecord()
                        .label("Gamepad")
                        .description("Gamepad Data")
                        .definition(SWEHelper.getPropertyUri("Gamepad"))
                        .addField("gamepadName", sweFactory.createText()
                                .label("Gamepad Name")
                                .definition("GamepadName"))
                        .addField("isPrimaryController", sweFactory.createBoolean()
                                .label("Is Primary Controller")
                                .definition(SWEHelper.getPropertyUri("IsPrimaryController"))
                                .value(false))
                        .addField("numComponents", sweFactory.createCount()
                                .label("Num Components")
                                .description("Number of button and axis components on gamepad")
                                .definition(SWEHelper.getPropertyUri("NumGamepadComponents"))
                                .id("numComponents")
                                .build())
                        .addField("gamepadComponents", sweFactory.createArray()
                                .name("gamepadComponents")
                                .label("Gamepad Components")
                                .description("Data of Connected Gamepad Components")
                                .definition(SWEHelper.getPropertyUri("GamepadComponentArray"))
                                .withVariableSize("numComponents")
                                .withElement("component", sweFactory.createRecord()
                                        .name("component")
                                        .label("Component")
                                        .description("Gamepad Component (A button, B button, X axis, etc.)")
                                        .definition(SWEHelper.getPropertyUri("GamepadComponent"))
                                        .addField("componentName", sweFactory.createText()
                                                .label("Component Name")
                                                .description("Name of component")
                                                .definition(SWEHelper.getPropertyUri("ComponentName"))
                                                .value(""))
                                        .addField("componentValue", sweFactory.createQuantity()
                                                .label("Component Value")
                                                .description("Value of component")
                                                .definition(SWEHelper.getPropertyUri("ComponentValue"))
                                                .dataType(DataType.FLOAT)
                                                .value(0.0f)
                                                .addAllowedInterval(-1.0f, 1.0f)))
                        )).build();
    }

    @Override
    public void init() throws ProcessException {
        super.init();
    }

    @Override
    public void execute() throws ProcessException {
        System.out.println("Executing gamepadchain");
        reportError("num and isPrimary" + numGamepads + " : " + gamepadsInput.getComponent("isPrimaryController").getData().getBooleanValue());
        reportError("vals " + povValue + ", " + xValue + ", " + yValue + ", " + leftValue + "," + rightValue);
        try {

            gamepadCountInput = createGamepadCount();
            DataBlock gamepadCountBlock = gamepadCountInput.createDataBlock();
            gamepadCountInput.setData(gamepadCountBlock);

            gamepadsInput = createGamepadArray();
            DataBlock gamepadsArrayBlock = gamepadsInput.createDataBlock();
            gamepadsInput.setData(gamepadsArrayBlock);

            if (gamepadsInput.getComponentCount() > 0) {
                povValue = 0.0f;
                xValue = 0.0f;
                yValue = 0.0f;
                leftValue = 0.0f;
                rightValue = 0.0f;
                // parse gamepads, extract values for pov, x, y, LT, RT, set output values to primary controller's values
                DataArrayImpl gamepadsArray = (DataArrayImpl) gamepadsDataBlock;
                gamepadsArray.updateSize();
                gamepadsDataBlock.updateAtomCount();

                getLogger().debug("PROCESS: Number of gamepads = " + numGamepads);

                if (numGamepads > 0) {
                    for (int i = 0; i < numGamepads; i++) {
                        DataComponent gamepad = (DataComponent) ((DataBlockMixed) gamepadsDataBlock).getUnderlyingObject()[i];
                        DataComponent componentArray = gamepad.getComponent("gamepadComponents");
                        for (int j = 0; j < componentArray.getComponentCount(); j++) {
                            if(Objects.equals(componentArray.getComponent(j).getComponent("componentName").getData().getStringValue(), "pov")) {
                                povValue = componentArray.getComponent(j).getComponent("componentValue").getData().getFloatValue();
                            }
                            if(Objects.equals(componentArray.getComponent(j).getComponent("componentName").getData().getStringValue(), "x")) {
                                xValue = componentArray.getComponent(j).getComponent("componentValue").getData().getFloatValue();
                            }
                            if(Objects.equals(componentArray.getComponent(j).getComponent("componentName").getData().getStringValue(), "y")) {
                                yValue = componentArray.getComponent(j).getComponent("componentValue").getData().getFloatValue();
                            }
                            if(Objects.equals(componentArray.getComponent(j).getComponent("componentName").getData().getStringValue(), "LeftThumb")) {
                                leftValue = componentArray.getComponent(j).getComponent("componentValue").getData().getFloatValue();
                            }
                            if(Objects.equals(componentArray.getComponent(j).getComponent("componentName").getData().getStringValue(), "RightThumb")) {
                                rightValue = componentArray.getComponent(j).getComponent("componentValue").getData().getFloatValue();
                            }
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
