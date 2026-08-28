package dev.flutter.netbeans.designer.canvas.payload;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import dev.flutter.netbeans.designer.canvas.CanvasRenderProfile;
import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireHandshakeLimits;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical projection of one validated Designer revision for the isolated
 * native Flutter renderer.
 *
 * <p>The projection intentionally excludes source paths, managed-region
 * hashes, extension bags, callback names and arbitrary Dart expressions. The
 * runner receives only semantic widget values needed for read-only rendering
 * and cannot turn this payload into file or Designer-command authority.</p>
 */
public final class CanvasModelPayloadCodec {
    public static final String FORMAT = "netbeans-flutter-canvas-model";
    public static final int VERSION = 1;
    private static final int MAX_PAYLOAD_BYTES =
            CanvasWireHandshakeLimits.MAX_MODEL_BYTES;
    private static final Map<String, Map<String, Set<PropertyValueKind>>> PROPERTIES = Map.of(
            "flutter.material.Scaffold", Map.of(
                    "backgroundColor", Set.of(PropertyValueKind.COLOR),
                    "resizeToAvoidBottomInset", Set.of(PropertyValueKind.BOOLEAN)),
            "flutter.widgets.Column", Map.of(
                    "mainAxisAlignment", Set.of(PropertyValueKind.ENUM),
                    "mainAxisSize", Set.of(PropertyValueKind.ENUM),
                    "crossAxisAlignment", Set.of(PropertyValueKind.ENUM),
                    "textDirection", Set.of(PropertyValueKind.ENUM),
                    "verticalDirection", Set.of(PropertyValueKind.ENUM),
                    "textBaseline", Set.of(PropertyValueKind.ENUM),
                    "spacing", Set.of(PropertyValueKind.DOUBLE)),
            "flutter.widgets.Row", Map.of(
                    "mainAxisAlignment", Set.of(PropertyValueKind.ENUM),
                    "mainAxisSize", Set.of(PropertyValueKind.ENUM),
                    "crossAxisAlignment", Set.of(PropertyValueKind.ENUM),
                    "textDirection", Set.of(PropertyValueKind.ENUM),
                    "verticalDirection", Set.of(PropertyValueKind.ENUM),
                    "textBaseline", Set.of(PropertyValueKind.ENUM),
                    "spacing", Set.of(PropertyValueKind.DOUBLE)),
            "flutter.widgets.Padding", Map.of(
                    "padding", Set.of(PropertyValueKind.EDGE_INSETS)),
            "flutter.widgets.Center", Map.of(
                    "widthFactor", Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    "heightFactor", Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE)),
            "flutter.widgets.Text", Map.of(
                    "data", Set.of(PropertyValueKind.STRING),
                    "textAlign", Set.of(PropertyValueKind.ENUM),
                    "textDirection", Set.of(PropertyValueKind.ENUM),
                    "softWrap", Set.of(PropertyValueKind.BOOLEAN),
                    "maxLines", Set.of(PropertyValueKind.INTEGER),
                    "overflow", Set.of(PropertyValueKind.ENUM),
                    "semanticsLabel", Set.of(PropertyValueKind.STRING),
                    "semanticsIdentifier", Set.of(PropertyValueKind.STRING),
                    "textWidthBasis", Set.of(PropertyValueKind.ENUM),
                    "selectionColor", Set.of(PropertyValueKind.COLOR)));
    private static final Map<String, Set<String>> SLOTS = Map.of(
            "flutter.material.Scaffold", Set.of("appBar", "body", "floatingActionButton"),
            "flutter.widgets.Column", Set.of("children"),
            "flutter.widgets.Row", Set.of("children"),
            "flutter.widgets.Padding", Set.of("child"),
            "flutter.widgets.Center", Set.of("child"),
            "flutter.widgets.Text", Set.of());

    private final JsonFactory jsonFactory = JsonFactory.builder().build();

    /**
     * Returns whether the definition is one of the reviewed built-in widget
     * contracts rendered by the version 1 native Canvas projection.
     *
     * <p>This is the single Java-side capability predicate for both payload
     * admission and context UI such as the NetBeans Palette. A contributed or
     * altered definition cannot become renderable merely by reusing a supported
     * type id.</p>
     */
    public static boolean supports(WidgetDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        String type = definition.typeId().value();
        if (!PROPERTIES.containsKey(type) || !SLOTS.containsKey(type)) {
            return false;
        }
        return BuiltInWidgetCatalog.getDefault().find(definition.typeId())
                .filter(definition::equals)
                .isPresent();
    }

    /** Encodes compact UTF-8 JSON in stable field and map-key order. */
    public byte[] encode(CanvasRenderRequest request)
            throws CanvasModelPayloadException {
        Objects.requireNonNull(request, "request");
        validateProjection(request);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(16 * 1024);
        try (JsonGenerator json = jsonFactory.createGenerator(bytes)) {
            json.writeStartObject();
            json.writeStringField("format", FORMAT);
            json.writeNumberField("protocolVersion", VERSION);
            json.writeStringField(
                    "sessionId", request.revisionKey().sessionId().toString());
            json.writeNumberField(
                    "presentationSequence",
                    request.revisionKey().presentationSequence());
            json.writeStringField(
                    "documentId", request.revisionKey().documentId().toString());
            json.writeNumberField(
                    "logicalRevisionId",
                    request.revisionKey().logicalRevisionId());
            writeProfile(json, request.renderProfile());
            json.writeFieldName("root");
            writeWidget(json, request.snapshot().document().root());
            json.writeEndObject();
        } catch (IOException | RuntimeException failure) {
            throw new CanvasModelPayloadException(
                    "Could not encode the validated Canvas model payload.", failure);
        }
        byte[] encoded = bytes.toByteArray();
        if (encoded.length == 0 || encoded.length > MAX_PAYLOAD_BYTES) {
            throw new CanvasModelPayloadException(
                    "Canvas model payload exceeds the " + MAX_PAYLOAD_BYTES
                    + " byte safety limit.");
        }
        return encoded;
    }

    private static void validateProjection(CanvasRenderRequest request)
            throws CanvasModelPayloadException {
        Deque<WidgetNode> pending = new ArrayDeque<>();
        pending.add(request.snapshot().document().root());
        while (!pending.isEmpty()) {
            WidgetNode widget = pending.removeFirst();
            String type = widget.type().value();
            Map<String, Set<PropertyValueKind>> properties = PROPERTIES.get(type);
            Set<String> slots = SLOTS.get(type);
            if (properties == null || slots == null) {
                throw new CanvasModelPayloadException(
                        "Canvas CORE_V1 does not render widget type " + type + '.');
            }
            var active = request.snapshot().catalog().find(widget.type())
                    .orElseThrow(() -> new CanvasModelPayloadException(
                            "The active Canvas catalog is missing widget type " + type + '.'));
            if (!supports(active)) {
                throw new CanvasModelPayloadException(
                        "Canvas CORE_V1 requires the reviewed built-in definition for "
                        + type + '.');
            }
            for (Map.Entry<PropertyName, PropertyValue> entry
                    : widget.properties().entrySet()) {
                Set<PropertyValueKind> kinds = properties.get(entry.getKey().value());
                if (kinds == null || !kinds.contains(entry.getValue().kind())) {
                    throw new CanvasModelPayloadException(
                            "Canvas CORE_V1 cannot project property "
                            + entry.getKey().value() + " on " + type + '.');
                }
            }
            for (Map.Entry<SlotName, WidgetSlot> entry : widget.slots().entrySet()) {
                if (!slots.contains(entry.getKey().value())) {
                    throw new CanvasModelPayloadException(
                            "Canvas CORE_V1 cannot project slot "
                            + entry.getKey().value() + " on " + type + '.');
                }
                switch (entry.getValue()) {
                    case WidgetSlot.SingleSlot single ->
                        single.child().ifPresent(pending::addLast);
                    case WidgetSlot.ListSlot list -> pending.addAll(list.children());
                }
            }
        }
    }

    private static void writeProfile(JsonGenerator json, CanvasRenderProfile profile)
            throws IOException {
        json.writeObjectFieldStart("profile");
        json.writeStringField(
                "previewMode", profile.previewMode().name().toLowerCase(
                        java.util.Locale.ROOT));
        json.writeStringField(
                "targetPlatform", profile.targetPlatform().name().toLowerCase(
                        java.util.Locale.ROOT));
        json.writeNumberField("logicalWidth", profile.viewport().logicalWidth());
        json.writeNumberField("logicalHeight", profile.viewport().logicalHeight());
        json.writeNumberField("devicePixelRatio", profile.devicePixelRatio().value());
        json.writeStringField(
                "brightness", profile.theme().brightness().name().toLowerCase(
                        java.util.Locale.ROOT));
        json.writeStringField("themeIdentity", profile.theme().themeIdentity());
        json.writeStringField("locale", profile.locale().languageTag());
        json.writeNumberField("textScaleFactor", profile.textScaleFactor().value());
        json.writeEndObject();
    }

    private static void writeWidget(JsonGenerator json, WidgetNode widget)
            throws IOException {
        json.writeStartObject();
        json.writeStringField("id", widget.id().toString());
        json.writeStringField("type", widget.type().value());
        json.writeObjectFieldStart("properties");
        for (Map.Entry<?, PropertyValue> entry : widget.properties().entrySet().stream()
                .sorted(java.util.Comparator.comparing(value -> value.getKey().toString()))
                .toList()) {
            json.writeFieldName(entry.getKey().toString());
            writeProperty(json, entry.getValue());
        }
        json.writeEndObject();
        json.writeObjectFieldStart("slots");
        for (Map.Entry<?, WidgetSlot> entry : widget.slots().entrySet().stream()
                .sorted(java.util.Comparator.comparing(value -> value.getKey().toString()))
                .toList()) {
            json.writeFieldName(entry.getKey().toString());
            writeSlot(json, entry.getValue());
        }
        json.writeEndObject();
        json.writeEndObject();
    }

    private static void writeSlot(JsonGenerator json, WidgetSlot slot)
            throws IOException {
        json.writeStartObject();
        switch (slot) {
            case WidgetSlot.SingleSlot single -> {
                json.writeStringField("kind", "single");
                json.writeFieldName("child");
                if (single.child().isPresent()) {
                    writeWidget(json, single.child().orElseThrow());
                } else {
                    json.writeNull();
                }
            }
            case WidgetSlot.ListSlot list -> {
                json.writeStringField("kind", "list");
                json.writeArrayFieldStart("children");
                for (WidgetNode child : list.children()) {
                    writeWidget(json, child);
                }
                json.writeEndArray();
            }
        }
        json.writeEndObject();
    }

    private static void writeProperty(JsonGenerator json, PropertyValue value)
            throws IOException {
        json.writeStartObject();
        switch (value) {
            case PropertyValue.StringValue string -> {
                json.writeStringField("kind", "string");
                json.writeStringField("value", string.value());
            }
            case PropertyValue.BooleanValue bool -> {
                json.writeStringField("kind", "boolean");
                json.writeBooleanField("value", bool.value());
            }
            case PropertyValue.IntegerValue integer -> {
                json.writeStringField("kind", "integer");
                json.writeNumberField("value", integer.value());
            }
            case PropertyValue.DoubleValue decimal -> {
                json.writeStringField("kind", "double");
                json.writeNumberField("value", decimal.value());
            }
            case PropertyValue.EnumValue enumeration -> {
                json.writeStringField("kind", "enum");
                json.writeStringField("type", enumeration.type());
                json.writeStringField("value", enumeration.value());
            }
            case PropertyValue.ColorValue color -> {
                json.writeStringField("kind", "color");
                json.writeStringField("argb", color.wireArgb());
            }
            case PropertyValue.EdgeInsetsValue insets -> {
                json.writeStringField("kind", "edgeInsets");
                json.writeNumberField("left", insets.left());
                json.writeNumberField("top", insets.top());
                json.writeNumberField("right", insets.right());
                json.writeNumberField("bottom", insets.bottom());
            }
            case PropertyValue.AssetValue ignored -> throw unsupported(value);
            case PropertyValue.CallbackValue ignored -> throw unsupported(value);
            case PropertyValue.DartExpressionValue ignored -> throw unsupported(value);
        }
        json.writeEndObject();
    }

    private static IllegalStateException unsupported(PropertyValue value) {
        return new IllegalStateException(
                "Canvas CORE_V1 cannot encode " + value.kind().wireName());
    }
}
