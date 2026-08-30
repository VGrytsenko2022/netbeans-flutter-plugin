package dev.flutter.netbeans.plugin.designer.canvas;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Exact virtual-origin, request, response-header, and MIME policy for WebView2. */
final class WebCanvasOriginPolicy {
    static final String CONTENT_SECURITY_POLICY = String.join(" ",
            "default-src 'none';",
            "base-uri 'none';",
            "connect-src 'self';",
            "font-src 'self';",
            "form-action 'none';",
            "frame-ancestors 'none';",
            "img-src 'self' data: blob:;",
            "manifest-src 'none';",
            "media-src 'none';",
            "object-src 'none';",
            "script-src 'self' 'wasm-unsafe-eval';",
            "style-src 'self' 'unsafe-inline';",
            "worker-src 'none'");

    private static final String INDEX_PATH = "index.html";
    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
            Map.entry(".bin", "application/octet-stream"),
            Map.entry(".css", "text/css; charset=utf-8"),
            Map.entry(".frag", "application/octet-stream"),
            Map.entry(".html", "text/html; charset=utf-8"),
            Map.entry(".js", "text/javascript; charset=utf-8"),
            Map.entry(".json", "application/json; charset=utf-8"),
            Map.entry(".otf", "font/otf"),
            Map.entry(".symbols", "text/plain; charset=utf-8"),
            Map.entry(".ttf", "font/ttf"),
            Map.entry(".txt", "text/plain; charset=utf-8"),
            Map.entry(".wasm", "application/wasm"));
    private static final Map<String, String> SECURITY_HEADERS = securityHeaders();

    private final String virtualHostName;
    private final String origin;
    private final String indexUri;
    private final Set<String> artifactPaths;

    WebCanvasOriginPolicy(
            String sessionNonce,
            String generationId,
            Set<String> artifactPaths) {
        String nonce = requireDigest(sessionNonce, "session nonce");
        String generation = requireDigest(generationId, "generation id");
        this.artifactPaths = validateArtifactPaths(artifactPaths);
        String identity = sha256("NETBEANS_WEB_CANVAS_ORIGIN|1|" + nonce + "|" + generation);
        this.virtualHostName = "nbfc-" + identity.substring(0, 32) + "."
                + identity.substring(32) + ".invalid";
        this.origin = "https://" + virtualHostName;
        this.indexUri = origin + "/" + INDEX_PATH;
    }

    String virtualHostName() {
        return virtualHostName;
    }

    String origin() {
        return origin;
    }

    String indexUri() {
        return indexUri;
    }

    Set<String> artifactPaths() {
        return artifactPaths;
    }

    boolean allowsTopLevelNavigation(String candidate) {
        return indexUri.equals(candidate) && resolve(candidate).isPresent();
    }

    Optional<Resource> resolve(String candidate) {
        if (candidate == null
                || candidate.indexOf('\\') >= 0
                || candidate.indexOf('%') >= 0
                || candidate.indexOf('?') >= 0
                || candidate.indexOf('#') >= 0) {
            return Optional.empty();
        }
        final URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }
        if (!"https".equals(uri.getScheme())
                || uri.getUserInfo() != null
                || uri.getPort() != -1
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !virtualHostName.equals(uri.getHost())
                || !virtualHostName.equals(uri.getRawAuthority())) {
            return Optional.empty();
        }
        String rawPath = uri.getRawPath();
        if (rawPath == null || rawPath.length() < 2 || rawPath.charAt(0) != '/') {
            return Optional.empty();
        }
        String relative = rawPath.substring(1);
        if (!isCanonicalRelativePath(relative) || !artifactPaths.contains(relative)) {
            return Optional.empty();
        }
        String contentType = contentType(relative);
        return Optional.of(new Resource(relative, contentType, responseHeaders(contentType)));
    }

    String responseHeadersBlock(String relativePath) {
        String contentType = requireKnownPath(relativePath);
        StringBuilder result = new StringBuilder();
        responseHeaders(contentType).forEach((name, value) -> result
                .append(name).append(": ").append(value).append("\r\n"));
        return result.toString();
    }

    String contentTypeFor(String relativePath) {
        return requireKnownPath(relativePath);
    }

    private String requireKnownPath(String relativePath) {
        Objects.requireNonNull(relativePath, "relativePath");
        if (!isCanonicalRelativePath(relativePath) || !artifactPaths.contains(relativePath)) {
            throw new IllegalArgumentException("unknown Web Canvas artifact path: " + relativePath);
        }
        return contentType(relativePath);
    }

    private static Set<String> validateArtifactPaths(Set<String> paths) {
        Objects.requireNonNull(paths, "artifactPaths");
        TreeSet<String> accepted = new TreeSet<>();
        Set<String> folded = new HashSet<>();
        for (String path : paths) {
            if (!isCanonicalRelativePath(path)) {
                throw new IllegalArgumentException("invalid Web Canvas artifact path: " + path);
            }
            if (!folded.add(path.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        "case-insensitive Web Canvas artifact path collision: " + path);
            }
            contentType(path);
            accepted.add(path);
        }
        if (!accepted.contains(INDEX_PATH)) {
            throw new IllegalArgumentException("Web Canvas artifact has no index.html");
        }
        return Collections.unmodifiableSet(accepted);
    }

    private static boolean isCanonicalRelativePath(String value) {
        if (value == null || value.isEmpty() || value.startsWith("/") || value.endsWith("/")
                || value.indexOf('\\') >= 0 || value.indexOf('%') >= 0) {
            return false;
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")
                    || !segment.matches("[A-Za-z0-9._-]+")) {
                return false;
            }
        }
        return true;
    }

    private static String contentType(String relativePath) {
        if (relativePath.endsWith("/NOTICES")) {
            return "text/plain; charset=utf-8";
        }
        int slash = relativePath.lastIndexOf('/');
        int dot = relativePath.lastIndexOf('.');
        if (dot <= slash) {
            throw new IllegalArgumentException(
                    "Web Canvas artifact has no approved MIME type: " + relativePath);
        }
        String type = MIME_TYPES.get(relativePath.substring(dot).toLowerCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException(
                    "Web Canvas artifact has no approved MIME type: " + relativePath);
        }
        return type;
    }

    private static Map<String, String> responseHeaders(String contentType) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", contentType);
        headers.putAll(SECURITY_HEADERS);
        return Collections.unmodifiableMap(headers);
    }

    private static Map<String, String> securityHeaders() {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put("Cache-Control", "no-store");
        headers.put("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        headers.put("Cross-Origin-Embedder-Policy", "require-corp");
        headers.put("Cross-Origin-Opener-Policy", "same-origin");
        headers.put("Cross-Origin-Resource-Policy", "same-origin");
        headers.put("Permissions-Policy",
                "camera=(), geolocation=(), microphone=(), payment=(), usb=()");
        headers.put("Referrer-Policy", "no-referrer");
        headers.put("X-Content-Type-Options", "nosniff");
        return Collections.unmodifiableMap(headers);
    }

    private static String requireDigest(String value, String label) {
        Objects.requireNonNull(value, label);
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "Web Canvas " + label + " must be a lowercase SHA-256 value");
        }
        return value;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK does not provide SHA-256", exception);
        }
    }

    record Resource(String relativePath, String contentType, Map<String, String> headers) {
        Resource {
            Objects.requireNonNull(relativePath, "relativePath");
            Objects.requireNonNull(contentType, "contentType");
            headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
        }
    }
}
