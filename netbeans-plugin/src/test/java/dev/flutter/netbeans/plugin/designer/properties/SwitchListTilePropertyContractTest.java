package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class SwitchListTilePropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE).orElseThrow();

    @Test void all236FieldsHaveTypedEditorsStableRowsAndIndependentSparseResets() throws Exception {
        var prototype = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(prototype, commands); var sets = node.getPropertySets();
        assertEquals(236, DEF.properties().size()); assertEquals(3, DEF.slots().size()); var seen = new HashSet<String>();
        assertEquals(Map.of(p("value"), new PropertyValue.BooleanValue(false), p("onChanged"), new PropertyValue.StringValue("noop"), p("variant"), new PropertyValue.StringValue("standard")), prototype.properties());
        for (var field : DEF.properties()) {
            String name = field.name().value(); var properties = new LinkedHashMap<>(prototype.properties()); properties.putAll(sparsePrerequisites(name));
            var before = new WidgetNode(prototype.id(), DEF.typeId(), properties, prototype.slots());
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var row = cell(node, name); var editor = row.getPropertyEditor(); var value = value(name);
            assertTrue(seen.add(name)); assertTrue(row.canWrite()); assertEquals(!field.parameter().required(), row.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue(), name);
            commands.clear(); row.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(1, commands.size(), name);
            var after = apply(before, commands.removeFirst()); assertValid(after); assertEquals(value, after.properties().get(field.name())); assertEquals(before.slots(), after.slots());
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, cell(node, name)); assertEquals(editor.getClass(), row.getPropertyEditor().getClass()); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            if (!field.parameter().required()) {
                row.restoreDefaultValue(); assertEquals(1, commands.size(), name); var reset = apply(after, commands.removeFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name()));
            } else {
                assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.unset())); row.restoreDefaultValue(); assertTrue(commands.isEmpty());
            }
        }
        assertEquals(236, seen.size()); assertTrue(DEF.property(p("enabled")).isEmpty()); assertTrue(DEF.property(p("tristate")).isEmpty());
        assertTrue(DEF.property(p("titleAlignment")).isEmpty()); assertTrue(DEF.property(p("trackOutlineWidth")).isEmpty());
    }

    @Test void bothConstructorsRetainAdaptiveSettingAndAllTenShapeBranchesWithoutChangingSlots() throws Exception {
        for (String variant : SwitchListTileWidgetPropertySchema.variants()) for (String kind : SwitchListTileWidgetPropertySchema.shapeKinds()) {
            var prototype = prototype(); var before = new WidgetNode(prototype.id(), DEF.typeId(), full(variant, kind), prototype.slots()); assertValid(before);
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(variant.equals("standard") ? "adaptive" : "standard")));
            var changed = apply(before, commands.removeFirst()); assertValid(changed); var expected = new LinkedHashMap<>(before.properties()); expected.put(p("variant"), changed.properties().get(p("variant")));
            assertEquals(expected, changed.properties()); assertEquals(before.slots(), changed.slots());
            for (PropertyValue flag : List.of(new PropertyValue.BooleanValue(false), new PropertyValue.NullValue())) {
                cell(node, "applyCupertinoTheme").setValue(FlutterPropertyCellValue.explicit(flag)); var edited = apply(before, commands.removeFirst()); assertValid(edited);
                assertEquals(before.properties().get(p("variant")), edited.properties().get(p("variant"))); assertEquals(flag, edited.properties().get(p("applyCupertinoTheme")));
            }
            cell(node, "applyCupertinoTheme").restoreDefaultValue(); var reset = apply(before, commands.removeFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(p("applyCupertinoTheme")));
        }
    }

    @Test void allWholeAndLocalFamiliesSwitchAtomicallyAndKeepUnrelatedFieldsAndChildren() throws Exception {
        var families = new ArrayList<>(SwitchListTileWidgetPropertySchema.colorFamilies()); families.addAll(List.of("thumbIcon", "shape", "visualDensity", "mouseCursor"));
        for (String family : families) {
            var prototype = prototype(); var before = new WidgetNode(prototype.id(), DEF.typeId(), full(), prototype.slots()); assertValid(before);
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands); var local = locals(family);
            cell(node, family).setValue(FlutterPropertyCellValue.explicit(value(family))); assertEquals(1, commands.size()); var whole = apply(before, commands.removeFirst()); assertValid(whole);
            local.forEach(name -> assertFalse(whole.properties().containsKey(p(name)), name)); assertEquals(before.slots(), whole.slots());
            before.properties().forEach((name, value) -> { if (!local.contains(name.value())) assertEquals(value, whole.properties().get(name)); });
            for (String leaf : local) {
                var properties = new LinkedHashMap<>(whole.properties()); properties.putAll(sparsePrerequisites(leaf)); if (!sparsePrerequisites(leaf).isEmpty()) properties.remove(p(family));
                var sparse = new WidgetNode(before.id(), DEF.typeId(), properties, before.slots());
                node.refreshPresentation(sparse, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, leaf).setValue(FlutterPropertyCellValue.explicit(value(leaf))); assertEquals(1, commands.size(), leaf);
                var restored = apply(sparse, commands.removeFirst()); assertValid(restored); assertFalse(restored.properties().containsKey(p(family))); assertEquals(before.slots(), restored.slots());
            }
        }
    }

    @Test void nineThumbIconModesAndMatchingImageErrorDependenciesAreIndependent() throws Exception {
        var prototype = prototype(); var before = new WidgetNode(prototype.id(), DEF.typeId(), full(), prototype.slots());
        for (String state : SwitchListTileWidgetPropertySchema.thumbIconStates()) {
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands); var bucket = SwitchListTileWidgetPropertySchema.thumbIconBucketProperties(state);
            cell(node, bucket.getFirst()).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("inherit"))); var inherited = apply(before, commands.removeFirst()); assertValid(inherited);
            bucket.stream().skip(1).forEach(name -> assertFalse(inherited.properties().containsKey(p(name))));
            before.properties().forEach((name, value) -> { if (!bucket.contains(name.value())) assertEquals(value, inherited.properties().get(name)); });
            node.refreshPresentation(inherited, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            cell(node, bucket.get(1)).setValue(FlutterPropertyCellValue.explicit(value(bucket.get(1)))); var icon = apply(inherited, commands.removeFirst()); assertValid(icon);
            assertEquals(new PropertyValue.StringValue("icon"), icon.properties().get(p(bucket.getFirst())));
        }
        for (String prefix : List.of("Active", "Inactive")) {
            String image = Character.toLowerCase(prefix.charAt(0)) + prefix.substring(1) + "ThumbImage", callback = "on" + prefix + "ThumbImageError";
            var commands = new ArrayList<DesignerCommand>(); var node = node(prototype, commands);
            assertTrue(assertThrows(IllegalArgumentException.class, () -> cell(node, callback).setValue(FlutterPropertyCellValue.explicit(value(callback)))).getMessage().contains("SwitchListTile")); assertTrue(commands.isEmpty());
            cell(node, callback).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())); var nullHandler = apply(prototype, commands.removeFirst()); assertValid(nullHandler);
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            cell(node, image).restoreDefaultValue(); var reset = apply(before, commands.removeFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(p(callback))); assertFalse(reset.properties().containsKey(p(image)));
            before.properties().forEach((name, value) -> { if (!List.of(image, callback).contains(name.value())) assertEquals(value, reset.properties().get(name)); });
        }
    }

    @Test void controlledBoolNullCallbackThreeLineCursorDefaultAndReadOnlyRemainExplicit() throws Exception {
        var empty = WidgetNodePrototypeFactory.create(DEF, StableId.random()); var commands = new ArrayList<DesignerCommand>(); var node = node(empty, commands);
        assertThrows(IllegalArgumentException.class, () -> cell(node, "value").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> cell(node, "isThreeLine").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)))).getMessage().contains("Subtitle"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> cell(node, "mouseCursorPressed").setValue(FlutterPropertyCellValue.explicit(value("mouseCursorPressed")))).getMessage().contains("Default")); assertTrue(commands.isEmpty());
        cell(node, "onChanged").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())); var disabled = apply(empty, commands.removeFirst()); assertValid(disabled);
        assertEquals(empty.properties().get(p("value")), disabled.properties().get(p("value"))); assertEquals(empty.slots(), disabled.slots());
        var row = cell(node, "value"); node.refreshPresentation(disabled, DEF, null, null, null, FlutterImageAssetChoices.empty());
        assertSame(row, cell(node, "value")); assertFalse(row.canWrite()); assertThrows(IllegalAccessException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)))); assertTrue(commands.isEmpty());
        assertEquals(4, WidgetEventCatalog.eventsFor(DEF).size()); assertTrue(WidgetEventCatalog.eventsFor(DEF).stream().allMatch(event -> event.kind() == WidgetEventDescriptor.Kind.EVENT));
    }

    @Test void requiredCallbackNoopNullAndTypedFactoryDraftsCommitOnlyOnOkAndRespectWithdrawnAuthority() throws Exception {
        var prototype = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(prototype, commands); var row = cell(node, "onChanged");
        for (String branch : List.of("Explicit null", FlutterPresetDartReferenceEditorComponent.PRESET, FlutterPresetDartReferenceEditorComponent.PROJECT)) {
            SwingUtilities.invokeAndWait(() -> {
                var editor = row.getPropertyEditor(); editor.setValue(rowValue(prototype, "onChanged"));
                var env = PropertyEnv.create(row); ((ExPropertyEditor) editor).attachEnv(env); var panel = editor.getCustomEditor();
                var mode = find(panel, FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class);
                assertEquals(List.of("Explicit null", FlutterPresetDartReferenceEditorComponent.PRESET, FlutterPresetDartReferenceEditorComponent.PROJECT),
                        java.util.stream.IntStream.range(0, mode.getItemCount()).mapToObj(mode::getItemAt).toList());
                mode.setSelectedItem(branch);
                if (branch.equals(FlutterPresetDartReferenceEditorComponent.PROJECT)) {
                    find(panel, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME, JComboBox.class).setSelectedIndex(1);
                    find(panel, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME, JTextField.class).setText("package:app/callbacks.dart");
                    find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).setText("Callbacks");
                    find(panel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).setText("createChange");
                    find(panel, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME, JComboBox.class).setSelectedIndex(1);
                }
                var note = find(panel, FlutterPresetDartReferenceEditorComponent.NOTE_NAME, JTextArea.class).getText();
                assertFalse(note.contains("Enabled is independent")); assertFalse(note.contains("default cursor"));
                if (branch.equals("Explicit null")) assertTrue(note.contains("disables"), note);
                assertEquals(rowValue(prototype, "onChanged"), editor.getValue(), "Closing or cancelling a draft leaves the stored required callback untouched."); assertTrue(commands.isEmpty());
                env.setState(PropertyEnv.STATE_VALID);
                var expected = branch.equals("Explicit null") ? new PropertyValue.NullValue() : branch.equals(FlutterPresetDartReferenceEditorComponent.PRESET)
                        ? new PropertyValue.StringValue("noop") : new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/callbacks.dart"), "Callbacks", Optional.of("createChange"),
                                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
                assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue());
                var reopened = row.getPropertyEditor(); reopened.setValue(editor.getValue()); ((ExPropertyEditor) reopened).attachEnv(PropertyEnv.create(row));
                assertEquals(branch, find(reopened.getCustomEditor(), FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class).getSelectedItem());
            });
        }
        var editor = row.getPropertyEditor(); editor.setValue(rowValue(prototype, "onChanged")); var env = PropertyEnv.create(row); ((ExPropertyEditor) editor).attachEnv(env);
        SwingUtilities.invokeAndWait(() -> find(editor.getCustomEditor(), FlutterPresetDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem("Explicit null"));
        node.refreshPresentation(prototype, DEF, null, null, null, FlutterImageAssetChoices.empty()); SwingUtilities.invokeAndWait(() -> env.setState(PropertyEnv.STATE_VALID));
        assertThrows(IllegalAccessException.class, () -> row.setValue((FlutterPropertyCellValue) editor.getValue())); assertTrue(commands.isEmpty());
    }

    @Test void variantRefreshNotifiesOnlyChangedRowsAndKeepsInactiveFlagBooleanEditor() throws Exception {
        var prototype = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(prototype, commands);
        var variant = cell(node, "variant"); var flag = cell(node, "applyCupertinoTheme"); var title = Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(row -> row.getName().equals("title")).findFirst().orElseThrow();
        var sets = node.getPropertySets(); var changes = new ArrayList<String>(); node.addPropertyChangeListener(event -> changes.add(event.getPropertyName()));
        assertTrue(flag.getDisplayName().contains("inactive; retained"));
        var properties = new LinkedHashMap<>(prototype.properties()); properties.put(p("variant"), new PropertyValue.StringValue("adaptive"));
        var adaptive = new WidgetNode(prototype.id(), DEF.typeId(), properties, prototype.slots()); node.refreshPresentation(adaptive, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertEquals(Set.of("variant", "applyCupertinoTheme"), Set.copyOf(changes)); assertSame(variant, cell(node, "variant")); assertSame(flag, cell(node, "applyCupertinoTheme")); assertArrayEquals(sets, node.getPropertySets());
        assertFalse(flag.getDisplayName().contains("inactive")); assertTrue(title.canRead());
        properties.put(p("applyCupertinoTheme"), new PropertyValue.BooleanValue(false));
        node.refreshPresentation(new WidgetNode(prototype.id(), DEF.typeId(), properties, prototype.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertTrue(flag.getPropertyEditor().isPaintable()); assertNull(flag.getPropertyEditor().getTags());
    }

    /** Dense locals; SDK fixtures must declare assets/switch_thumb.png for both image provider variants. */
    public static LinkedHashMap<PropertyName, PropertyValue> full() { return full("standard", "roundedRectangle"); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, String shapeKind) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value(); if (SwitchListTileWidgetPropertySchema.colorFamilies().contains(name) || List.of("thumbIcon", "shape", "visualDensity", "mouseCursor").contains(name)) continue;
            if (SwitchListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name) && !SwitchListTileWidgetPropertySchema.shapePropertyAppliesToKind(name, shapeKind)) continue;
            result.put(field.name(), switch (name) {
                case "variant" -> new PropertyValue.StringValue(variant);
                case "shapeKind" -> new PropertyValue.StringValue(shapeKind);
                default -> value(name);
            });
        }
        return result;
    }
    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) {
        if (SwitchListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name) && !name.equals("mouseCursorDefault")) return Map.of(p("mouseCursorDefault"), value("mouseCursorDefault"));
        return SwitchPropertyContractTest.sparsePrerequisites(name);
    }
    public static PropertyValue value(String name) {
        if (name.equals("controlAffinity")) return new PropertyValue.EnumValue("ListTileControlAffinity", "leading");
        if (SwitchListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name)) return CardPropertyContractTest.value(name);
        if (SwitchListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)) return new PropertyValue.StringValue("click");
        if (ListTileWidgetPropertySchema.find(name).isPresent() && SwitchWidgetPropertySchema.find(name).isEmpty()) return CheckboxListTilePropertyContractTest.value(name);
        return SwitchPropertyContractTest.value(name);
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FloatingActionButtonPropertyContractTest.reference(name); }
    public static WidgetNode prototype() {
        var base = WidgetNodePrototypeFactory.create(DEF, StableId.random()); var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        for (String name : List.of("title", "subtitle", "secondary")) slots.put(new SlotName(name), WidgetSlot.SingleSlot.of(new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), new PropertyValue.StringValue(name)), Map.of())));
        return new WidgetNode(base.id(), DEF.typeId(), base.properties(), slots);
    }
    private static List<String> locals(String family) {
        return switch (family) {
            case "shape" -> SwitchListTileWidgetPropertySchema.builtInShapePropertyNames();
            case "thumbIcon" -> SwitchListTileWidgetPropertySchema.thumbIconLocalProperties();
            case "visualDensity" -> List.of("visualDensityHorizontal", "visualDensityVertical");
            case "mouseCursor" -> SwitchListTileWidgetPropertySchema.mouseCursorStateProperties();
            default -> SwitchListTileWidgetPropertySchema.colorStateProperties(family);
        };
    }
    static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return FloatingActionButtonPropertyContractTest.apply(widget, command); }
    static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(property -> property.getName().equals(name)).findFirst().orElseThrow(); }
    static void assertValid(WidgetNode widget) { ListTilePropertyContractTest.assertValid(widget); }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static FlutterPropertyCellValue rowValue(WidgetNode widget, String name) { return new FlutterPropertyCellValue(Optional.ofNullable(widget.properties().get(p(name)))); }
    private static <T extends Component> T find(Component component, String name, Class<T> type) {
        if (name.equals(component.getName()) && type.isInstance(component)) return type.cast(component);
        if (component instanceof Container container) for (var child : container.getComponents()) {
            try { return find(child, name, type); } catch (IllegalArgumentException ignored) { }
        }
        throw new IllegalArgumentException("Missing component " + name);
    }
}
