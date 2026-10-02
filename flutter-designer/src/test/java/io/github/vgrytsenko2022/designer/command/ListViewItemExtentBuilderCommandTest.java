package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ListViewItemExtentBuilderCommandTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final PropertyName BUILDER = new PropertyName("itemExtentBuilder");
    private static final PropertyName FIXED = new PropertyName("itemExtent");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final String USER_BODY = "  double? _extent(int index, SliverLayoutDimensions dimensions) {\n"
            + "    // User-owned extent policy; null is only for out-of-range indexes.\n"
            + "    return index >= 2 ? null : (index == 0 ? 24.0 : 48.0);\n  }\n"
            + "  ItemExtentBuilder? get _nullableExtent => _extent;\n"
            + "  ItemExtentBuilder? _extentFactory() => _extent;\n";

    @Test void savesReopensAndResetsNullReferenceAndFactoryWithoutChangingChildrenOrUserSource() throws Exception {
        for (var kind : WidgetClassKind.values()) {
            var initial = open(kind);
            var id = initial.current().document().root().id();
            var children = children(initial);
            var explicitNull = apply(initial, new SetProperty(id, BUILDER, new PropertyValue.NullValue()));
            assertTrue(source(explicitNull).contains("itemExtentBuilder: null"));
            assertExact(explicitNull, reopen(explicitNull.markSaved()));
            var reference = apply(explicitNull, new SetProperty(id, BUILDER, reference("_nullableExtent", false)));
            var factory = apply(reference, new SetProperty(id, BUILDER, reference("_extentFactory", true)));
            assertTrue(source(factory).contains("itemExtentBuilder: _extentFactory()"));
            assertTrue(source(factory).contains("reverse: true"));
            assertTrue(source(factory).contains("ClampingScrollPhysics"));
            assertTrue(source(factory).contains(USER_BODY));
            assertEquals(children, children(factory));
            assertExact(reference, factory.undo().session());
            assertExact(factory, factory.undo().session().redo().session());
            assertExact(factory, reopen(factory.markSaved()));
            var reset = apply(factory, new ResetProperty(id, BUILDER));
            assertExact(initial, reset);
            assertExact(factory, reset.undo().session());
        }
    }

    @Test void sizingConflictsRejectInBothDirectionsAndPreserveSourceSelectionAndRedoUntilExplicitReset() throws Exception {
        var initial = open(WidgetClassKind.STATELESS);
        var id = initial.current().document().root().id();
        var extent = new PropertyValue.IntegerValue(BigInteger.valueOf(40));
        var fixed = apply(initial, new SetProperty(id, FIXED, extent));
        for (boolean factory : List.of(false, true)) {
            var reference = reference(factory ? "_extentFactory" : "_nullableExtent", factory);
            var rejected = fixed.apply(new SetProperty(id, BUILDER, reference));
            assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
            assertSame(fixed, rejected.session());
            assertExact(fixed, rejected.session());
            assertTrue(rejected.diagnostics().toString().contains("Reset itemExtent"));
            var nullBuilder = apply(fixed, new SetProperty(id, BUILDER, new PropertyValue.NullValue()));
            assertTrue(source(nullBuilder).contains("itemExtent: 40"));
            assertTrue(source(nullBuilder).contains("itemExtentBuilder: null"));
            var undone = nullBuilder.undo().session();
            var rejectedWithRedo = undone.apply(new SetProperty(id, BUILDER, reference));
            assertSame(undone, rejectedWithRedo.session());
            assertTrue(rejectedWithRedo.session().canRedo());
            assertExact(nullBuilder, rejectedWithRedo.session().redo().session());
            var noFixed = apply(nullBuilder, new ResetProperty(id, FIXED));
            var bound = apply(noFixed, new SetProperty(id, BUILDER, reference));
            assertFalse(source(bound).contains("itemExtent:"));
            var reverseRejected = bound.apply(new SetProperty(id, FIXED, extent));
            assertNotEquals(DesignerCommandStatus.APPLIED, reverseRejected.status());
            assertSame(bound, reverseRejected.session());
            assertTrue(source(reverseRejected.session()).contains(USER_BODY));
            var removedBuilder = apply(bound, new ResetProperty(id, BUILDER));
            assertExact(fixed, apply(removedBuilder, new SetProperty(id, FIXED, extent)));
        }
    }

    @Test void buildersAreNotEventsOrStateAndInvalidLegacyValuesNeverAlterHistory() throws Exception {
        var initial = open(WidgetClassKind.STATEFUL);
        var id = initial.current().document().root().id();
        var bound = apply(initial, new SetProperty(id, BUILDER, reference("_extent", false)));
        for (DesignerCommand command : List.of(new CreateEventHandler(id, BUILDER, "_newExtent"),
                new RenameEventHandler(id, BUILDER, "_renamed"),
                new BindPropertyToState(id, BUILDER, new StatePropertyBinding("_flag", StateBinding.Type.BOOL,
                        Optional.empty(), StatePropertyBinding.Transform.DIRECT)))) {
            var rejected = bound.apply(command);
            assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
            assertSame(bound, rejected.session());
            assertExact(bound, rejected.session());
        }
        var undone = bound.undo().session();
        for (PropertyValue invalid : List.of(new PropertyValue.CallbackValue("_extent"), new PropertyValue.BooleanValue(false),
                new PropertyValue.StringValue("_extent"), new PropertyValue.DartExpressionValue("(index, dimensions) => 42.0"))) {
            var rejected = undone.apply(new SetProperty(id, BUILDER, invalid));
            assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
            assertSame(undone, rejected.session());
            assertTrue(rejected.session().canRedo());
            assertExact(bound, rejected.session().redo().session());
        }
    }

    @Test void childAddMoveRemoveRemainAtomicWithBuilderAndKeepTheStaticConstructor() throws Exception {
        var initial = open(WidgetClassKind.STATELESS);
        var id = initial.current().document().root().id();
        var bound = apply(initial, new SetProperty(id, BUILDER, reference("_extent", false)));
        var original = children(bound);
        var third = box(72);
        var added = apply(bound, new AddWidget(new WidgetPlacement(id, CHILDREN, 2), third));
        assertEquals(List.of(original.getFirst(), original.getLast(), third), children(added));
        var moved = apply(added, new MoveWidget(third.id(), new WidgetPlacement(id, CHILDREN, 0)));
        assertEquals(List.of(third, original.getFirst(), original.getLast()), children(moved));
        assertTrue(source(moved).contains("itemExtentBuilder: _extent"));
        assertFalse(source(moved).contains("ListView.builder("));
        assertTrue(source(moved).contains(USER_BODY));
        assertExact(moved, reopen(moved.markSaved()));
        assertExact(added, moved.undo().session());
        assertExact(bound, apply(moved, new RemoveWidget(third.id())));
    }

    private static PropertyValue.DartObjectReferenceValue reference(String name, boolean factory) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(), factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); return result.session();
    }
    private static String source(DesignerCommandSession session) { return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8); }
    private static List<WidgetNode> children(DesignerCommandSession session) {
        return ((WidgetSlot.ListSlot) session.current().document().root().slots().get(CHILDREN)).children();
    }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes());
        assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session) {
        var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow();
    }
    private static WidgetNode box(int height) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("height"), new PropertyValue.IntegerValue(BigInteger.valueOf(height))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
    }
    private static DesignerCommandSession open(WidgetClassKind kind) throws Exception {
        var id = StableId.random();
        var root = new WidgetNode(StableId.random(), ListViewWidgetPropertySchema.LIST_VIEW_TYPE,
                Map.of(new PropertyName("reverse"), new PropertyValue.BooleanValue(true), new PropertyName("physics"), new PropertyValue.StringValue("clamping")),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(box(24), box(48)))));
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var generation = new DartRegionGenerator().generate(provisional, CATALOG);
        assertTrue(generation.successful(), generation.diagnostics().toString());
        var generated = generation.generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n"
                : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n  @override\n  State<Sample> createState() => SampleState();\n}\nclass SampleState extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\nimport 'package:flutter/rendering.dart';\n\n" + owner
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n  bool _flag = false;\n" + USER_BODY + "}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
