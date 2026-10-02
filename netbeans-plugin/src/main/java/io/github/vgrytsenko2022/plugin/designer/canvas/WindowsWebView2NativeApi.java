package io.github.vgrytsenko2022.plugin.designer.canvas;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Narrow Java side of the versioned native WebView2 C ABI. */
interface WindowsWebView2NativeApi {
    String runtimeVersion() throws IOException;

    NativeSession create(CreateRequest request, Listener listener) throws IOException;

    record CreateRequest(
            long parentWindow,
            int x,
            int y,
            int width,
            int height,
            Path userDataFolder,
            Path contentRoot,
            WebCanvasOriginPolicy originPolicy,
            Map<String, WebCanvasArtifactContract.ArtifactFile> artifactFiles,
            String sessionNonce) {
        public CreateRequest {
            if (parentWindow == 0 || width <= 0 || height <= 0
                    || width > 32_767 || height > 32_767
                    || x > Integer.MAX_VALUE - width
                    || y > Integer.MAX_VALUE - height) {
                throw new IllegalArgumentException("invalid WebView2 parent window or bounds");
            }
            userDataFolder = requireAbsolute(userDataFolder, "userDataFolder");
            contentRoot = requireAbsolute(contentRoot, "contentRoot");
            originPolicy = Objects.requireNonNull(originPolicy, "originPolicy");
            artifactFiles = Map.copyOf(Objects.requireNonNull(
                    artifactFiles, "artifactFiles"));
            if (!artifactFiles.keySet().equals(originPolicy.artifactPaths())) {
                throw new IllegalArgumentException(
                        "WebView2 artifact manifest and origin paths disagree");
            }
            if (artifactFiles.isEmpty()
                    || artifactFiles.size() > WebCanvasArtifactContract.MAX_SNAPSHOT_FILES) {
                throw new IllegalArgumentException(
                        "WebView2 artifact manifest must contain between 1 and "
                        + WebCanvasArtifactContract.MAX_SNAPSHOT_FILES + " files");
            }
            long totalBytes = 0;
            artifactFiles.forEach((path, file) -> {
                if (!path.equals(file.relativePath())
                        || file.size() > WebCanvasArtifactContract.MAX_SNAPSHOT_FILE_BYTES
                        || !file.sha256().matches("[0-9a-f]{64}")) {
                    throw new IllegalArgumentException(
                            "invalid WebView2 artifact manifest entry: " + path);
                }
            });
            for (WebCanvasArtifactContract.ArtifactFile file : artifactFiles.values()) {
                try {
                    totalBytes = Math.addExact(totalBytes, file.size());
                } catch (ArithmeticException overflow) {
                    throw new IllegalArgumentException(
                            "WebView2 artifact manifest byte total overflow", overflow);
                }
            }
            if (totalBytes > WebCanvasArtifactContract.MAX_SNAPSHOT_TOTAL_BYTES) {
                throw new IllegalArgumentException(
                        "WebView2 artifact manifest exceeds the 128 MiB native snapshot bound");
            }
            if (sessionNonce == null || !sessionNonce.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException(
                        "WebView2 session nonce must be 64 lowercase hexadecimal characters");
            }
        }

        Map<String, WebCanvasArtifactContract.ArtifactFile> resources() {
            return artifactFiles;
        }

        private static Path requireAbsolute(Path value, String label) {
            Path path = Objects.requireNonNull(value, label).toAbsolutePath().normalize();
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException(label + " must be absolute");
            }
            return path;
        }
    }

    interface NativeSession {
        void postWebMessageJson(String json) throws IOException;

        void setBounds(int x, int y, int width, int height) throws IOException;

        void setVisible(boolean visible) throws IOException;

        void requestFocus() throws IOException;

        /**
         * Synchronously reparents the controller away from the AWT carrier.
         * This bounded call is the native part of the pre-peer-loss barrier.
         * It must run off the Swing EDT; an owner must await the host-level
         * asynchronous barrier before allowing AWT to destroy the parent HWND.
         */
        void prepareParentRelease(long expectedParentWindow, Duration timeout)
                throws IOException;

        /**
         * Blocking native teardown. Never call this method on the Swing EDT.
         * A failed call retains native ownership and may be retried; success is
         * the only proof that the handle and its browser process were released.
         */
        DestroyResult destroy(Duration timeout) throws IOException;
    }

    /**
     * Failed native creation with explicit ownership evidence. A retained
     * session must be torn down exactly like a successfully returned session;
     * when neither a session nor release proof exists, callers must quarantine
     * all path-owned resources rather than infer release from a null handle.
     */
    final class CreateException extends IOException {
        private final NativeSession retainedSession;
        private final boolean releaseConfirmed;

        CreateException(
                String message,
                NativeSession retainedSession,
                boolean releaseConfirmed) {
            this(message, retainedSession, releaseConfirmed, null);
        }

        CreateException(
                String message,
                NativeSession retainedSession,
                boolean releaseConfirmed,
                Throwable cause) {
            super(message, cause);
            if (retainedSession != null && releaseConfirmed) {
                throw new IllegalArgumentException(
                        "retained WebView2 session contradicts release confirmation");
            }
            this.retainedSession = retainedSession;
            this.releaseConfirmed = releaseConfirmed;
        }

        Optional<NativeSession> retainedSession() {
            return Optional.ofNullable(retainedSession);
        }

        boolean releaseConfirmed() {
            return releaseConfirmed;
        }
    }

    record DestroyResult(
            int flags,
            long expectedBrowserProcessId,
            long observedBrowserProcessId,
            int browserExitKind,
            int terminalHresult) {
        static final int PARENT_RELEASED = 1 << 0;
        static final int UDF_IDENTITY_VERIFIED = 1 << 1;
        static final int CONTROLLER_CLOSED = 1 << 2;
        static final int BROWSER_EXIT_OBSERVED = 1 << 3;
        static final int PID_MATCHED = 1 << 4;
        static final int UDF_RELEASE_CONFIRMED = 1 << 5;
        static final int THREAD_JOINED = 1 << 6;
        static final int CALLBACK_RETIRED = 1 << 7;
        static final int NO_BROWSER_STARTED = 1 << 8;
        static final int ALL_FLAGS = (1 << 9) - 1;

        public DestroyResult {
            if ((flags & ~ALL_FLAGS) != 0
                    || expectedBrowserProcessId < 0
                    || expectedBrowserProcessId > 0xffff_ffffL
                    || observedBrowserProcessId < 0
                    || observedBrowserProcessId > 0xffff_ffffL) {
                throw new IllegalArgumentException("invalid native WebView2 destroy result");
            }
        }

        boolean udfReleaseConfirmed() {
            return (flags & (UDF_RELEASE_CONFIRMED | NO_BROWSER_STARTED)) != 0;
        }
    }

    interface Listener {
        void event(Event event);
    }

    record Event(Kind kind, int statusCode, String source, String payload) {
        public Event {
            kind = Objects.requireNonNull(kind, "kind");
            source = bounded(source, 2_048);
            payload = bounded(payload, 1_500_000);
        }

        private static String bounded(String value, int maximum) {
            String text = value == null ? "" : value;
            if (text.length() > maximum) {
                throw new IllegalArgumentException("native WebView2 event payload exceeds bound");
            }
            return text;
        }
    }

    enum Kind {
        CONTROLLER_READY(1),
        WEB_MESSAGE(2),
        DIAGNOSTIC(3),
        FAILED(4),
        PROCESS_FAILED(5),
        CLOSED(6);

        private final int nativeValue;

        Kind(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        static Kind fromNative(int value) {
            for (Kind kind : values()) {
                if (kind.nativeValue == value) {
                    return kind;
                }
            }
            throw new IllegalArgumentException("unknown native WebView2 event kind: " + value);
        }
    }
}
