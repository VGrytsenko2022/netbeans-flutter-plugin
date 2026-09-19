package io.github.vgrytsenko2022.plugin.designer.canvas;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Lazy JNA binding for the native COM-owning WebView2 adapter. */
final class JnaWindowsWebView2NativeApi implements WindowsWebView2NativeApi {
    private static final int ABI_VERSION = 3;
    private static final int ERROR_INSUFFICIENT_BUFFER_HRESULT = 0x8007007A;
    private static final int S_OK = 0;
    private static final int S_FALSE = 1;
    private static final int MAXIMUM_RUNTIME_VERSION_CHARACTERS = 256;
    private static final Set<JnaSession> FAILED_DESTROY_SESSIONS =
            ConcurrentHashMap.newKeySet();
    private static final Set<CallbackState> QUARANTINED_CREATE_CALLBACKS =
            ConcurrentHashMap.newKeySet();

    private final Binding binding;

    static JnaWindowsWebView2NativeApi loadPackaged() throws IOException {
        WebView2NativeBundle.ExtractedBundle bundle = WebView2NativeBundle.packaged().extract();
        try {
            System.load(bundle.loader().toString());
            Binding binding = Native.load(
                    bundle.adapter().toString(), Binding.class,
                    W32APIOptions.UNICODE_OPTIONS);
            int abi = binding.nbwv2_get_abi_version();
            if (abi != ABI_VERSION) {
                throw new IOException("WebView2 native adapter ABI mismatch: expected "
                        + ABI_VERSION + ", got " + Integer.toUnsignedString(abi));
            }
            return new JnaWindowsWebView2NativeApi(binding);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError failure) {
            throw new IOException(
                    "Cannot load the Windows x64 WebView2 native adapter: "
                    + boundedFailure(failure), failure);
        }
    }

    JnaWindowsWebView2NativeApi(Binding binding) {
        this.binding = Objects.requireNonNull(binding, "binding");
    }

    @Override
    public String runtimeVersion() throws IOException {
        IntByReference required = new IntByReference();
        int status = binding.nbwv2_get_runtime_version(null, 0, required);
        int characters = required.getValue();
        if (status != ERROR_INSUFFICIENT_BUFFER_HRESULT) {
            requireSuccess(status, "detect installed WebView2 Runtime");
        }
        if (characters < 2 || characters > MAXIMUM_RUNTIME_VERSION_CHARACTERS) {
            throw new IOException("Installed WebView2 Runtime returned an invalid version length");
        }
        char[] buffer = new char[characters];
        status = binding.nbwv2_get_runtime_version(buffer, buffer.length, required);
        requireSuccess(status, "read installed WebView2 Runtime version");
        int terminator = 0;
        while (terminator < buffer.length && buffer[terminator] != '\0') {
            terminator++;
        }
        String version = new String(buffer, 0, terminator);
        if (terminator == 0 || terminator >= buffer.length
                || required.getValue() != terminator + 1
                || !version.matches("[0-9]+(?:\\.[0-9]+){2,4}")) {
            throw new IOException("Installed WebView2 Runtime returned a malformed version");
        }
        return version;
    }

    @Override
    public NativeSession create(CreateRequest request, Listener listener) throws IOException {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");
        NativeOptions nativeOptions = new NativeOptions(request);
        CallbackState callback = new CallbackState(listener);
        PointerByReference handle = new PointerByReference();
        final int status;
        try {
            status = binding.nbwv2_create(
                    nativeOptions, callback, Pointer.NULL, handle);
        } catch (RuntimeException | LinkageError failure) {
            Pointer uncertainHandle = handle.getValue();
            if (hasHandle(uncertainHandle)) {
                JnaSession retained = new JnaSession(
                        binding, uncertainHandle, callback);
                FAILED_DESTROY_SESSIONS.add(retained);
                throw new CreateException(
                        "WebView2 native create call failed after publishing a session handle",
                        retained, false, failure);
            }
            QUARANTINED_CREATE_CALLBACKS.add(callback);
            throw new CreateException(
                    "WebView2 native create call failed without release confirmation",
                    null, false, failure);
        }
        Pointer pointer = handle.getValue();
        if (status < 0) {
            String message = statusFailureMessage(
                    status, "create WebView2 environment/controller");
            if (hasHandle(pointer)) {
                JnaSession retained = new JnaSession(binding, pointer, callback);
                FAILED_DESTROY_SESSIONS.add(retained);
                throw new CreateException(message, retained, false);
            }
            // ABI v3 guarantees that a failed create with a null output handle
            // has joined its thread and retired the callback/browser lifetime.
            callback.release();
            throw new CreateException(message, null, true);
        }
        if (status != S_OK) {
            if (hasHandle(pointer)) {
                JnaSession retained = new JnaSession(binding, pointer, callback);
                FAILED_DESTROY_SESSIONS.add(retained);
                throw new CreateException(
                        "WebView2 native create returned unsupported success HRESULT 0x"
                        + String.format(Locale.ROOT, "%08X", status),
                        retained, false);
            }
            QUARANTINED_CREATE_CALLBACKS.add(callback);
            throw new CreateException(
                    "WebView2 native create returned unsupported success HRESULT 0x"
                    + String.format(Locale.ROOT, "%08X", status)
                    + " without release confirmation",
                    null, false);
        }
        if (!hasHandle(pointer)) {
            QUARANTINED_CREATE_CALLBACKS.add(callback);
            throw new CreateException(
                    "WebView2 native adapter returned success without a session handle "
                    + "or release confirmation",
                    null, false);
        }
        return new JnaSession(binding, pointer, callback);
    }

    private static boolean hasHandle(Pointer pointer) {
        return pointer != null && Pointer.nativeValue(pointer) != 0;
    }

    private static final class JnaSession implements NativeSession {
        private final Binding binding;
        private final CallbackState callback;
        private Pointer handle;
        private SessionState state = SessionState.OPEN;
        private DestroyResult releasedResult;
        private IOException terminalFailure;

        private JnaSession(Binding binding, Pointer handle, CallbackState callback) {
            this.binding = binding;
            this.handle = handle;
            this.callback = callback;
        }

        @Override
        public synchronized void postWebMessageJson(String json) throws IOException {
            String value = Objects.requireNonNull(json, "json");
            if (value.isEmpty() || value.length() > 1_500_000) {
                throw new IOException("WebView2 outbound JSON is outside its size bound");
            }
            requireSuccess(binding.nbwv2_post_web_message_json(
                    requireOpenHandle(), new WString(value)), "post WebView2 JSON message");
        }

        @Override
        public synchronized void setBounds(
                int x, int y, int width, int height) throws IOException {
            if (width <= 0 || height <= 0 || width > 32_767 || height > 32_767
                    || x > Integer.MAX_VALUE - width
                    || y > Integer.MAX_VALUE - height) {
                throw new IOException("WebView2 bounds are outside the native range");
            }
            requireSuccess(binding.nbwv2_set_bounds(
                    requireOpenHandle(), x, y, width, height), "resize WebView2 controller");
        }

        @Override
        public synchronized void setVisible(boolean visible) throws IOException {
            requireSuccess(binding.nbwv2_set_visible(
                    requireOpenHandle(), visible ? 1 : 0), "change WebView2 visibility");
        }

        @Override
        public synchronized void requestFocus() throws IOException {
            requireSuccess(binding.nbwv2_request_focus(
                    requireOpenHandle()), "focus WebView2 controller");
        }

        @Override
        public synchronized void prepareParentRelease(
                long expectedParentWindow, Duration timeout) throws IOException {
            if (expectedParentWindow == 0) {
                throw new IOException("WebView2 expected parent HWND must be non-zero");
            }
            int timeoutMilliseconds = boundedTimeoutMilliseconds(timeout);
            requireSuccess(binding.nbwv2_prepare_parent_release(
                    requireOpenHandle(),
                    Pointer.createConstant(expectedParentWindow),
                    timeoutMilliseconds),
                    "prepare WebView2 controller for AWT parent release");
        }

        @Override
        public synchronized DestroyResult destroy(Duration timeout) throws IOException {
            if (state == SessionState.RELEASED) {
                return releasedResult;
            }
            if (state == SessionState.TERMINAL) {
                throw terminalFailure;
            }
            int timeoutMilliseconds = boundedTimeoutMilliseconds(timeout);
            Pointer current = handle;
            state = SessionState.CLOSING;
            PointerByReference nativeHandle = new PointerByReference(current);
            NativeDestroyResult nativeResult = new NativeDestroyResult();
            nativeResult.struct_size = nativeResult.size();
            nativeResult.write();
            boolean retryAuthorized = false;
            try {
                int status = binding.nbwv2_destroy(
                        nativeHandle, timeoutMilliseconds, nativeResult);
                nativeResult.read();
                Pointer returnedHandle = nativeHandle.getValue();
                if (status < 0) {
                    requireSameLiveHandle(current, returnedHandle,
                            "failed WebView2 destroy changed its native handle");
                    FAILED_DESTROY_SESSIONS.add(this);
                    retryAuthorized = true;
                    throw new IOException(statusFailureMessage(
                            status, "destroy WebView2 controller"));
                }

                DestroyResult result = validateSuccessfulDestroy(
                        status, returnedHandle, nativeResult);
                handle = null;
                state = SessionState.RELEASED;
                releasedResult = result;
                FAILED_DESTROY_SESSIONS.remove(this);
                callback.release();
                return result;
            } catch (IOException failure) {
                // A failed native teardown may still own and invoke this JNA
                // function pointer. Only an explicit failing HRESULT that
                // preserves the exact non-zero handle authorizes a retry.
                FAILED_DESTROY_SESSIONS.add(this);
                if (!retryAuthorized) {
                    MalformedDestroyException terminal =
                            failure instanceof MalformedDestroyException malformed
                                    ? malformed
                                    : new MalformedDestroyException(
                                            "WebView2 destroy ownership became indeterminate",
                                            failure);
                    state = SessionState.TERMINAL;
                    terminalFailure = terminal;
                    throw terminal;
                }
                throw failure;
            } catch (RuntimeException | LinkageError failure) {
                FAILED_DESTROY_SESSIONS.add(this);
                MalformedDestroyException terminal = new MalformedDestroyException(
                        "WebView2 destroy invocation ended with indeterminate ownership",
                        failure);
                state = SessionState.TERMINAL;
                terminalFailure = terminal;
                throw terminal;
            } catch (Error failure) {
                FAILED_DESTROY_SESSIONS.add(this);
                state = SessionState.TERMINAL;
                terminalFailure = new MalformedDestroyException(
                        "WebView2 destroy invocation ended with indeterminate ownership",
                        failure);
                throw failure;
            }
        }

        private static DestroyResult validateSuccessfulDestroy(
                int status,
                Pointer returnedHandle,
                NativeDestroyResult nativeResult) throws IOException {
            if (status != S_OK && status != S_FALSE) {
                throw new MalformedDestroyException(
                        "WebView2 destroy returned unsupported success HRESULT 0x"
                        + String.format(Locale.ROOT, "%08X", status));
            }
            if (returnedHandle != null && Pointer.nativeValue(returnedHandle) != 0) {
                throw new MalformedDestroyException(
                        "WebView2 destroy reported success but retained its native handle");
            }
            if (nativeResult.struct_size != nativeResult.size()
                    || nativeResult.reserved[0] != 0
                    || nativeResult.reserved[1] != 0) {
                throw new MalformedDestroyException(
                        "WebView2 destroy returned a malformed ABI v3 result structure");
            }

            final DestroyResult result;
            try {
                result = new DestroyResult(
                        nativeResult.flags,
                        Integer.toUnsignedLong(nativeResult.expected_browser_pid),
                        Integer.toUnsignedLong(nativeResult.observed_browser_pid),
                        nativeResult.browser_exit_kind,
                        nativeResult.terminal_hresult);
            } catch (IllegalArgumentException malformed) {
                throw new MalformedDestroyException(
                        "WebView2 destroy returned invalid ABI v3 result values", malformed);
            }
            int releaseProof = DestroyResult.THREAD_JOINED
                    | DestroyResult.CALLBACK_RETIRED
                    | DestroyResult.PARENT_RELEASED;
            if ((result.flags() & releaseProof) != releaseProof) {
                throw new MalformedDestroyException(
                        "WebView2 destroy did not prove parent, native thread, and callback release");
            }
            if (result.terminalHresult() != status) {
                throw new MalformedDestroyException(
                        "WebView2 destroy result HRESULT disagrees with its return value");
            }
            validateBrowserReleaseEvidence(result);
            boolean udfConfirmed = result.udfReleaseConfirmed();
            if ((status == S_OK) != udfConfirmed) {
                throw new MalformedDestroyException(status == S_OK
                        ? "WebView2 destroy did not prove user-data-folder release"
                        : "WebView2 destroy returned S_FALSE with confirmed user-data release");
            }
            return result;
        }

        private static void validateBrowserReleaseEvidence(DestroyResult result)
                throws MalformedDestroyException {
            int flags = result.flags();
            boolean noBrowser = (flags & DestroyResult.NO_BROWSER_STARTED) != 0;
            boolean exitObserved = (flags & DestroyResult.BROWSER_EXIT_OBSERVED) != 0;
            boolean pidMatched = (flags & DestroyResult.PID_MATCHED) != 0;
            boolean udfIdentity = (flags & DestroyResult.UDF_IDENTITY_VERIFIED) != 0;
            boolean udfReleased = (flags & DestroyResult.UDF_RELEASE_CONFIRMED) != 0;
            long expectedPid = result.expectedBrowserProcessId();
            long observedPid = result.observedBrowserProcessId();

            if (noBrowser) {
                int impossible = DestroyResult.CONTROLLER_CLOSED
                        | DestroyResult.UDF_IDENTITY_VERIFIED
                        | DestroyResult.BROWSER_EXIT_OBSERVED
                        | DestroyResult.PID_MATCHED
                        | DestroyResult.UDF_RELEASE_CONFIRMED;
                if ((flags & impossible) != 0 || expectedPid != 0 || observedPid != 0
                        || result.browserExitKind() != 0) {
                    throw new MalformedDestroyException(
                            "WebView2 destroy returned contradictory no-browser evidence");
                }
                return;
            }
            if (exitObserved != (observedPid != 0)) {
                throw new MalformedDestroyException(
                        "WebView2 destroy browser-exit flag and observed PID disagree");
            }
            if (exitObserved
                    && result.browserExitKind() != 0
                    && result.browserExitKind() != 1) {
                throw new MalformedDestroyException(
                        "WebView2 destroy returned an unknown browser exit kind");
            }
            if (!exitObserved && result.browserExitKind() != 0) {
                throw new MalformedDestroyException(
                        "WebView2 destroy returned an exit kind without an exit event");
            }
            boolean exactPidMatch = expectedPid != 0
                    && observedPid != 0
                    && expectedPid == observedPid;
            if (pidMatched != (exitObserved && exactPidMatch)) {
                throw new MalformedDestroyException(
                        "WebView2 destroy PID-match evidence is inconsistent");
            }
            if (udfReleased && (!udfIdentity || !exitObserved || !pidMatched)) {
                throw new MalformedDestroyException(
                        "WebView2 destroy returned incomplete UDF-release evidence");
            }
        }

        private static void requireSameLiveHandle(
                Pointer expected, Pointer actual, String message) throws IOException {
            long expectedValue = expected == null ? 0 : Pointer.nativeValue(expected);
            long actualValue = actual == null ? 0 : Pointer.nativeValue(actual);
            if (expectedValue == 0 || actualValue != expectedValue) {
                throw new MalformedDestroyException(message);
            }
        }

        private Pointer requireOpenHandle() throws IOException {
            if (handle == null || state != SessionState.OPEN) {
                throw new IOException("WebView2 native session is already closed");
            }
            return handle;
        }

        private enum SessionState {
            OPEN,
            CLOSING,
            RELEASED,
            TERMINAL
        }
    }

    private static final class MalformedDestroyException extends IOException {
        private MalformedDestroyException(String message) {
            super(message);
        }

        private MalformedDestroyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class CallbackState implements NativeEventCallback {
        private volatile Listener listener;

        private CallbackState(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void invoke(
                Pointer ignored,
                int kind,
                int status,
                Pointer source,
                Pointer payload) {
            Listener current = listener;
            if (current == null) {
                return;
            }
            final Event event;
            try {
                event = new Event(
                        Kind.fromNative(kind),
                        status,
                        copyWideString(source, 2_048),
                        copyWideString(payload, 1_500_000));
            } catch (RuntimeException | Error invalid) {
                eventFailure(current,
                        "Native WebView2 callback violated its bounded ABI contract");
                return;
            }
            try {
                current.event(event);
            } catch (RuntimeException | Error ignoredFailure) {
                // Java UI callbacks cannot unwind through the native COM thread.
            }
        }

        private void release() {
            listener = null;
        }

        private static void eventFailure(Listener listener, String message) {
            try {
                listener.event(new Event(Kind.FAILED, 0x80004005, "", message));
            } catch (RuntimeException | Error ignored) {
                // Never unwind a JNA callback.
            }
        }
    }

    @Structure.FieldOrder({
        "struct_size", "abi_version", "parent_window",
        "x", "y", "width", "height", "user_data_folder", "content_root",
        "virtual_host", "session_nonce", "resource_manifest"
    })
    public static final class NativeOptions extends Structure {
        public int struct_size;
        public int abi_version;
        public Pointer parent_window;
        public int x;
        public int y;
        public int width;
        public int height;
        public WString user_data_folder;
        public WString content_root;
        public WString virtual_host;
        public WString session_nonce;
        public WString resource_manifest;

        public NativeOptions() {}

        NativeOptions(CreateRequest request) {
            struct_size = size();
            abi_version = ABI_VERSION;
            parent_window = Pointer.createConstant(request.parentWindow());
            x = request.x();
            y = request.y();
            width = request.width();
            height = request.height();
            user_data_folder = new WString(request.userDataFolder().toString());
            content_root = new WString(request.contentRoot().toString());
            virtual_host = new WString(request.originPolicy().virtualHostName());
            session_nonce = new WString(request.sessionNonce());
            List<WebCanvasArtifactContract.ArtifactFile> files =
                    new ArrayList<>(request.resources().values());
            files.sort(Comparator.comparing(
                    WebCanvasArtifactContract.ArtifactFile::relativePath));
            List<String> records = files.stream()
                    .map(file -> file.relativePath() + "|" + file.size()
                            + "|" + file.sha256())
                    .toList();
            resource_manifest = new WString(String.join("\n", records));
            write();
        }
    }

    @Structure.FieldOrder({
        "struct_size", "flags", "expected_browser_pid", "observed_browser_pid",
        "browser_exit_kind", "terminal_hresult", "reserved"
    })
    public static final class NativeDestroyResult extends Structure {
        public int struct_size;
        public int flags;
        public int expected_browser_pid;
        public int observed_browser_pid;
        public int browser_exit_kind;
        public int terminal_hresult;
        public int[] reserved = new int[2];

        public NativeDestroyResult() {}
    }

    interface NativeEventCallback extends Callback, StdCallLibrary.StdCallCallback {
        void invoke(Pointer context, int kind, int status, Pointer source, Pointer payload);
    }

    interface Binding extends StdCallLibrary {
        int nbwv2_get_abi_version();

        int nbwv2_get_runtime_version(
                char[] buffer, int bufferCharacters, IntByReference requiredCharacters);

        int nbwv2_create(
                NativeOptions options,
                NativeEventCallback callback,
                Pointer callbackContext,
                PointerByReference hostHandle);

        int nbwv2_post_web_message_json(Pointer hostHandle, WString json);

        int nbwv2_set_bounds(
                Pointer hostHandle, int x, int y, int width, int height);

        int nbwv2_set_visible(Pointer hostHandle, int visible);

        int nbwv2_request_focus(Pointer hostHandle);

        int nbwv2_prepare_parent_release(
                Pointer hostHandle, Pointer expectedParentWindow, int timeoutMilliseconds);

        int nbwv2_destroy(
                PointerByReference hostHandle,
                int timeoutMilliseconds,
                NativeDestroyResult result);
    }

    private static int boundedTimeoutMilliseconds(Duration timeout) throws IOException {
        if (timeout == null) {
            throw new IOException("WebView2 native timeout is required");
        }
        Duration value = timeout;
        if (value.isZero() || value.isNegative()) {
            throw new IOException("WebView2 native timeout must be positive");
        }
        final long milliseconds;
        try {
            milliseconds = value.toMillis();
        } catch (ArithmeticException overflow) {
            throw new IOException("WebView2 native timeout exceeds the ABI range", overflow);
        }
        if (milliseconds < 1 || milliseconds > Integer.MAX_VALUE) {
            throw new IOException("WebView2 native timeout exceeds the ABI range");
        }
        return (int) milliseconds;
    }

    private static String copyWideString(Pointer pointer, int maximum) {
        if (pointer == null || Pointer.nativeValue(pointer) == 0) {
            return "";
        }
        String text = pointer.getWideString(0);
        if (text.length() > maximum) {
            throw new IllegalArgumentException("native wide string exceeds its bound");
        }
        return text;
    }

    private static void requireSuccess(int status, String operation) throws IOException {
        if (status < 0) {
            throw new IOException(statusFailureMessage(status, operation));
        }
    }

    private static String statusFailureMessage(int status, String operation) {
        return operation + " failed with HRESULT 0x"
                + String.format(Locale.ROOT, "%08X", status);
    }

    private static String boundedFailure(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        String normalized = message.replace('\r', ' ').replace('\n', ' ').strip();
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 512);
    }
}
