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
        assertEquals(18, javaTypes.size(),
                "the reviewed Canvas source set includes Image");
        assertTrue(javaTypes.contains("flutter.widgets.Container"));
        assertTrue(javaTypes.contains("flutter.widgets.AspectRatio"));
        assertTrue(javaTypes.contains("flutter.widgets.Opacity"));
        assertTrue(javaTypes.contains("flutter.widgets.Align"));
        assertTrue(javaTypes.contains("flutter.widgets.FractionallySizedBox"));
        assertTrue(javaTypes.contains("flutter.widgets.Stack"));
        assertTrue(javaTypes.contains("flutter.widgets.Expanded"));
        assertTrue(javaTypes.contains("flutter.widgets.Image"));

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
        assertTrue(javaContainers.contains("flutter.widgets.Stack"),
                "Stack.children must remain a Java-admitted DnD target");
        assertTrue(!javaContainers.contains("flutter.widgets.Expanded"),
                "Expanded.child is required replacement-only, not an insertion target");

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
