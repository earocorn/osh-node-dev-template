package com.sample.impl.sensor.gamepad.helpers;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;

public class GamepadHelper extends SWEHelper {

    /**
     * Creates a new DataRecprd object that is based on a controller's input components.
     *
     * @param controller
     * @return a data structure based on the controller components
     */
    public DataRecord newGamepadOutput(String name, Controller controller) {
        SWEBuilders.DataRecordBuilder recordBuilder;
        DataRecord dataRecord;

        recordBuilder = this.createRecord()
                .name(name)
                .addField("sampleTime", this.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"));

        Component[] components = controller.getComponents();

        SWEBuilders.DataRecordBuilder gamepadData;
        gamepadData = this.createRecord();

        for (Component component : components) {
            gamepadData.addField(component.getIdentifier().toString(), this.createQuantity()
                    .label(component.getName()));
        }

        recordBuilder.addField("gamepadData", gamepadData.build())
                .label("Gamepad Data")
                .description("Output data from game controller");

        recordBuilder.addField("action", this.createText()
                .label("Event Action"));

        dataRecord = recordBuilder.build();

        return dataRecord;
    }

}
