package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CardContractTest {
    private static final WidgetTypeId TYPE = CardWidgetPropertySchema.CARD_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    private static final List<String> VARIANTS = List.of("elevated", "filled", "outlined");
    @Test void exactFullSchemaUsesRequiredDesignerVariantAndOptionalSdkFieldsAndChild() {
        var definition = definition();
        assertEquals("Card", definition.dartClassName()); assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty()); assertTrue(definition.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.material",100,70,"Card"), definition.palette());
        assertEquals(31, definition.properties().size()); assertEquals(31, CardWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, definition.slots().size()); assertEquals(1, CardWidgetPropertySchema.SLOT_COUNT);
        assertEquals(DartParameter.named(8,false), definition.slot(CHILD).orElseThrow().parameter());
        assertEquals(new SlotAcceptance.AnyWidget(), definition.slot(CHILD).orElseThrow().acceptance());
        assertEquals(new ArrayList<>(CardWidgetPropertySchema.definitions().keySet()), definition.properties().stream().map(v -> v.name().value()).toList());
        for (var property : definition.properties()) {
            var hint=CardWidgetPropertySchema.find(property.name()).orElseThrow();
            assertFalse(hint.description().isBlank());
            assertEquals(DartParameter.named(hint.dartOrder(),property.name().value().equals("variant")),property.parameter());
            assertEquals(property.name().value().equals("variant"), property.creationDefault().isPresent());
        }
        var prototype=WidgetNodePrototypeFactory.create(definition,StableId.random());
        assertEquals(Map.of(p("variant"),s("elevated")),prototype.properties()); assertTrue(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(13,DesignerDocument.SCHEMA_VERSION);assertEquals(14,WidgetCatalog.API_VERSION);assertEquals(18,CanvasModelPayloadCodec.VERSION);
    }
    @Test void exactCapabilityRejectsRequiredDefaultDomainTypeAndSlotDrift() {
        var original=definition();assertEquals(Set.of(WidgetCapability.CANVAS,WidgetCapability.CREATE,WidgetCapability.DND,WidgetCapability.PROPERTIES),BuiltInWidgetCapabilityCatalog.capabilities(original));
        assertEquals(31,BuiltInWidgetCapabilityCatalog.canvasProjection(original).orElseThrow().propertyContracts().size());
        for(String name:List.of("variant","shape","shapeRadius","shapePoints","shapeStartAlignment")) {
            var properties=new ArrayList<>(original.properties());var property=original.property(p(name)).orElseThrow();
            List<PropertyValueConstraint> constraint=name.equals("shape")?List.of(new PropertyValueConstraint.DartObjectReferenceValues("Object")):name.equals("shapeRadius")?List.of(new PropertyValueConstraint.BorderRadiusValues(false)):name.equals("variant")?property.constraints():List.of(new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO,true,BigDecimal.ONE,true));
            var changed=new PropertyDefinition(property.name(),name.equals("variant")?DartParameter.named(9,false):property.parameter(),constraint,Optional.empty());properties.set(properties.indexOf(property),changed);assertNoCapability(properties,original.slots());
        }
        var properties=new ArrayList<>(original.properties());var variant=original.property(p("variant")).orElseThrow();properties.set(properties.indexOf(variant),new PropertyDefinition(variant.name(),variant.parameter(),variant.constraints(),Optional.of(s("filled"))));assertNoCapability(properties,original.slots());
        assertNoCapability(original.properties(),List.of(new SlotDefinition(CHILD,DartParameter.named(8,true),SlotCardinality.SINGLE,1,1,new SlotAcceptance.AnyWidget())));
    }
    private static void assertNoCapability(List<PropertyDefinition> properties,List<SlotDefinition> slots){var original=definition();var changed=new WidgetDefinition(TYPE,original.dartClassName(),Optional.empty(),true,original.dartLibraryUri(),original.importUris(),original.traits(),original.palette(),properties,slots);assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty());}
    @Test void threeConstConstructorsPreserveThemeOmissionAndExplicitBooleans() {
        for(String variant:VARIANTS) for(int state=0;state<9;state++) {
            var values=props(variant,null);int cursor=state;
            for(String name:List.of("borderOnForeground","semanticContainer")) { int digit=cursor%3;cursor/=3;if(digit>0)values.put(p(name),new PropertyValue.BooleanValue(digit==2)); }
            String source=generated(node(values)).build().payload();
            assertTrue(source.contains("return const Card"+(variant.equals("elevated")?"":"."+variant)+"("),source);
            for(String name:List.of("borderOnForeground","semanticContainer")) assertEquals(values.containsKey(p(name)),source.contains(name+":"),source);
            for(String name:List.of("variant:","color:","shadowColor:","surfaceTintColor:","elevation:","shape:","margin:","clipBehavior:")) assertFalse(source.contains(name),source);
            if(!variant.equals("elevated")) assertTrue(generated(node(values)).symbolOccurrences().stream().anyMatch(v->v.symbolName().equals(variant)&&v.modelPath().equals("/root/properties/variant")));
        }
    }
    @Test void allThirtyVariantShapeBranchesPreserveEveryApplicableLeafCodecAndExactSymbolOffsets() throws Exception {
        int count=0;
        for(String variant:VARIANTS) for(String kind:CardWidgetPropertySchema.shapeKinds()) for(boolean directional:List.of(false,true)) {
            count++;var values=props(variant,kind);
            for(String name:CardWidgetPropertySchema.builtInShapePropertyNames()) if(CardWidgetPropertySchema.isShapeDetailProperty(name)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(name,kind)) values.put(p(name),value(name,directional));
            values.put(p("color"),theme());values.put(p("shadowColor"),theme());values.put(p("surfaceTintColor"),theme());
            values.put(p("elevation"),d("3.5"));values.put(p("margin"),directional?new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE,BigDecimal.TWO,new BigDecimal("3"),new BigDecimal("4")):new PropertyValue.EdgeInsetsValue(BigDecimal.ONE,BigDecimal.TWO,new BigDecimal("3"),new BigDecimal("4")));
            values.put(p("clipBehavior"),new PropertyValue.EnumValue("Clip","antiAlias"));
            var root=node(values);assertTrue(valid(root));var generated=generated(root);String source=generated.build().payload();
            assertFalse(source.contains("return const Card"),source);assertTrue(source.contains("shape:"));
            assertFalse(source.contains("shapeKind:"));assertFalse(source.contains("variant:"));
            String owner=switch(kind){case "roundedRectangle"->"RoundedRectangleBorder";case "beveledRectangle"->"BeveledRectangleBorder";case "continuousRectangle"->"ContinuousRectangleBorder";case "roundedSuperellipse"->"RoundedSuperellipseBorder";case "circle"->"CircleBorder";case "oval"->"OvalBorder";case "stadium"->"StadiumBorder";case "linear"->"LinearBorder";default->"StarBorder";};
            assertTrue(source.contains(owner+(kind.equals("polygon")?".polygon":"")+"("),source);
            assertTrue(generated.symbolOccurrences().stream().anyMatch(v->v.symbolName().equals(owner)&&v.modelPath().equals("/root/properties/shape")));
            var ids=new HashSet<String>(); for(var symbol:generated.symbolOccurrences()){assertEquals(symbol.symbolName(),source.substring(symbol.offset(),symbol.endOffset()));assertTrue(ids.add(symbol.id()),symbol.toString());}
            for(String name:List.of("color","shadowColor","surfaceTintColor","shapeSideColor")) assertTrue(generated.symbolOccurrences().stream().anyMatch(v->v.modelPath().equals("/root/properties/"+name)),name);
            var codec=new FdDocumentCodec();var doc=document(root);var encoded=codec.encode(doc);var decoded=assertInstanceOf(FdDecodeResult.Current.class,codec.decode(encoded));assertEquals(doc,decoded.document());assertArrayEquals(encoded.copyBytes(),codec.encode(decoded.document()).copyBytes());
            assertEquals(generated,generated(root));
        }
        assertEquals(60,count);
    }
    @Test void shapeDefaultOmissionDoesNotInventBorderSideRadiusEccentricityOrLinearEdges() {
        for(String kind:CardWidgetPropertySchema.shapeKinds()) {
            String source=generated(node(props("outlined",kind))).build().payload();
            assertFalse(source.contains("BorderSide("),source);assertFalse(source.contains("borderRadius:"),source);assertFalse(source.contains("eccentricity:"),source);
            assertFalse(source.contains("LinearBorderEdge("),source);assertFalse(source.contains("points:"),source);assertFalse(source.contains("sides:"),source);
            assertTrue(source.contains("return const Card.outlined("),source);
        }
    }
    @Test void eachLinearEdgeCanBeAbsentOrExplicitlyCreatedByEitherOrBothLeaves() {
        for(int states=0;states<81;states++) {
            int remaining=states;var values=props("elevated","linear");
            for(String edge:List.of("Start","End","Top","Bottom")){int state=remaining%3;remaining/=3;if(state>0)values.put(p("shape"+edge+(state==1?"Size":"Alignment")),d(state==1?"0":"2.5"));}
            String source=generated(node(values)).build().payload();
            for(String edge:List.of("Start","End","Top","Bottom")){boolean exists=values.containsKey(p("shape"+edge+"Size"))||values.containsKey(p("shape"+edge+"Alignment"));assertEquals(exists,source.contains(Character.toLowerCase(edge.charAt(0))+edge.substring(1)+": const LinearBorderEdge("),source);}
        }
    }
    @Test void allStarRoundingStatesHonorSdkSumIncludingUnsetAndPolygonRejectsStarOnlyLeaves() {
        List<PropertyValue> values=Arrays.asList(null,d("0"),d("0.4"),d("0.6"),d("1"));
        for(var point:values)for(var valley:values){var properties=props("elevated","star");if(point!=null)properties.put(p("shapePointRounding"),point);if(valley!=null)properties.put(p("shapeValleyRounding"),valley);BigDecimal a=point==null?BigDecimal.ZERO:((PropertyValue.DoubleValue)point).value(),b=valley==null?BigDecimal.ZERO:((PropertyValue.DoubleValue)valley).value();assertEquals(a.add(b).compareTo(BigDecimal.ONE)<=0,valid(node(properties)));}
        for(String name:List.of("shapeInnerRadiusRatio","shapeValleyRounding")){var properties=props("filled","polygon");properties.put(p(name),d("0"));assertFalse(valid(node(properties)));}
    }
    @Test void numericDomainsAreFinitePortableAndRetainUncappedFractionalPointsAndAlignment() {
        for(String name:List.of("elevation","shapeSideWidth")){assertTrue(accepts(name,i(0)));assertTrue(accepts(name,d("0.25")));assertFalse(accepts(name,d("-0.1")));}
        for(String name:List.of("shapeCircleEccentricity","shapeInnerRadiusRatio","shapePointRounding","shapeValleyRounding","shapeSquash","shapeStartSize","shapeEndSize","shapeTopSize","shapeBottomSize")){for(String v:List.of("0","0.5","1"))assertTrue(accepts(name,d(v)));for(String v:List.of("-0.1","1.1"))assertFalse(accepts(name,d(v)));}
        for(String v:List.of("2","2.5","4097","10000000"))assertTrue(accepts("shapePoints",d(v)));assertFalse(accepts("shapePoints",d("1.9")));
        for(String name:List.of("shapeRotation","shapeSideStrokeAlign","shapeStartAlignment","shapeEndAlignment","shapeTopAlignment","shapeBottomAlignment"))for(String v:List.of("-720","0","12.5"))assertTrue(accepts(name,d(v)));
        for(var property:definition().properties())if(property.constraints().stream().anyMatch(v->v.kind()==PropertyValueKind.DOUBLE)){assertFalse(accepts(property.name().value(),d("1e999")));assertFalse(accepts(property.name().value(),new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE))));}
    }
    @Test void everyInapplicableStoredLeafAndReferenceConflictFailClosedBeforeGeneration() {
        for(String kind:CardWidgetPropertySchema.shapeKinds())for(String name:CardWidgetPropertySchema.builtInShapePropertyNames())if(CardWidgetPropertySchema.isShapeDetailProperty(name)){
            var values=props("filled",kind);values.put(p(name),value(name,false));assertEquals(CardWidgetPropertySchema.shapePropertyAppliesToKind(name,kind),valid(node(values)),kind+":"+name);
            values=props("filled",null);values.put(p(name),value(name,false));assertFalse(valid(node(values)));
        }
        for(String name:CardWidgetPropertySchema.builtInShapePropertyNames()){var values=props("filled",null);values.put(p("shape"),reference());values.put(p(name),name.equals("shapeKind")?s("roundedRectangle"):value(name,false));assertFalse(valid(node(values)));assertFalse(new DartRegionGenerator().generate(document(node(values)),BuiltInWidgetCatalog.getDefault()).successful());}
    }
    @Test void typedReferencePreservesExpectedShapeBorderProvenanceAndNonconstAncestors() {
        for(String variant:VARIANTS){var values=props(variant,null);values.put(p("shape"),reference());var root=node(values);assertTrue(valid(root));var output=generated(root);String source=output.build().payload();assertTrue(output.imports().payload().contains("package:app/shapes.dart"));assertFalse(source.contains("return const Card"));assertTrue(source.contains("makeShape()"),source);var occurrence=output.symbolOccurrences().stream().filter(v->v.symbolName().equals("makeShape")&&v.modelPath().equals("/root/properties/shape/rootSymbol")).findFirst().orElseThrow();assertEquals("ShapeBorder",occurrence.staticTypeRequirement().orElseThrow().expectedDartType());}
    }
    @Test void importedCardAndStarBorderNamesRemainPrefixedBesideNamedSdkConstructors() {
        for(String variant:List.of("filled","outlined")) {
            var children=new ArrayList<WidgetNode>();
            for(String name:List.of("Card","StarBorder")) {
                var reference=new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/shapes.dart"),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,Optional.of(true));
                children.add(new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.ClipPath"),Map.of(p("shape"),reference),Map.of()));
            }
            var row=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Row"),Map.of(),Map.of(new SlotName("children"),new WidgetSlot.ListSlot(children)));
            var root=new WidgetNode(StableId.random(),TYPE,props(variant,"polygon"),Map.of(CHILD,WidgetSlot.SingleSlot.of(row)));
            var output=generated(root);String source=output.build().payload();
            String prefix=output.importPlan().directives().stream().filter(v->v.uri().equals("package:app/shapes.dart")).findFirst().orElseThrow().prefix().orElseThrow();
            assertTrue(source.contains(prefix+".Card()"),source);assertTrue(source.contains(prefix+".StarBorder()"),source);
            for(String symbol:List.of(variant,"polygon")) {var occurrence=output.symbolOccurrences().stream().filter(v->v.symbolName().equals(symbol)).findFirst().orElseThrow();assertEquals(symbol,source.substring(occurrence.offset(),occurrence.endOffset()));assertEquals(Optional.of(root.id()),occurrence.widgetId());}
            assertTrue(source.contains("Card."+variant+"("),source);assertTrue(source.contains("StarBorder.polygon("),source);
        }
    }
    @Test void optionalChildPreservesEmptyNonemptyAndConstPropagationWithoutParentDataShortcuts() {
        var empty=node(props("filled",null));assertTrue(valid(empty));
        var child=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
        var root=new WidgetNode(empty.id(),TYPE,empty.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.of(child)));assertTrue(valid(root));assertTrue(generated(root).build().payload().contains("child: const Text("));
        var expanded=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Spacer")).orElseThrow(),StableId.random());
        assertFalse(valid(new WidgetNode(empty.id(),TYPE,empty.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.of(expanded)))));
    }
    @Test void missingVariantNullsUnknownHelpersAndWrongValueKindsCannotEnterModel() {
        assertFalse(valid(new WidgetNode(StableId.random(),TYPE,Map.of(),Map.of())));
        for(var property:definition().properties()){var values=props("elevated",null);values.put(property.name(),new PropertyValue.NullValue());assertFalse(valid(node(values)));values.put(property.name(),new PropertyValue.DartExpressionValue("anything()"));assertFalse(valid(node(values)));}
        for(String variant:List.of("Card","normal","merge","ELEVATED"))assertFalse(valid(node(props(variant,null))));
        for(String name:List.of("key","data","semanticLabel","useOriginalColors","filled","shapeSides")){var values=props("elevated",null);values.put(p(name),d("1"));assertFalse(valid(node(values)));}
    }
    private static LinkedHashMap<PropertyName,PropertyValue> props(String variant,String kind){var values=new LinkedHashMap<PropertyName,PropertyValue>();values.put(p("variant"),s(variant));if(kind!=null)values.put(p("shapeKind"),s(kind));return values;}
    private static PropertyValue value(String name,boolean directional){return switch(name){case "shapeRadius"->radius(directional);case "shapeSideColor"->theme();case "shapeSideStyle"->new PropertyValue.EnumValue("BorderStyle","solid");case "shapePoints"->d("5.5");case "shapeRotation"->d("-22.5");case "shapeSideStrokeAlign"->d("2");case "shapeSideWidth"->d("2.5");default->d("0.25");};}
    private static PropertyValue.BorderRadiusValue radius(boolean directional){var a=new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE,BigDecimal.TWO);var b=new PropertyValue.BoxDecorationValue.Radius(new BigDecimal("3"),new BigDecimal("4"));return new PropertyValue.BorderRadiusValue(directional?new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(a,b,b,a):new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(a,b,b,a));}
    private static PropertyValue.DartObjectReferenceValue reference(){return new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/shapes.dart"),"makeShape",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,Optional.of(false));}
    private static PropertyValue.ThemeTokenValue theme(){return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.outlineVariant"));}
    private static PropertyName p(String name){return new PropertyName(name);}private static PropertyValue.StringValue s(String value){return new PropertyValue.StringValue(value);}private static PropertyValue.DoubleValue d(String value){return new PropertyValue.DoubleValue(new BigDecimal(value));}private static PropertyValue.IntegerValue i(int value){return new PropertyValue.IntegerValue(BigInteger.valueOf(value));}
    private static WidgetDefinition definition(){return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();}
    private static boolean accepts(String name,PropertyValue value){return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v->v.accepts(value));}
    private static WidgetNode node(Map<PropertyName,PropertyValue> values){return new WidgetNode(StableId.random(),TYPE,values,Map.of());}
    private static boolean valid(WidgetNode node){return new WidgetTreeValidator().validate(document(node),BuiltInWidgetCatalog.getDefault()).valid();}
    private static DesignerDocument document(WidgetNode root){var region=new ManagedRegion("0".repeat(64));return new DesignerDocument(StableId.parse("2af55f57-c70c-424f-8413-4c4985e1b02b"),new DartSourceDescriptor("sample.dart","Sample",WidgetClassKind.STATELESS,Optional.empty(),new ManagedRegions(region,region)),root);}
    private static GeneratedDartRegions generated(WidgetNode root){var result=new DartRegionGenerator().generate(document(root),BuiltInWidgetCatalog.getDefault());assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow();}
}
