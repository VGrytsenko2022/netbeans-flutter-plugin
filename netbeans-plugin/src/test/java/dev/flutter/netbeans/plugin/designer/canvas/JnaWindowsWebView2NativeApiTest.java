package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JnaWindowsWebView2NativeApiTest {
    private static final int S_OK = 0;
    private static final int E_FAIL = 0x80004005;
    private static final int ERROR_FILE_NOT_FOUND_HRESULT = 0x80070002;
    private static final int ERROR_INSUFFICIENT_BUFFER_HRESULT = 0x8007007A;
    private static final String NONCE =
            "0123456789abcdef0123456789abcdef"
            + "0123456789abcdef0123456789abcdef";
    private static final String GENERATION = "a".repeat(64);
    private static final long HWND = 0x0000_7fff_abcd_ef01L;
    private static final Pointer HANDLE =
            Pointer.createConstant(0x0000_7fff_1234_5678L);

    @Test
    void runtimeVersionUsesBoundedTwoCallContract() throws Exception {
        FakeBinding binding = new FakeBinding();
        binding.runtimeVersion = "151.0.4129.107";
        JnaWindowsWebView2NativeApi api = new JnaWindowsWebView2NativeApi(binding);

        assertEquals("151.0.4129.107", api.runtimeVersion());
        assertEquals(1, binding.runtimeProbeCalls);
        assertEquals(1, binding.runtimeReadCalls);
        assertEquals(binding.runtimeVersion.length() + 1,
                binding.lastRuntimeBufferCharacters);
    }

    @Test
    void runtimeVersionRejectsMalformedNativeAnswers() {
        FakeBinding malformed = new FakeBinding();
        malformed.runtimeVersion = "151.preview.1";
        assertMalformedRuntime(malformed);

        FakeBinding unterminated = new FakeBinding();
        unterminated.runtimeVersion = "151.0.1";
        unterminated.omitRuntimeTerminator = true;
        assertMalformedRuntime(unterminated);

        FakeBinding lengthMismatch = new FakeBinding();
        lengthMismatch.runtimeVersion = "151.0.1";
        lengthMismatch.runtimeReadRequiredOverride = 2;
        assertMalformedRuntime(lengthMismatch);

        FakeBinding tooShort = new FakeBinding();
        tooShort.runtimeProbeRequiredOverride = 1;
        assertTrue(assertThrows(IOException.class,
                () -> new JnaWindowsWebView2NativeApi(tooShort).runtimeVersion())
                .getMessage().contains("invalid version length"));

        FakeBinding excessive = new FakeBinding();
        excessive.runtimeProbeRequiredOverride = 257;
        assertTrue(assertThrows(IOException.class,
                () -> new JnaWindowsWebView2NativeApi(excessive).runtimeVersion())
                .getMessage().contains("invalid version length"));
    }

    @Test
    void missingRuntimePreservesTheNativeHresult() {
        FakeBinding binding = new FakeBinding();
        binding.runtimeProbeStatus = ERROR_FILE_NOT_FOUND_HRESULT;
        binding.runtimeProbeRequiredOverride = 0;

        IOException failure = assertThrows(IOException.class,
                () -> new JnaWindowsWebView2NativeApi(binding).runtimeVersion());

        assertTrue(failure.getMessage().contains(
                "detect installed WebView2 Runtime"));
        assertTrue(failure.getMessage().contains("HRESULT 0x80070002"));
        assertEquals(1, binding.runtimeProbeCalls);
        assertEquals(0, binding.runtimeReadCalls);
    }

    @Test
    void createWritesExactVersionedX64OptionsAndSortedResourceManifest()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        JnaWindowsWebView2NativeApi api = new JnaWindowsWebView2NativeApi(binding);

        WindowsWebView2NativeApi.NativeSession session =
                api.create(request(), event -> { });

        assertNotNull(session);
        assertEquals(1, binding.createCalls);
        assertNotNull(binding.createdOptions);
        assertEquals(binding.createdOptions.size(),
                binding.createdOptions.struct_size);
        assertEquals(2, binding.createdOptions.abi_version);
        assertEquals(HWND,
                Pointer.nativeValue(binding.createdOptions.parent_window));
        assertEquals(-12, binding.createdOptions.x);
        assertEquals(34, binding.createdOptions.y);
        assertEquals(1280, binding.createdOptions.width);
        assertEquals(720, binding.createdOptions.height);
        assertEquals(request().userDataFolder().toString(),
                binding.createdOptions.user_data_folder.toString());
        assertEquals(request().contentRoot().toString(),
                binding.createdOptions.content_root.toString());
        assertEquals(request().originPolicy().virtualHostName(),
                binding.createdOptions.virtual_host.toString());
        assertEquals(NONCE, binding.createdOptions.session_nonce.toString());
        assertEquals("a.css|7|" + "a".repeat(64)
                + "\nindex.html|11|" + "b".repeat(64)
                + "\nz.js|13|" + "c".repeat(64),
                binding.createdOptions.resource_manifest.toString());
        assertEquals(Pointer.NULL, binding.createdContext);
        assertNotNull(binding.createdCallback);
    }

    @Test
    void createRejectsNativeFailureAndMissingSessionHandle() {
        FakeBinding failed = new FakeBinding();
        failed.createStatus = E_FAIL;
        IOException nativeFailure = assertThrows(IOException.class,
                () -> new JnaWindowsWebView2NativeApi(failed)
                        .create(request(), event -> { }));
        assertTrue(nativeFailure.getMessage().contains(
                "create WebView2 environment/controller"));
        assertTrue(nativeFailure.getMessage().contains("HRESULT 0x80004005"));

        FakeBinding missing = new FakeBinding();
        missing.createdHandle = Pointer.NULL;
        IOException missingHandle = assertThrows(IOException.class,
                () -> new JnaWindowsWebView2NativeApi(missing)
                        .create(request(), event -> { }));
        assertTrue(missingHandle.getMessage().contains("no session handle"));
    }

    @Test
    void createRequestRejectsBoundsThatOverflowTheNativeRect() {
        WindowsWebView2NativeApi.CreateRequest valid = request();

        assertThrows(IllegalArgumentException.class,
                () -> requestWithBounds(valid, Integer.MAX_VALUE, 0, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> requestWithBounds(valid, 0, Integer.MAX_VALUE, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> requestWithBounds(valid, 0, 0, 32_768, 1));
    }

    @Test
    void callbackCopiesPointersImmediatelyAndMapsEveryNativeEvent()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        List<WindowsWebView2NativeApi.Event> events = new ArrayList<>();
        new JnaWindowsWebView2NativeApi(binding).create(request(), events::add);

        WindowsWebView2NativeApi.Kind[] kinds =
                WindowsWebView2NativeApi.Kind.values();
        for (int index = 0; index < kinds.length; index++) {
            String originalSource = "https://canvas.invalid/" + index;
            String originalPayload = "payload-" + index;
            Memory source = wide(originalSource);
            Memory payload = wide(originalPayload);
            binding.createdCallback.invoke(
                    Pointer.NULL,
                    index + 1,
                    100 + index,
                    source,
                    payload);
            source.setWideString(0, "mutated-source");
            payload.setWideString(0, "changed");

            WindowsWebView2NativeApi.Event event = events.get(index);
            assertEquals(kinds[index], event.kind());
            assertEquals(100 + index, event.statusCode());
            assertEquals(originalSource, event.source());
            assertEquals(originalPayload, event.payload());
        }
    }

    @Test
    void callbackTurnsUnknownOrOversizedNativePayloadIntoBoundedFailure()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        List<WindowsWebView2NativeApi.Event> events = new ArrayList<>();
        new JnaWindowsWebView2NativeApi(binding).create(request(), events::add);

        binding.createdCallback.invoke(
                Pointer.NULL, 999, 7, wide("source"), wide("payload"));
        binding.createdCallback.invoke(
                Pointer.NULL,
                2,
                8,
                wide("s".repeat(2_049)),
                wide("payload"));

        assertEquals(2, events.size());
        for (WindowsWebView2NativeApi.Event event : events) {
            assertEquals(WindowsWebView2NativeApi.Kind.FAILED, event.kind());
            assertEquals(E_FAIL, event.statusCode());
            assertEquals("", event.source());
            assertEquals(
                    "Native WebView2 callback violated its bounded ABI contract",
                    event.payload());
        }
    }

    @Test
    void outboundSessionCallsUseExactHandleValuesAndVisibilityEncoding()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), event -> { });

        session.postWebMessageJson("{\"kind\":\"chunk\"}");
        session.setBounds(-4, 9, 640, 480);
        session.setVisible(true);
        session.setVisible(false);
        session.requestFocus();

        assertEquals(HANDLE, binding.lastPostHandle);
        assertEquals("{\"kind\":\"chunk\"}", binding.lastPostedJson.toString());
        assertEquals(HANDLE, binding.lastBoundsHandle);
        assertEquals(List.of(-4, 9, 640, 480), binding.lastBounds);
        assertEquals(List.of(1, 0), binding.visibilityValues);
        assertEquals(List.of(HANDLE, HANDLE), binding.visibilityHandles);
        assertEquals(HANDLE, binding.lastFocusHandle);
    }

    @Test
    void outboundValidationAndEveryNativeStatusFailureAreActionable()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), event -> { });

        assertTrue(assertThrows(IOException.class,
                () -> session.postWebMessageJson(""))
                .getMessage().contains("outside its size bound"));
        assertTrue(assertThrows(IOException.class,
                () -> session.postWebMessageJson("x".repeat(1_500_001)))
                .getMessage().contains("outside its size bound"));
        assertTrue(assertThrows(IOException.class,
                () -> session.setBounds(0, 0, 0, 1))
                .getMessage().contains("outside the native range"));
        assertTrue(assertThrows(IOException.class,
                () -> session.setBounds(0, 0, 1, 32_768))
                .getMessage().contains("outside the native range"));
        assertTrue(assertThrows(IOException.class,
                () -> session.setBounds(Integer.MAX_VALUE, 0, 1, 1))
                .getMessage().contains("outside the native range"));
        assertTrue(assertThrows(IOException.class,
                () -> session.setBounds(0, Integer.MAX_VALUE, 1, 1))
                .getMessage().contains("outside the native range"));

        binding.postStatus = E_FAIL;
        assertStatusFailure(
                () -> session.postWebMessageJson("{}"),
                "post WebView2 JSON message");
        binding.boundsStatus = E_FAIL;
        assertStatusFailure(
                () -> session.setBounds(0, 0, 1, 1),
                "resize WebView2 controller");
        binding.visibleStatus = E_FAIL;
        assertStatusFailure(
                () -> session.setVisible(true),
                "change WebView2 visibility");
        binding.focusStatus = E_FAIL;
        assertStatusFailure(session::requestFocus, "focus WebView2 controller");
    }

    @Test
    void destroyIsIdempotentReleasesCallbackAndRejectsLateOperations()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        List<WindowsWebView2NativeApi.Event> events = new ArrayList<>();
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), events::add);
        JnaWindowsWebView2NativeApi.NativeEventCallback retainedCallback =
                binding.createdCallback;

        session.destroy();
        session.destroy();
        retainedCallback.invoke(
                Pointer.NULL, 2, S_OK, wide("late"), wide("ignored"));

        assertEquals(1, binding.destroyCalls);
        assertEquals(HANDLE, binding.destroyedHandle);
        assertTrue(events.isEmpty());
        assertTrue(assertThrows(IOException.class,
                () -> session.postWebMessageJson("{}"))
                .getMessage().contains("already closed"));
        assertTrue(assertThrows(IOException.class,
                () -> session.setBounds(0, 0, 1, 1))
                .getMessage().contains("already closed"));
        assertTrue(assertThrows(IOException.class,
                () -> session.setVisible(true))
                .getMessage().contains("already closed"));
        assertTrue(assertThrows(IOException.class, session::requestFocus)
                .getMessage().contains("already closed"));
    }

    @Test
    void failedDestroyRetainsCallbackUntilLateNativeCloseAndRemainsIdempotent()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        binding.destroyStatus = E_FAIL;
        List<WindowsWebView2NativeApi.Event> events = new ArrayList<>();
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), events::add);
        JnaWindowsWebView2NativeApi.NativeEventCallback retainedCallback =
                binding.createdCallback;

        assertStatusFailure(session::destroy, "destroy WebView2 controller");
        session.destroy();
        retainedCallback.invoke(
                Pointer.NULL, 2, S_OK, wide("late"), wide("retained"));
        retainedCallback.invoke(
                Pointer.NULL, 6, S_OK, Pointer.NULL, Pointer.NULL);
        retainedCallback.invoke(
                Pointer.NULL, 2, S_OK, wide("after-close"), wide("ignored"));

        assertEquals(1, binding.destroyCalls);
        assertEquals(List.of(
                new WindowsWebView2NativeApi.Event(
                        WindowsWebView2NativeApi.Kind.WEB_MESSAGE,
                        S_OK, "late", "retained"),
                new WindowsWebView2NativeApi.Event(
                        WindowsWebView2NativeApi.Kind.CLOSED,
                        S_OK, "", "")), events);
    }

    private static WindowsWebView2NativeApi.CreateRequest request() {
        WebCanvasOriginPolicy originPolicy = new WebCanvasOriginPolicy(
                NONCE,
                GENERATION,
                Set.of("z.js", "index.html", "a.css"));
        Map<String, WebCanvasArtifactContract.ArtifactFile> files = Map.of(
                "a.css", new WebCanvasArtifactContract.ArtifactFile(
                        "a.css", 7, "a".repeat(64)),
                "index.html", new WebCanvasArtifactContract.ArtifactFile(
                        "index.html", 11, "b".repeat(64)),
                "z.js", new WebCanvasArtifactContract.ArtifactFile(
                        "z.js", 13, "c".repeat(64)));
        Path root = Path.of(".").toAbsolutePath().normalize();
        return new WindowsWebView2NativeApi.CreateRequest(
                HWND,
                -12,
                34,
                1280,
                720,
                root.resolve("user-data"),
                root.resolve("content"),
                originPolicy,
                files,
                NONCE);
    }

    private static Memory wide(String value) {
        Memory memory = new Memory(
                (long) (value.length() + 1) * Native.WCHAR_SIZE);
        memory.setWideString(0, value);
        return memory;
    }

    private static WindowsWebView2NativeApi.CreateRequest requestWithBounds(
            WindowsWebView2NativeApi.CreateRequest source,
            int x,
            int y,
            int width,
            int height) {
        return new WindowsWebView2NativeApi.CreateRequest(
                source.parentWindow(), x, y, width, height,
                source.userDataFolder(), source.contentRoot(),
                source.originPolicy(), source.artifactFiles(),
                source.sessionNonce());
    }

    private static void assertMalformedRuntime(FakeBinding binding) {
        IOException failure = assertThrows(IOException.class,
                () -> new JnaWindowsWebView2NativeApi(binding).runtimeVersion());
        assertTrue(failure.getMessage().contains("malformed version"));
    }

    private static void assertStatusFailure(
            IoOperation operation, String expectedOperation) {
        IOException failure = assertThrows(IOException.class, operation::run);
        assertTrue(failure.getMessage().contains(expectedOperation));
        assertTrue(failure.getMessage().contains("HRESULT 0x80004005"));
    }

    @FunctionalInterface
    private interface IoOperation {
        void run() throws IOException;
    }

    private static final class FakeBinding
            implements JnaWindowsWebView2NativeApi.Binding {
        private String runtimeVersion = "151.0.4129.107";
        private int runtimeProbeStatus = ERROR_INSUFFICIENT_BUFFER_HRESULT;
        private int runtimeReadStatus = S_OK;
        private int runtimeProbeRequiredOverride = -1;
        private int runtimeReadRequiredOverride = -1;
        private boolean omitRuntimeTerminator;
        private int runtimeProbeCalls;
        private int runtimeReadCalls;
        private int lastRuntimeBufferCharacters;

        private int createStatus = S_OK;
        private Pointer createdHandle = HANDLE;
        private int createCalls;
        private JnaWindowsWebView2NativeApi.NativeOptions createdOptions;
        private JnaWindowsWebView2NativeApi.NativeEventCallback createdCallback;
        private Pointer createdContext;

        private int postStatus = S_OK;
        private Pointer lastPostHandle;
        private WString lastPostedJson;

        private int boundsStatus = S_OK;
        private Pointer lastBoundsHandle;
        private List<Integer> lastBounds = List.of();

        private int visibleStatus = S_OK;
        private final List<Pointer> visibilityHandles = new ArrayList<>();
        private final List<Integer> visibilityValues = new ArrayList<>();

        private int focusStatus = S_OK;
        private Pointer lastFocusHandle;

        private int destroyStatus = S_OK;
        private int destroyCalls;
        private Pointer destroyedHandle;

        @Override
        public int nbwv2_get_abi_version() {
            return 2;
        }

        @Override
        public int nbwv2_get_runtime_version(
                char[] buffer,
                int bufferCharacters,
                IntByReference requiredCharacters) {
            if (buffer == null) {
                runtimeProbeCalls++;
                requiredCharacters.setValue(runtimeProbeRequiredOverride >= 0
                        ? runtimeProbeRequiredOverride
                        : runtimeVersion.length() + 1);
                return runtimeProbeStatus;
            }
            runtimeReadCalls++;
            lastRuntimeBufferCharacters = bufferCharacters;
            Arrays.fill(buffer, omitRuntimeTerminator ? 'x' : '\0');
            int copied = Math.min(runtimeVersion.length(), buffer.length);
            runtimeVersion.getChars(0, copied, buffer, 0);
            if (!omitRuntimeTerminator && copied < buffer.length) {
                buffer[copied] = '\0';
            }
            requiredCharacters.setValue(runtimeReadRequiredOverride >= 0
                    ? runtimeReadRequiredOverride : copied + 1);
            return runtimeReadStatus;
        }

        @Override
        public int nbwv2_create(
                JnaWindowsWebView2NativeApi.NativeOptions options,
                JnaWindowsWebView2NativeApi.NativeEventCallback callback,
                Pointer callbackContext,
                PointerByReference hostHandle) {
            createCalls++;
            createdOptions = options;
            createdCallback = callback;
            createdContext = callbackContext;
            hostHandle.setValue(createdHandle);
            return createStatus;
        }

        @Override
        public int nbwv2_post_web_message_json(Pointer hostHandle, WString json) {
            lastPostHandle = hostHandle;
            lastPostedJson = json;
            return postStatus;
        }

        @Override
        public int nbwv2_set_bounds(
                Pointer hostHandle,
                int x,
                int y,
                int width,
                int height) {
            lastBoundsHandle = hostHandle;
            lastBounds = List.of(x, y, width, height);
            return boundsStatus;
        }

        @Override
        public int nbwv2_set_visible(Pointer hostHandle, int visible) {
            visibilityHandles.add(hostHandle);
            visibilityValues.add(visible);
            return visibleStatus;
        }

        @Override
        public int nbwv2_request_focus(Pointer hostHandle) {
            lastFocusHandle = hostHandle;
            return focusStatus;
        }

        @Override
        public int nbwv2_destroy(Pointer hostHandle) {
            destroyCalls++;
            destroyedHandle = hostHandle;
            return destroyStatus;
        }
    }
}
