package dev.flutter.netbeans.dart;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Exact, bounded analyzer-only proof request for one generated expression.
 *
 * <p>A separate analyzer context constructs a second in-memory overlay with
 * proof-owned strict-casts options and assigns the exact expression to the
 * exact expected type. The typed initializer preserves downward
 * inference for generic constructor and factory invocations. In conjunction
 * with original call-site analysis, it rejects {@code dynamic} and wrong generic
 * instantiations. Legacy non-null requirements also reject nullable outer types
 * and {@code null}; reviewed nullable node, AnimationStyle, Duration, Curve and
 * callback contracts accept nullable values without weakening dynamic checks. An explicit Radio source-type identity may request nullable
 * values and adds independent non-dynamic and registry-consumption checks. Neither result is accepted
 * in isolation.</p>
 */
public record DartStaticTypeProbe(
        int expressionOffset,
        int expressionLength,
        int importInsertionOffset,
        int statementInsertionOffset,
        String expectedDartType,
        String expectedTypeLibraryUri,
        Optional<String> sourceTypeOverride,
        Optional<String> sourceTypeBound) {

    static final String SCAFFOLD_SCRIM_BUILDER_TYPE = "Widget? Function(BuildContext, Animation<double>)";
    private static final Pattern EXPECTED_TYPE = Pattern.compile(
            "(?:[A-Za-z][A-Za-z0-9_]*(?:<[A-Za-z][A-Za-z0-9_]*\\??>)?|(?:Object|bool|int|double|num|String|FocusNode|AnimationStyle|Duration|Curve|ShapeBorder|IconThemeData|TextStyle|TextHeightBehavior|BorderRadius|SystemUiOverlayStyle|AlignmentGeometry|Color|Decoration|BoxConstraints|Matrix4|ScrollController|EdgeInsets|EdgeInsetsGeometry|InputCounterWidgetBuilder|EditableTextContextMenuBuilder|ItemExtentBuilder|ChildIndexGetter)\\?)");
    private static final Pattern SOURCE_TYPE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?\\??");
    private static final Pattern LIBRARY_URI = Pattern.compile(
            "(?:dart:[a-z][a-z0-9_.]*|package:[a-z][a-z0-9_]*/"
            + "(?:[A-Za-z0-9_-][A-Za-z0-9_.-]*/)*"
            + "[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.dart)");

    public DartStaticTypeProbe(int expressionOffset, int expressionLength, int importInsertionOffset,
            int statementInsertionOffset, String expectedDartType, String expectedTypeLibraryUri) {
        this(expressionOffset, expressionLength, importInsertionOffset, statementInsertionOffset,
                expectedDartType, expectedTypeLibraryUri, Optional.empty(), Optional.empty());
    }

    public DartStaticTypeProbe(int expressionOffset, int expressionLength, int importInsertionOffset,
            int statementInsertionOffset, String expectedDartType, String expectedTypeLibraryUri,
            Optional<String> sourceTypeOverride) {
        this(expressionOffset, expressionLength, importInsertionOffset, statementInsertionOffset,
                expectedDartType, expectedTypeLibraryUri, sourceTypeOverride, Optional.empty());
    }

    public DartStaticTypeProbe {
        if (expressionOffset < 0 || expressionLength <= 0) {
            throw new IllegalArgumentException(
                    "static-type expression offset must be non-negative and length positive");
        }
        if (importInsertionOffset < 0 || statementInsertionOffset < 0
                || importInsertionOffset > statementInsertionOffset
                || statementInsertionOffset > expressionOffset) {
            throw new IllegalArgumentException(
                    "static-type proof insertion offsets are invalid");
        }
        expectedDartType = Objects.requireNonNull(
                expectedDartType, "expectedDartType");
        if (expectedDartType.length() > 128
                || !EXPECTED_TYPE.matcher(expectedDartType).matches()
                && !SCAFFOLD_SCRIM_BUILDER_TYPE.equals(expectedDartType)
                && !java.util.Set.of("VoidCallback?", "ValueNotifier<EdgeInsets>?", "ImageErrorWidgetBuilder?", "Image?", "Rect?", "BackdropKey?", "Animation<double>?", "CustomPainter?", "DateTime?", "Key?", "LocalKey?", "TableBorder?", "Map<int, TableColumnWidth>?", "TableColumnWidth?", "WidgetStateProperty<Color?>?", "WidgetStateProperty<MouseCursor?>?", "Size").contains(expectedDartType)) {
            throw new IllegalArgumentException(
                    "expected Dart type must use a reviewed closed type form");
        }
        if (expectedDartType.equals("AnimatedIconData") && !expectedTypeLibraryUri.equals("package:flutter/material.dart")) {
            throw new IllegalArgumentException("AnimatedIconData proof requires the Flutter Material library");
        }
        Objects.requireNonNull(sourceTypeOverride, "sourceTypeOverride");
        if (sourceTypeOverride.isPresent() && (sourceTypeOverride.orElseThrow().length() > 256
                || !SOURCE_TYPE.matcher(sourceTypeOverride.orElseThrow()).matches()
                || !java.util.Set.of("Type", "Object", "Object?", "ValueChanged<Object?>", "RadioGroupRegistry<Object>", "Tween<Object>", "ValueListenable<Object>", "ValueWidgetBuilder<Object>",
                        "NotificationListenerCallback<Notification>").contains(expectedDartType))) {
            throw new IllegalArgumentException("Source type override must be a reviewed closed generated type identity");
        }
        if (expectedDartType.equals("NotificationListenerCallback<Notification>")
                && sourceTypeOverride.filter(type -> type.endsWith("?")).isPresent()) {
            throw new IllegalArgumentException("Notification callback type must be non-nullable");
        }
        Objects.requireNonNull(sourceTypeBound, "sourceTypeBound");
        if (sourceTypeBound.isPresent() && (!sourceTypeBound.orElseThrow().equals("Notification")
                || !expectedDartType.equals("Type") || sourceTypeOverride.isEmpty()
                || sourceTypeOverride.orElseThrow().endsWith("?"))) {
            throw new IllegalArgumentException("Only a non-null selected Type may request the reviewed Notification bound");
        }
        expectedTypeLibraryUri = Objects.requireNonNull(
                expectedTypeLibraryUri, "expectedTypeLibraryUri");
        if (SCAFFOLD_SCRIM_BUILDER_TYPE.equals(expectedDartType)
                && !java.util.Set.of("package:flutter/widgets.dart", "package:flutter/material.dart").contains(expectedTypeLibraryUri)) {
            throw new IllegalArgumentException("Scaffold scrim builder proof requires the reviewed Flutter Widgets/Material library");
        }
        if (java.util.Set.of("InputCounterWidgetBuilder", "InputCounterWidgetBuilder?").contains(expectedDartType)
                && !expectedTypeLibraryUri.equals("package:flutter/material.dart")) {
            throw new IllegalArgumentException("Input counter proof requires the reviewed Flutter Material library");
        }
        if (java.util.Set.of("EditableTextContextMenuBuilder", "EditableTextContextMenuBuilder?").contains(expectedDartType)
                && !java.util.Set.of("package:flutter/widgets.dart", "package:flutter/material.dart").contains(expectedTypeLibraryUri)) {
            throw new IllegalArgumentException("Editable text menu proof requires the reviewed Flutter Widgets/Material library");
        }
        if (java.util.Set.of("TableColumnWidth", "Map<int, TableColumnWidth>?", "TableBorder?", "Key?", "LocalKey?", "MultiChildLayoutDelegate", "FlowDelegate", "SingleChildLayoutDelegate", "CustomPainter?", "Size", "ShaderCallback", "ImageFilterConfig", "BackdropKey?", "ImageFilter", "FragmentShader", "Float64List", "ColorFilter", "Image?", "Rect?", "Animation<double>?", "ImageProvider<Object>", "ImageErrorWidgetBuilder?", "VoidCallback?", "ValueNotifier<EdgeInsets>?", "TransformCallback", "NullableIndexedWidgetBuilder", "IndexedWidgetBuilder", "SliverLayoutWidgetBuilder", "LayoutWidgetBuilder", "OrientationWidgetBuilder", "TransitionBuilder", "AnimatedCrossFadeBuilder", "AnimatedSwitcherTransitionBuilder", "AnimatedSwitcherLayoutBuilder", "Listenable", "Tween<Object>", "ValueListenable<Object>", "ValueWidgetBuilder<Object>", "ChildIndexGetter", "ChildIndexGetter?").contains(expectedDartType)
                && !java.util.Set.of("package:flutter/widgets.dart", "package:flutter/material.dart").contains(expectedTypeLibraryUri)) {
            throw new IllegalArgumentException("Layout/sliver callback proofs require reviewed Flutter Widgets/Material context");
        }
        if (java.util.Set.of("ItemExtentBuilder", "ItemExtentBuilder?").contains(expectedDartType)
                && !java.util.Set.of("package:flutter/widgets.dart", "package:flutter/material.dart").contains(expectedTypeLibraryUri)) {
            throw new IllegalArgumentException("Item extent proof requires the reviewed Flutter Widgets/Material context with Rendering types");
        }
        if (java.util.Set.of("AsyncCallback", "SystemUiOverlayStyle?").contains(expectedDartType)
                && !java.util.Set.of("package:flutter/widgets.dart", "package:flutter/material.dart").contains(expectedTypeLibraryUri)) {
            throw new IllegalArgumentException("Foundation/services witnesses require the reviewed Flutter proof context");
        }
        if (expectedTypeLibraryUri.length() > 512
                || !LIBRARY_URI.matcher(expectedTypeLibraryUri).matches()
                || expectedTypeLibraryUri.contains("//")
                || expectedTypeLibraryUri.contains("/./")
                || expectedTypeLibraryUri.contains("/../")) {
            throw new IllegalArgumentException(
                    "expected type library URI must be canonical and bounded");
        }
    }

    public int expressionEndOffset() {
        return Math.addExact(expressionOffset, expressionLength);
    }
}
