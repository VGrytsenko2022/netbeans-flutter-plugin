package dev.flutter.netbeans.designer.canvas.payload;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import dev.flutter.netbeans.designer.canvas.CanvasRenderProfile;
import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import dev.flutter.netbeans.designer.canvas.CanvasThemeColorValue;
import dev.flutter.netbeans.designer.canvas.CanvasThemeTextStyleOverride;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireHandshakeLimits;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCapabilityCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCapability;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Map;

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
    public static final int VERSION = 5;
    private static final int MAX_PAYLOAD_BYTES =
            CanvasWireHandshakeLimits.MAX_MODEL_BYTES;
    private final JsonFactory jsonFactory = JsonFactory.builder().build();

    /**
     * Returns whether the definition is one of the exact reviewed built-in
     * contracts admitted to the native Canvas projection.
     *
     * <p>Payload admission is delegated to the shared capability catalog;
     * context UI must request its own capability (for example, {@code CREATE}
     * or {@code DND}) from that same gate. A contributed or altered definition
     * cannot become renderable merely by reusing a supported type id.</p>
     */
    public static boolean supports(WidgetDefinition definition) {
        return BuiltInWidgetCapabilityCatalog.supports(
                Objects.requireNonNull(definition, "definition"),
                WidgetCapability.CANVAS);
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
            var active = request.snapshot().catalog().find(widget.type())
                    .orElseThrow(() -> new CanvasModelPayloadException(
                            "The active Canvas catalog is missing widget type " + type + '.'));
            var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(active)
                    .orElseThrow(() -> new CanvasModelPayloadException(
                            "The reviewed Canvas catalog does not render widget type "
                            + type + '.'));
            for (Map.Entry<PropertyName, PropertyValue> entry
                    : widget.properties().entrySet()) {
                var kinds = projection.properties().get(entry.getKey());
                if (kinds == null || !kinds.contains(entry.getValue().kind())) {
                    throw new CanvasModelPayloadException(
                            "The reviewed Canvas catalog cannot project property "
                            + entry.getKey().value() + " on " + type + '.');
                }
            }
            for (Map.Entry<SlotName, WidgetSlot> entry : widget.slots().entrySet()) {
                if (!projection.slots().contains(entry.getKey())) {
                    throw new CanvasModelPayloadException(
                            "The reviewed Canvas catalog cannot project slot "
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
        json.writeObjectFieldStart("theme");
        json.writeStringField("definitionId", profile.theme().definitionId());
        json.writeStringField("seedArgb", profile.theme().seedArgbLiteral());
        json.writeStringField(
                "brightness", profile.theme().brightness().name().toLowerCase(
                        java.util.Locale.ROOT));
        json.writeStringField("digestIdentity", profile.theme().digestIdentity());
        json.writeObjectFieldStart("colorScheme");
        for (Map.Entry<String, Integer> entry
                : profile.theme().colorSchemeOverrides().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()).toList()) {
            json.writeStringField(entry.getKey(), "0x%08X".formatted(entry.getValue()));
        }
        json.writeEndObject();
        json.writeObjectFieldStart("textTheme");
        for (Map.Entry<String, CanvasThemeTextStyleOverride> entry
                : profile.theme().textThemeOverrides().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()).toList()) {
            json.writeObjectFieldStart(entry.getKey());
            writeTextStyleOverride(json, entry.getValue());
            json.writeEndObject();
        }
        json.writeEndObject();
        json.writeEndObject();
        json.writeStringField("locale", profile.locale().languageTag());
        json.writeNumberField("textScaleFactor", profile.textScaleFactor().value());
        json.writeEndObject();
    }

    private static void writeTextStyleOverride(
            JsonGenerator json, CanvasThemeTextStyleOverride style) throws IOException {
        writeThemeColor(json, "color", style.color());
        writeThemeColor(json, "backgroundColor", style.backgroundColor());
        writeDouble(json, "fontSize", style.fontSize());
        if (style.fontWeight().isPresent()) {
            json.writeStringField("fontWeight", style.fontWeight().orElseThrow());
        }
        if (style.fontStyle().isPresent()) {
            json.writeStringField("fontStyle", style.fontStyle().orElseThrow());
        }
        writeDouble(json, "letterSpacing", style.letterSpacing());
        writeDouble(json, "wordSpacing", style.wordSpacing());
        writeDouble(json, "height", style.height());
        if (style.fontFamily().isPresent()) {
            json.writeStringField("fontFamily", style.fontFamily().orElseThrow());
        }
        if (style.decoration().isPresent()) {
            json.writeArrayFieldStart("decoration");
            for (String line : java.util.List.of(
                    "underline", "overline", "lineThrough")) {
                if (style.decoration().orElseThrow().contains(line)) {
                    json.writeString(line);
                }
            }
            json.writeEndArray();
        }
        writeThemeColor(json, "decorationColor", style.decorationColor());
        if (style.decorationStyle().isPresent()) {
            json.writeStringField("decorationStyle", style.decorationStyle().orElseThrow());
        }
        writeDouble(json, "decorationThickness", style.decorationThickness());
    }

    private static void writeThemeColor(
            JsonGenerator json,
            String field,
            java.util.Optional<CanvasThemeColorValue> value) throws IOException {
        if (value.isEmpty()) {
            return;
        }
        json.writeObjectFieldStart(field);
        switch (value.orElseThrow()) {
            case CanvasThemeColorValue.Literal literal -> {
                json.writeStringField("kind", "argb");
                json.writeStringField("argb", literal.argbLiteral());
            }
            case CanvasThemeColorValue.ColorRole role -> {
                json.writeStringField("kind", "colorScheme");
                json.writeStringField("role", role.role());
            }
        }
        json.writeEndObject();
    }

    private static void writeDouble(
            JsonGenerator json, String field, java.util.Optional<Double> value)
            throws IOException {
        if (value.isPresent()) {
            json.writeNumberField(field, value.orElseThrow());
        }
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
            case PropertyValue.EdgeInsetsDirectionalValue insets -> {
                json.writeStringField("kind", "edgeInsetsDirectional");
                json.writeNumberField("start", insets.start());
                json.writeNumberField("top", insets.top());
                json.writeNumberField("end", insets.end());
                json.writeNumberField("bottom", insets.bottom());
            }
            case PropertyValue.ThemeTokenValue token -> {
                json.writeStringField("kind", "themeToken");
                json.writeStringField("token", token.token().wireId());
            }
            case PropertyValue.PaintValue paint -> writePaint(json, paint);
            case PropertyValue.ShadowListValue shadows -> writeShadows(json, shadows);
            case PropertyValue.FontFeatureListValue features -> writeFontFeatures(json, features);
            case PropertyValue.FontVariationListValue variations ->
                writeFontVariations(json, variations);
            case PropertyValue.AssetValue ignored -> throw unsupported(value);
            case PropertyValue.CallbackValue ignored -> throw unsupported(value);
            case PropertyValue.DartExpressionValue ignored -> throw unsupported(value);
        }
        json.writeEndObject();
    }

    private static void writePaint(JsonGenerator json, PropertyValue.PaintValue paint)
            throws IOException {
        json.writeStringField("kind", "paint");
        json.writeFieldName("color");
        writeColorSource(json, paint.color());
        json.writeStringField("blendMode", paint.blendMode().wireName());
        json.writeStringField("style", paint.style().wireName());
        json.writeNumberField("strokeWidth", paint.strokeWidth());
        json.writeStringField("strokeCap", paint.strokeCap().wireName());
        json.writeStringField("strokeJoin", paint.strokeJoin().wireName());
        json.writeNumberField("strokeMiterLimit", paint.strokeMiterLimit());
        json.writeBooleanField("antiAlias", paint.antiAlias());
        json.writeStringField("filterQuality", paint.filterQuality().wireName());
        json.writeBooleanField("invertColors", paint.invertColors());
        if (paint.maskFilter().isPresent()) {
            PropertyValue.PaintValue.BlurMask mask = paint.maskFilter().orElseThrow();
            json.writeObjectFieldStart("maskFilter");
            json.writeStringField("style", mask.style().wireName());
            json.writeNumberField("sigma", mask.sigma());
            json.writeEndObject();
        }
    }

    private static void writeShadows(
            JsonGenerator json,
            PropertyValue.ShadowListValue shadows) throws IOException {
        json.writeStringField("kind", "shadowList");
        json.writeArrayFieldStart("items");
        for (PropertyValue.ShadowListValue.Shadow shadow : shadows.items()) {
            json.writeStartObject();
            json.writeStringField("id", shadow.id().toString());
            json.writeFieldName("color");
            writeColorSource(json, shadow.color());
            json.writeNumberField("offsetX", shadow.offsetX());
            json.writeNumberField("offsetY", shadow.offsetY());
            json.writeNumberField("blurRadius", shadow.blurRadius());
            json.writeEndObject();
        }
        json.writeEndArray();
    }

    private static void writeFontFeatures(
            JsonGenerator json,
            PropertyValue.FontFeatureListValue features) throws IOException {
        json.writeStringField("kind", "fontFeatureList");
        json.writeArrayFieldStart("items");
        for (PropertyValue.FontFeatureListValue.FontFeature feature : features.items()) {
            json.writeStartObject();
            json.writeStringField("id", feature.id().toString());
            json.writeStringField("tag", feature.tag());
            json.writeNumberField("value", feature.value());
            json.writeEndObject();
        }
        json.writeEndArray();
    }

    private static void writeFontVariations(
            JsonGenerator json,
            PropertyValue.FontVariationListValue variations) throws IOException {
        json.writeStringField("kind", "fontVariationList");
        json.writeArrayFieldStart("items");
        for (PropertyValue.FontVariationListValue.FontVariation variation : variations.items()) {
            json.writeStartObject();
            json.writeStringField("id", variation.id().toString());
            json.writeStringField("axis", variation.axis());
            json.writeNumberField("value", variation.value());
            json.writeEndObject();
        }
        json.writeEndArray();
    }

    private static void writeColorSource(JsonGenerator json, ColorSource source)
            throws IOException {
        json.writeStartObject();
        switch (source) {
            case ColorSource.Literal literal -> {
                json.writeStringField("kind", "literal");
                json.writeStringField("argb", literal.wireArgb());
            }
            case ColorSource.Theme theme -> {
                json.writeStringField("kind", "theme");
                json.writeStringField("token", theme.token().wireId());
            }
        }
        json.writeEndObject();
    }

    private static IllegalStateException unsupported(PropertyValue value) {
        return new IllegalStateException(
                "The reviewed Canvas catalog cannot encode " + value.kind().wireName());
    }
}
