package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 RadioGroup contract with an explicit closed generic type. */
public final class RadioGroupWidgetPropertySchema {
    public static final WidgetTypeId RADIO_GROUP_TYPE = new WidgetTypeId("flutter.widgets.RadioGroup");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 4;
    public static final int DIRECT_PROPERTY_COUNT = 4;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        BEHAVIOR;
        public String setName() { return "radioGroupBehavior"; }
        public String displayName() { return "Behavior"; }
        public String description() { return "Flutter RadioGroup controlled selection and generic type."; }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(description);
            Objects.requireNonNull(dartName);
            if (dartOrder < 0) throw new IllegalArgumentException("Negative RadioGroup property order");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private RadioGroupWidgetPropertySchema() {}
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static List<String> valueTypes() { return RadioWidgetPropertySchema.valueTypes(); }
    public static boolean isTypeReference(PropertyValue value) { return RadioWidgetPropertySchema.isTypeReference(value); }
    public static boolean nullableValueType(WidgetNode node) { return RadioWidgetPropertySchema.nullableValueType(node); }

    /** Checks only this group's selected type/value, not runtime descendants or equality. */
    public static Optional<String> valueTypeError(WidgetNode node) {
        return RadioWidgetPropertySchema.valueTypeError(node).map(message -> message.replace("Radio ", "RadioGroup "));
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "groupValue", 0, "Group Value",
                "Optional controlled selected value. Omission and explicit null both pass null; matching nullable Radio values may still be selected, and explicit null remains stored. Literals and strict nullable project references must match the selected generic type. Equality and selection are performed by the SDK, never by project code in Canvas.");
        add(values, "onChanged", 1, "On Changed",
                "Required No-op preset or strict non-null ValueChanged<T?> reference. Creation stores No-op. Null and omission are not accepted by this constructor. Requests do not mutate the stored group value, and Canvas never executes project callbacks.");
        add(values, "valueType", 3, "Value Type",
                "Required explicit generic type, created String. Choose String, int, double, num, bool, Object, or a simple project class/enum/typedef reference. No member, invocation or raw Dart type is accepted. Descendant radios join only the nearest group with exactly the same T, including nullability.");
        add(values, "nullableValueType", 4, "Nullable Value Type",
                "Unset/false emits T; true emits T?. Group Value and callback arguments remain nullable in either case. Changing the group type never rewrites descendant Radio types, values or callbacks.");
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, int order, String label, String description) {
        values.put(name, new Definition(Group.BEHAVIOR, label, description, name, order));
    }
}
