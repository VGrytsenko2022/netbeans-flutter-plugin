package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;

/** All 23 general-constructor parameters, Flutter 3.44.8; shared Key excluded. */
public final class FadeInImageWidgetPropertySchema {
    public static final WidgetTypeId TYPE=new WidgetTypeId("flutter.widgets.FadeInImage");
    public static final String DESCRIPTION="Shows a placeholder until the target loads, then fades out/in. "
        +"All 23 constructor parameters are editable. Providers are required: declared assets or typed ImageProvider<Object> source. "
        +"Network, memory, file, bundles and custom providers belong in source factories; Canvas never runs project code or arbitrary network requests. "
        +"Error builders are Properties, not Events. Synchronously cached images skip fading.";
    public record Field(String name,String label,String group,String description) {}
    public static final List<Field> FIELDS=List.of(
        new Field("placeholder","Placeholder image","Images","Required declared asset with DPR/exact scale and ResizeImage settings, or non-null ImageProvider<Object> getter/factory. Sources may return MemoryImage, NetworkImage, FileImage or custom providers. An unconfigured local value remains an editable built-in placeholder."),
        new Field("placeholderErrorBuilder","Placeholder error builder","Images","Optional ImageErrorWidgetBuilder? source. Returns Widget from BuildContext, Object error, StackTrace? stackTrace. Null/unset reports errors to FlutterError. The builder owns replacement or rethrow; Canvas never executes it."),
        new Field("image","Target image","Images","Required declared asset/resize configuration or typed ImageProvider<Object> source. NetworkImage headers/scale, MemoryImage bytes and bundle overrides belong in source. Cached synchronous data skips the placeholder; updates preserve native gapless playback."),
        new Field("imageErrorBuilder","Target error builder","Images","Optional ImageErrorWidgetBuilder? source, independent of placeholder errors. Create/bind/navigate/rename/disconnect through the callable editor. Canvas uses an explicit safe error preview instead of executing project code."),
        new Field("excludeFromSemantics","Exclude from semantics","Accessibility","Optional checkbox; omission is false. True suppresses the image semantic node and ignores its label."),
        new Field("imageSemanticLabel","Image semantic label","Accessibility","Optional string, including empty, null or unset. One label describes placeholder and target; internal images are excluded to avoid duplicate announcements."),
        new Field("fadeOutDurationUs","Fade-out duration (microseconds)","Animation","Optional Duration or local microseconds, default 300000. Each phase needs at least 1000 microseconds: Flutter TweenSequence uses positive millisecond weights. Zero/sub-millisecond phases fail native assertions."),
        new Field("fadeOutCurve","Fade-out curve","Animation","All 43 Curves presets or non-null Curve reference. Omission/source preview uses easeOut."),
        new Field("fadeInDurationUs","Fade-in duration (microseconds)","Animation","Optional Duration or local microseconds, default 700000. At least 1000 microseconds per phase. Phases run sequentially. Project Duration previews 700000."),
        new Field("fadeInCurve","Fade-in curve","Animation","All 43 Curves presets or non-null Curve reference. Omission/source preview uses easeIn."),
        new Field("color","Target color","Appearance","Optional ARGB, ColorScheme token, null/unset or Color? source."),
        new Field("colorBlendMode","Target blend mode","Appearance","Optional BlendMode/null/unset; default image color blending is srcIn."),
        new Field("placeholderColor","Placeholder color","Appearance","Independent optional ARGB, ColorScheme token, null/unset or Color? source."),
        new Field("placeholderColorBlendMode","Placeholder blend mode","Appearance","Independent optional BlendMode/null/unset; defaults to srcIn."),
        new Field("width","Width","Layout","Optional nonnegative logical pixels, null/unset or double? source. Shared by both images; distinct from cache/decode dimensions."),
        new Field("height","Height","Layout","Optional nonnegative logical pixels, null/unset or double? source. Intrinsic size can change when the target replaces its placeholder."),
        new Field("fit","Target fit","Layout","Optional BoxFit/null/unset, retaining native behavior."),
        new Field("placeholderFit","Placeholder fit","Layout","Optional BoxFit/null/unset. Null/omission inherits Target fit."),
        new Field("filterQuality","Target filter quality","Appearance","Optional FilterQuality, default medium. Explicit null is not allowed."),
        new Field("placeholderFilterQuality","Placeholder filter quality","Appearance","Optional FilterQuality/null/unset. Null/omission inherits Target filter quality."),
        new Field("alignment","Alignment","Layout","Physical/directional coordinates or non-null AlignmentGeometry source. Omission/source preview uses center. Directional values use ambient direction."),
        new Field("repeat","Repeat","Layout","Optional ImageRepeat, default noRepeat; applied to both images."),
        new Field("matchTextDirection","Match text direction","Layout","Optional checkbox, default false. True mirrors both images in RTL."));
    public static List<PropertyDefinition> properties(){
        var result=new ArrayList<PropertyDefinition>();
        for(int i=0;i<FIELDS.size();i++){
            String name=FIELDS.get(i).name();
            boolean required=name.equals("placeholder")||name.equals("image");
            List<PropertyValueConstraint> constraints=switch(name){
                case "placeholder","image"->List.of(new PropertyValueConstraint.ImageProviderValues(),new PropertyValueConstraint.DartObjectReferenceValues("ImageProvider<Object>"));
                case "placeholderErrorBuilder","imageErrorBuilder"->List.of(new PropertyValueConstraint.DartObjectReferenceValues("ImageErrorWidgetBuilder?"),new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "excludeFromSemantics","matchTextDirection"->List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN));
                case "imageSemanticLabel"->List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING),new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "fadeOutDurationUs","fadeInDurationUs"->List.of(new PropertyValueConstraint.IntegerRange(BigInteger.valueOf(1000),new BigInteger("9007199254740991")),new PropertyValueConstraint.DartObjectReferenceValues("Duration"));
                case "fadeOutCurve","fadeInCurve"->AnimatedOpacityWidgetPropertySchema.properties().get(1).constraints();
                case "color","placeholderColor"->ModalBarrierWidgetPropertySchema.properties().getFirst().constraints();
                case "width","height"->List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO,new BigInteger("9007199254740991")),new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO,true,null,true),new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL),new PropertyValueConstraint.DartObjectReferenceValues("double?"));
                case "colorBlendMode","placeholderColorBlendMode"->enumeration("BlendMode",true,List.of("clear","src","dst","srcOver","dstOver","srcIn","dstIn","srcOut","dstOut","srcATop","dstATop","xor","plus","modulate","screen","overlay","darken","lighten","colorDodge","colorBurn","hardLight","softLight","difference","exclusion","multiply","hue","saturation","color","luminosity"));
                case "fit","placeholderFit"->enumeration("BoxFit",true,List.of("fill","contain","cover","fitWidth","fitHeight","none","scaleDown"));
                case "filterQuality","placeholderFilterQuality"->enumeration("FilterQuality",name.equals("placeholderFilterQuality"),List.of("none","low","medium","high"));
                case "alignment"->List.of(new PropertyValueConstraint.AlignmentGeometryValues(),new PropertyValueConstraint.DartObjectReferenceValues("AlignmentGeometry"));
                case "repeat"->enumeration("ImageRepeat",false,List.of("repeat","repeatX","repeatY","noRepeat"));
                default->throw new IllegalStateException(name);
            };
            result.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(i,required),constraints,
                required?Optional.of(PropertyValue.ImageProviderValue.unresolved()):Optional.empty()));
        }
        return List.copyOf(result);
    }
    private static List<PropertyValueConstraint> enumeration(String type,boolean nullable,List<String> values){
        var result=new ArrayList<PropertyValueConstraint>();result.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart",type),values));
        if(nullable)result.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));return List.copyOf(result);
    }
    private FadeInImageWidgetPropertySchema(){}
}
