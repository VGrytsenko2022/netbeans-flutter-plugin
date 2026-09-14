package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** All five Flutter 3.44.8 ColorFilter families, source override, and optional Child. */
public final class ColorFilteredWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.ColorFiltered");
    public static final List<String> FILTERS = List.of("mode", "matrix", "linearToSrgbGamma", "srgbToLinearGamma", "saturation");
    public static final String DESCRIPTION = "Filters Child pixels without changing layout or hit testing. "
            + "All five ColorFilter families and a non-null typed ColorFilter source are supported. "
            + "Creation uses the identity matrix and optional empty Child. Only the selected branch is emitted; other local drafts are preserved. "
            + "Canvas does not execute project code and previews a project filter as identity.";
    public record Field(String name,String label,String group,String description) {}
    public static final List<Field> FIELDS = fields();
    private static List<Field> fields() {
        var result = new ArrayList<Field>();
        result.add(new Field("colorFilter","Color filter","Filter","Required mode/matrix/linearToSrgbGamma/srgbToLinearGamma or saturation preset, or non-null ColorFilter getter/factory. No null/unset. Default identity matrix. Local branch drafts remain stored when switching to project mode."));
        result.add(new Field("color","Source color","Blend","Used only by mode. ARGB, theme token or non-null Color source. Omission is transparent. This is the source color; Child pixels are the destination. Canvas previews source Color as transparent."));
        result.add(new Field("blendMode","Blend mode","Blend","Used only by mode. All 29 BlendMode values; omission is srcOver. No null. Filter output is subsequently composited with the background."));
        for(int row=0;row<4;row++)for(int column=0;column<5;column++) {
            String name="m"+row+column;
            result.add(new Field(name,"Matrix ["+row+", "+column+"]","Matrix 4 x 5",
                    "Used only by matrix. Finite row-major coefficient; rows are output R, G, B, A and columns input R, G, B, A, offset. "
                    + "Offsets use unnormalized 0..255 units (signed values allowed). Omission is "+(row==column?"1":"0")+"."));
        }
        result.add(new Field("saturation","Saturation","Saturation","Used only by saturation. Finite signed value; 0 is grayscale, 1 unchanged, above 1 increases saturation. Negative values are preserved. Native factory is non-const. Omission is 1."));
        return List.copyOf(result);
    }
    public static List<PropertyDefinition> properties() {
        var result=new ArrayList<PropertyDefinition>();
        for(int i=0;i<FIELDS.size();i++){
            String name=FIELDS.get(i).name();
            List<PropertyValueConstraint> constraints;
            if(name.equals("colorFilter")) constraints=List.of(new PropertyValueConstraint.StringPattern("(?:mode|matrix|linearToSrgbGamma|srgbToLinearGamma|saturation)","one of the five ColorFilter families"),new PropertyValueConstraint.DartObjectReferenceValues("ColorFilter"));
            else if(name.equals("color")) constraints=List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR),
                    new PropertyValueConstraint.ThemeTokenValues(MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()),
                    new PropertyValueConstraint.DartObjectReferenceValues("Color"));
            else if(name.equals("blendMode")) constraints=FadeInImageWidgetPropertySchema.properties().stream().filter(p->p.name().value().equals("colorBlendMode")).findFirst().orElseThrow().constraints().stream().filter(c->c.kind()!=PropertyValueKind.NULL).toList();
            else constraints=List.of(new PropertyValueConstraint.IntegerRange(new BigInteger("-9007199254740991"),new BigInteger("9007199254740991")),
                    new PropertyValueConstraint.DoubleRange(null,true,null,true));
            result.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(i,i==0),constraints,
                    i==0?Optional.of(new PropertyValue.StringValue("matrix")):Optional.empty()));
        }
        return List.copyOf(result);
    }
    public static BigDecimal number(WidgetNode node,String name,BigDecimal fallback) {
        PropertyValue value=node.properties().get(new PropertyName(name));
        return value instanceof PropertyValue.IntegerValue n?new BigDecimal(n.value()):value instanceof PropertyValue.DoubleValue n?n.value():fallback;
    }
    private ColorFilteredWidgetPropertySchema(){}
}
