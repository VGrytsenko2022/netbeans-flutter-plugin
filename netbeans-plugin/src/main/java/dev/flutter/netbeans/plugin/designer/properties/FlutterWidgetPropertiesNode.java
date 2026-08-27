package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.plugin.designer.icons.FlutterWidgetIconRegistry;
import java.util.Objects;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.Lookups;

/**
 * Standard NetBeans node projection for one selected Flutter Designer widget.
 * The exposed property sheet is deliberately read-only until mutation admission
 * is connected to the designer command pipeline.
 */
public final class FlutterWidgetPropertiesNode extends AbstractNode {
    public static final String IDENTITY_SET_NAME = "identity";
    public static final String PROPERTIES_SET_NAME = Sheet.PROPERTIES;
    public static final String STABLE_ID_PROPERTY_NAME = "stableId";
    public static final String TYPE_PROPERTY_NAME = "type";
    public static final String NOT_SET = "<not set>";

    private final WidgetNode widget;
    private final WidgetDefinition definition;

    /**
     * Creates a read-only widget node usable by Explorer and the standard
     * Properties window.
     *
     * @param children explorer children representing the widget slots
     * @param widget immutable widget model node
     * @param definition matching catalog definition
     */
    public FlutterWidgetPropertiesNode(
            Children children,
            WidgetNode widget,
            WidgetDefinition definition) {
        super(
                Objects.requireNonNull(children, "children"),
                Lookups.fixed(
                        Objects.requireNonNull(widget, "widget").id(),
                        widget,
                        Objects.requireNonNull(definition, "definition")));
        if (!widget.type().equals(definition.typeId())) {
            throw new IllegalArgumentException(
                    "Widget type " + widget.type() + " does not match catalog definition "
                    + definition.typeId());
        }
        this.widget = widget;
        this.definition = definition;
        String displayName = definition.palette().displayName();
        setName(widget.id().toString());
        setDisplayName(displayName);
        setShortDescription(displayName + " — " + widget.id());
        FlutterWidgetIconRegistry.findIconPath(widget.type())
                .ifPresent(this::setIconBaseWithExtension);
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = new Sheet();

        Sheet.Set identity = new Sheet.Set();
        identity.setName(IDENTITY_SET_NAME);
        identity.setDisplayName("Widget identity");
        identity.setShortDescription("Stable identity and catalog type of the selected widget.");
        identity.put(readOnly(
                STABLE_ID_PROPERTY_NAME,
                "Stable ID",
                "Stable identifier stored in the Flutter Designer model.",
                widget.id().toString()));
        identity.put(readOnly(
                TYPE_PROPERTY_NAME,
                "Widget type",
                "Catalog type identifier stored in the Flutter Designer model.",
                widget.type().value()));
        sheet.put(identity);

        Sheet.Set properties = new Sheet.Set();
        properties.setName(PROPERTIES_SET_NAME);
        properties.setDisplayName("Widget properties");
        properties.setShortDescription(
                "Explicit property values stored on the selected widget; catalog creation defaults are not applied.");
        for (PropertyDefinition property : definition.properties()) {
            PropertyValue explicitValue = widget.properties().get(property.name());
            String value = explicitValue == null
                    ? NOT_SET
                    : PropertyValueFormatter.format(explicitValue);
            properties.put(readOnly(
                    property.name().value(),
                    property.name().value(),
                    "Explicit model value for " + property.name().value() + ".",
                    value));
        }
        sheet.put(properties);
        return sheet;
    }

    private static PropertySupport.ReadOnly<String> readOnly(
            String name,
            String displayName,
            String description,
            String value) {
        return new PropertySupport.ReadOnly<>(name, String.class, displayName, description) {
            @Override
            public String getValue() {
                return value;
            }
        };
    }
}
