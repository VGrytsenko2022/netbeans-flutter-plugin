package io.github.vgrytsenko2022.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.api.ProcessResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AndroidSdkAvdServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void listsDetailedAvdAndSurfacesRecoverableWarning() throws Exception {
        Path avdDirectory = temporaryDirectory.resolve("Galaxy_S24_Ultra.avd");
        Files.createDirectories(avdDirectory);
        Files.writeString(avdDirectory.resolve("config.ini"), """
                avd.ini.displayname=Galaxy S24 Ultra API 36
                hw.device.name=Galaxy S24 Ultra
                image.sysdir.1=system-images\\android-36\\google_apis\\x86_64\\
                tag.id=google_apis
                abi.type=x86_64
                target=android-36
                """);
        ScriptedExecutor executor = new ScriptedExecutor(invocation -> {
            if (invocation.arguments().equals(List.of("list", "avd"))) {
                return new ProcessResult(0, avdOutput("Galaxy_S24_Ultra", avdDirectory),
                        "Error: Could not load one devices.xml");
            }
            if (invocation.arguments().equals(List.of("devices", "-l"))) {
                return new ProcessResult(0, "List of devices attached\n", "");
            }
            throw new AssertionError("Unexpected invocation " + invocation);
        });
        AndroidSdkAvdService service = service(executor, failingStarter());

        AndroidAvd avd = service.listAvds().getFirst();

        assertEquals("Galaxy_S24_Ultra", avd.id());
        assertEquals("Galaxy S24 Ultra API 36", avd.displayName());
        assertEquals("Galaxy S24 Ultra", avd.deviceProfile());
        assertEquals("36", avd.apiLevel());
        assertEquals("google_apis", avd.tag());
        assertEquals("x86_64", avd.abi());
        assertEquals(AndroidAvdState.STOPPED, avd.state());
        assertTrue(avd.error().orElseThrow().contains("devices.xml"));
    }

    @Test
    void listsConnectedDevicesWithExactAvdMappingAndBootState() throws Exception {
        ScriptedExecutor executor = new ScriptedExecutor(invocation -> {
            List<String> args = invocation.arguments();
            if (args.equals(List.of("devices", "-l"))) {
                return new ProcessResult(0, """
                        List of devices attached
                        emulator-5554 device product:sdk_phone model:sdk_gphone64_x86_64 device:emu64xa transport_id:1
                        R58N123 unauthorized usb:1-2 product:physical model:Galaxy_S24 device:e3q
                        emulator-5556 offline transport_id:3
                        """, "");
            }
            if (args.equals(List.of("-s", "emulator-5554", "emu", "avd", "name"))) {
                return new ProcessResult(0, "Galaxy_S24_Ultra\nOK\n", "");
            }
            if (args.equals(List.of("-s", "emulator-5556", "emu", "avd", "name"))) {
                return new ProcessResult(0, "Pixel_8\nOK\n", "");
            }
            if (args.equals(List.of("-s", "emulator-5554", "shell", "getprop", "sys.boot_completed"))) {
                return new ProcessResult(0, "1\n", "");
            }
            throw new AssertionError("Unexpected invocation " + invocation);
        });
        AndroidSdkAvdService service = service(executor, failingStarter());

        List<AndroidConnectedDevice> devices = service.listConnectedDevices();

        assertEquals(3, devices.size());
        AndroidConnectedDevice emulator = devices.get(0);
        assertEquals("emulator-5554", emulator.id());
        assertEquals("Galaxy_S24_Ultra", emulator.avdId().orElseThrow());
        assertEquals(AndroidConnectedDeviceState.ONLINE, emulator.state());
        assertEquals("sdk_gphone64_x86_64", emulator.model());
        assertEquals(AndroidConnectedDeviceState.UNAUTHORIZED, devices.get(1).state());
        assertEquals(AndroidConnectedDeviceState.OFFLINE, devices.get(2).state());
        assertEquals("Pixel_8", devices.get(2).avdId().orElseThrow());
    }

    @Test
    void mapsBootingConnectedEmulatorIntoAvdStartingState() throws Exception {
        Path avdDirectory = temporaryDirectory.resolve("Pixel_8.avd");
        ScriptedExecutor executor = standardAvdAndAdbExecutor(
                "Pixel_8", avdDirectory, "emulator-5554", "");
        AndroidSdkAvdService service = service(executor, failingStarter());

        AndroidAvd avd = service.listAvds().getFirst();

        assertEquals(AndroidAvdState.STARTING, avd.state());
        assertEquals("emulator-5554", avd.connectedDeviceId().orElseThrow());
    }

    @Test
    void parsesInstalledAvailableAndWrappedSystemImageRows() {
        String output = """
                Installed packages:
                  Path                                        | Version | Description                    | Location
                  -------                                     | ------- | -------                        | -------
                  system-images;android-35;google_apis;x86_64 | 10      | Google APIs Intel x86_64 Image | system-images\\android-35

                Available Packages:
                  Path                                        | Version | Description
                  -------                                     | ------- | -------
                  system-images;android-35;google_apis;x86_64 | 11      | Newer duplicate
                  system-images;android-36.1;google_apis;
                  x86_64 | 4 | Google APIs Intel x86_64 Image
                """;

        List<AndroidSystemImage> images = AndroidSdkAvdService.parseSystemImages(output);

        assertEquals(2, images.size());
        assertEquals("system-images;android-35;google_apis;x86_64", images.get(0).packageId());
        assertTrue(images.get(0).installed());
        assertEquals("10", images.get(0).version());
        assertEquals("36.1", images.get(1).apiLevel());
        assertFalse(images.get(1).installed());
    }

    @Test
    void invokesSdkManagerWithStableChannelAndTimeout() throws Exception {
        ScriptedExecutor executor = new ScriptedExecutor(invocation -> {
            assertEquals(Duration.ofMinutes(2), invocation.timeout());
            assertEquals(List.of("--list", "--channel=0"), invocation.arguments());
            return new ProcessResult(0, "Available Packages:\n", "");
        });

        assertTrue(service(executor, failingStarter()).listSystemImages().isEmpty());
    }

    @Test
    void parsesDeviceDefinitions() throws Exception {
        String output = """
                Error: recoverable devices.xml warning
                Available devices definitions:
                id: 0 or "medium_phone"
                    Name: Medium Phone
                    OEM : Generic
                ---------
                id: 1 or "pixel_8"
                    Name: Pixel 8
                    OEM : Google
                    Tag : google_apis
                """;
        ScriptedExecutor executor = new ScriptedExecutor(invocation ->
                new ProcessResult(0, output, ""));

        List<AndroidDeviceDefinition> definitions = service(executor, failingStarter())
                .listDeviceDefinitions();

        assertEquals(2, definitions.size());
        assertEquals(new AndroidDeviceDefinition("medium_phone", "Medium Phone", "Generic", ""),
                definitions.get(0));
        assertEquals("google_apis", definitions.get(1).tag());
    }

    @Test
    void createsAvdWithValidatedArgumentsAndDefaultPromptAnswer() throws Exception {
        Path sdkRoot = temporaryDirectory.resolve("sdk");
        installImage(sdkRoot, "system-images;android-36;google_apis;x86_64");
        AtomicBoolean created = new AtomicBoolean();
        ScriptedExecutor executor = new ScriptedExecutor(invocation -> {
            if (invocation.arguments().equals(List.of("list", "avd"))) {
                return new ProcessResult(0,
                        created.get() ? avdOutput("Pixel_8_API_36", temporaryDirectory.resolve("avd")) : "",
                        "");
            }
            if (invocation.arguments().getFirst().equals("create")) {
                assertEquals(Duration.ofMinutes(2), invocation.timeout());
                assertEquals("no" + System.lineSeparator(), invocation.standardInput());
                assertEquals(List.of(
                        "create", "avd", "--name", "Pixel_8_API_36",
                        "--package", "system-images;android-36;google_apis;x86_64",
                        "--device", "pixel_8", "--sdcard", "512M"), invocation.arguments());
                created.set(true);
                return new ProcessResult(0, "Created AVD", "");
            }
            throw new AssertionError("Unexpected invocation " + invocation);
        });
        AndroidSdkAvdService service = service(sdkRoot, executor, failingStarter());
        AndroidAvdCreateRequest request = new AndroidAvdCreateRequest(
                "Pixel_8_API_36", "system-images;android-36;google_apis;x86_64",
                Optional.of("pixel_8"), Optional.of("512M"), false);

        AndroidAvd createdAvd = service.create(request);

        assertEquals("Pixel_8_API_36", createdAvd.id());
        assertTrue(created.get());
    }

    @Test
    void rejectsMissingImageAndDuplicateWithoutOverwrite() throws Exception {
        Path avdDirectory = temporaryDirectory.resolve("existing.avd");
        ScriptedExecutor duplicateExecutor = new ScriptedExecutor(invocation ->
                new ProcessResult(0, avdOutput("Existing", avdDirectory), ""));
        AndroidSdkAvdService duplicateService = service(duplicateExecutor, failingStarter());

        IOException duplicate = assertThrows(IOException.class, () -> duplicateService.create(
                new AndroidAvdCreateRequest("Existing",
                        "system-images;android-36;google_apis;x86_64")));

        assertTrue(duplicate.getMessage().contains("already exists"));

        ScriptedExecutor emptyExecutor = new ScriptedExecutor(invocation ->
                new ProcessResult(0, "", ""));
        IOException missing = assertThrows(IOException.class, () -> service(emptyExecutor, failingStarter())
                .create(new AndroidAvdCreateRequest("New_Avd",
                        "system-images;android-36;google_apis;x86_64")));
        assertTrue(missing.getMessage().contains("not installed"));
    }

    @Test
    void startsAndTracksEmulatorWithoutKillingItOnClose() throws Exception {
        Path avdDirectory = temporaryDirectory.resolve("Pixel_8.avd");
        ScriptedExecutor executor = stoppedAvdExecutor("Pixel_8", avdDirectory);
        TestFlutterProcess process = new TestFlutterProcess();
        AtomicReference<StartInvocation> started = new AtomicReference<>();
        AndroidProcessStarter starter = (executable, workingDirectory, environment, arguments) -> {
            started.set(new StartInvocation(executable, workingDirectory, environment, arguments));
            return process;
        };
        AndroidSdkAvdService service = service(executor, starter);

        AndroidEmulatorProcess session = service.start("Pixel_8");

        assertEquals(List.of("-avd", "Pixel_8"), started.get().arguments());
        assertEquals(service.sdk().root().toString(), started.get().environment().get("ANDROID_SDK_ROOT"));
        assertEquals(AndroidAvdState.STARTING, service.listAvds().getFirst().state());
        session.close();
        service.close();
        assertTrue(process.isAlive(), "detaching the Device Manager must not stop the emulator");
        process.finish(0);
    }

    @Test
    void explicitStopTerminatesOwnedProcess() throws Exception {
        ScriptedExecutor executor = stoppedAvdExecutor("Pixel_8", temporaryDirectory.resolve("avd"));
        TestFlutterProcess process = new TestFlutterProcess();
        AndroidSdkAvdService service = service(executor,
                (executable, workingDirectory, environment, arguments) -> process);
        service.start("Pixel_8");

        service.stop("Pixel_8");

        assertFalse(process.isAlive());
    }

    @Test
    void stopsExternalEmulatorThroughItsExactAdbSerial() throws Exception {
        Path avdDirectory = temporaryDirectory.resolve("Pixel_8.avd");
        AtomicBoolean killed = new AtomicBoolean();
        ScriptedExecutor executor = new ScriptedExecutor(invocation -> {
            List<String> args = invocation.arguments();
            if (args.equals(List.of("devices", "-l"))) {
                return new ProcessResult(0, killed.get()
                        ? "List of devices attached\n"
                        : "List of devices attached\nemulator-5588 device model:Pixel_8\n", "");
            }
            if (args.equals(List.of("-s", "emulator-5588", "emu", "avd", "name"))) {
                return new ProcessResult(0, "Pixel_8\nOK\n", "");
            }
            if (args.equals(List.of("-s", "emulator-5588", "shell", "getprop", "sys.boot_completed"))) {
                return new ProcessResult(0, "1\n", "");
            }
            if (args.equals(List.of("-s", "emulator-5588", "emu", "kill"))) {
                killed.set(true);
                return new ProcessResult(0, "OK\n", "");
            }
            if (args.equals(List.of("list", "avd"))) {
                return new ProcessResult(0, avdOutput("Pixel_8", avdDirectory), "");
            }
            throw new AssertionError("Unexpected invocation " + invocation);
        });

        service(executor, failingStarter()).stop("Pixel_8");

        assertTrue(killed.get());
    }

    @Test
    void wipeStartsStoppedAvdWithOfficialWipeDataOption() throws Exception {
        ScriptedExecutor executor = stoppedAvdExecutor("Pixel_8", temporaryDirectory.resolve("avd"));
        TestFlutterProcess process = new TestFlutterProcess();
        AtomicReference<List<String>> arguments = new AtomicReference<>();
        AndroidSdkAvdService service = service(executor,
                (executable, workingDirectory, environment, values) -> {
                    arguments.set(values);
                    return process;
                });

        service.wipe("Pixel_8");

        assertEquals(List.of("-avd", "Pixel_8", "-wipe-data"), arguments.get());
        process.finish(0);
    }

    @Test
    void deleteRefusesRunningAvdAndUsesAvdManagerForStoppedAvd() throws Exception {
        Path avdDirectory = temporaryDirectory.resolve("Pixel_8.avd");
        ScriptedExecutor running = standardAvdAndAdbExecutor(
                "Pixel_8", avdDirectory, "emulator-5554", "1");
        IOException refusal = assertThrows(IOException.class,
                () -> service(running, failingStarter()).delete("Pixel_8"));
        assertTrue(refusal.getMessage().contains("Stop it first"));

        AtomicBoolean deleted = new AtomicBoolean();
        ScriptedExecutor stopped = new ScriptedExecutor(invocation -> {
            if (invocation.arguments().equals(List.of("devices", "-l"))) {
                return new ProcessResult(0, "List of devices attached\n", "");
            }
            if (invocation.arguments().equals(List.of("delete", "avd", "--name", "Pixel_8"))) {
                deleted.set(true);
                return new ProcessResult(0, "AVD deleted", "");
            }
            throw new AssertionError("Unexpected invocation " + invocation);
        });
        service(stopped, failingStarter()).delete("Pixel_8");
        assertTrue(deleted.get());
    }

    @Test
    void waitForBootPollsWithoutOwningOrStoppingEmulator() throws Exception {
        AtomicInteger bootQueries = new AtomicInteger();
        ScriptedExecutor executor = new ScriptedExecutor(invocation -> {
            List<String> args = invocation.arguments();
            if (args.equals(List.of("devices", "-l"))) {
                return new ProcessResult(0,
                        "List of devices attached\nemulator-5554 device model:Pixel_8\n", "");
            }
            if (args.equals(List.of("-s", "emulator-5554", "emu", "avd", "name"))) {
                return new ProcessResult(0, "Pixel_8\nOK\n", "");
            }
            if (args.equals(List.of("-s", "emulator-5554", "shell", "getprop", "sys.boot_completed"))) {
                return new ProcessResult(0, bootQueries.incrementAndGet() >= 2 ? "1\n" : "\n", "");
            }
            throw new AssertionError("Unexpected invocation " + invocation);
        });

        AndroidConnectedDevice device = service(executor, failingStarter())
                .waitForBoot("Pixel_8", Duration.ofSeconds(2));

        assertEquals(AndroidConnectedDeviceState.ONLINE, device.state());
        assertTrue(bootQueries.get() >= 2);
    }

    @Test
    void timeoutLeavesStartedEmulatorRunning() throws Exception {
        ScriptedExecutor executor = stoppedAvdExecutor("Pixel_8", temporaryDirectory.resolve("avd"));
        TestFlutterProcess process = new TestFlutterProcess();
        AndroidSdkAvdService service = service(executor,
                (executable, workingDirectory, environment, arguments) -> process);
        service.start("Pixel_8");

        IOException timeout = assertThrows(IOException.class,
                () -> service.waitForBoot("Pixel_8", Duration.ofMillis(25)));

        assertTrue(timeout.getMessage().contains("remains running"));
        assertTrue(process.isAlive());
        service.stop("Pixel_8");
    }

    @Test
    void reportsMissingToolAndCommandFailureWithConcreteOperation() throws Exception {
        AndroidSdkInstallation noTools = new AndroidSdkInstallation(
                temporaryDirectory, Map.of(), "test");
        AndroidSdkAvdService missing = new AndroidSdkAvdService(
                noTools, new ScriptedExecutor(invocation -> new ProcessResult(0, "", "")), failingStarter());
        IOException missingFailure = assertThrows(IOException.class, missing::listSystemImages);
        assertTrue(missingFailure.getMessage().contains("sdkmanager"));
        assertTrue(missingFailure.getMessage().contains(temporaryDirectory.toString()));

        ScriptedExecutor failed = new ScriptedExecutor(invocation ->
                new ProcessResult(23, "", "repository unavailable"));
        IOException commandFailure = assertThrows(IOException.class,
                () -> service(failed, failingStarter()).listSystemImages());
        assertTrue(commandFailure.getMessage().contains("list Android system images"));
        assertTrue(commandFailure.getMessage().contains("code 23"));
        assertTrue(commandFailure.getMessage().contains("repository unavailable"));
    }

    @Test
    void validatesDestructiveIdentifiersAndCreateInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> new AndroidAvdCreateRequest("../escape",
                        "system-images;android-36;google_apis;x86_64"));
        assertThrows(IllegalArgumentException.class,
                () -> new AndroidAvdCreateRequest("Safe",
                        "platforms;android-36"));
        assertThrows(IllegalArgumentException.class,
                () -> new AndroidAvdCreateRequest(
                        "Safe", "system-images;android-36;google_apis;x86_64",
                        Optional.empty(), Optional.of("../../disk"), false));
    }

    private AndroidSdkAvdService service(
            ScriptedExecutor executor, AndroidProcessStarter starter) throws Exception {
        return service(temporaryDirectory.resolve("sdk"), executor, starter);
    }

    private static AndroidSdkAvdService service(
            Path sdkRoot, ScriptedExecutor executor, AndroidProcessStarter starter) throws Exception {
        Files.createDirectories(sdkRoot);
        EnumMap<AndroidSdkTool, Path> tools = new EnumMap<>(AndroidSdkTool.class);
        for (AndroidSdkTool tool : AndroidSdkTool.values()) {
            tools.put(tool, sdkRoot.resolve(tool.displayName() + ".test"));
        }
        return new AndroidSdkAvdService(
                new AndroidSdkInstallation(sdkRoot, tools, "test"), executor, starter);
    }

    private ScriptedExecutor stoppedAvdExecutor(String id, Path avdDirectory) {
        return new ScriptedExecutor(invocation -> {
            if (invocation.arguments().equals(List.of("list", "avd"))) {
                return new ProcessResult(0, avdOutput(id, avdDirectory), "");
            }
            if (invocation.arguments().equals(List.of("devices", "-l"))) {
                return new ProcessResult(0, "List of devices attached\n", "");
            }
            throw new AssertionError("Unexpected invocation " + invocation);
        });
    }

    private ScriptedExecutor standardAvdAndAdbExecutor(
            String id, Path avdDirectory, String serial, String bootComplete) {
        return new ScriptedExecutor(invocation -> {
            List<String> args = invocation.arguments();
            if (args.equals(List.of("list", "avd"))) {
                return new ProcessResult(0, avdOutput(id, avdDirectory), "");
            }
            if (args.equals(List.of("devices", "-l"))) {
                return new ProcessResult(0,
                        "List of devices attached\n" + serial + " device model:Pixel_8\n", "");
            }
            if (args.equals(List.of("-s", serial, "emu", "avd", "name"))) {
                return new ProcessResult(0, id + "\nOK\n", "");
            }
            if (args.equals(List.of("-s", serial, "shell", "getprop", "sys.boot_completed"))) {
                return new ProcessResult(0, bootComplete + "\n", "");
            }
            throw new AssertionError("Unexpected invocation " + invocation);
        });
    }

    private static String avdOutput(String id, Path path) {
        return """
                Available Android Virtual Devices:
                    Name: %s
                  Device: Pixel 8 (Google)
                    Path: %s
                  Target: Google APIs (Google Inc.)
                          Based on: Android 16.0 (\"Baklava\") Tag/ABI: google_apis/x86_64
                ---------
                """.formatted(id, path);
    }

    private static void installImage(Path sdkRoot, String packageId) throws Exception {
        Path image = sdkRoot;
        for (String segment : packageId.split(";")) {
            image = image.resolve(segment);
        }
        Files.createDirectories(image);
    }

    private static AndroidProcessStarter failingStarter() {
        return (executable, workingDirectory, environment, arguments) -> {
            throw new AssertionError("Emulator process must not be started");
        };
    }

    @FunctionalInterface
    private interface Responder {
        ProcessResult respond(CommandInvocation invocation) throws IOException, InterruptedException;
    }

    private static final class ScriptedExecutor implements AndroidCommandExecutor {
        private final Responder responder;
        private final List<CommandInvocation> invocations = new ArrayList<>();

        ScriptedExecutor(Responder responder) {
            this.responder = responder;
        }

        @Override
        public ProcessResult execute(
                Path executable,
                Path workingDirectory,
                Duration timeout,
                Map<String, String> environment,
                String standardInput,
                List<String> arguments) throws IOException, InterruptedException {
            CommandInvocation invocation = new CommandInvocation(
                    executable, workingDirectory, timeout, environment, standardInput, arguments);
            invocations.add(invocation);
            return responder.respond(invocation);
        }
    }

    private record CommandInvocation(
            Path executable,
            Path workingDirectory,
            Duration timeout,
            Map<String, String> environment,
            String standardInput,
            List<String> arguments) {
    }

    private record StartInvocation(
            Path executable,
            Path workingDirectory,
            Map<String, String> environment,
            List<String> arguments) {
    }
}
