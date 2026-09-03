package dev.flutter.netbeans.designer.canvas.payload;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import dev.flutter.netbeans.designer.canvas.CanvasRenderProfile;
import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import dev.flutter.netbeans.designer.canvas.CanvasImageAsset;
import dev.flutter.netbeans.designer.canvas.CanvasImageAssetId;
import dev.flutter.netbeans.designer.canvas.CanvasImageResolutionIssue;
import dev.flutter.netbeans.designer.canvas.CanvasImageResourceBundle;
import dev.flutter.netbeans.designer.canvas.CanvasImageVariant;
import dev.flutter.netbeans.designer.canvas.CanvasThemeColorValue;
import dev.flutter.netbeans.designer.canvas.CanvasThemeComponentColorRole;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map;
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
    public static final int VERSION = 14;
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
            ProjectionContext context = new ProjectionContext(request);
            json.writeFieldName("root");
            writeWidget(json, request.snapshot().document().root(), context);
            context.requireExactResourceCoverage();
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
                var property = active.property(entry.getKey()).orElseThrow(
                        () -> new CanvasModelPayloadException(
                                "The active Canvas catalog is missing property "
                                + entry.getKey().value() + " on " + type + '.'));
                if (property.constraints().stream().noneMatch(
                        constraint -> constraint.kind() == entry.getValue().kind()
                        && constraint.accepts(entry.getValue()))) {
                    throw new CanvasModelPayloadException(
                            "The reviewed Canvas catalog rejects the value of property "
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
        json.writeObjectFieldStart("components");
        for (CanvasThemeComponentColorRole role
                : CanvasThemeComponentColorRole.values()) {
            CanvasThemeColorValue value = profile.theme().componentColors().get(role);
            if (value != null) {
                writeThemeColor(json, role.wireName(), java.util.Optional.of(value));
            }
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

    private static void writeWidget(
            JsonGenerator json,
            WidgetNode widget,
            ProjectionContext context)
            throws IOException {
        json.writeStartObject();
        json.writeStringField("id", widget.id().toString());
        json.writeStringField("type", widget.type().value());
        json.writeObjectFieldStart("properties");
        for (Map.Entry<?, PropertyValue> entry : widget.properties().entrySet().stream()
                .sorted(java.util.Comparator.comparing(value -> value.getKey().toString()))
                .toList()) {
            json.writeFieldName(entry.getKey().toString());
            writeProperty(json, entry.getValue(), context);
        }
        json.writeEndObject();
        json.writeObjectFieldStart("slots");
        for (Map.Entry<?, WidgetSlot> entry : widget.slots().entrySet().stream()
                .sorted(java.util.Comparator.comparing(value -> value.getKey().toString()))
                .toList()) {
            json.writeFieldName(entry.getKey().toString());
            writeSlot(json, entry.getValue(), context);
        }
        json.writeEndObject();
        json.writeEndObject();
    }

    private static void writeSlot(
            JsonGenerator json,
            WidgetSlot slot,
            ProjectionContext context)
            throws IOException {
        json.writeStartObject();
        switch (slot) {
            case WidgetSlot.SingleSlot single -> {
                json.writeStringField("kind", "single");
                json.writeFieldName("child");
                if (single.child().isPresent()) {
                    writeWidget(json, single.child().orElseThrow(), context);
                } else {
                    json.writeNull();
                }
            }
            case WidgetSlot.ListSlot list -> {
                json.writeStringField("kind", "list");
                json.writeArrayFieldStart("children");
                for (WidgetNode child : list.children()) {
                    writeWidget(json, child, context);
                }
                json.writeEndArray();
            }
        }
        json.writeEndObject();
    }

    private static void writeProperty(
            JsonGenerator json,
            PropertyValue value,
            ProjectionContext context)
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
            case PropertyValue.IconDataValue iconData ->
                writeIconData(json, iconData);
            case PropertyValue.PaintValue paint -> writePaint(json, paint);
            case PropertyValue.ShadowListValue shadows -> writeShadows(json, shadows);
            case PropertyValue.FontFeatureListValue features -> writeFontFeatures(json, features);
            case PropertyValue.FontVariationListValue variations ->
                writeFontVariations(json, variations);
            case PropertyValue.AlignmentGeometryValue alignment ->
                writeAlignment(json, alignment);
            case PropertyValue.OffsetValue offset -> writeOffset(json, offset);
            case PropertyValue.SizeValue size -> writeSize(json, size);
            case PropertyValue.BoxConstraintsValue constraints ->
                writeBoxConstraints(json, constraints);
            case PropertyValue.Matrix4Value matrix -> writeMatrix4(json, matrix);
            case PropertyValue.ImageProviderValue provider -> {
                json.writeStringField("kind", "imageProvider");
                json.writeObjectFieldStart("value");
                writeImageProviderFields(json, provider, context);
                json.writeEndObject();
            }
            case PropertyValue.BoxDecorationValue decoration ->
                writeBoxDecoration(json, decoration, context);
            case PropertyValue.AssetValue ignored -> throw unsupported(value);
            case PropertyValue.CallbackValue ignored ->
                // Executable handler identifiers never cross the Canvas
                // boundary. The isolated runner only receives the fact that a
                // reviewed callback reference is configured.
                json.writeStringField("kind", "callbackPresence");
            case PropertyValue.DartExpressionValue ignored -> throw unsupported(value);
        }
        json.writeEndObject();
    }

    private static void writeIconData(
            JsonGenerator json,
            PropertyValue.IconDataValue iconData) throws IOException {
        json.writeStringField("kind", "iconData");
        if (iconData.codePoint().isPresent()) {
            json.writeNumberField("codePoint", iconData.codePoint().orElseThrow());
        } else {
            json.writeNullField("codePoint");
        }
        if (iconData.fontFamily().isPresent()) {
            json.writeStringField("fontFamily", iconData.fontFamily().orElseThrow());
        } else {
            json.writeNullField("fontFamily");
        }
        if (iconData.fontPackage().isPresent()) {
            json.writeStringField("fontPackage", iconData.fontPackage().orElseThrow());
        } else {
            json.writeNullField("fontPackage");
        }
        json.writeBooleanField(
                "matchTextDirection", iconData.matchTextDirection());
        json.writeArrayFieldStart("fontFamilyFallback");
        for (String fallback : iconData.fontFamilyFallback()) {
            json.writeString(fallback);
        }
        json.writeEndArray();
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

    private static void writeAlignment(
            JsonGenerator json,
            PropertyValue.AlignmentGeometryValue alignment) throws IOException {
        json.writeStringField("kind", "alignmentGeometry");
        writeAlignmentFields(json, alignment);
    }

    private static void writeAlignmentFields(
            JsonGenerator json,
            PropertyValue.AlignmentGeometryValue alignment) throws IOException {
        json.writeStringField("basis", alignment.basis().wireName());
        json.writeNumberField("horizontal", alignment.horizontal());
        json.writeNumberField("vertical", alignment.vertical());
    }

    private static void writeSize(
            JsonGenerator json,
            PropertyValue.SizeValue size) throws IOException {
        json.writeStringField("kind", "size");
        json.writeNumberField("width", size.width());
        json.writeNumberField("height", size.height());
    }

    private static void writeOffset(
            JsonGenerator json,
            PropertyValue.OffsetValue offset) throws IOException {
        json.writeStringField("kind", "offset");
        json.writeNumberField("dx", offset.dx());
        json.writeNumberField("dy", offset.dy());
    }

    private static void writeBoxConstraints(
            JsonGenerator json,
            PropertyValue.BoxConstraintsValue constraints) throws IOException {
        json.writeStringField("kind", "boxConstraints");
        writeBoxConstraintBound(json, "minWidth", constraints.minWidth());
        writeBoxConstraintBound(json, "maxWidth", constraints.maxWidth());
        writeBoxConstraintBound(json, "minHeight", constraints.minHeight());
        writeBoxConstraintBound(json, "maxHeight", constraints.maxHeight());
    }

    private static void writeBoxConstraintBound(
            JsonGenerator json,
            String name,
            PropertyValue.BoxConstraintBound bound) throws IOException {
        if (bound instanceof PropertyValue.BoxConstraintBound.Finite finite) {
            json.writeNumberField(name, finite.value());
        } else {
            json.writeNullField(name);
        }
    }

    private static void writeMatrix4(
            JsonGenerator json,
            PropertyValue.Matrix4Value matrix) throws IOException {
        json.writeStringField("kind", "matrix4");
        json.writeArrayFieldStart("storage");
        for (var value : matrix.storage()) {
            json.writeNumber(value);
        }
        json.writeEndArray();
    }

    private static void writeBoxDecoration(
            JsonGenerator json,
            PropertyValue.BoxDecorationValue decoration,
            ProjectionContext context) throws IOException {
        json.writeStringField("kind", "boxDecoration");
        json.writeFieldName("color");
        if (decoration.color().isPresent()) {
            writeColorSource(json, decoration.color().orElseThrow());
        } else {
            json.writeNull();
        }
        json.writeFieldName("image");
        if (decoration.image().isPresent()) {
            writeDecorationImage(
                    json, decoration.image().orElseThrow(), context);
        } else {
            json.writeNull();
        }
        json.writeFieldName("border");
        if (decoration.border().isPresent()) {
            writeBoxBorder(json, decoration.border().orElseThrow());
        } else {
            json.writeNull();
        }
        json.writeFieldName("borderRadius");
        if (decoration.borderRadius().isPresent()) {
            writeBorderRadius(json, decoration.borderRadius().orElseThrow());
        } else {
            json.writeNull();
        }
        json.writeArrayFieldStart("boxShadow");
        for (PropertyValue.BoxDecorationValue.BoxShadow shadow
                : decoration.boxShadow()) {
            json.writeStartObject();
            json.writeStringField("id", shadow.id().toString());
            json.writeFieldName("color");
            writeColorSource(json, shadow.color());
            json.writeNumberField("offsetX", shadow.offsetX());
            json.writeNumberField("offsetY", shadow.offsetY());
            json.writeNumberField("blurRadius", shadow.blurRadius());
            json.writeNumberField("spreadRadius", shadow.spreadRadius());
            json.writeStringField("blurStyle", shadow.blurStyle().wireName());
            json.writeEndObject();
        }
        json.writeEndArray();
        json.writeFieldName("gradient");
        if (decoration.gradient().isPresent()) {
            writeBoxGradient(json, decoration.gradient().orElseThrow());
        } else {
            json.writeNull();
        }
        if (decoration.backgroundBlendMode().isPresent()) {
            json.writeStringField(
                    "backgroundBlendMode",
                    decoration.backgroundBlendMode().orElseThrow().wireName());
        } else {
            json.writeNullField("backgroundBlendMode");
        }
        json.writeStringField("shape", decoration.shape().wireName());
    }

    private static void writeDecorationImage(
            JsonGenerator json,
            PropertyValue.DecorationImageValue image,
            ProjectionContext context) throws IOException {
        json.writeStartObject();
        json.writeObjectFieldStart("image");
        writeImageProviderFields(json, image.image(), context);
        json.writeEndObject();
        json.writeBooleanField("onError", image.onError().isPresent());
        json.writeFieldName("colorFilter");
        if (image.colorFilter().isPresent()) {
            writeColorFilter(json, image.colorFilter().orElseThrow());
        } else {
            json.writeNull();
        }
        if (image.fit().isPresent()) {
            json.writeStringField("fit", image.fit().orElseThrow().wireName());
        } else {
            json.writeNullField("fit");
        }
        json.writeObjectFieldStart("alignment");
        writeAlignmentFields(json, image.alignment());
        json.writeEndObject();
        json.writeFieldName("centerSlice");
        if (image.centerSlice().isPresent()) {
            PropertyValue.DecorationImageValue.Rect rect =
                    image.centerSlice().orElseThrow();
            json.writeStartObject();
            json.writeNumberField("left", rect.left());
            json.writeNumberField("top", rect.top());
            json.writeNumberField("right", rect.right());
            json.writeNumberField("bottom", rect.bottom());
            json.writeEndObject();
        } else {
            json.writeNull();
        }
        json.writeStringField("repeat", image.repeat().wireName());
        json.writeBooleanField(
                "matchTextDirection", image.matchTextDirection());
        json.writeNumberField("scale", image.scale());
        json.writeNumberField("opacity", image.opacity());
        json.writeStringField(
                "filterQuality", image.filterQuality().wireName());
        json.writeBooleanField("invertColors", image.invertColors());
        json.writeBooleanField("isAntiAlias", image.isAntiAlias());
        json.writeEndObject();
    }

    private static void writeImageProviderFields(
            JsonGenerator json,
            PropertyValue.ImageProviderValue provider,
            ProjectionContext context) throws IOException {
        json.writeStringField("kind", provider.providerKind().wireName());
        json.writeStringField("assetName", provider.assetName());
        if (provider.packageName().isPresent()) {
            json.writeStringField(
                    "packageName", provider.packageName().orElseThrow());
        } else {
            json.writeNullField("packageName");
        }
        if (provider.exactScale().isPresent()) {
            json.writeNumberField(
                    "exactScale", provider.exactScale().orElseThrow());
        } else {
            json.writeNullField("exactScale");
        }
        json.writeFieldName("resize");
        if (provider.resize().isPresent()) {
            PropertyValue.ImageProviderValue.ResizeImageConfig resize =
                    provider.resize().orElseThrow();
            json.writeStartObject();
            if (resize.width().isPresent()) {
                json.writeNumberField("width", resize.width().orElseThrow());
            } else {
                json.writeNullField("width");
            }
            if (resize.height().isPresent()) {
                json.writeNumberField("height", resize.height().orElseThrow());
            } else {
                json.writeNullField("height");
            }
            json.writeStringField("policy", resize.policy().wireName());
            json.writeBooleanField(
                    "allowUpscaling", resize.allowUpscaling());
            json.writeEndObject();
        } else {
            json.writeNull();
        }
        json.writeObjectFieldStart("resolution");
        switch (context.resolve(provider)) {
            case ResolvedImage resolved -> {
                json.writeStringField("kind", "resolved");
                json.writeStringField(
                        "resourceId", resolved.resourceId());
                json.writeNumberField(
                        "resolvedScale", resolved.resolvedScale());
            }
            case UnavailableImage unavailable -> {
                json.writeStringField("kind", "unavailable");
                json.writeStringField(
                        "code", unavailable.issue().code().wireName());
                json.writeStringField(
                        "reason", unavailable.issue().reason());
            }
        }
        json.writeEndObject();
    }

    private static void writeColorFilter(
            JsonGenerator json,
            PropertyValue.DecorationImageValue.ColorFilter filter)
            throws IOException {
        json.writeStartObject();
        json.writeStringField("kind", filter.wireKind());
        switch (filter) {
            case PropertyValue.DecorationImageValue.Mode mode -> {
                json.writeFieldName("color");
                writeColorSource(json, mode.color());
                json.writeStringField(
                        "blendMode", mode.blendMode().wireName());
            }
            case PropertyValue.DecorationImageValue.Matrix matrix -> {
                json.writeArrayFieldStart("values");
                for (var value : matrix.values()) {
                    json.writeNumber(value);
                }
                json.writeEndArray();
            }
            case PropertyValue.DecorationImageValue.Saturation saturation ->
                json.writeNumberField("value", saturation.value());
            case PropertyValue.DecorationImageValue.LinearToSrgbGamma ignored -> {
            }
            case PropertyValue.DecorationImageValue.SrgbToLinearGamma ignored -> {
            }
        }
        json.writeEndObject();
    }

    private static void writeBoxBorder(
            JsonGenerator json,
            PropertyValue.BoxDecorationValue.BoxBorder border) throws IOException {
        json.writeStartObject();
        if (border instanceof PropertyValue.BoxDecorationValue.PhysicalBorder physical) {
            json.writeStringField("kind", "physical");
            writeBorderSide(json, "top", physical.top());
            writeBorderSide(json, "right", physical.right());
            writeBorderSide(json, "bottom", physical.bottom());
            writeBorderSide(json, "left", physical.left());
        } else {
            PropertyValue.BoxDecorationValue.DirectionalBorder directional =
                    (PropertyValue.BoxDecorationValue.DirectionalBorder) border;
            json.writeStringField("kind", "directional");
            writeBorderSide(json, "top", directional.top());
            writeBorderSide(json, "start", directional.start());
            writeBorderSide(json, "end", directional.end());
            writeBorderSide(json, "bottom", directional.bottom());
        }
        json.writeEndObject();
    }

    private static void writeBorderSide(
            JsonGenerator json,
            String name,
            PropertyValue.BoxDecorationValue.BorderSide side) throws IOException {
        json.writeObjectFieldStart(name);
        json.writeFieldName("color");
        writeColorSource(json, side.color());
        json.writeNumberField("width", side.width());
        json.writeStringField("style", side.style().wireName());
        json.writeNumberField("strokeAlign", side.strokeAlign());
        json.writeEndObject();
    }

    private static void writeBorderRadius(
            JsonGenerator json,
            PropertyValue.BoxDecorationValue.BorderRadiusGeometry radius)
            throws IOException {
        json.writeStartObject();
        if (radius instanceof PropertyValue.BoxDecorationValue.PhysicalBorderRadius physical) {
            json.writeStringField("kind", "physical");
            writeRadius(json, "topLeft", physical.topLeft());
            writeRadius(json, "topRight", physical.topRight());
            writeRadius(json, "bottomRight", physical.bottomRight());
            writeRadius(json, "bottomLeft", physical.bottomLeft());
        } else {
            PropertyValue.BoxDecorationValue.DirectionalBorderRadius directional =
                    (PropertyValue.BoxDecorationValue.DirectionalBorderRadius) radius;
            json.writeStringField("kind", "directional");
            writeRadius(json, "topStart", directional.topStart());
            writeRadius(json, "topEnd", directional.topEnd());
            writeRadius(json, "bottomEnd", directional.bottomEnd());
            writeRadius(json, "bottomStart", directional.bottomStart());
        }
        json.writeEndObject();
    }

    private static void writeRadius(
            JsonGenerator json,
            String name,
            PropertyValue.BoxDecorationValue.Radius radius) throws IOException {
        json.writeObjectFieldStart(name);
        json.writeNumberField("x", radius.x());
        json.writeNumberField("y", radius.y());
        json.writeEndObject();
    }

    private static void writeBoxGradient(
            JsonGenerator json,
            PropertyValue.BoxDecorationValue.BoxGradient gradient)
            throws IOException {
        json.writeStartObject();
        if (gradient instanceof PropertyValue.BoxDecorationValue.LinearGradient linear) {
            json.writeStringField("kind", "linear");
            writeNestedAlignment(json, "begin", linear.begin());
            writeNestedAlignment(json, "end", linear.end());
        } else if (gradient instanceof PropertyValue.BoxDecorationValue.RadialGradient radial) {
            json.writeStringField("kind", "radial");
            writeNestedAlignment(json, "center", radial.center());
            json.writeNumberField("radius", radial.radius());
            json.writeFieldName("focal");
            if (radial.focal().isPresent()) {
                writeAlignmentObject(json, radial.focal().orElseThrow());
            } else {
                json.writeNull();
            }
            json.writeNumberField("focalRadius", radial.focalRadius());
        } else {
            PropertyValue.BoxDecorationValue.SweepGradient sweep =
                    (PropertyValue.BoxDecorationValue.SweepGradient) gradient;
            json.writeStringField("kind", "sweep");
            writeNestedAlignment(json, "center", sweep.center());
            json.writeNumberField("startAngle", sweep.startAngle());
            json.writeNumberField("endAngle", sweep.endAngle());
        }
        json.writeArrayFieldStart("stops");
        for (PropertyValue.BoxDecorationValue.GradientStop stop : gradient.stops()) {
            json.writeStartObject();
            json.writeStringField("id", stop.id().toString());
            json.writeFieldName("color");
            writeColorSource(json, stop.color());
            json.writeNumberField("stop", stop.stop());
            json.writeEndObject();
        }
        json.writeEndArray();
        json.writeStringField("tileMode", gradient.tileMode().wireName());
        if (gradient.rotationRadians().isPresent()) {
            json.writeNumberField(
                    "rotationRadians", gradient.rotationRadians().orElseThrow());
        } else {
            json.writeNullField("rotationRadians");
        }
        json.writeEndObject();
    }

    private static void writeNestedAlignment(
            JsonGenerator json,
            String name,
            PropertyValue.AlignmentGeometryValue alignment) throws IOException {
        json.writeFieldName(name);
        writeAlignmentObject(json, alignment);
    }

    private static void writeAlignmentObject(
            JsonGenerator json,
            PropertyValue.AlignmentGeometryValue alignment) throws IOException {
        json.writeStartObject();
        writeAlignmentFields(json, alignment);
        json.writeEndObject();
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

    private sealed interface ImageResolution
            permits ResolvedImage, UnavailableImage {
    }

    private record ResolvedImage(
            String resourceId,
            java.math.BigDecimal resolvedScale) implements ImageResolution {
    }

    private record UnavailableImage(
            CanvasImageResolutionIssue issue) implements ImageResolution {
    }

    /** Mutable only during one synchronous encoding call. */
    private static final class ProjectionContext {
        private final CanvasRenderRequest request;
        private final CanvasImageResourceBundle bundle;
        private final Set<CanvasImageAssetId> referencedAssets = new HashSet<>();
        private final Set<CanvasImageAssetId> referencedIssues = new HashSet<>();
        private final Set<String> referencedResources = new HashSet<>();

        ProjectionContext(CanvasRenderRequest request) {
            this.request = Objects.requireNonNull(request, "request");
            bundle = request.imageResources();
        }

        ImageResolution resolve(PropertyValue.ImageProviderValue provider) {
            CanvasImageAssetId assetId = new CanvasImageAssetId(
                    provider.packageName(), provider.assetName());
            var asset = bundle.find(assetId);
            if (asset.isPresent()) {
                CanvasImageAsset resolvedAsset = asset.orElseThrow();
                String resourceId;
                java.math.BigDecimal resolvedScale;
                if (provider.providerKind()
                        == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET) {
                    resourceId = resolvedAsset.exactResourceId();
                    resolvedScale = provider.exactScale().orElseThrow();
                } else {
                    CanvasImageVariant variant = resolvedAsset.selectVariant(
                            request.renderProfile().devicePixelRatio().value());
                    resourceId = variant.resourceId();
                    resolvedScale = variant.scale();
                }
                referencedAssets.add(assetId);
                referencedResources.add(resourceId);
                return new ResolvedImage(resourceId, resolvedScale);
            }

            CanvasImageResolutionIssue issue = bundle.findIssue(assetId)
                    .orElseGet(() -> new CanvasImageResolutionIssue(
                            assetId,
                            CanvasImageResolutionIssue.Code.UNDECLARED,
                            "Resolve Canvas image " + assetId.externalName()
                            + ": no declared, readable image bytes are available "
                            + "in the current bounded project snapshot."));
            referencedIssues.add(assetId);
            return new UnavailableImage(issue);
        }

        void requireExactResourceCoverage()
                throws CanvasModelPayloadException {
            Set<CanvasImageAssetId> suppliedAssets = bundle.assets().stream()
                    .map(CanvasImageAsset::assetId)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            Set<CanvasImageAssetId> suppliedIssues = bundle.issues().stream()
                    .map(CanvasImageResolutionIssue::assetId)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            Set<String> suppliedResources = bundle.resources().stream()
                    .map(dev.flutter.netbeans.designer.canvas.CanvasImageResource::resourceId)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (!suppliedAssets.equals(referencedAssets)) {
                throw new CanvasModelPayloadException(
                        "Canvas image snapshot must contain exactly the logical assets "
                        + "referenced by this Designer revision.");
            }
            if (!suppliedIssues.equals(referencedIssues)) {
                throw new CanvasModelPayloadException(
                        "Canvas image snapshot must contain exactly the unavailable "
                        + "asset reasons referenced by this Designer revision.");
            }
            if (!suppliedResources.equals(referencedResources)) {
                throw new CanvasModelPayloadException(
                        "Canvas image snapshot must contain exactly the encoded image "
                        + "resources selected for this presentation.");
            }
        }
    }

    private static IllegalStateException unsupported(PropertyValue value) {
        return new IllegalStateException(
                "The reviewed Canvas catalog cannot encode " + value.kind().wireName());
    }
}
