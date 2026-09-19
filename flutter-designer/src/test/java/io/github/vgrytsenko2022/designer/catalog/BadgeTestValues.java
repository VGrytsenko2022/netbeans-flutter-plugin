package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;

/** Representative complete Badge values shared by core contracts and command-history tests. */
public final class BadgeTestValues {
    private BadgeTestValues() {}
    public static PropertyName p(String name){return new PropertyName(name);}
    public static PropertyValue.DoubleValue d(String value){return new PropertyValue.DoubleValue(new BigDecimal(value));}
    public static PropertyValue.IntegerValue i(long value){return new PropertyValue.IntegerValue(BigInteger.valueOf(value));}
    public static PropertyValue.StringValue s(String value){return new PropertyValue.StringValue(value);}
    public static PropertyValue.ThemeTokenValue theme(){return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.error"));}
    public static PropertyValue.PaintValue paint(){return new PropertyValue.PaintValue(
        new ColorSource.Theme(new ThemeToken("material.colorScheme.error")),
        PropertyValue.PaintValue.BlendMode.SRC_OVER,PropertyValue.PaintValue.Style.STROKE,new BigDecimal("2.5"),
        PropertyValue.PaintValue.StrokeCap.ROUND,PropertyValue.PaintValue.StrokeJoin.BEVEL,BigDecimal.valueOf(4),true,
        PropertyValue.PaintValue.FilterQuality.MEDIUM,false,Optional.empty());}
    public static PropertyValue value(String name) {
        if (name.equals("textStyleThemeTextStyle")) return new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.labelSmall"));
        if (name.endsWith("Color")) return theme();
        if (name.equals("count")) return i(1000);
        if (name.equals("maxCount")) return i(999);
        if (name.equals("padding")) return new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE,BigDecimal.TWO,new BigDecimal("3"),new BigDecimal("4"));
        if (name.equals("alignment")) return new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,new BigDecimal("1.25"),new BigDecimal("-1"));
        if (name.equals("offset")) return new PropertyValue.OffsetValue(new BigDecimal("-4.5"),new BigDecimal("3.25"));
        if (name.endsWith("Foreground")||name.equals("textStyleBackground")) return paint();
        if (name.endsWith("Shadows")) return new PropertyValue.ShadowListValue(List.of(new PropertyValue.ShadowListValue.Shadow(StableId.parse("782709fe-06e9-47a0-bc08-d1ee9094b79e"),new ColorSource.Theme(new ThemeToken("material.colorScheme.error")),BigDecimal.ONE,BigDecimal.TWO,new BigDecimal("3"))));
        if (name.endsWith("FontFeatures")) return new PropertyValue.FontFeatureListValue(List.of(new PropertyValue.FontFeatureListValue.FontFeature(StableId.parse("2e76cc65-ce92-4b56-b9f2-0fd01ff551b7"),"liga",1)));
        if (name.endsWith("FontVariations")) return new PropertyValue.FontVariationListValue(List.of(new PropertyValue.FontVariationListValue.FontVariation(StableId.parse("0b539c6a-1a39-422e-a97f-a4106c4c4160"),"wght",BigDecimal.valueOf(700))));
        if (name.endsWith("LocaleLanguageCode")) return s("uk");
        if (name.endsWith("LocaleScriptCode")) return s("Cyrl");
        if (name.endsWith("LocaleCountryCode")) return s("UA");
        if (name.endsWith("FontFamilyFallback")) return s("Noto Sans\nRoboto");
        if (name.endsWith("FontFamily")) return s("Noto Sans");
        if (name.endsWith("Package")) return s("sample_fonts");
        if (name.endsWith("DebugLabel")) return s("Badge test");
        var property=BuiltInWidgetCatalog.getDefault().find(BadgeWidgetPropertySchema.BADGE_TYPE).orElseThrow().property(p(name)).orElseThrow();
        for(var constraint:property.constraints()){
            if(constraint instanceof PropertyValueConstraint.EnumValues e)return new PropertyValue.EnumValue(e.dartType().name(),e.values().getLast());
            if(constraint.kind()==PropertyValueKind.BOOLEAN)return new PropertyValue.BooleanValue(false);
        }
        return d(name.equals("largeSize")?"-2.5":"2.5");
    }
    public static LinkedHashMap<PropertyName,PropertyValue> full(boolean paints,boolean count) {
        var values=new LinkedHashMap<PropertyName,PropertyValue>();
        for(String name:BadgeWidgetPropertySchema.definitions().keySet()){
            if(!count&&(name.equals("count")||name.equals("maxCount")))continue;
            if(paints&&(name.equals("textStyleColor")||name.equals("textStyleBackgroundColor")))continue;
            if(!paints&&(name.equals("textStyleForeground")||name.equals("textStyleBackground")))continue;
            values.put(p(name),value(name));
        }
        return values;
    }
}
