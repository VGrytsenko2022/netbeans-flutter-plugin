package io.github.vgrytsenko2022.designer.codec;

/**
 * Immutable resource limits applied independently by the Flutter Designer
 * reader and writer. These limits are a security policy and can intentionally
 * be stricter than the JSON schema.
 */
public record FdCodecLimits(
        int maxDocumentBytes,
        int maxJsonNestingDepth,
        long maxJsonTokens,
        int maxFieldNameUtf16Units,
        int maxStringUtf16Units,
        int maxStringCodePoints,
        int maxNumberCharacters,
        int maxAbsoluteDecimalScale,
        int maxWidgetDepth,
        int maxWidgetNodes,
        int maxPropertiesPerWidget,
        int maxSlotsPerWidget,
        int maxListChildren,
        int maxExtensionKeysPerBag,
        int maxExtensionNestingDepth,
        int maxExtensionValues,
        int maxJsonObjectFields,
        int maxJsonArrayElements,
        int maxDiagnostics) {

    public static final int DEFAULT_MAX_DOCUMENT_BYTES = 16 * 1024 * 1024;
    public static final int DEFAULT_MAX_JSON_NESTING_DEPTH = 2_048;
    public static final long DEFAULT_MAX_JSON_TOKENS = 1_000_000L;
    public static final int DEFAULT_MAX_FIELD_NAME_UTF16_UNITS = 256;
    public static final int DEFAULT_MAX_STRING_UTF16_UNITS = 131_072;
    public static final int DEFAULT_MAX_STRING_CODE_POINTS = 65_536;
    public static final int DEFAULT_MAX_NUMBER_CHARACTERS = 128;
    public static final int DEFAULT_MAX_ABSOLUTE_DECIMAL_SCALE = 10_000;
    public static final int DEFAULT_MAX_WIDGET_DEPTH = 256;
    public static final int DEFAULT_MAX_WIDGET_NODES = 10_000;
    public static final int DEFAULT_MAX_PROPERTIES_PER_WIDGET = 1024;
    public static final int DEFAULT_MAX_SLOTS_PER_WIDGET = 128;
    public static final int DEFAULT_MAX_LIST_CHILDREN = 10_000;
    public static final int DEFAULT_MAX_EXTENSION_KEYS_PER_BAG = 128;
    public static final int DEFAULT_MAX_EXTENSION_NESTING_DEPTH = 64;
    public static final int DEFAULT_MAX_EXTENSION_VALUES = 100_000;
    public static final int DEFAULT_MAX_JSON_OBJECT_FIELDS = 1_024;
    public static final int DEFAULT_MAX_JSON_ARRAY_ELEMENTS = 10_000;
    public static final int DEFAULT_MAX_DIAGNOSTICS = 100;

    public FdCodecLimits {
        positive(maxDocumentBytes, "maxDocumentBytes");
        positive(maxJsonNestingDepth, "maxJsonNestingDepth");
        positive(maxJsonTokens, "maxJsonTokens");
        positive(maxFieldNameUtf16Units, "maxFieldNameUtf16Units");
        positive(maxStringUtf16Units, "maxStringUtf16Units");
        positive(maxStringCodePoints, "maxStringCodePoints");
        positive(maxNumberCharacters, "maxNumberCharacters");
        positive(maxAbsoluteDecimalScale, "maxAbsoluteDecimalScale");
        positive(maxWidgetDepth, "maxWidgetDepth");
        positive(maxWidgetNodes, "maxWidgetNodes");
        positive(maxPropertiesPerWidget, "maxPropertiesPerWidget");
        positive(maxSlotsPerWidget, "maxSlotsPerWidget");
        positive(maxListChildren, "maxListChildren");
        positive(maxExtensionKeysPerBag, "maxExtensionKeysPerBag");
        positive(maxExtensionNestingDepth, "maxExtensionNestingDepth");
        positive(maxExtensionValues, "maxExtensionValues");
        positive(maxJsonObjectFields, "maxJsonObjectFields");
        positive(maxJsonArrayElements, "maxJsonArrayElements");
        positive(maxDiagnostics, "maxDiagnostics");
        if (maxStringCodePoints > maxStringUtf16Units) {
            throw new IllegalArgumentException(
                    "maxStringCodePoints cannot exceed maxStringUtf16Units");
        }
        if (maxWidgetDepth > maxJsonNestingDepth) {
            throw new IllegalArgumentException(
                    "maxWidgetDepth cannot exceed maxJsonNestingDepth");
        }
        if (maxWidgetDepth > DEFAULT_MAX_WIDGET_DEPTH) {
            throw new IllegalArgumentException(
                    "maxWidgetDepth cannot exceed the safe supported maximum "
                            + DEFAULT_MAX_WIDGET_DEPTH);
        }
        if (maxExtensionNestingDepth > maxJsonNestingDepth) {
            throw new IllegalArgumentException(
                    "maxExtensionNestingDepth cannot exceed maxJsonNestingDepth");
        }
        if (maxExtensionNestingDepth > DEFAULT_MAX_EXTENSION_NESTING_DEPTH) {
            throw new IllegalArgumentException(
                    "maxExtensionNestingDepth cannot exceed the safe supported maximum "
                            + DEFAULT_MAX_EXTENSION_NESTING_DEPTH);
        }
        if (maxListChildren > DEFAULT_MAX_LIST_CHILDREN) {
            throw new IllegalArgumentException(
                    "maxListChildren cannot exceed the supported schema maximum "
                            + DEFAULT_MAX_LIST_CHILDREN);
        }
    }

    public static FdCodecLimits defaults() {
        return new FdCodecLimits(
                DEFAULT_MAX_DOCUMENT_BYTES,
                DEFAULT_MAX_JSON_NESTING_DEPTH,
                DEFAULT_MAX_JSON_TOKENS,
                DEFAULT_MAX_FIELD_NAME_UTF16_UNITS,
                DEFAULT_MAX_STRING_UTF16_UNITS,
                DEFAULT_MAX_STRING_CODE_POINTS,
                DEFAULT_MAX_NUMBER_CHARACTERS,
                DEFAULT_MAX_ABSOLUTE_DECIMAL_SCALE,
                DEFAULT_MAX_WIDGET_DEPTH,
                DEFAULT_MAX_WIDGET_NODES,
                DEFAULT_MAX_PROPERTIES_PER_WIDGET,
                DEFAULT_MAX_SLOTS_PER_WIDGET,
                DEFAULT_MAX_LIST_CHILDREN,
                DEFAULT_MAX_EXTENSION_KEYS_PER_BAG,
                DEFAULT_MAX_EXTENSION_NESTING_DEPTH,
                DEFAULT_MAX_EXTENSION_VALUES,
                DEFAULT_MAX_JSON_OBJECT_FIELDS,
                DEFAULT_MAX_JSON_ARRAY_ELEMENTS,
                DEFAULT_MAX_DIAGNOSTICS);
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
}
