package org.sensorhub.process.gamepadptz.clean;

import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import org.sensorhub.process.gamepadptz.helpers.AbstractControllerTaskingProcess;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;

public class ControllerPTZProcess extends AbstractControllerTaskingProcess {

    public static final OSHProcessInfo INFO = new OSHProcessInfo("universalcontrollerptz", "Universal Controller PTZ Process", null, ControllerPTZProcess.class);
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
    public ControllerPTZProcess() {
        super(INFO);

        outputData.add("rpan", rPanOutput = fac.createQuantity()
                .dataType(DataType.FLOAT)
                .definition(SWEHelper.getPropertyUri("RelativePan"))
                .label("Relative Pan")
                .uomCode("deg")
                .build());
        outputData.add("rtilt", rTiltOutput = fac.createQuantity()
                .dataType(DataType.FLOAT)
                .definition(SWEHelper.getPropertyUri("RelativeTilt"))
                .label("Relative Tilt")
                .uomCode("deg")
                .build());
        outputData.add("rzoom", rZoomOutput = fac.createQuantity()
                .dataType(DataType.FLOAT)
                .definition(SWEHelper.getPropertyUri("RelativeZoomFactor"))
                .label("Relative Zoom Factor")
                .uomCode("1")
                .build());

        // TODO: i accept constructive criticism
        outputData.add("sensitivity", sensitivityOutput = fac.createQuantity()
                .definition(SWEHelper.getPropertyUri("JoystickSensitivity"))
                .dataType(DataType.INT)
                .value(1)
                .label("Sensitivity")
                .build());
    }

    @Override
    public void updateOutputs() throws ProcessException {
        try {
            curXValue = fac.getComponentValueInput("x");
            System.out.println("CURXVAL = " + curXValue);
            curYValue = fac.getComponentValueInput("y");

            // Zoom in/out button isPressed values for HID and Wii controllers
            isLeftPressed = fac.getComponentValueInput("LeftThumb") == 1.0f;
            isRightPressed = fac.getComponentValueInput("RightThumb") == 1.0f;

            // Sensitivity determined by dpadInput up and down on scale of 1-10
            if(fac.getComponentValueInput("pov") == 0.25f) {
                if(sensitivityOutput.getData().getIntValue() < 10) {
                    sensitivityOutput.getData().setIntValue(sensitivityOutput.getData().getIntValue() + 1);
                }
            } else if(fac.getComponentValueInput("pov") == 0.75f) {
                if(sensitivityOutput.getData().getIntValue() > 1) {
                    sensitivityOutput.getData().setIntValue(sensitivityOutput.getData().getIntValue() - 1);
                }
            }

            newPan = curXValue * sensitivityOutput.getData().getIntValue() * 5;

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
        // TODO: Use helper to get component data and set outputs
    }

}
