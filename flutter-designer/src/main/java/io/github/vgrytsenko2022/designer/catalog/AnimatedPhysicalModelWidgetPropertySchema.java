package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;
/** Complete non-key Flutter 3.44.8 AnimatedPhysicalModel constructor. */
public final class AnimatedPhysicalModelWidgetPropertySchema {
 public static final WidgetTypeId TYPE=new WidgetTypeId("flutter.widgets.AnimatedPhysicalModel");
 public static final String DESCRIPTION="Animates physical corner radii, elevation and optional fill/shadow colors. Shape and clipping update immediately. Required Child: wrap an existing box child. Canvas never executes project references.";
 public record Field(String name,String label,String description){}
 public static final List<Field> FIELDS=List.of(
   new Field("shape","Shape","Rectangle or circle. Changes immediately; circle ignores but retains the corner radii."),
   new Field("clipBehavior","Clip behavior","All native Clip modes. Changes immediately; omitted means none."),
   new Field("borderRadius","Border radius","Physical elliptical corners, explicit null or typed BorderRadius? reference. Omitted/null is animated toward zero. Directional radii are not accepted by this constructor."),
   new Field("elevation","Elevation","Finite nonnegative logical pixels or typed double reference; omitted means zero. Animates with the selected curve."),
   new Field("color","Color","Required literal/theme color or typed Color reference. Creation: blue. Animate color controls interpolation, not storage."),
   new Field("animateColor","Animate color","Native boolean, default true. False applies Color immediately; the SDK still tracks its tween and completion."),
   new Field("shadowColor","Shadow color","Required literal/theme color or typed Color reference. Creation: opaque black."),
   new Field("animateShadowColor","Animate shadow color","Native boolean, default true. False applies Shadow color immediately without discarding the native tween."),
   new Field("curve","Curve","All 43 pinned Curves presets or typed Curve reference; default linear. Unsafe overshooting elevation is reported by Canvas without rewriting source."),
   new Field("durationUs","Duration (microseconds)","Required nonnegative portable integer microseconds or typed Duration reference. Creation/custom-reference preview: 300000; zero completes immediately."),
   new Field("onEnd","On end","Optional native VoidCallback reference/factory or null. Use Events. No completion on initial mount; Canvas never executes project handlers."));
 public static List<PropertyDefinition> properties(){
   var animation=AnimatedOpacityWidgetPropertySchema.properties();
   return List.of(
    p("shape",0,false,List.of(en("BoxShape","rectangle","circle")),null),
    p("clipBehavior",1,false,List.of(en("Clip","none","hardEdge","antiAlias","antiAliasWithSaveLayer")),null),
    p("borderRadius",2,false,List.of(new PropertyValueConstraint.BorderRadiusValues(false),new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL),new PropertyValueConstraint.DartObjectReferenceValues("BorderRadius?")),null),
    p("elevation",3,false,List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO,new BigInteger("9007199254740991")),new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO,true,null,true),new PropertyValueConstraint.DartObjectReferenceValues("double")),null),
    color("color",4,0xFF2196F3L),
    p("animateColor",5,false,List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)),null),
    color("shadowColor",6,0xFF000000L),
    p("animateShadowColor",7,false,List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)),null),
    at(animation.get(1),8),at(animation.get(2),9),at(animation.get(3),10));
 }
 private static PropertyDefinition color(String name,int i,long value){return p(name,i,true,List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR),
   new PropertyValueConstraint.ThemeTokenValues(MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()),
   new PropertyValueConstraint.DartObjectReferenceValues("Color")),new PropertyValue.ColorValue(value));}
 private static PropertyValueConstraint.EnumValues en(String type,String... values){return new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart",type),List.of(values));}
 private static PropertyDefinition p(String name,int i,boolean required,List<PropertyValueConstraint> constraints,PropertyValue value){return new PropertyDefinition(new PropertyName(name),DartParameter.named(i,required),constraints,Optional.ofNullable(value));}
 private static PropertyDefinition at(PropertyDefinition p,int i){return new PropertyDefinition(p.name(),DartParameter.named(i,p.parameter().required()),p.constraints(),p.creationDefault());}
 private AnimatedPhysicalModelWidgetPropertySchema(){}
}

