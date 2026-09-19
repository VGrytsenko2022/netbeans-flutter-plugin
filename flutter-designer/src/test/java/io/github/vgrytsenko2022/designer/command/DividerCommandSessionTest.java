package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.codec.FdCodecLimits;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.codec.OriginalFdBytes;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DividerCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.Divider");
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId DIVIDER = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void emptyLeafCreationSaveReopenAndFurtherEditingKeepSdkDefaultsAndStableId() throws Exception {
        var initial = openDefault();
        var added = add(initial);
        assertEquals(1, added.cursor());
        assertEquals(Map.of(), find(added, DIVIDER).properties());
        assertTrue(source(added).contains("Divider()"));
        assertExactPair(initial, added.undo().session());
        assertExactPair(added, added.undo().session().redo().session());
        var reopened = reopen(added);
        assertFalse(reopened.dirty());
        var edited = applied(reopened, new SetProperty(DIVIDER, p("height"), n("32")));
        edited = applied(edited, new SetProperty(FIRST, p("data"), new PropertyValue.StringValue("After divider reopen")));
        assertTrue(source(edited).contains("height: 32.0"));
        assertTrue(source(edited).contains("After divider reopen"));
        assertEquals(DIVIDER, find(edited, DIVIDER).id());
        assertExactPair(edited, reopen(edited));
    }

    @Test
    void everyOptionalFieldPatchesAsOneUnitThenResetsAfterSaveReopen() throws Exception {
        for (boolean directional : List.of(false, true)) {
            var initial = add(openDefault());
            var values = values(directional);
            var configured = applied(initial, new PatchProperties(DIVIDER, values.entrySet().stream().<PatchProperties.Patch>map(e -> new PatchProperties.SetPatch(e.getKey(), e.getValue())).toList()));
            assertEquals(initial.cursor() + 1, configured.cursor());
            assertEquals(values, find(configured, DIVIDER).properties());
            assertTrue(source(configured).contains("Theme.of(context).colorScheme.outlineVariant"));
            assertTrue(source(configured).contains(directional ? "BorderRadiusDirectional.only" : "BorderRadius.only"));
            assertExactPair(initial, configured.undo().session());
            assertExactPair(configured, configured.undo().session().redo().session());
            var current = reopen(configured);
            for (PropertyName name : values.keySet()) {
                var before = current;
                current = applied(current, new ResetProperty(DIVIDER, name));
                assertFalse(find(current, DIVIDER).properties().containsKey(name));
                assertExactPair(before, current.undo().session());
                assertExactPair(current, current.undo().session().redo().session());
            }
            assertEquals(Map.of(), find(current, DIVIDER).properties());
            assertFalse(source(current).contains("Theme.of(context)"));
            assertTrue(source(current).contains("Divider()"));
            assertExactPair(current, reopen(current));
        }
    }

    @Test
    void explicitZerosAndAllRadiusCoordinatesRemainIndependentAcrossHistoryAndReopen() throws Exception {
        var states = new ArrayList<DesignerCommandSession>();
        var current = openDefault(); states.add(current);
        current = add(current); states.add(current);
        for (var value : values(false).entrySet()) { current = applied(current, new SetProperty(DIVIDER, value.getKey(), value.getValue())); states.add(current); }
        current = applied(current, new SetProperty(DIVIDER, p("radius"), radius(true, false))); states.add(current);
        current = applied(current, new SetProperty(DIVIDER, p("radius"), radius(false, true))); states.add(current);
        for (String name : List.of("height", "thickness", "indent", "endIndent")) {
            current = applied(current, new SetProperty(DIVIDER, p(name), n("0"))); states.add(current);
            assertEquals(n("0"), find(current, DIVIDER).properties().get(p(name)));
        }
        current = applied(current, new SetProperty(DIVIDER, p("color"), new PropertyValue.ColorValue(0))); states.add(current);
        current = applied(current, new MoveWidget(DIVIDER, new WidgetPlacement(ROOT, CHILDREN, 0))); states.add(current);
        assertExactPair(current, reopen(current));
        for (int index = states.size() - 2; index >= 0; index--) { current = current.undo().session(); assertExactPair(states.get(index), current); assertEquals(index, current.cursor()); }
        assertFalse(current.canUndo());
        for (int index = 1; index < states.size(); index++) { current = current.redo().session(); assertExactPair(states.get(index), current); assertEquals(index, current.cursor()); }
        assertFalse(current.canRedo());
    }

    @Test
    void invalidAtomicPatchPreservesPairDirtyCursorAndRedoBranch() throws Exception {
        var initial = add(openDefault());
        var configured = applied(initial, new SetProperty(DIVIDER, p("height"), n("32")));
        var current = configured.undo().session();
        assertTrue(current.canRedo());
        for (String name : values(false).keySet().stream().map(PropertyName::value).toList()) {
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.BooleanValue(false), new PropertyValue.StringValue("invalid"), new PropertyValue.DartExpressionValue("Divider.createBorderSide(context)"))) {
                String other = name.equals("height") ? "indent" : "height";
                assertRejectedUnchanged(current, new PatchProperties(DIVIDER, List.of(new PatchProperties.SetPatch(p(other), n("4")), new PatchProperties.SetPatch(p(name), value))), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
            }
        }
        for (String name : List.of("height", "thickness", "indent", "endIndent")) {
            for (String value : List.of("-1", "1e999")) assertRejectedUnchanged(current, new SetProperty(DIVIDER, p(name), n(value)), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        assertRejectedUnchanged(current, new SetProperty(DIVIDER, p("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        for (String name : List.of("width", "space", "key", "borderRadius", "semanticLabel", "textDirection", "createBorderSide")) {
            assertRejectedUnchanged(current, new SetProperty(DIVIDER, p(name), n("1")), DesignerCommandDiagnosticCode.PROPERTY_UNKNOWN);
        }
        assertExactPair(configured, current.redo().session());
    }

    @Test
    void movingRemovingReplacingAndWrappingLeafUseExistingAnyWidgetSlotAuthority() throws Exception {
        var initial = add(openDefault());
        var moved = applied(initial, new MoveWidget(DIVIDER, new WidgetPlacement(ROOT, CHILDREN, 0)));
        assertEquals(List.of(DIVIDER, FIRST, SECOND), rootChildren(moved).stream().map(WidgetNode::id).toList());
        assertExactPair(initial, moved.undo().session());
        var wrapperId = StableId.random();
        var wrapper = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets.Directionality")).orElseThrow(), wrapperId);
        var wrapped = applied(moved, new WrapWidget(DIVIDER, wrapper, CHILD, 0));
        assertRejectedUnchanged(wrapped, new RemoveWidget(DIVIDER), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        assertRejectedUnchanged(wrapped, new MoveWidget(DIVIDER, new WidgetPlacement(ROOT, CHILDREN, 0)), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        var replacement = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random(), Map.of(p("radius"), radius(true, false), p("thickness"), n("2")));
        var replaced = applied(wrapped, new ReplaceSlotChild(wrapperId, CHILD, DIVIDER, new ReplaceSlotChild.NewSubtree(replacement)));
        assertEquals(replacement, find(replaced, replacement.id()));
        assertExactPair(wrapped, replaced.undo().session());
        assertExactPair(replaced, reopen(replaced));
        var removed = applied(initial, new RemoveWidget(DIVIDER));
        assertEquals(List.of(FIRST, SECOND), rootChildren(removed).stream().map(WidgetNode::id).toList());
        assertExactPair(initial, removed.undo().session());
        var malformed = new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(CHILD, WidgetSlot.SingleSlot.empty()));
        assertRejectedUnchanged(initial, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 0), malformed), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
    }

    @Test
    void constructorAcceptedIndependentDimensionsAndHairlineRadiusAreNotSilentlyRewritten() throws Exception {
        var initial = add(openDefault());
        var values = Map.<PropertyName, PropertyValue>of(p("height"), n("1"), p("thickness"), n("20"), p("indent"), n("1000"), p("endIndent"), n("2000"), p("radius"), radius(true, false));
        var configured = applied(initial, new PatchProperties(DIVIDER, values.entrySet().stream().<PatchProperties.Patch>map(e -> new PatchProperties.SetPatch(e.getKey(), e.getValue())).toList()));
        assertEquals(values, find(configured, DIVIDER).properties());
        var hairline = applied(configured, new SetProperty(DIVIDER, p("thickness"), n("0")));
        assertEquals(radius(true, false), find(hairline, DIVIDER).properties().get(p("radius")));
        assertEquals(n("0"), find(hairline, DIVIDER).properties().get(p("thickness")));
        assertExactPair(hairline, reopen(hairline));
    }

    private static Map<PropertyName, PropertyValue> values(boolean directional) { return Map.of(p("height"), n("32"), p("thickness"), n("2.5"), p("indent"), n("8"), p("endIndent"), n("12"), p("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.outlineVariant")), p("radius"), radius(directional, false)); }
    private static PropertyValue.BorderRadiusValue radius(boolean directional, boolean zero) { var a = r(zero ? "0" : "1", zero ? "0" : "2"); var b = r(zero ? "0" : "3", zero ? "0" : "4"); var c = r(zero ? "0" : "5", zero ? "0" : "6"); var d = r(zero ? "0" : "7", zero ? "0" : "8"); return new PropertyValue.BorderRadiusValue(directional ? new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(a,b,c,d) : new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(a,b,c,d)); }
    private static PropertyValue.BoxDecorationValue.Radius r(String x, String y) { return new PropertyValue.BoxDecorationValue.Radius(new BigDecimal(x), new BigDecimal(y)); }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.DoubleValue n(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static DesignerCommandSession add(DesignerCommandSession current) { return applied(current, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 2), WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), DIVIDER))); }
    private static List<WidgetNode> rootChildren(DesignerCommandSession session) {
        return ((WidgetSlot.ListSlot) session.current().document().root().slots().get(CHILDREN)).children();
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
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
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
