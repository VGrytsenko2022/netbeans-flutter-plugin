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

class ImageIconCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.ImageIcon");
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId IMAGE_ID = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final PropertyName IMAGE = new PropertyName("image");

    @Test
    void assetFreeCreationAddsRealNullIconThenSaveReopenAllowsAssetAndNoneEditing() throws Exception {
        var initial = openDefault();
        var added = add(initial);
        assertEquals(1, added.cursor());
        assertEquals(Map.of(IMAGE, new PropertyValue.NullValue()), find(added, IMAGE_ID).properties());
        assertTrue(source(added).contains("ImageIcon(null)"));
        assertFalse(source(added).contains("MemoryImage"));
        assertExactPair(initial, added.undo().session());
        assertExactPair(added, added.undo().session().redo().session());
        var reopened = reopen(added);
        assertFalse(reopened.dirty());
        var selected = applied(reopened, new SetProperty(IMAGE_ID, IMAGE, PropertyValue.ImageProviderValue.asset("assets/icon.png")));
        assertTrue(source(selected).contains("AssetImage('assets/icon.png')"));
        assertExactPair(selected, reopen(selected));
        var cleared = applied(reopen(selected), new SetProperty(IMAGE_ID, IMAGE, new PropertyValue.NullValue()));
        assertEquals(find(added, IMAGE_ID), find(cleared, IMAGE_ID));
        assertFalse(source(cleared).contains("AssetImage"));
        assertExactPair(cleared, reopen(cleared));
    }

    @Test
    void requiredNullableImageRejectsOmissionAndAtomicPatchRollbackKeepsRedo() throws Exception {
        var base = add(openDefault());
        var configured = applied(base, new SetProperty(IMAGE_ID, IMAGE, PropertyValue.ImageProviderValue.asset("assets/icon.png")));
        var withRedo = configured.undo().session();
        assertTrue(withRedo.canRedo());
        assertRejectedUnchanged(withRedo, new ResetProperty(IMAGE_ID, IMAGE), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        assertRejectedUnchanged(withRedo, new PatchProperties(IMAGE_ID, List.of(
                new PatchProperties.SetPatch(new PropertyName("size"), number("42")),
                new PatchProperties.ResetPatch(IMAGE))), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        for (PropertyValue invalid : List.of(new PropertyValue.StringValue("assets/icon.png"), new PropertyValue.BooleanValue(false),
                number("1"), new PropertyValue.DartExpressionValue("NetworkImage('https://example.com/icon.png')"))) {
            assertRejectedUnchanged(withRedo, new PatchProperties(IMAGE_ID, List.of(
                    new PatchProperties.SetPatch(new PropertyName("size"), number("42")),
                    new PatchProperties.SetPatch(IMAGE, invalid))), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        for (String name : List.of("useOriginalColors", "key", "fit", "opacity", "applyTextScaling", "shadows")) {
            assertRejectedUnchanged(withRedo, new SetProperty(IMAGE_ID, new PropertyName(name), new PropertyValue.BooleanValue(true)),
                    DesignerCommandDiagnosticCode.PROPERTY_UNKNOWN);
        }
        assertExactPair(configured, withRedo.redo().session());
    }

    @Test
    void everyFieldPatchResetAndProviderVariantSurvivesReopenWithoutChangingStableNode() throws Exception {
        var initial = add(openDefault());
        for (var kind : PropertyValue.ImageProviderValue.ProviderKind.values()) for (var policy : PropertyValue.ImageProviderValue.ResizePolicy.values()) for (boolean upscale : List.of(false, true)) {
            var provider = new PropertyValue.ImageProviderValue(kind, "assets/icon.png", Optional.of("reviewed_icons"),
                    kind == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET ? Optional.of(new BigDecimal("2.5")) : Optional.empty(),
                    Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig(Optional.of(32), Optional.of(48), policy, upscale)));
            var properties = Map.<PropertyName, PropertyValue>of(IMAGE, provider,
                    new PropertyName("size"), number("32.5"),
                    new PropertyName("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")),
                    new PropertyName("semanticLabel"), new PropertyValue.StringValue("Reviewed image icon"));
            var configured = applied(initial, new PatchProperties(IMAGE_ID, properties.entrySet().stream().<PatchProperties.Patch>map(e -> new PatchProperties.SetPatch(e.getKey(), e.getValue())).toList()));
            assertEquals(2, configured.cursor());
            assertEquals(properties, find(configured, IMAGE_ID).properties());
            assertExactPair(initial, configured.undo().session());
            assertExactPair(configured, configured.undo().session().redo().session());
            var reopened = reopen(configured);
            var reset = applied(reopened, new PatchProperties(IMAGE_ID, List.of(
                    new PatchProperties.ResetPatch(new PropertyName("size")),
                    new PatchProperties.ResetPatch(new PropertyName("color")),
                    new PatchProperties.ResetPatch(new PropertyName("semanticLabel")))));
            assertEquals(Map.of(IMAGE, provider), find(reset, IMAGE_ID).properties());
            assertFalse(source(reset).contains("Theme.of(context)"));
            assertEquals(IMAGE_ID, find(reset, IMAGE_ID).id());
            assertExactPair(reset, reopen(reset));
        }
    }

    @Test
    void moveRemoveReplaceAndRequiredWrapperChildPreserveExactHistoryAndSlotRules() throws Exception {
        var initial = add(openDefault());
        var moved = applied(initial, new MoveWidget(IMAGE_ID, new WidgetPlacement(ROOT, CHILDREN, 0)));
        assertEquals(List.of(IMAGE_ID, FIRST, SECOND), rootChildren(moved).stream().map(WidgetNode::id).toList());
        assertExactPair(initial, moved.undo().session());
        var wrapperId = StableId.random();
        var wrapper = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets.IconTheme")).orElseThrow(), wrapperId);
        var wrapped = applied(moved, new WrapWidget(IMAGE_ID, wrapper, CHILD, 0));
        assertRejectedUnchanged(wrapped, new RemoveWidget(IMAGE_ID), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        assertRejectedUnchanged(wrapped, new MoveWidget(IMAGE_ID, new WidgetPlacement(ROOT, CHILDREN, 0)), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        var replacement = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random(), Map.of(IMAGE, PropertyValue.ImageProviderValue.asset("assets/other.png")));
        var replaced = applied(wrapped, new ReplaceSlotChild(wrapperId, CHILD, IMAGE_ID, new ReplaceSlotChild.NewSubtree(replacement)));
        assertEquals(replacement, find(replaced, replacement.id()));
        assertExactPair(wrapped, replaced.undo().session());
        assertExactPair(replaced, reopen(replaced));
        var removed = applied(initial, new RemoveWidget(IMAGE_ID));
        assertEquals(List.of(FIRST, SECOND), rootChildren(removed).stream().map(WidgetNode::id).toList());
        assertExactPair(initial, removed.undo().session());
        var malformed = new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of());
        assertRejectedUnchanged(initial, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 0), malformed), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
    }

    @Test
    void nullAssetExactUnresolvedThemeTransitionsRestoreFullChronologyAndImports() throws Exception {
        var states = new ArrayList<DesignerCommandSession>();
        var current = openDefault();
        states.add(current);
        current = add(current); states.add(current);
        for (PropertyValue provider : List.of(PropertyValue.ImageProviderValue.asset("assets/icon.png"),
                PropertyValue.ImageProviderValue.exactAsset("assets/icon.png", new BigDecimal("2.5")),
                PropertyValue.ImageProviderValue.unresolved(), new PropertyValue.NullValue())) {
            current = applied(current, new SetProperty(IMAGE_ID, IMAGE, provider));
            states.add(current);
            boolean unresolved = provider instanceof PropertyValue.ImageProviderValue value && value.isUnresolved();
            assertEquals(unresolved, source(current).contains("dart:convert"));
            assertEquals(unresolved, source(current).contains("MemoryImage("));
            assertExactPair(current, reopen(current));
        }
        current = applied(current, new SetProperty(IMAGE_ID, new PropertyName("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")))); states.add(current);
        current = applied(current, new ResetProperty(IMAGE_ID, new PropertyName("color"))); states.add(current);
        current = applied(current, new MoveWidget(IMAGE_ID, new WidgetPlacement(ROOT, CHILDREN, 0))); states.add(current);
        current = applied(current, new RemoveWidget(IMAGE_ID)); states.add(current);
        for (int index = states.size() - 2; index >= 0; index--) { current = current.undo().session(); assertExactPair(states.get(index), current); assertEquals(index, current.cursor()); }
        assertFalse(current.canUndo());
        for (int index = 1; index < states.size(); index++) { current = current.redo().session(); assertExactPair(states.get(index), current); assertEquals(index, current.cursor()); }
        assertFalse(current.canRedo());
    }

    @Test
    void invalidImageSizesOptionalNullAndForeignThemeRejectWithoutDirtyOrHistoryDrift() throws Exception {
        var current = add(openDefault());
        for (var entry : Map.<String, PropertyValue>of("size", number("-1"), "color", new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium")),
                "semanticLabel", new PropertyValue.BooleanValue(true)).entrySet()) {
            assertRejectedUnchanged(current, new SetProperty(IMAGE_ID, new PropertyName(entry.getKey()), entry.getValue()), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        for (String name : List.of("size", "color", "semanticLabel")) {
            assertRejectedUnchanged(current, new SetProperty(IMAGE_ID, new PropertyName(name), new PropertyValue.NullValue()), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
    }

    private static PropertyValue.DoubleValue number(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static DesignerCommandSession add(DesignerCommandSession current) { return applied(current, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 2), WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), IMAGE_ID))); }
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
