package io.github.vgrytsenko2022.plugin.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/** Correlates Dart completion requests with their framed LSP responses. */
final class DartLspCompletionCompatibility {
    private static final int MAX_PENDING_REQUESTS = 256;
    private static final String BRIDGE_MARKER = "__netbeans30CompletionBridge";
    private static final String BRIDGE_VERSION = "1";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Set<String> pendingCompletionIds = new LinkedHashSet<>();

    synchronized void recordRequest(JsonNode message) {
        if (message == null
                || !"textDocument/completion".equals(message.path("method").asText())
                || !message.hasNonNull("id")) {
            return;
        }
        if (pendingCompletionIds.size() >= MAX_PENDING_REQUESTS) {
            Iterator<String> oldest = pendingCompletionIds.iterator();
            if (oldest.hasNext()) {
                oldest.next();
                oldest.remove();
            }
        }
        pendingCompletionIds.add(key(message.get("id")));
    }

    synchronized boolean hasPendingRequests() {
        return !pendingCompletionIds.isEmpty();
    }

    synchronized boolean takeResponse(JsonNode id) {
        return id != null && pendingCompletionIds.remove(key(id));
    }

    static boolean hideResolvableTextEdit(ObjectNode completionItem) {
        JsonNode originalData = completionItem.get("data");
        JsonNode originalTextEdit = completionItem.get("textEdit");
        if (originalData == null || originalData.isNull()
                || originalTextEdit == null || !originalTextEdit.isObject()) {
            return false;
        }
        try {
            ObjectNode envelope = JSON.createObjectNode();
            envelope.put("version", BRIDGE_VERSION);
            envelope.put("data", encode(originalData));
            envelope.put("textEdit", encode(originalTextEdit));
            completionItem.set("data", JSON.createObjectNode()
                    .set(BRIDGE_MARKER, envelope));
            completionItem.remove("textEdit");
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    static boolean restoreResolveTextEdit(JsonNode message) {
        if (!(message instanceof ObjectNode request)
                || !"completionItem/resolve".equals(request.path("method").asText())
                || !(request.get("params") instanceof ObjectNode item)
                || !(item.get("data") instanceof ObjectNode wrappedData)
                || !(wrappedData.get(BRIDGE_MARKER) instanceof ObjectNode envelope)
                || !BRIDGE_VERSION.equals(envelope.path("version").asText())
                || !envelope.path("data").isTextual()
                || !envelope.path("textEdit").isTextual()) {
            return false;
        }
        try {
            JsonNode originalData = decode(envelope.path("data").asText());
            JsonNode originalEdit = decode(envelope.path("textEdit").asText());
            if (originalData == null || originalData.isNull()
                    || originalEdit == null || !originalEdit.isObject()) {
                return false;
            }
            item.set("data", originalData);
            item.set("textEdit", originalEdit);
            return true;
        } catch (IOException | IllegalArgumentException ex) {
            return false;
        }
    }

    private static String key(JsonNode id) {
        return id.getNodeType().name() + ':' + id.toString();
    }

    private static String encode(JsonNode value) throws IOException {
        return Base64.getEncoder().encodeToString(JSON.writeValueAsBytes(value));
    }

    private static JsonNode decode(String value) throws IOException {
        return JSON.readTree(Base64.getDecoder().decode(value));
    }
}
