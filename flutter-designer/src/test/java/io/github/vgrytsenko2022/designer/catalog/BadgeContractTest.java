package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.canvas.payload.CanvasModelPayloadCodec;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.BadgeTestValues.*;

class BadgeContractTest {
    private static final WidgetTypeId TYPE=BadgeWidgetPropertySchema.BADGE_TYPE;
    private static final SlotName LABEL=new SlotName("label"),CHILD=new SlotName("child");
    @Test void exactFortyOneOptionalRowsAndTwoOptionalSlotsPreserveSdkOmission() {
        var definition=definition();
        assertEquals("Badge",definition.dartClassName());assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());assertTrue(definition.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.material",100,80,"Badge"),definition.palette());
        assertEquals(41,definition.properties().size());assertEquals(41,BadgeWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(2,definition.slots().size());assertEquals(2,BadgeWidgetPropertySchema.SLOT_COUNT);
        assertEquals(new ArrayList<>(BadgeWidgetPropertySchema.definitions().keySet()),definition.properties().stream().map(v->v.name().value()).toList());
        for(var property:definition.properties()){
            var hint=BadgeWidgetPropertySchema.find(property.name()).orElseThrow();
            assertFalse(hint.description().isBlank());assertEquals(DartParameter.named(hint.dartOrder(),false),property.parameter());
            assertTrue(property.creationDefault().isEmpty());
        }
        for(var slot:definition.slots()){assertEquals(DartParameter.named(slot.name().equals(LABEL)?8:9,false),slot.parameter());assertEquals(new SlotAcceptance.AnyWidget(),slot.acceptance());assertEquals(0,slot.minChildren());assertEquals(1,slot.maxChildren());}
        var prototype=WidgetNodePrototypeFactory.create(definition,StableId.random());
        assertTrue(prototype.properties().isEmpty());assertTrue(valid(prototype));assertTrue(generated(prototype).build().payload().contains("const Badge("));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(17,DesignerDocument.SCHEMA_VERSION);assertEquals(16,WidgetCatalog.API_VERSION);assertEquals(20,CanvasModelPayloadCodec.VERSION);
    }
    @Test void completeThirtyOneTextStyleLeavesReuseExactTextValueDomainsAndBinding() {
        var text=BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
        int count=0;
        for(var property:definition().properties())if(BadgeWidgetPropertySchema.isTextStyleProperty(property.name())){
            count++;var original=new PropertyName("style"+property.name().value().substring(9));
            assertEquals(text.property(original).orElseThrow().constraints(),property.constraints(),property.name().value());
            assertEquals(TextWidgetPropertySchema.find(original),BadgeWidgetPropertySchema.textStyleBinding(property.name()));
        }
        assertEquals(31,count);assertTrue(BadgeWidgetPropertySchema.textStyleBinding(p("textStyleUnknown")).isEmpty());
    }
    @Test void directAndCountConstructorsRetainZeroMaximumAndConditionalConstAndSymbolOffsets() {
        for(boolean count:List.of(false,true))for(boolean maximum:List.of(false,true)){
            if(!count&&maximum)continue;
            var values=new LinkedHashMap<PropertyName,PropertyValue>();if(count)values.put(p("count"),i(0));if(maximum)values.put(p("maxCount"),i(1));
            var root=node(values,Map.of(LABEL,WidgetSlot.SingleSlot.empty(),CHILD,WidgetSlot.SingleSlot.of(text("base"))));
            var result=generated(root);String source=result.build().payload();
            assertEquals(count,source.contains("Badge.count("));assertEquals(!count,source.contains("return const Badge("));
            assertEquals(count,source.contains("count: 0"));assertEquals(maximum,source.contains("maxCount: 1"));
            if(count){assertFalse(source.contains("label:"),source);var symbol=result.symbolOccurrences().stream().filter(v->v.symbolName().equals("count")).findFirst().orElseThrow();assertEquals("/root/properties/count",symbol.modelPath());assertEquals("count",source.substring(symbol.offset(),symbol.endOffset()));}
            assertTrue(source.contains("child: const Text("));
        }
    }
    @Test void allBooleanTristatesAndSlotPresenceModesRetainHiddenLabelAndChildInSource() {
        for(int state=0;state<3;state++)for(boolean label:List.of(false,true))for(boolean child:List.of(false,true)){
            var values=new LinkedHashMap<PropertyName,PropertyValue>();if(state>0)values.put(p("isLabelVisible"),new PropertyValue.BooleanValue(state==2));
            var slots=new LinkedHashMap<SlotName,WidgetSlot>();if(label)slots.put(LABEL,WidgetSlot.SingleSlot.of(text("retained label")));if(child)slots.put(CHILD,WidgetSlot.SingleSlot.of(text("retained child")));
            var root=node(values,slots);assertTrue(valid(root));String source=generated(root).build().payload();
            assertEquals(label,source.contains("retained label"));assertEquals(child,source.contains("retained child"));assertEquals(state>0,source.contains("isLabelVisible:"));
            assertTrue(BadgeWidgetPropertySchema.slotUnavailableReason(root,LABEL).isEmpty());
        }
    }
    @Test void fullStylePaintAndLiteralBranchesRoundTripDeterministicallyWithExactUniqueProvenance() throws Exception {
        var covered=new HashSet<PropertyName>();
        for(boolean paints:List.of(false,true))for(boolean count:List.of(false,true)){
            var values=full(paints,count);covered.addAll(values.keySet());var root=node(values,count?Map.of(CHILD,WidgetSlot.SingleSlot.of(text("base"))):Map.of(LABEL,WidgetSlot.SingleSlot.of(text("label")),CHILD,WidgetSlot.SingleSlot.of(text("base"))));
            assertTrue(valid(root));var result=generated(root);String source=result.build().payload();
            assertTrue(source.contains("textStyle: (Theme.of(context).textTheme.labelSmall ?? const TextStyle()).copyWith("),source);
            for(String required:List.of("Locale.fromSubtags(","FontFeature(","FontVariation(","Shadow(","fontFamilyFallback:","package:","debugLabel:"))assertTrue(source.contains(required),source);
            assertFalse(source.contains("textStyleFontSize:"),source);assertFalse(source.contains("return const Badge"),source);
            var ids=new HashSet<String>();for(var symbol:result.symbolOccurrences()){assertEquals(symbol.symbolName(),source.substring(symbol.offset(),symbol.endOffset()),symbol.toString());assertTrue(ids.add(symbol.id()),symbol.toString());}
            for(String property:paints?List.of("backgroundColor","textColor","textStyleForeground","textStyleBackground","textStyleShadows","textStyleDecorationColor"):List.of("backgroundColor","textColor","textStyleColor","textStyleBackgroundColor","textStyleShadows","textStyleDecorationColor"))assertTrue(result.symbolOccurrences().stream().anyMatch(v->v.modelPath().startsWith("/root/properties/"+property)),property);
            var codec=new FdDocumentCodec();var doc=document(root);var encoded=codec.encode(doc);var decoded=assertInstanceOf(FdDecodeResult.Current.class,codec.decode(encoded));assertEquals(doc,decoded.document());assertArrayEquals(encoded.copyBytes(),codec.encode(decoded.document()).copyBytes());assertEquals(result,generated(root));
        }
        assertEquals(41,covered.size());
    }
    @Test void everyIndependentStyleLeafGeneratesWithItsOwnRecordedProvenance() {
        for(var property:definition().properties())if(BadgeWidgetPropertySchema.isTextStyleProperty(property.name())){
            var values=new LinkedHashMap<PropertyName,PropertyValue>();values.put(property.name(),value(property.name().value()));
            if(property.name().value().equals("textStylePackage"))values.put(p("textStyleFontFamily"),s("Roboto"));
            var result=generated(node(values,Map.of(LABEL,WidgetSlot.SingleSlot.of(text("label")))));
            assertTrue(result.build().payload().contains("textStyle:"),property.name().value());
            assertFalse(result.build().payload().contains(property.name().value()+":"),property.name().value());
        }
    }
    @Test void twentySevenDecorationStatesKeepUnsetFalseAndCombinedTrueDistinct() {
        var names=List.of("textStyleDecorationUnderline","textStyleDecorationOverline","textStyleDecorationLineThrough");
        for(int states=0;states<27;states++){int rest=states;var values=new LinkedHashMap<PropertyName,PropertyValue>();int enabled=0;
            for(String name:names){int state=rest%3;rest/=3;if(state>0)values.put(p(name),new PropertyValue.BooleanValue(state==2));if(state==2)enabled++;}
            String source=generated(node(values,Map.of())).build().payload();
            assertEquals(states!=0,source.contains("decoration:"));assertEquals(enabled>1,source.contains("TextDecoration.combine("));
            if(states!=0&&enabled==0)assertTrue(source.contains("TextDecoration.none"),source);
        }
    }
    @Test void physicalDirectionalInsetsAndAlignmentsAndSignedOffsetAreNotFlattenedOrClamped() {
        for(var basis:PropertyValue.AlignmentGeometryValue.HorizontalBasis.values()){
            var values=new LinkedHashMap<PropertyName,PropertyValue>();values.put(p("alignment"),new PropertyValue.AlignmentGeometryValue(basis,new BigDecimal("-2.5"),new BigDecimal("3")));
            values.put(p("offset"),new PropertyValue.OffsetValue(new BigDecimal("-7.5"),new BigDecimal("-4")));
            values.put(p("largeSize"),d("-100"));values.put(p("smallSize"),i(0));
            values.put(p("padding"),basis==PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL?value("padding"):new PropertyValue.EdgeInsetsValue(BigDecimal.ONE,BigDecimal.TWO,new BigDecimal("3"),new BigDecimal("4")));
            var root=node(values,Map.of());assertTrue(valid(root));String source=generated(root).build().payload();
            assertTrue(source.contains(basis==PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL?"AlignmentDirectional(":"Alignment("),source);assertTrue(source.contains("largeSize: -100.0"),source);assertTrue(source.contains("Offset(-7.5, -4.0)"),source);
        }
    }
    @Test void numericDomainsMatchPortableCountAndSignedLargeSizeWithoutInventedCrossConstraints() {
        for(String name:List.of("smallSize","largeSize")){assertTrue(accepts(name,i(0)));assertTrue(accepts(name,d("0.25")));assertFalse(accepts(name,d("1e999")));}
        assertFalse(accepts("smallSize",d("-1")));assertTrue(accepts("largeSize",d("-1")));assertTrue(accepts("largeSize",i(-100)));
        assertTrue(accepts("count",i(0)));assertFalse(accepts("count",i(-1)));assertTrue(accepts("maxCount",i(1)));assertFalse(accepts("maxCount",i(0)));
        for(String name:List.of("count","maxCount")){assertTrue(accepts(name,new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER)));assertFalse(accepts(name,new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE))));assertFalse(accepts(name,d("1")));}
        assertTrue(valid(node(Map.of(p("count"),i(10000),p("maxCount"),i(1),p("smallSize"),d("1000"),p("largeSize"),d("-1000")),Map.of())));
    }
    @Test void countAndMaximumRelationsRejectHiddenNonemptyLabelWithoutDeletingIt() {
        assertFalse(valid(node(Map.of(p("maxCount"),i(1)),Map.of())));
        for(boolean visible:List.of(false,true)){var root=node(Map.of(p("count"),i(0),p("isLabelVisible"),new PropertyValue.BooleanValue(visible)),Map.of(LABEL,WidgetSlot.SingleSlot.of(text("keep"))));assertFalse(valid(root));assertFalse(new DartRegionGenerator().generate(document(root),BuiltInWidgetCatalog.getDefault()).successful());}
        var count=node(Map.of(p("count"),i(0)),Map.of());assertTrue(BadgeWidgetPropertySchema.slotUnavailableReason(count,LABEL).orElseThrow().contains("Clear Count"));assertTrue(BadgeWidgetPropertySchema.slotUnavailableReason(count,CHILD).isEmpty());assertTrue(BadgeWidgetPropertySchema.slotUnavailableReason(text("other"),LABEL).isEmpty());
    }
    @Test void textStylePaintAndPackageDependenciesAreExactWhileBadgeTextColorStillAllowsForeground() {
        for(var pair:List.of(List.of("textStyleColor","textStyleForeground"),List.of("textStyleBackgroundColor","textStyleBackground"))){assertFalse(valid(node(Map.of(p(pair.get(0)),theme(),p(pair.get(1)),paint()),Map.of())));}
        assertTrue(valid(node(Map.of(p("textColor"),theme(),p("textStyleForeground"),paint()),Map.of())));
        assertFalse(valid(node(Map.of(p("textStylePackage"),s("fonts")),Map.of())));
        assertTrue(valid(node(Map.of(p("textStylePackage"),s("fonts"),p("textStyleFontFamilyFallback"),s("Roboto")),Map.of())));
    }
    @Test void countAndPaintPropagateNonconstThroughAncestorsButLiteralStyleRemainsConst() {
        for(var values:List.of(Map.<PropertyName,PropertyValue>of(p("count"),i(1)),Map.<PropertyName,PropertyValue>of(p("textStyleForeground"),paint()),Map.<PropertyName,PropertyValue>of(p("textStyleFontSize"),d("12")))){
            var badge=node(values,Map.of());var center=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Center"),Map.of(),Map.of(CHILD,WidgetSlot.SingleSlot.of(badge)));
            String source=generated(center).build().payload();assertEquals(values.containsKey(p("textStyleFontSize")),source.contains("return const Center("),source);
        }
    }
    @Test void exactCapabilityRejectsRequiredDefaultNumericDomainAndSlotDrift() {
        var original=definition();assertEquals(Set.of(WidgetCapability.CANVAS,WidgetCapability.CREATE,WidgetCapability.DND,WidgetCapability.PROPERTIES),BuiltInWidgetCapabilityCatalog.capabilities(original));
        assertEquals(41,BuiltInWidgetCapabilityCatalog.canvasProjection(original).orElseThrow().propertyContracts().size());
        for(String name:List.of("count","maxCount","largeSize","textStyleFontSize")){
            var properties=new ArrayList<>(original.properties());var property=original.property(p(name)).orElseThrow();
            properties.set(properties.indexOf(property),new PropertyDefinition(property.name(),DartParameter.named(property.parameter().order(),true),property.constraints(),Optional.empty()));assertNoCapabilities(properties,original.slots());
            properties.set(properties.indexOf(properties.stream().filter(v->v.name().equals(p(name))).findFirst().orElseThrow()),new PropertyDefinition(property.name(),property.parameter(),property.constraints(),Optional.of(value(name))));assertNoCapabilities(properties,original.slots());
        }
        assertNoCapabilities(original.properties(),List.of(new SlotDefinition(LABEL,DartParameter.named(8,true),SlotCardinality.SINGLE,1,1,new SlotAcceptance.AnyWidget()),original.slot(CHILD).orElseThrow()));
    }
    @Test void nullExpressionsUnknownConstructorHelpersAndFlexParentDataAreRejected() {
        for(var property:definition().properties())for(var bad:List.of(new PropertyValue.NullValue(),new PropertyValue.DartExpressionValue("anything()"))){var values=new LinkedHashMap<PropertyName,PropertyValue>();values.put(property.name(),bad);assertFalse(valid(node(values,Map.of())),property.name().value());}
        for(String name:List.of("variant","label","child","textStyle","key","merge"))assertFalse(valid(node(Map.of(p(name),s("bad")),Map.of())));
        var spacer=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Spacer")).orElseThrow(),StableId.random());
        for(var slot:List.of(LABEL,CHILD))assertFalse(valid(node(Map.of(),Map.of(slot,WidgetSlot.SingleSlot.of(spacer)))));
    }
    private static void assertNoCapabilities(List<PropertyDefinition> properties,List<SlotDefinition> slots){var o=definition();var changed=new WidgetDefinition(TYPE,o.dartClassName(),Optional.empty(),true,o.dartLibraryUri(),o.importUris(),o.traits(),o.palette(),properties,slots);assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty());}
    private static WidgetDefinition definition(){return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();}
    private static boolean accepts(String name,PropertyValue value){return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v->v.accepts(value));}
    private static WidgetNode node(Map<PropertyName,PropertyValue> values,Map<SlotName,WidgetSlot> slots){return new WidgetNode(StableId.random(),TYPE,values,slots);}
    private static WidgetNode text(String data){return new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Text"),Map.of(p("data"),s(data)),Map.of());}
    private static boolean valid(WidgetNode root){return new WidgetTreeValidator().validate(document(root),BuiltInWidgetCatalog.getDefault()).valid();}
    private static DesignerDocument document(WidgetNode root){var region=new ManagedRegion("0".repeat(64));return new DesignerDocument(StableId.parse("2af55f57-c70c-424f-8413-4c4985e1b02b"),new DartSourceDescriptor("sample.dart","Sample",WidgetClassKind.STATELESS,Optional.empty(),new ManagedRegions(region,region)),root);}
    private static GeneratedDartRegions generated(WidgetNode root){var result=new DartRegionGenerator().generate(document(root),BuiltInWidgetCatalog.getDefault());assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow();}
}
