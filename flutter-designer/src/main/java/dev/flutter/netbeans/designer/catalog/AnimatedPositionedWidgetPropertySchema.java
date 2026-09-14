package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** All public positioned implicit-animation constructors in Flutter 3.44.8. */
public final class AnimatedPositionedWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedPositioned");
    public static final WidgetTypeId RECT_TYPE = new WidgetTypeId("flutter.widgets.AnimatedPositioned.fromRect");
    public static final WidgetTypeId DIRECTIONAL_TYPE = new WidgetTypeId("flutter.widgets.AnimatedPositionedDirectional");
    public static final List<WidgetTypeId> TYPES = List.of(TYPE, RECT_TYPE, DIRECTIONAL_TYPE);
    public static boolean supports(WidgetTypeId type) { return TYPES.contains(type); }
    public static boolean directional(WidgetTypeId type) { return DIRECTIONAL_TYPE.equals(type); }
    public static String description(WidgetTypeId type) {
        return "Animates position and size as a direct Stack.children member. Required Child is created by wrapping an existing Stack child. "
            + (RECT_TYPE.equals(type) ? "Full fromRect constructor: local LTWH rectangle or typed Rect reference. Editing a local rectangle field selects the local source. "
                : "At most two values per axis: edges and size. Setting a third clears the opposite edge for size edits, or clears size for edge edits, atomically. ")
            + "Signed coordinates and sizes are retained; Flutter clamps negative derived sizes to zero. "
            + "Duration is required (creation: 300 ms), Curve defaults to linear. Null removes a tween immediately. "
            + "Stack needs finite bounds when all children are positioned. Canvas never executes project references.";
    }
    public record Field(String name, String label, String description) {}
    public static List<Field> fields(WidgetTypeId type) {
        var result = new ArrayList<Field>();
        if (RECT_TYPE.equals(type)) {
            result.add(new Field("rect", "Rectangle source", "Required local rectangle or typed Rect reference/getter/factory. Local fields are retained while a project rectangle is selected; editing one selects local. Preview of a custom rectangle uses (0,0,48,48)."));
            for(String name:List.of("left","top","width","height"))
                result.add(new Field("rect"+Character.toUpperCase(name.charAt(0))+name.substring(1), "Rect "+name,
                    "Exact finite signed local rectangle "+name+". Editing selects the local rectangle atomically. Negative width/height are retained; native Stack clamps layout size to zero."));
        } else {
            for(String name:axisFields(type))
                result.add(new Field(name, Character.toUpperCase(name.charAt(0))+name.substring(1),
                    "Finite signed number, null or typed double? reference. Omission/null means unconstrained; null removes its tween immediately. At most two non-null fields per axis. Native Stack clamps negative layout sizes to zero."));
        }
        result.add(new Field("curve","Curve","All 43 pinned Curves presets or typed Curve reference; default linear. Native overshoot is retained."));
        result.add(new Field("durationUs","Duration (microseconds)","Required nonnegative portable integer microseconds or typed Duration reference. Creation/custom-reference preview: 300000. Zero completes immediately."));
        result.add(new Field("onEnd","On end","Optional VoidCallback reference/factory or null, configured in Events. Runs on completed transitions, not initial mount. Never executed in Canvas."));
        return List.copyOf(result);
    }
    public static List<String> axisFields(WidgetTypeId type) {
        return directional(type) ? List.of("start","top","end","bottom","width","height")
            : List.of("left","top","right","bottom","width","height");
    }
    public static List<PropertyDefinition> properties(WidgetTypeId type) {
        var result=new ArrayList<PropertyDefinition>();
        if (RECT_TYPE.equals(type)) {
            result.add(new PropertyDefinition(new PropertyName("rect"), DartParameter.named(0,true),
                List.of(new PropertyValueConstraint.StringPattern("local","local LTWH rectangle"),
                    new PropertyValueConstraint.DartObjectReferenceValues("Rect")), Optional.of(new PropertyValue.StringValue("local"))));
            for(String name:List.of("rectLeft","rectTop","rectWidth","rectHeight"))
                result.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(result.size(),true),
                    signedNumbers(),Optional.of(new PropertyValue.DoubleValue(name.equals("rectLeft")||name.equals("rectTop")?BigDecimal.ZERO:BigDecimal.valueOf(48)))));
        } else {
            for(String name:axisFields(type)){
                var constraints=new ArrayList<>(signedNumbers());
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("double?"));
                result.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(result.size(),false),constraints,Optional.empty()));
            }
        }
        var animation=AnimatedOpacityWidgetPropertySchema.properties();
        for(var p:List.of(animation.get(1),animation.get(2),animation.get(3)))
            result.add(new PropertyDefinition(p.name(),DartParameter.named(result.size(),p.parameter().required()),p.constraints(),p.creationDefault()));
        return List.copyOf(result);
    }
    private static List<PropertyValueConstraint> signedNumbers() {
        return List.of(new PropertyValueConstraint.IntegerRange(new BigInteger("-9007199254740991"),new BigInteger("9007199254740991")),
            new PropertyValueConstraint.DoubleRange(null,true,null,true));
    }
    public static boolean nonNull(PropertyValue value) { return value!=null && !(value instanceof PropertyValue.NullValue); }
    public static Optional<String> conflict(WidgetNode node) {
        if (!supports(node.type())) return Optional.empty();
        if (RECT_TYPE.equals(node.type())) {
            if (node.properties().get(new PropertyName("rect")) instanceof PropertyValue.DartObjectReferenceValue) return Optional.empty();
            for (var pair : List.of(List.of("rectLeft", "rectWidth"), List.of("rectTop", "rectHeight"))) {
                Double origin = localNumber(node, pair.get(0)), extent = localNumber(node, pair.get(1));
                if (origin != null && extent != null && (!Double.isFinite(origin + extent) || !Double.isFinite((origin + extent) - origin)))
                    return Optional.of("Local Rect edges and derived sizes must remain finite; reduce " + String.join(" or ", pair) + ".");
            }
            return Optional.empty();
        }
        for(var axis:List.of(List.of(directional(node.type())?"start":"left",directional(node.type())?"end":"right","width"),List.of("top","bottom","height")))
            if(axis.stream().allMatch(name->nonNull(node.properties().get(new PropertyName(name)))))
                return Optional.of("At most two non-null fields per axis are allowed: "+String.join(", ",axis)+". Reset an edge or size; project references count as potentially non-null.");
        return Optional.empty();
    }
    private static Double localNumber(WidgetNode node, String name) {
        var value = node.properties().get(new PropertyName(name));
        if (value instanceof PropertyValue.DoubleValue d) return d.value().doubleValue();
        if (value instanceof PropertyValue.IntegerValue i) return i.value().doubleValue();
        return null;
    }
    private AnimatedPositionedWidgetPropertySchema() {}
}
