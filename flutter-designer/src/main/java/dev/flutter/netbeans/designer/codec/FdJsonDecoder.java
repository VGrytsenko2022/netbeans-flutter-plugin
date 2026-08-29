package dev.flutter.netbeans.designer.codec;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import dev.flutter.netbeans.designer.catalog.DartNumericLiterals;
import dev.flutter.netbeans.designer.model.CanvasOrientation;
import dev.flutter.netbeans.designer.model.CanvasPreferences;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.DesignerThemeMode;
import dev.flutter.netbeans.designer.model.ExtensionKey;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.model.json.JsonValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;

/** Bounded, strict JSON reader used by {@link FdDocumentCodec}. */
final class FdJsonDecoder {
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final BigInteger CURRENT_VERSION =
            BigInteger.valueOf(DesignerDocument.SCHEMA_VERSION);
    private static final int SAFE_WIDGET_RECURSION_DEPTH = 256;
    private static final int SAFE_EXTENSION_RECURSION_DEPTH = 64;
    private static final int V1_MAX_LIST_CHILDREN = 10_000;

    private final FdCodecLimits limits;
    private final JsonFactory jsonFactory;

    FdJsonDecoder(FdCodecLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
        this.jsonFactory = createJsonFactory(limits);
    }

    FdDecodeResult decode(OriginalFdBytes original) {
        Objects.requireNonNull(original, "original");
        if (original.size() > limits.maxDocumentBytes()) {
            return invalid(
                    original,
                    Optional.empty(),
                    FdCodecDiagnostic.withoutLocation(
                            FdCodecDiagnosticCode.RESOURCE_LIMIT,
                            "",
                            "The document exceeds the configured byte limit."));
        }

        final String json;
        try {
            json = decodeUtf8(original.copyBytes());
        } catch (DecodeFailure failure) {
            return invalid(original, Optional.empty(), failure.diagnostic());
        } catch (RuntimeException unexpected) {
            return invalid(
                    original,
                    Optional.empty(),
                    FdCodecDiagnostic.withoutLocation(
                            FdCodecDiagnosticCode.MALFORMED_UTF8,
                            "",
                            "The document is not valid UTF-8."));
        }

        final Envelope envelope;
        try {
            envelope = scanEnvelope(json);
        } catch (DecodeFailure failure) {
            return invalid(original, failure.declaredVersion(), failure.diagnostic());
        } catch (StreamConstraintsException failure) {
            return invalid(original, Optional.empty(), parserFailure(
                    failure,
                    FdCodecDiagnosticCode.RESOURCE_LIMIT,
                    "The JSON document exceeds a configured resource limit."));
        } catch (JsonParseException failure) {
            return invalid(original, Optional.empty(), jsonParseFailure(failure));
        } catch (IOException | RuntimeException failure) {
            return invalid(
                    original,
                    Optional.empty(),
                    FdCodecDiagnostic.withoutLocation(
                            FdCodecDiagnosticCode.MALFORMED_JSON,
                            "",
                            "The document is not valid JSON."));
        }

        Optional<BigInteger> declaredVersion = Optional.of(envelope.schemaVersion());
        if (!DesignerDocument.FORMAT.equals(envelope.format())) {
            return invalid(
                    original,
                    declaredVersion,
                    FdCodecDiagnostic.withoutLocation(
                            FdCodecDiagnosticCode.UNSUPPORTED_FORMAT,
                            "/format",
                            "The document format is not supported."));
        }
        int comparison = envelope.schemaVersion().compareTo(CURRENT_VERSION);
        if (comparison > 0) {
            return new FdDecodeResult.UnsupportedNewer(envelope.schemaVersion(), original);
        }
        if (envelope.schemaVersion().compareTo(BigInteger.ONE) < 0) {
            return invalid(
                    original,
                    declaredVersion,
                    FdCodecDiagnostic.withoutLocation(
                            FdCodecDiagnosticCode.UNSUPPORTED_OLDER_VERSION,
                            "/schemaVersion",
                            "The declared schema version is older than the supported migration range."));
        }

        try {
            int sourceVersion = envelope.schemaVersion().intValueExact();
            DesignerDocument document = readCurrentDocument(json, sourceVersion);
            return new FdDecodeResult.Current(
                    document,
                    sourceVersion,
                    sourceVersion != DesignerDocument.SCHEMA_VERSION,
                    original);
        } catch (DecodeFailure failure) {
            return invalid(original, declaredVersion, failure.diagnostic());
        } catch (StreamConstraintsException failure) {
            return invalid(original, declaredVersion, parserFailure(
                    failure,
                    FdCodecDiagnosticCode.RESOURCE_LIMIT,
                    "The JSON document exceeds a configured resource limit."));
        } catch (JsonParseException failure) {
            return invalid(original, declaredVersion, jsonParseFailure(failure));
        } catch (IOException failure) {
            return invalid(
                    original,
                    declaredVersion,
                    FdCodecDiagnostic.withoutLocation(
                            FdCodecDiagnosticCode.MALFORMED_JSON,
                            "",
                            "The document is not valid JSON."));
        } catch (RuntimeException failure) {
            // Model constructors deliberately validate their public invariants. User data
            // must never make those implementation exceptions escape the codec boundary.
            return invalid(
                    original,
                    declaredVersion,
                    FdCodecDiagnostic.withoutLocation(
                            FdCodecDiagnosticCode.INVALID_VALUE,
                            "",
                            "The document does not satisfy its declared Flutter Designer contract."));
        }
    }

    private static JsonFactory createJsonFactory(FdCodecLimits limits) {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxDocumentLength(limits.maxDocumentBytes())
                .maxNestingDepth(limits.maxJsonNestingDepth())
                .maxTokenCount(limits.maxJsonTokens())
                .maxNameLength(limits.maxFieldNameUtf16Units())
                .maxStringLength(limits.maxStringUtf16Units())
                .maxNumberLength(limits.maxNumberCharacters())
                .build();
        ErrorReportConfiguration errorReporting = ErrorReportConfiguration.builder()
                .maxRawContentLength(0)
                .maxErrorTokenLength(128)
                .build();
        JsonFactoryBuilder builder = new JsonFactoryBuilder()
                .streamReadConstraints(constraints)
                .errorReportConfiguration(errorReporting)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);
        for (JsonReadFeature feature : JsonReadFeature.values()) {
            builder.disable(feature);
        }
        return builder.build();
    }

    private String decodeUtf8(byte[] bytes) throws DecodeFailure {
        if (hasUtf32OrUtf16Bom(bytes)) {
            throw failure(
                    FdCodecDiagnosticCode.MALFORMED_UTF8,
                    "",
                    "Only UTF-8 encoded documents are supported.");
        }
        int offset = startsWith(bytes, UTF8_BOM) ? UTF8_BOM.length : 0;
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset))
                    .toString();
        } catch (CharacterCodingException failure) {
            throw failure(
                    FdCodecDiagnosticCode.MALFORMED_UTF8,
                    "",
                    "The document is not valid UTF-8.");
        }
    }

    private static boolean hasUtf32OrUtf16Bom(byte[] bytes) {
        return startsWith(bytes, new byte[]{0x00, 0x00, (byte) 0xFE, (byte) 0xFF})
                || startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xFE, 0x00, 0x00})
                || startsWith(bytes, new byte[]{(byte) 0xFE, (byte) 0xFF})
                || startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xFE});
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    /**
     * First-pass, non-recursive scan. It intentionally walks every nested token
     * before version dispatch, so future-version documents get no parser bypass.
     */
    private Envelope scanEnvelope(String json) throws IOException, DecodeFailure {
        try (JsonParser parser = jsonFactory.createParser(json)) {
            JsonToken first = parser.nextToken();
            if (first == null) {
                throw locatedFailure(
                        parser,
                        FdCodecDiagnosticCode.MALFORMED_JSON,
                        "",
                        "The JSON document is empty.");
            }
            if (first != JsonToken.START_OBJECT) {
                throw locatedFailure(
                        parser,
                        FdCodecDiagnosticCode.ROOT_MUST_BE_OBJECT,
                        "",
                        "The JSON root must be an object.");
            }

            Deque<ScanFrame> stack = new ArrayDeque<>();
            stack.push(new ScanFrame(true, ""));
            String format = null;
            BigInteger schemaVersion = null;
            boolean formatSeen = false;
            boolean versionSeen = false;
            DecodeFailure envelopeFailure = null;

            try {
                while (!stack.isEmpty()) {
                    JsonToken token = parser.nextToken();
                if (token == null) {
                    throw locatedFailure(
                            parser,
                            FdCodecDiagnosticCode.MALFORMED_JSON,
                            "",
                            "The JSON document ended before the root object was complete.");
                }
                if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                    ScanFrame completed = stack.peek();
                    if ((token == JsonToken.END_OBJECT) != completed.object()) {
                        throw locatedFailure(
                                parser,
                                FdCodecDiagnosticCode.MALFORMED_JSON,
                                "",
                                "The JSON container is malformed.");
                    }
                    stack.pop();
                    continue;
                }

                ScanFrame parent = stack.peek();
                if (token == JsonToken.FIELD_NAME) {
                    if (!parent.object()) {
                        throw locatedFailure(
                                parser,
                                FdCodecDiagnosticCode.MALFORMED_JSON,
                                "",
                                "The JSON container is malformed.");
                    }
                    parent.increment(limits.maxJsonObjectFields(), parser, "object fields");
                    String fieldName = checkedFieldName(parser, parent.pointer());
                    parent.pendingField(fieldName);
                    continue;
                }

                String rootField = null;
                String valuePointer;
                if (parent.object()) {
                    if (parent.pendingField() == null) {
                        throw locatedFailure(
                                parser,
                                FdCodecDiagnosticCode.MALFORMED_JSON,
                                "",
                                "An object value has no field name.");
                    }
                    if (stack.size() == 1) {
                        rootField = parent.pendingField();
                    }
                    valuePointer = pointer(parent.pointer(), parent.pendingField());
                    parent.pendingField(null);
                } else {
                    parent.increment(limits.maxJsonArrayElements(), parser, "array elements");
                    valuePointer = pointer(parent.pointer(), Integer.toString(parent.count() - 1));
                }

                if (token == JsonToken.VALUE_STRING) {
                    checkedString(parser, valuePointer);
                } else if (token.isNumeric()) {
                    checkedDecimal(parser, valuePointer);
                } else if (token == JsonToken.VALUE_EMBEDDED_OBJECT
                        || token == JsonToken.NOT_AVAILABLE) {
                    throw locatedFailure(
                            parser,
                            FdCodecDiagnosticCode.MALFORMED_JSON,
                            "",
                            "The document contains a non-JSON token.");
                }

                if ("format".equals(rootField)) {
                    formatSeen = true;
                    if (token == JsonToken.VALUE_STRING) {
                        format = parser.getText();
                    } else if (envelopeFailure == null) {
                        envelopeFailure = locatedFailure(
                                parser,
                                FdCodecDiagnosticCode.WRONG_VALUE_TYPE,
                                "/format",
                                "The format field must be a string.");
                    }
                } else if ("schemaVersion".equals(rootField)) {
                    versionSeen = true;
                    if (token.isNumeric()) {
                        try {
                            schemaVersion = parser.getDecimalValue().toBigIntegerExact();
                        } catch (ArithmeticException failure) {
                            if (envelopeFailure == null) {
                                envelopeFailure = locatedFailure(
                                        parser,
                                        FdCodecDiagnosticCode.INVALID_VALUE,
                                        "/schemaVersion",
                                        "The schemaVersion field must be a mathematical integer.");
                            }
                        }
                    } else if (envelopeFailure == null) {
                        envelopeFailure = locatedFailure(
                                parser,
                                FdCodecDiagnosticCode.WRONG_VALUE_TYPE,
                                "/schemaVersion",
                                "The schemaVersion field must be a number.");
                    }
                }

                    if (token == JsonToken.START_OBJECT) {
                        stack.push(new ScanFrame(true, valuePointer));
                    } else if (token == JsonToken.START_ARRAY) {
                        stack.push(new ScanFrame(false, valuePointer));
                    } else if (!token.isScalarValue()) {
                        throw locatedFailure(
                                parser,
                                FdCodecDiagnosticCode.MALFORMED_JSON,
                                "",
                                "The document contains an unexpected JSON token.");
                    }
                }
            } catch (DecodeFailure failure) {
                throw failure.withDeclaredVersion(schemaVersion);
            } catch (StreamConstraintsException failure) {
                throw new DecodeFailure(parserFailure(
                        failure,
                        FdCodecDiagnosticCode.RESOURCE_LIMIT,
                        "The JSON document exceeds a configured resource limit."),
                        Optional.ofNullable(schemaVersion));
            } catch (JsonParseException failure) {
                throw new DecodeFailure(
                        jsonParseFailure(failure),
                        Optional.ofNullable(schemaVersion));
            } catch (IOException | RuntimeException failure) {
                throw new DecodeFailure(
                        diagnostic(
                                FdCodecDiagnosticCode.MALFORMED_JSON,
                                "",
                                "The document is not valid JSON.",
                                parser.currentTokenLocation()),
                        Optional.ofNullable(schemaVersion));
            }

            try {
                if (parser.nextToken() != null) {
                    throw locatedFailure(
                            parser,
                            FdCodecDiagnosticCode.TRAILING_CONTENT,
                            "",
                            "Only one JSON root value is allowed.")
                            .withDeclaredVersion(schemaVersion);
                }
            } catch (JsonParseException trailing) {
                // Once the root is complete, any further non-whitespace byte is
                // trailing content even when it cannot start another JSON value.
                throw new DecodeFailure(diagnostic(
                        FdCodecDiagnosticCode.TRAILING_CONTENT,
                        "",
                        "Only one JSON root value is allowed.",
                        trailing.getLocation()),
                        Optional.ofNullable(schemaVersion));
            } catch (IOException trailing) {
                throw new DecodeFailure(diagnostic(
                        FdCodecDiagnosticCode.TRAILING_CONTENT,
                        "",
                        "Only one JSON root value is allowed.",
                        parser.currentTokenLocation()),
                        Optional.ofNullable(schemaVersion));
            }
            if (!formatSeen) {
                throw failure(
                        FdCodecDiagnosticCode.MISSING_REQUIRED_FIELD,
                        "/format",
                        "The required format field is missing.")
                        .withDeclaredVersion(schemaVersion);
            }
            if (!versionSeen) {
                throw failure(
                        FdCodecDiagnosticCode.MISSING_REQUIRED_FIELD,
                        "/schemaVersion",
                        "The required schemaVersion field is missing.");
            }
            if (envelopeFailure != null) {
                throw envelopeFailure.withDeclaredVersion(schemaVersion);
            }
            if (schemaVersion == null) {
                throw failure(
                        FdCodecDiagnosticCode.INVALID_VALUE,
                        "/schemaVersion",
                        "The schemaVersion field must be a mathematical integer.")
                        .withDeclaredVersion(schemaVersion);
            }
            return new Envelope(format, schemaVersion);
        }
    }

    private DesignerDocument readCurrentDocument(String json, int sourceVersion)
            throws IOException, DecodeFailure {
        DecodeContext context = new DecodeContext();
        try (JsonParser parser = jsonFactory.createParser(json)) {
            requireToken(parser, parser.nextToken(), JsonToken.START_OBJECT, "", "object");

            Optional<String> schemaReference = Optional.empty();
            StableId documentId = null;
            DartSourceDescriptor source = null;
            Optional<CanvasPreferences> canvas = Optional.empty();
            WidgetNode root = null;
            Extensions extensions = Extensions.empty();
            boolean schemaSeen = false;
            boolean formatSeen = false;
            boolean versionSeen = false;
            boolean documentIdSeen = false;
            boolean sourceSeen = false;
            boolean canvasSeen = false;
            boolean rootSeen = false;
            boolean extensionsSeen = false;

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                requireCurrent(parser, JsonToken.FIELD_NAME, "", "field name");
                String field = checkedFieldName(parser, "");
                String pointer = pointer("", field);
                JsonToken valueToken = requiredNext(parser, pointer);
                switch (field) {
                    case "$schema" -> {
                        schemaSeen = true;
                        schemaReference = Optional.of(requireString(parser, valueToken, pointer));
                    }
                    case "format" -> {
                        formatSeen = true;
                        String value = requireString(parser, valueToken, pointer);
                        if (!DesignerDocument.FORMAT.equals(value)) {
                            throw locatedFailure(
                                    parser,
                                    FdCodecDiagnosticCode.UNSUPPORTED_FORMAT,
                                    pointer,
                                    "The document format is not supported.");
                        }
                    }
                    case "schemaVersion" -> {
                        versionSeen = true;
                        BigInteger value = requireMathematicalInteger(parser, valueToken, pointer);
                        if (!BigInteger.valueOf(sourceVersion).equals(value)) {
                            throw locatedFailure(
                                    parser,
                                    FdCodecDiagnosticCode.INVALID_VALUE,
                                    pointer,
                                    "The document schemaVersion changed during version dispatch.");
                        }
                    }
                    case "documentId" -> {
                        documentIdSeen = true;
                        String value = requireString(parser, valueToken, pointer);
                        documentId = modelValue(pointer, () -> StableId.parse(value));
                    }
                    case "source" -> {
                        sourceSeen = true;
                        source = readSource(parser, valueToken, pointer);
                    }
                    case "canvas" -> {
                        canvasSeen = true;
                        canvas = Optional.of(readCanvas(parser, valueToken, pointer));
                    }
                    case "root" -> {
                        rootSeen = true;
                        root = readWidget(parser, valueToken, pointer, 1, context, sourceVersion);
                    }
                    case "extensions" -> {
                        extensionsSeen = true;
                        extensions = readExtensions(parser, valueToken, pointer, context);
                    }
                    default -> throw locatedFailure(
                            parser,
                            FdCodecDiagnosticCode.UNKNOWN_FIELD,
                            pointer,
                            "The document contains an unknown core field.");
                }
            }

            if (!formatSeen) {
                throw missing(parser, "/format", "format");
            }
            if (!versionSeen) {
                throw missing(parser, "/schemaVersion", "schemaVersion");
            }
            if (!documentIdSeen) {
                throw missing(parser, "/documentId", "documentId");
            }
            if (!sourceSeen) {
                throw missing(parser, "/source", "source");
            }
            if (!rootSeen) {
                throw missing(parser, "/root", "root");
            }
            if (parser.nextToken() != null) {
                throw locatedFailure(
                        parser,
                        FdCodecDiagnosticCode.TRAILING_CONTENT,
                        "",
                        "Only one JSON root value is allowed.");
            }

            final StableId finalDocumentId = documentId;
            final DartSourceDescriptor finalSource = source;
            final WidgetNode finalRoot = root;
            final Optional<String> finalSchemaReference = schemaSeen
                    ? migrateSchemaReference(schemaReference, sourceVersion) : Optional.empty();
            final Optional<CanvasPreferences> finalCanvas = canvasSeen
                    ? canvas : Optional.empty();
            final Extensions finalExtensions = extensionsSeen
                    ? extensions : Extensions.empty();
            return modelValue("", () -> new DesignerDocument(
                    finalSchemaReference,
                    finalDocumentId,
                    finalSource,
                    finalCanvas,
                    finalRoot,
                    finalExtensions));
        }
    }

    private static Optional<String> migrateSchemaReference(
            Optional<String> reference,
            int sourceVersion) {
        if (sourceVersion >= DesignerDocument.SCHEMA_VERSION || reference.isEmpty()) {
            return reference;
        }
        return Optional.of(switch (reference.orElseThrow()) {
            case "urn:netbeans-flutter-designer:schema:fd:1",
                    "urn:netbeans-flutter-designer:schema:fd:2" ->
                    "urn:netbeans-flutter-designer:schema:fd:3";
            case "../fd-v1.schema.json", "../fd-v2.schema.json" ->
                    "../fd-v3.schema.json";
            default -> reference.orElseThrow();
        });
    }

    private DartSourceDescriptor readSource(
            JsonParser parser,
            JsonToken token,
            String base) throws IOException, DecodeFailure {
        requireToken(parser, token, JsonToken.START_OBJECT, base, "object");
        String dartFile = null;
        String className = null;
        WidgetClassKind widgetKind = null;
        Optional<String> generatorVersion = Optional.empty();
        ManagedRegions managedRegions = null;
        boolean dartFileSeen = false;
        boolean classNameSeen = false;
        boolean widgetKindSeen = false;
        boolean generatorVersionSeen = false;
        boolean managedRegionsSeen = false;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
            String field = checkedFieldName(parser, base);
            String pointer = pointer(base, field);
            JsonToken valueToken = requiredNext(parser, pointer);
            switch (field) {
                case "dartFile" -> {
                    dartFileSeen = true;
                    dartFile = requireString(parser, valueToken, pointer);
                }
                case "className" -> {
                    classNameSeen = true;
                    className = requireString(parser, valueToken, pointer);
                }
                case "widgetKind" -> {
                    widgetKindSeen = true;
                    String value = requireString(parser, valueToken, pointer);
                    widgetKind = switch (value) {
                        case "stateless" -> WidgetClassKind.STATELESS;
                        case "stateful" -> WidgetClassKind.STATEFUL;
                        default -> throw invalidValue(parser, pointer, "Unknown widgetKind value.");
                    };
                }
                case "generatorVersion" -> {
                    generatorVersionSeen = true;
                    generatorVersion = Optional.of(requireString(parser, valueToken, pointer));
                }
                case "managedRegions" -> {
                    managedRegionsSeen = true;
                    managedRegions = readManagedRegions(parser, valueToken, pointer);
                }
                default -> throw unknownField(parser, pointer);
            }
        }
        if (!dartFileSeen) {
            throw missing(parser, pointer(base, "dartFile"), "dartFile");
        }
        if (!classNameSeen) {
            throw missing(parser, pointer(base, "className"), "className");
        }
        if (!widgetKindSeen) {
            throw missing(parser, pointer(base, "widgetKind"), "widgetKind");
        }
        if (!managedRegionsSeen) {
            throw missing(parser, pointer(base, "managedRegions"), "managedRegions");
        }
        final String finalDartFile = dartFile;
        final String finalClassName = className;
        final WidgetClassKind finalWidgetKind = widgetKind;
        final Optional<String> finalGeneratorVersion = generatorVersionSeen
                ? generatorVersion : Optional.empty();
        final ManagedRegions finalManagedRegions = managedRegions;
        validateSourceFields(
                base,
                finalDartFile,
                finalClassName,
                finalGeneratorVersion,
                finalManagedRegions);
        return modelValue(base, () -> new DartSourceDescriptor(
                finalDartFile,
                finalClassName,
                finalWidgetKind,
                finalGeneratorVersion,
                finalManagedRegions));
    }

    private ManagedRegions readManagedRegions(
            JsonParser parser,
            JsonToken token,
            String base) throws IOException, DecodeFailure {
        requireToken(parser, token, JsonToken.START_OBJECT, base, "object");
        ManagedRegion imports = null;
        ManagedRegion build = null;
        boolean importsSeen = false;
        boolean buildSeen = false;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
            String field = checkedFieldName(parser, base);
            String pointer = pointer(base, field);
            JsonToken valueToken = requiredNext(parser, pointer);
            switch (field) {
                case "imports" -> {
                    importsSeen = true;
                    imports = readManagedRegion(parser, valueToken, pointer);
                }
                case "build" -> {
                    buildSeen = true;
                    build = readManagedRegion(parser, valueToken, pointer);
                }
                default -> throw unknownField(parser, pointer);
            }
        }
        if (!importsSeen) {
            throw missing(parser, pointer(base, "imports"), "imports");
        }
        if (!buildSeen) {
            throw missing(parser, pointer(base, "build"), "build");
        }
        final ManagedRegion finalImports = imports;
        final ManagedRegion finalBuild = build;
        return modelValue(base, () -> new ManagedRegions(finalImports, finalBuild));
    }

    private ManagedRegion readManagedRegion(
            JsonParser parser,
            JsonToken token,
            String base) throws IOException, DecodeFailure {
        requireToken(parser, token, JsonToken.START_OBJECT, base, "object");
        String sha256 = null;
        boolean shaSeen = false;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
            String field = checkedFieldName(parser, base);
            String pointer = pointer(base, field);
            JsonToken valueToken = requiredNext(parser, pointer);
            if (!"sha256".equals(field)) {
                throw unknownField(parser, pointer);
            }
            shaSeen = true;
            sha256 = requireString(parser, valueToken, pointer);
        }
        if (!shaSeen) {
            throw missing(parser, pointer(base, "sha256"), "sha256");
        }
        final String finalSha256 = sha256;
        return modelValue(pointer(base, "sha256"), () -> new ManagedRegion(finalSha256));
    }

    private CanvasPreferences readCanvas(
            JsonParser parser,
            JsonToken token,
            String base) throws IOException, DecodeFailure {
        requireToken(parser, token, JsonToken.START_OBJECT, base, "object");
        Optional<String> preset = Optional.empty();
        Optional<BigDecimal> logicalWidth = Optional.empty();
        Optional<BigDecimal> logicalHeight = Optional.empty();
        Optional<BigDecimal> devicePixelRatio = Optional.empty();
        Optional<CanvasOrientation> orientation = Optional.empty();
        Optional<DesignerThemeMode> themeMode = Optional.empty();
        Optional<BigDecimal> textScaleFactor = Optional.empty();
        Optional<String> locale = Optional.empty();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
            String field = checkedFieldName(parser, base);
            String pointer = pointer(base, field);
            JsonToken valueToken = requiredNext(parser, pointer);
            switch (field) {
                case "preset" -> preset = Optional.of(requireString(parser, valueToken, pointer));
                case "logicalWidth" -> logicalWidth = Optional.of(
                        requireNumber(parser, valueToken, pointer));
                case "logicalHeight" -> logicalHeight = Optional.of(
                        requireNumber(parser, valueToken, pointer));
                case "devicePixelRatio" -> devicePixelRatio = Optional.of(
                        requireNumber(parser, valueToken, pointer));
                case "orientation" -> {
                    String value = requireString(parser, valueToken, pointer);
                    orientation = Optional.of(switch (value) {
                        case "portrait" -> CanvasOrientation.PORTRAIT;
                        case "landscape" -> CanvasOrientation.LANDSCAPE;
                        default -> throw invalidValue(parser, pointer, "Unknown orientation value.");
                    });
                }
                case "themeMode" -> {
                    String value = requireString(parser, valueToken, pointer);
                    themeMode = Optional.of(switch (value) {
                        case "system" -> DesignerThemeMode.SYSTEM;
                        case "light" -> DesignerThemeMode.LIGHT;
                        case "dark" -> DesignerThemeMode.DARK;
                        default -> throw invalidValue(parser, pointer, "Unknown themeMode value.");
                    });
                }
                case "textScaleFactor" -> textScaleFactor = Optional.of(
                        requireNumber(parser, valueToken, pointer));
                case "locale" -> locale = Optional.of(requireString(parser, valueToken, pointer));
                default -> throw unknownField(parser, pointer);
            }
        }
        final Optional<String> finalPreset = preset;
        final Optional<BigDecimal> finalLogicalWidth = logicalWidth;
        final Optional<BigDecimal> finalLogicalHeight = logicalHeight;
        final Optional<BigDecimal> finalDevicePixelRatio = devicePixelRatio;
        final Optional<CanvasOrientation> finalOrientation = orientation;
        final Optional<DesignerThemeMode> finalThemeMode = themeMode;
        final Optional<BigDecimal> finalTextScaleFactor = textScaleFactor;
        final Optional<String> finalLocale = locale;
        validateCanvasFields(
                base,
                finalPreset,
                finalLogicalWidth,
                finalLogicalHeight,
                finalDevicePixelRatio,
                finalTextScaleFactor,
                finalLocale);
        return modelValue(base, () -> new CanvasPreferences(
                finalPreset,
                finalLogicalWidth,
                finalLogicalHeight,
                finalDevicePixelRatio,
                finalOrientation,
                finalThemeMode,
                finalTextScaleFactor,
                finalLocale));
    }

    private static void validateSourceFields(
            String base,
            String dartFile,
            String className,
            Optional<String> generatorVersion,
            ManagedRegions managedRegions) throws DecodeFailure {
        modelValue(pointer(base, "dartFile"), () -> new DartSourceDescriptor(
                dartFile,
                "ValidClass",
                WidgetClassKind.STATELESS,
                Optional.empty(),
                managedRegions));
        modelValue(pointer(base, "className"), () -> new DartSourceDescriptor(
                "valid.dart",
                className,
                WidgetClassKind.STATELESS,
                Optional.empty(),
                managedRegions));
        if (generatorVersion.isPresent()) {
            modelValue(pointer(base, "generatorVersion"), () -> new DartSourceDescriptor(
                    "valid.dart",
                    "ValidClass",
                    WidgetClassKind.STATELESS,
                    generatorVersion,
                    managedRegions));
        }
    }

    private static void validateCanvasFields(
            String base,
            Optional<String> preset,
            Optional<BigDecimal> logicalWidth,
            Optional<BigDecimal> logicalHeight,
            Optional<BigDecimal> devicePixelRatio,
            Optional<BigDecimal> textScaleFactor,
            Optional<String> locale) throws DecodeFailure {
        if (preset.isPresent()) {
            modelValue(pointer(base, "preset"), () -> new CanvasPreferences(
                    preset,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()));
        }
        if (logicalWidth.isPresent()) {
            modelValue(pointer(base, "logicalWidth"), () -> new CanvasPreferences(
                    Optional.empty(),
                    logicalWidth,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()));
        }
        if (logicalHeight.isPresent()) {
            modelValue(pointer(base, "logicalHeight"), () -> new CanvasPreferences(
                    Optional.empty(),
                    Optional.empty(),
                    logicalHeight,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()));
        }
        if (devicePixelRatio.isPresent()) {
            modelValue(pointer(base, "devicePixelRatio"), () -> new CanvasPreferences(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    devicePixelRatio,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()));
        }
        if (textScaleFactor.isPresent()) {
            modelValue(pointer(base, "textScaleFactor"), () -> new CanvasPreferences(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    textScaleFactor,
                    Optional.empty()));
        }
        if (locale.isPresent()) {
            modelValue(pointer(base, "locale"), () -> new CanvasPreferences(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    locale));
        }
    }

    private WidgetNode readWidget(
            JsonParser parser,
            JsonToken token,
            String base,
            int depth,
            DecodeContext context,
            int sourceVersion) throws IOException, DecodeFailure {
        if (depth > Math.min(limits.maxWidgetDepth(), SAFE_WIDGET_RECURSION_DEPTH)) {
            throw resourceLimit(parser, base, "The widget tree exceeds the configured depth limit.");
        }
        context.incrementWidgetNodes(parser, base);
        requireToken(parser, token, JsonToken.START_OBJECT, base, "object");
        StableId id = null;
        WidgetTypeId type = null;
        Map<PropertyName, PropertyValue> properties = null;
        Map<SlotName, WidgetSlot> slots = null;
        Extensions extensions = Extensions.empty();
        boolean idSeen = false;
        boolean typeSeen = false;
        boolean propertiesSeen = false;
        boolean slotsSeen = false;
        boolean extensionsSeen = false;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
            String field = checkedFieldName(parser, base);
            String pointer = pointer(base, field);
            JsonToken valueToken = requiredNext(parser, pointer);
            switch (field) {
                case "id" -> {
                    idSeen = true;
                    String value = requireString(parser, valueToken, pointer);
                    id = modelValue(pointer, () -> StableId.parse(value));
                }
                case "type" -> {
                    typeSeen = true;
                    String value = requireString(parser, valueToken, pointer);
                    type = modelValue(pointer, () -> new WidgetTypeId(value));
                }
                case "properties" -> {
                    propertiesSeen = true;
                    properties = readProperties(parser, valueToken, pointer, sourceVersion);
                }
                case "slots" -> {
                    slotsSeen = true;
                    slots = readSlots(
                            parser, valueToken, pointer, depth, context, sourceVersion);
                }
                case "extensions" -> {
                    extensionsSeen = true;
                    extensions = readExtensions(parser, valueToken, pointer, context);
                }
                default -> throw unknownField(parser, pointer);
            }
        }
        if (!idSeen) {
            throw missing(parser, pointer(base, "id"), "id");
        }
        if (!typeSeen) {
            throw missing(parser, pointer(base, "type"), "type");
        }
        if (!propertiesSeen) {
            throw missing(parser, pointer(base, "properties"), "properties");
        }
        if (!slotsSeen) {
            throw missing(parser, pointer(base, "slots"), "slots");
        }
        final StableId finalId = id;
        final WidgetTypeId finalType = type;
        final Map<PropertyName, PropertyValue> finalProperties = properties;
        final Map<SlotName, WidgetSlot> finalSlots = slots;
        final Extensions finalExtensions = extensionsSeen ? extensions : Extensions.empty();
        return modelValue(base, () -> new WidgetNode(
                finalId,
                finalType,
                finalProperties,
                finalSlots,
                finalExtensions));
    }

    private Map<PropertyName, PropertyValue> readProperties(
            JsonParser parser,
            JsonToken token,
            String base,
            int sourceVersion) throws IOException, DecodeFailure {
        requireToken(parser, token, JsonToken.START_OBJECT, base, "object");
        LinkedHashMap<PropertyName, PropertyValue> values = new LinkedHashMap<>();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (values.size() >= limits.maxPropertiesPerWidget()) {
                throw resourceLimit(
                        parser,
                        base,
                        "A widget exceeds the configured property-count limit.");
            }
            requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
            String field = checkedFieldName(parser, base);
            String pointer = pointer(base, field);
            PropertyName name = modelValue(pointer, () -> new PropertyName(field));
            JsonToken valueToken = requiredNext(parser, pointer);
            values.put(name, readPropertyValue(parser, valueToken, pointer, sourceVersion));
        }
        return values;
    }

    private PropertyValue readPropertyValue(
            JsonParser parser,
            JsonToken token,
            String base,
            int sourceVersion) throws IOException, DecodeFailure {
        requireToken(parser, token, JsonToken.START_OBJECT, base, "object");
        LinkedHashMap<String, JsonValue> fields = new LinkedHashMap<>();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
            String field = checkedFieldName(parser, base);
            String pointer = pointer(base, field);
            JsonToken valueToken = requiredNext(parser, pointer);
            fields.put(field, readPropertyJsonValue(parser, valueToken, pointer, 1));
        }
        String kind = jsonString(fields, "kind", base);
        if (sourceVersion == 1 && !Set.of(
                "string", "boolean", "integer", "double", "enum", "color",
                "edgeInsets", "asset", "callback", "dartExpression").contains(kind)) {
            throw invalidValue(
                    parser,
                    pointer(base, "kind"),
                    "Schema version 1 does not define this property kind.");
        }
        return switch (kind) {
            case "string" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "value"));
                String value = jsonString(fields, "value", base);
                yield modelValue(base, () -> new PropertyValue.StringValue(value));
            }
            case "boolean" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "value"));
                boolean value = jsonBoolean(fields, "value", base);
                yield new PropertyValue.BooleanValue(value);
            }
            case "integer" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "value"));
                BigInteger value = jsonInteger(fields, "value", base);
                yield modelValue(base, () -> new PropertyValue.IntegerValue(value));
            }
            case "double" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "value"));
                BigDecimal value = jsonNumber(fields, "value", base);
                yield modelValue(base, () -> new PropertyValue.DoubleValue(value));
            }
            case "enum" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "type", "value"));
                String type = jsonString(fields, "type", base);
                String value = jsonString(fields, "value", base);
                modelValue(
                        pointer(base, "type"),
                        () -> new PropertyValue.EnumValue(type, "value"));
                modelValue(
                        pointer(base, "value"),
                        () -> new PropertyValue.EnumValue("EnumType", value));
                yield modelValue(base, () -> new PropertyValue.EnumValue(type, value));
            }
            case "color" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "argb"));
                String argb = jsonString(fields, "argb", base);
                yield modelValue(
                        pointer(base, "argb"),
                        () -> PropertyValue.ColorValue.fromWireArgb(argb));
            }
            case "edgeInsets" -> {
                boolean directional = fields.containsKey("start")
                        || fields.containsKey("end");
                if (directional) {
                    if (sourceVersion < 3) {
                        throw invalidValue(
                                parser,
                                pointer(base, "kind"),
                                "Directional edge insets require schema version 3.");
                    }
                    enforceAllowedFields(
                            parser,
                            fields,
                            base,
                            Set.of("kind", "start", "top", "end", "bottom"));
                    BigDecimal start = jsonNumber(fields, "start", base);
                    BigDecimal top = jsonNumber(fields, "top", base);
                    BigDecimal end = jsonNumber(fields, "end", base);
                    BigDecimal bottom = jsonNumber(fields, "bottom", base);
                    yield modelValue(base,
                            () -> new PropertyValue.EdgeInsetsDirectionalValue(
                                    start, top, end, bottom));
                }
                enforceAllowedFields(
                        parser,
                        fields,
                        base,
                        Set.of("kind", "left", "top", "right", "bottom"));
                BigDecimal left = jsonNumber(fields, "left", base);
                BigDecimal top = jsonNumber(fields, "top", base);
                BigDecimal right = jsonNumber(fields, "right", base);
                BigDecimal bottom = jsonNumber(fields, "bottom", base);
                yield modelValue(base, () -> new PropertyValue.EdgeInsetsValue(
                        left, top, right, bottom));
            }
            case "asset" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "path"));
                String path = jsonString(fields, "path", base);
                yield modelValue(
                        pointer(base, "path"),
                        () -> new PropertyValue.AssetValue(path));
            }
            case "callback" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "handler"));
                String handler = jsonString(fields, "handler", base);
                yield modelValue(
                        pointer(base, "handler"),
                        () -> new PropertyValue.CallbackValue(handler));
            }
            case "dartExpression" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "code"));
                String code = jsonString(fields, "code", base);
                yield modelValue(
                        pointer(base, "code"),
                        () -> new PropertyValue.DartExpressionValue(code));
            }
            case "themeToken" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "token"));
                String tokenValue = jsonString(fields, "token", base);
                ThemeToken themeToken = modelValue(
                        pointer(base, "token"), () -> new ThemeToken(tokenValue));
                yield new PropertyValue.ThemeTokenValue(themeToken);
            }
            case "paint" -> {
                enforceAllowedFields(
                        parser,
                        fields,
                        base,
                        Set.of(
                                "kind", "color", "blendMode", "style", "strokeWidth",
                                "strokeCap", "strokeJoin", "strokeMiterLimit", "antiAlias",
                                "filterQuality", "invertColors", "maskFilter"));
                ColorSource color = readColorSource(requiredJson(fields, "color", base),
                        pointer(base, "color"));
                String blendModeName = jsonString(fields, "blendMode", base);
                PropertyValue.PaintValue.BlendMode blendMode = modelValue(
                        pointer(base, "blendMode"),
                        () -> PropertyValue.PaintValue.BlendMode.fromWireName(
                                blendModeName));
                String styleName = jsonString(fields, "style", base);
                PropertyValue.PaintValue.Style style = modelValue(
                        pointer(base, "style"),
                        () -> PropertyValue.PaintValue.Style.fromWireName(
                                styleName));
                BigDecimal strokeWidth = jsonDartDouble(fields, "strokeWidth", base);
                String strokeCapName = jsonString(fields, "strokeCap", base);
                PropertyValue.PaintValue.StrokeCap strokeCap = modelValue(
                        pointer(base, "strokeCap"),
                        () -> PropertyValue.PaintValue.StrokeCap.fromWireName(
                                strokeCapName));
                String strokeJoinName = jsonString(fields, "strokeJoin", base);
                PropertyValue.PaintValue.StrokeJoin strokeJoin = modelValue(
                        pointer(base, "strokeJoin"),
                        () -> PropertyValue.PaintValue.StrokeJoin.fromWireName(
                                strokeJoinName));
                BigDecimal strokeMiterLimit = jsonDartDouble(
                        fields, "strokeMiterLimit", base);
                boolean antiAlias = jsonBoolean(fields, "antiAlias", base);
                String filterQualityName = jsonString(fields, "filterQuality", base);
                PropertyValue.PaintValue.FilterQuality filterQuality = modelValue(
                        pointer(base, "filterQuality"),
                        () -> PropertyValue.PaintValue.FilterQuality.fromWireName(
                                filterQualityName));
                boolean invertColors = jsonBoolean(fields, "invertColors", base);
                Optional<PropertyValue.PaintValue.BlurMask> maskFilter = fields.containsKey("maskFilter")
                        ? Optional.of(readBlurMask(
                                fields.get("maskFilter"), pointer(base, "maskFilter")))
                        : Optional.empty();
                yield modelValue(base, () -> new PropertyValue.PaintValue(
                        color,
                        blendMode,
                        style,
                        strokeWidth,
                        strokeCap,
                        strokeJoin,
                        strokeMiterLimit,
                        antiAlias,
                        filterQuality,
                        invertColors,
                        maskFilter));
            }
            case "shadowList" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "items"));
                yield readShadowList(requiredJson(fields, "items", base), pointer(base, "items"));
            }
            case "fontFeatureList" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "items"));
                yield readFontFeatureList(
                        requiredJson(fields, "items", base), pointer(base, "items"));
            }
            case "fontVariationList" -> {
                enforceAllowedFields(parser, fields, base, Set.of("kind", "items"));
                yield readFontVariationList(
                        requiredJson(fields, "items", base), pointer(base, "items"));
            }
            default -> throw invalidValue(parser, pointer(base, "kind"), "Unknown property kind.");
        };
    }

    private JsonValue readPropertyJsonValue(
            JsonParser parser,
            JsonToken token,
            String base,
            int depth) throws IOException, DecodeFailure {
        if (depth > 8) {
            throw resourceLimit(parser, base, "A structured property exceeds its nesting limit.");
        }
        return switch (token) {
            case VALUE_NULL -> JsonValue.NullValue.INSTANCE;
            case VALUE_TRUE -> new JsonValue.BooleanValue(true);
            case VALUE_FALSE -> new JsonValue.BooleanValue(false);
            case VALUE_STRING -> new JsonValue.StringValue(checkedString(parser, base));
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT ->
                    new JsonValue.NumberValue(checkedDecimal(parser, base));
            case START_ARRAY -> {
                List<JsonValue> values = new ArrayList<>();
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    if (values.size() >= PropertyValue.ShadowListValue.MAX_ITEMS) {
                        throw resourceLimit(
                                parser, base, "A structured property list exceeds 256 items.");
                    }
                    String itemPointer = pointer(base, Integer.toString(values.size()));
                    values.add(readPropertyJsonValue(
                            parser, parser.currentToken(), itemPointer, depth + 1));
                }
                yield new JsonValue.ArrayValue(values);
            }
            case START_OBJECT -> {
                LinkedHashMap<String, JsonValue> values = new LinkedHashMap<>();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    if (values.size() >= 32) {
                        throw resourceLimit(
                                parser, base, "A structured property object exceeds 32 fields.");
                    }
                    requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
                    String field = checkedFieldName(parser, base);
                    String fieldPointer = pointer(base, field);
                    JsonToken valueToken = requiredNext(parser, fieldPointer);
                    values.put(field, readPropertyJsonValue(
                            parser, valueToken, fieldPointer, depth + 1));
                }
                yield new JsonValue.ObjectValue(values);
            }
            default -> throw wrongType(parser, base, "JSON value");
        };
    }

    private ColorSource readColorSource(JsonValue value, String base) throws DecodeFailure {
        Map<String, JsonValue> fields = jsonObject(value, base);
        String kind = jsonString(fields, "kind", base);
        return switch (kind) {
            case "literal" -> {
                enforceAllowedFields(fields, base, Set.of("kind", "argb"));
                String argb = jsonString(fields, "argb", base);
                yield modelValue(
                        pointer(base, "argb"), () -> ColorSource.Literal.fromWireArgb(argb));
            }
            case "theme" -> {
                enforceAllowedFields(fields, base, Set.of("kind", "token"));
                String token = jsonString(fields, "token", base);
                yield modelValue(base, () -> new ColorSource.Theme(new ThemeToken(token)));
            }
            default -> throw failure(
                    FdCodecDiagnosticCode.INVALID_VALUE,
                    pointer(base, "kind"),
                    "Unknown color source kind.");
        };
    }

    private PropertyValue.PaintValue.BlurMask readBlurMask(JsonValue value, String base)
            throws DecodeFailure {
        Map<String, JsonValue> fields = jsonObject(value, base);
        enforceAllowedFields(fields, base, Set.of("style", "sigma"));
        String styleName = jsonString(fields, "style", base);
        PropertyValue.PaintValue.BlurStyle style = modelValue(
                pointer(base, "style"),
                () -> PropertyValue.PaintValue.BlurStyle.fromWireName(
                        styleName));
        BigDecimal sigma = jsonDartDouble(fields, "sigma", base);
        return modelValue(base, () -> new PropertyValue.PaintValue.BlurMask(style, sigma));
    }

    private PropertyValue.ShadowListValue readShadowList(JsonValue value, String base)
            throws DecodeFailure {
        List<JsonValue> values = jsonArray(value, base);
        List<PropertyValue.ShadowListValue.Shadow> items = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String itemPointer = pointer(base, Integer.toString(index));
            Map<String, JsonValue> fields = jsonObject(values.get(index), itemPointer);
            enforceAllowedFields(
                    fields,
                    itemPointer,
                    Set.of("id", "color", "offsetX", "offsetY", "blurRadius"));
            String idText = jsonString(fields, "id", itemPointer);
            StableId id = modelValue(pointer(itemPointer, "id"), () -> StableId.parse(idText));
            ColorSource color = readColorSource(
                    requiredJson(fields, "color", itemPointer), pointer(itemPointer, "color"));
            BigDecimal offsetX = jsonDartDouble(fields, "offsetX", itemPointer);
            BigDecimal offsetY = jsonDartDouble(fields, "offsetY", itemPointer);
            BigDecimal blurRadius = jsonDartDouble(fields, "blurRadius", itemPointer);
            items.add(modelValue(itemPointer, () -> new PropertyValue.ShadowListValue.Shadow(
                    id, color, offsetX, offsetY, blurRadius)));
        }
        return modelValue(base, () -> new PropertyValue.ShadowListValue(items));
    }

    private PropertyValue.FontFeatureListValue readFontFeatureList(JsonValue value, String base)
            throws DecodeFailure {
        List<JsonValue> values = jsonArray(value, base);
        List<PropertyValue.FontFeatureListValue.FontFeature> items =
                new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String itemPointer = pointer(base, Integer.toString(index));
            Map<String, JsonValue> fields = jsonObject(values.get(index), itemPointer);
            enforceAllowedFields(fields, itemPointer, Set.of("id", "tag", "value"));
            String idText = jsonString(fields, "id", itemPointer);
            StableId id = modelValue(pointer(itemPointer, "id"), () -> StableId.parse(idText));
            String tag = jsonString(fields, "tag", itemPointer);
            BigInteger rawValue = jsonInteger(fields, "value", itemPointer);
            int featureValue = modelValue(
                    pointer(itemPointer, "value"), rawValue::intValueExact);
            items.add(modelValue(itemPointer, () ->
                    new PropertyValue.FontFeatureListValue.FontFeature(id, tag, featureValue)));
        }
        return modelValue(base, () -> new PropertyValue.FontFeatureListValue(items));
    }

    private PropertyValue.FontVariationListValue readFontVariationList(
            JsonValue value,
            String base) throws DecodeFailure {
        List<JsonValue> values = jsonArray(value, base);
        List<PropertyValue.FontVariationListValue.FontVariation> items =
                new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String itemPointer = pointer(base, Integer.toString(index));
            Map<String, JsonValue> fields = jsonObject(values.get(index), itemPointer);
            enforceAllowedFields(fields, itemPointer, Set.of("id", "axis", "value"));
            String idText = jsonString(fields, "id", itemPointer);
            StableId id = modelValue(pointer(itemPointer, "id"), () -> StableId.parse(idText));
            String axis = jsonString(fields, "axis", itemPointer);
            BigDecimal variationValue = jsonDartDouble(fields, "value", itemPointer);
            items.add(modelValue(itemPointer, () ->
                    new PropertyValue.FontVariationListValue.FontVariation(
                            id, axis, variationValue)));
        }
        return modelValue(base, () -> new PropertyValue.FontVariationListValue(items));
    }

    private Map<SlotName, WidgetSlot> readSlots(
            JsonParser parser,
            JsonToken token,
            String base,
            int widgetDepth,
            DecodeContext context,
            int sourceVersion) throws IOException, DecodeFailure {
        requireToken(parser, token, JsonToken.START_OBJECT, base, "object");
        LinkedHashMap<SlotName, WidgetSlot> values = new LinkedHashMap<>();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (values.size() >= limits.maxSlotsPerWidget()) {
                throw resourceLimit(
                        parser,
                        base,
                        "A widget exceeds the configured slot-count limit.");
            }
            requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
            String field = checkedFieldName(parser, base);
            String pointer = pointer(base, field);
            SlotName name = modelValue(pointer, () -> new SlotName(field));
            JsonToken valueToken = requiredNext(parser, pointer);
            values.put(name, readSlot(
                    parser, valueToken, pointer, widgetDepth, context, sourceVersion));
        }
        return values;
    }

    private WidgetSlot readSlot(
            JsonParser parser,
            JsonToken token,
            String base,
            int widgetDepth,
            DecodeContext context,
            int sourceVersion) throws IOException, DecodeFailure {
        requireToken(parser, token, JsonToken.START_OBJECT, base, "object");
        String kind = null;
        WidgetNode child = null;
        List<WidgetNode> children = null;
        boolean kindSeen = false;
        boolean childSeen = false;
        boolean childrenSeen = false;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
            String field = checkedFieldName(parser, base);
            String pointer = pointer(base, field);
            JsonToken valueToken = requiredNext(parser, pointer);
            switch (field) {
                case "kind" -> {
                    kindSeen = true;
                    kind = requireString(parser, valueToken, pointer);
                }
                case "child" -> {
                    childSeen = true;
                    if (valueToken == JsonToken.VALUE_NULL) {
                        child = null;
                    } else {
                        child = readWidget(
                                parser,
                                valueToken,
                                pointer,
                                widgetDepth + 1,
                                context,
                                sourceVersion);
                    }
                }
                case "children" -> {
                    childrenSeen = true;
                    children = readWidgetList(
                            parser,
                            valueToken,
                            pointer,
                            widgetDepth + 1,
                            context,
                            sourceVersion);
                }
                default -> throw unknownField(parser, pointer);
            }
        }
        if (!kindSeen) {
            throw missing(parser, pointer(base, "kind"), "kind");
        }
        if ("single".equals(kind)) {
            if (childrenSeen) {
                throw unknownField(parser, pointer(base, "children"));
            }
            if (!childSeen) {
                throw missing(parser, pointer(base, "child"), "child");
            }
            final WidgetNode finalChild = child;
            return modelValue(base, () -> new WidgetSlot.SingleSlot(
                    Optional.ofNullable(finalChild)));
        }
        if ("list".equals(kind)) {
            if (childSeen) {
                throw unknownField(parser, pointer(base, "child"));
            }
            if (!childrenSeen) {
                throw missing(parser, pointer(base, "children"), "children");
            }
            final List<WidgetNode> finalChildren = children;
            return modelValue(base, () -> new WidgetSlot.ListSlot(finalChildren));
        }
        throw invalidValue(parser, pointer(base, "kind"), "Unknown slot kind.");
    }

    private List<WidgetNode> readWidgetList(
            JsonParser parser,
            JsonToken token,
            String base,
            int childDepth,
            DecodeContext context,
            int sourceVersion) throws IOException, DecodeFailure {
        requireToken(parser, token, JsonToken.START_ARRAY, base, "array");
        List<WidgetNode> children = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (children.size() >= Math.min(
                    limits.maxListChildren(), V1_MAX_LIST_CHILDREN)) {
                throw resourceLimit(
                        parser,
                        base,
                        "A list slot exceeds the configured child-count limit.");
            }
            String pointer = pointer(base, Integer.toString(children.size()));
            children.add(readWidget(
                    parser,
                    parser.currentToken(),
                    pointer,
                    childDepth,
                    context,
                    sourceVersion));
        }
        return children;
    }

    private Extensions readExtensions(
            JsonParser parser,
            JsonToken token,
            String base,
            DecodeContext context) throws IOException, DecodeFailure {
        requireToken(parser, token, JsonToken.START_OBJECT, base, "object");
        LinkedHashMap<ExtensionKey, JsonValue> values = new LinkedHashMap<>();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (values.size() >= limits.maxExtensionKeysPerBag()) {
                throw resourceLimit(
                        parser,
                        base,
                        "An extension bag exceeds the configured key-count limit.");
            }
            requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
            String field = checkedFieldName(parser, base);
            String pointer = pointer(base, field);
            ExtensionKey key = modelValue(pointer, () -> new ExtensionKey(field));
            JsonToken valueToken = requiredNext(parser, pointer);
            values.put(key, readExtensionValue(parser, valueToken, pointer, 1, context));
        }
        return modelValue(base, () -> new Extensions(values));
    }

    private JsonValue readExtensionValue(
            JsonParser parser,
            JsonToken token,
            String base,
            int depth,
            DecodeContext context) throws IOException, DecodeFailure {
        if (depth > Math.min(
                limits.maxExtensionNestingDepth(), SAFE_EXTENSION_RECURSION_DEPTH)) {
            throw resourceLimit(
                    parser,
                    base,
                    "Extension data exceeds the configured nesting-depth limit.");
        }
        context.incrementExtensionValues(parser, base);
        return switch (token) {
            case VALUE_NULL -> JsonValue.NullValue.INSTANCE;
            case VALUE_TRUE -> new JsonValue.BooleanValue(true);
            case VALUE_FALSE -> new JsonValue.BooleanValue(false);
            case VALUE_STRING -> new JsonValue.StringValue(checkedString(parser, base));
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT ->
                    new JsonValue.NumberValue(checkedDecimal(parser, base));
            case START_ARRAY -> {
                List<JsonValue> values = new ArrayList<>();
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    if (values.size() >= limits.maxJsonArrayElements()) {
                        throw resourceLimit(
                                parser,
                                base,
                                "A JSON array exceeds the configured element-count limit.");
                    }
                    String pointer = pointer(base, Integer.toString(values.size()));
                    values.add(readExtensionValue(
                            parser, parser.currentToken(), pointer, depth + 1, context));
                }
                yield new JsonValue.ArrayValue(values);
            }
            case START_OBJECT -> {
                LinkedHashMap<String, JsonValue> values = new LinkedHashMap<>();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    if (values.size() >= limits.maxJsonObjectFields()) {
                        throw resourceLimit(
                                parser,
                                base,
                                "A JSON object exceeds the configured field-count limit.");
                    }
                    requireCurrent(parser, JsonToken.FIELD_NAME, base, "field name");
                    String field = checkedFieldName(parser, base);
                    String pointer = pointer(base, field);
                    JsonToken valueToken = requiredNext(parser, pointer);
                    values.put(field, readExtensionValue(
                            parser, valueToken, pointer, depth + 1, context));
                }
                yield new JsonValue.ObjectValue(values);
            }
            default -> throw wrongType(parser, base, "JSON value");
        };
    }

    private String jsonString(Map<String, JsonValue> fields, String field, String base)
            throws DecodeFailure {
        JsonValue value = requiredJson(fields, field, base);
        if (!(value instanceof JsonValue.StringValue stringValue)) {
            throw failure(
                    FdCodecDiagnosticCode.WRONG_VALUE_TYPE,
                    pointer(base, field),
                    "The field must be a string.");
        }
        return stringValue.value();
    }

    private boolean jsonBoolean(Map<String, JsonValue> fields, String field, String base)
            throws DecodeFailure {
        JsonValue value = requiredJson(fields, field, base);
        if (!(value instanceof JsonValue.BooleanValue booleanValue)) {
            throw failure(
                    FdCodecDiagnosticCode.WRONG_VALUE_TYPE,
                    pointer(base, field),
                    "The field must be a boolean.");
        }
        return booleanValue.value();
    }

    private BigDecimal jsonNumber(Map<String, JsonValue> fields, String field, String base)
            throws DecodeFailure {
        JsonValue value = requiredJson(fields, field, base);
        if (!(value instanceof JsonValue.NumberValue numberValue)) {
            throw failure(
                    FdCodecDiagnosticCode.WRONG_VALUE_TYPE,
                    pointer(base, field),
                    "The field must be a number.");
        }
        return numberValue.value();
    }

    private BigDecimal jsonDartDouble(
            Map<String, JsonValue> fields,
            String field,
            String base) throws DecodeFailure {
        BigDecimal value = jsonNumber(fields, field, base);
        if (!DartNumericLiterals.isRepresentableDouble(value)) {
            throw failure(
                    FdCodecDiagnosticCode.INVALID_VALUE,
                    pointer(base, field),
                    "The field must be exactly representable as a finite Dart double.");
        }
        return value;
    }

    private BigInteger jsonInteger(Map<String, JsonValue> fields, String field, String base)
            throws DecodeFailure {
        BigDecimal value = jsonNumber(fields, field, base);
        try {
            return value.toBigIntegerExact();
        } catch (ArithmeticException failure) {
            throw failure(
                    FdCodecDiagnosticCode.INVALID_VALUE,
                    pointer(base, field),
                    "The field must be a mathematical integer.");
        }
    }

    private JsonValue requiredJson(Map<String, JsonValue> fields, String field, String base)
            throws DecodeFailure {
        JsonValue value = fields.get(field);
        if (value == null) {
            throw failure(
                    FdCodecDiagnosticCode.MISSING_REQUIRED_FIELD,
                    pointer(base, field),
                    "A required field is missing.");
        }
        return value;
    }

    private Map<String, JsonValue> jsonObject(JsonValue value, String pointer)
            throws DecodeFailure {
        if (!(value instanceof JsonValue.ObjectValue objectValue)) {
            throw failure(
                    FdCodecDiagnosticCode.WRONG_VALUE_TYPE,
                    pointer,
                    "The field must be an object.");
        }
        return objectValue.values();
    }

    private List<JsonValue> jsonArray(JsonValue value, String pointer)
            throws DecodeFailure {
        if (!(value instanceof JsonValue.ArrayValue arrayValue)) {
            throw failure(
                    FdCodecDiagnosticCode.WRONG_VALUE_TYPE,
                    pointer,
                    "The field must be an array.");
        }
        return arrayValue.values();
    }

    private void enforceAllowedFields(
            JsonParser parser,
            Map<String, JsonValue> fields,
            String base,
            Set<String> allowed) throws DecodeFailure {
        for (String field : fields.keySet()) {
            if (!allowed.contains(field)) {
                throw unknownField(parser, pointer(base, field));
            }
        }
    }

    private void enforceAllowedFields(
            Map<String, JsonValue> fields,
            String base,
            Set<String> allowed) throws DecodeFailure {
        for (String field : fields.keySet()) {
            if (!allowed.contains(field)) {
                throw failure(
                        FdCodecDiagnosticCode.UNKNOWN_FIELD,
                        pointer(base, field),
                        "The object contains an unknown field.");
            }
        }
    }

    private String requireString(JsonParser parser, JsonToken token, String pointer)
            throws IOException, DecodeFailure {
        if (token != JsonToken.VALUE_STRING) {
            throw wrongType(parser, pointer, "string");
        }
        return checkedString(parser, pointer);
    }

    private BigDecimal requireNumber(JsonParser parser, JsonToken token, String pointer)
            throws IOException, DecodeFailure {
        if (!token.isNumeric()) {
            throw wrongType(parser, pointer, "number");
        }
        return checkedDecimal(parser, pointer);
    }

    private BigInteger requireMathematicalInteger(
            JsonParser parser,
            JsonToken token,
            String pointer) throws IOException, DecodeFailure {
        BigDecimal number = requireNumber(parser, token, pointer);
        try {
            return number.toBigIntegerExact();
        } catch (ArithmeticException failure) {
            throw invalidValue(parser, pointer, "The field must be a mathematical integer.");
        }
    }

    private BigDecimal checkedDecimal(JsonParser parser, String pointer)
            throws IOException, DecodeFailure {
        BigDecimal value = parser.getDecimalValue();
        int scale = value.scale();
        if (scale > limits.maxAbsoluteDecimalScale()
                || scale < -limits.maxAbsoluteDecimalScale()) {
            throw locatedFailure(
                    parser,
                    FdCodecDiagnosticCode.NUMBER_RANGE,
                    pointer,
                    "The number exceeds the configured decimal-scale limit.");
        }
        return value;
    }

    private String checkedFieldName(JsonParser parser, String base)
            throws IOException, DecodeFailure {
        String value = parser.getText();
        validateUnicodeScalarString(parser, value, base, true);
        String pointer = pointer(base, value);
        if (value.length() > limits.maxFieldNameUtf16Units()) {
            throw resourceLimit(
                    parser,
                    pointer,
                    "A JSON field name exceeds the configured length limit.");
        }
        return value;
    }

    private String checkedString(JsonParser parser, String pointer)
            throws IOException, DecodeFailure {
        String value = parser.getText();
        if (value.length() > limits.maxStringUtf16Units()) {
            throw resourceLimit(
                    parser,
                    pointer,
                    "A JSON string exceeds the configured UTF-16 length limit.");
        }
        validateUnicodeScalarString(parser, value, pointer, false);
        if (value.codePointCount(0, value.length()) > limits.maxStringCodePoints()) {
            throw resourceLimit(
                    parser,
                    pointer,
                    "A JSON string exceeds the configured code-point limit.");
        }
        return value;
    }

    private void validateUnicodeScalarString(
            JsonParser parser,
            String value,
            String pointer,
            boolean fieldName) throws DecodeFailure {
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            if (Character.isHighSurrogate(unit)) {
                if (index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw locatedFailure(
                            parser,
                            FdCodecDiagnosticCode.INVALID_VALUE,
                            pointer,
                            fieldName
                                    ? "A JSON field name contains a lone surrogate."
                                    : "A JSON string contains a lone surrogate.");
                }
                index++;
            } else if (Character.isLowSurrogate(unit)) {
                throw locatedFailure(
                        parser,
                        FdCodecDiagnosticCode.INVALID_VALUE,
                        pointer,
                        fieldName
                                ? "A JSON field name contains a lone surrogate."
                                : "A JSON string contains a lone surrogate.");
            }
        }
    }

    private static JsonToken requiredNext(JsonParser parser, String pointer)
            throws IOException, DecodeFailure {
        JsonToken token = parser.nextToken();
        if (token == null) {
            throw locatedFailure(
                    parser,
                    FdCodecDiagnosticCode.MALFORMED_JSON,
                    pointer,
                    "The JSON document ended before a field value was read.");
        }
        return token;
    }

    private static void requireCurrent(
            JsonParser parser,
            JsonToken expected,
            String pointer,
            String expectedDescription) throws DecodeFailure {
        requireToken(parser, parser.currentToken(), expected, pointer, expectedDescription);
    }

    private static void requireToken(
            JsonParser parser,
            JsonToken actual,
            JsonToken expected,
            String pointer,
            String expectedDescription) throws DecodeFailure {
        if (actual != expected) {
            throw wrongType(parser, pointer, expectedDescription);
        }
    }

    private static DecodeFailure missing(JsonParser parser, String pointer, String field) {
        return locatedFailure(
                parser,
                FdCodecDiagnosticCode.MISSING_REQUIRED_FIELD,
                pointer,
                "The required " + field + " field is missing.");
    }

    private static DecodeFailure unknownField(JsonParser parser, String pointer) {
        return locatedFailure(
                parser,
                FdCodecDiagnosticCode.UNKNOWN_FIELD,
                pointer,
                "The version 1 object contains an unknown core field.");
    }

    private static DecodeFailure wrongType(
            JsonParser parser,
            String pointer,
            String expectedDescription) {
        return locatedFailure(
                parser,
                FdCodecDiagnosticCode.WRONG_VALUE_TYPE,
                pointer,
                "The value must be a JSON " + expectedDescription + ".");
    }

    private static DecodeFailure invalidValue(
            JsonParser parser,
            String pointer,
            String message) {
        return locatedFailure(
                parser,
                FdCodecDiagnosticCode.INVALID_VALUE,
                pointer,
                message);
    }

    private static DecodeFailure resourceLimit(
            JsonParser parser,
            String pointer,
            String message) {
        return locatedFailure(
                parser,
                FdCodecDiagnosticCode.RESOURCE_LIMIT,
                pointer,
                message);
    }

    private static <T> T modelValue(String pointer, Supplier<T> constructor)
            throws DecodeFailure {
        try {
            return constructor.get();
        } catch (RuntimeException failure) {
            throw failure(
                    FdCodecDiagnosticCode.INVALID_VALUE,
                    pointer,
                    "The value does not satisfy the version 1 document contract.");
        }
    }

    private static String pointer(String base, String segment) {
        return base + "/" + segment.replace("~", "~0").replace("/", "~1");
    }

    private static DecodeFailure failure(
            FdCodecDiagnosticCode code,
            String pointer,
            String message) {
        return new DecodeFailure(FdCodecDiagnostic.withoutLocation(code, pointer, message));
    }

    private static DecodeFailure locatedFailure(
            JsonParser parser,
            FdCodecDiagnosticCode code,
            String pointer,
            String message) {
        JsonLocation location = parser == null ? null : parser.currentTokenLocation();
        return new DecodeFailure(diagnostic(code, pointer, message, location));
    }

    private static FdCodecDiagnostic jsonParseFailure(JsonParseException failure) {
        String originalMessage = failure.getOriginalMessage();
        boolean duplicate = originalMessage != null
                && originalMessage.startsWith("Duplicate field");
        return parserFailure(
                failure,
                duplicate
                        ? FdCodecDiagnosticCode.DUPLICATE_FIELD
                        : FdCodecDiagnosticCode.MALFORMED_JSON,
                duplicate
                        ? "A JSON object contains a duplicate field."
                        : "The document is not valid JSON.");
    }

    private static FdCodecDiagnostic parserFailure(
            Exception failure,
            FdCodecDiagnosticCode code,
            String message) {
        JsonLocation location = failure instanceof com.fasterxml.jackson.core.JsonProcessingException processing
                ? processing.getLocation() : null;
        return diagnostic(code, "", message, location);
    }

    private static FdCodecDiagnostic diagnostic(
            FdCodecDiagnosticCode code,
            String pointer,
            String message,
            JsonLocation location) {
        OptionalInt line = location != null && location.getLineNr() > 0
                ? OptionalInt.of(location.getLineNr()) : OptionalInt.empty();
        OptionalInt column = location != null && location.getColumnNr() > 0
                ? OptionalInt.of(location.getColumnNr()) : OptionalInt.empty();
        return new FdCodecDiagnostic(
                code,
                pointer,
                message,
                OptionalLong.empty(),
                line,
                column);
    }

    private static FdDecodeResult.Invalid invalid(
            OriginalFdBytes original,
            Optional<BigInteger> version,
            FdCodecDiagnostic diagnostic) {
        return new FdDecodeResult.Invalid(version, List.of(diagnostic), original);
    }

    private record Envelope(String format, BigInteger schemaVersion) {
    }

    private static final class ScanFrame {
        private final boolean object;
        private final String pointer;
        private int count;
        private String pendingField;

        private ScanFrame(boolean object, String pointer) {
            this.object = object;
            this.pointer = pointer;
        }

        boolean object() {
            return object;
        }

        String pointer() {
            return pointer;
        }

        int count() {
            return count;
        }

        String pendingField() {
            return pendingField;
        }

        void pendingField(String field) {
            pendingField = field;
        }

        void increment(int maximum, JsonParser parser, String label) throws DecodeFailure {
            count++;
            if (count > maximum) {
                throw resourceLimit(
                        parser,
                        "",
                        "A JSON container exceeds the configured " + label + " limit.");
            }
        }
    }

    private final class DecodeContext {
        private int widgetNodes;
        private int extensionValues;

        void incrementWidgetNodes(JsonParser parser, String pointer) throws DecodeFailure {
            widgetNodes++;
            if (widgetNodes > limits.maxWidgetNodes()) {
                throw resourceLimit(
                        parser,
                        pointer,
                        "The widget tree exceeds the configured node-count limit.");
            }
        }

        void incrementExtensionValues(JsonParser parser, String pointer) throws DecodeFailure {
            extensionValues++;
            if (extensionValues > limits.maxExtensionValues()) {
                throw resourceLimit(
                        parser,
                        pointer,
                        "Extension data exceeds the configured value-count limit.");
            }
        }
    }

    private static final class DecodeFailure extends Exception {
        private final FdCodecDiagnostic diagnostic;
        private final Optional<BigInteger> declaredVersion;

        private DecodeFailure(FdCodecDiagnostic diagnostic) {
            this(diagnostic, Optional.empty());
        }

        private DecodeFailure(
                FdCodecDiagnostic diagnostic,
                Optional<BigInteger> declaredVersion) {
            super(null, null, false, false);
            this.diagnostic = diagnostic;
            this.declaredVersion = declaredVersion;
        }

        FdCodecDiagnostic diagnostic() {
            return diagnostic;
        }

        Optional<BigInteger> declaredVersion() {
            return declaredVersion;
        }

        DecodeFailure withDeclaredVersion(BigInteger version) {
            if (declaredVersion.isPresent() || version == null) {
                return this;
            }
            return new DecodeFailure(diagnostic, Optional.of(version));
        }
    }
}
