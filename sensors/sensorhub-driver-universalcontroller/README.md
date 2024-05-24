# Universal Controller Driver

Sensor adapter for HID Compliant gamepads, Wii Remotes w/ or w/o nunchuk extension.

## Configuration

Configuring the sensor requires:
Select ```Sensors``` from the left hand accordion control and right click for context sensitive menu in accordion control
- **Module Name:** A name for the instance of the driver
- **Serial Number:** The platforms serial number, or a unique identifier
- **Auto Start:** Check the box to start this module when OSH node is launched

## Supported Platforms

The Universal Controller Driver has the most controller support on Linux machines or 32-bit Raspberry Pi operating systems/

## Common Issues and Fixes

 - `BlueCove native library version mismatch` or `BlueCove library bluecove not available`
   - To solve this, replace the `bluecove-2.1.1-SNAPSHOT.jar` and `bluecove-gpl-2.1.1-SNAPSHOT.jar` with older version from the `altlibs` folder
 - Driver is unable to find HID gamepads with the GAMEPAD setting
   - Ensure that the following argument is included in your `launch.sh` file:
   - `-Djava.library.path="./jinputlibs"` or `-Djava.library.path="./armlib"` (for Raspberry Pi)