package dev.flutter.netbeans.plugin.designer.canvas;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.flutter.netbeans.designer.canvas.CanvasFrameKey;
import dev.flutter.netbeans.designer.canvas.CanvasIntentId;
import dev.flutter.netbeans.designer.canvas.CanvasIntentKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireProtocol;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameKind;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessPayloadDescriptor;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/** Strict small-message codec layered beside the frozen lifecycle wire v1. */
public final class CanvasRunnerControlCodec {
    public static final String FORMAT = "netbeans-flutter-canvas-runtime";
    public static final int VERSION = 1;
    private static final int MAX_CONTROL_BYTES = 256 * 1024;
    private static final Set<String> ROOT_FIELDS = Set.of(
            "format", "protocolVersion", "sessionId", "type", "body");
    private static final Set<String> PRESENTED_FIELDS = Set.of(
            "presentationSequence", "documentId", "logicalRevisionId",
            "frameSequence", "layoutSequence");
    private static final Set<String> SELECTION_FIELDS = Set.of(
            "presentationSequence", "documentId", "logicalRevisionId",
            "frameSequence", "layoutSequence", "intentSequence", "widgetId");
    private static final Set<String> PALETTE_DROP_FIELDS = Set.of(
            "presentationSequence", "documentId", "logicalRevisionId",
            "frameSequence", "layoutSequence", "intentSequence", "token",
            "operation", "parentWidgetId", "slotName", "insertionIndex");
    private static final String PALETTE_DROP_TOKEN_PREFIX = "nbfdnd:v1:";
    private static final int MAX_PALETTE_DROP_TOKEN_CHARACTERS = 160;

    private final JsonFactory jsonFactory;
    private final ObjectMapper mapper;

    public CanvasRunnerControlCodec() {
        jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxDocumentLength(MAX_CONTROL_BYTES)
                        .maxNestingDepth(16)
                        .maxTokenCount(128)
                        .maxNameLength(64)
                        .maxStringLength(256)
                        .maxNumberLength(32)
                        .build())
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();
        mapper = new ObjectMapper(jsonFactory);
    }

    public byte[] encodeRender(
            CanvasRenderRequest request,
            CanvasProcessPayloadDescriptor model)
            throws CanvasRunnerControlException {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(model, "model");
        if (model.kind() != CanvasProcessFrameKind.MODEL_JSON) {
            throw new IllegalArgumentException("render requires one MODEL_JSON descriptor");
        }
        return encode(json -> {
            writeEnvelopeStart(json, request.revisionKey().sessionId(), "host.render");
            writeRevision(json, request.revisionKey());
            json.writeObjectFieldStart("model");
            json.writeStringField("kind", "model.json");
            json.writeNumberField("payloadBytes", model.payloadBytes());
            json.writeStringField(
                    "sha256", HexFormat.of().formatHex(model.copySha256()));
            json.writeEndObject();
            writeEnvelopeEnd(json);
        });
    }

    public byte[] encodeSelection(CanvasLayoutKey layout, StableId widgetId)
            throws CanvasRunnerControlException {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(widgetId, "widgetId");
        return encode(json -> {
            CanvasRevisionKey revision = layout.frameKey().revisionKey();
            writeEnvelopeStart(json, revision.sessionId(), "host.selection");
            writeRevision(json, revision);
            json.writeNumberField("frameSequence", layout.frameKey().frameSequence());
            json.writeNumberField("layoutSequence", layout.layoutSequence());
            json.writeStringField("widgetId", widgetId.toString());
            writeEnvelopeEnd(json);
        });
    }

    /** Decodes only the explicitly supported runner runtime events. */
    public CanvasRunnerRuntimeEvent decode(byte[] bytes)
            throws CanvasRunnerControlException {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length == 0 || bytes.length > MAX_CONTROL_BYTES) {
            throw new CanvasRunnerControlException(
                    "Canvas runtime control message violates its byte limit.");
        }
        try (JsonParser parser = jsonFactory.createParser(bytes)) {
            JsonNode root = mapper.readTree(parser);
            if (root == null || !root.isObject() || parser.nextToken() != null) {
                throw invalid("Canvas runtime control root must be one JSON object.");
            }
            requireFields(root, ROOT_FIELDS, "runtime envelope");
            requireText(root, "format", FORMAT);
            requireExactInt(root, "protocolVersion", VERSION);
            CanvasSessionId sessionId = CanvasSessionId.parse(
                    requireText(root, "sessionId"));
            String type = requireText(root, "type");
            JsonNode body = required(root, "body");
            if (!body.isObject()) {
                throw invalid("Canvas runtime body must be an object.");
            }
            return switch (type) {
                case "runner.presented" -> decodePresented(sessionId, body);
                case "runner.selection" -> decodeSelection(sessionId, body);
                case "runner.paletteDrop" -> decodePaletteDrop(sessionId, body);
                default -> throw invalid("Canvas runtime message type is not supported.");
            };
        } catch (CanvasRunnerControlException failure) {
            throw failure;
        } catch (IOException | RuntimeException failure) {
            throw new CanvasRunnerControlException(
                    "Canvas runtime control message is invalid.", failure);
        }
    }

    private static CanvasRunnerRuntimeEvent.Presented decodePresented(
            CanvasSessionId sessionId,
            JsonNode body) throws CanvasRunnerControlException {
        requireFields(body, PRESENTED_FIELDS, "runner.presented body");
        CanvasRevisionKey revision = readRevision(sessionId, body);
        CanvasFrameKey frame = new CanvasFrameKey(
                revision, requireSequence(body, "frameSequence"));
        return new CanvasRunnerRuntimeEvent.Presented(new CanvasLayoutKey(
                frame, requireSequence(body, "layoutSequence")));
    }

    private static CanvasRunnerRuntimeEvent.Selection decodeSelection(
            CanvasSessionId sessionId,
            JsonNode body) throws CanvasRunnerControlException {
        requireFields(body, SELECTION_FIELDS, "runner.selection body");
        CanvasRevisionKey revision = readRevision(sessionId, body);
        CanvasFrameKey frame = new CanvasFrameKey(
                revision, requireSequence(body, "frameSequence"));
        CanvasLayoutKey layout = new CanvasLayoutKey(
                frame, requireSequence(body, "layoutSequence"));
        CanvasIntentId intentId = new CanvasIntentId(
                sessionId, requireSequence(body, "intentSequence"));
        return new CanvasRunnerRuntimeEvent.Selection(
                new CanvasIntentKey(intentId, layout),
                StableId.parse(requireText(body, "widgetId")));
    }

    private static CanvasRunnerRuntimeEvent.PaletteDrop decodePaletteDrop(
            CanvasSessionId sessionId,
            JsonNode body) throws CanvasRunnerControlException {
        requireFields(body, PALETTE_DROP_FIELDS, "runner.paletteDrop body");
        CanvasRevisionKey revision = readRevision(sessionId, body);
        CanvasFrameKey frame = new CanvasFrameKey(
                revision, requireSequence(body, "frameSequence"));
        CanvasLayoutKey layout = new CanvasLayoutKey(
                frame, requireSequence(body, "layoutSequence"));
        CanvasIntentId intentId = new CanvasIntentId(
                sessionId, requireSequence(body, "intentSequence"));
        requireText(body, "operation", "ADD");
        String slot = requireText(body, "slotName");
        if (!"children".equals(slot)) {
            throw invalid("Canvas runtime field slotName is not supported.");
        }
        return new CanvasRunnerRuntimeEvent.PaletteDrop(
                new CanvasIntentKey(intentId, layout),
                requirePaletteDropToken(body),
                parseStableId(body, "parentWidgetId"),
                new SlotName(slot),
                requireInsertionIndex(body));
    }

    private static CanvasRevisionKey readRevision(
            CanvasSessionId sessionId,
            JsonNode body) throws CanvasRunnerControlException {
        return new CanvasRevisionKey(
                sessionId,
                requireSequence(body, "presentationSequence"),
                StableId.parse(requireText(body, "documentId")),
                requireSequence(body, "logicalRevisionId"));
    }

    private byte[] encode(JsonWriter writer) throws CanvasRunnerControlException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(512);
        try (JsonGenerator json = jsonFactory.createGenerator(bytes)) {
            writer.write(json);
        } catch (IOException | RuntimeException failure) {
            throw new CanvasRunnerControlException(
                    "Could not encode Canvas runtime control message.", failure);
        }
        byte[] encoded = bytes.toByteArray();
        if (encoded.length == 0 || encoded.length > MAX_CONTROL_BYTES) {
            throw new CanvasRunnerControlException(
                    "Canvas runtime control message violates its byte limit.");
        }
        return encoded;
    }

    private static void writeEnvelopeStart(
            JsonGenerator json,
            CanvasSessionId sessionId,
            String type) throws IOException {
        json.writeStartObject();
        json.writeStringField("format", FORMAT);
        json.writeNumberField("protocolVersion", VERSION);
        json.writeStringField("sessionId", sessionId.toString());
        json.writeStringField("type", type);
        json.writeObjectFieldStart("body");
    }

    private static void writeRevision(JsonGenerator json, CanvasRevisionKey revision)
            throws IOException {
        json.writeNumberField(
                "presentationSequence", revision.presentationSequence());
        json.writeStringField("documentId", revision.documentId().toString());
        json.writeNumberField("logicalRevisionId", revision.logicalRevisionId());
    }

    private static void writeEnvelopeEnd(JsonGenerator json) throws IOException {
        json.writeEndObject();
        json.writeEndObject();
    }

    private static JsonNode required(JsonNode object, String name)
            throws CanvasRunnerControlException {
        JsonNode value = object.get(name);
        if (value == null) {
            throw invalid("Canvas runtime control message is missing " + name + '.');
        }
        return value;
    }

    private static String requireText(JsonNode object, String name)
            throws CanvasRunnerControlException {
        JsonNode value = required(object, name);
        if (!value.isTextual() || value.textValue().isEmpty()) {
            throw invalid("Canvas runtime field " + name + " must be text.");
        }
        return value.textValue();
    }

    private static void requireText(JsonNode object, String name, String expected)
            throws CanvasRunnerControlException {
        if (!expected.equals(requireText(object, name))) {
            throw invalid("Canvas runtime field " + name + " is not supported.");
        }
    }

    private static void requireExactInt(JsonNode object, String name, int expected)
            throws CanvasRunnerControlException {
        JsonNode value = required(object, name);
        if (!value.isIntegralNumber() || !value.canConvertToInt()
                || value.intValue() != expected) {
            throw invalid("Canvas runtime field " + name + " is not supported.");
        }
    }

    private static long requireSequence(JsonNode object, String name)
            throws CanvasRunnerControlException {
        JsonNode value = required(object, name);
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw invalid("Canvas runtime field " + name + " must be an integer.");
        }
        long result = value.longValue();
        if (result < 0 || result > CanvasWireProtocol.MAX_SEQUENCE) {
            throw invalid("Canvas runtime field " + name + " is outside its range.");
        }
        return result;
    }

    private static String requirePaletteDropToken(JsonNode object)
            throws CanvasRunnerControlException {
        String token = requireText(object, "token");
        if (token.length() > MAX_PALETTE_DROP_TOKEN_CHARACTERS
                || !token.startsWith(PALETTE_DROP_TOKEN_PREFIX)) {
            throw invalid("Canvas runtime field token is not a supported Palette drop token.");
        }
        for (int index = 0; index < token.length(); index++) {
            char character = token.charAt(index);
            if (character < 0x20 || character > 0x7e) {
                throw invalid("Canvas runtime field token must contain printable ASCII only.");
            }
        }
        return token;
    }

    private static StableId parseStableId(JsonNode object, String name)
            throws CanvasRunnerControlException {
        try {
            return StableId.parse(requireText(object, name));
        } catch (IllegalArgumentException failure) {
            throw invalid("Canvas runtime field " + name + " is not a valid stable id.");
        }
    }

    private static int requireInsertionIndex(JsonNode object)
            throws CanvasRunnerControlException {
        JsonNode value = required(object, "insertionIndex");
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw invalid("Canvas runtime field insertionIndex must be an integer.");
        }
        int index = value.intValue();
        if (index < 0 || index > WidgetSlot.MAX_LIST_CHILDREN) {
            throw invalid("Canvas runtime field insertionIndex is outside its range.");
        }
        return index;
    }

    private static void requireFields(JsonNode object, Set<String> expected, String label)
            throws CanvasRunnerControlException {
        Iterator<String> names = object.fieldNames();
        int count = 0;
        while (names.hasNext()) {
            String name = names.next();
            count++;
            if (!expected.contains(name)) {
                throw invalid("Canvas " + label + " contains unknown field " + name + '.');
            }
        }
        if (count != expected.size()) {
            throw invalid("Canvas " + label + " is missing a required field.");
        }
    }

    private static CanvasRunnerControlException invalid(String message) {
        return new CanvasRunnerControlException(message);
    }

    @FunctionalInterface
    private interface JsonWriter {
        void write(JsonGenerator json) throws IOException;
    }
}
