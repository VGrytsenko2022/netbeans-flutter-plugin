package dev.flutter.netbeans.designer.generation;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Generator-owned expression range that requires analyzer proof of a closed
 * requested Dart type before the candidate can be saved.
 *
 * <p>Offsets use Dart/Java UTF-16 code units and are relative to the owning
 * managed-region payload. The expected type deliberately uses the catalog's
 * closed simple or single-generic spelling; the analyzer boundary qualifies
 * it through a proof-only import rather than trusting ambient identifiers.
 * Reviewed generic widgets may additionally provide a closed generator-owned source type identity,
 * whose separately manifested symbol provenance and strict generic constraints
 * are verified without accepting arbitrary type expressions. NotificationListener
 * also requires a separate non-nullable Notification subtype bound, including when
 * no callback is emitted; Radio retains its existing unbounded type contract.</p>
 */
public record GeneratedDartStaticTypeRequirement(
        int expressionOffset,
        int expressionLength,
        String expectedDartType,
        Optional<String> sourceTypeOverride,
        Optional<String> sourceTypeBound) {

    private static final Pattern EXPECTED_TYPE = Pattern.compile(
            "(?:[A-Za-z][A-Za-z0-9_]*(?:<[A-Za-z][A-Za-z0-9_]*\\??>)?|(?:Object|bool|int|double|num|String|FocusNode|AnimationStyle|Duration|Curve|ShapeBorder|IconThemeData|TextStyle|TextHeightBehavior|BorderRadius|SystemUiOverlayStyle|AlignmentGeometry|Color|Decoration|BoxConstraints|Matrix4|EdgeInsetsGeometry)\\?)");
    private static final Pattern SOURCE_TYPE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?\\??");

    public GeneratedDartStaticTypeRequirement(int expressionOffset, int expressionLength, String expectedDartType) {
        this(expressionOffset, expressionLength, expectedDartType, Optional.empty());
    }

    public GeneratedDartStaticTypeRequirement(int expressionOffset, int expressionLength, String expectedDartType,
            Optional<String> sourceTypeOverride) {
        this(expressionOffset, expressionLength, expectedDartType, sourceTypeOverride, Optional.empty());
    }

    public GeneratedDartStaticTypeRequirement {
        if (expressionOffset < 0 || expressionLength <= 0) {
            throw new IllegalArgumentException(
                    "static-type expression offset must be non-negative and length positive");
        }
        expectedDartType = Objects.requireNonNull(
                expectedDartType, "expectedDartType");
        if (expectedDartType.length() > 128
                || !EXPECTED_TYPE.matcher(expectedDartType).matches()
                && !dev.flutter.netbeans.designer.catalog.ScaffoldWidgetPropertySchema.BOTTOM_SHEET_SCRIM_BUILDER_TYPE.equals(expectedDartType)
                && !java.util.Set.of(dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema.INPUT_COUNTER_BUILDER_TYPE,
                        dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema.CONTEXT_MENU_BUILDER_TYPE,
                        dev.flutter.netbeans.designer.catalog.ListViewWidgetPropertySchema.ITEM_EXTENT_BUILDER_TYPE, "ChildIndexGetter?", "VoidCallback?", "ValueNotifier<EdgeInsets>?", "ImageErrorWidgetBuilder?", "Image?", "Rect?", "Animation<double>?").contains(expectedDartType)) {
            throw new IllegalArgumentException(
                    "expected Dart type must use the closed simple/generic form");
        }
        Objects.requireNonNull(sourceTypeOverride, "sourceTypeOverride");
        if (sourceTypeOverride.isPresent() && (sourceTypeOverride.orElseThrow().length() > 256
                || !SOURCE_TYPE.matcher(sourceTypeOverride.orElseThrow()).matches()
                || !java.util.Set.of("Type", "Object", "Object?", "ValueChanged<Object?>", "RadioGroupRegistry<Object>", "Tween<Object>", "ValueListenable<Object>", "ValueWidgetBuilder<Object>",
                        "NotificationListenerCallback<Notification>").contains(expectedDartType))) {
            throw new IllegalArgumentException("Source type override must be a reviewed closed generated generic type identity");
        }
        Objects.requireNonNull(sourceTypeBound, "sourceTypeBound");
        if (sourceTypeBound.isPresent() && (!sourceTypeBound.orElseThrow().equals("Notification")
                || !expectedDartType.equals("Type") || sourceTypeOverride.isEmpty()
                || sourceTypeOverride.orElseThrow().endsWith("?"))) {
            throw new IllegalArgumentException("Only a non-nullable generated Notification type identity may require the Notification bound");
        }
        if (expectedDartType.equals("NotificationListenerCallback<Notification>") && sourceTypeOverride.isPresent()
                && sourceTypeOverride.orElseThrow().endsWith("?")) {
            throw new IllegalArgumentException("NotificationListener callback requires a non-nullable Notification subtype");
        }
    }

    public int expressionEndOffset() {
        return Math.addExact(expressionOffset, expressionLength);
    }

    public GeneratedDartStaticTypeRequirement shifted(int delta) {
        if (delta < 0) {
            throw new IllegalArgumentException(
                    "static-type expression shift must not be negative");
        }
        return new GeneratedDartStaticTypeRequirement(
                Math.addExact(expressionOffset, delta),
                expressionLength,
                expectedDartType, sourceTypeOverride, sourceTypeBound);
    }
}
