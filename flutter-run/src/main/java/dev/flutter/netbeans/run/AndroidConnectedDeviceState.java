package dev.flutter.netbeans.run;

/** State reported by ADB for a connected Android device. */
public enum AndroidConnectedDeviceState {
    ONLINE,
    BOOTING,
    OFFLINE,
    UNAUTHORIZED,
    DISCONNECTED,
    ERROR
}
