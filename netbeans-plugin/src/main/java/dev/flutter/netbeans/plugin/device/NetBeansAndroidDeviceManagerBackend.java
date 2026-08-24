package dev.flutter.netbeans.plugin.device;

import dev.flutter.netbeans.api.FlutterDevice;
import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.plugin.project.FlutterProject;
import dev.flutter.netbeans.plugin.project.FlutterRunController;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainService;
import dev.flutter.netbeans.run.AndroidAvd;
import dev.flutter.netbeans.run.AndroidAvdCreateRequest;
import dev.flutter.netbeans.run.AndroidAvdService;
import dev.flutter.netbeans.run.AndroidConnectedDevice;
import dev.flutter.netbeans.run.AndroidDeviceDefinition;
import dev.flutter.netbeans.run.AndroidEmulatorProcess;
import dev.flutter.netbeans.run.AndroidSdkAvdService;
import dev.flutter.netbeans.run.AndroidSdkDiscovery;
import dev.flutter.netbeans.run.AndroidSdkInstallation;
import dev.flutter.netbeans.run.AndroidSdkTool;
import dev.flutter.netbeans.run.AndroidSystemImage;
import dev.flutter.netbeans.sdk.FlutterCli;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/** Production bridge from the NetBeans window to the headless Android SDK services. */
final class NetBeansAndroidDeviceManagerBackend implements AndroidDeviceManagerBackend {
    private static final Duration BOOT_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration FLUTTER_TARGET_TIMEOUT = Duration.ofSeconds(30);

    private final FlutterToolchainService toolchains;
    private final Supplier<FlutterProject> activeProject;
    private AndroidSdkInstallation installation;
    private AndroidAvdService service;

    NetBeansAndroidDeviceManagerBackend() {
        this(new FlutterToolchainService(), new ActiveFlutterProjectResolver());
    }

    NetBeansAndroidDeviceManagerBackend(
            FlutterToolchainService toolchains,
            Supplier<FlutterProject> activeProject) {
        this.toolchains = java.util.Objects.requireNonNull(toolchains, "toolchains");
        this.activeProject = java.util.Objects.requireNonNull(activeProject, "activeProject");
    }

    @Override
    public synchronized DeviceManagerInventory refresh()
            throws IOException, InterruptedException {
        Optional<FlutterSdk> flutter = toolchains.resolve().flutterSdk();
        Optional<AndroidSdkInstallation> detected = flutter.isPresent()
                ? new AndroidSdkDiscovery(new FlutterCli(flutter.orElseThrow())).detect()
                : new AndroidSdkDiscovery().detect();
        if (detected.isEmpty()) {
            replaceService(null);
            return new DeviceManagerInventory(
                    AndroidToolchainStatus.unavailable(
                            "Android SDK was not detected from android.sdk, ANDROID_SDK_ROOT, "
                            + "ANDROID_HOME, Flutter config, or the platform default location."),
                    List.of(),
                    List.of(),
                    selectedTargetId(),
                    "Configure Android SDK command-line tools, then use Refresh.");
        }

        AndroidSdkInstallation sdk = detected.orElseThrow();
        ensureService(sdk);
        List<String> warnings = new ArrayList<>();
        List<AndroidVirtualDevice> avds;
        try {
            avds = service.listAvds().stream()
                    .map(NetBeansAndroidDeviceManagerBackend::toUiAvd)
                    .sorted(Comparator.comparing(
                            AndroidVirtualDevice::displayName,
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException ex) {
            avds = List.of();
            warnings.add(messageOf(ex));
        }

        List<ConnectedAndroidDevice> connected;
        try {
            connected = service.listConnectedDevices().stream()
                    .map(NetBeansAndroidDeviceManagerBackend::toUiDevice)
                    .sorted(Comparator.comparing(
                            ConnectedAndroidDevice::displayName,
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException ex) {
            connected = List.of();
            warnings.add(messageOf(ex));
        }

        AndroidToolchainStatus status = toolchainStatus(sdk);
        String message = warnings.isEmpty()
                ? inventoryMessage(connected, avds)
                : "Android Device Manager refreshed with warnings: " + String.join("; ", warnings);
        return new DeviceManagerInventory(
                status, connected, avds, selectedTargetId(), message);
    }

    @Override
    public synchronized CreationOptions creationOptions()
            throws IOException, InterruptedException {
        AndroidAvdService current = requireService("prepare Android AVD creation");
        List<AndroidSystemImage> images = current.listSystemImages().stream()
                .filter(AndroidSystemImage::installed)
                .sorted(Comparator
                        .comparingInt((AndroidSystemImage image) ->
                                image.numericApiLevel().orElse(-1)).reversed()
                        .thenComparing(AndroidSystemImage::tag, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AndroidSystemImage::abi, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (images.isEmpty()) {
            throw new IOException("Cannot create an Android Virtual Device: the Android SDK at "
                    + installation.root() + " has no installed system image. Install a stable "
                    + "system-images package with Android SDK Manager and accept its license first.");
        }
        List<AndroidDeviceDefinition> definitions = current.listDeviceDefinitions().stream()
                .sorted(Comparator.comparing(
                        AndroidDeviceDefinition::displayName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        Set<String> ids = new HashSet<>();
        current.listAvds().forEach(avd -> ids.add(avd.id().toLowerCase(Locale.ROOT)));
        return new CreationOptions(images, definitions, ids);
    }

    @Override
    public synchronized void create(AndroidAvdCreateRequest request)
            throws IOException, InterruptedException {
        requireService("create Android AVD").create(request);
    }

    @Override
    public synchronized String start(String avdId) throws IOException, InterruptedException {
        AndroidAvdService current = requireService("start Android AVD");
        detach(current.start(avdId));
        AndroidConnectedDevice device = current.waitForBoot(avdId, BOOT_TIMEOUT);
        return "Started Android Virtual Device '" + avdId + "' as " + device.id() + ". "
                + selectAfterLifecycle(device.id());
    }

    @Override
    public synchronized String stop(String avdId) throws IOException, InterruptedException {
        requireService("stop Android AVD").stop(avdId);
        return "Stopped Android Virtual Device '" + avdId + "'.";
    }

    @Override
    public synchronized String restart(String avdId) throws IOException, InterruptedException {
        AndroidAvdService current = requireService("restart Android AVD");
        detach(current.restart(avdId));
        AndroidConnectedDevice device = current.waitForBoot(avdId, BOOT_TIMEOUT);
        return "Restarted Android Virtual Device '" + avdId + "' as " + device.id() + ". "
                + selectAfterLifecycle(device.id());
    }

    @Override
    public synchronized String wipe(String avdId) throws IOException, InterruptedException {
        AndroidAvdService current = requireService("wipe Android AVD user data");
        detach(current.wipe(avdId));
        AndroidConnectedDevice device = current.waitForBoot(avdId, BOOT_TIMEOUT);
        return "Wiped user data for Android Virtual Device '" + avdId
                + "' and started it as " + device.id() + ". "
                + selectAfterLifecycle(device.id());
    }

    @Override
    public synchronized String delete(String avdId) throws IOException, InterruptedException {
        requireService("delete Android AVD").delete(avdId);
        return "Deleted Android Virtual Device '" + avdId + "'.";
    }

    @Override
    public synchronized String selectTarget(DeviceManagerSelection selection)
            throws IOException, InterruptedException {
        java.util.Objects.requireNonNull(selection, "selection");
        String deviceId = switch (selection.kind()) {
            case CONNECTED_DEVICE -> selection.id();
            case AVD -> requireService("select Android AVD as Flutter target").listAvds().stream()
                    .filter(avd -> avd.id().equals(selection.id()))
                    .findFirst()
                    .flatMap(AndroidAvd::connectedDeviceId)
                    .orElseThrow(() -> new IOException("Cannot select Android Virtual Device '"
                            + selection.id() + "': ADB does not report an online device id."));
            case NONE -> throw new IOException(
                    "Cannot select a Flutter target: no Android device or AVD is selected.");
        };
        return selectExactFlutterTarget(deviceId, false);
    }

    private String selectExactFlutterTarget(String deviceId, boolean wait)
            throws IOException, InterruptedException {
        FlutterProject project = activeProject.get();
        if (project == null) {
            return "No Flutter target was selected because no open Flutter project is active.";
        }
        FlutterRunController controller = project.getLookup().lookup(FlutterRunController.class);
        if (controller == null) {
            throw new IOException("Cannot select Android device '" + deviceId
                    + "' for Flutter project " + project.getProjectDirectory().getPath()
                    + ": run support is unavailable.");
        }

        long timeoutNanos = wait ? FLUTTER_TARGET_TIMEOUT.toNanos() : 0;
        long deadline = System.nanoTime() + timeoutNanos;
        IOException lastFailure = null;
        do {
            try {
                Optional<FlutterDevice> exact = controller.discoverTargets().stream()
                        .filter(device -> device.id().equals(deviceId))
                        .findFirst();
                if (exact.isPresent()) {
                    controller.selectTarget(exact.orElseThrow());
                    return "Selected " + exact.orElseThrow().name() + " (" + deviceId
                            + ") for Flutter project "
                            + project.getProjectDirectory().getPath() + ".";
                }
            } catch (IOException ex) {
                lastFailure = ex;
            }
            if (!wait || System.nanoTime() >= deadline) {
                break;
            }
            Thread.sleep(750);
        } while (true);

        String cause = lastFailure == null
                ? "Flutter did not report that exact device"
                : messageOf(lastFailure);
        throw new IOException("Cannot select Android device '" + deviceId
                + "' for Flutter project " + project.getProjectDirectory().getPath()
                + ": " + cause + ".");
    }

    private String selectAfterLifecycle(String deviceId) throws InterruptedException {
        try {
            return selectExactFlutterTarget(deviceId, true);
        } catch (IOException selectionFailure) {
            return "The emulator is ready, but its Flutter toolbar target was not selected: "
                    + messageOf(selectionFailure);
        }
    }

    private void ensureService(AndroidSdkInstallation sdk) {
        if (installation != null && installation.root().equals(sdk.root()) && service != null) {
            installation = sdk;
            return;
        }
        replaceService(sdk);
    }

    private void replaceService(AndroidSdkInstallation sdk) {
        if (service != null) {
            service.close();
        }
        installation = sdk;
        service = sdk == null ? null : new AndroidSdkAvdService(sdk);
    }

    private AndroidAvdService requireService(String operation) throws IOException {
        if (service == null || installation == null) {
            throw new IOException("Cannot " + operation
                    + ": Android SDK discovery has not completed successfully. Use Refresh first.");
        }
        return service;
    }

    private String selectedTargetId() {
        FlutterProject project = activeProject.get();
        if (project == null) {
            return "";
        }
        FlutterRunController controller = project.getLookup().lookup(FlutterRunController.class);
        return controller == null ? "" : controller.selectedTargetId();
    }

    private static AndroidToolchainStatus toolchainStatus(AndroidSdkInstallation sdk) {
        AndroidToolchainStatus.State state = sdk.complete()
                ? AndroidToolchainStatus.State.READY
                : AndroidToolchainStatus.State.INCOMPLETE;
        return new AndroidToolchainStatus(
                state,
                sdk.root().toString(),
                toolState(sdk, AndroidSdkTool.ADB),
                toolState(sdk, AndroidSdkTool.EMULATOR),
                toolState(sdk, AndroidSdkTool.AVD_MANAGER),
                toolState(sdk, AndroidSdkTool.SDK_MANAGER),
                "Detected from " + sdk.source() + ".");
    }

    private static AndroidToolchainStatus.ToolState toolState(
            AndroidSdkInstallation sdk,
            AndroidSdkTool tool) {
        return sdk.executable(tool).isPresent()
                ? AndroidToolchainStatus.ToolState.AVAILABLE
                : AndroidToolchainStatus.ToolState.MISSING;
    }

    private static ConnectedAndroidDevice toUiDevice(AndroidConnectedDevice device) {
        String detail = combine(device.detail(), device.error().orElse(""));
        return new ConnectedAndroidDevice(
                device.id(),
                device.displayName(),
                "",
                "",
                device.emulator(),
                ConnectedAndroidDevice.State.valueOf(device.state().name()),
                detail);
    }

    private static AndroidVirtualDevice toUiAvd(AndroidAvd avd) {
        String detail = combine(avd.detail(), avd.error().orElse(""));
        return new AndroidVirtualDevice(
                avd.id(),
                avd.displayName(),
                avd.deviceProfile(),
                avd.apiLevel().equals("unknown") ? "" : avd.apiLevel(),
                avd.abi().equals("unknown") ? "" : avd.abi(),
                AndroidVirtualDevice.State.valueOf(avd.state().name()),
                avd.connectedDeviceId().orElse(""),
                detail);
    }

    private static String inventoryMessage(
            List<ConnectedAndroidDevice> devices,
            List<AndroidVirtualDevice> avds) {
        return "Found " + devices.size() + " connected Android device"
                + (devices.size() == 1 ? "" : "s") + " and " + avds.size()
                + " Android Virtual Device" + (avds.size() == 1 ? "" : "s") + ".";
    }

    private static void detach(AndroidEmulatorProcess process) {
        if (process != null) {
            process.close();
        }
    }

    private static String combine(String first, String second) {
        String left = first == null ? "" : first.strip();
        String right = second == null ? "" : second.strip();
        if (left.isBlank()) return right;
        if (right.isBlank() || left.contains(right)) return left;
        return left + "; " + right;
    }

    private static String messageOf(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message.strip();
    }

    @Override
    public synchronized void close() {
        replaceService(null);
    }
}
