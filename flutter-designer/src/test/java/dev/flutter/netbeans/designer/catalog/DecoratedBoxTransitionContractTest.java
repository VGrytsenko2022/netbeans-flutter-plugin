package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class DecoratedBoxTransitionContractTest {
    public static final WidgetTypeId TYPE=DecoratedBoxTransitionWidgetPropertySchema.TYPE;
    public static final SlotName CHILD=new SlotName("child");
    public static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static WidgetNode node(){
        var prototype=WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());
        return new WidgetNode(prototype.id(),TYPE,prototype.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.of(
            WidgetNodePrototypeFactory.create(C.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random()))));
    }
    public static PropertyValue.BoxDecorationValue decoration(ColorSource color){
        return new PropertyValue.BoxDecorationValue(Optional.of(color),Optional.empty(),Optional.empty(),List.of(),Optional.empty(),Optional.empty(),PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }
    static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var copy=new LinkedHashMap<>(n.properties());copy.putAll(p);return new WidgetNode(n.id(),TYPE,copy,n.slots());}
    static DesignerDocument doc(WidgetNode n){return FlexibleSpaceBarContractTest.doc(n);}
    static GeneratedDartRegions generated(WidgetNode n){var r=new DartRegionGenerator().generate(doc(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
    @Test void exactNativeConstructorAndAllDestinationCompatibility(){
        var d=C.find(TYPE).orElseThrow();
        assertTrue(d.constConstructor());assertTrue(d.namedConstructor().isEmpty());
        assertEquals(List.of("decoration","position"),d.properties().stream().map(p->p.name().value()).toList());
        var decoration=d.property(new PropertyName("decoration")).orElseThrow();
        assertEquals(DartParameter.named(0,true),decoration.parameter());
        assertEquals(Set.of(PropertyValueKind.BOX_DECORATION,PropertyValueKind.DART_OBJECT_REFERENCE),decoration.acceptedKinds());
        assertEquals(DecoratedBoxTransitionWidgetPropertySchema.emptyDecoration(),decoration.creationDefault().orElseThrow());
        assertEquals(DartParameter.named(1,false),d.property(new PropertyName("position")).orElseThrow().parameter());
        assertEquals(DartParameter.named(2,true),d.slots().getFirst().parameter());
        assertEquals(1,d.slots().getFirst().minChildren());assertEquals(1,d.slots().getFirst().maxChildren());
        assertEquals(CHILD,WidgetPlacementRules.requiredWrapperSlot(d).orElseThrow().name());assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());
        var old=C.find(DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE).orElseThrow();
        for(var owner:C.definitions())for(var slot:owner.slots())
            assertEquals(WidgetPlacementRules.accepts(owner,slot,old),WidgetPlacementRules.accepts(owner,slot,d),owner.typeId()+"."+slot.name());
        for(var child:C.definitions())assertEquals(WidgetPlacementRules.accepts(old,old.slots().getFirst(),child),WidgetPlacementRules.accepts(d,d.slots().getFirst(),child));
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
    }
    @Test void localDecorationRoundTripStoppedAnimationAndConstPropagation()throws Exception{
        for(var color:List.<ColorSource>of(new ColorSource.Literal(0xff102030L),new ColorSource.Theme(new ThemeToken("material.colorScheme.primary"))))
        for(String position:List.of("background","foreground")){
            var n=with(node(),Map.of(new PropertyName("decoration"),decoration(color),new PropertyName("position"),new PropertyValue.EnumValue("DecorationPosition",position)));
            var out=generated(n);var dart=out.build().payload();
            assertTrue(dart.contains("AlwaysStoppedAnimation<Decoration>("),dart);
            assertEquals(color instanceof ColorSource.Literal,dart.contains("const DecoratedBoxTransition("),dart);
            assertEquals(color instanceof ColorSource.Literal,dart.contains("const AlwaysStoppedAnimation<Decoration>("),dart);
            assertTrue(dart.contains("DecorationPosition."+position),dart);
            assertTrue(out.symbolOccurrences().stream().anyMatch(s->s.id().endsWith(":stopped-decoration-animation")));
            assertTrue(out.symbolOccurrences().stream().anyMatch(s->s.id().endsWith(":stopped-decoration-type")));
            var codec=new FdDocumentCodec();var document=doc(n);assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
        }
        var defaults=generated(node()).build().payload();assertFalse(defaults.contains("position:"));assertTrue(defaults.contains("BoxDecoration("));
    }
    @Test void allEightReferenceShapesCarryExactAnimationEvidence()throws Exception{
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/decorations.dart"):Optional.empty(),
                member?"Decorations":"decoration",member?Optional.of("animation"):Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            var n=with(node(),Map.of(new PropertyName("decoration"),ref));var out=generated(n);
            assertFalse(out.build().payload().contains("AlwaysStoppedAnimation"));assertFalse(out.build().payload().contains("const DecoratedBoxTransition("));
            assertTrue(out.symbolOccurrences().stream().flatMap(s->s.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals("Animation<Decoration>")));
            var codec=new FdDocumentCodec();var document=doc(n);assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
        }
    }
    @Test void completeNestedImageMaintainsMultilineOffsetsAndNonConstPropagation()throws Exception{
        PropertyValue.ImageProviderValue provider =
                new PropertyValue.ImageProviderValue(
                        PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET,
                        "assets/images/logo.png",
                        Optional.empty(),
                        Optional.of(new BigDecimal("2")),
                        Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig(
                                Optional.of(512), Optional.of(256),
                                PropertyValue.ImageProviderValue.ResizePolicy.FIT,
                                true)));
        PropertyValue.DecorationImageValue image =
                new PropertyValue.DecorationImageValue(
                        provider,
                        Optional.of(new PropertyValue.CallbackValue("onImageError")),
                        Optional.of(new PropertyValue.DecorationImageValue.Mode(
                                new ColorSource.Theme(new ThemeToken(
                                        "material.colorScheme.primary")),
                                PropertyValue.PaintValue.BlendMode.SRC_IN)),
                        Optional.of(PropertyValue.DecorationImageValue.BoxFit.CONTAIN),
                        new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,new BigDecimal("0.25"),new BigDecimal("-0.5")),
                        Optional.of(new PropertyValue.DecorationImageValue.Rect(
                                BigDecimal.ONE, BigDecimal.TWO,
                                new BigDecimal("20"), new BigDecimal("30"))),
                        PropertyValue.DecorationImageValue.ImageRepeat.REPEAT_X,
                        true,
                        new BigDecimal("1.5"),
                        new BigDecimal("0.75"),
                        PropertyValue.PaintValue.FilterQuality.HIGH,
                        true,
                        true);
        PropertyValue.BoxDecorationValue decoration =
                new PropertyValue.BoxDecorationValue(
                        Optional.of(new ColorSource.Literal(0xFF010203L)),
                        Optional.of(image),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);

        var n=with(node(),Map.of(new PropertyName("decoration"),decoration));var out=generated(n);var source=out.build().payload();
        for(String symbol:List.of("AlwaysStoppedAnimation","Decoration","BoxDecoration","DecorationImage","ExactAssetImage","ResizeImage","ColorFilter","Theme"))
          assertTrue(out.symbolOccurrences().stream().anyMatch(o->o.symbolName().equals(symbol)),symbol);
        for(String token:List.of("onError: onImageError","centerSlice: const Rect.fromLTRB(1.0, 2.0, 20.0, 30.0)","matchTextDirection: true","opacity: 0.75","filterQuality: FilterQuality.high","invertColors: true","isAntiAlias: true"))
          assertTrue(source.contains(token),source);
        assertFalse(source.contains("const AlwaysStoppedAnimation<Decoration>("));
        for(var occurrence:out.symbolOccurrences()){
          var payload=occurrence.region()==DartManagedRegionId.BUILD?source:out.imports().payload();
          assertEquals(occurrence.symbolName(),payload.substring(occurrence.offset(),occurrence.endOffset()));
        }
        var codec=new FdDocumentCodec();var document=doc(n);assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
    }
    @Test void requiredChildAndDecorationFailClosedAndNoInventedParameters(){
        var n=node();var missing=new LinkedHashMap<>(n.properties());missing.remove(new PropertyName("decoration"));
        assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(n.id(),TYPE,missing,n.slots())),C).valid());
        assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(n.id(),TYPE,n.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.empty()))),C).valid());
        for(String field:List.of("decoration","position"))for(var bad:List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.StringValue("arbitraryDart"),new PropertyValue.BooleanValue(true)))
            assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName(field),bad))),C).valid());
        for(String field:List.of("durationUs","curve","onEnd","clipBehavior","padding","alignment","width"))
            assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName(field),new PropertyValue.BooleanValue(true)))),C).valid());
        assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName("position"),new PropertyValue.EnumValue("DecorationPosition","middle")))),C).valid());
    }
}
