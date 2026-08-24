package dev.flutter.netbeans.plugin.device;

import dev.flutter.netbeans.run.AndroidAvdCreateRequest;
import dev.flutter.netbeans.run.AndroidDeviceDefinition;
import dev.flutter.netbeans.run.AndroidSystemImage;
import java.io.IOException;
import java.util.List;
import java.util.Set;

interface AndroidDeviceManagerBackend extends AutoCloseable {
    DeviceManagerInventory refresh() throws IOException, InterruptedException;

    CreationOptions creationOptions() throws IOException, InterruptedException;

    void create(AndroidAvdCreateRequest request) throws IOException, InterruptedException;

    String start(String avdId) throws IOException, InterruptedException;

    String stop(String avdId) throws IOException, InterruptedException;

    String restart(String avdId) throws IOException, InterruptedException;

    String wipe(String avdId) throws IOException, InterruptedException;

    String delete(String avdId) throws IOException, InterruptedException;

    String selectTarget(DeviceManagerSelection selection) throws IOException, InterruptedException;

    @Override
    void close();

    record CreationOptions(
            List<AndroidSystemImage> systemImages,
            List<AndroidDeviceDefinition> deviceDefinitions,
            Set<String> existingAvdIds) {
        public CreationOptions {
            systemImages = List.copyOf(systemImages);
            deviceDefinitions = List.copyOf(deviceDefinitions);
            existingAvdIds = Set.copyOf(existingAvdIds);
        }
    }
}
