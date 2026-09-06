package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.canvas.runner.CanvasRunnerBundle;
import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCapabilityCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCapability;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Prevents Java admission and the packaged Dart runner from drifting apart. */
class FlutterWidgetCapabilityParityTest {
    private static final Pattern WIDGET_TYPE = Pattern.compile(
            "'(flutter\\.(?:material|widgets)\\.[A-Za-z0-9]+)'");

    @Test
    void canvasAdmissionMatchesDartDecoderAndRendererExactly() throws Exception {
        Set<String> javaTypes = capabilityTypes(WidgetCapability.CANVAS);
        String model = runnerSource("lib/src/canvas_model.dart");
        String view = runnerSource("lib/src/canvas_view.dart");
        assertEquals(79, javaTypes.size(),
                "the reviewed Canvas source set includes Wrap, ListView, FittedBox, "
                + "ConstrainedBox, UnconstrainedBox, LimitedBox, OverflowBox, Spacer, "
                + "Baseline, IntrinsicHeight, IntrinsicWidth, Offstage, SizedOverflowBox, "
                + "Transform, RotatedBox, ListBody, OverflowBar, GridView.count, "
                + "SingleChildScrollView, SafeArea, ColoredBox, Placeholder, and "
                + "Directionality, DecoratedBox, ExcludeSemantics, IndexedStack and "
                + "ClipRect, ClipOval, ClipRRect, ClipPath and ClipRSuperellipse");
        assertTrue(javaTypes.contains("flutter.widgets.Container"));
        assertTrue(javaTypes.contains("flutter.widgets.AspectRatio"));
        assertTrue(javaTypes.contains("flutter.widgets.Opacity"));
        assertTrue(javaTypes.contains("flutter.widgets.Align"));
        assertTrue(javaTypes.contains("flutter.widgets.FractionallySizedBox"));
        assertTrue(javaTypes.contains("flutter.widgets.FittedBox"));
        assertTrue(javaTypes.contains("flutter.widgets.ConstrainedBox"));
        assertTrue(javaTypes.contains("flutter.widgets.UnconstrainedBox"));
        assertTrue(javaTypes.contains("flutter.widgets.LimitedBox"));
        assertTrue(javaTypes.contains("flutter.widgets.OverflowBox"));
        assertTrue(javaTypes.contains("flutter.widgets.Stack"));
        assertTrue(javaTypes.contains("flutter.widgets.IndexedStack"));
        assertTrue(javaTypes.contains("flutter.widgets.Wrap"));
        assertTrue(javaTypes.contains("flutter.widgets.Expanded"));
        assertTrue(javaTypes.contains("flutter.widgets.Flexible"));
        assertTrue(javaTypes.contains("flutter.widgets.Spacer"));
        assertTrue(javaTypes.contains("flutter.widgets.Baseline"));
        assertTrue(javaTypes.contains("flutter.widgets.IntrinsicHeight"));
        assertTrue(javaTypes.contains("flutter.widgets.IntrinsicWidth"));
        assertTrue(javaTypes.contains("flutter.widgets.Offstage"));
        assertTrue(javaTypes.contains("flutter.widgets.SizedOverflowBox"));
        assertTrue(javaTypes.contains("flutter.widgets.Transform"));
        assertTrue(javaTypes.contains("flutter.widgets.RotatedBox"));
        assertTrue(javaTypes.contains("flutter.widgets.ListBody"));
        assertTrue(javaTypes.contains("flutter.widgets.OverflowBar"));
        assertTrue(javaTypes.contains("flutter.widgets.SafeArea"));
        assertTrue(javaTypes.contains("flutter.widgets.ListView"));
        assertTrue(javaTypes.contains("flutter.widgets.GridView"));
        assertTrue(javaTypes.contains("flutter.widgets.SingleChildScrollView"));
        assertTrue(javaTypes.contains("flutter.widgets.Image"));
        assertTrue(javaTypes.contains("flutter.widgets.ColoredBox"));
        assertTrue(javaTypes.contains("flutter.widgets.Placeholder"));
        assertTrue(javaTypes.contains("flutter.widgets.Directionality"));
        assertTrue(javaTypes.contains("flutter.widgets.DecoratedBox"));
        assertTrue(javaTypes.contains("flutter.widgets.ExcludeSemantics"));
        assertTrue(javaTypes.contains("flutter.widgets.ClipRect"));
        assertTrue(javaTypes.contains("flutter.widgets.ClipOval"));
        assertTrue(javaTypes.contains("flutter.widgets.ClipRRect"));
        assertTrue(javaTypes.contains("flutter.widgets.ClipPath"));
        assertTrue(javaTypes.contains("flutter.widgets.ClipRSuperellipse"));
        assertTrue(javaTypes.contains("flutter.widgets.PhysicalModel"));
        assertTrue(javaTypes.contains("flutter.widgets.PhysicalShape"));
        assertTrue(javaTypes.contains("flutter.widgets.RepaintBoundary"));
        assertTrue(javaTypes.contains("flutter.widgets.IgnorePointer"));
        assertTrue(javaTypes.contains("flutter.widgets.AbsorbPointer"));
        assertTrue(javaTypes.contains("flutter.widgets.BlockSemantics"));
        assertTrue(javaTypes.contains("flutter.widgets.MergeSemantics"));
        assertTrue(javaTypes.contains("flutter.widgets.IndexedSemantics"));
        assertTrue(javaTypes.contains("flutter.widgets.ExcludeFocus"));
        assertTrue(javaTypes.contains("flutter.widgets.ExcludeFocusTraversal"));
        assertTrue(javaTypes.contains("flutter.widgets.Visibility"));
        assertTrue(javaTypes.contains("flutter.widgets.TickerMode"));
        assertTrue(javaTypes.contains("flutter.widgets.DefaultTextHeightBehavior"));
        assertTrue(javaTypes.contains("flutter.widgets.DefaultSelectionStyle"));
        assertTrue(javaTypes.contains("flutter.widgets.IconTheme"));
        assertTrue(javaTypes.contains("flutter.widgets.ImageIcon"));
        assertTrue(javaTypes.contains("flutter.material.Divider"));
        assertTrue(javaTypes.contains("flutter.material.VerticalDivider"));
        assertTrue(javaTypes.contains("flutter.material.Card"));
        assertTrue(javaTypes.contains("flutter.material.Badge"));
        assertTrue(javaTypes.contains("flutter.material.CircleAvatar"));
        assertTrue(javaTypes.contains("flutter.material.LinearProgressIndicator"));
        assertTrue(javaTypes.contains("flutter.material.CircularProgressIndicator"));
        assertTrue(javaTypes.contains("flutter.material.RefreshProgressIndicator"));
        assertTrue(javaTypes.contains("flutter.material.RefreshIndicator"));
        assertTrue(javaTypes.contains("flutter.material.TextButton"));
        assertTrue(javaTypes.contains("flutter.material.OutlinedButton"));
        assertTrue(javaTypes.contains("flutter.material.FilledButton"));
        assertTrue(javaTypes.contains("flutter.material.TextField"));

        assertEquals(18, CanvasModelPayloadCodec.VERSION);
        assertTrue(model.contains("const canvasModelProtocolVersion = 18;"),
                "the packaged Dart decoder must consume Java payload v18");

        assertEquals(javaTypes, widgetTypes(block(
                model, "const _widgetSpecifications", "class _NodeBudget")),
                "Java Canvas gate and Dart strict decoder must be exact peers");
        assertEquals(javaTypes, widgetTypes(block(
                view, "final child = switch (node.type)",
                "final selected = selectedWidgetId")),
                "Java Canvas gate and Dart renderer must be exact peers");
    }

    @Test
    void exactBelowTypeCanvasContractMatchesReviewedDartRuntimeSchema()
            throws Exception {
        String model = runnerSource("lib/src/canvas_model.dart");
        String dartContract = reviewedSchemaContract(model);

        assertEquals(
                BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract(),
                dartContract,
                "Java and Dart must agree on wire kinds, required/default values, "
                + "numeric bounds, constraints and slot cardinality");
        assertTrue(dartContract.contains("W|flutter.widgets.ClipRRect\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.ClipPath\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.ClipRSuperellipse\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.PhysicalModel\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.PhysicalShape\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.RepaintBoundary\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.IgnorePointer\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.AbsorbPointer\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.BlockSemantics\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.MergeSemantics\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.IndexedSemantics\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.ExcludeFocus\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.ExcludeFocusTraversal\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.Visibility\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.TickerMode\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.DefaultTextHeightBehavior\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.DefaultSelectionStyle\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.IconTheme\n"));
        assertTrue(dartContract.contains("W|flutter.widgets.ImageIcon\n"));
        assertTrue(dartContract.contains("W|flutter.material.Divider\n"));
        assertTrue(dartContract.contains("W|flutter.material.VerticalDivider\n"));
        assertTrue(dartContract.contains("W|flutter.material.Card\n"));
        assertTrue(dartContract.contains("W|flutter.material.Badge\n"));
        assertTrue(dartContract.contains("W|flutter.material.CircleAvatar\n"));
        assertTrue(dartContract.contains("W|flutter.material.LinearProgressIndicator\n"));
        assertTrue(dartContract.contains("W|flutter.material.CircularProgressIndicator\n"));
        assertTrue(dartContract.contains("W|flutter.material.RefreshProgressIndicator\n"));
        assertTrue(dartContract.contains("W|flutter.material.RefreshIndicator\n"));
        assertTrue(dartContract.contains("W|flutter.material.TextButton\n"));
        assertTrue(dartContract.contains("W|flutter.material.OutlinedButton\n"));
        assertTrue(dartContract.contains("W|flutter.material.FilledButton\n"));
        assertTrue(dartContract.contains(
                "borderRadius:borderRadius:v1:physical:finiteNonNegative\n"));
        assertTrue(dartContract.contains(
                "dartObjectReference:v1:CustomClipper<Path>:currentOrPackage"));
        assertTrue(dartContract.contains(
                "dartObjectReference:v1:ShapeBorder:currentOrPackage"));
        assertTrue(dartContract.contains(
                "P|borderRadius|borderRadius|0|-|-|"
                + "borderRadius:borderRadius:v1:physical,directional:finiteNonNegative\n"));
        String rectClipper =
                "P|clipper|dartObjectReference|0|-|-|dartObjectReference:"
                + "dartObjectReference:v1:CustomClipper<Rect>:currentOrPackage:"
                + "root,optionalMember:reference,zeroArgumentInvocation:"
                + "requiredConstnessBoolean(false,true)\n";
        assertTrue(dartContract.contains(
                "W|flutter.widgets.ClipOval\n"
                + "P|clipBehavior|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n"
                + rectClipper));
        assertTrue(dartContract.contains(
                "W|flutter.widgets.ClipRect\n"
                + "P|clipBehavior|enum|0|-|-|enum:enum:"
                + "cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:"
                + "Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n"
                + rectClipper));
    }

    @Test
    void paletteContainerAdmissionMatchesDartDropMatrixExactly()
            throws Exception {
        Set<String> javaContainers =
                BuiltInWidgetCapabilityCatalog.definitionsSupporting(
                        WidgetCapability.DND).stream()
                        .filter(definition -> definition.slots().stream()
                                .anyMatch(slot -> slot.minChildren() == 0))
                        .map(definition -> definition.typeId().value())
                        .collect(Collectors.toUnmodifiableSet());
        String drop = runnerSource("lib/src/canvas_drop.dart");
        assertTrue(javaContainers.contains("flutter.widgets.AspectRatio"),
                "AspectRatio.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.Opacity"),
                "Opacity.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.Align"),
                "Align.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.FractionallySizedBox"),
                "FractionallySizedBox.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.FittedBox"),
                "FittedBox.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.ConstrainedBox"),
                "ConstrainedBox.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.UnconstrainedBox"),
                "UnconstrainedBox.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.LimitedBox"),
                "LimitedBox.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.OverflowBox"),
                "OverflowBox.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.Stack"),
                "Stack.children must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.IndexedStack"),
                "IndexedStack.children must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.Wrap"),
                "Wrap.children must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.ListView"),
                "ListView.children must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.GridView"),
                "GridView.count.children must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.SingleChildScrollView"),
                "SingleChildScrollView.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.ColoredBox"),
                "ColoredBox.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.Placeholder"),
                "Placeholder.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.Visibility"),
                "Visibility.replacement is optional; its required child remains replacement-only");
        assertTrue(!javaContainers.contains("flutter.widgets.TickerMode"),
                "TickerMode.child is required replacement-only, not an empty insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.DefaultTextHeightBehavior"),
                "DefaultTextHeightBehavior.child is required replacement-only, not an insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.DefaultSelectionStyle"),
                "DefaultSelectionStyle.child is required replacement-only, not an insertion target");
        assertTrue(javaContainers.contains("flutter.material.TextButton"), "TextButton.icon has an optional Icon destination; Standard excludes it at runtime");
        assertTrue(javaContainers.contains("flutter.material.OutlinedButton"), "OutlinedButton.icon has an optional Icon destination; Standard excludes it at runtime");
        assertTrue(javaContainers.contains("flutter.material.FilledButton"), "FilledButton has optional Child and constructor-dependent Icon destinations");
        assertTrue(!javaContainers.contains("flutter.material.RefreshIndicator"),
                "RefreshIndicator.child is required replacement-only, never an empty insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.IconTheme"),
                "IconTheme.child is required replacement-only, not an insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.Expanded"),
                "Expanded.child is required replacement-only, not an insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.Flexible"),
                "Flexible.child is required replacement-only, not an insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.Spacer"),
                "Spacer has no child slot and is never a DnD destination");
        assertTrue(!javaContainers.contains("flutter.widgets.SafeArea"),
                "SafeArea.child is required replacement-only, not an empty insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.ExcludeFocus"),
                "ExcludeFocus.child is required replacement-only, not an empty insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.ExcludeFocusTraversal"),
                "ExcludeFocusTraversal.child is required replacement-only, not an empty insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.Directionality"),
                "Directionality.child is required replacement-only, not an empty insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.DecoratedBox"),
                "DecoratedBox.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.ExcludeSemantics"),
                "ExcludeSemantics.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.ClipRect"),
                "ClipRect.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.ClipOval"),
                "ClipOval.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.ClipRRect"),
                "ClipRRect.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.ClipPath"),
                "ClipPath.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.ClipRSuperellipse"),
                "ClipRSuperellipse.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.PhysicalModel"),
                "PhysicalModel.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.IgnorePointer"),
                "IgnorePointer.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.AbsorbPointer"),
                "AbsorbPointer.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.BlockSemantics"),
                "BlockSemantics.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.IndexedSemantics"),
                "IndexedSemantics.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.MergeSemantics"),
                "MergeSemantics.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.RepaintBoundary"),
                "RepaintBoundary.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.PhysicalShape"),
                "PhysicalShape.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.Baseline"),
                "Baseline.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.IntrinsicHeight"),
                "IntrinsicHeight.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.IntrinsicWidth"),
                "IntrinsicWidth.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.Offstage"),
                "Offstage.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.SizedOverflowBox"),
                "SizedOverflowBox.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.Transform"),
                "Transform.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.RotatedBox"),
                "RotatedBox.child must remain a Java-admitted DnD target");
        assertTrue(javaContainers.contains("flutter.widgets.OverflowBar"),
                "OverflowBar.children must remain a Java-admitted DnD target");

        assertEquals(javaContainers, widgetTypes(block(
                drop, "canvasDropSlotsForWidgetType", "canvasDropSlotForWidgetSlot")),
                "Java Palette containers and Dart native drop targets must stay in parity");
    }

    private static Set<String> capabilityTypes(WidgetCapability capability) {
        return BuiltInWidgetCapabilityCatalog.definitionsSupporting(capability)
                .stream().map(definition -> definition.typeId().value())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String runnerSource(String path) throws IOException {
        try (var input = CanvasRunnerBundle.openSource(path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String block(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        if (start < 0 || end < 0) {
            throw new AssertionError("Missing Dart contract block " + startMarker);
        }
        return source.substring(start, end);
    }

    private static String reviewedSchemaContract(String source) {
        String marker =
                "const String canvasReviewedWidgetSchemaContract = '''";
        int declaration = source.indexOf(marker);
        int contentStart = source.indexOf('\n', declaration + marker.length());
        int contentEnd = source.indexOf("'''", contentStart + 1);
        if (declaration < 0 || contentStart < 0 || contentEnd < 0) {
            throw new AssertionError("Missing reviewed Dart Canvas schema contract");
        }
        String framed = source.substring(contentStart + 1, contentEnd)
                .replace("\r\n", "\n");
        if (!framed.startsWith("\n")) {
            throw new AssertionError(
                    "Reviewed Dart Canvas schema is missing its framing newline");
        }
        return framed.substring(1);
    }

    private static Set<String> widgetTypes(String source) {
        Matcher matcher = WIDGET_TYPE.matcher(source);
        HashSet<String> result = new HashSet<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return Set.copyOf(result);
    }
}
