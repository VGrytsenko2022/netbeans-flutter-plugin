package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.canvas.runner.CanvasRunnerBundle;
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
        assertEquals(45, javaTypes.size(),
                "the reviewed Canvas source set includes Wrap, ListView, FittedBox, "
                + "ConstrainedBox, UnconstrainedBox, LimitedBox, OverflowBox, Spacer, "
                + "Baseline, IntrinsicHeight, IntrinsicWidth, Offstage, SizedOverflowBox, "
                + "Transform, RotatedBox, ListBody, OverflowBar, GridView.count, "
                + "SingleChildScrollView, SafeArea, ColoredBox, Placeholder, and "
                + "Directionality, DecoratedBox and ExcludeSemantics");
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
        assertTrue(javaTypes.contains("flutter.material.TextField"));

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

        assertEquals(
                BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract(),
                reviewedSchemaContract(model),
                "Java and Dart must agree on wire kinds, required/default values, "
                + "numeric bounds, constraints and slot cardinality");
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
        assertTrue(!javaContainers.contains("flutter.widgets.Expanded"),
                "Expanded.child is required replacement-only, not an insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.Flexible"),
                "Flexible.child is required replacement-only, not an insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.Spacer"),
                "Spacer has no child slot and is never a DnD destination");
        assertTrue(!javaContainers.contains("flutter.widgets.SafeArea"),
                "SafeArea.child is required replacement-only, not an empty insertion target");
        assertTrue(!javaContainers.contains("flutter.widgets.Directionality"),
                "Directionality.child is required replacement-only, not an empty insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.DecoratedBox"),
                "DecoratedBox.child is an ordinary optional any-widget insertion target");
        assertTrue(javaContainers.contains("flutter.widgets.ExcludeSemantics"),
                "ExcludeSemantics.child is an ordinary optional any-widget insertion target");
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
