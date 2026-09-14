package dev.flutter.netbeans.designer.codec;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import dev.flutter.netbeans.designer.catalog.DartNumericLiterals;
import dev.flutter.netbeans.designer.model.CanvasPreferences;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ExtensionKey;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StateBinding;
import dev.flutter.netbeans.designer.model.StatePropertyBinding;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.json.JsonValue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Bounded canonical JSON writer for current-version Flutter Designer documents. */
final class FdJsonEncoder {
    private final FdCodecLimits limits;
    private final JsonFactory jsonFactory;

    FdJsonEncoder(FdCodecLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
        this.jsonFactory = JsonFactory.builder()
                .streamWriteConstraints(StreamWriteConstraints.builder()
                        .maxNestingDepth(limits.maxJsonNestingDepth())
                        .build())
                .build();
    }

    OriginalFdBytes encode(DesignerDocument document) throws FdEncodeException {
        Objects.requireNonNull(document, "document");
        BoundedOutputStream output = new BoundedOutputStream(limits.maxDocumentBytes());
        try {
            try (JsonGenerator generator = jsonFactory.createGenerator(output, JsonEncoding.UTF8)) {
                generator.setPrettyPrinter(new CanonicalPrettyPrinter());
                EncodingContext context = new EncodingContext(generator, output, limits);
                writeDocument(document, context);
            }
            output.setPointer("");
            output.write('\n');
        } catch (FdEncodeException failure) {
            throw failure;
        } catch (IOException failure) {
            OutputLimitIOException outputLimit = findOutputLimit(failure);
            if (outputLimit != null) {
                throw new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                        FdCodecDiagnosticCode.RESOURCE_LIMIT,
                        outputLimit.pointer(),
                        "Canonical output exceeds maxDocumentBytes="
                                + outputLimit.maximumBytes()), failure);
            }
            throw new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                    FdCodecDiagnosticCode.INVALID_VALUE,
                    "",
                    "Could not encode the Flutter Designer document as canonical JSON"), failure);
        }

        byte[] bytes = output.toByteArray();
        try {
            return OriginalFdBytes.copyOf(bytes, limits);
        } catch (FdInputLimitException impossibleAfterBoundedWrite) {
            throw new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                    FdCodecDiagnosticCode.RESOURCE_LIMIT,
                    "",
                    "Canonical output exceeds maxDocumentBytes="
                            + limits.maxDocumentBytes()), impossibleAfterBoundedWrite);
        }
    }

    private static void writeDocument(
            DesignerDocument document,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject("");
        if (document.schemaReference().isPresent()) {
            context.stringField("$schema", document.schemaReference().orElseThrow(), "/$schema");
        }
        context.stringField("format", document.format(), "/format");
        context.numberField("schemaVersion", Integer.toString(document.schemaVersion()), "/schemaVersion");
        context.stringField("documentId", document.documentId().toString(), "/documentId");
        writeSource(document.source(), context);
        if (document.canvas().isPresent()) {
            writeCanvas(document.canvas().orElseThrow(), context);
        }
        context.fieldName("root", "/root");

        Deque<WriteTask> tasks = new ArrayDeque<>();
        tasks.push(new EndObjectTask(""));
        if (!document.extensions().isEmpty()) {
            tasks.push(new ExtensionsFieldTask(document.extensions(), "/extensions"));
        }
        tasks.push(new WidgetTask(document.root(), 1, "/root"));
        while (!tasks.isEmpty()) {
            tasks.pop().write(context, tasks);
        }
    }

    private static void writeSource(
            DartSourceDescriptor source,
            EncodingContext context) throws IOException, FdEncodeException {
        context.fieldName("source", "/source");
        context.startObject("/source");
        context.stringField("dartFile", source.dartFile(), "/source/dartFile");
        context.stringField("className", source.className(), "/source/className");
        context.stringField("widgetKind", source.widgetKind().wireName(), "/source/widgetKind");
        if (source.generatorVersion().isPresent()) {
            context.stringField(
                    "generatorVersion",
                    source.generatorVersion().orElseThrow(),
                    "/source/generatorVersion");
        }
        writeManagedRegions(source.managedRegions(), context);
        context.endObject("/source");
    }

    private static void writeManagedRegions(
            ManagedRegions regions,
            EncodingContext context) throws IOException, FdEncodeException {
        context.fieldName("managedRegions", "/source/managedRegions");
        context.startObject("/source/managedRegions");
        writeManagedRegion("imports", regions.imports(), context);
        writeManagedRegion("build", regions.build(), context);
        context.endObject("/source/managedRegions");
    }

    private static void writeManagedRegion(
            String name,
            ManagedRegion region,
            EncodingContext context) throws IOException, FdEncodeException {
        String pointer = "/source/managedRegions/" + name;
        context.fieldName(name, pointer);
        context.startObject(pointer);
        context.stringField("sha256", region.sha256(), pointer + "/sha256");
        context.endObject(pointer);
    }

    private static void writeCanvas(
            CanvasPreferences canvas,
            EncodingContext context) throws IOException, FdEncodeException {
        context.fieldName("canvas", "/canvas");
        context.startObject("/canvas");
        if (canvas.preset().isPresent()) {
            context.stringField("preset", canvas.preset().orElseThrow(), "/canvas/preset");
        }
        writeOptionalDecimal("logicalWidth", canvas.logicalWidth(), context);
        writeOptionalDecimal("logicalHeight", canvas.logicalHeight(), context);
        writeOptionalDecimal("devicePixelRatio", canvas.devicePixelRatio(), context);
        if (canvas.orientation().isPresent()) {
            context.stringField(
                    "orientation",
                    canvas.orientation().orElseThrow().wireName(),
                    "/canvas/orientation");
        }
        if (canvas.themeMode().isPresent()) {
            context.stringField(
                    "themeMode",
                    canvas.themeMode().orElseThrow().wireName(),
                    "/canvas/themeMode");
        }
        writeOptionalDecimal("textScaleFactor", canvas.textScaleFactor(), context);
        if (canvas.locale().isPresent()) {
            context.stringField("locale", canvas.locale().orElseThrow(), "/canvas/locale");
        }
        context.endObject("/canvas");
    }

    private static void writeOptionalDecimal(
            String name,
            java.util.Optional<BigDecimal> value,
            EncodingContext context) throws IOException, FdEncodeException {
        if (value.isEmpty()) {
            return;
        }
        String pointer = "/canvas/" + name;
        String token = CanonicalJsonNumbers.decimal(value.orElseThrow(), context.limits(), pointer);
        context.numberField(name, token, pointer);
    }

    private interface WriteTask {
        void write(EncodingContext context, Deque<WriteTask> tasks)
                throws IOException, FdEncodeException;
    }

    private record EndObjectTask(String pointer) implements WriteTask {
        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks)
                throws IOException, FdEncodeException {
            context.endObject(pointer);
        }
    }

    private record EndArrayTask(String pointer) implements WriteTask {
        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks)
                throws IOException, FdEncodeException {
            context.endArray(pointer);
        }
    }

    private record WidgetTask(WidgetNode widget, int depth, String pointer) implements WriteTask {
        WidgetTask {
            Objects.requireNonNull(widget, "widget");
            Objects.requireNonNull(pointer, "pointer");
        }

        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks)
                throws IOException, FdEncodeException {
            context.enterWidget(depth, pointer);
            context.requireAtMost(
                    widget.properties().size(),
                    context.limits().maxPropertiesPerWidget(),
                    pointer + "/properties",
                    "properties per widget");
            context.requireAtMost(
                    widget.slots().size(),
                    context.limits().maxSlotsPerWidget(),
                    pointer + "/slots",
                    "slots per widget");

            context.startObject(pointer);
            context.stringField("id", widget.id().toString(), pointer + "/id");
            context.stringField("type", widget.type().value(), pointer + "/type");
            context.fieldName("properties", pointer + "/properties");
            context.startObject(pointer + "/properties");
            List<Map.Entry<PropertyName, PropertyValue>> properties =
                    new ArrayList<>(widget.properties().entrySet());
            properties.sort(Comparator.comparing(entry -> entry.getKey().value()));
            for (Map.Entry<PropertyName, PropertyValue> entry : properties) {
                String propertyPointer = pointer + "/properties/"
                        + pointerToken(entry.getKey().value());
                context.fieldName(entry.getKey().value(), propertyPointer);
                writePropertyValue(entry.getValue(), propertyPointer, context);
            }
            context.endObject(pointer + "/properties");

            if (widget.stateBinding().isPresent()) {
                writeStateBinding(widget.stateBinding().orElseThrow(), pointer + "/stateBinding", context);
            }
            if (!widget.propertyBindings().isEmpty()) {
                String base = pointer + "/propertyBindings";
                context.requireAtMost(widget.propertyBindings().size(), context.limits().maxPropertiesPerWidget(),
                        base, "State property bindings per widget");
                context.fieldName("propertyBindings", base);
                context.startObject(base);
                List<Map.Entry<PropertyName, StatePropertyBinding>> bindings = new ArrayList<>(widget.propertyBindings().entrySet());
                bindings.sort(Comparator.comparing(entry -> entry.getKey().value()));
                for (var entry : bindings) {
                    String path = base + "/" + pointerToken(entry.getKey().value());
                    context.fieldName(entry.getKey().value(), path);
                    writeStatePropertyBinding(entry.getValue(), path, context);
                }
                context.endObject(base);
            }

            context.fieldName("slots", pointer + "/slots");
            context.startObject(pointer + "/slots");
            List<Map.Entry<SlotName, WidgetSlot>> slots =
                    new ArrayList<>(widget.slots().entrySet());
            slots.sort(Comparator.comparing(entry -> entry.getKey().value()));

            tasks.push(new EndObjectTask(pointer));
            if (!widget.extensions().isEmpty()) {
                tasks.push(new ExtensionsFieldTask(
                        widget.extensions(), pointer + "/extensions"));
            }
            tasks.push(new EndObjectTask(pointer + "/slots"));
            if (!slots.isEmpty()) {
                tasks.push(new SlotEntriesTask(slots, 0, depth + 1, pointer + "/slots"));
            }
        }
    }

    private static void writeStateBinding(
            StateBinding binding, String pointer, EncodingContext context)
            throws IOException, FdEncodeException {
        context.fieldName("stateBinding", pointer);
        context.startObject(pointer);
        context.stringField("fieldName", binding.fieldName(), pointer + "/fieldName");
        context.stringField("handlerName", binding.handlerName(), pointer + "/handlerName");
        context.stringField("type", binding.type().wireName(), pointer + "/type");
        if (binding.action() != StateBinding.Action.CHANGE) {
            context.stringField("action", binding.action().wireName(), pointer + "/action");
        }
        if (binding.selectedValue().isPresent()) {
            context.fieldName("selectedValue", pointer + "/selectedValue");
            writePropertyValue(binding.selectedValue().orElseThrow(), pointer + "/selectedValue", context);
        }
        if (binding.referenceType().isPresent()) {
            context.fieldName("referenceType", pointer + "/referenceType");
            writePropertyValue(binding.referenceType().orElseThrow(), pointer + "/referenceType", context);
        }
        if (binding.previousOnChanged().isPresent()) {
            context.fieldName("previousOnChanged", pointer + "/previousOnChanged");
            writePropertyValue(binding.previousOnChanged().orElseThrow(), pointer + "/previousOnChanged", context);
        }
        context.endObject(pointer);
    }

    private static void writeStatePropertyBinding(StatePropertyBinding binding, String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject(pointer);
        context.stringField("fieldName", binding.fieldName(), pointer + "/fieldName");
        context.stringField("type", binding.type().wireName(), pointer + "/type");
        context.stringField("transform", binding.transform().wireName(), pointer + "/transform");
        if (binding.referenceType().isPresent()) {
            context.fieldName("referenceType", pointer + "/referenceType");
            writePropertyValue(binding.referenceType().orElseThrow(), pointer + "/referenceType", context);
        }
        if (binding.comparisonValue().isPresent()) {
            context.fieldName("comparisonValue", pointer + "/comparisonValue");
            writePropertyValue(binding.comparisonValue().orElseThrow(), pointer + "/comparisonValue", context);
        }
        context.endObject(pointer);
    }

    private record SlotEntriesTask(
            List<Map.Entry<SlotName, WidgetSlot>> entries,
            int index,
            int childDepth,
            String pointer) implements WriteTask {
        SlotEntriesTask {
            Objects.requireNonNull(entries, "entries");
            Objects.requireNonNull(pointer, "pointer");
        }

        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks) {
            Map.Entry<SlotName, WidgetSlot> entry = entries.get(index);
            if (index + 1 < entries.size()) {
                tasks.push(new SlotEntriesTask(entries, index + 1, childDepth, pointer));
            }
            String slotPointer = pointer + "/" + pointerToken(entry.getKey().value());
            tasks.push(new NamedSlotTask(
                    entry.getKey().value(), entry.getValue(), childDepth, slotPointer));
        }
    }

    private record NamedSlotTask(
            String name,
            WidgetSlot slot,
            int childDepth,
            String pointer) implements WriteTask {
        NamedSlotTask {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(slot, "slot");
            Objects.requireNonNull(pointer, "pointer");
        }

        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks)
                throws IOException, FdEncodeException {
            context.fieldName(name, pointer);
            context.startObject(pointer);
            if (slot instanceof WidgetSlot.SingleSlot single) {
                context.stringField("kind", "single", pointer + "/kind");
                context.fieldName("child", pointer + "/child");
                if (single.child().isEmpty()) {
                    context.nullValue(pointer + "/child");
                    context.endObject(pointer);
                } else {
                    tasks.push(new EndObjectTask(pointer));
                    tasks.push(new WidgetTask(
                            single.child().orElseThrow(),
                            childDepth,
                            pointer + "/child"));
                }
                return;
            }

            WidgetSlot.ListSlot list = (WidgetSlot.ListSlot) slot;
            context.requireAtMost(
                    list.children().size(),
                    context.limits().maxListChildren(),
                    pointer + "/children",
                    "children per list slot");
            context.requireAtMost(
                    list.children().size(),
                    context.limits().maxJsonArrayElements(),
                    pointer + "/children",
                    "JSON array elements");
            context.stringField("kind", "list", pointer + "/kind");
            context.fieldName("children", pointer + "/children");
            context.startArray(pointer + "/children");
            tasks.push(new EndObjectTask(pointer));
            tasks.push(new EndArrayTask(pointer + "/children"));
            if (!list.children().isEmpty()) {
                tasks.push(new WidgetChildrenTask(
                        list.children(), 0, childDepth, pointer + "/children"));
            }
        }
    }

    private record WidgetChildrenTask(
            List<WidgetNode> children,
            int index,
            int depth,
            String pointer) implements WriteTask {
        WidgetChildrenTask {
            Objects.requireNonNull(children, "children");
            Objects.requireNonNull(pointer, "pointer");
        }

        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks) {
            if (index + 1 < children.size()) {
                tasks.push(new WidgetChildrenTask(children, index + 1, depth, pointer));
            }
            tasks.push(new WidgetTask(
                    children.get(index), depth, pointer + "/" + index));
        }
    }

    private static void writePropertyValue(
            PropertyValue value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject(pointer);
        context.stringField("kind", value.kind().wireName(), pointer + "/kind");
        if (value instanceof PropertyValue.NullValue) {
            // The kind tag is the complete canonical representation.
        } else if (value instanceof PropertyValue.StringValue stringValue) {
            context.stringField("value", stringValue.value(), pointer + "/value");
        } else if (value instanceof PropertyValue.BooleanValue booleanValue) {
            context.booleanField("value", booleanValue.value(), pointer + "/value");
        } else if (value instanceof PropertyValue.IntegerValue integerValue) {
            String token = CanonicalJsonNumbers.integer(
                    integerValue.value(), context.limits(), pointer + "/value");
            context.numberField("value", token, pointer + "/value");
        } else if (value instanceof PropertyValue.DoubleValue doubleValue) {
            String token = CanonicalJsonNumbers.decimal(
                    doubleValue.value(), context.limits(), pointer + "/value");
            context.numberField("value", token, pointer + "/value");
        } else if (value instanceof PropertyValue.EnumValue enumValue) {
            context.stringField("type", enumValue.type(), pointer + "/type");
            context.stringField("value", enumValue.value(), pointer + "/value");
        } else if (value instanceof PropertyValue.PointerDeviceKindSetValue devices) {
            context.requireAtMost(devices.values().size(), context.limits().maxJsonArrayElements(),
                    pointer + "/values", "JSON array elements");
            context.fieldName("values", pointer + "/values");
            context.startArray(pointer + "/values");
            for (int index = 0; index < devices.values().size(); index++) {
                context.stringValue(devices.values().get(index).wireName(), pointer + "/values/" + index);
            }
            context.endArray(pointer + "/values");
        } else if (value instanceof PropertyValue.ColorValue colorValue) {
            context.stringField("argb", colorValue.wireArgb(), pointer + "/argb");
        } else if (value instanceof PropertyValue.EdgeInsetsValue edgeInsets) {
            writeDecimalField("left", edgeInsets.left(), pointer, context);
            writeDecimalField("top", edgeInsets.top(), pointer, context);
            writeDecimalField("right", edgeInsets.right(), pointer, context);
            writeDecimalField("bottom", edgeInsets.bottom(), pointer, context);
        } else if (value instanceof PropertyValue.EdgeInsetsDirectionalValue edgeInsets) {
            writeDecimalField("start", edgeInsets.start(), pointer, context);
            writeDecimalField("top", edgeInsets.top(), pointer, context);
            writeDecimalField("end", edgeInsets.end(), pointer, context);
            writeDecimalField("bottom", edgeInsets.bottom(), pointer, context);
        } else if (value instanceof PropertyValue.AssetValue assetValue) {
            context.stringField("path", assetValue.path(), pointer + "/path");
        } else if (value instanceof PropertyValue.CallbackValue callbackValue) {
            context.stringField("handler", callbackValue.handler(), pointer + "/handler");
        } else if (value instanceof PropertyValue.DartExpressionValue expressionValue) {
            context.stringField("code", expressionValue.code(), pointer + "/code");
        } else if (value instanceof PropertyValue.DartObjectReferenceValue reference) {
            writeOptionalStringField(
                    "libraryUri", reference.libraryUri(), pointer, context);
            context.stringField(
                    "rootSymbol", reference.rootSymbol(), pointer + "/rootSymbol");
            writeOptionalStringField("member", reference.member(), pointer, context);
            context.stringField(
                    "access", reference.access().wireName(), pointer + "/access");
            if (reference.constant().isPresent()) {
                context.booleanField(
                        "constant", reference.constant().orElseThrow(),
                        pointer + "/constant");
            }
        } else if (value instanceof PropertyValue.IconDataValue iconData) {
            context.fieldName("codePoint", pointer + "/codePoint");
            if (iconData.codePoint().isPresent()) {
                context.numberValue(
                        Integer.toString(iconData.codePoint().orElseThrow()),
                        pointer + "/codePoint");
            } else {
                context.nullValue(pointer + "/codePoint");
            }
            writeOptionalStringField(
                    "fontFamily", iconData.fontFamily(), pointer, context);
            writeOptionalStringField(
                    "fontPackage", iconData.fontPackage(), pointer, context);
            context.booleanField(
                    "matchTextDirection", iconData.matchTextDirection(),
                    pointer + "/matchTextDirection");
            context.fieldName("fontFamilyFallback", pointer + "/fontFamilyFallback");
            context.startArray(pointer + "/fontFamilyFallback");
            for (int index = 0; index < iconData.fontFamilyFallback().size(); index++) {
                context.stringValue(
                        iconData.fontFamilyFallback().get(index),
                        pointer + "/fontFamilyFallback/" + index);
            }
            context.endArray(pointer + "/fontFamilyFallback");
        } else if (value instanceof PropertyValue.ThemeTokenValue themeTokenValue) {
            context.stringField(
                    "token", themeTokenValue.token().wireId(), pointer + "/token");
        } else if (value instanceof PropertyValue.PaintValue paintValue) {
            context.fieldName("color", pointer + "/color");
            writeColorSource(paintValue.color(), pointer + "/color", context);
            context.stringField(
                    "blendMode", paintValue.blendMode().wireName(), pointer + "/blendMode");
            context.stringField("style", paintValue.style().wireName(), pointer + "/style");
            writeDartDoubleField("strokeWidth", paintValue.strokeWidth(), pointer, context);
            context.stringField(
                    "strokeCap", paintValue.strokeCap().wireName(), pointer + "/strokeCap");
            context.stringField(
                    "strokeJoin", paintValue.strokeJoin().wireName(), pointer + "/strokeJoin");
            writeDartDoubleField(
                    "strokeMiterLimit", paintValue.strokeMiterLimit(), pointer, context);
            context.booleanField("antiAlias", paintValue.antiAlias(), pointer + "/antiAlias");
            context.stringField(
                    "filterQuality",
                    paintValue.filterQuality().wireName(),
                    pointer + "/filterQuality");
            context.booleanField(
                    "invertColors", paintValue.invertColors(), pointer + "/invertColors");
            if (paintValue.maskFilter().isPresent()) {
                PropertyValue.PaintValue.BlurMask mask = paintValue.maskFilter().orElseThrow();
                context.fieldName("maskFilter", pointer + "/maskFilter");
                context.startObject(pointer + "/maskFilter");
                context.stringField(
                        "style", mask.style().wireName(), pointer + "/maskFilter/style");
                writeDartDoubleField(
                        "sigma", mask.sigma(), pointer + "/maskFilter", context);
                context.endObject(pointer + "/maskFilter");
            }
        } else if (value instanceof PropertyValue.ShadowListValue shadowList) {
            writeShadowList(shadowList, pointer, context);
        } else if (value instanceof PropertyValue.FontFeatureListValue featureList) {
            writeFontFeatureList(featureList, pointer, context);
        } else if (value instanceof PropertyValue.FontVariationListValue variationList) {
            writeFontVariationList(variationList, pointer, context);
        } else if (value instanceof PropertyValue.AlignmentGeometryValue alignment) {
            writeAlignmentFields(alignment, pointer, context);
        } else if (value instanceof PropertyValue.OffsetValue offset) {
            writeDartDoubleField("dx", offset.dx(), pointer, context);
            writeDartDoubleField("dy", offset.dy(), pointer, context);
        } else if (value instanceof PropertyValue.SizeValue size) {
            writeDartDoubleField("width", size.width(), pointer, context);
            writeDartDoubleField("height", size.height(), pointer, context);
        } else if (value instanceof PropertyValue.BoxConstraintsValue constraints) {
            writeOptionalDartDoubleField(
                    "minWidth", constraints.minWidth().finiteValue(), pointer, context);
            writeOptionalDartDoubleField(
                    "maxWidth", constraints.maxWidth().finiteValue(), pointer, context);
            writeOptionalDartDoubleField(
                    "minHeight", constraints.minHeight().finiteValue(), pointer, context);
            writeOptionalDartDoubleField(
                    "maxHeight", constraints.maxHeight().finiteValue(), pointer, context);
        } else if (value instanceof PropertyValue.Matrix4Value matrix) {
            context.fieldName("storage", pointer + "/storage");
            context.startArray(pointer + "/storage");
            for (int index = 0; index < matrix.storage().size(); index++) {
                String itemPointer = pointer + "/storage/" + index;
                BigDecimal number = matrix.storage().get(index);
                if (!DartNumericLiterals.isRepresentableDouble(number)) {
                    throw new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                            FdCodecDiagnosticCode.INVALID_VALUE, itemPointer,
                            "The matrix entry must be exactly representable as a finite Dart double."));
                }
                context.numberValue(
                        CanonicalJsonNumbers.decimal(number, context.limits(), itemPointer),
                        itemPointer);
            }
            context.endArray(pointer + "/storage");
        } else if (value instanceof PropertyValue.ImageProviderValue imageProvider) {
            writeImageProviderFields(imageProvider, pointer, context);
        } else if (value instanceof PropertyValue.BorderRadiusValue borderRadius) {
            context.fieldName("geometry", pointer + "/geometry");
            writeBorderRadius(borderRadius.geometry(), pointer + "/geometry", context);
        } else if (value instanceof PropertyValue.ShapeBorderClipperValue clipper) {
            context.stringField("shape", clipper.shape().wireName(), pointer + "/shape");
            context.fieldName("borderRadius", pointer + "/borderRadius");
            writeBorderRadius(clipper.borderRadius(), pointer + "/borderRadius", context);
            writeOptionalStringField("textDirection", clipper.textDirection().map(
                    PropertyValue.ShapeBorderClipperValue.TextDirection::wireName), pointer, context);
        } else if (value instanceof PropertyValue.BoxDecorationValue decoration) {
            writeBoxDecoration(decoration, pointer, context);
        } else {
            throw new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                    FdCodecDiagnosticCode.INVALID_VALUE,
                    pointer,
                    "Unsupported property value implementation: "
                            + value.getClass().getName()));
        }
        context.endObject(pointer);
    }

    private static void writeOptionalStringField(
            String field,
            java.util.Optional<String> value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        String valuePointer = pointer + '/' + field;
        context.fieldName(field, valuePointer);
        if (value.isPresent()) {
            context.stringValue(value.orElseThrow(), valuePointer);
        } else {
            context.nullValue(valuePointer);
        }
    }

    private static void writeColorSource(
            ColorSource source,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject(pointer);
        if (source instanceof ColorSource.Literal literal) {
            context.stringField("kind", "literal", pointer + "/kind");
            context.stringField("argb", literal.wireArgb(), pointer + "/argb");
        } else {
            ColorSource.Theme theme = (ColorSource.Theme) source;
            context.stringField("kind", "theme", pointer + "/kind");
            context.stringField("token", theme.token().wireId(), pointer + "/token");
        }
        context.endObject(pointer);
    }

    private static void writeAlignmentFields(
            PropertyValue.AlignmentGeometryValue value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.stringField("basis", value.basis().wireName(), pointer + "/basis");
        writeDartDoubleField("horizontal", value.horizontal(), pointer, context);
        writeDartDoubleField("vertical", value.vertical(), pointer, context);
    }

    private static void writeBoxDecoration(
            PropertyValue.BoxDecorationValue value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        writeOptionalColorSource("color", value.color(), pointer, context);
        context.fieldName("image", pointer + "/image");
        if (value.image().isPresent()) {
            writeDecorationImage(
                    value.image().orElseThrow(), pointer + "/image", context);
        } else {
            context.nullValue(pointer + "/image");
        }
        context.fieldName("border", pointer + "/border");
        if (value.border().isPresent()) {
            writeBoxBorder(value.border().orElseThrow(), pointer + "/border", context);
        } else {
            context.nullValue(pointer + "/border");
        }
        context.fieldName("borderRadius", pointer + "/borderRadius");
        if (value.borderRadius().isPresent()) {
            writeBorderRadius(value.borderRadius().orElseThrow(), pointer + "/borderRadius", context);
        } else {
            context.nullValue(pointer + "/borderRadius");
        }
        requireStructuredListSize(value.boxShadow().size(), pointer + "/boxShadow", context);
        context.fieldName("boxShadow", pointer + "/boxShadow");
        context.startArray(pointer + "/boxShadow");
        for (int index = 0; index < value.boxShadow().size(); index++) {
            PropertyValue.BoxDecorationValue.BoxShadow shadow = value.boxShadow().get(index);
            String item = pointer + "/boxShadow/" + index;
            context.startObject(item);
            context.stringField("id", shadow.id().toString(), item + "/id");
            context.fieldName("color", item + "/color");
            writeColorSource(shadow.color(), item + "/color", context);
            writeDartDoubleField("offsetX", shadow.offsetX(), item, context);
            writeDartDoubleField("offsetY", shadow.offsetY(), item, context);
            writeDartDoubleField("blurRadius", shadow.blurRadius(), item, context);
            writeDartDoubleField("spreadRadius", shadow.spreadRadius(), item, context);
            context.stringField("blurStyle", shadow.blurStyle().wireName(), item + "/blurStyle");
            context.endObject(item);
        }
        context.endArray(pointer + "/boxShadow");
        context.fieldName("gradient", pointer + "/gradient");
        if (value.gradient().isPresent()) {
            writeBoxGradient(value.gradient().orElseThrow(), pointer + "/gradient", context);
        } else {
            context.nullValue(pointer + "/gradient");
        }
        String blendPointer = pointer + "/backgroundBlendMode";
        context.fieldName("backgroundBlendMode", blendPointer);
        if (value.backgroundBlendMode().isPresent()) {
            context.stringValue(value.backgroundBlendMode().orElseThrow().wireName(), blendPointer);
        } else {
            context.nullValue(blendPointer);
        }
        context.stringField("shape", value.shape().wireName(), pointer + "/shape");
    }

    private static void writeImageProvider(
            PropertyValue.ImageProviderValue value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject(pointer);
        context.stringField("kind", value.kind().wireName(), pointer + "/kind");
        writeImageProviderFields(value, pointer, context);
        context.endObject(pointer);
    }

    private static void writeImageProviderFields(
            PropertyValue.ImageProviderValue value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.stringField(
                "providerKind", value.providerKind().wireName(),
                pointer + "/providerKind");
        context.stringField("assetName", value.assetName(), pointer + "/assetName");
        writeOptionalStringField(
                "packageName", value.packageName(), pointer, context);
        writeOptionalDartDoubleField(
                "exactScale", value.exactScale(), pointer, context);
        String resizePointer = pointer + "/resize";
        context.fieldName("resize", resizePointer);
        if (value.resize().isEmpty()) {
            context.nullValue(resizePointer);
            return;
        }
        PropertyValue.ImageProviderValue.ResizeImageConfig resize =
                value.resize().orElseThrow();
        context.startObject(resizePointer);
        writeOptionalIntegerField(
                "width", resize.width(), resizePointer, context);
        writeOptionalIntegerField(
                "height", resize.height(), resizePointer, context);
        context.stringField(
                "policy", resize.policy().wireName(), resizePointer + "/policy");
        context.booleanField(
                "allowUpscaling", resize.allowUpscaling(),
                resizePointer + "/allowUpscaling");
        context.endObject(resizePointer);
    }

    private static void writeDecorationImage(
            PropertyValue.DecorationImageValue value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject(pointer);
        context.fieldName("image", pointer + "/image");
        writeImageProvider(value.image(), pointer + "/image", context);
        String onErrorPointer = pointer + "/onError";
        context.fieldName("onError", onErrorPointer);
        if (value.onError().isPresent()) {
            context.startObject(onErrorPointer);
            context.stringField("kind", "callback", onErrorPointer + "/kind");
            context.stringField(
                    "handler", value.onError().orElseThrow().handler(),
                    onErrorPointer + "/handler");
            context.endObject(onErrorPointer);
        } else {
            context.nullValue(onErrorPointer);
        }
        String colorFilterPointer = pointer + "/colorFilter";
        context.fieldName("colorFilter", colorFilterPointer);
        if (value.colorFilter().isPresent()) {
            writeDecorationColorFilter(
                    value.colorFilter().orElseThrow(), colorFilterPointer, context);
        } else {
            context.nullValue(colorFilterPointer);
        }
        String fitPointer = pointer + "/fit";
        context.fieldName("fit", fitPointer);
        if (value.fit().isPresent()) {
            context.stringValue(value.fit().orElseThrow().wireName(), fitPointer);
        } else {
            context.nullValue(fitPointer);
        }
        String alignmentPointer = pointer + "/alignment";
        context.fieldName("alignment", alignmentPointer);
        context.startObject(alignmentPointer);
        writeAlignmentFields(value.alignment(), alignmentPointer, context);
        context.endObject(alignmentPointer);
        String centerSlicePointer = pointer + "/centerSlice";
        context.fieldName("centerSlice", centerSlicePointer);
        if (value.centerSlice().isPresent()) {
            PropertyValue.DecorationImageValue.Rect rect =
                    value.centerSlice().orElseThrow();
            context.startObject(centerSlicePointer);
            writeDartDoubleField("left", rect.left(), centerSlicePointer, context);
            writeDartDoubleField("top", rect.top(), centerSlicePointer, context);
            writeDartDoubleField("right", rect.right(), centerSlicePointer, context);
            writeDartDoubleField("bottom", rect.bottom(), centerSlicePointer, context);
            context.endObject(centerSlicePointer);
        } else {
            context.nullValue(centerSlicePointer);
        }
        context.stringField("repeat", value.repeat().wireName(), pointer + "/repeat");
        context.booleanField(
                "matchTextDirection", value.matchTextDirection(),
                pointer + "/matchTextDirection");
        writeDartDoubleField("scale", value.scale(), pointer, context);
        writeDartDoubleField("opacity", value.opacity(), pointer, context);
        context.stringField(
                "filterQuality", value.filterQuality().wireName(),
                pointer + "/filterQuality");
        context.booleanField(
                "invertColors", value.invertColors(), pointer + "/invertColors");
        context.booleanField(
                "isAntiAlias", value.isAntiAlias(), pointer + "/isAntiAlias");
        context.endObject(pointer);
    }

    private static void writeDecorationColorFilter(
            PropertyValue.DecorationImageValue.ColorFilter value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject(pointer);
        context.stringField("kind", value.wireKind(), pointer + "/kind");
        if (value instanceof PropertyValue.DecorationImageValue.Mode mode) {
            context.fieldName("color", pointer + "/color");
            writeColorSource(mode.color(), pointer + "/color", context);
            context.stringField(
                    "blendMode", mode.blendMode().wireName(),
                    pointer + "/blendMode");
        } else if (value instanceof PropertyValue.DecorationImageValue.Matrix matrix) {
            context.fieldName("values", pointer + "/values");
            context.startArray(pointer + "/values");
            for (int index = 0; index < matrix.values().size(); index++) {
                writeDartDoubleValue(
                        matrix.values().get(index),
                        pointer + "/values/" + index,
                        context);
            }
            context.endArray(pointer + "/values");
        } else if (value instanceof PropertyValue.DecorationImageValue.Saturation saturation) {
            writeDartDoubleField("value", saturation.value(), pointer, context);
        }
        context.endObject(pointer);
    }

    private static void writeOptionalIntegerField(
            String field,
            Optional<Integer> value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        String valuePointer = pointer + '/' + field;
        context.fieldName(field, valuePointer);
        if (value.isPresent()) {
            context.numberValue(Integer.toString(value.orElseThrow()), valuePointer);
        } else {
            context.nullValue(valuePointer);
        }
    }

    private static void writeOptionalColorSource(
            String name,
            java.util.Optional<ColorSource> value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        String fieldPointer = pointer + '/' + name;
        context.fieldName(name, fieldPointer);
        if (value.isPresent()) {
            writeColorSource(value.orElseThrow(), fieldPointer, context);
        } else {
            context.nullValue(fieldPointer);
        }
    }

    private static void writeBoxBorder(
            PropertyValue.BoxDecorationValue.BoxBorder value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject(pointer);
        if (value instanceof PropertyValue.BoxDecorationValue.PhysicalBorder border) {
            context.stringField("kind", "physical", pointer + "/kind");
            writeBorderSide("top", border.top(), pointer, context);
            writeBorderSide("right", border.right(), pointer, context);
            writeBorderSide("bottom", border.bottom(), pointer, context);
            writeBorderSide("left", border.left(), pointer, context);
        } else {
            PropertyValue.BoxDecorationValue.DirectionalBorder border =
                    (PropertyValue.BoxDecorationValue.DirectionalBorder) value;
            context.stringField("kind", "directional", pointer + "/kind");
            writeBorderSide("top", border.top(), pointer, context);
            writeBorderSide("start", border.start(), pointer, context);
            writeBorderSide("end", border.end(), pointer, context);
            writeBorderSide("bottom", border.bottom(), pointer, context);
        }
        context.endObject(pointer);
    }

    private static void writeBorderSide(
            String name,
            PropertyValue.BoxDecorationValue.BorderSide side,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        String sidePointer = pointer + '/' + name;
        context.fieldName(name, sidePointer);
        context.startObject(sidePointer);
        context.fieldName("color", sidePointer + "/color");
        writeColorSource(side.color(), sidePointer + "/color", context);
        writeDartDoubleField("width", side.width(), sidePointer, context);
        context.stringField("style", side.style().wireName(), sidePointer + "/style");
        writeDartDoubleField("strokeAlign", side.strokeAlign(), sidePointer, context);
        context.endObject(sidePointer);
    }

    private static void writeBorderRadius(
            PropertyValue.BoxDecorationValue.BorderRadiusGeometry value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject(pointer);
        if (value instanceof PropertyValue.BoxDecorationValue.PhysicalBorderRadius radius) {
            context.stringField("kind", "physical", pointer + "/kind");
            writeRadius("topLeft", radius.topLeft(), pointer, context);
            writeRadius("topRight", radius.topRight(), pointer, context);
            writeRadius("bottomRight", radius.bottomRight(), pointer, context);
            writeRadius("bottomLeft", radius.bottomLeft(), pointer, context);
        } else {
            PropertyValue.BoxDecorationValue.DirectionalBorderRadius radius =
                    (PropertyValue.BoxDecorationValue.DirectionalBorderRadius) value;
            context.stringField("kind", "directional", pointer + "/kind");
            writeRadius("topStart", radius.topStart(), pointer, context);
            writeRadius("topEnd", radius.topEnd(), pointer, context);
            writeRadius("bottomEnd", radius.bottomEnd(), pointer, context);
            writeRadius("bottomStart", radius.bottomStart(), pointer, context);
        }
        context.endObject(pointer);
    }

    private static void writeRadius(
            String name,
            PropertyValue.BoxDecorationValue.Radius radius,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        String radiusPointer = pointer + '/' + name;
        context.fieldName(name, radiusPointer);
        context.startObject(radiusPointer);
        writeDartDoubleField("x", radius.x(), radiusPointer, context);
        writeDartDoubleField("y", radius.y(), radiusPointer, context);
        context.endObject(radiusPointer);
    }

    private static void writeBoxGradient(
            PropertyValue.BoxDecorationValue.BoxGradient value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject(pointer);
        if (value instanceof PropertyValue.BoxDecorationValue.LinearGradient gradient) {
            context.stringField("kind", "linear", pointer + "/kind");
            writeNestedAlignment("begin", gradient.begin(), pointer, context);
            writeNestedAlignment("end", gradient.end(), pointer, context);
        } else if (value instanceof PropertyValue.BoxDecorationValue.RadialGradient gradient) {
            context.stringField("kind", "radial", pointer + "/kind");
            writeNestedAlignment("center", gradient.center(), pointer, context);
            writeDartDoubleField("radius", gradient.radius(), pointer, context);
            String focalPointer = pointer + "/focal";
            context.fieldName("focal", focalPointer);
            if (gradient.focal().isPresent()) {
                writeAlignmentObject(gradient.focal().orElseThrow(), focalPointer, context);
            } else {
                context.nullValue(focalPointer);
            }
            writeDartDoubleField("focalRadius", gradient.focalRadius(), pointer, context);
        } else {
            PropertyValue.BoxDecorationValue.SweepGradient gradient =
                    (PropertyValue.BoxDecorationValue.SweepGradient) value;
            context.stringField("kind", "sweep", pointer + "/kind");
            writeNestedAlignment("center", gradient.center(), pointer, context);
            writeDartDoubleField("startAngle", gradient.startAngle(), pointer, context);
            writeDartDoubleField("endAngle", gradient.endAngle(), pointer, context);
        }
        writeGradientStops(value.stops(), pointer, context);
        context.stringField("tileMode", value.tileMode().wireName(), pointer + "/tileMode");
        writeOptionalDartDoubleField(
                "rotationRadians", value.rotationRadians(), pointer, context);
        context.endObject(pointer);
    }

    private static void writeNestedAlignment(
            String name,
            PropertyValue.AlignmentGeometryValue alignment,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        String alignmentPointer = pointer + '/' + name;
        context.fieldName(name, alignmentPointer);
        writeAlignmentObject(alignment, alignmentPointer, context);
    }

    private static void writeAlignmentObject(
            PropertyValue.AlignmentGeometryValue alignment,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        context.startObject(pointer);
        writeAlignmentFields(alignment, pointer, context);
        context.endObject(pointer);
    }

    private static void writeGradientStops(
            List<PropertyValue.BoxDecorationValue.GradientStop> stops,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        requireStructuredListSize(stops.size(), pointer + "/stops", context);
        context.fieldName("stops", pointer + "/stops");
        context.startArray(pointer + "/stops");
        for (int index = 0; index < stops.size(); index++) {
            PropertyValue.BoxDecorationValue.GradientStop stop = stops.get(index);
            String item = pointer + "/stops/" + index;
            context.startObject(item);
            context.stringField("id", stop.id().toString(), item + "/id");
            context.fieldName("color", item + "/color");
            writeColorSource(stop.color(), item + "/color", context);
            writeDartDoubleField("stop", stop.stop(), item, context);
            context.endObject(item);
        }
        context.endArray(pointer + "/stops");
    }

    private static void writeOptionalDartDoubleField(
            String name,
            java.util.Optional<BigDecimal> value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        String fieldPointer = pointer + '/' + name;
        context.fieldName(name, fieldPointer);
        if (value.isPresent()) {
            BigDecimal number = value.orElseThrow();
            if (!DartNumericLiterals.isRepresentableDouble(number)) {
                throw new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                        FdCodecDiagnosticCode.INVALID_VALUE, fieldPointer,
                        "The field must be exactly representable as a finite Dart double."));
            }
            context.numberValue(
                    CanonicalJsonNumbers.decimal(number, context.limits(), fieldPointer),
                    fieldPointer);
        } else {
            context.nullValue(fieldPointer);
        }
    }

    private static void writeShadowList(
            PropertyValue.ShadowListValue value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        requireStructuredListSize(value.items().size(), pointer, context);
        context.fieldName("items", pointer + "/items");
        context.startArray(pointer + "/items");
        for (int index = 0; index < value.items().size(); index++) {
            PropertyValue.ShadowListValue.Shadow shadow = value.items().get(index);
            String itemPointer = pointer + "/items/" + index;
            context.startObject(itemPointer);
            context.stringField("id", shadow.id().toString(), itemPointer + "/id");
            context.fieldName("color", itemPointer + "/color");
            writeColorSource(shadow.color(), itemPointer + "/color", context);
            writeDartDoubleField("offsetX", shadow.offsetX(), itemPointer, context);
            writeDartDoubleField("offsetY", shadow.offsetY(), itemPointer, context);
            writeDartDoubleField("blurRadius", shadow.blurRadius(), itemPointer, context);
            context.endObject(itemPointer);
        }
        context.endArray(pointer + "/items");
    }

    private static void writeFontFeatureList(
            PropertyValue.FontFeatureListValue value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        requireStructuredListSize(value.items().size(), pointer, context);
        context.fieldName("items", pointer + "/items");
        context.startArray(pointer + "/items");
        for (int index = 0; index < value.items().size(); index++) {
            PropertyValue.FontFeatureListValue.FontFeature feature = value.items().get(index);
            String itemPointer = pointer + "/items/" + index;
            context.startObject(itemPointer);
            context.stringField("id", feature.id().toString(), itemPointer + "/id");
            context.stringField("tag", feature.tag(), itemPointer + "/tag");
            context.numberField(
                    "value", Integer.toString(feature.value()), itemPointer + "/value");
            context.endObject(itemPointer);
        }
        context.endArray(pointer + "/items");
    }

    private static void writeFontVariationList(
            PropertyValue.FontVariationListValue value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        requireStructuredListSize(value.items().size(), pointer, context);
        context.fieldName("items", pointer + "/items");
        context.startArray(pointer + "/items");
        for (int index = 0; index < value.items().size(); index++) {
            PropertyValue.FontVariationListValue.FontVariation variation = value.items().get(index);
            String itemPointer = pointer + "/items/" + index;
            context.startObject(itemPointer);
            context.stringField("id", variation.id().toString(), itemPointer + "/id");
            context.stringField("axis", variation.axis(), itemPointer + "/axis");
            writeDartDoubleField("value", variation.value(), itemPointer, context);
            context.endObject(itemPointer);
        }
        context.endArray(pointer + "/items");
    }

    private static void requireStructuredListSize(
            int size,
            String pointer,
            EncodingContext context) throws FdEncodeException {
        context.requireAtMost(
                size,
                PropertyValue.ShadowListValue.MAX_ITEMS,
                pointer + "/items",
                "structured property items");
        context.requireAtMost(
                size,
                context.limits().maxJsonArrayElements(),
                pointer + "/items",
                "JSON array elements");
    }

    private static void writeDecimalField(
            String name,
            BigDecimal value,
            String parentPointer,
            EncodingContext context) throws IOException, FdEncodeException {
        String pointer = parentPointer + "/" + name;
        String token = CanonicalJsonNumbers.decimal(value, context.limits(), pointer);
        context.numberField(name, token, pointer);
    }

    private static void writeDartDoubleField(
            String name,
            BigDecimal value,
            String parentPointer,
            EncodingContext context) throws IOException, FdEncodeException {
        String pointer = parentPointer + "/" + name;
        if (!DartNumericLiterals.isRepresentableDouble(value)) {
            throw new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                    FdCodecDiagnosticCode.INVALID_VALUE,
                    pointer,
                    "The field must be exactly representable as a finite Dart double."));
        }
        writeDecimalField(name, value, parentPointer, context);
    }

    private static void writeDartDoubleValue(
            BigDecimal value,
            String pointer,
            EncodingContext context) throws IOException, FdEncodeException {
        if (!DartNumericLiterals.isRepresentableDouble(value)) {
            throw new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                    FdCodecDiagnosticCode.INVALID_VALUE,
                    pointer,
                    "The value must be exactly representable as a finite Dart double."));
        }
        context.numberValue(
                CanonicalJsonNumbers.decimal(value, context.limits(), pointer),
                pointer);
    }

    private record ExtensionsFieldTask(Extensions extensions, String pointer) implements WriteTask {
        ExtensionsFieldTask {
            Objects.requireNonNull(extensions, "extensions");
            Objects.requireNonNull(pointer, "pointer");
        }

        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks)
                throws IOException, FdEncodeException {
            context.requireAtMost(
                    extensions.values().size(),
                    context.limits().maxExtensionKeysPerBag(),
                    pointer,
                    "extension keys per bag");
            context.fieldName("extensions", pointer);
            context.startObject(pointer);
            List<Map.Entry<ExtensionKey, JsonValue>> entries =
                    new ArrayList<>(extensions.values().entrySet());
            entries.sort(Comparator.comparing(entry -> entry.getKey().value()));
            tasks.push(new EndObjectTask(pointer));
            if (!entries.isEmpty()) {
                tasks.push(new ExtensionEntriesTask(entries, 0, pointer));
            }
        }
    }

    private record ExtensionEntriesTask(
            List<Map.Entry<ExtensionKey, JsonValue>> entries,
            int index,
            String pointer) implements WriteTask {
        ExtensionEntriesTask {
            Objects.requireNonNull(entries, "entries");
            Objects.requireNonNull(pointer, "pointer");
        }

        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks)
                throws IOException, FdEncodeException {
            Map.Entry<ExtensionKey, JsonValue> entry = entries.get(index);
            if (index + 1 < entries.size()) {
                tasks.push(new ExtensionEntriesTask(entries, index + 1, pointer));
            }
            String valuePointer = pointer + "/" + pointerToken(entry.getKey().value());
            context.fieldName(entry.getKey().value(), valuePointer);
            tasks.push(new JsonValueTask(entry.getValue(), 1, valuePointer));
        }
    }

    private record JsonValueTask(JsonValue value, int depth, String pointer) implements WriteTask {
        JsonValueTask {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(pointer, "pointer");
        }

        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks)
                throws IOException, FdEncodeException {
            context.enterExtensionValue(depth, pointer);
            if (value instanceof JsonValue.NullValue) {
                context.nullValue(pointer);
            } else if (value instanceof JsonValue.BooleanValue booleanValue) {
                context.booleanValue(booleanValue.value(), pointer);
            } else if (value instanceof JsonValue.NumberValue numberValue) {
                context.numberValue(
                        CanonicalJsonNumbers.decimal(
                                numberValue.value(), context.limits(), pointer),
                        pointer);
            } else if (value instanceof JsonValue.StringValue stringValue) {
                context.stringValue(stringValue.value(), pointer);
            } else if (value instanceof JsonValue.ArrayValue arrayValue) {
                context.requireAtMost(
                        arrayValue.values().size(),
                        context.limits().maxJsonArrayElements(),
                        pointer,
                        "JSON array elements");
                context.startArray(pointer);
                tasks.push(new EndArrayTask(pointer));
                if (!arrayValue.values().isEmpty()) {
                    tasks.push(new JsonArrayElementsTask(
                            arrayValue.values(), 0, depth + 1, pointer));
                }
            } else if (value instanceof JsonValue.ObjectValue objectValue) {
                context.requireAtMost(
                        objectValue.values().size(),
                        context.limits().maxJsonObjectFields(),
                        pointer,
                        "JSON object fields");
                context.startObject(pointer);
                List<Map.Entry<String, JsonValue>> entries =
                        new ArrayList<>(objectValue.values().entrySet());
                entries.sort(Map.Entry.comparingByKey());
                tasks.push(new EndObjectTask(pointer));
                if (!entries.isEmpty()) {
                    tasks.push(new JsonObjectEntriesTask(entries, 0, depth + 1, pointer));
                }
            } else {
                throw new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                        FdCodecDiagnosticCode.INVALID_VALUE,
                        pointer,
                        "Unsupported extension JSON value implementation: "
                                + value.getClass().getName()));
            }
        }
    }

    private record JsonArrayElementsTask(
            List<JsonValue> values,
            int index,
            int depth,
            String pointer) implements WriteTask {
        JsonArrayElementsTask {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(pointer, "pointer");
        }

        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks) {
            if (index + 1 < values.size()) {
                tasks.push(new JsonArrayElementsTask(values, index + 1, depth, pointer));
            }
            tasks.push(new JsonValueTask(values.get(index), depth, pointer + "/" + index));
        }
    }

    private record JsonObjectEntriesTask(
            List<Map.Entry<String, JsonValue>> entries,
            int index,
            int depth,
            String pointer) implements WriteTask {
        JsonObjectEntriesTask {
            Objects.requireNonNull(entries, "entries");
            Objects.requireNonNull(pointer, "pointer");
        }

        @Override
        public void write(EncodingContext context, Deque<WriteTask> tasks)
                throws IOException, FdEncodeException {
            Map.Entry<String, JsonValue> entry = entries.get(index);
            if (index + 1 < entries.size()) {
                tasks.push(new JsonObjectEntriesTask(entries, index + 1, depth, pointer));
            }
            String valuePointer = pointer + "/" + pointerToken(entry.getKey());
            context.fieldName(entry.getKey(), valuePointer);
            tasks.push(new JsonValueTask(entry.getValue(), depth, valuePointer));
        }
    }

    private static final class EncodingContext {
        private final JsonGenerator generator;
        private final BoundedOutputStream output;
        private final FdCodecLimits limits;
        private final Deque<ContainerState> containers = new ArrayDeque<>();
        private long tokenCount;
        private long widgetCount;
        private long extensionValueCount;

        EncodingContext(
                JsonGenerator generator,
                BoundedOutputStream output,
                FdCodecLimits limits) {
            this.generator = Objects.requireNonNull(generator, "generator");
            this.output = Objects.requireNonNull(output, "output");
            this.limits = Objects.requireNonNull(limits, "limits");
        }

        FdCodecLimits limits() {
            return limits;
        }

        void startObject(String pointer) throws IOException, FdEncodeException {
            beforeValue(pointer);
            requireNesting(pointer);
            countToken(pointer);
            output.setPointer(pointer);
            generator.writeStartObject();
            containers.push(new ContainerState(ContainerKind.OBJECT));
        }

        void endObject(String pointer) throws IOException, FdEncodeException {
            requireContainer(ContainerKind.OBJECT);
            countToken(pointer);
            output.setPointer(pointer);
            generator.writeEndObject();
            containers.pop();
        }

        void startArray(String pointer) throws IOException, FdEncodeException {
            beforeValue(pointer);
            requireNesting(pointer);
            countToken(pointer);
            output.setPointer(pointer);
            generator.writeStartArray();
            containers.push(new ContainerState(ContainerKind.ARRAY));
        }

        void endArray(String pointer) throws IOException, FdEncodeException {
            requireContainer(ContainerKind.ARRAY);
            countToken(pointer);
            output.setPointer(pointer);
            generator.writeEndArray();
            containers.pop();
        }

        void fieldName(String name, String pointer) throws IOException, FdEncodeException {
            validateFieldName(name, pointer);
            requireContainer(ContainerKind.OBJECT);
            ContainerState object = containers.peek();
            if (object.entryCount >= limits.maxJsonObjectFields()) {
                throw resourceLimit(
                        pointer,
                        "JSON object fields",
                        (long) object.entryCount + 1L,
                        limits.maxJsonObjectFields());
            }
            object.entryCount++;
            countToken(pointer);
            output.setPointer(pointer);
            generator.writeFieldName(name);
        }

        void stringField(String name, String value, String pointer)
                throws IOException, FdEncodeException {
            validateString(value, pointer);
            fieldName(name, pointer);
            stringValueAlreadyValidated(value, pointer);
        }

        void booleanField(String name, boolean value, String pointer)
                throws IOException, FdEncodeException {
            fieldName(name, pointer);
            booleanValue(value, pointer);
        }

        void numberField(String name, String token, String pointer)
                throws IOException, FdEncodeException {
            fieldName(name, pointer);
            numberValue(token, pointer);
        }

        void stringValue(String value, String pointer) throws IOException, FdEncodeException {
            validateString(value, pointer);
            stringValueAlreadyValidated(value, pointer);
        }

        private void stringValueAlreadyValidated(String value, String pointer)
                throws IOException, FdEncodeException {
            beforeValue(pointer);
            countToken(pointer);
            output.setPointer(pointer);
            generator.writeString(value);
        }

        void booleanValue(boolean value, String pointer) throws IOException, FdEncodeException {
            beforeValue(pointer);
            countToken(pointer);
            output.setPointer(pointer);
            generator.writeBoolean(value);
        }

        void numberValue(String token, String pointer) throws IOException, FdEncodeException {
            Objects.requireNonNull(token, "token");
            if (token.length() > limits.maxNumberCharacters()) {
                throw resourceLimit(
                        pointer,
                        "number characters",
                        token.length(),
                        limits.maxNumberCharacters());
            }
            beforeValue(pointer);
            countToken(pointer);
            output.setPointer(pointer);
            generator.writeNumber(token);
        }

        void nullValue(String pointer) throws IOException, FdEncodeException {
            beforeValue(pointer);
            countToken(pointer);
            output.setPointer(pointer);
            generator.writeNull();
        }

        void enterWidget(int depth, String pointer) throws FdEncodeException {
            if (depth > limits.maxWidgetDepth()) {
                throw resourceLimit(
                        pointer,
                        "widget depth",
                        depth,
                        limits.maxWidgetDepth());
            }
            if (widgetCount >= limits.maxWidgetNodes()) {
                throw resourceLimit(
                        pointer,
                        "widget nodes",
                        widgetCount + 1L,
                        limits.maxWidgetNodes());
            }
            widgetCount++;
        }

        void enterExtensionValue(int depth, String pointer) throws FdEncodeException {
            if (depth > limits.maxExtensionNestingDepth()) {
                throw resourceLimit(
                        pointer,
                        "extension nesting depth",
                        depth,
                        limits.maxExtensionNestingDepth());
            }
            if (extensionValueCount >= limits.maxExtensionValues()) {
                throw resourceLimit(
                        pointer,
                        "extension values",
                        extensionValueCount + 1L,
                        limits.maxExtensionValues());
            }
            extensionValueCount++;
        }

        void requireAtMost(long actual, long maximum, String pointer, String label)
                throws FdEncodeException {
            if (actual > maximum) {
                throw resourceLimit(pointer, label, actual, maximum);
            }
        }

        private void beforeValue(String pointer) throws FdEncodeException {
            if (!containers.isEmpty() && containers.peek().kind == ContainerKind.ARRAY) {
                ContainerState array = containers.peek();
                if (array.entryCount >= limits.maxJsonArrayElements()) {
                    throw resourceLimit(
                            pointer,
                            "JSON array elements",
                            (long) array.entryCount + 1L,
                            limits.maxJsonArrayElements());
                }
                array.entryCount++;
            }
        }

        private void requireNesting(String pointer) throws FdEncodeException {
            long nextDepth = (long) containers.size() + 1L;
            if (nextDepth > limits.maxJsonNestingDepth()) {
                throw resourceLimit(
                        pointer,
                        "JSON nesting depth",
                        nextDepth,
                        limits.maxJsonNestingDepth());
            }
        }

        private void countToken(String pointer) throws FdEncodeException {
            if (tokenCount >= limits.maxJsonTokens()) {
                throw resourceLimit(
                        pointer,
                        "JSON tokens",
                        tokenCount + 1L,
                        limits.maxJsonTokens());
            }
            tokenCount++;
        }

        private void validateFieldName(String name, String pointer) throws FdEncodeException {
            Objects.requireNonNull(name, "name");
            if (name.length() > limits.maxFieldNameUtf16Units()) {
                throw resourceLimit(
                        pointer,
                        "field-name UTF-16 units",
                        name.length(),
                        limits.maxFieldNameUtf16Units());
            }
            requirePairedSurrogates(name, pointer, "field name");
        }

        private void validateString(String value, String pointer) throws FdEncodeException {
            Objects.requireNonNull(value, "value");
            if (value.length() > limits.maxStringUtf16Units()) {
                throw resourceLimit(
                        pointer,
                        "string UTF-16 units",
                        value.length(),
                        limits.maxStringUtf16Units());
            }
            int codePoints = 0;
            for (int index = 0; index < value.length(); index++) {
                char current = value.charAt(index);
                if (Character.isHighSurrogate(current)) {
                    if (index + 1 >= value.length()
                            || !Character.isLowSurrogate(value.charAt(index + 1))) {
                        throw invalidSurrogate(pointer, "string value");
                    }
                    index++;
                } else if (Character.isLowSurrogate(current)) {
                    throw invalidSurrogate(pointer, "string value");
                }
                codePoints++;
                if (codePoints > limits.maxStringCodePoints()) {
                    throw resourceLimit(
                            pointer,
                            "string code points",
                            codePoints,
                            limits.maxStringCodePoints());
                }
            }
        }

        private void requirePairedSurrogates(String value, String pointer, String label)
                throws FdEncodeException {
            for (int index = 0; index < value.length(); index++) {
                char current = value.charAt(index);
                if (Character.isHighSurrogate(current)) {
                    if (index + 1 >= value.length()
                            || !Character.isLowSurrogate(value.charAt(index + 1))) {
                        throw invalidSurrogate(pointer, label);
                    }
                    index++;
                } else if (Character.isLowSurrogate(current)) {
                    throw invalidSurrogate(pointer, label);
                }
            }
        }

        private static FdEncodeException invalidSurrogate(String pointer, String label) {
            return new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                    FdCodecDiagnosticCode.INVALID_VALUE,
                    pointer,
                    "Unpaired UTF-16 surrogate in " + label));
        }

        private static FdEncodeException resourceLimit(
                String pointer,
                String label,
                long actual,
                long maximum) {
            return new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                    FdCodecDiagnosticCode.RESOURCE_LIMIT,
                    pointer,
                    label + " count " + actual + " exceeds configured limit " + maximum));
        }

        private void requireContainer(ContainerKind expected) {
            if (containers.isEmpty() || containers.peek().kind != expected) {
                throw new IllegalStateException("Canonical writer container state is inconsistent");
            }
        }
    }

    private enum ContainerKind {
        OBJECT,
        ARRAY
    }

    private static final class ContainerState {
        private final ContainerKind kind;
        private int entryCount;

        ContainerState(ContainerKind kind) {
            this.kind = Objects.requireNonNull(kind, "kind");
        }
    }

    private static final class CanonicalPrettyPrinter extends DefaultPrettyPrinter {
        CanonicalPrettyPrinter() {
            DefaultIndenter lfTwoSpaces = new DefaultIndenter("  ", "\n");
            indentObjectsWith(lfTwoSpaces);
            indentArraysWith(lfTwoSpaces);
        }

        private CanonicalPrettyPrinter(CanonicalPrettyPrinter base) {
            super(base);
        }

        @Override
        public DefaultPrettyPrinter createInstance() {
            return new CanonicalPrettyPrinter(this);
        }

        @Override
        public void writeObjectFieldValueSeparator(JsonGenerator generator) throws IOException {
            generator.writeRaw(": ");
        }

        @Override
        public void writeEndObject(JsonGenerator generator, int entryCount) throws IOException {
            if (!_objectIndenter.isInline()) {
                _nesting--;
            }
            if (entryCount > 0) {
                _objectIndenter.writeIndentation(generator, _nesting);
            }
            generator.writeRaw('}');
        }

        @Override
        public void writeEndArray(JsonGenerator generator, int valueCount) throws IOException {
            if (!_arrayIndenter.isInline()) {
                _nesting--;
            }
            if (valueCount > 0) {
                _arrayIndenter.writeIndentation(generator, _nesting);
            }
            generator.writeRaw(']');
        }
    }

    private static final class BoundedOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate;
        private final int maximumBytes;
        private String pointer = "";

        BoundedOutputStream(int maximumBytes) {
            this.maximumBytes = maximumBytes;
            this.delegate = new ByteArrayOutputStream(Math.min(maximumBytes, 8_192));
        }

        void setPointer(String pointer) {
            this.pointer = Objects.requireNonNull(pointer, "pointer");
        }

        @Override
        public void write(int value) throws IOException {
            reserve(1);
            delegate.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            reserve(length);
            delegate.write(bytes, offset, length);
        }

        byte[] toByteArray() {
            return delegate.toByteArray();
        }

        private void reserve(int bytes) throws OutputLimitIOException {
            long attempted = (long) delegate.size() + (long) bytes;
            if (attempted > maximumBytes) {
                throw new OutputLimitIOException(maximumBytes, attempted, pointer);
            }
        }
    }

    private static final class OutputLimitIOException extends IOException {
        private final int maximumBytes;
        private final long attemptedBytes;
        private final String pointer;

        OutputLimitIOException(int maximumBytes, long attemptedBytes, String pointer) {
            super("Canonical output would contain " + attemptedBytes
                    + " bytes; maximum is " + maximumBytes);
            this.maximumBytes = maximumBytes;
            this.attemptedBytes = attemptedBytes;
            this.pointer = Objects.requireNonNull(pointer, "pointer");
        }

        int maximumBytes() {
            return maximumBytes;
        }

        @SuppressWarnings("unused")
        long attemptedBytes() {
            return attemptedBytes;
        }

        String pointer() {
            return pointer;
        }
    }

    private static OutputLimitIOException findOutputLimit(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof OutputLimitIOException outputLimit) {
                return outputLimit;
            }
            current = current.getCause();
        }
        return null;
    }

    private static String pointerToken(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }
}
