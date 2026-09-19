package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircularProgressIndicatorCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId PROGRESS = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD = new SlotName("child"), CHILDREN = new SlotName("children");

    @Test
    void addMoveAndAllFifteenFieldsKeepExactHistorySaveReopenAndReset() throws Exception {
        var initial = openDefault();
        var added = add(initial);
        assertEquals(Map.of(p("variant"), new PropertyValue.StringValue("material")), find(added, PROGRESS).properties());
        assertTrue(source(added).contains("const CircularProgressIndicator()"));
        assertExactPair(initial, added.undo().session());
        var current = applied(added, new MoveWidget(PROGRESS, new WidgetPlacement(ROOT, CHILDREN, 0)));
        var values = values();
        for (var entry : values.entrySet()) {
            var before = current;
            current = applied(current, new SetProperty(PROGRESS, entry.getKey(), entry.getValue()));
            assertExactPair(before, current.undo().session());
            assertExactPair(current, current.undo().session().redo().session());
        }
        var configured = applied(current, patch(Map.of(p("controller"), reference()), Set.of(p("value"))));
        assertEquals(current.cursor() + 1, configured.cursor());
        assertFalse(find(configured, PROGRESS).properties().containsKey(p("value")));
        assertEquals(14, find(configured, PROGRESS).properties().size());
        current = reopen(configured);
        assertFalse(current.dirty());
        for (var name : new ArrayList<>(find(current, PROGRESS).properties().keySet())) {
            if (name.value().equals("variant")) continue;
            var before = current;
            current = applied(current, new ResetProperty(PROGRESS, name));
            assertExactPair(before, current.undo().session());
        }
        assertEquals(Map.of(p("variant"), new PropertyValue.StringValue("material")), find(current, PROGRESS).properties());
        assertTrue(source(current).contains("const CircularProgressIndicator()"));
        assertExactPair(current, reopen(current));
    }

    @Test
    void progressModesRequireAtomicMutualResetAndRejectedEditPreservesRedoBranch() throws Exception {
        var base = add(openDefault());
        var determinate = applied(base, new SetProperty(PROGRESS, p("value"), d("1.5")));
        assertTrue(source(determinate).contains("value: 1.5"));
        assertRejectedUnchanged(determinate, new SetProperty(PROGRESS, p("controller"), reference()), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var controlled = applied(determinate, patch(Map.of(p("controller"), reference()), Set.of(p("value"))));
        assertExactPair(determinate, controlled.undo().session());
        assertRejectedUnchanged(controlled, new SetProperty(PROGRESS, p("value"), d("-0.25")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var restored = applied(controlled, patch(Map.of(p("value"), d("-0.25")), Set.of(p("controller"))));
        var undone = restored.undo().session();
        assertTrue(undone.canRedo());
        assertRejectedUnchanged(undone, new SetProperty(PROGRESS, p("strokeWidth"), CircularProgressIndicatorWidgetPropertySchema.POSITIVE_INFINITY), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertExactPair(restored, undone.redo().session());
        assertExactPair(restored, reopen(restored));
    }

    @Test
    void valueColorLiteralNullThemeAndProjectAnimationAllSurviveSaveReopenAndReset() throws Exception {
        for (PropertyValue value : List.of(new PropertyValue.ColorValue(0xff00ff00L), new PropertyValue.NullValue(),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")), reference())) {
            var base = add(openDefault());
            var configured = applied(base, new SetProperty(PROGRESS, p("valueColor"), value));
            assertEquals(value, find(configured, PROGRESS).properties().get(p("valueColor")));
            assertExactPair(base, configured.undo().session());
            assertExactPair(configured, configured.undo().session().redo().session());
            var reopened = reopen(configured);
            var cleared = applied(reopened, new ResetProperty(PROGRESS, p("valueColor")));
            assertFalse(find(cleared, PROGRESS).properties().containsKey(p("valueColor")));
            assertFalse(source(cleared).contains("AlwaysStoppedAnimation"));
            assertFalse(source(cleared).contains("projectProgress"));
        }
    }

    @Test
    void geometryInfinityAndExplicitFalseRemainDistinctFromUnsetAcrossDurableReopen() throws Exception {
        var initial = add(openDefault());
        var configured = applied(initial, patch(Map.of(p("trackGap"), CircularProgressIndicatorWidgetPropertySchema.POSITIVE_INFINITY,
                p("year2023"), new PropertyValue.BooleanValue(false)), Set.of()));
        assertFalse(source(configured).contains("dart:core"));
        assertEquals(1, source(configured).split("1.0 / 0.0", -1).length - 1);
        var reopened = reopen(configured);
        assertEquals(new PropertyValue.BooleanValue(false), find(reopened, PROGRESS).properties().get(p("year2023")));
        var reset = applied(reopened, patch(Map.of(), Set.of(p("trackGap"), p("year2023"))));
        assertEquals(Map.of(p("variant"), new PropertyValue.StringValue("material")), find(reset, PROGRESS).properties());
        assertExactPair(reopened, reset.undo().session());
        assertExactPair(reset, reopen(reset));
    }

    @Test
    void unsupportedNullsExpressionsAndChildOperationsLeaveTheExactPairUnchanged() throws Exception {
        var base = add(openDefault());
        for (String name : List.of("controller", "value", "strokeWidth", "year2023")) {
            assertRejectedUnchanged(base, new SetProperty(PROGRESS, p(name), new PropertyValue.NullValue()), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        for (String name : List.of("valueColor", "controller")) {
            assertRejectedUnchanged(base, new SetProperty(PROGRESS, p(name), new PropertyValue.DartExpressionValue("unreviewed()")), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        var invalid = new AddWidget(new WidgetPlacement(PROGRESS, CHILD, 0), text(StableId.random(), "No child"));
        var result = base.apply(invalid);
        assertFalse(result.changed());
        assertSame(base, result.session());
        assertExactPair(base, result.session());
        var removed = applied(base, new RemoveWidget(PROGRESS));
        assertExactPair(base, removed.undo().session());
    }

    @Test
    void constructorSwitchesAreAtomicAndRequiredVariantResetNeverAdvancesHistory() throws Exception {
        var initial = add(openDefault());
        assertRejectedUnchanged(initial, new ResetProperty(PROGRESS, p("variant")), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        var color = new PropertyValue.ColorValue(0xff00aa00L);
        var material = applied(initial, patch(Map.of(p("color"), color, p("valueColor"), new PropertyValue.NullValue(),
                p("strokeAlign"), d("-8"), p("year2023"), new PropertyValue.BooleanValue(false)), Set.of()));
        assertRejectedUnchanged(material, new SetProperty(PROGRESS, p("variant"), new PropertyValue.StringValue("adaptive")),
                DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var adaptive = applied(material, patch(Map.of(p("variant"), new PropertyValue.StringValue("adaptive")), Set.of(p("color"))));
        assertEquals(material.cursor() + 1, adaptive.cursor());
        assertTrue(source(adaptive).contains("CircularProgressIndicator.adaptive("));
        assertEquals(new PropertyValue.NullValue(), find(adaptive, PROGRESS).properties().get(p("valueColor")));
        assertExactPair(material, adaptive.undo().session());
        assertExactPair(adaptive, adaptive.undo().session().redo().session());
        var reopened = reopen(adaptive);
        assertRejectedUnchanged(reopened, new SetProperty(PROGRESS, p("color"), color), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(reopened, new ResetProperty(PROGRESS, p("variant")), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        var restored = applied(reopened, patch(Map.of(p("variant"), new PropertyValue.StringValue("material"), p("color"), color), Set.of()));
        assertEquals(find(material, PROGRESS).properties(), find(restored, PROGRESS).properties());
        assertExactPair(restored, reopen(restored));
        assertExactPair(reopened, restored.undo().session());
    }

    private static Map<PropertyName, PropertyValue> values() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("value"), d("0.5"));
        values.put(p("backgroundColor"), new PropertyValue.ColorValue(0xff123456L));
        values.put(p("color"), new PropertyValue.ColorValue(0xff00ff00L));
        values.put(p("valueColor"), new PropertyValue.NullValue());
        values.put(p("strokeWidth"), d("-2"));
        values.put(p("strokeAlign"), d("-3"));
        values.put(p("semanticsLabel"), new PropertyValue.StringValue("Progress"));
        values.put(p("semanticsValue"), new PropertyValue.StringValue("45%"));
        values.put(p("strokeCap"), new PropertyValue.EnumValue("StrokeCap", "square"));
        values.put(p("constraints"), new PropertyValue.BoxConstraintsValue(BigDecimal.ONE, Optional.of(BigDecimal.TEN),
                BigDecimal.ONE, Optional.empty()));
        values.put(p("padding"), new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2),
                BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        values.put(p("trackGap"), d("-4"));
        values.put(p("year2023"), new PropertyValue.BooleanValue(false));
        return values;
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static PropertyValue.DartObjectReferenceValue reference() {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), "projectProgress", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static PatchProperties patch(Map<PropertyName, PropertyValue> values, Set<PropertyName> resets) {
        List<PatchProperties.Patch> patches = new ArrayList<>();
        resets.forEach(name -> patches.add(new PatchProperties.ResetPatch(name)));
        values.forEach((name, value) -> patches.add(new PatchProperties.SetPatch(name, value)));
        return new PatchProperties(PROGRESS, patches);
    }
    private static DesignerCommandSession add(DesignerCommandSession session) {
        return applied(session, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 2),
                WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), PROGRESS)));
    }
    private static WidgetNode find(DesignerCommandSession session, StableId id) {
        return find(session.current().document().root(), id).orElseThrow();
    }
    private static Optional<WidgetNode> find(WidgetNode node, StableId id) {
        if (node.id().equals(id)) return Optional.of(node);
        for (WidgetSlot slot : node.slots().values()) {
            var children = slot instanceof WidgetSlot.SingleSlot single ? single.child().stream().toList()
                    : ((WidgetSlot.ListSlot) slot).children();
            for (var child : children) {
                var result = find(child, id);
                if (result.isPresent()) return result;
            }
        }
        return Optional.empty();
    }
    private static WidgetNode text(StableId id, String value) {
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    private static DesignerCommandSession applied(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }
    private static void assertRejectedUnchanged(DesignerCommandSession session, DesignerCommand command,
            DesignerCommandDiagnosticCode expected) {
        var result = session.apply(command);
        assertFalse(result.changed());
        assertSame(session, result.session());
        assertEquals(expected, result.diagnostics().getFirst().code());
        assertEquals(session.cursor(), result.session().cursor());
        assertEquals(session.canUndo(), result.session().canUndo());
        assertEquals(session.canRedo(), result.session().canRedo());
        assertExactPair(session, result.session());
    }
    private static void assertExactPair(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes());
        assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
        assertEquals(expected.current().document(), actual.current().document());
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }
    private static DesignerCommandSession reopen(DesignerCommandSession current) throws Exception {
        var saved = current.markSaved();
        var result = DesignerCommandSession.open(OriginalFdBytes.copyOf(saved.current().fdBytes(), FdCodecLimits.defaults()),
                saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DesignerCommandSession openDefault() throws Exception {
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(text(FIRST, "First"), text(SECOND, "Second")))));
        var id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64)), root);
        var generated = new DartRegionGenerator().generate(provisional, CATALOG).generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(),
                generated.build().normalizedSha256()), root);
        byte[] dart = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\nclass Sample extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), dart, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
