package dev.flutter.netbeans.plugin.designer.canvas;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.flutter.netbeans.designer.canvas.CanvasFrameKey;
import dev.flutter.netbeans.designer.canvas.CanvasImageResource;
import dev.flutter.netbeans.designer.canvas.CanvasIntentId;
import dev.flutter.netbeans.designer.canvas.CanvasIntentKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasRenderProfile;
import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.CanvasSurfaceMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.canvas.CanvasZoomMode;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireLimits;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireProtocol;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameKind;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessPayloadDescriptor;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Strict small-message codec layered beside the frozen lifecycle wire v1. */
public final class CanvasRunnerControlCodec {
    public static final String FORMAT = "netbeans-flutter-canvas-runtime";
    public static final int VERSION = 1;
    private static final int MAX_CONTROL_BYTES = 256 * 1024;
    private static final Set<String> ROOT_FIELDS = Set.of(
            "format", "protocolVersion", "sessionId", "type", "body");
    private static final Set<String> PRESENTED_FIELDS = Set.of(
            "presentationSequence", "documentId", "logicalRevisionId",
            "frameSequence", "layoutSequence", "physicalWidth",
            "physicalHeight", "devicePixelRatioMicros");
    private static final Set<String> SELECTION_FIELDS = Set.of(
            "presentationSequence", "documentId", "logicalRevisionId",
            "frameSequence", "layoutSequence", "intentSequence", "widgetId");
    private static final Set<String> INTERACTION_FIELDS = Set.of(
            "presentationSequence", "documentId", "logicalRevisionId",
            "frameSequence", "layoutSequence", "intentSequence",
            "interactionFenceSequence");
    private static final Set<String> INTERACTION_FENCE_APPLIED_FIELDS = Set.of(
            "presentationSequence", "documentId", "logicalRevisionId",
            "frameSequence", "layoutSequence", "interactionFenceSequence");
    private static final Set<String> DELETE_SELECTION_FIELDS = SELECTION_FIELDS;
    private static final Set<String> TEXT_EDIT_COMMIT_FIELDS = Set.of(
            "presentationSequence", "documentId", "logicalRevisionId",
            "frameSequence", "layoutSequence", "intentSequence",
            "interactionFenceSequence", "widgetId", "text",
            "compositionObserved");
    private static final Set<String> PALETTE_DROP_FIELDS = Set.of(
            "presentationSequence", "documentId", "logicalRevisionId",
            "frameSequence", "layoutSequence", "intentSequence", "token",
            "operation", "parentWidgetId", "slotName", "insertionIndex");
    private static final Set<String> RUNNER_VIEWPORT_FIELDS = Set.of(
            "presentationSequence", "documentId", "logicalRevisionId",
            "commandSequence", "mode", "zoomMicros", "horizontalScrollMicros",
            "verticalScrollMicros", "effectiveScaleMicros",
            "horizontalScrollable", "verticalScrollable");
    private static final String PALETTE_DROP_TOKEN_PREFIX = "nbfdnd:v1:";
    private static final int MAX_PALETTE_DROP_TOKEN_CHARACTERS = 160;
    private static final int MAX_PALETTE_SOURCE_TRAITS = 32;
    private static final Pattern PALETTE_DROP_TOKEN = Pattern.compile(
            "^nbfdnd:v1:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-"
            + "[89ab][0-9a-f]{3}-[0-9a-f]{12}:[0-9a-f]{8}-[0-9a-f]{4}-"
            + "[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    private static final Pattern WIDGET_TRAIT = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9_.-]{0,254}$");
    private static final Set<String> PALETTE_DROP_SLOTS = Set.of(
            "children",
            "child",
            "body",
            "floatingActionButton",
            "appBar",
            "leading",
            "title",
            "actions",
            "flexibleSpace",
            "bottom");

    private final JsonFactory jsonFactory;
    private final ObjectMapper mapper;

    public CanvasRunnerControlCodec() {
        jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxDocumentLength(MAX_CONTROL_BYTES)
                        .maxNestingDepth(16)
                        .maxTokenCount(128)
                        .maxNameLength(64)
                        .maxStringLength(CanvasWireLimits.DEFAULT_MAX_STRING_UTF16_UNITS)
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
        List<CanvasImageResource> images = request.imageResources().resources();
        return encode(json -> {
            writeEnvelopeStart(json, request.revisionKey().sessionId(), "host.render");
            writeRevision(json, request.revisionKey());
            json.writeObjectFieldStart("model");
            json.writeStringField("kind", "model.json");
            json.writeNumberField("payloadBytes", model.payloadBytes());
            json.writeStringField(
                    "sha256", HexFormat.of().formatHex(model.copySha256()));
            json.writeEndObject();
            json.writeArrayFieldStart("images");
            for (CanvasImageResource image : images) {
                json.writeStartObject();
                json.writeStringField("resourceId", image.resourceId());
                json.writeStringField("kind", "image.bytes");
                json.writeStringField("mediaType", image.format().mediaType());
                json.writeNumberField("pixelWidth", image.pixelWidth());
                json.writeNumberField("pixelHeight", image.pixelHeight());
                json.writeNumberField(
                        "payloadBytes", image.encodedByteLength());
                json.writeStringField("sha256", image.resourceId());
                json.writeEndObject();
            }
            json.writeEndArray();
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

    /** Advances the host-owned interaction fence for one exact Canvas layout. */
    public byte[] encodeInteractionFence(
            CanvasLayoutKey layout,
            long interactionFenceSequence) throws CanvasRunnerControlException {
        Objects.requireNonNull(layout, "layout");
        requireInteractionFenceSequence(interactionFenceSequence);
        return encode(json -> {
            CanvasRevisionKey revision = layout.frameKey().revisionKey();
            writeEnvelopeStart(
                    json, revision.sessionId(), "host.interactionFence");
            writeRevision(json, revision);
            json.writeNumberField(
                    "frameSequence", layout.frameKey().frameSequence());
            json.writeNumberField("layoutSequence", layout.layoutSequence());
            json.writeNumberField(
                    "interactionFenceSequence", interactionFenceSequence);
            writeEnvelopeEnd(json);
        });
    }

    /**
     * Binds one opaque native Palette token to its host-authoritative source
     * type and traits for the exact currently presented layout.
     *
     * <p>The transferable remains opaque. This projection grants only enough
     * information for Flutter to filter visual drop zones; Java consumes the
     * token and repeats the canonical catalog compatibility check before any
     * mutation is submitted.</p>
     */
    public byte[] encodePaletteDragSource(
            CanvasLayoutKey layout,
            String token,
            WidgetTypeId widgetType,
            Set<String> traits) throws CanvasRunnerControlException {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(widgetType, "widgetType");
        Objects.requireNonNull(traits, "traits");
        if (token == null
                || token.length() > MAX_PALETTE_DROP_TOKEN_CHARACTERS
                || !PALETTE_DROP_TOKEN.matcher(token).matches()) {
            throw new IllegalArgumentException(
                    "Palette drag source token has an invalid bounded shape.");
        }
        TreeSet<String> orderedTraits = new TreeSet<>();
        for (String trait : traits) {
            if (trait == null || !WIDGET_TRAIT.matcher(trait).matches()) {
                throw new IllegalArgumentException(
                        "Palette drag source contains an invalid widget trait.");
            }
            orderedTraits.add(trait);
        }
        if (orderedTraits.size() > MAX_PALETTE_SOURCE_TRAITS) {
            throw new IllegalArgumentException(
                    "Palette drag source contains too many widget traits.");
        }
        return encode(json -> {
            CanvasRevisionKey revision = layout.frameKey().revisionKey();
            writeEnvelopeStart(
                    json, revision.sessionId(), "host.paletteDragSource");
            writeRevision(json, revision);
            json.writeNumberField(
                    "frameSequence", layout.frameKey().frameSequence());
            json.writeNumberField("layoutSequence", layout.layoutSequence());
            json.writeStringField("token", token);
            json.writeStringField("widgetType", widgetType.value());
            json.writeArrayFieldStart("traits");
            for (String trait : orderedTraits) {
                json.writeString(trait);
            }
            json.writeEndArray();
            writeEnvelopeEnd(json);
        });
    }

    /**
     * Encodes one host-authorized widget move target for the exact presented
     * Flutter layout. The runner receives placement only; catalog
     * compatibility and mutation authority remain in the Java host.
     */
    public byte[] encodeWidgetMovePreview(
            CanvasLayoutKey layout,
            long previewSequence,
            StableId sourceWidgetId,
            StableId parentWidgetId,
            SlotName slotName,
            int insertionIndex) throws CanvasRunnerControlException {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(sourceWidgetId, "sourceWidgetId");
        Objects.requireNonNull(parentWidgetId, "parentWidgetId");
        Objects.requireNonNull(slotName, "slotName");
        requirePreviewSequence(previewSequence);
        requireInsertionIndex(insertionIndex, "host widget move preview");
        return encode(json -> {
            CanvasRevisionKey revision = layout.frameKey().revisionKey();
            writeEnvelopeStart(
                    json, revision.sessionId(), "host.widgetMovePreview");
            writeRevision(json, revision);
            json.writeNumberField(
                    "frameSequence", layout.frameKey().frameSequence());
            json.writeNumberField("layoutSequence", layout.layoutSequence());
            json.writeNumberField("previewSequence", previewSequence);
            json.writeStringField("sourceWidgetId", sourceWidgetId.toString());
            json.writeStringField("parentWidgetId", parentWidgetId.toString());
            json.writeStringField("slotName", slotName.value());
            json.writeNumberField("insertionIndex", insertionIndex);
            writeEnvelopeEnd(json);
        });
    }

    /** Clears the latest widget move target for one exact presented layout. */
    public byte[] encodeWidgetMovePreviewClear(
            CanvasLayoutKey layout,
            long previewSequence) throws CanvasRunnerControlException {
        Objects.requireNonNull(layout, "layout");
        requirePreviewSequence(previewSequence);
        return encode(json -> {
            CanvasRevisionKey revision = layout.frameKey().revisionKey();
            writeEnvelopeStart(
                    json, revision.sessionId(), "host.widgetMovePreviewClear");
            writeRevision(json, revision);
            json.writeNumberField(
                    "frameSequence", layout.frameKey().frameSequence());
            json.writeNumberField("layoutSequence", layout.layoutSequence());
            json.writeNumberField("previewSequence", previewSequence);
            writeEnvelopeEnd(json);
        });
    }

    /** Encodes one presentation request for an exact published revision. */
    public byte[] encodeViewport(
            CanvasRevisionKey revision,
            long commandSequence,
            CanvasViewportPresentation presentation)
            throws CanvasRunnerControlException {
        Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(presentation, "presentation");
        return encode(json -> {
            writeEnvelopeStart(json, revision.sessionId(), "host.viewport");
            writeRevision(json, revision);
            json.writeNumberField(
                    "commandSequence",
                    requireCommandSequence(commandSequence));
            writePresentation(json, presentation);
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
                case "runner.interaction" -> decodeInteraction(sessionId, body);
                case "runner.interactionFenceApplied" ->
                    decodeInteractionFenceApplied(sessionId, body);
                case "runner.paletteDrop" -> decodePaletteDrop(sessionId, body);
                case "runner.deleteSelection" -> decodeDeleteSelection(
                        sessionId, body);
                case "runner.textEditCommit" -> decodeTextEditCommit(
                        sessionId, body);
                case "runner.viewport" -> decodeViewport(sessionId, body);
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
        CanvasLayoutKey layout = new CanvasLayoutKey(
                frame, requireSequence(body, "layoutSequence"));
        CanvasSurfaceMetrics metrics = new CanvasSurfaceMetrics(
                requireBoundedInt(
                        body,
                        "physicalWidth",
                        1,
                        CanvasRenderProfile.MAX_PHYSICAL_DIMENSION),
                requireBoundedInt(
                        body,
                        "physicalHeight",
                        1,
                        CanvasRenderProfile.MAX_PHYSICAL_DIMENSION),
                requireBoundedInt(
                        body,
                        "devicePixelRatioMicros",
                        CanvasSurfaceMetrics.MIN_DEVICE_PIXEL_RATIO_MICROS,
                        CanvasSurfaceMetrics.MAX_DEVICE_PIXEL_RATIO_MICROS));
        return new CanvasRunnerRuntimeEvent.Presented(layout, metrics);
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

    private static CanvasRunnerRuntimeEvent.Interaction decodeInteraction(
            CanvasSessionId sessionId,
            JsonNode body) throws CanvasRunnerControlException {
        requireFields(body, INTERACTION_FIELDS, "runner.interaction body");
        CanvasRevisionKey revision = readRevision(sessionId, body);
        CanvasFrameKey frame = new CanvasFrameKey(
                revision, requireSequence(body, "frameSequence"));
        CanvasLayoutKey layout = new CanvasLayoutKey(
                frame, requireSequence(body, "layoutSequence"));
        CanvasIntentId intentId = new CanvasIntentId(
                sessionId, requireSequence(body, "intentSequence"));
        return new CanvasRunnerRuntimeEvent.Interaction(
                new CanvasIntentKey(intentId, layout),
                requireSequence(body, "interactionFenceSequence"));
    }

    private static CanvasRunnerRuntimeEvent.InteractionFenceApplied
            decodeInteractionFenceApplied(
                    CanvasSessionId sessionId,
                    JsonNode body) throws CanvasRunnerControlException {
        requireFields(
                body,
                INTERACTION_FENCE_APPLIED_FIELDS,
                "runner.interactionFenceApplied body");
        CanvasRevisionKey revision = readRevision(sessionId, body);
        CanvasFrameKey frame = new CanvasFrameKey(
                revision, requireSequence(body, "frameSequence"));
        CanvasLayoutKey layout = new CanvasLayoutKey(
                frame, requireSequence(body, "layoutSequence"));
        return new CanvasRunnerRuntimeEvent.InteractionFenceApplied(
                layout,
                requireSequence(body, "interactionFenceSequence"));
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
        if (!PALETTE_DROP_SLOTS.contains(slot)) {
            throw invalid("Canvas runtime field slotName is not supported.");
        }
        return new CanvasRunnerRuntimeEvent.PaletteDrop(
                new CanvasIntentKey(intentId, layout),
                requirePaletteDropToken(body),
                parseStableId(body, "parentWidgetId"),
                new SlotName(slot),
                requireInsertionIndex(body));
    }

    private static CanvasRunnerRuntimeEvent.DeleteSelection decodeDeleteSelection(
            CanvasSessionId sessionId,
            JsonNode body) throws CanvasRunnerControlException {
        requireFields(
                body, DELETE_SELECTION_FIELDS, "runner.deleteSelection body");
        CanvasRevisionKey revision = readRevision(sessionId, body);
        CanvasFrameKey frame = new CanvasFrameKey(
                revision, requireSequence(body, "frameSequence"));
        CanvasLayoutKey layout = new CanvasLayoutKey(
                frame, requireSequence(body, "layoutSequence"));
        CanvasIntentId intentId = new CanvasIntentId(
                sessionId, requireSequence(body, "intentSequence"));
        return new CanvasRunnerRuntimeEvent.DeleteSelection(
                new CanvasIntentKey(intentId, layout),
                parseStableId(body, "widgetId"));
    }

    private static CanvasRunnerRuntimeEvent.TextEditCommit decodeTextEditCommit(
            CanvasSessionId sessionId,
            JsonNode body) throws CanvasRunnerControlException {
        requireFields(
                body, TEXT_EDIT_COMMIT_FIELDS, "runner.textEditCommit body");
        CanvasRevisionKey revision = readRevision(sessionId, body);
        CanvasFrameKey frame = new CanvasFrameKey(
                revision, requireSequence(body, "frameSequence"));
        CanvasLayoutKey layout = new CanvasLayoutKey(
                frame, requireSequence(body, "layoutSequence"));
        CanvasIntentId intentId = new CanvasIntentId(
                sessionId, requireSequence(body, "intentSequence"));
        return new CanvasRunnerRuntimeEvent.TextEditCommit(
                new CanvasIntentKey(intentId, layout),
                requireSequence(body, "interactionFenceSequence"),
                parseStableId(body, "widgetId"),
                requireInlineText(body),
                requireBoolean(body, "compositionObserved"));
    }

    private static CanvasRunnerRuntimeEvent.ViewportMetrics decodeViewport(
            CanvasSessionId sessionId,
            JsonNode body) throws CanvasRunnerControlException {
        requireFields(body, RUNNER_VIEWPORT_FIELDS, "runner.viewport body");
        CanvasRevisionKey revision = readRevision(sessionId, body);
        long commandSequence = requireSequence(body, "commandSequence");
        CanvasViewportPresentation presentation = readPresentation(body);
        int effectiveScaleMicros = requireBoundedInt(
                body,
                "effectiveScaleMicros",
                CanvasViewportMetrics.MIN_EFFECTIVE_SCALE_MICROS,
                CanvasViewportPresentation.MAX_ZOOM_MICROS);
        return new CanvasRunnerRuntimeEvent.ViewportMetrics(
                new CanvasViewportMetrics(
                        revision,
                        commandSequence,
                        presentation,
                        effectiveScaleMicros,
                        requireBoolean(body, "horizontalScrollable"),
                        requireBoolean(body, "verticalScrollable")));
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

    private static void writePresentation(
            JsonGenerator json,
            CanvasViewportPresentation presentation) throws IOException {
        json.writeStringField(
                "mode",
                presentation.mode() == CanvasZoomMode.FIT ? "fit" : "manual");
        json.writeNumberField("zoomMicros", presentation.zoomMicros());
        json.writeNumberField(
                "horizontalScrollMicros",
                presentation.horizontalScrollMicros());
        json.writeNumberField(
                "verticalScrollMicros",
                presentation.verticalScrollMicros());
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

    private static String requireInlineText(JsonNode object)
            throws CanvasRunnerControlException {
        JsonNode value = required(object, "text");
        if (!value.isTextual()) {
            throw invalid("Canvas runtime field text must be text.");
        }
        String text = value.textValue();
        if (text.length() > CanvasWireLimits.DEFAULT_MAX_STRING_UTF16_UNITS) {
            throw invalid("Canvas runtime field text exceeds its UTF-16 limit.");
        }
        int scalarCount = 0;
        for (int index = 0; index < text.length(); index++) {
            char unit = text.charAt(index);
            if (Character.isHighSurrogate(unit)) {
                if (index + 1 >= text.length()
                        || !Character.isLowSurrogate(text.charAt(index + 1))) {
                    throw invalid(
                            "Canvas runtime field text contains malformed UTF-16.");
                }
                index++;
            } else if (Character.isLowSurrogate(unit)) {
                throw invalid(
                        "Canvas runtime field text contains malformed UTF-16.");
            }
            scalarCount++;
            if (scalarCount > CanvasWireLimits.DEFAULT_MAX_STRING_CODE_POINTS) {
                throw invalid(
                        "Canvas runtime field text exceeds its Unicode scalar limit.");
            }
        }
        return text;
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

    private static long requireCommandSequence(long commandSequence) {
        if (commandSequence < 1
                || commandSequence > CanvasWireProtocol.MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "host viewport commandSequence must be between 1 and "
                    + CanvasWireProtocol.MAX_SEQUENCE);
        }
        return commandSequence;
    }

    private static long requirePreviewSequence(long previewSequence) {
        if (previewSequence < 1
                || previewSequence > CanvasWireProtocol.MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "host widget move previewSequence must be between 1 and "
                    + CanvasWireProtocol.MAX_SEQUENCE);
        }
        return previewSequence;
    }

    private static long requireInteractionFenceSequence(long sequence) {
        if (sequence < 0 || sequence > CanvasWireProtocol.MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "Canvas interaction fence sequence must be between 0 and "
                    + CanvasWireProtocol.MAX_SEQUENCE);
        }
        return sequence;
    }

    private static int requireInsertionIndex(int insertionIndex, String label) {
        if (insertionIndex < 0
                || insertionIndex > WidgetSlot.MAX_LIST_CHILDREN) {
            throw new IllegalArgumentException(
                    label + " insertionIndex must be between 0 and "
                    + WidgetSlot.MAX_LIST_CHILDREN);
        }
        return insertionIndex;
    }

    private static CanvasViewportPresentation readPresentation(JsonNode body)
            throws CanvasRunnerControlException {
        String modeValue = requireText(body, "mode");
        final CanvasZoomMode mode;
        if ("fit".equals(modeValue)) {
            mode = CanvasZoomMode.FIT;
        } else if ("manual".equals(modeValue)) {
            mode = CanvasZoomMode.MANUAL;
        } else {
            throw invalid("Canvas runtime field mode is not supported.");
        }
        int zoomMicros = requireBoundedInt(
                body,
                "zoomMicros",
                CanvasViewportPresentation.MIN_ZOOM_MICROS,
                CanvasViewportPresentation.MAX_ZOOM_MICROS);
        int horizontalScrollMicros = requireBoundedInt(
                body,
                "horizontalScrollMicros",
                CanvasViewportPresentation.MIN_SCROLL_MICROS,
                CanvasViewportPresentation.MAX_SCROLL_MICROS);
        int verticalScrollMicros = requireBoundedInt(
                body,
                "verticalScrollMicros",
                CanvasViewportPresentation.MIN_SCROLL_MICROS,
                CanvasViewportPresentation.MAX_SCROLL_MICROS);
        return new CanvasViewportPresentation(
                mode,
                zoomMicros,
                horizontalScrollMicros,
                verticalScrollMicros);
    }

    private static int requireBoundedInt(
            JsonNode object,
            String name,
            int minimum,
            int maximum) throws CanvasRunnerControlException {
        JsonNode value = required(object, name);
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw invalid("Canvas runtime field " + name + " must be an integer.");
        }
        int result = value.intValue();
        if (result < minimum || result > maximum) {
            throw invalid("Canvas runtime field " + name + " is outside its range.");
        }
        return result;
    }

    private static boolean requireBoolean(JsonNode object, String name)
            throws CanvasRunnerControlException {
        JsonNode value = required(object, name);
        if (!value.isBoolean()) {
            throw invalid("Canvas runtime field " + name + " must be boolean.");
        }
        return value.booleanValue();
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
