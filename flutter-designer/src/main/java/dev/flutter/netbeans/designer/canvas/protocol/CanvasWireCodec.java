package dev.flutter.netbeans.designer.canvas.protocol;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/** Strict deterministic codec for one already-framed version 1 control body. */
public final class CanvasWireCodec {
    private static final byte[] UTF8_BOM = {
        (byte) 0xEF, (byte) 0xBB, (byte) 0xBF
    };

    private final CanvasWireLimits limits;
    private final JsonFactory jsonFactory;

    public CanvasWireCodec() {
        this(CanvasWireLimits.defaults());
    }

    public CanvasWireCodec(CanvasWireLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
        this.jsonFactory = createJsonFactory(limits);
    }

    public CanvasWireLimits limits() {
        return limits;
    }

    /** Encodes one trusted message in compact canonical field order, without a BOM or newline. */
    public byte[] encode(CanvasWireMessage message) throws CanvasWireEncodeException {
        Objects.requireNonNull(message, "message");
        validateForEncoding(message);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(512);
        try (JsonGenerator generator = jsonFactory.createGenerator(bytes)) {
            generator.writeStartObject();
            generator.writeStringField("format", CanvasWireProtocol.FORMAT);
            generator.writeNumberField("protocolVersion", CanvasWireProtocol.VERSION);
            generator.writeStringField("sessionId", message.sessionId().toString());
            generator.writeNumberField("sequence", message.sequence());
            generator.writeStringField("type", message.type().wireValue());
            if (message.replyTo().isPresent()) {
                generator.writeNumberField("replyTo", message.replyTo().orElseThrow());
            }
            generator.writeObjectFieldStart("body");
            writeBody(generator, message);
            generator.writeEndObject();
            generator.writeEndObject();
        } catch (IOException | RuntimeException failure) {
            throw new CanvasWireEncodeException(
                    CanvasWireDiagnosticCode.INVALID_VALUE,
                    "Could not encode the Canvas control message.",
                    failure);
        }
        byte[] encoded = bytes.toByteArray();
        if (encoded.length > limits.maxMessageBytes()) {
            throw new CanvasWireEncodeException(
                    CanvasWireDiagnosticCode.MESSAGE_LIMIT,
                    "Canvas control message exceeds the "
                            + limits.maxMessageBytes() + " byte limit.",
                    null);
        }
        return encoded;
    }

    /** Decodes untrusted bytes without allowing content failures to escape. */
    public CanvasWireDecodeResult decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length > limits.maxMessageBytes()) {
            return invalid(
                    CanvasWireDiagnosticCode.MESSAGE_LIMIT,
                    "",
                    "Canvas control message exceeds the configured byte limit.");
        }
        byte[] snapshot = bytes.clone();

        final String json;
        try {
            json = strictUtf8(snapshot);
        } catch (DecodeFailure failure) {
            return new CanvasWireDecodeResult.Invalid(failure.diagnostic());
        }

        try {
            Envelope envelope = scanEnvelope(json);
            if (!CanvasWireProtocol.FORMAT.equals(envelope.format())) {
                return invalid(
                        CanvasWireDiagnosticCode.UNSUPPORTED_FORMAT,
                        "/format",
                        "The Canvas wire format is not supported.");
            }
            if (envelope.protocolVersion() != CanvasWireProtocol.VERSION) {
                return invalid(
                        CanvasWireDiagnosticCode.UNSUPPORTED_VERSION,
                        "/protocolVersion",
                        "The Canvas wire protocol version is not supported.");
            }
            final CanvasWireMessageType type;
            try {
                type = CanvasWireMessageType.parse(envelope.type());
            } catch (IllegalArgumentException failure) {
                return invalid(
                        CanvasWireDiagnosticCode.UNKNOWN_MESSAGE_TYPE,
                        "/type",
                        "Canvas control message type is not supported.");
            }
            validateReplyShape(type, envelope.replySeen());
            CanvasWireMessage message = readTypedMessage(
                    json,
                    new TypedEnvelope(
                            envelope.sessionId(),
                            envelope.sequence(),
                            type,
                            envelope.replyTo()));
            return new CanvasWireDecodeResult.Decoded(message);
        } catch (DecodeFailure failure) {
            return new CanvasWireDecodeResult.Invalid(failure.diagnostic());
        } catch (StreamConstraintsException failure) {
            return invalid(
                    CanvasWireDiagnosticCode.RESOURCE_LIMIT,
                    "",
                    "Canvas control JSON exceeds a configured resource limit.");
        } catch (JsonParseException failure) {
            CanvasWireDiagnosticCode code = failure.getOriginalMessage() != null
                    && failure.getOriginalMessage().contains("Duplicate field")
                    ? CanvasWireDiagnosticCode.DUPLICATE_FIELD
                    : CanvasWireDiagnosticCode.MALFORMED_JSON;
            return invalid(code, "", code == CanvasWireDiagnosticCode.DUPLICATE_FIELD
                    ? "Canvas control JSON contains a duplicate field."
                    : "Canvas control message is not valid JSON.");
        } catch (IOException failure) {
            return invalid(
                    CanvasWireDiagnosticCode.MALFORMED_JSON,
                    "",
                    "Canvas control message is not valid JSON.");
        } catch (IllegalArgumentException failure) {
            return invalid(
                    CanvasWireDiagnosticCode.INVALID_VALUE,
                    "",
                    boundedFailureMessage(failure));
        } catch (RuntimeException failure) {
            return invalid(
                    CanvasWireDiagnosticCode.INVALID_VALUE,
                    "",
                    "Canvas control message does not satisfy the version 1 contract.");
        }
    }

    private static JsonFactory createJsonFactory(CanvasWireLimits limits) {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxDocumentLength(limits.maxMessageBytes())
                .maxNestingDepth(limits.maxJsonNestingDepth())
                .maxTokenCount(limits.maxJsonTokens())
                .maxNameLength(limits.maxFieldNameUtf16Units())
                .maxStringLength(limits.maxStringUtf16Units())
                .maxNumberLength(32)
                .build();
        ErrorReportConfiguration errors = ErrorReportConfiguration.builder()
                .maxRawContentLength(0)
                .maxErrorTokenLength(128)
                .build();
        JsonFactoryBuilder builder = new JsonFactoryBuilder()
                .streamReadConstraints(constraints)
                .errorReportConfiguration(errors)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);
        for (JsonReadFeature feature : JsonReadFeature.values()) {
            builder.disable(feature);
        }
        return builder.build();
    }

    private String strictUtf8(byte[] bytes) throws DecodeFailure {
        if (startsWith(bytes, UTF8_BOM) || hasUtf16OrUtf32Bom(bytes)) {
            throw failure(
                    CanvasWireDiagnosticCode.MALFORMED_UTF8,
                    "",
                    "Canvas control messages must be UTF-8 without a BOM.");
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException failure) {
            throw failure(
                    CanvasWireDiagnosticCode.MALFORMED_UTF8,
                    "",
                    "Canvas control message is not strict UTF-8.");
        }
    }

    private Envelope scanEnvelope(String json) throws IOException, DecodeFailure {
        try (JsonParser parser = jsonFactory.createParser(json)) {
            requireToken(parser.nextToken(), JsonToken.START_OBJECT, parser, "",
                    CanvasWireDiagnosticCode.ROOT_MUST_BE_OBJECT,
                    "Canvas control JSON root must be an object.");
            String format = null;
            Integer version = null;
            CanvasSessionId sessionId = null;
            Long sequence = null;
            String type = null;
            OptionalLong replyTo = OptionalLong.empty();
            boolean replySeen = false;
            boolean bodySeen = false;
            int fields = 0;

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                requireCurrent(parser, JsonToken.FIELD_NAME, "",
                        "Canvas envelope field name is malformed.");
                fields = increment(fields, limits.maxObjectFields(), "",
                        "Canvas envelope has too many fields.");
                String name = checkedText(parser.currentName(), "");
                JsonToken value = parser.nextToken();
                if (value == null) {
                    throw failure(CanvasWireDiagnosticCode.MALFORMED_JSON, "",
                            "Canvas envelope ended before a field value.");
                }
                switch (name) {
                    case "format" -> format = requiredOnce(
                            format, readString(parser, "/format"), "/format");
                    case "protocolVersion" -> version = requiredOnce(
                            version, readInt(parser, "/protocolVersion"),
                            "/protocolVersion");
                    case "sessionId" -> {
                        if (sessionId != null) {
                            throw duplicate("/sessionId");
                        }
                        String text = readString(parser, "/sessionId");
                        try {
                            sessionId = CanvasSessionId.parse(text);
                        } catch (IllegalArgumentException failure) {
                            throw failure(CanvasWireDiagnosticCode.INVALID_VALUE,
                                    "/sessionId",
                                    "Canvas session id is invalid.");
                        }
                    }
                    case "sequence" -> sequence = requiredOnce(
                            sequence, readSequence(parser, "/sequence"), "/sequence");
                    case "type" -> {
                        type = requiredOnce(
                                type, readString(parser, "/type"), "/type");
                    }
                    case "replyTo" -> {
                        if (replySeen) {
                            throw duplicate("/replyTo");
                        }
                        replySeen = true;
                        replyTo = OptionalLong.of(readSequence(parser, "/replyTo"));
                    }
                    case "body" -> {
                        if (bodySeen) {
                            throw duplicate("/body");
                        }
                        bodySeen = true;
                        requireCurrent(parser, JsonToken.START_OBJECT, "/body",
                                "Canvas message body must be an object.");
                        scanContainer(parser, "/body");
                    }
                    default -> throw failure(
                            CanvasWireDiagnosticCode.UNKNOWN_FIELD,
                            "/" + escapePointer(name),
                            "Canvas envelope contains an unknown field.");
                }
            }
            if (parser.nextToken() != null) {
                throw failure(CanvasWireDiagnosticCode.TRAILING_CONTENT, "",
                        "Canvas control message contains trailing content.");
            }
            if (format == null) {
                throw missing("/format");
            }
            if (version == null) {
                throw missing("/protocolVersion");
            }
            if (sessionId == null) {
                throw missing("/sessionId");
            }
            if (sequence == null) {
                throw missing("/sequence");
            }
            if (type == null) {
                throw missing("/type");
            }
            if (!bodySeen) {
                throw missing("/body");
            }
            return new Envelope(
                    format, version, sessionId, sequence, type, replyTo, replySeen);
        }
    }

    private CanvasWireMessage readTypedMessage(String json, TypedEnvelope envelope)
            throws IOException, DecodeFailure {
        try (JsonParser parser = jsonFactory.createParser(json)) {
            parser.nextToken();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String name = parser.currentName();
                parser.nextToken();
                if ("body".equals(name)) {
                    return switch (envelope.type()) {
                        case HOST_HELLO -> readHostHello(parser, envelope);
                        case RUNNER_HELLO -> readRunnerHello(parser, envelope);
                        case HOST_CLOSE -> readHostClose(parser, envelope);
                        case RUNNER_CLOSED -> readRunnerClosed(parser, envelope);
                        case RUNNER_FAILURE -> readRunnerFailure(parser, envelope);
                    };
                }
                parser.skipChildren();
            }
        }
        throw missing("/body");
    }

    private CanvasHostHello readHostHello(JsonParser parser, TypedEnvelope envelope)
            throws IOException, DecodeFailure {
        String hostVersion = null;
        List<CanvasWireCapability> capabilities = null;
        CanvasWireHandshakeLimits offeredLimits = null;
        int fields = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, "/body",
                    "Canvas host hello field name is malformed.");
            fields = increment(fields, limits.maxObjectFields(), "/body",
                    "Canvas host hello has too many fields.");
            String name = parser.currentName();
            parser.nextToken();
            switch (name) {
                case "hostVersion" -> hostVersion = requiredOnce(
                        hostVersion, readVersionText(parser, "/body/hostVersion"),
                        "/body/hostVersion");
                case "requestedCapabilities" -> {
                    if (capabilities != null) {
                        throw duplicate("/body/requestedCapabilities");
                    }
                    capabilities = readCapabilities(parser,
                            "/body/requestedCapabilities");
                }
                case "offeredLimits" -> {
                    if (offeredLimits != null) {
                        throw duplicate("/body/offeredLimits");
                    }
                    offeredLimits = readHandshakeLimits(parser,
                            "/body/offeredLimits");
                }
                default -> throw unknownBody(name);
            }
        }
        return new CanvasHostHello(
                envelope.sessionId(),
                envelope.sequence(),
                require(hostVersion, "/body/hostVersion"),
                require(capabilities, "/body/requestedCapabilities"),
                require(offeredLimits, "/body/offeredLimits"));
    }

    private CanvasRunnerHello readRunnerHello(JsonParser parser, TypedEnvelope envelope)
            throws IOException, DecodeFailure {
        String runnerVersion = null;
        CanvasEngineIdentity engine = null;
        List<CanvasWireCapability> capabilities = null;
        CanvasWireHandshakeLimits effectiveLimits = null;
        int fields = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, "/body",
                    "Canvas runner hello field name is malformed.");
            fields = increment(fields, limits.maxObjectFields(), "/body",
                    "Canvas runner hello has too many fields.");
            String name = parser.currentName();
            parser.nextToken();
            switch (name) {
                case "runnerVersion" -> runnerVersion = requiredOnce(
                        runnerVersion, readVersionText(parser, "/body/runnerVersion"),
                        "/body/runnerVersion");
                case "engine" -> {
                    if (engine != null) {
                        throw duplicate("/body/engine");
                    }
                    engine = readEngine(parser, "/body/engine");
                }
                case "acceptedCapabilities" -> {
                    if (capabilities != null) {
                        throw duplicate("/body/acceptedCapabilities");
                    }
                    capabilities = readCapabilities(parser,
                            "/body/acceptedCapabilities");
                }
                case "effectiveLimits" -> {
                    if (effectiveLimits != null) {
                        throw duplicate("/body/effectiveLimits");
                    }
                    effectiveLimits = readHandshakeLimits(parser,
                            "/body/effectiveLimits");
                }
                default -> throw unknownBody(name);
            }
        }
        return new CanvasRunnerHello(
                envelope.sessionId(),
                envelope.sequence(),
                envelope.replyTo().orElseThrow(),
                require(runnerVersion, "/body/runnerVersion"),
                require(engine, "/body/engine"),
                require(capabilities, "/body/acceptedCapabilities"),
                require(effectiveLimits, "/body/effectiveLimits"));
    }

    private CanvasHostClose readHostClose(JsonParser parser, TypedEnvelope envelope)
            throws IOException, DecodeFailure {
        CanvasWireCloseReason reason = null;
        int fields = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, "/body",
                    "Canvas host close field name is malformed.");
            fields = increment(fields, limits.maxObjectFields(), "/body",
                    "Canvas host close has too many fields.");
            String name = parser.currentName();
            parser.nextToken();
            if (!"reason".equals(name)) {
                throw unknownBody(name);
            }
            if (reason != null) {
                throw duplicate("/body/reason");
            }
            String text = readString(parser, "/body/reason");
            try {
                reason = CanvasWireCloseReason.parse(text);
            } catch (IllegalArgumentException failure) {
                throw failure(CanvasWireDiagnosticCode.INVALID_VALUE,
                        "/body/reason", "Canvas close reason is not supported.");
            }
        }
        return new CanvasHostClose(
                envelope.sessionId(), envelope.sequence(),
                require(reason, "/body/reason"));
    }

    private CanvasRunnerClosed readRunnerClosed(JsonParser parser, TypedEnvelope envelope)
            throws IOException, DecodeFailure {
        if (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.currentToken() == JsonToken.FIELD_NAME
                    ? parser.currentName() : "";
            throw failure(CanvasWireDiagnosticCode.UNKNOWN_FIELD,
                    name.isEmpty() ? "/body" : "/body/" + escapePointer(name),
                    "Canvas runner closed body must be empty.");
        }
        return new CanvasRunnerClosed(
                envelope.sessionId(),
                envelope.sequence(),
                envelope.replyTo().orElseThrow());
    }

    private CanvasRunnerFailure readRunnerFailure(JsonParser parser, TypedEnvelope envelope)
            throws IOException, DecodeFailure {
        CanvasWireFailureCode code = null;
        Boolean fatal = null;
        String message = null;
        int fields = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, "/body",
                    "Canvas runner failure field name is malformed.");
            fields = increment(fields, limits.maxObjectFields(), "/body",
                    "Canvas runner failure has too many fields.");
            String name = parser.currentName();
            parser.nextToken();
            switch (name) {
                case "code" -> {
                    if (code != null) {
                        throw duplicate("/body/code");
                    }
                    String text = readString(parser, "/body/code");
                    try {
                        code = CanvasWireFailureCode.parse(text);
                    } catch (IllegalArgumentException failure) {
                        throw failure(CanvasWireDiagnosticCode.INVALID_VALUE,
                                "/body/code", "Canvas failure code is not supported.");
                    }
                }
                case "fatal" -> fatal = requiredOnce(
                        fatal, readBoolean(parser, "/body/fatal"), "/body/fatal");
                case "message" -> message = requiredOnce(
                        message, readFailureText(parser, "/body/message"),
                        "/body/message");
                default -> throw unknownBody(name);
            }
        }
        return new CanvasRunnerFailure(
                envelope.sessionId(),
                envelope.sequence(),
                envelope.replyTo(),
                require(code, "/body/code"),
                require(fatal, "/body/fatal"),
                require(message, "/body/message"));
    }

    private CanvasEngineIdentity readEngine(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        requireCurrent(parser, JsonToken.START_OBJECT, path,
                "Canvas engine identity must be an object.");
        String flutter = null;
        String framework = null;
        String engine = null;
        String dart = null;
        int fields = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, path,
                    "Canvas engine identity field name is malformed.");
            fields = increment(fields, limits.maxObjectFields(), path,
                    "Canvas engine identity has too many fields.");
            String name = parser.currentName();
            parser.nextToken();
            switch (name) {
                case "flutterVersion" -> flutter = requiredOnce(
                        flutter, readVersionText(parser, path + "/flutterVersion"),
                        path + "/flutterVersion");
                case "frameworkRevision" -> framework = requiredOnce(
                        framework, readVersionText(parser, path + "/frameworkRevision"),
                        path + "/frameworkRevision");
                case "engineRevision" -> engine = requiredOnce(
                        engine, readVersionText(parser, path + "/engineRevision"),
                        path + "/engineRevision");
                case "dartSdkVersion" -> dart = requiredOnce(
                        dart, readVersionText(parser, path + "/dartSdkVersion"),
                        path + "/dartSdkVersion");
                default -> throw failure(CanvasWireDiagnosticCode.UNKNOWN_FIELD,
                        path + "/" + escapePointer(name),
                        "Canvas engine identity contains an unknown field.");
            }
        }
        return new CanvasEngineIdentity(
                require(flutter, path + "/flutterVersion"),
                require(framework, path + "/frameworkRevision"),
                require(engine, path + "/engineRevision"),
                require(dart, path + "/dartSdkVersion"));
    }

    private CanvasWireHandshakeLimits readHandshakeLimits(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        requireCurrent(parser, JsonToken.START_OBJECT, path,
                "Canvas handshake limits must be an object.");
        Integer control = null;
        Integer model = null;
        Integer catalog = null;
        Integer layout = null;
        Integer image = null;
        Integer dimension = null;
        Long pixels = null;
        int fields = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            requireCurrent(parser, JsonToken.FIELD_NAME, path,
                    "Canvas handshake limit field name is malformed.");
            fields = increment(fields, limits.maxObjectFields(), path,
                    "Canvas handshake limits have too many fields.");
            String name = parser.currentName();
            parser.nextToken();
            String fieldPath = path + "/" + escapePointer(name);
            switch (name) {
                case "maxControlMessageBytes" -> control = requiredOnce(
                        control, readPositiveInt(parser, fieldPath), fieldPath);
                case "maxModelBytes" -> model = requiredOnce(
                        model, readPositiveInt(parser, fieldPath), fieldPath);
                case "maxCatalogBytes" -> catalog = requiredOnce(
                        catalog, readPositiveInt(parser, fieldPath), fieldPath);
                case "maxLayoutBytes" -> layout = requiredOnce(
                        layout, readPositiveInt(parser, fieldPath), fieldPath);
                case "maxEncodedImageBytes" -> image = requiredOnce(
                        image, readPositiveInt(parser, fieldPath), fieldPath);
                case "maxPhysicalDimension" -> dimension = requiredOnce(
                        dimension, readPositiveInt(parser, fieldPath), fieldPath);
                case "maxPhysicalPixels" -> pixels = requiredOnce(
                        pixels, readPositiveLong(parser, fieldPath), fieldPath);
                default -> throw failure(CanvasWireDiagnosticCode.UNKNOWN_FIELD,
                        fieldPath, "Canvas handshake limits contain an unknown field.");
            }
        }
        int controlValue = require(control, path + "/maxControlMessageBytes");
        int modelValue = require(model, path + "/maxModelBytes");
        int catalogValue = require(catalog, path + "/maxCatalogBytes");
        int layoutValue = require(layout, path + "/maxLayoutBytes");
        int imageValue = require(image, path + "/maxEncodedImageBytes");
        int dimensionValue = require(dimension, path + "/maxPhysicalDimension");
        long pixelValue = require(pixels, path + "/maxPhysicalPixels");
        checkHandshakeLimit(controlValue,
                CanvasWireHandshakeLimits.MAX_CONTROL_MESSAGE_BYTES,
                path + "/maxControlMessageBytes");
        checkHandshakeLimit(modelValue, CanvasWireHandshakeLimits.MAX_MODEL_BYTES,
                path + "/maxModelBytes");
        checkHandshakeLimit(catalogValue, CanvasWireHandshakeLimits.MAX_CATALOG_BYTES,
                path + "/maxCatalogBytes");
        checkHandshakeLimit(layoutValue, CanvasWireHandshakeLimits.MAX_LAYOUT_BYTES,
                path + "/maxLayoutBytes");
        checkHandshakeLimit(imageValue,
                CanvasWireHandshakeLimits.MAX_ENCODED_IMAGE_BYTES,
                path + "/maxEncodedImageBytes");
        checkHandshakeLimit(dimensionValue,
                CanvasWireHandshakeLimits.MAX_PHYSICAL_DIMENSION,
                path + "/maxPhysicalDimension");
        checkHandshakeLimit(pixelValue,
                CanvasWireHandshakeLimits.MAX_PHYSICAL_PIXELS,
                path + "/maxPhysicalPixels");
        if (controlValue > limits.maxMessageBytes()) {
            throw failure(CanvasWireDiagnosticCode.RESOURCE_LIMIT,
                    path + "/maxControlMessageBytes",
                    "Peer control-message limit exceeds the active codec limit.");
        }
        return new CanvasWireHandshakeLimits(
                controlValue,
                modelValue,
                catalogValue,
                layoutValue,
                imageValue,
                dimensionValue,
                pixelValue);
    }

    private List<CanvasWireCapability> readCapabilities(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        requireCurrent(parser, JsonToken.START_ARRAY, path,
                "Canvas capabilities must be an array.");
        EnumSet<CanvasWireCapability> unique = EnumSet.noneOf(CanvasWireCapability.class);
        int count = 0;
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            count = increment(count, Math.min(
                    limits.maxCapabilities(), limits.maxArrayElements()), path,
                    "Canvas capability list exceeds the configured limit.");
            String value = readString(parser, path + "/" + (count - 1));
            checkCodePoints(value, path + "/" + (count - 1),
                    limits.maxCapabilityCodePoints(),
                    "Canvas capability name exceeds the configured limit.");
            final CanvasWireCapability capability;
            try {
                capability = CanvasWireCapability.parse(value);
            } catch (IllegalArgumentException failure) {
                throw failure(CanvasWireDiagnosticCode.INVALID_VALUE,
                        path + "/" + (count - 1),
                        "Canvas capability is not supported by protocol version 1.");
            }
            if (!unique.add(capability)) {
                throw failure(CanvasWireDiagnosticCode.INVALID_VALUE,
                        path + "/" + (count - 1),
                        "Canvas capability list contains a duplicate value.");
            }
        }
        return List.copyOf(new ArrayList<>(unique));
    }

    private void scanContainer(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        Deque<ContainerFrame> stack = new ArrayDeque<>();
        stack.push(new ContainerFrame(true));
        while (!stack.isEmpty()) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                throw failure(CanvasWireDiagnosticCode.MALFORMED_JSON, path,
                        "Canvas control JSON ended inside a container.");
            }
            ContainerFrame current = stack.peek();
            if (token == JsonToken.FIELD_NAME) {
                if (!current.object()) {
                    throw failure(CanvasWireDiagnosticCode.MALFORMED_JSON, path,
                            "Canvas control JSON container is malformed.");
                }
                current.increment(limits.maxObjectFields(), path,
                        "Canvas control JSON object has too many fields.");
                checkedText(parser.currentName(), path);
                continue;
            }
            if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                if ((token == JsonToken.END_OBJECT) != current.object()) {
                    throw failure(CanvasWireDiagnosticCode.MALFORMED_JSON, path,
                            "Canvas control JSON container is malformed.");
                }
                stack.pop();
                continue;
            }
            if (!current.object()) {
                current.increment(limits.maxArrayElements(), path,
                        "Canvas control JSON array has too many elements.");
            }
            if (token == JsonToken.VALUE_STRING) {
                checkedText(parser.getText(), path);
            } else if (token == JsonToken.START_OBJECT) {
                stack.push(new ContainerFrame(true));
            } else if (token == JsonToken.START_ARRAY) {
                stack.push(new ContainerFrame(false));
            }
        }
    }

    private void writeBody(JsonGenerator generator, CanvasWireMessage message)
            throws IOException {
        switch (message) {
            case CanvasHostHello hello -> {
                generator.writeStringField("hostVersion", hello.hostVersion());
                writeCapabilities(generator, "requestedCapabilities",
                        hello.requestedCapabilities());
                writeHandshakeLimits(generator, "offeredLimits", hello.offeredLimits());
            }
            case CanvasRunnerHello hello -> {
                generator.writeStringField("runnerVersion", hello.runnerVersion());
                writeEngine(generator, hello.engineIdentity());
                writeCapabilities(generator, "acceptedCapabilities",
                        hello.acceptedCapabilities());
                writeHandshakeLimits(generator, "effectiveLimits", hello.effectiveLimits());
            }
            case CanvasHostClose close ->
                generator.writeStringField("reason", close.reason().wireValue());
            case CanvasRunnerClosed ignored -> {
                // Empty body is canonical for the acknowledgement.
            }
            case CanvasRunnerFailure failure -> {
                generator.writeStringField("code", failure.code().wireValue());
                generator.writeBooleanField("fatal", failure.fatal());
                generator.writeStringField("message", failure.message());
            }
        }
    }

    private void validateForEncoding(CanvasWireMessage message)
            throws CanvasWireEncodeException {
        try {
            checkEncodedText(message.sessionId().toString(), "sessionId");
            switch (message) {
                case CanvasHostHello hello -> {
                    checkEncodedText(hello.hostVersion(), "hostVersion");
                    checkCapabilitiesForEncoding(hello.requestedCapabilities());
                    checkHandshakeForEncoding(hello.offeredLimits());
                }
                case CanvasRunnerHello hello -> {
                    checkEncodedText(hello.runnerVersion(), "runnerVersion");
                    checkEncodedText(hello.engineIdentity().flutterVersion(),
                            "flutterVersion");
                    checkEncodedText(hello.engineIdentity().frameworkRevision(),
                            "frameworkRevision");
                    checkEncodedText(hello.engineIdentity().engineRevision(),
                            "engineRevision");
                    checkEncodedText(hello.engineIdentity().dartSdkVersion(),
                            "dartSdkVersion");
                    checkCapabilitiesForEncoding(hello.acceptedCapabilities());
                    checkHandshakeForEncoding(hello.effectiveLimits());
                }
                case CanvasRunnerFailure failure -> {
                    checkEncodedText(failure.message(), "message");
                    if (failure.message().codePointCount(
                            0, failure.message().length())
                            > limits.maxFailureMessageCodePoints()) {
                        throw new IllegalArgumentException(
                                "message exceeds the active failure-message limit");
                    }
                }
                case CanvasHostClose ignored -> {
                }
                case CanvasRunnerClosed ignored -> {
                }
            }
        } catch (IllegalArgumentException failure) {
            throw new CanvasWireEncodeException(
                    CanvasWireDiagnosticCode.RESOURCE_LIMIT,
                    "Canvas control message exceeds the active codec policy.",
                    failure);
        }
    }

    private void checkEncodedText(String value, String name) {
        CanvasWireValues.requireWellFormedUnicode(value, name);
        if (value.length() > limits.maxStringUtf16Units()
                || value.codePointCount(0, value.length())
                        > limits.maxStringCodePoints()) {
            throw new IllegalArgumentException(name + " exceeds the active string limit");
        }
    }

    private void checkCapabilitiesForEncoding(
            List<CanvasWireCapability> capabilities) {
        if (capabilities.size() > limits.maxCapabilities()
                || capabilities.size() > limits.maxArrayElements()) {
            throw new IllegalArgumentException(
                    "capabilities exceed the active collection limit");
        }
        for (CanvasWireCapability capability : capabilities) {
            checkEncodedText(capability.wireValue(), "capability");
            if (capability.wireValue().codePointCount(
                    0, capability.wireValue().length())
                    > limits.maxCapabilityCodePoints()) {
                throw new IllegalArgumentException(
                        "capability exceeds the active capability limit");
            }
        }
    }

    private void checkHandshakeForEncoding(CanvasWireHandshakeLimits handshake) {
        if (handshake.maxControlMessageBytes() > limits.maxMessageBytes()) {
            throw new IllegalArgumentException(
                    "peer control-message limit exceeds the active codec limit");
        }
    }

    private static void writeCapabilities(
            JsonGenerator generator,
            String field,
            List<CanvasWireCapability> capabilities) throws IOException {
        generator.writeArrayFieldStart(field);
        for (CanvasWireCapability capability : capabilities) {
            generator.writeString(capability.wireValue());
        }
        generator.writeEndArray();
    }

    private static void writeEngine(
            JsonGenerator generator,
            CanvasEngineIdentity engine) throws IOException {
        generator.writeObjectFieldStart("engine");
        generator.writeStringField("flutterVersion", engine.flutterVersion());
        generator.writeStringField("frameworkRevision", engine.frameworkRevision());
        generator.writeStringField("engineRevision", engine.engineRevision());
        generator.writeStringField("dartSdkVersion", engine.dartSdkVersion());
        generator.writeEndObject();
    }

    private static void writeHandshakeLimits(
            JsonGenerator generator,
            String field,
            CanvasWireHandshakeLimits value) throws IOException {
        generator.writeObjectFieldStart(field);
        generator.writeNumberField("maxControlMessageBytes", value.maxControlMessageBytes());
        generator.writeNumberField("maxModelBytes", value.maxModelBytes());
        generator.writeNumberField("maxCatalogBytes", value.maxCatalogBytes());
        generator.writeNumberField("maxLayoutBytes", value.maxLayoutBytes());
        generator.writeNumberField("maxEncodedImageBytes", value.maxEncodedImageBytes());
        generator.writeNumberField("maxPhysicalDimension", value.maxPhysicalDimension());
        generator.writeNumberField("maxPhysicalPixels", value.maxPhysicalPixels());
        generator.writeEndObject();
    }

    private String readVersionText(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        String value = readString(parser, path);
        try {
            return CanvasWireValues.printableText(
                    value, "version", CanvasWireValues.MAX_VERSION_CODE_POINTS);
        } catch (IllegalArgumentException failure) {
            throw failure(CanvasWireDiagnosticCode.INVALID_VALUE, path,
                    "Canvas version text is invalid.");
        }
    }

    private String readFailureText(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        String value = readString(parser, path);
        try {
            return CanvasWireValues.printableText(
                    value, "failure message", limits.maxFailureMessageCodePoints());
        } catch (IllegalArgumentException failure) {
            throw failure(CanvasWireDiagnosticCode.INVALID_VALUE, path,
                    "Canvas failure message is invalid.");
        }
    }

    private String readString(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        requireCurrent(parser, JsonToken.VALUE_STRING, path,
                "Canvas control field must be a string.");
        return checkedText(parser.getText(), path);
    }

    private String checkedText(String value, String path) throws DecodeFailure {
        checkCodePoints(value, path, limits.maxStringCodePoints(),
                "Canvas control string exceeds the configured code-point limit.");
        return value;
    }

    private int readInt(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        BigInteger value = readInteger(parser, path);
        if (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0
                || value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw failure(CanvasWireDiagnosticCode.INVALID_VALUE, path,
                    "Canvas control integer is outside the supported range.");
        }
        return value.intValue();
    }

    private int readPositiveInt(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        int value = readInt(parser, path);
        if (value <= 0) {
            throw failure(CanvasWireDiagnosticCode.INVALID_VALUE, path,
                    "Canvas handshake limit must be positive.");
        }
        return value;
    }

    private long readPositiveLong(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        BigInteger value = readInteger(parser, path);
        if (value.signum() <= 0
                || value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            throw failure(CanvasWireDiagnosticCode.INVALID_VALUE, path,
                    "Canvas handshake limit is outside the supported range.");
        }
        return value.longValue();
    }

    private long readSequence(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        BigInteger value = readInteger(parser, path);
        if (value.signum() < 0
                || value.compareTo(BigInteger.valueOf(
                        CanvasWireProtocol.MAX_SEQUENCE)) > 0) {
            throw failure(CanvasWireDiagnosticCode.INVALID_VALUE, path,
                    "Canvas message sequence is outside the supported range.");
        }
        return value.longValue();
    }

    private BigInteger readInteger(JsonParser parser, String path)
            throws IOException, DecodeFailure {
        requireCurrent(parser, JsonToken.VALUE_NUMBER_INT, path,
                "Canvas control field must be an exact integer.");
        return parser.getBigIntegerValue();
    }

    private boolean readBoolean(JsonParser parser, String path)
            throws DecodeFailure {
        if (parser.currentToken() == JsonToken.VALUE_TRUE) {
            return true;
        }
        if (parser.currentToken() == JsonToken.VALUE_FALSE) {
            return false;
        }
        throw failure(CanvasWireDiagnosticCode.WRONG_VALUE_TYPE, path,
                "Canvas control field must be a boolean.");
    }

    private static void validateReplyShape(
            CanvasWireMessageType type,
            boolean replySeen) throws DecodeFailure {
        boolean required = type == CanvasWireMessageType.RUNNER_HELLO
                || type == CanvasWireMessageType.RUNNER_CLOSED;
        boolean forbidden = type == CanvasWireMessageType.HOST_HELLO
                || type == CanvasWireMessageType.HOST_CLOSE;
        if (required && !replySeen) {
            throw missing("/replyTo");
        }
        if (forbidden && replySeen) {
            throw failure(CanvasWireDiagnosticCode.INVALID_VALUE, "/replyTo",
                    "This Canvas message type must not contain replyTo.");
        }
    }

    private static <T> T requiredOnce(T current, T value, String path)
            throws DecodeFailure {
        if (current != null) {
            throw duplicate(path);
        }
        return value;
    }

    private static <T> T require(T value, String path) throws DecodeFailure {
        if (value == null) {
            throw missing(path);
        }
        return value;
    }

    private static void checkHandshakeLimit(
            long value,
            long maximum,
            String path) throws DecodeFailure {
        if (value > maximum) {
            throw failure(CanvasWireDiagnosticCode.RESOURCE_LIMIT, path,
                    "Canvas handshake limit exceeds the supported maximum.");
        }
    }

    private static int increment(int value, int maximum, String path, String message)
            throws DecodeFailure {
        if (value >= maximum) {
            throw failure(CanvasWireDiagnosticCode.RESOURCE_LIMIT, path, message);
        }
        return value + 1;
    }

    private static void requireCurrent(
            JsonParser parser,
            JsonToken expected,
            String path,
            String message) throws DecodeFailure {
        requireToken(parser.currentToken(), expected, parser, path,
                CanvasWireDiagnosticCode.WRONG_VALUE_TYPE, message);
    }

    private static void requireToken(
            JsonToken actual,
            JsonToken expected,
            JsonParser parser,
            String path,
            CanvasWireDiagnosticCode code,
            String message) throws DecodeFailure {
        if (actual != expected) {
            throw failure(code, path, message);
        }
    }

    private static DecodeFailure unknownBody(String name) {
        return failure(CanvasWireDiagnosticCode.UNKNOWN_FIELD,
                "/body/" + escapePointer(name),
                "Canvas message body contains an unknown field.");
    }

    private static DecodeFailure missing(String path) {
        return failure(CanvasWireDiagnosticCode.MISSING_REQUIRED_FIELD, path,
                "Canvas control message is missing a required field.");
    }

    private static DecodeFailure duplicate(String path) {
        return failure(CanvasWireDiagnosticCode.DUPLICATE_FIELD, path,
                "Canvas control message contains a duplicate field.");
    }

    private static DecodeFailure failure(
            CanvasWireDiagnosticCode code,
            String path,
            String message) {
        return new DecodeFailure(new CanvasWireDiagnostic(code, path, message));
    }

    private static CanvasWireDecodeResult.Invalid invalid(
            CanvasWireDiagnosticCode code,
            String path,
            String message) {
        return new CanvasWireDecodeResult.Invalid(
                new CanvasWireDiagnostic(code, path, message));
    }

    private static String boundedFailureMessage(IllegalArgumentException failure) {
        return "Canvas control message contains an invalid value.";
    }

    private static void checkCodePoints(
            String value,
            String path,
            int maximum,
            String message) throws DecodeFailure {
        if (value.codePointCount(0, value.length()) > maximum) {
            throw failure(CanvasWireDiagnosticCode.RESOURCE_LIMIT, path, message);
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw failure(CanvasWireDiagnosticCode.INVALID_VALUE, path,
                            "Canvas control string contains an unpaired surrogate.");
                }
                index++;
            } else if (Character.isLowSurrogate(current)) {
                throw failure(CanvasWireDiagnosticCode.INVALID_VALUE, path,
                        "Canvas control string contains an unpaired surrogate.");
            }
        }
    }

    private static boolean hasUtf16OrUtf32Bom(byte[] bytes) {
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

    private static String escapePointer(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if ((codePoint >= 'a' && codePoint <= 'z')
                    || (codePoint >= 'A' && codePoint <= 'Z')
                    || (codePoint >= '0' && codePoint <= '9')
                    || codePoint == '.'
                    || codePoint == '_'
                    || codePoint == '-') {
                escaped.appendCodePoint(codePoint);
            } else if (codePoint == '~') {
                escaped.append("~0");
            } else if (codePoint == '/') {
                escaped.append("~1");
            } else {
                escaped.append("~u")
                        .append(Integer.toHexString(codePoint).toUpperCase(
                                java.util.Locale.ROOT));
            }
        });
        return escaped.toString();
    }

    private record Envelope(
            String format,
            int protocolVersion,
            CanvasSessionId sessionId,
            long sequence,
            String type,
            OptionalLong replyTo,
            boolean replySeen) {
    }

    private record TypedEnvelope(
            CanvasSessionId sessionId,
            long sequence,
            CanvasWireMessageType type,
            OptionalLong replyTo) {
    }

    private static final class ContainerFrame {
        private final boolean object;
        private int entries;

        private ContainerFrame(boolean object) {
            this.object = object;
        }

        boolean object() {
            return object;
        }

        void increment(int maximum, String path, String message)
                throws DecodeFailure {
            entries = CanvasWireCodec.increment(entries, maximum, path, message);
        }
    }

    private static final class DecodeFailure extends Exception {
        private final CanvasWireDiagnostic diagnostic;

        private DecodeFailure(CanvasWireDiagnostic diagnostic) {
            this.diagnostic = diagnostic;
        }

        CanvasWireDiagnostic diagnostic() {
            return diagnostic;
        }
    }
}
