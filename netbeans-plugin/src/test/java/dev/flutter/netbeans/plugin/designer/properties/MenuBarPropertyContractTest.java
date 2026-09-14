package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

import static org.junit.jupiter.api.Assertions.*;

/** Property-sheet coverage for the full MenuBar vertical slice. */
public class MenuBarPropertyContractTest {
    private static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault()
            .find(MenuBarWidgetPropertySchema.MENU_BAR_TYPE).orElseThrow();

    @Test
    void all206RowsRemainTypedWritableAndKeepTheirIdentityAcrossRefresh() {
        WidgetNode base = WidgetNodePrototypeFactory.create(DEF, StableId.random());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode properties = node(base, commands);
        Node.PropertySet[] groups = properties.getPropertySets();
        // Widget identity and Slots are added around the 12 MenuBar schema groups.
        assertEquals(14, groups.length);
        assertEquals(206, DEF.properties().size());

        for (PropertyDefinition definition : DEF.properties()) {
            String name = definition.name().value();
            Node.Property<FlutterPropertyCellValue> row = cell(properties, name);
            assertNotNull(row.getPropertyEditor(), name);
            assertTrue(row.canWrite(), name);
            assertEquals(!definition.parameter().required(), row.supportsDefaultValue(), name);
            properties.refreshPresentation(base, DEF, commands::add, null, null,
                    FlutterImageAssetChoices.empty());
            assertSame(row, cell(properties, name), name);
            assertArrayEquals(groups, properties.getPropertySets(), name);
            commands.clear();
        }
    }

    @Test
    void wholeStyleAndLocalStyleEditsAreAtomicWithoutTouchingChildren() throws Exception {
        WidgetNode base = WidgetNodePrototypeFactory.create(DEF, StableId.random());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode properties = node(base, commands);

        properties.refreshPresentation(base, DEF, commands::add, null, null,
                FlutterImageAssetChoices.empty());
        cell(properties, "style").setValue(FlutterPropertyCellValue.explicit(
                TextButtonPropertyContractTest.reference("menuStyle")));
        assertEquals(1, commands.size());
        WidgetNode whole = apply(base, commands.removeFirst());
        assertEquals(base.slots(), whole.slots());
        assertTrue(whole.properties().containsKey(new PropertyName("style")));
        for (String local : MenuBarWidgetPropertySchema.localStyleProperties()) {
            assertFalse(whole.properties().containsKey(new PropertyName(local)), local);
        }

        properties.refreshPresentation(whole, DEF, commands::add, null, null,
                FlutterImageAssetChoices.empty());
        PropertyValue elevation = TextButtonPropertyContractTest.value(
                DEF.property(new PropertyName("styleElevation")).orElseThrow());
        cell(properties, "styleElevation").setValue(FlutterPropertyCellValue.explicit(elevation));
        assertEquals(1, commands.size());
        WidgetNode local = apply(whole, commands.removeFirst());
        assertFalse(local.properties().containsKey(new PropertyName("style")));
        assertEquals(elevation, local.properties().get(new PropertyName("styleElevation")));
        assertEquals(base.slots(), local.slots());
    }

    private static FlutterWidgetPropertiesNode node(WidgetNode widget,
            List<DesignerCommand> commands) {
        return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add);
    }

    @SuppressWarnings("unchecked")
    private static Node.Property<FlutterPropertyCellValue> cell(
            FlutterWidgetPropertiesNode node, String name) {
        return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets())
                .flatMap(set -> Arrays.stream(set.getProperties()))
                .filter(property -> property.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private static WidgetNode apply(WidgetNode before, DesignerCommand command) {
        return new WidgetNode(before.id(), before.type(),
                TextButtonPropertyContractTest.apply(before.properties(), command),
                before.slots(), before.extensions(), before.stateBinding(), before.propertyBindings());
    }
}
