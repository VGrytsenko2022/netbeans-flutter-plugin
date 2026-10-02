package io.github.vgrytsenko2022.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JnaWindowsWebView2NativeApiTest {
    private static final int S_OK = 0;
    private static final int S_FALSE = 1;
    private static final int E_FAIL = 0x80004005;
    private static final int ERROR_TIMEOUT_HRESULT = 0x800705B4;
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
        assertEquals(3, binding.createdOptions.abi_version);
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
    void createFailureCarriesExactReleaseOrRetainedSessionEvidence()
            throws Exception {
        FakeBinding released = new FakeBinding();
        released.createStatus = E_FAIL;
        released.createdHandle = Pointer.NULL;
        WindowsWebView2NativeApi.CreateException nativeFailure = assertThrows(
                WindowsWebView2NativeApi.CreateException.class,
                () -> new JnaWindowsWebView2NativeApi(released)
                        .create(request(), event -> { }));
        assertTrue(nativeFailure.getMessage().contains(
                "create WebView2 environment/controller"));
        assertTrue(nativeFailure.getMessage().contains("HRESULT 0x80004005"));
        assertTrue(nativeFailure.releaseConfirmed());
        assertTrue(nativeFailure.retainedSession().isEmpty());

        FakeBinding retained = new FakeBinding();
        retained.createStatus = E_FAIL;
        WindowsWebView2NativeApi.CreateException retainedFailure = assertThrows(
                WindowsWebView2NativeApi.CreateException.class,
                () -> new JnaWindowsWebView2NativeApi(retained)
                        .create(request(), event -> { }));
        assertFalse(retainedFailure.releaseConfirmed());
        WindowsWebView2NativeApi.NativeSession retainedSession =
                retainedFailure.retainedSession().orElseThrow();

        WindowsWebView2NativeApi.DestroyResult result = retainedSession.destroy(
                Duration.ofSeconds(1));
        assertTrue(result.udfReleaseConfirmed());
        assertEquals(1, retained.destroyCalls);
        assertEquals(List.of(HANDLE), retained.destroyInputHandles);

        FakeBinding missing = new FakeBinding();
        missing.createdHandle = Pointer.NULL;
        WindowsWebView2NativeApi.CreateException missingHandle = assertThrows(
                WindowsWebView2NativeApi.CreateException.class,
                () -> new JnaWindowsWebView2NativeApi(missing)
                        .create(request(), event -> { }));
        assertTrue(missingHandle.getMessage().contains(
                "success without a session handle"));
        assertFalse(missingHandle.releaseConfirmed());
        assertTrue(missingHandle.retainedSession().isEmpty());
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
    void destroyResultHasExactAbiV3Size() {
        assertEquals(32,
                new JnaWindowsWebView2NativeApi.NativeDestroyResult().size());
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
    void parentReleaseUsesExpectedHwndAndBoundedTimeout() throws Exception {
        FakeBinding binding = new FakeBinding();
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), event -> { });

        session.prepareParentRelease(HWND, Duration.ofMillis(321));

        assertEquals(1, binding.parentReleaseCalls);
        assertEquals(HANDLE, binding.parentReleaseHandle);
        assertEquals(HWND, Pointer.nativeValue(binding.expectedParentWindow));
        assertEquals(321, binding.parentReleaseTimeoutMilliseconds);

        binding.parentReleaseStatus = E_FAIL;
        assertStatusFailure(
                () -> session.prepareParentRelease(HWND, Duration.ofSeconds(1)),
                "prepare WebView2 controller for AWT parent release");
    }

    @Test
    void nativeTimeoutsAndExpectedParentAreValidatedBeforeCrossingAbi()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), event -> { });

        assertTrue(assertThrows(IOException.class,
                () -> session.prepareParentRelease(0, Duration.ofSeconds(1)))
                .getMessage().contains("HWND must be non-zero"));
        assertInvalidTimeout(() -> session.prepareParentRelease(HWND, null));
        assertInvalidTimeout(() -> session.prepareParentRelease(HWND, Duration.ZERO));
        assertInvalidTimeout(() -> session.prepareParentRelease(
                HWND, Duration.ofNanos(1)));
        assertInvalidTimeout(() -> session.prepareParentRelease(
                HWND, Duration.ofMillis((long) Integer.MAX_VALUE + 1)));
        assertInvalidTimeout(() -> session.destroy(Duration.ofMillis(-1)));

        assertEquals(0, binding.parentReleaseCalls);
        assertEquals(0, binding.destroyCalls);
    }

    @Test
    void destroyTimeoutRetainsSameHandleForRetryThenReleasesExactlyOnce()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        binding.destroyResponses = List.of(
                DestroyResponse.failure(ERROR_TIMEOUT_HRESULT, HANDLE),
                DestroyResponse.confirmed());
        List<WindowsWebView2NativeApi.Event> events = new ArrayList<>();
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), events::add);
        JnaWindowsWebView2NativeApi.NativeEventCallback retainedCallback =
                binding.createdCallback;

        IOException timeout = assertThrows(IOException.class,
                () -> session.destroy(Duration.ofMillis(250)));
        assertTrue(timeout.getMessage().contains("HRESULT 0x800705B4"));
        retainedCallback.invoke(
                Pointer.NULL, 2, S_OK, wide("late"), wide("retained"));
        retainedCallback.invoke(
                Pointer.NULL, 6, S_OK, Pointer.NULL, Pointer.NULL);
        retainedCallback.invoke(
                Pointer.NULL, 2, S_OK, wide("after-closed"), wide("still-retained"));

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

        WindowsWebView2NativeApi.DestroyResult result =
                session.destroy(Duration.ofSeconds(2));
        WindowsWebView2NativeApi.DestroyResult cached =
                session.destroy(Duration.ofSeconds(3));
        retainedCallback.invoke(
                Pointer.NULL, 2, S_OK, wide("late"), wide("ignored"));

        assertEquals(2, binding.destroyCalls);
        assertEquals(List.of(HANDLE, HANDLE), binding.destroyInputHandles);
        assertEquals(List.of(250, 2_000), binding.destroyTimeouts);
        assertEquals(result, cached);
        assertTrue(result.udfReleaseConfirmed());
        assertEquals(4_242L, result.expectedBrowserProcessId());
        assertEquals(4_242L, result.observedBrowserProcessId());
        assertEquals(0, result.browserExitKind());
        assertEquals(S_OK, result.terminalHresult());
        assertEquals(List.of(
                new WindowsWebView2NativeApi.Event(
                        WindowsWebView2NativeApi.Kind.WEB_MESSAGE,
                        S_OK, "late", "retained"),
                new WindowsWebView2NativeApi.Event(
                        WindowsWebView2NativeApi.Kind.CLOSED,
                        S_OK, "", ""),
                new WindowsWebView2NativeApi.Event(
                        WindowsWebView2NativeApi.Kind.WEB_MESSAGE,
                        S_OK, "after-closed", "still-retained")), events);
    }

    @Test
    void indeterminateDestroyInvocationIsTerminalAndNeverRetriesStaleHandle()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        binding.destroyInvocationFailure = new IllegalStateException(
                "simulated JNA result-read failure");
        List<WindowsWebView2NativeApi.Event> events = new ArrayList<>();
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), events::add);

        IOException first = assertThrows(IOException.class,
                () -> session.destroy(Duration.ofSeconds(1)));
        assertTrue(first.getMessage().contains("indeterminate ownership"));
        assertTrue(first.getCause() instanceof IllegalStateException);
        binding.destroyInvocationFailure = null;
        IOException cached = assertThrows(IOException.class,
                () -> session.destroy(Duration.ofSeconds(1)));
        binding.createdCallback.invoke(
                Pointer.NULL, 2, S_OK, wide("late"), wide("retained"));

        assertEquals(first.getMessage(), cached.getMessage());
        assertEquals(1, binding.destroyCalls);
        assertEquals(List.of(new WindowsWebView2NativeApi.Event(
                WindowsWebView2NativeApi.Kind.WEB_MESSAGE,
                S_OK, "late", "retained")), events);
    }

    @Test
    void sFalseReleasesNativeOwnershipButMapsUnconfirmedUserDataFolder()
            throws Exception {
        FakeBinding binding = new FakeBinding();
        binding.destroyResponses = List.of(DestroyResponse.unconfirmed());
        List<WindowsWebView2NativeApi.Event> events = new ArrayList<>();
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), events::add);
        JnaWindowsWebView2NativeApi.NativeEventCallback retainedCallback =
                binding.createdCallback;

        WindowsWebView2NativeApi.DestroyResult result =
                session.destroy(Duration.ofSeconds(1));
        retainedCallback.invoke(
                Pointer.NULL, 2, S_OK, wide("after-close"), wide("ignored"));

        assertEquals(1, binding.destroyCalls);
        assertTrue(!result.udfReleaseConfirmed());
        assertTrue(events.isEmpty());
    }

    @Test
    void sOkAcceptsProofThatNoBrowserProcessStarted() throws Exception {
        FakeBinding binding = new FakeBinding();
        binding.destroyResponses = List.of(DestroyResponse.noBrowserStarted());
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), event -> { });

        WindowsWebView2NativeApi.DestroyResult result =
                session.destroy(Duration.ofSeconds(1));

        assertTrue((result.flags()
                & WindowsWebView2NativeApi.DestroyResult.NO_BROWSER_STARTED) != 0);
        assertEquals(0L, result.expectedBrowserProcessId());
        assertEquals(0L, result.observedBrowserProcessId());
    }

    @Test
    void malformedSuccessfulDestroyFailsClosedWithoutReusingHandle()
            throws Exception {
        FakeBinding retainedHandle = new FakeBinding();
        retainedHandle.destroyResponses = List.of(
                DestroyResponse.confirmed().withReturnedHandle(HANDLE));
        WindowsWebView2NativeApi.NativeSession retainedSession =
                new JnaWindowsWebView2NativeApi(retainedHandle)
                        .create(request(), event -> { });

        IOException retainedFailure = assertThrows(IOException.class,
                () -> retainedSession.destroy(Duration.ofSeconds(1)));
        assertTrue(retainedFailure.getMessage().contains(
                "reported success but retained its native handle"));
        assertThrows(IOException.class,
                () -> retainedSession.destroy(Duration.ofSeconds(1)));
        assertEquals(1, retainedHandle.destroyCalls);

        FakeBinding missingProof = new FakeBinding();
        missingProof.destroyResponses = List.of(
                DestroyResponse.confirmed().withoutCallbackRetired());
        WindowsWebView2NativeApi.NativeSession missingProofSession =
                new JnaWindowsWebView2NativeApi(missingProof)
                        .create(request(), event -> { });

        IOException proofFailure = assertThrows(IOException.class,
                () -> missingProofSession.destroy(Duration.ofSeconds(1)));
        assertTrue(proofFailure.getMessage().contains(
                "did not prove parent, native thread, and callback release"));
        assertThrows(IOException.class,
                () -> missingProofSession.destroy(Duration.ofSeconds(1)));
        assertEquals(1, missingProof.destroyCalls);
    }

    @Test
    void malformedDestroyEvidenceIsRejectedBeforeItCanAuthorizeUserDataDeletion()
            throws Exception {
        List<DestroyResponse> malformed = List.of(
                DestroyResponse.confirmed().withTerminalHresult(S_FALSE),
                DestroyResponse.confirmed().withObservedBrowserProcessId(7_777),
                DestroyResponse.confirmed().withoutUdfIdentity(),
                DestroyResponse.noBrowserStarted().withExpectedBrowserProcessId(4_242),
                DestroyResponse.noBrowserStarted().withUdfIdentity());

        for (DestroyResponse response : malformed) {
            FakeBinding binding = new FakeBinding();
            binding.destroyResponses = List.of(response);
            WindowsWebView2NativeApi.NativeSession session =
                    new JnaWindowsWebView2NativeApi(binding)
                            .create(request(), event -> { });

            assertThrows(IOException.class,
                    () -> session.destroy(Duration.ofSeconds(1)));
            assertThrows(IOException.class,
                    () -> session.destroy(Duration.ofSeconds(1)));
            assertEquals(1, binding.destroyCalls);
        }
    }

    @Test
    void matchingFailedBrowserExitStillProvesUserDataRelease() throws Exception {
        FakeBinding binding = new FakeBinding();
        binding.destroyResponses = List.of(
                DestroyResponse.confirmed().withBrowserExitKind(1));
        WindowsWebView2NativeApi.NativeSession session =
                new JnaWindowsWebView2NativeApi(binding)
                        .create(request(), event -> { });

        WindowsWebView2NativeApi.DestroyResult result =
                session.destroy(Duration.ofSeconds(1));

        assertTrue(result.udfReleaseConfirmed());
        assertEquals(1, result.browserExitKind());
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

    private static void assertInvalidTimeout(IoOperation operation) {
        IOException failure = assertThrows(IOException.class, operation::run);
        assertTrue(failure.getMessage().contains("timeout"));
    }

    @FunctionalInterface
    private interface IoOperation {
        void run() throws IOException;
    }

    private record DestroyResponse(
            int status,
            Pointer returnedHandle,
            int structSizeOverride,
            int flags,
            int expectedBrowserProcessId,
            int observedBrowserProcessId,
            int browserExitKind,
            int terminalHresult,
            int reserved0,
            int reserved1) {
        private static DestroyResponse failure(int status, Pointer handle) {
            return new DestroyResponse(
                    status, handle, -1, 0, 0, 0, 0, status, 0, 0);
        }

        private static DestroyResponse confirmed() {
            return new DestroyResponse(
                    S_OK,
                    Pointer.NULL,
                    -1,
                    WindowsWebView2NativeApi.DestroyResult.PARENT_RELEASED
                    | WindowsWebView2NativeApi.DestroyResult.UDF_IDENTITY_VERIFIED
                    | WindowsWebView2NativeApi.DestroyResult.CONTROLLER_CLOSED
                    | WindowsWebView2NativeApi.DestroyResult.BROWSER_EXIT_OBSERVED
                    | WindowsWebView2NativeApi.DestroyResult.PID_MATCHED
                    | WindowsWebView2NativeApi.DestroyResult.UDF_RELEASE_CONFIRMED
                    | WindowsWebView2NativeApi.DestroyResult.THREAD_JOINED
                    | WindowsWebView2NativeApi.DestroyResult.CALLBACK_RETIRED,
                    4_242,
                    4_242,
                    0,
                    S_OK,
                    0,
                    0);
        }

        private static DestroyResponse unconfirmed() {
            return new DestroyResponse(
                    S_FALSE,
                    Pointer.NULL,
                    -1,
                    WindowsWebView2NativeApi.DestroyResult.PARENT_RELEASED
                    | WindowsWebView2NativeApi.DestroyResult.UDF_IDENTITY_VERIFIED
                    | WindowsWebView2NativeApi.DestroyResult.CONTROLLER_CLOSED
                    | WindowsWebView2NativeApi.DestroyResult.BROWSER_EXIT_OBSERVED
                    | WindowsWebView2NativeApi.DestroyResult.PID_MATCHED
                    | WindowsWebView2NativeApi.DestroyResult.THREAD_JOINED
                    | WindowsWebView2NativeApi.DestroyResult.CALLBACK_RETIRED,
                    4_242,
                    4_242,
                    0,
                    S_FALSE,
                    0,
                    0);
        }

        private static DestroyResponse noBrowserStarted() {
            return new DestroyResponse(
                    S_OK,
                    Pointer.NULL,
                    -1,
                    WindowsWebView2NativeApi.DestroyResult.PARENT_RELEASED
                    | WindowsWebView2NativeApi.DestroyResult.THREAD_JOINED
                    | WindowsWebView2NativeApi.DestroyResult.CALLBACK_RETIRED
                    | WindowsWebView2NativeApi.DestroyResult.NO_BROWSER_STARTED,
                    0,
                    0,
                    0,
                    S_OK,
                    0,
                    0);
        }

        private DestroyResponse withReturnedHandle(Pointer value) {
            return new DestroyResponse(
                    status, value, structSizeOverride, flags,
                    expectedBrowserProcessId, observedBrowserProcessId,
                    browserExitKind, terminalHresult, reserved0, reserved1);
        }

        private DestroyResponse withoutCallbackRetired() {
            return new DestroyResponse(
                    status, returnedHandle, structSizeOverride,
                    flags & ~WindowsWebView2NativeApi.DestroyResult.CALLBACK_RETIRED,
                    expectedBrowserProcessId, observedBrowserProcessId,
                    browserExitKind, terminalHresult, reserved0, reserved1);
        }

        private DestroyResponse withTerminalHresult(int value) {
            return new DestroyResponse(
                    status, returnedHandle, structSizeOverride, flags,
                    expectedBrowserProcessId, observedBrowserProcessId,
                    browserExitKind, value, reserved0, reserved1);
        }

        private DestroyResponse withExpectedBrowserProcessId(int value) {
            return new DestroyResponse(
                    status, returnedHandle, structSizeOverride, flags,
                    value, observedBrowserProcessId,
                    browserExitKind, terminalHresult, reserved0, reserved1);
        }

        private DestroyResponse withObservedBrowserProcessId(int value) {
            return new DestroyResponse(
                    status, returnedHandle, structSizeOverride, flags,
                    expectedBrowserProcessId, value,
                    browserExitKind, terminalHresult, reserved0, reserved1);
        }

        private DestroyResponse withBrowserExitKind(int value) {
            return new DestroyResponse(
                    status, returnedHandle, structSizeOverride, flags,
                    expectedBrowserProcessId, observedBrowserProcessId,
                    value, terminalHresult, reserved0, reserved1);
        }

        private DestroyResponse withoutUdfIdentity() {
            return new DestroyResponse(
                    status, returnedHandle, structSizeOverride,
                    flags & ~WindowsWebView2NativeApi.DestroyResult.UDF_IDENTITY_VERIFIED,
                    expectedBrowserProcessId, observedBrowserProcessId,
                    browserExitKind, terminalHresult, reserved0, reserved1);
        }

        private DestroyResponse withUdfIdentity() {
            return new DestroyResponse(
                    status, returnedHandle, structSizeOverride,
                    flags | WindowsWebView2NativeApi.DestroyResult.UDF_IDENTITY_VERIFIED,
                    expectedBrowserProcessId, observedBrowserProcessId,
                    browserExitKind, terminalHresult, reserved0, reserved1);
        }
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

        private int parentReleaseStatus = S_OK;
        private int parentReleaseCalls;
        private Pointer parentReleaseHandle;
        private Pointer expectedParentWindow;
        private int parentReleaseTimeoutMilliseconds;

        private List<DestroyResponse> destroyResponses =
                List.of(DestroyResponse.confirmed());
        private RuntimeException destroyInvocationFailure;
        private int destroyCalls;
        private final List<Pointer> destroyInputHandles = new ArrayList<>();
        private final List<Integer> destroyTimeouts = new ArrayList<>();

        @Override
        public int nbwv2_get_abi_version() {
            return 3;
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
        public int nbwv2_prepare_parent_release(
                Pointer hostHandle,
                Pointer expectedParent,
                int timeoutMilliseconds) {
            parentReleaseCalls++;
            parentReleaseHandle = hostHandle;
            expectedParentWindow = expectedParent;
            parentReleaseTimeoutMilliseconds = timeoutMilliseconds;
            return parentReleaseStatus;
        }

        @Override
        public int nbwv2_destroy(
                PointerByReference hostHandle,
                int timeoutMilliseconds,
                JnaWindowsWebView2NativeApi.NativeDestroyResult result) {
            int responseIndex = Math.min(destroyCalls, destroyResponses.size() - 1);
            DestroyResponse response = destroyResponses.get(responseIndex);
            destroyCalls++;
            destroyInputHandles.add(hostHandle.getValue());
            destroyTimeouts.add(timeoutMilliseconds);
            hostHandle.setValue(response.returnedHandle());
            if (destroyInvocationFailure != null) {
                throw destroyInvocationFailure;
            }
            result.struct_size = response.structSizeOverride() >= 0
                    ? response.structSizeOverride() : result.size();
            result.flags = response.flags();
            result.expected_browser_pid = response.expectedBrowserProcessId();
            result.observed_browser_pid = response.observedBrowserProcessId();
            result.browser_exit_kind = response.browserExitKind();
            result.terminal_hresult = response.terminalHresult();
            result.reserved[0] = response.reserved0();
            result.reserved[1] = response.reserved1();
            result.write();
            return response.status();
        }
    }
}
