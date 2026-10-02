package io.github.vgrytsenko2022.run;

import io.github.vgrytsenko2022.api.ProcessResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Default timeout-bounded implementation backed by Android SDK command-line tools. */
public final class AndroidSdkAvdService implements AndroidAvdService {
    private static final Duration LIST_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration SDK_LIST_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration MUTATION_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration ADB_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_CONFIG_BYTES = 256 * 1024;
    private static final Pattern SAFE_AVD_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final Pattern DEVICE_ID = Pattern.compile(
            "^\\s*id:\\s*\\d+\\s+or\\s+\"([^\"]+)\"\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_DIRECTORY = Pattern.compile(
            "(?:^|[/\\\\])android-([^/\\\\]+)[/\\\\]([^/\\\\]+)[/\\\\]([^/\\\\]+)(?:[/\\\\]|$)");

    private final AndroidSdkInstallation sdk;
    private final AndroidCommandExecutor command;
    private final AndroidProcessStarter processStarter;
    private final Map<String, String> processEnvironment;
    private final ConcurrentHashMap<String, AndroidEmulatorProcess> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AndroidAvdState> transientStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> lastErrors = new ConcurrentHashMap<>();

    public AndroidSdkAvdService(AndroidSdkInstallation sdk) {
        this(sdk, new AndroidProcessSupport(), new AndroidProcessSupport());
    }

    AndroidSdkAvdService(
            AndroidSdkInstallation sdk,
            AndroidCommandExecutor command,
            AndroidProcessStarter processStarter) {
        this.sdk = Objects.requireNonNull(sdk, "sdk");
        this.command = Objects.requireNonNull(command, "command");
        this.processStarter = Objects.requireNonNull(processStarter, "processStarter");
        this.processEnvironment = Map.of(
                "ANDROID_SDK_ROOT", sdk.root().toString(),
                "ANDROID_HOME", sdk.root().toString());
    }

    public AndroidSdkInstallation sdk() {
        return sdk;
    }

    @Override
    public List<AndroidAvd> listAvds() throws IOException, InterruptedException {
        List<AndroidAvd> configured = loadAvdConfigurations();
        ConnectionSnapshot connections = connectedEmulators();
        List<AndroidAvd> snapshots = new ArrayList<>(configured.size());
        for (AndroidAvd avd : configured) {
            snapshots.add(applyRuntime(avd, connections));
        }
        return List.copyOf(snapshots);
    }

    @Override
    public List<AndroidSystemImage> listSystemImages() throws IOException, InterruptedException {
        Path sdkManager = requireTool(AndroidSdkTool.SDK_MANAGER, "list Android system images");
        ProcessResult result = execute(
                sdkManager, SDK_LIST_TIMEOUT, null, List.of("--list", "--channel=0"));
        requireSuccess("list Android system images", null, result);
        return parseSystemImages(result.stdout());
    }

    @Override
    public List<AndroidDeviceDefinition> listDeviceDefinitions()
            throws IOException, InterruptedException {
        Path avdManager = requireTool(AndroidSdkTool.AVD_MANAGER,
                "list Android device definitions");
        ProcessResult result = execute(
                avdManager, LIST_TIMEOUT, null, List.of("list", "device"));
        requireSuccess("list Android device definitions", null, result);
        return parseDeviceDefinitions(result.stdout());
    }

    @Override
    public List<AndroidConnectedDevice> listConnectedDevices()
            throws IOException, InterruptedException {
        Path adb = requireTool(AndroidSdkTool.ADB, "list connected Android devices");
        ProcessResult devices = execute(adb, ADB_TIMEOUT, null, List.of("devices", "-l"));
        requireSuccess("list connected Android devices", null, devices);
        List<AndroidConnectedDevice> result = new ArrayList<>();
        String deviceOutput = devices.stdout() == null ? "" : devices.stdout();
        for (String raw : deviceOutput.lines().toList()) {
            String line = raw.strip();
            if (line.isBlank() || line.startsWith("List of devices attached") || line.startsWith("*")) {
                continue;
            }
            String[] columns = line.split("\\s+", 3);
            if (columns.length < 2) {
                continue;
            }
            String serial = columns[0];
            String adbState = columns[1].toLowerCase(Locale.ROOT);
            Map<String, String> attributes = columns.length < 3
                    ? Map.of() : parseAdbAttributes(columns[2]);
            boolean emulatorDevice = serial.startsWith("emulator-");
            Optional<String> avdName = Optional.empty();
            String mappingError = null;
            if (emulatorDevice) {
                try {
                    ProcessResult name = execute(adb, ADB_TIMEOUT, null,
                            List.of("-s", serial, "emu", "avd", "name"));
                    if (name.success()) {
                        avdName = Optional.ofNullable(firstAvdName(name.stdout()));
                    } else {
                        mappingError = "Unable to resolve the AVD name for " + serial + ": "
                                + failureDetail(name);
                    }
                } catch (IOException ex) {
                    mappingError = "Unable to resolve the AVD name for " + serial + ": "
                            + cleanMessage(ex);
                }
            }

            AndroidConnectedDeviceState state;
            String detail;
            if (adbState.equals("device")) {
                boolean booted = false;
                try {
                    ProcessResult boot = execute(adb, ADB_TIMEOUT, null,
                            List.of("-s", serial, "shell", "getprop", "sys.boot_completed"));
                    booted = boot.success() && boot.stdout() != null
                            && boot.stdout().lines().anyMatch(value -> value.strip().equals("1"));
                    if (!boot.success() && mappingError == null) {
                        mappingError = "Unable to query boot state for " + serial + ": "
                                + failureDetail(boot);
                    }
                } catch (IOException ex) {
                    if (mappingError == null) {
                        mappingError = "Unable to query boot state for " + serial + ": "
                                + cleanMessage(ex);
                    }
                }
                state = booted ? AndroidConnectedDeviceState.ONLINE
                        : AndroidConnectedDeviceState.BOOTING;
                detail = booted ? "Connected and booted" : "Connected; Android is still booting";
            } else if (adbState.equals("offline")) {
                state = AndroidConnectedDeviceState.OFFLINE;
                detail = "ADB reports the device as offline";
            } else if (adbState.equals("unauthorized")) {
                state = AndroidConnectedDeviceState.UNAUTHORIZED;
                detail = "ADB authorization is required on the device";
            } else {
                state = AndroidConnectedDeviceState.ERROR;
                detail = "ADB reports state '" + adbState + "'";
            }
            String model = attributes.getOrDefault("model", "");
            result.add(new AndroidConnectedDevice(
                    serial,
                    model.isBlank() ? serial : model.replace('_', ' '),
                    model,
                    attributes.getOrDefault("product", ""),
                    attributes.getOrDefault("device", ""),
                    emulatorDevice,
                    avdName,
                    state,
                    detail,
                    Optional.ofNullable(mappingError)));
        }
        return List.copyOf(result);
    }

    @Override
    public AndroidConnectedDevice waitForBoot(String avdId, Duration timeout)
            throws IOException, InterruptedException {
        String id = validateAvdId(avdId);
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Android boot timeout must be positive");
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        AndroidConnectedDevice last = null;
        while (System.nanoTime() < deadline) {
            for (AndroidConnectedDevice device : listConnectedDevices()) {
                if (device.avdId().isPresent() && device.avdId().get().equals(id)) {
                    last = device;
                    if (device.state() == AndroidConnectedDeviceState.ONLINE) {
                        transientStates.remove(id);
                        return device;
                    }
                    if (device.state() == AndroidConnectedDeviceState.ERROR
                            || device.state() == AndroidConnectedDeviceState.UNAUTHORIZED) {
                        throw new IOException("Unable to wait for Android AVD '" + id
                                + "' to boot: " + device.detail());
                    }
                }
            }
            AndroidEmulatorProcess process = sessions.get(id);
            if (process != null && !process.isAlive() && process.exitCode().orElse(0) != 0) {
                throw new IOException(lastErrors.getOrDefault(id,
                        "Android Emulator for AVD '" + id + "' exited before boot completed."));
            }
            long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            if (remainingMillis > 0) {
                Thread.sleep(Math.min(500, remainingMillis));
            }
        }
        String lastState = last == null ? "it never appeared in ADB"
                : "its last state was " + last.state().name().toLowerCase(Locale.ROOT);
        throw new IOException("Android AVD '" + id + "' did not finish booting within "
                + timeout + "; " + lastState + ". The emulator remains running.");
    }

    @Override
    public synchronized AndroidAvd create(AndroidAvdCreateRequest request)
            throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request");
        Path avdManager = requireTool(AndroidSdkTool.AVD_MANAGER, "create Android AVD");
        Optional<AndroidAvd> existing = findConfiguration(request.name());
        if (existing.isPresent() && !request.overwriteExisting()) {
            throw new IOException("Unable to create Android AVD '" + request.name()
                    + "': an AVD with that id already exists.");
        }
        if (existing.isPresent()) {
            requireStopped(request.name(), "overwrite Android AVD");
        }
        requireInstalledSystemImage(request.systemImagePackage());

        List<String> arguments = new ArrayList<>(List.of(
                "create", "avd",
                "--name", request.name(),
                "--package", request.systemImagePackage()));
        request.deviceDefinitionId().ifPresent(value -> {
            arguments.add("--device");
            arguments.add(value);
        });
        request.sdCardSize().ifPresent(value -> {
            arguments.add("--sdcard");
            arguments.add(value);
        });
        if (request.overwriteExisting()) {
            arguments.add("--force");
        }
        ProcessResult result = execute(
                avdManager, MUTATION_TIMEOUT, "no" + System.lineSeparator(), arguments);
        requireSuccess("create Android AVD", request.name(), result);
        lastErrors.remove(request.name());
        return findConfiguration(request.name()).orElseGet(() -> avdFromRequest(request));
    }

    @Override
    public synchronized AndroidEmulatorProcess start(String avdId) throws IOException, InterruptedException {
        return startInternal(validateAvdId(avdId), false);
    }

    @Override
    public synchronized void stop(String avdId) throws IOException, InterruptedException {
        String id = validateAvdId(avdId);
        transientStates.put(id, AndroidAvdState.STOPPING);
        AndroidEmulatorProcess owned = sessions.get(id);
        ConnectionSnapshot snapshot = connectedEmulators();
        DeviceConnection connection = snapshot.byAvdId().get(id);
        IOException commandFailure = null;
        try {
            if (connection != null) {
                Path adb = requireTool(AndroidSdkTool.ADB, "stop Android AVD");
                ProcessResult result = execute(adb, ADB_TIMEOUT, null,
                        List.of("-s", connection.serial(), "emu", "kill"));
                try {
                    requireSuccess("stop Android AVD", id, result);
                    waitForDisconnect(connection.serial(), id, STOP_TIMEOUT);
                } catch (IOException ex) {
                    commandFailure = ex;
                }
            } else if (owned == null && snapshot.error() != null) {
                commandFailure = new IOException("Unable to stop Android AVD '" + id
                        + "': " + snapshot.error());
            }
            if (owned != null) {
                sessions.remove(id, owned);
                owned.terminate();
            }
        } finally {
            if (owned != null) {
                sessions.remove(id, owned);
            }
            transientStates.remove(id);
        }
        if (commandFailure != null) {
            lastErrors.put(id, commandFailure.getMessage());
            throw commandFailure;
        }
        lastErrors.remove(id);
    }

    @Override
    public synchronized AndroidEmulatorProcess restart(String avdId)
            throws IOException, InterruptedException {
        String id = validateAvdId(avdId);
        stop(id);
        return startInternal(id, false);
    }

    @Override
    public synchronized AndroidEmulatorProcess wipe(String avdId) throws IOException, InterruptedException {
        String id = validateAvdId(avdId);
        requireStopped(id, "wipe Android AVD user data");
        return startInternal(id, true);
    }

    @Override
    public synchronized void delete(String avdId) throws IOException, InterruptedException {
        String id = validateAvdId(avdId);
        requireStopped(id, "delete Android AVD");
        Path avdManager = requireTool(AndroidSdkTool.AVD_MANAGER, "delete Android AVD");
        ProcessResult result = execute(
                avdManager, MUTATION_TIMEOUT, null,
                List.of("delete", "avd", "--name", id));
        requireSuccess("delete Android AVD", id, result);
        lastErrors.remove(id);
        transientStates.remove(id);
    }

    @Override
    public synchronized void close() {
        // Detach from launched emulators. Closing an IDE window must not stop user devices.
        sessions.clear();
        transientStates.clear();
    }

    private AndroidEmulatorProcess startInternal(String id, boolean wipeData)
            throws IOException, InterruptedException {
        if (findConfiguration(id).isEmpty()) {
            throw new IOException("Unable to start Android AVD '" + id
                    + "': no configured AVD has that id.");
        }
        AndroidEmulatorProcess current = sessions.get(id);
        if (current != null && current.isAlive()) {
            throw new IOException("Unable to start Android AVD '" + id
                    + "': its emulator process is already running.");
        }
        ConnectionSnapshot connections = connectedEmulators();
        DeviceConnection connected = connections.byAvdId().get(id);
        if (connected != null) {
            throw new IOException("Unable to start Android AVD '" + id + "': it is already "
                    + connected.state().name().toLowerCase(Locale.ROOT) + " as "
                    + connected.serial() + ".");
        }
        if (connections.error() != null) {
            throw new IOException("Unable to start Android AVD '" + id
                    + "' safely: " + connections.error());
        }

        Path emulator = requireTool(AndroidSdkTool.EMULATOR, "start Android AVD");
        List<String> arguments = new ArrayList<>(List.of("-avd", id));
        if (wipeData) {
            arguments.add("-wipe-data");
        }
        transientStates.put(id, AndroidAvdState.STARTING);
        lastErrors.remove(id);
        final Process process;
        try {
            process = processStarter.start(
                    emulator, sdk.root(), processEnvironment, List.copyOf(arguments));
        } catch (IOException ex) {
            transientStates.remove(id);
            String message = "Unable to start Android AVD '" + id + "' with " + emulator
                    + ": " + cleanMessage(ex);
            lastErrors.put(id, message);
            throw new IOException(message, ex);
        }
        AndroidEmulatorProcess session = new AndroidEmulatorProcess(id, process, this::onSessionExit);
        AndroidEmulatorProcess replaced = sessions.put(id, session);
        if (replaced != null && replaced != session) {
            replaced.close();
        }
        if (!session.isAlive()) {
            onSessionExit(session, session.exitCode().orElse(-1));
        }
        return session;
    }

    private void onSessionExit(AndroidEmulatorProcess session, int exitCode) {
        boolean wasOwned = sessions.remove(session.avdId(), session);
        transientStates.remove(session.avdId());
        if (exitCode != 0 && wasOwned) {
            String detail = compact(session.diagnosticOutput());
            lastErrors.put(session.avdId(), "Android Emulator for AVD '" + session.avdId()
                    + "' exited with code " + exitCode + ". "
                    + (detail.isBlank() ? "The emulator did not provide an error message." : detail));
        }
    }

    private AndroidAvd applyRuntime(AndroidAvd avd, ConnectionSnapshot connections) {
        DeviceConnection connected = connections.byAvdId().get(avd.id());
        if (connected != null) {
            AndroidAvdState state = connected.state();
            if (state == AndroidAvdState.RUNNING) {
                transientStates.remove(avd.id());
                lastErrors.remove(avd.id());
            }
            String detail = switch (state) {
                case RUNNING -> "Running as " + connected.serial();
                case STARTING -> "Android is booting on " + connected.serial();
                case OFFLINE -> "ADB reports " + connected.serial() + " as offline";
                case ERROR -> "ADB reported an error for " + connected.serial();
                default -> state.name();
            };
            String error = switch (state) {
                case OFFLINE -> "ADB device " + connected.serial() + " is offline.";
                case ERROR -> "ADB could not determine a usable state for " + connected.serial() + ".";
                default -> avd.error().orElse(null);
            };
            return avd.withRuntime(state, connected.serial(), detail, error);
        }

        AndroidEmulatorProcess session = sessions.get(avd.id());
        AndroidAvdState transientState = transientStates.get(avd.id());
        if (session != null && session.isAlive()) {
            return avd.withRuntime(
                    transientState == AndroidAvdState.STOPPING
                            ? AndroidAvdState.STOPPING : AndroidAvdState.STARTING,
                    null,
                    transientState == AndroidAvdState.STOPPING
                            ? "Stopping Android Emulator" : "Waiting for Android Emulator to register with ADB",
                    avd.error().orElse(null));
        }
        String operationError = lastErrors.get(avd.id());
        if (operationError != null) {
            return avd.withRuntime(AndroidAvdState.ERROR, null,
                    "Android Emulator operation failed", operationError);
        }
        if (connections.error() != null) {
            return avd.withRuntime(AndroidAvdState.ERROR, null,
                    "Runtime state could not be verified", connections.error());
        }
        return avd.withRuntime(AndroidAvdState.STOPPED, null, "Stopped",
                avd.error().orElse(null));
    }

    private void requireStopped(String id, String operation)
            throws IOException, InterruptedException {
        AndroidEmulatorProcess owned = sessions.get(id);
        if (owned != null && owned.isAlive()) {
            throw new IOException("Unable to " + operation + " '" + id
                    + "': its emulator process is running.");
        }
        ConnectionSnapshot connections = connectedEmulators();
        DeviceConnection connected = connections.byAvdId().get(id);
        if (connected != null) {
            throw new IOException("Unable to " + operation + " '" + id + "': ADB reports it as "
                    + connected.state().name().toLowerCase(Locale.ROOT) + " on "
                    + connected.serial() + ". Stop it first.");
        }
        if (connections.error() != null) {
            throw new IOException("Unable to " + operation + " '" + id
                    + "' safely: " + connections.error());
        }
    }

    private List<AndroidAvd> loadAvdConfigurations() throws IOException, InterruptedException {
        if (sdk.avdManager().isPresent()) {
            ProcessResult result = execute(
                    sdk.avdManager().orElseThrow(), LIST_TIMEOUT, null,
                    List.of("list", "avd"));
            requireSuccess("list Android AVDs", null, result);
            String warning = result.stderr() == null || result.stderr().isBlank()
                    ? null : "avdmanager warning: " + compact(result.stderr());
            List<AndroidAvd> parsed = parseAvds(result.stdout(), warning);
            return parsed.stream().map(this::enrichFromConfig).toList();
        }
        Path emulator = requireTool(AndroidSdkTool.EMULATOR, "list Android AVDs");
        ProcessResult result = execute(emulator, LIST_TIMEOUT, null, List.of("-list-avds"));
        requireSuccess("list Android AVDs", null, result);
        return result.stdout().lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .filter(line -> SAFE_AVD_ID.matcher(line).matches())
                .distinct()
                .map(id -> new AndroidAvd(
                        id, id, "", "unknown", "unknown", "", "",
                        AndroidAvdState.STOPPED, Optional.empty(), Optional.empty(),
                        "Listed by Android Emulator", Optional.empty()))
                .toList();
    }

    private Optional<AndroidAvd> findConfiguration(String id)
            throws IOException, InterruptedException {
        return loadAvdConfigurations().stream().filter(avd -> avd.id().equals(id)).findFirst();
    }

    private AndroidAvd enrichFromConfig(AndroidAvd avd) {
        if (avd.dataDirectory().isEmpty()) {
            return avd;
        }
        Path config = avd.dataDirectory().orElseThrow().resolve("config.ini");
        if (!Files.isRegularFile(config)) {
            return avd;
        }
        try {
            if (Files.size(config) > MAX_CONFIG_BYTES) {
                return avd.withRuntime(avd.state(), null, avd.detail(),
                        "AVD config.ini exceeds " + MAX_CONFIG_BYTES + " bytes and was not read.");
            }
            Map<String, String> values = parseIni(Files.readString(config, StandardCharsets.UTF_8));
            String displayName = valueOr(values.get("avd.ini.displayname"), avd.displayName());
            String device = valueOr(values.get("hw.device.name"), avd.deviceProfile());
            String api = avd.apiLevel();
            String tag = valueOr(values.get("tag.id"), avd.tag());
            String abi = valueOr(values.get("abi.type"), avd.abi());
            Matcher image = IMAGE_DIRECTORY.matcher(valueOr(values.get("image.sysdir.1"), ""));
            if (image.find()) {
                api = image.group(1);
                tag = image.group(2);
                abi = image.group(3);
            }
            return new AndroidAvd(
                    avd.id(), displayName, device, api, abi,
                    valueOr(values.get("target"), avd.target()), tag,
                    avd.state(), avd.connectedDeviceId(), avd.dataDirectory(), avd.detail(), avd.error());
        } catch (IOException | SecurityException ex) {
            return avd.withRuntime(avd.state(), null, avd.detail(),
                    "Unable to read " + config + ": " + cleanMessage(ex));
        }
    }

    private ConnectionSnapshot connectedEmulators() throws InterruptedException {
        if (sdk.adb().isEmpty()) {
            return ConnectionSnapshot.failure("Android SDK tool 'adb' is unavailable under "
                    + sdk.root() + "; emulator runtime state cannot be verified.");
        }
        try {
            Map<String, DeviceConnection> byName = new LinkedHashMap<>();
            List<String> mappingErrors = new ArrayList<>();
            for (AndroidConnectedDevice device : listConnectedDevices()) {
                if (!device.emulator()) {
                    continue;
                }
                if (device.avdId().isEmpty()) {
                    mappingErrors.add(device.error().orElse(
                            "Unable to map running emulator " + device.id() + " to an AVD id."));
                    continue;
                }
                AndroidAvdState state = switch (device.state()) {
                    case ONLINE -> AndroidAvdState.RUNNING;
                    case BOOTING -> AndroidAvdState.STARTING;
                    case OFFLINE, UNAUTHORIZED, DISCONNECTED -> AndroidAvdState.OFFLINE;
                    case ERROR -> AndroidAvdState.ERROR;
                };
                byName.put(device.avdId().orElseThrow(), new DeviceConnection(device.id(), state));
            }
            return new ConnectionSnapshot(Map.copyOf(byName),
                    mappingErrors.isEmpty() ? null : String.join(" ", mappingErrors));
        } catch (IOException ex) {
            return ConnectionSnapshot.failure(cleanMessage(ex));
        }
    }

    private void waitForDisconnect(String serial, String avdId, Duration timeout)
            throws IOException, InterruptedException {
        Path adb = requireTool(AndroidSdkTool.ADB, "confirm Android AVD stop");
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            ProcessResult result = execute(adb, ADB_TIMEOUT, null, List.of("devices", "-l"));
            requireSuccess("confirm Android AVD stop", avdId, result);
            boolean present = result.stdout() != null && result.stdout().lines()
                    .map(String::strip)
                    .anyMatch(line -> line.equals(serial) || line.startsWith(serial + " ")
                            || line.startsWith(serial + "\t"));
            if (!present) {
                return;
            }
            Thread.sleep(250);
        }
        throw new IOException("Unable to stop Android AVD '" + avdId + "': ADB device "
                + serial + " is still connected after " + timeout + ".");
    }

    private ProcessResult execute(
            Path executable,
            Duration timeout,
            String input,
            List<String> arguments) throws IOException, InterruptedException {
        return command.execute(
                executable, sdk.root(), timeout, processEnvironment, input, List.copyOf(arguments));
    }

    private Path requireTool(AndroidSdkTool tool, String operation) throws IOException {
        return sdk.executable(tool).orElseThrow(() -> new IOException(
                "Unable to " + operation + ": Android SDK tool '" + tool.displayName()
                        + "' was not found under " + sdk.root() + "."));
    }

    private void requireInstalledSystemImage(String packageId) throws IOException {
        Path image = sdk.root();
        for (String segment : packageId.split(";")) {
            image = image.resolve(segment);
        }
        if (!Files.isDirectory(image)) {
            throw new IOException("Unable to create Android AVD with system image '" + packageId
                    + "': the package is not installed under " + sdk.root() + ".");
        }
    }

    private static AndroidAvd avdFromRequest(AndroidAvdCreateRequest request) {
        String[] image = request.systemImagePackage().split(";");
        return new AndroidAvd(
                request.name(), request.name(), request.deviceDefinitionId().orElse(""),
                image[1].replaceFirst("^android-", ""), image[3], request.systemImagePackage(), image[2],
                AndroidAvdState.STOPPED, Optional.empty(), Optional.empty(),
                "Created from " + request.systemImagePackage(), Optional.empty());
    }

    static List<AndroidAvd> parseAvds(String output, String warning) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        List<AndroidAvd> result = new ArrayList<>();
        AvdBuilder builder = null;
        for (String raw : output.lines().toList()) {
            String line = raw.strip();
            if (line.startsWith("Name:")) {
                if (builder != null) {
                    builder.build(warning).ifPresent(result::add);
                }
                builder = new AvdBuilder(line.substring("Name:".length()).strip());
            } else if (builder != null) {
                if (line.startsWith("Device:")) {
                    builder.device = line.substring("Device:".length()).strip();
                } else if (line.startsWith("Path:")) {
                    builder.path = line.substring("Path:".length()).strip();
                } else if (line.startsWith("Target:")) {
                    builder.target = line.substring("Target:".length()).strip();
                } else if (line.contains("Based on:")) {
                    builder.basedOn = line.substring(line.indexOf("Based on:") + "Based on:".length()).strip();
                    int tagAbi = builder.basedOn.indexOf("Tag/ABI:");
                    if (tagAbi >= 0) {
                        String value = builder.basedOn.substring(tagAbi + "Tag/ABI:".length()).strip();
                        builder.basedOn = builder.basedOn.substring(0, tagAbi).strip();
                        int separator = value.lastIndexOf('/');
                        if (separator > 0) {
                            builder.tag = value.substring(0, separator).strip();
                            builder.abi = value.substring(separator + 1).strip();
                        }
                    }
                }
            }
        }
        if (builder != null) {
            builder.build(warning).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    static List<AndroidSystemImage> parseSystemImages(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        LinkedHashMap<String, AndroidSystemImage> images = new LinkedHashMap<>();
        boolean installed = false;
        boolean packageSection = false;
        String pending = null;
        for (String raw : output.lines().toList()) {
            String line = raw.strip();
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("installed package")) {
                installed = true;
                packageSection = true;
                pending = null;
                continue;
            }
            if (lower.startsWith("available package") || lower.startsWith("available update")) {
                installed = false;
                packageSection = true;
                pending = null;
                continue;
            }
            if (!packageSection || line.isBlank() || line.startsWith("Path")
                    || line.startsWith("---")) {
                continue;
            }
            if (line.startsWith("system-images;")) {
                pending = line;
            } else if (pending != null && !pending.contains("|") && !line.contains(":")) {
                pending += line;
            } else {
                pending = null;
            }
            if (pending == null || !pending.contains("|")) {
                continue;
            }
            String[] columns = pending.split("\\s*\\|\\s*", 4);
            pending = null;
            if (columns.length < 3) {
                continue;
            }
            String packageId = columns[0].strip();
            String[] segments = packageId.split(";");
            if (segments.length != 4) {
                continue;
            }
            AndroidSystemImage parsed = new AndroidSystemImage(
                    packageId,
                    segments[1].replaceFirst("^android-", ""),
                    segments[2], segments[3], columns[2], columns[1], installed);
            AndroidSystemImage current = images.get(packageId);
            if (current == null || parsed.installed()) {
                images.put(packageId, parsed);
            }
        }
        return images.values().stream()
                .sorted(Comparator.comparing(AndroidSystemImage::installed).reversed()
                        .thenComparing(AndroidSystemImage::packageId))
                .toList();
    }

    static List<AndroidDeviceDefinition> parseDeviceDefinitions(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        List<AndroidDeviceDefinition> result = new ArrayList<>();
        DeviceBuilder builder = null;
        for (String raw : output.lines().toList()) {
            String line = raw.strip();
            Matcher id = DEVICE_ID.matcher(line);
            if (id.matches()) {
                if (builder != null) {
                    builder.build().ifPresent(result::add);
                }
                builder = new DeviceBuilder(id.group(1));
            } else if (builder != null) {
                if (line.startsWith("Name:")) {
                    builder.name = line.substring("Name:".length()).strip();
                } else if (line.startsWith("OEM :")) {
                    builder.manufacturer = line.substring("OEM :".length()).strip();
                } else if (line.startsWith("Tag :")) {
                    builder.tag = line.substring("Tag :".length()).strip();
                }
            }
        }
        if (builder != null) {
            builder.build().ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    private static Map<String, String> parseIni(String content) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (String raw : content.lines().toList()) {
            String line = raw.strip();
            if (line.isBlank() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator > 0) {
                values.put(line.substring(0, separator).strip(),
                        line.substring(separator + 1).strip());
            }
        }
        return Map.copyOf(values);
    }

    private static Map<String, String> parseAdbAttributes(String text) {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        for (String token : text.split("\\s+")) {
            int separator = token.indexOf(':');
            if (separator > 0 && separator < token.length() - 1) {
                attributes.put(token.substring(0, separator), token.substring(separator + 1));
            }
        }
        return Map.copyOf(attributes);
    }

    private static String firstAvdName(String output) {
        if (output == null) {
            return null;
        }
        return output.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank() && !line.equalsIgnoreCase("OK"))
                .filter(line -> SAFE_AVD_ID.matcher(line).matches())
                .findFirst().orElse(null);
    }

    private static String validateAvdId(String value) {
        if (value == null || !SAFE_AVD_ID.matcher(value.strip()).matches()) {
            throw new IllegalArgumentException("Android AVD id is invalid");
        }
        return value.strip();
    }

    private static void requireSuccess(String operation, String target, ProcessResult result)
            throws IOException {
        if (result.success()) {
            return;
        }
        String targetText = target == null ? "" : " '" + target + "'";
        throw new IOException("Unable to " + operation + targetText + ": command exited with code "
                + result.exitCode() + ". " + failureDetail(result));
    }

    private static String failureDetail(ProcessResult result) {
        String detail = result.stderr() == null || result.stderr().isBlank()
                ? result.stdout() : result.stderr();
        String compact = compact(detail);
        return compact.isBlank() ? "The Android SDK tool did not provide an error message." : compact;
    }

    private static String compact(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compact = value.strip().replaceAll("\\s+", " ");
        return compact.length() <= 1200 ? compact : compact.substring(0, 1200) + "…";
    }

    private static String cleanMessage(Throwable failure) {
        return failure.getMessage() == null || failure.getMessage().isBlank()
                ? failure.getClass().getSimpleName() : failure.getMessage().strip();
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private record DeviceConnection(String serial, AndroidAvdState state) {
    }

    private record ConnectionSnapshot(Map<String, DeviceConnection> byAvdId, String error) {
        static ConnectionSnapshot failure(String message) {
            return new ConnectionSnapshot(Map.of(), message);
        }
    }

    private static final class AvdBuilder {
        private final String id;
        private String device = "";
        private String path = "";
        private String target = "";
        private String basedOn = "";
        private String tag = "";
        private String abi = "unknown";

        AvdBuilder(String id) {
            this.id = id;
        }

        Optional<AndroidAvd> build(String warning) {
            if (!SAFE_AVD_ID.matcher(id).matches()) {
                return Optional.empty();
            }
            Optional<Path> dataPath = Optional.empty();
            if (!path.isBlank()) {
                try {
                    dataPath = Optional.of(Path.of(path).toAbsolutePath().normalize());
                } catch (InvalidPathException ignored) {
                }
            }
            String detail = target;
            if (!basedOn.isBlank()) {
                detail = detail.isBlank() ? basedOn : detail + " — " + basedOn;
            }
            return Optional.of(new AndroidAvd(
                    id, id, device, "unknown", abi, target, tag,
                    AndroidAvdState.STOPPED, Optional.empty(), dataPath, detail,
                    Optional.ofNullable(warning)));
        }
    }

    private static final class DeviceBuilder {
        private final String id;
        private String name = "";
        private String manufacturer = "";
        private String tag = "";

        DeviceBuilder(String id) {
            this.id = id;
        }

        Optional<AndroidDeviceDefinition> build() {
            return id.isBlank() ? Optional.empty()
                    : Optional.of(new AndroidDeviceDefinition(id, name, manufacturer, tag));
        }
    }
}
