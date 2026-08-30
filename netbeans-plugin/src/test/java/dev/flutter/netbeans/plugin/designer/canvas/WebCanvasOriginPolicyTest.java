package dev.flutter.netbeans.plugin.designer.canvas;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebCanvasOriginPolicyTest {
    private static final String NONCE = "1".repeat(64);
    private static final String GENERATION = "2".repeat(64);
    private static final Set<String> FILES = Set.of(
            "index.html",
            "main.dart.js",
            "canvas.css",
            "assets/AssetManifest.bin",
            "assets/NOTICES",
            "assets/assets/licenses/Roboto-LICENSE.txt",
            "assets/fonts/MaterialIcons-Regular.otf",
            "canvaskit/canvaskit.wasm",
            "canvaskit/canvaskit.js.symbols");

    @Test
    void derivesStableUniqueLowercaseInvalidOriginFromCompleteIdentities() {
        WebCanvasOriginPolicy first = policy();
        WebCanvasOriginPolicy repeated = policy();
        WebCanvasOriginPolicy another = new WebCanvasOriginPolicy(
                "3".repeat(64), GENERATION, FILES);

        assertEquals(first.virtualHostName(), repeated.virtualHostName());
        assertEquals(first.indexUri(), repeated.indexUri());
        assertNotEquals(first.virtualHostName(), another.virtualHostName());
        assertTrue(first.virtualHostName().matches(
                "nbfc-[0-9a-f]{32}\\.[0-9a-f]{32}\\.invalid"));
        for (String label : first.virtualHostName().split("\\.")) {
            assertTrue(label.length() <= 63);
        }
        assertEquals("https://" + first.virtualHostName(), first.origin());
        assertEquals(first.origin() + "/index.html", first.indexUri());
    }

    @Test
    void acceptsOnlyExactIndexNavigation() {
        WebCanvasOriginPolicy policy = policy();

        assertTrue(policy.allowsTopLevelNavigation(policy.indexUri()));
        assertFalse(policy.allowsTopLevelNavigation(policy.origin() + "/"));
        assertFalse(policy.allowsTopLevelNavigation(
                policy.origin() + "/main.dart.js"));
        assertFalse(policy.allowsTopLevelNavigation(
                policy.indexUri() + "?reload=true"));
    }

    @Test
    void resolvesOnlyExactKnownOriginAndArtifactPath() {
        WebCanvasOriginPolicy policy = policy();
        WebCanvasOriginPolicy.Resource resource = policy.resolve(
                policy.origin() + "/canvaskit/canvaskit.wasm").orElseThrow();

        assertEquals("canvaskit/canvaskit.wasm", resource.relativePath());
        assertEquals("application/wasm", resource.contentType());
        assertEquals("nosniff", resource.headers().get("X-Content-Type-Options"));
        assertEquals("no-store", resource.headers().get("Cache-Control"));
        assertEquals(WebCanvasOriginPolicy.CONTENT_SECURITY_POLICY,
                resource.headers().get("Content-Security-Policy"));
    }

    @Test
    void rejectsTraversalEncodingBackslashQueryPortAndCaseAliases() {
        WebCanvasOriginPolicy policy = policy();
        String origin = policy.origin();
        String upperHost = origin.replace("nbfc-", "NBFC-");

        for (String rejected : Set.of(
                "http://" + policy.virtualHostName() + "/index.html",
                upperHost + "/index.html",
                "https://" + policy.virtualHostName() + ":443/index.html",
                origin + "/Index.html",
                origin + "/assets/../index.html",
                origin + "/assets/./NOTICES",
                origin + "//index.html",
                origin + "/%69ndex.html",
                origin + "/%2e%2e/index.html",
                origin + "/assets%2fNOTICES",
                origin + "/assets\\NOTICES",
                origin + "/index.html?x=1",
                origin + "/index.html#fragment",
                origin + "/not-present.js")) {
            assertTrue(policy.resolve(rejected).isEmpty(), rejected);
        }
    }

    @Test
    void suppliesFixedSecurityHeadersAndApprovedMimeTypes() {
        WebCanvasOriginPolicy policy = policy();

        assertEquals("text/html; charset=utf-8", policy.contentTypeFor("index.html"));
        assertEquals("text/javascript; charset=utf-8",
                policy.contentTypeFor("main.dart.js"));
        assertEquals("application/octet-stream",
                policy.contentTypeFor("assets/AssetManifest.bin"));
        assertEquals("text/plain; charset=utf-8",
                policy.contentTypeFor("assets/NOTICES"));
        assertEquals("text/plain; charset=utf-8",
                policy.contentTypeFor("assets/assets/licenses/Roboto-LICENSE.txt"));
        assertEquals("font/otf",
                policy.contentTypeFor("assets/fonts/MaterialIcons-Regular.otf"));
        String headers = policy.responseHeadersBlock("index.html");
        assertTrue(headers.contains("Content-Security-Policy: default-src 'none';"));
        assertTrue(headers.contains("Cross-Origin-Embedder-Policy: require-corp\r\n"));
        assertTrue(headers.endsWith("\r\n"));
        assertThrows(IllegalArgumentException.class,
                () -> policy.contentTypeFor("missing.js"));
    }

    @Test
    void rejectsInvalidIdentitiesUnsafePathsCaseCollisionsAndUnknownMime() {
        assertThrows(IllegalArgumentException.class,
                () -> new WebCanvasOriginPolicy("ABC", GENERATION, FILES));
        assertThrows(IllegalArgumentException.class,
                () -> new WebCanvasOriginPolicy(NONCE, GENERATION,
                        Set.of("index.html", "../escape.js")));
        assertThrows(IllegalArgumentException.class,
                () -> new WebCanvasOriginPolicy(NONCE, GENERATION,
                        Set.of("index.html", "INDEX.HTML")));
        assertThrows(IllegalArgumentException.class,
                () -> new WebCanvasOriginPolicy(NONCE, GENERATION,
                        Set.of("index.html", "asset.unknown")));
        assertThrows(IllegalArgumentException.class,
                () -> new WebCanvasOriginPolicy(NONCE, GENERATION,
                        Set.of("main.dart.js")));
    }

    @Test
    void exposedCollectionsAreImmutable() {
        WebCanvasOriginPolicy policy = policy();
        assertThrows(UnsupportedOperationException.class,
                () -> policy.artifactPaths().add("other.js"));
        Map<String, String> headers = policy.resolve(policy.indexUri())
                .orElseThrow().headers();
        assertThrows(UnsupportedOperationException.class,
                () -> headers.put("Unsafe", "value"));
    }

    private static WebCanvasOriginPolicy policy() {
        return new WebCanvasOriginPolicy(NONCE, GENERATION, FILES);
    }
}
