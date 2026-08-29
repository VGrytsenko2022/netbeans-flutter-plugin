package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.icons.FlutterWidgetIconRegistry;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.Lookups;

/**
 * Standard NetBeans node projection for one selected Flutter Designer widget.
 * The three-argument form is fully read-only; the mutation-aware form exposes
 * only the explicitly admitted property slice.
 */
public final class FlutterWidgetPropertiesNode extends AbstractNode {
    private static final Set<WidgetTypeId> WRITABLE_WIDGET_TYPES = Set.of(
            new WidgetTypeId("flutter.widgets.Column"),
            new WidgetTypeId("flutter.widgets.Row"),
            new WidgetTypeId("flutter.widgets.Padding"),
            new WidgetTypeId("flutter.widgets.Center"),
            new WidgetTypeId("flutter.widgets.Text"));

    public static final String IDENTITY_SET_NAME = "identity";
    public static final String PROPERTIES_SET_NAME = Sheet.PROPERTIES;
    public static final String STABLE_ID_PROPERTY_NAME = "stableId";
    public static final String TYPE_PROPERTY_NAME = "type";
    public static final String NOT_SET = FlutterPropertyCellValue.NOT_SET_TEXT;

    private final WidgetNode widget;
    private final WidgetDefinition definition;
    private final PropertyMutationHandler mutationHandler;

    /** Dispatches one exact command from the immutable selected-widget snapshot. */
    @FunctionalInterface
    public interface PropertyMutationHandler {
        void submit(DesignerCommand command);
    }

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
        this(children, widget, definition, null);
    }

    /**
     * Creates a widget node with the first explicitly admitted writable
     * property slice. A {@code null} handler keeps the entire sheet read-only.
     *
     * @param children explorer children representing the widget slots
     * @param widget immutable widget model node
     * @param definition matching catalog definition
     * @param mutationHandler mutation dispatcher, or {@code null} for a
     *     read-only node
     */
    public FlutterWidgetPropertiesNode(
            Children children,
            WidgetNode widget,
            WidgetDefinition definition,
            PropertyMutationHandler mutationHandler) {
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
        this.mutationHandler = mutationHandler;
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

        if (TextWidgetPropertySchema.TEXT_TYPE.equals(widget.type())) {
            addTextPropertySets(sheet);
        } else {
            sheet.put(createGenericPropertySet());
        }
        return sheet;
    }

    private Sheet.Set createGenericPropertySet() {
        Sheet.Set properties = propertySet(
                PROPERTIES_SET_NAME,
                "Widget properties",
                "Explicit property values stored on the selected widget; catalog creation defaults are not applied.");
        for (PropertyDefinition property : definition.properties()) {
            properties.put(projectProperty(property, Optional.empty()));
        }
        return properties;
    }

    private void addTextPropertySets(Sheet sheet) {
        EnumMap<TextWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(TextWidgetPropertySchema.Group.class);
        for (TextWidgetPropertySchema.Group group
                : TextWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            groups.put(group, set);
            sheet.put(set);
        }

        Sheet.Set unmatched = null;
        for (PropertyDefinition property : definition.properties()) {
            Optional<TextWidgetPropertySchema.Definition> schema =
                    TextWidgetPropertySchema.find(property.name());
            if (schema.isPresent()) {
                groups.get(schema.orElseThrow().group())
                        .put(projectProperty(property, schema));
                continue;
            }
            // Preserve contributed Text properties even when they are outside
            // the reviewed built-in scalar projection. Built-in Text never
            // reaches this fallback because its catalog/schema parity is tested.
            if (unmatched == null) {
                unmatched = propertySet(
                        PROPERTIES_SET_NAME,
                        "Other properties",
                        "Catalog properties outside the built-in Text scalar projection.");
                sheet.put(unmatched);
            }
            unmatched.put(projectProperty(property, Optional.empty()));
        }
    }

    private Node.Property<?> projectProperty(
            PropertyDefinition property,
            Optional<TextWidgetPropertySchema.Definition> textSchema) {
        PropertyValue explicitValue = widget.properties().get(property.name());
        var binding = writableBinding(property, textSchema);
        String projectedDisplayName = textSchema
                .map(TextWidgetPropertySchema.Definition::displayName)
                .orElseGet(() -> displayName(property.name()));
        String projectedDescription = textSchema
                .map(TextWidgetPropertySchema.Definition::description)
                .orElseGet(() -> "Explicit model value for "
                + property.name().value() + ".");
        if (binding.isPresent()) {
            return writableProperty(
                    binding.orElseThrow(), explicitValue,
                    projectedDisplayName, projectedDescription);
        }
        String value = explicitValue == null
                ? NOT_SET
                : PropertyValueFormatter.format(explicitValue);
        return readOnly(
                property.name().value(),
                projectedDisplayName,
                projectedDescription,
                value);
    }

    private java.util.Optional<FlutterTypedPropertyEditors.Binding> writableBinding(
            PropertyDefinition property,
            Optional<TextWidgetPropertySchema.Definition> textSchema) {
        if (mutationHandler == null || !WRITABLE_WIDGET_TYPES.contains(widget.type())) {
            return java.util.Optional.empty();
        }
        return FlutterTypedPropertyEditors.binding(property, textSchema);
    }

    private PropertySupport.ReadWrite<FlutterPropertyCellValue> writableProperty(
            FlutterTypedPropertyEditors.Binding binding,
            PropertyValue explicitValue,
            String displayName,
            String schemaDescription) {
        PropertyDefinition property = binding.definition();
        PropertyName propertyName = property.name();
        FlutterPropertyCellValue captured = explicitValue == null
                ? FlutterPropertyCellValue.unset()
                : FlutterPropertyCellValue.explicit(explicitValue);
        binding.validate(captured);
        String description = propertyDescription(property, schemaDescription);
        PropertySupport.ReadWrite<FlutterPropertyCellValue> result =
                new PropertySupport.ReadWrite<>(
                propertyName.value(),
                FlutterPropertyCellValue.class,
                displayName,
                description) {
            @Override
            public FlutterPropertyCellValue getValue() {
                return captured;
            }

            @Override
            public void setValue(FlutterPropertyCellValue value) {
                FlutterPropertyCellValue accepted = binding.validate(value);
                if (captured.equals(accepted)) {
                    return;
                }
                DesignerCommand command = accepted.explicitValue()
                        .<DesignerCommand>map(explicit -> new SetProperty(
                                widget.id(), propertyName, explicit))
                        .orElseGet(() -> new ResetProperty(
                                widget.id(), propertyName));
                mutationHandler.submit(command);
            }

            @Override
            public PropertyEditor getPropertyEditor() {
                return binding.createEditor();
            }

            @Override
            public boolean supportsDefaultValue() {
                return binding.optional();
            }

            @Override
            public boolean isDefaultValue() {
                return !captured.isExplicit();
            }

            @Override
            public void restoreDefaultValue()
                    throws IllegalAccessException, InvocationTargetException {
                if (binding.optional() && captured.isExplicit()) {
                    mutationHandler.submit(new ResetProperty(
                            widget.id(), propertyName));
                }
            }
        };
        // Custom editors keep a local draft. NetBeans applies the accepted
        // value only when the cell/custom dialog is committed, so a chooser,
        // spinner, or text area's intermediate events can never consume the
        // designer's one-shot mutation lease.
        result.setValue("changeImmediate", Boolean.FALSE);
        return result;
    }

    private static String displayName(PropertyName propertyName) {
        String value = propertyName.value();
        StringBuilder result = new StringBuilder(value.length() + 4);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (index > 0 && Character.isUpperCase(current)) {
                result.append(' ');
            }
            result.append(index == 0
                    ? Character.toUpperCase(current) : current);
        }
        return result.toString();
    }

    private static String propertyDescription(
            PropertyDefinition property,
            String schemaDescription) {
        String accepted = property.constraints().stream()
                .map(dev.flutter.netbeans.designer.catalog.PropertyValueConstraint::description)
                .reduce((left, right) -> left + "; " + right)
                .orElse("catalog-declared values");
        String reset = property.parameter().required()
                ? " This required constructor argument cannot be unset."
                : " Restore Default removes the explicit constructor argument.";
        return schemaDescription + " Accepted: " + accepted + "." + reset;
    }

    private static Sheet.Set propertySet(
            String name,
            String displayName,
            String description) {
        Sheet.Set set = new Sheet.Set();
        set.setName(name);
        set.setDisplayName(displayName);
        set.setShortDescription(description);
        return set;
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
