package io.github.vgrytsenko2022.designer.canvas.protocol;

/**
 * Immutable security policy for one JSON control-message body.
 *
 * <p>Callers may tighten these limits but cannot relax them past the reviewed
 * safe caps. Large model, catalog, layout and image payloads are deliberately
 * outside this control-message codec.</p>
 */
public record CanvasWireLimits(
        int maxMessageBytes,
        int maxJsonNestingDepth,
        long maxJsonTokens,
        int maxFieldNameUtf16Units,
        int maxStringUtf16Units,
        int maxStringCodePoints,
        int maxObjectFields,
        int maxArrayElements,
        int maxCapabilities,
        int maxCapabilityCodePoints,
        int maxFailureMessageCodePoints) {

    public static final int DEFAULT_MAX_MESSAGE_BYTES = 256 * 1024;
    public static final int DEFAULT_MAX_JSON_NESTING_DEPTH = 32;
    public static final long DEFAULT_MAX_JSON_TOKENS = 16_384L;
    public static final int DEFAULT_MAX_FIELD_NAME_UTF16_UNITS = 128;
    public static final int DEFAULT_MAX_STRING_UTF16_UNITS = 65_536;
    public static final int DEFAULT_MAX_STRING_CODE_POINTS = 32_768;
    public static final int DEFAULT_MAX_OBJECT_FIELDS = 64;
    public static final int DEFAULT_MAX_ARRAY_ELEMENTS = 64;
    public static final int DEFAULT_MAX_CAPABILITIES = 32;
    public static final int DEFAULT_MAX_CAPABILITY_CODE_POINTS = 64;
    public static final int DEFAULT_MAX_FAILURE_MESSAGE_CODE_POINTS = 4_096;
    private static final int MIN_JSON_NESTING_DEPTH = 3;
    private static final long MIN_JSON_TOKENS = 96L;
    private static final int MIN_FIELD_NAME_UTF16_UNITS = 22;
    private static final int MIN_OBJECT_FIELDS = 7;

    public CanvasWireLimits {
        positive(maxMessageBytes, "maxMessageBytes");
        positive(maxJsonNestingDepth, "maxJsonNestingDepth");
        positive(maxJsonTokens, "maxJsonTokens");
        positive(maxFieldNameUtf16Units, "maxFieldNameUtf16Units");
        positive(maxStringUtf16Units, "maxStringUtf16Units");
        positive(maxStringCodePoints, "maxStringCodePoints");
        positive(maxObjectFields, "maxObjectFields");
        positive(maxArrayElements, "maxArrayElements");
        positive(maxCapabilities, "maxCapabilities");
        positive(maxCapabilityCodePoints, "maxCapabilityCodePoints");
        positive(maxFailureMessageCodePoints, "maxFailureMessageCodePoints");
        notLess(maxJsonNestingDepth, MIN_JSON_NESTING_DEPTH,
                "maxJsonNestingDepth");
        notLess(maxJsonTokens, MIN_JSON_TOKENS, "maxJsonTokens");
        notLess(maxFieldNameUtf16Units, MIN_FIELD_NAME_UTF16_UNITS,
                "maxFieldNameUtf16Units");
        notLess(maxObjectFields, MIN_OBJECT_FIELDS, "maxObjectFields");
        notGreater(maxMessageBytes, DEFAULT_MAX_MESSAGE_BYTES, "maxMessageBytes");
        notGreater(maxJsonNestingDepth, DEFAULT_MAX_JSON_NESTING_DEPTH,
                "maxJsonNestingDepth");
        notGreater(maxJsonTokens, DEFAULT_MAX_JSON_TOKENS, "maxJsonTokens");
        notGreater(maxFieldNameUtf16Units, DEFAULT_MAX_FIELD_NAME_UTF16_UNITS,
                "maxFieldNameUtf16Units");
        notGreater(maxStringUtf16Units, DEFAULT_MAX_STRING_UTF16_UNITS,
                "maxStringUtf16Units");
        notGreater(maxStringCodePoints, DEFAULT_MAX_STRING_CODE_POINTS,
                "maxStringCodePoints");
        notGreater(maxObjectFields, DEFAULT_MAX_OBJECT_FIELDS,
                "maxObjectFields");
        notGreater(maxArrayElements, DEFAULT_MAX_ARRAY_ELEMENTS,
                "maxArrayElements");
        notGreater(maxCapabilities, DEFAULT_MAX_CAPABILITIES,
                "maxCapabilities");
        notGreater(maxCapabilityCodePoints, DEFAULT_MAX_CAPABILITY_CODE_POINTS,
                "maxCapabilityCodePoints");
        notGreater(maxFailureMessageCodePoints,
                DEFAULT_MAX_FAILURE_MESSAGE_CODE_POINTS,
                "maxFailureMessageCodePoints");
        if (maxStringCodePoints > maxStringUtf16Units) {
            throw new IllegalArgumentException(
                    "maxStringCodePoints cannot exceed maxStringUtf16Units");
        }
        if (maxCapabilities > maxArrayElements) {
            throw new IllegalArgumentException(
                    "maxCapabilities cannot exceed maxArrayElements");
        }
        if (maxCapabilityCodePoints > maxStringCodePoints) {
            throw new IllegalArgumentException(
                    "maxCapabilityCodePoints cannot exceed maxStringCodePoints");
        }
        if (maxFailureMessageCodePoints > maxStringCodePoints) {
            throw new IllegalArgumentException(
                    "maxFailureMessageCodePoints cannot exceed maxStringCodePoints");
        }
    }

    public static CanvasWireLimits defaults() {
        return new CanvasWireLimits(
                DEFAULT_MAX_MESSAGE_BYTES,
                DEFAULT_MAX_JSON_NESTING_DEPTH,
                DEFAULT_MAX_JSON_TOKENS,
                DEFAULT_MAX_FIELD_NAME_UTF16_UNITS,
                DEFAULT_MAX_STRING_UTF16_UNITS,
                DEFAULT_MAX_STRING_CODE_POINTS,
                DEFAULT_MAX_OBJECT_FIELDS,
                DEFAULT_MAX_ARRAY_ELEMENTS,
                DEFAULT_MAX_CAPABILITIES,
                DEFAULT_MAX_CAPABILITY_CODE_POINTS,
                DEFAULT_MAX_FAILURE_MESSAGE_CODE_POINTS);
    }

    private static void positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }

    private static void positive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }

    private static void notGreater(int value, int maximum, String name) {
        if (value > maximum) {
            throw new IllegalArgumentException(
                    name + " cannot exceed the safe supported maximum " + maximum);
        }
    }

    private static void notGreater(long value, long maximum, String name) {
        if (value > maximum) {
            throw new IllegalArgumentException(
                    name + " cannot exceed the safe supported maximum " + maximum);
        }
    }

    private static void notLess(int value, int minimum, String name) {
        if (value < minimum) {
            throw new IllegalArgumentException(
                    name + " cannot be below the protocol minimum " + minimum);
        }
    }

    private static void notLess(long value, long minimum, String name) {
        if (value < minimum) {
            throw new IllegalArgumentException(
                    name + " cannot be below the protocol minimum " + minimum);
        }
    }
}
