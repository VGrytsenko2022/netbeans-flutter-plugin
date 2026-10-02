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

class CircleAvatarCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = CircleAvatarWidgetPropertySchema.CIRCLE_AVATAR_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId AVATAR = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD = new SlotName("child"), CHILDREN = new SlotName("children");

    @Test
    void addMoveSaveReopenAndChildReplacementRetainIdentityAndExactUndoRedo() throws Exception {
        var initial = openDefault();
        var added = add(initial);
        assertTrue(find(added, AVATAR).properties().isEmpty());
        assertTrue(source(added).contains("const CircleAvatar("));
        assertExactPair(initial, added.undo().session());
        assertExactPair(added, added.undo().session().redo().session());
        var moved = applied(added, new MoveWidget(FIRST, new WidgetPlacement(AVATAR, CHILD, 0)));
        assertEquals(FIRST, ((WidgetSlot.SingleSlot) find(moved, AVATAR).slots().get(CHILD)).child().orElseThrow().id());
        assertExactPair(added, moved.undo().session());
        var saved = reopen(moved);
        assertFalse(saved.dirty());
        var replaced = applied(saved, new ReplaceSlotChild(AVATAR, CHILD, FIRST,
                new ReplaceSlotChild.ExistingWidget(SECOND)));
        assertEquals(SECOND, ((WidgetSlot.SingleSlot) find(replaced, AVATAR).slots().get(CHILD)).child().orElseThrow().id());
        assertExactPair(saved, replaced.undo().session());
        var cleared = applied(replaced, new RemoveWidget(SECOND));
        assertFalse(find(cleared, AVATAR).slots().containsKey(CHILD));
        assertExactPair(cleared, reopen(cleared));
        assertExactPair(replaced, cleared.undo().session());
    }

    @Test
    void allNinePropertiesThroughTwoRadiusModesPersistAndResetWithoutLosingUnrelatedData() throws Exception {
        var initial = add(openDefault());
        var current = initial;
        var fields = new LinkedHashMap<PropertyName, PropertyValue>();
        fields.put(p("backgroundColor"), new PropertyValue.ColorValue(0xffaabbccL));
        fields.put(p("backgroundImage"), PropertyValue.ImageProviderValue.asset("assets/back.png"));
        fields.put(p("foregroundImage"), PropertyValue.ImageProviderValue.exactAsset("assets/front.png", new BigDecimal("2")));
        fields.put(p("onBackgroundImageError"), new PropertyValue.CallbackValue("backgroundError"));
        fields.put(p("onForegroundImageError"), new PropertyValue.CallbackValue("foregroundError"));
        fields.put(p("foregroundColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")));
        fields.put(p("radius"), d("24.5"));
        for (var entry : fields.entrySet()) {
            var before = current;
            current = applied(current, new SetProperty(AVATAR, entry.getKey(), entry.getValue()));
            assertExactPair(before, current.undo().session());
            assertExactPair(current, current.undo().session().redo().session());
        }
        current = applied(current, patch(Map.of(p("minRadius"), d("12"), p("maxRadius"), CircleAvatarWidgetPropertySchema.POSITIVE_INFINITY), Set.of(p("radius"))));
        assertEquals(initial.cursor() + 8, current.cursor());
        current = reopen(current);
        assertEquals(CircleAvatarWidgetPropertySchema.POSITIVE_INFINITY, find(current, AVATAR).properties().get(p("maxRadius")));
        for (String name : List.of("onBackgroundImageError", "onForegroundImageError", "backgroundImage", "foregroundImage",
                "foregroundColor", "backgroundColor", "minRadius", "maxRadius")) {
            var before = current;
            current = applied(current, new ResetProperty(AVATAR, p(name)));
            assertExactPair(before, current.undo().session());
        }
        assertTrue(find(current, AVATAR).properties().isEmpty());
        assertFalse(source(current).contains("dart:core"));
        assertExactPair(current, reopen(current));
    }

    @Test
    void fixedRadiusAndBoundsRequireAtomicTransitionsAndInvalidMutationsPreserveRedo() throws Exception {
        var base = add(openDefault());
        var fixed = applied(base, new SetProperty(AVATAR, p("radius"), d("20")));
        assertRejectedUnchanged(fixed, new SetProperty(AVATAR, p("minRadius"), d("10")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var bounded = applied(fixed, patch(Map.of(p("minRadius"), d("10"), p("maxRadius"), d("30")), Set.of(p("radius"))));
        assertEquals(fixed.cursor() + 1, bounded.cursor());
        assertExactPair(fixed, bounded.undo().session());
        assertRejectedUnchanged(bounded, new SetProperty(AVATAR, p("radius"), d("50")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(bounded, new SetProperty(AVATAR, p("minRadius"), d("40")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var restored = applied(bounded, patch(Map.of(p("radius"), CircleAvatarWidgetPropertySchema.POSITIVE_INFINITY), Set.of(p("minRadius"), p("maxRadius"))));
        assertExactPair(restored, reopen(restored));
        var undone = restored.undo().session();
        assertTrue(undone.canRedo());
        assertRejectedUnchanged(undone, new SetProperty(AVATAR, p("maxRadius"), d("-1")), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertExactPair(restored, undone.redo().session());
    }

    @Test
    void matchingImageCallbacksCanBeRemovedOnlyWithAtomicDependencyReset() throws Exception {
        for (String layer : List.of("Background", "Foreground")) {
            var base = add(openDefault());
            var image = p(Character.toLowerCase(layer.charAt(0)) + layer.substring(1) + "Image");
            var callback = p("on" + layer + "ImageError");
            assertRejectedUnchanged(base, new SetProperty(AVATAR, callback, new PropertyValue.CallbackValue("imageError")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
            var configured = applied(base, patch(Map.of(image, PropertyValue.ImageProviderValue.asset("assets/photo.png"),
                    callback, new PropertyValue.CallbackValue("imageError")), Set.of()));
            assertRejectedUnchanged(configured, new ResetProperty(AVATAR, image), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
            var cleared = applied(configured, patch(Map.of(), Set.of(image, callback)));
            assertEquals(configured.cursor() + 1, cleared.cursor());
            assertTrue(find(cleared, AVATAR).properties().isEmpty());
            assertExactPair(configured, cleared.undo().session());
            assertExactPair(cleared, reopen(cleared));
        }
    }

    @Test
    void nullableImageOmissionAndInfinityAreNotInterchangeableWithUnreviewedValues() throws Exception {
        var base = add(openDefault());
        for (String name : List.of("backgroundImage", "foregroundImage", "radius", "minRadius", "maxRadius")) {
            assertRejectedUnchanged(base, new SetProperty(AVATAR, p(name), new PropertyValue.NullValue()), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        for (String name : List.of("radius", "minRadius", "maxRadius")) {
            assertRejectedUnchanged(base, new SetProperty(AVATAR, p(name), new PropertyValue.StringValue("Infinity")), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
            var configured = applied(base, new SetProperty(AVATAR, p(name), CircleAvatarWidgetPropertySchema.POSITIVE_INFINITY));
            var reopened = reopen(configured);
            assertEquals(CircleAvatarWidgetPropertySchema.POSITIVE_INFINITY, find(reopened, AVATAR).properties().get(p(name)));
            var reset = applied(reopened, new ResetProperty(AVATAR, p(name)));
            assertFalse(find(reset, AVATAR).properties().containsKey(p(name)));
            assertFalse(source(reset).contains("double.infinity"));
        }
    }

    @Test
    void emptyOptionalChildIsInsertableButFullSlotAndFlexParentDataRemainProtected() throws Exception {
        var base = add(openDefault());
        var child = text(StableId.random(), "Avatar");
        var filled = applied(base, new AddWidget(new WidgetPlacement(AVATAR, CHILD, 0), child));
        assertRejectedUnchanged(filled, new AddWidget(new WidgetPlacement(AVATAR, CHILD, 0), text(StableId.random(), "Extra")), DesignerCommandDiagnosticCode.SLOT_FULL);
        var spacer = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets.Spacer")).orElseThrow(), StableId.random());
        assertRejectedUnchanged(base, new AddWidget(new WidgetPlacement(AVATAR, CHILD, 0), spacer), DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED);
        var invalid = new ReplaceSlotChild(AVATAR, CHILD, child.id(), new ReplaceSlotChild.NewSubtree(spacer));
        assertRejectedUnchanged(filled, invalid, DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED);
        var moved = applied(filled, new MoveWidget(child.id(), new WidgetPlacement(ROOT, CHILDREN, 0)));
        assertFalse(find(moved, AVATAR).slots().containsKey(CHILD));
        assertExactPair(filled, moved.undo().session());
    }

    @Test
    void infinityDoesNotChangeUserCoreImportScopeOrCoreNamedDeclarations() throws Exception {
        for (String directive : List.of("", "import 'dart:core';\n", "import 'dart:core' as userCore;\n",
                "import 'dart:core' show String, Object;\n", "import 'dart:core' hide double;\n")) {
            var initial = openDefault();
            String before = directive + source(initial) + "\nString untouched(String value, Object object) => value;\n";
            var opened = DesignerCommandSession.open(OriginalFdBytes.copyOf(initial.current().fdBytes(), FdCodecLimits.defaults()),
                    before.getBytes(StandardCharsets.UTF_8), CATALOG);
            assertTrue(opened.ready(), opened.diagnostics().toString());
            var added = add(opened.session().orElseThrow());
            var configured = applied(added, new SetProperty(AVATAR, p("radius"), CircleAvatarWidgetPropertySchema.POSITIVE_INFINITY));
            assertTrue(source(configured).startsWith(directive));
            assertTrue(source(configured).endsWith("String untouched(String value, Object object) => value;\n"));
            assertTrue(source(configured).contains("radius: (1.0 / 0.0)"));
            assertEquals(directive.isEmpty() ? 0 : 1, source(configured).split("dart:core", -1).length - 1);
            assertExactPair(configured, reopen(configured));
            var reset = applied(configured, new ResetProperty(AVATAR, p("radius")));
            assertExactPair(added, reset);
        }
    }

    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static PatchProperties patch(Map<PropertyName, PropertyValue> values, Set<PropertyName> resets) {
        List<PatchProperties.Patch> patches = new ArrayList<>();
        resets.forEach(name -> patches.add(new PatchProperties.ResetPatch(name)));
        values.forEach((name, value) -> patches.add(new PatchProperties.SetPatch(name, value)));
        return new PatchProperties(AVATAR, patches);
    }
    private static DesignerCommandSession add(DesignerCommandSession session) {
        return applied(session, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 2),
                WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), AVATAR)));
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
