package io.github.vgrytsenko2022.run;

/** State reported by ADB for a connected Android device. */
public enum AndroidConnectedDeviceState {
    ONLINE,
    BOOTING,
    OFFLINE,
    UNAUTHORIZED,
    DISCONNECTED,
    ERROR
}
