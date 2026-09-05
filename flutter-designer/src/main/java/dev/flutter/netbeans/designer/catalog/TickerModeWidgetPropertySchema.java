package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Complete Flutter 3.44.8 TickerMode constructor projection, excluding managed key.
 * Enabled is required by Flutter; true is an explicit Designer creation value,
 * not a framework constructor default. Child is supplied by atomic wrapping.
 */
public final class TickerModeWidgetPropertySchema {
    public static final WidgetTypeId TICKER_MODE_TYPE = new WidgetTypeId("flutter.widgets.TickerMode");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 2;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        TICKERS("tickerModeBehavior", "Tickers", "Requested ticker execution and frame scheduling for this subtree.");

        private final String setName;
        private final String displayName;
        private final String description;
        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }
        public String setName() { return setName; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) throw new IllegalArgumentException("dartOrder must be non-negative");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();
    private TickerModeWidgetPropertySchema() { }
    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }

    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "enabled", "Enabled",
                "Required boolean; Designer creates an explicit true value, not a Flutter constructor default. False mutes widget-aware tickers while elapsed time continues; it does not pause animation time. Effective enabled is this value AND every ancestor TickerMode's enabled value. Applies to widget-aware ticker providers, not arbitrary timers. Layout, painting, pointer interaction, focus and semantics remain unchanged.", 0);
        add(values, "forceFrames", "Force frames",
                "Omission preserves false. True requests animation frames even when normal frame scheduling is suspended; use sparingly because this can increase battery usage. Effective forceFrames is this value OR any ancestor TickerMode's forceFrames value. Explicit false cannot cancel an ancestor request and is valid even when enabled=false. Enabled and Force frames are independent constructor arguments.", 2);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("TickerMode schema/property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, String displayName, String description, int order) {
        if (values.putIfAbsent(name, new Definition(Group.TICKERS, displayName, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate TickerMode property schema: " + name);
        }
    }
    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}
