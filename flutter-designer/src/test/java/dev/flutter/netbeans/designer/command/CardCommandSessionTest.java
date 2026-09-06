package dev.flutter.netbeans.designer.command;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CardCommandSessionTest {
    private static final WidgetCatalog CATALOG=BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE=CardWidgetPropertySchema.CARD_TYPE;
    private static final StableId ROOT=StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST=StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND=StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId CARD=StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD=new SlotName("child"),CHILDREN=new SlotName("children");

    @Test void creationAllThreeVariantsSaveReopenAndSubsequentEditsKeepExplicitRequiredMode() throws Exception {
        var initial=openDefault();var current=add(initial);
        assertEquals(Map.of(p("variant"),s("elevated")),find(current,CARD).properties());
        assertExactPair(initial,current.undo().session());assertExactPair(current,current.undo().session().redo().session());
        for(String variant:List.of("filled","outlined","elevated")){
            var before=current;current=applied(current,new SetProperty(CARD,p("variant"),s(variant)));
            assertTrue(source(current).contains("Card"+(variant.equals("elevated")?"":"."+variant)+"("));
            assertFalse(source(current).contains("variant:"));
            assertExactPair(before,current.undo().session());assertExactPair(current,current.undo().session().redo().session());
            current=reopen(current);assertFalse(current.dirty());
            current=applied(current,new SetProperty(FIRST,p("data"),s("Card "+variant)));
        }
        assertEquals(CARD,find(current,CARD).id());
        assertRejectedUnchanged(current,new ResetProperty(CARD,p("variant")),DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
    }
    @Test void tenShapeFamiliesPatchAsOneHistoryUnitThenResetAfterReopen() throws Exception {
        for(String kind:CardWidgetPropertySchema.shapeKinds()){
            var initial=add(openDefault());var values=shapeValues(kind);
            var configured=applied(initial,patch(values,Set.of()));
            assertEquals(initial.cursor()+1,configured.cursor());assertExactPair(initial,configured.undo().session());assertExactPair(configured,configured.undo().session().redo().session());
            var current=reopen(configured);
            var names=new ArrayList<>(values.keySet());names.remove(p("shapeKind"));
            for(var name:names){var before=current;current=applied(current,new ResetProperty(CARD,name));assertFalse(find(current,CARD).properties().containsKey(name));assertExactPair(before,current.undo().session());assertExactPair(current,current.undo().session().redo().session());}
            current=applied(current,new ResetProperty(CARD,p("shapeKind")));
            assertEquals(Map.of(p("variant"),s("elevated")),find(current,CARD).properties());assertExactPair(current,reopen(current));
        }
    }
    @Test void shapeKindChangesRequireAtomicPruningAndPreserveCommonSidesAcrossHistory() throws Exception {
        var base=add(openDefault());var rounded=applied(base,patch(shapeValues("roundedRectangle"),Set.of()));
        assertRejectedUnchanged(rounded,new SetProperty(CARD,p("shapeKind"),s("circle")),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var circle=applied(rounded,patch(Map.of(p("shapeKind"),s("circle"),p("shapeCircleEccentricity"),d("0.75")),Set.of(p("shapeRadius"))));
        assertEquals(find(rounded,CARD).properties().get(p("shapeSideColor")),find(circle,CARD).properties().get(p("shapeSideColor")));
        assertExactPair(rounded,circle.undo().session());assertExactPair(circle,circle.undo().session().redo().session());
        var star=applied(circle,patch(Map.of(p("shapeKind"),s("star"),p("shapePoints"),d("5.5"),p("shapeValleyRounding"),d("0.4")),Set.of(p("shapeCircleEccentricity"))));
        assertRejectedUnchanged(star,new SetProperty(CARD,p("shapeKind"),s("polygon")),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var polygon=applied(star,patch(Map.of(p("shapeKind"),s("polygon")),Set.of(p("shapeValleyRounding"))));
        assertTrue(source(polygon).contains("StarBorder.polygon("));assertTrue(source(polygon).contains("sides: 5.5"));assertExactPair(polygon,reopen(polygon));
    }
    @Test void typedShapeReferenceReplacesBuiltinsAtomicallyAndRoundTripsWithoutExecutingIt() throws Exception {
        var initial=applied(add(openDefault()),patch(shapeValues("linear"),Set.of()));
        var reference=new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/shapes.dart"),"makeShape",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,Optional.of(false));
        assertRejectedUnchanged(initial,new SetProperty(CARD,p("shape"),reference),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var names=new HashSet<PropertyName>();for(String name:CardWidgetPropertySchema.builtInShapePropertyNames())if(find(initial,CARD).properties().containsKey(p(name)))names.add(p(name));
        var changed=applied(initial,patch(Map.of(p("shape"),reference),names));assertTrue(source(changed).contains("makeShape()"));assertFalse(source(changed).contains("LinearBorder("));assertExactPair(initial,changed.undo().session());assertExactPair(changed,reopen(changed));
        var builtin=applied(changed,patch(shapeValues("oval"),Set.of(p("shape"))));assertFalse(source(builtin).contains("makeShape"));assertTrue(source(builtin).contains("OvalBorder("));assertExactPair(builtin,reopen(builtin));
    }
    @Test void allSdkFieldsResetAndTransparentZeroFalseRemainExplicit() throws Exception {
        var initial=add(openDefault());var values=new LinkedHashMap<PropertyName,PropertyValue>();
        for(String name:List.of("color","shadowColor","surfaceTintColor"))values.put(p(name),new PropertyValue.ColorValue(0));
        values.put(p("elevation"),d("0"));values.put(p("borderOnForeground"),new PropertyValue.BooleanValue(false));values.put(p("semanticContainer"),new PropertyValue.BooleanValue(false));
        values.put(p("clipBehavior"),new PropertyValue.EnumValue("Clip","none"));values.put(p("margin"),new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO));
        var current=applied(initial,patch(values,Set.of()));assertTrue(source(current).contains("borderOnForeground: false"));assertTrue(source(current).contains("semanticContainer: false"));assertTrue(source(current).contains("elevation: 0.0"));
        current=reopen(current);for(var name:values.keySet())current=applied(current,new ResetProperty(CARD,name));
        assertEquals(Map.of(p("variant"),s("elevated")),find(current,CARD).properties());assertExactPair(current,reopen(current));
    }
    @Test void optionalChildMoveRemoveReorderAndUndoRedoPreserveBothSourceAndFd() throws Exception {
        var states=new ArrayList<DesignerCommandSession>();var current=openDefault();states.add(current);
        current=add(current);states.add(current);
        current=applied(current,new MoveWidget(FIRST,new WidgetPlacement(CARD,CHILD,0)));states.add(current);
        assertTrue(source(current).contains("child: const Text("));assertEquals(FIRST,((WidgetSlot.SingleSlot)find(current,CARD).slots().get(CHILD)).child().orElseThrow().id());
        current=applied(current,new SetProperty(FIRST,p("data"),s("Inside card")));states.add(current);
        current=applied(current,new MoveWidget(FIRST,new WidgetPlacement(ROOT,CHILDREN,0)));states.add(current);
        current=applied(current,new MoveWidget(CARD,new WidgetPlacement(ROOT,CHILDREN,0)));states.add(current);
        current=applied(current,new RemoveWidget(CARD));states.add(current);assertExactPair(current,reopen(current));
        for(int index=states.size()-2;index>=0;index--){current=current.undo().session();assertExactPair(states.get(index),current);}
        for(int index=1;index<states.size();index++){current=current.redo().session();assertExactPair(states.get(index),current);}
    }
    @Test void invalidPatchAndMissingVariantRetainRedoCursorDirtyAndExactPair() throws Exception {
        var configured=applied(add(openDefault()),patch(shapeValues("star"),Set.of()));var current=configured.undo().session();assertTrue(current.canRedo());
        assertRejectedUnchanged(current,new SetProperty(CARD,p("shapePoints"),d("3")),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(current,new PatchProperties(CARD,List.of(new PatchProperties.SetPatch(p("variant"),s("outlined")),new PatchProperties.SetPatch(p("elevation"),d("-1")))),DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        var star=configured;
        assertRejectedUnchanged(star,new SetProperty(CARD,p("shapePointRounding"),d("0.9")),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(star,new ResetProperty(CARD,p("shapeKind")),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(star,new ResetProperty(CARD,p("variant")),DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        var malformed=new WidgetNode(StableId.random(),TYPE,Map.of(),Map.of());
        assertRejectedUnchanged(current,new AddWidget(new WidgetPlacement(ROOT,CHILDREN,0),malformed),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var spacer=WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets.Spacer")).orElseThrow(),StableId.random());
        assertRejectedUnchanged(current,new AddWidget(new WidgetPlacement(CARD,CHILD,0),spacer),DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED);
    }
    private static Map<PropertyName,PropertyValue> shapeValues(String kind){
        var values=new LinkedHashMap<PropertyName,PropertyValue>();values.put(p("shapeKind"),s(kind));
        for(String name:CardWidgetPropertySchema.builtInShapePropertyNames())if(CardWidgetPropertySchema.isShapeDetailProperty(name)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(name,kind)){
            PropertyValue value=switch(name){case "shapeRadius"->new PropertyValue.BorderRadiusValue(new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(r(1,2),r(3,4),r(5,6),r(7,8)));case "shapeSideColor"->new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.outlineVariant"));case "shapeSideStyle"->new PropertyValue.EnumValue("BorderStyle","solid");case "shapePoints"->d("5.5");default->d("0.25");};values.put(p(name),value);
        }return values;
    }
    private static PropertyValue.BoxDecorationValue.Radius r(int x,int y){return new PropertyValue.BoxDecorationValue.Radius(BigDecimal.valueOf(x),BigDecimal.valueOf(y));}
    private static PropertyName p(String name){return new PropertyName(name);}private static PropertyValue.StringValue s(String value){return new PropertyValue.StringValue(value);}private static PropertyValue.DoubleValue d(String value){return new PropertyValue.DoubleValue(new BigDecimal(value));}
    private static PatchProperties patch(Map<PropertyName,PropertyValue> values,Set<PropertyName> resets){var patches=new ArrayList<PatchProperties.Patch>();resets.forEach(name->patches.add(new PatchProperties.ResetPatch(name)));values.forEach((name,value)->patches.add(new PatchProperties.SetPatch(name,value)));return new PatchProperties(CARD,patches);}
    private static DesignerCommandSession add(DesignerCommandSession session){return applied(session,new AddWidget(new WidgetPlacement(ROOT,CHILDREN,2),WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(),CARD)));}
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
