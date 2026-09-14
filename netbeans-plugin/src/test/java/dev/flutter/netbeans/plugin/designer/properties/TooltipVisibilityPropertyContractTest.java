package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog;
import dev.flutter.netbeans.designer.state.WidgetStatePropertyBindingCatalog;
import java.beans.FeatureDescriptor;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

public class TooltipVisibilityPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault()
            .find(TooltipVisibilityWidgetPropertySchema.TOOLTIP_VISIBILITY_TYPE).orElseThrow();
    private static final PropertyName VISIBLE = new PropertyName("visible");
    private static final SlotName CHILD = new SlotName("child");

    @Test void requiredVisibleUsesStablePaintedCheckboxAndNeverDispatchesAnUnsetOrReset() throws Exception {
        var widget = prototype(); var commands = new ArrayList<DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add);
        var row = TooltipPropertyContractTest.cell(node, "visible"); var groups = node.getPropertySets();
        assertEquals(1, DEF.properties().size()); assertEquals(1, DEF.slots().size()); assertTrue(DEF.constConstructor());
        assertEquals(Map.of(VISIBLE, new PropertyValue.BooleanValue(true)), widget.properties());
        assertEquals(CHILD, WidgetPlacementRules.requiredAnyWidgetWrapperSlot(DEF).orElseThrow().name());
        assertTrue(row.canWrite()); assertFalse(row.supportsDefaultValue()); assertFalse(row.isDefaultValue());
        assertTrue(row.getShortDescription().contains("not an SDK default"));
        assertTrue(row.getShortDescription().contains("nearest scope wins"));
        assertTrue(row.getShortDescription().contains("not AND"));
        var editor = row.getPropertyEditor();
        assertTrue(editor.isPaintable()); assertNull(editor.getTags());
        assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        row.restoreDefaultValue(); assertTrue(commands.isEmpty());
        for (boolean value : List.of(false, true)) {
            row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(value)));
            assertEquals(new SetProperty(widget.id(), VISIBLE, new PropertyValue.BooleanValue(value)), commands.removeFirst());
            widget = new WidgetNode(widget.id(), widget.type(), Map.of(VISIBLE, new PropertyValue.BooleanValue(value)), widget.slots());
            node.refreshPresentation(widget, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, TooltipPropertyContractTest.cell(node, "visible")); assertArrayEquals(groups, node.getPropertySets());
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(value)), row.getValue());
        }
        node.refreshPresentation(widget, DEF, null, null, null, FlutterImageAssetChoices.empty());
        assertFalse(row.canWrite());
        assertThrows(IllegalAccessException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false))));
        assertTrue(commands.isEmpty());
        assertTrue(TooltipPropertyContractTest.cell(node, "child").getShortDescription().contains("cannot be removed or cleared"));
    }

    @Test void requiredBooleanInplaceFactoryRemainsCenteredAndDraftOnlyUntilCommit() throws Exception {
        var binding = FlutterTypedPropertyEditors.binding(DEF.property(VISIBLE).orElseThrow(), Optional.empty(), false, List.of()).orElseThrow();
        assertFalse(binding.optional()); var editor = binding.createEditor();
        var original = FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)); editor.setValue(original);
        assertThrows(IllegalArgumentException.class, () -> editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT));
        SwingUtilities.invokeAndWait(() -> {
            var descriptor = new FeatureDescriptor(); descriptor.setName("visible"); descriptor.setDisplayName("Visible");
            var inplace = FlutterPropertyEditorComponents.inplaceFactory(binding).orElseThrow().getInplaceEditor();
            inplace.connect(editor, PropertyEnv.create(descriptor));
            var checkbox = assertInstanceOf(JCheckBox.class, inplace.getComponent());
            assertEquals(SwingConstants.CENTER, checkbox.getHorizontalAlignment()); assertTrue(checkbox.isSelected());
            checkbox.doClick(); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)), inplace.getValue());
            assertEquals(original, editor.getValue(), "An in-place draft does not mutate the property editor before commit.");
            assertThrows(IllegalArgumentException.class, () -> inplace.setValue(FlutterPropertyCellValue.unset()));
            inplace.clear();
            inplace.connect(editor, PropertyEnv.create(descriptor)); assertTrue(assertInstanceOf(JCheckBox.class, inplace.getComponent()).isSelected());
            inplace.clear();
        });
    }

    @Test void stateConsumerIsAvailableWithoutInventingEventsOrAVisibilityProducer() {
        var widget = prototype(); assertTrue(WidgetEventCatalog.eventsFor(DEF).isEmpty());
        assertTrue(WidgetStateBindingCatalog.find(widget).isEmpty());
        assertEquals(List.of("visible"), WidgetStatePropertyBindingCatalog.descriptors(widget).stream().map(value -> value.propertyName().value()).toList());
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, ignored -> {});
        assertTrue(Arrays.stream(node.getPropertySets()).noneMatch(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)));
        assertTrue(widget.stateBinding().isEmpty()); assertTrue(widget.propertyBindings().isEmpty());
    }

    public static Map<PropertyName, PropertyValue> full() { return Map.of(VISIBLE, new PropertyValue.BooleanValue(false)); }
    public static WidgetNode prototype() {
        var base = WidgetNodePrototypeFactory.create(DEF, StableId.random());
        var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Retained Tooltip anchor")), Map.of());
        return new WidgetNode(base.id(), base.type(), base.properties(), Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
    }
}
