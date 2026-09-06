package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema.Encoding;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema.Target;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Flutter 3.44.8 OutlinedButton and OutlinedButton.icon. Both share the reviewed
 * nine-state local ButtonStyle projection with TextButton; neither constructor
 * accepts isSemanticButton, and both preserve null when clipBehavior is omitted.
 */
public final class OutlinedButtonWidgetPropertySchema {
    public static final WidgetTypeId OUTLINED_BUTTON_TYPE =
            new WidgetTypeId("flutter.material.OutlinedButton");
    public static final int DIRECT_PROPERTY_COUNT = 11;
    public static final int STATE_COUNT = TextButtonWidgetPropertySchema.STATE_COUNT;
    public static final int STATE_PROPERTY_COUNT = TextButtonWidgetPropertySchema.STATE_PROPERTY_COUNT;
    public static final int COMMON_STYLE_PROPERTY_COUNT = TextButtonWidgetPropertySchema.COMMON_STYLE_PROPERTY_COUNT;
    public static final int LOCAL_STYLE_PROPERTY_COUNT = TextButtonWidgetPropertySchema.LOCAL_STYLE_PROPERTY_COUNT;
    public static final int FLATTENED_PROPERTY_COUNT = DIRECT_PROPERTY_COUNT + LOCAL_STYLE_PROPERTY_COUNT + 1;
    public static final int SLOT_COUNT = 2;

    public enum Group {
        EVENTS, BEHAVIOR, ENABLED_STYLE, DISABLED_STYLE, ERROR_STYLE, DRAGGED_STYLE,
        PRESSED_STYLE, SELECTED_STYLE, SCROLLED_UNDER_STYLE, HOVERED_STYLE,
        FOCUSED_STYLE, COMMON_STYLE, STYLE_REFERENCE;

        private TextButtonWidgetPropertySchema.Group shared() {
            return TextButtonWidgetPropertySchema.Group.valueOf(name());
        }

        public String setName() {
            return shared().setName().replace("textButton", "outlinedButton");
        }

        public String displayName() {
            return shared().displayName();
        }

        public String description() {
            return shared().description().replace("TextButton", "OutlinedButton");
        }
    }

    public record Definition(Group group, String displayName, String description,
            Target target, String dartName, int dartOrder, Encoding encoding) {
        public Definition {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(dartName, "dartName");
            Objects.requireNonNull(encoding, "encoding");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative OutlinedButton property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private OutlinedButtonWidgetPropertySchema() {
    }

    public static Map<String, Definition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<Definition> find(PropertyName name) {
        return find(Objects.requireNonNull(name, "name").value());
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name")));
    }

    public static List<String> variants() {
        return TextButtonWidgetPropertySchema.variants();
    }

    public static List<String> statePrefixes() {
        return TextButtonWidgetPropertySchema.statePrefixes();
    }

    public static List<String> statePriority() {
        return TextButtonWidgetPropertySchema.statePriority();
    }

    public static List<String> localStyleProperties() {
        return TextButtonWidgetPropertySchema.localStyleProperties();
    }

    public static ElevatedButtonWidgetPropertySchema.Definition sharedStyleDefinition(String name) {
        return TextButtonWidgetPropertySchema.sharedStyleDefinition(name);
    }

    public static boolean isCompound(PropertyName name) {
        return find(name).map(value -> value.target() != Target.DIRECT).orElse(false);
    }

    public static boolean isIcon(WidgetNode node) {
        return node.type().equals(OUTLINED_BUTTON_TYPE) && isIconVariant(node);
    }

    public static boolean acceptsIcon(WidgetNode node) {
        return isIcon(node);
    }

    /** The reviewed full-style families use exactly the same assembly. */
    public static boolean isFullStyleButton(WidgetNode node) {
        return node.type().equals(OUTLINED_BUTTON_TYPE)
                || node.type().equals(FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE)
                || node.type().equals(TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE);
    }

    public static boolean isIconVariant(WidgetNode node) {
        return FilledButtonWidgetPropertySchema.isIcon(node)
                || isFullStyleButton(node) && new PropertyValue.StringValue("icon").equals(
                node.properties().get(new PropertyName("variant")));
    }

    /** Shared style semantics without imposing the Child/Icon constructor contract. */
    public static boolean usesFullStyleProjection(WidgetNode node) {
        return isFullStyleButton(node) || node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE);
    }

    public static Optional<String> slotUnavailableReason(WidgetNode node, SlotName slot) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(slot, "slot");
        return node.type().equals(OUTLINED_BUTTON_TYPE) && slot.value().equals("icon") && !isIcon(node)
                ? Optional.of("OutlinedButton icon slot requires the Icon constructor; switch Constructor to Icon first.")
                : Optional.empty();
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        TextButtonWidgetPropertySchema.definitions().forEach((name, shared) -> {
            if (name.equals("isSemanticButton")) {
                return;
            }
            String description = shared.description().replace("TextButton", "OutlinedButton");
            if (name.equals("clipBehavior")) {
                description = "Clip enum or explicit null. Both Standard and Icon omission preserve null, "
                        + "enabling SDK automatic antiAlias when layer builders are present.";
            }
            int order = shared.dartOrder();
            if ((shared.target() == Target.DIRECT || shared.target() == Target.ACTIVATION) && order > 9) {
                order--;
            }
            values.put(name, new Definition(Group.valueOf(shared.group().name()), shared.displayName(),
                    description, shared.target(), shared.dartName(), order, shared.encoding()));
        });
        if (values.size() != FLATTENED_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("OutlinedButton property count: " + values.size());
        }
        return Collections.unmodifiableMap(values);
    }
}
