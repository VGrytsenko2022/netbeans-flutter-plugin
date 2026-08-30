package dev.flutter.netbeans.plugin.designer.canvas;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

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

        /** Blocking native teardown. Never call this method on the Swing EDT. */
        void destroy() throws IOException;
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
