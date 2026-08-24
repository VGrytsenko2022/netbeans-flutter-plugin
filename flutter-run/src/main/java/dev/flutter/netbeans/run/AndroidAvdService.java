package dev.flutter.netbeans.run;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/** Headless Android SDK and AVD operations used by an IDE-facing controller. */
public interface AndroidAvdService extends AutoCloseable {
    List<AndroidAvd> listAvds() throws IOException, InterruptedException;

    List<AndroidSystemImage> listSystemImages() throws IOException, InterruptedException;

    List<AndroidDeviceDefinition> listDeviceDefinitions() throws IOException, InterruptedException;

    List<AndroidConnectedDevice> listConnectedDevices() throws IOException, InterruptedException;

    /** Waits for an AVD to finish booting without owning or stopping its emulator process. */
    AndroidConnectedDevice waitForBoot(String avdId, Duration timeout)
            throws IOException, InterruptedException;

    AndroidAvd create(AndroidAvdCreateRequest request) throws IOException, InterruptedException;

    AndroidEmulatorProcess start(String avdId) throws IOException, InterruptedException;

    void stop(String avdId) throws IOException, InterruptedException;

    AndroidEmulatorProcess restart(String avdId) throws IOException, InterruptedException;

    /** Wipes user data using the official emulator option and starts the reset AVD. */
    AndroidEmulatorProcess wipe(String avdId) throws IOException, InterruptedException;

    void delete(String avdId) throws IOException, InterruptedException;

    @Override
    void close();
}
