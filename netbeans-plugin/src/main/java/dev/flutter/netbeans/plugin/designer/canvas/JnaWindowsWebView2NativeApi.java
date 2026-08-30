package dev.flutter.netbeans.plugin.designer.canvas;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lazy JNA binding for the native COM-owning WebView2 adapter. */
final class JnaWindowsWebView2NativeApi implements WindowsWebView2NativeApi {
    private static final int ABI_VERSION = 2;
    private static final int ERROR_INSUFFICIENT_BUFFER_HRESULT = 0x8007007A;
    private static final int MAXIMUM_RUNTIME_VERSION_CHARACTERS = 256;
    private static final Set<CallbackState> FAILED_DESTROY_CALLBACKS =
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
        int status = binding.nbwv2_create(
                nativeOptions, callback, Pointer.NULL, handle);
        requireSuccess(status, "create WebView2 environment/controller");
        Pointer pointer = handle.getValue();
        if (pointer == null || Pointer.nativeValue(pointer) == 0) {
            throw new IOException("WebView2 native adapter returned no session handle");
        }
        return new JnaSession(binding, pointer, callback);
    }

    private static final class JnaSession implements NativeSession {
        private final Binding binding;
        private final CallbackState callback;
        private final AtomicBoolean destroyed = new AtomicBoolean();
        private Pointer handle;

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
                    requireHandle(), new WString(value)), "post WebView2 JSON message");
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
                    requireHandle(), x, y, width, height), "resize WebView2 controller");
        }

        @Override
        public synchronized void setVisible(boolean visible) throws IOException {
            requireSuccess(binding.nbwv2_set_visible(
                    requireHandle(), visible ? 1 : 0), "change WebView2 visibility");
        }

        @Override
        public synchronized void requestFocus() throws IOException {
            requireSuccess(binding.nbwv2_request_focus(
                    requireHandle()), "focus WebView2 controller");
        }

        @Override
        public synchronized void destroy() throws IOException {
            if (!destroyed.compareAndSet(false, true)) {
                return;
            }
            Pointer current = handle;
            handle = null;
            try {
                requireSuccess(binding.nbwv2_destroy(current), "destroy WebView2 controller");
                callback.release();
            } catch (IOException | RuntimeException | Error failure) {
                // A failed native teardown may still own and invoke this JNA
                // function pointer. Retain it until a late CLOSED event proves
                // the native thread released the callback, otherwise leak the
                // tiny callback object rather than permit a native UAF.
                callback.retainAfterFailedDestroy();
                throw failure;
            }
        }

        private Pointer requireHandle() throws IOException {
            if (handle == null || destroyed.get()) {
                throw new IOException("WebView2 native session is already closed");
            }
            return handle;
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
            } finally {
                if (event.kind() == Kind.CLOSED) {
                    listener = null;
                    FAILED_DESTROY_CALLBACKS.remove(this);
                }
            }
        }

        private void release() {
            listener = null;
            FAILED_DESTROY_CALLBACKS.remove(this);
        }

        private void retainAfterFailedDestroy() {
            FAILED_DESTROY_CALLBACKS.add(this);
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

        int nbwv2_destroy(Pointer hostHandle);
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
            throw new IOException(operation + " failed with HRESULT 0x"
                    + String.format(Locale.ROOT, "%08X", status));
        }
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
