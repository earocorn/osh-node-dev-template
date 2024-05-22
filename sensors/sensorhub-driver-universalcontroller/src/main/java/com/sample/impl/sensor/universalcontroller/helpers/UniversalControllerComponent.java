package com.sample.impl.sensor.universalcontroller.helpers;

public enum UniversalControllerComponent {
    A_BUTTON("A"),
    B_BUTTON("B"),
    X_BUTTON("X"),
    Y_BUTTON("Y"),
    LEFT_TRIGGER("LeftThumb"),
    RIGHT_TRIGGER("RightThumb"),
    X_AXIS("x"),
    Y_AXIS("y"),
    Z_AXIS("z"),
    RX_AXIS("rx"),
    RY_AXIS("ry"),
    RZ_AXIS("rz");
    // TODO: add the rest
    private String componentName;

    UniversalControllerComponent(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }
}
