package io.github.vgrytsenko2022.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetDefinition;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.events.WidgetEventDescriptor;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.model.StateBinding;
import io.github.vgrytsenko2022.designer.model.StatePropertyBinding;
import io.github.vgrytsenko2022.designer.state.WidgetStateBindingCatalog;
import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class FlutterWidgetEventsTest {
    private static final StableId ID = StableId.parse("ec2b958a-0a61-4e6b-9bfa-dde399f05201");

    @Test
    void everyCatalogEventMovesExactlyOnceAndOtherPropertiesAndSlotsArePreserved() {
        for (WidgetDefinition definition : BuiltInWidgetCatalog.getDefault().definitions()) {
            var creation = definition.typeId().value().equals("flutter.widgets.Image")
                    ? Map.of(new PropertyName("image"),
                            PropertyValue.ImageProviderValue.asset("assets/event-fixture.png"))
                    : Map.<PropertyName, PropertyValue.ImageProviderValue>of();
            var widget = WidgetNodePrototypeFactory.create(definition, ID, creation);
            var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition, ignored -> {});
            var events = WidgetEventCatalog.eventsFor(definition).stream()
                    .filter(event -> event.kind() == WidgetEventDescriptor.Kind.EVENT).toList();
            var rows = Arrays.stream(node.getPropertySets())
                    .filter(set -> !set.getName().equals(FlutterWidgetPropertiesNode.IDENTITY_SET_NAME)
                            && !set.getName().equals(FlutterWidgetPropertiesNode.SLOTS_SET_NAME))
                    .flatMap(set -> Arrays.stream(set.getProperties())).map(Node.Property::getName)
                    .filter(name -> !java.util.Set.of("tableGrid", "dataTableGrid").contains(name)).toList();
            assertEquals(definition.properties().size(), rows.size(), definition.typeId().value());
            assertEquals(rows.size(), rows.stream().distinct().count(), definition.typeId().value());
            assertEquals(definition.properties().stream().map(property -> property.name().value()).sorted().toList(),
                    rows.stream().sorted().toList(), definition.typeId().value());
            var eventSet = Arrays.stream(node.getPropertySets())
                    .filter(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst();
            assertEquals(!events.isEmpty(), eventSet.isPresent(), definition.typeId().value());
            if (eventSet.isPresent()) {
                assertEquals("Events", eventSet.orElseThrow().getValue("tabName"));
                assertEquals(events.stream().map(event -> event.propertyName().value()).toList(),
                        Arrays.stream(eventSet.orElseThrow().getProperties()).map(Node.Property::getName).toList());
            }
            for (var callback : WidgetEventCatalog.eventsFor(definition)) {
                if (callback.kind() != WidgetEventDescriptor.Kind.EVENT) {
                    assertFalse(eventSet.stream().flatMap(set -> Arrays.stream(set.getProperties()))
                            .anyMatch(property -> property.getName().equals(callback.propertyName().value())));
                }
            }
        }
    }

    @Test
    void eventAuthorityRefreshRetainsNodeSetsAndRowsAndInvalidatesOldEditors() throws Exception {
        WidgetDefinition definition = definition("flutter.material.TextField");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, ID);
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition, ignored -> {});
        var sets = node.getPropertySets();
        var row = property(node, "onChanged");
        assertFalse(row.getPropertyEditor() instanceof FlutterWidgetEventPropertyEditor,
                "Unwired contexts retain working legacy callback editing, not inactive new actions.");
        var context = new TestContext();
        context.discovered = new CompletableFuture<>();
        node.updateEventsContext(context);
        assertSame(sets[0], node.getPropertySets()[0]);
        assertSame(row, property(node, "onChanged"));
        var editor = assertInstanceOf(FlutterWidgetEventPropertyEditor.class, row.getPropertyEditor());
        AtomicReference<Component> panel = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> panel.set(editor.getCustomEditor()));
        assertEquals(0, context.changes.get(), "Opening the editor only discovers callable choices.");
        node.refreshPresentation(widget, definition, ignored -> {}, null, null, FlutterImageAssetChoices.empty());
        node.updateEventsContext(new TestContext());
        context.discovered.complete(List.of(new FlutterWidgetEventsContext.Handler("old", "old", "void Function(String)")));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(0, find(panel.get(), FlutterWidgetEventPropertyEditor.HANDLERS_NAME, JList.class).getModel().getSize());
            assertTrue(find(panel.get(), FlutterWidgetEventPropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
            assertFalse(find(panel.get(), FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).isEnabled());
        });
        assertSame(row, property(node, "onChanged"));
        assertEquals(0, context.changes.get());
    }

    @Test
    void createIsAnExplicitAsyncOperationAndReadOnlyOpeningDoesNotSubmitPropertyMutation() throws Exception {
        WidgetDefinition definition = definition("flutter.material.TextField");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, ID);
        AtomicInteger ordinaryMutations = new AtomicInteger();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition,
                ignored -> ordinaryMutations.incrementAndGet());
        var context = new TestContext();
        node.updateEventsContext(context);
        var editor = property(node, "onChanged").getPropertyEditor();
        SwingUtilities.invokeAndWait(() -> {
            Component panel = editor.getCustomEditor();
            assertEquals("Edit the handler in Source, then Save to analyze the current code. Invalid code stays in the editor unsaved.",
                    find(panel, FlutterWidgetEventPropertyEditor.SAVE_GUIDANCE_NAME, JLabel.class).getText());
            assertEquals(0, context.changes.get());
            find(panel, FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).doClick();
            assertEquals(1, context.changes.get());
            assertEquals("_textFieldOnChanged", context.createdName);
            assertEquals(new PropertyName("onChanged"), context.changedEvent);
        });
        assertEquals(0, ordinaryMutations.get());
        assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
    }

    @Test
    void summariesDistinguishAbsentExplicitNullNoopBindingAndDisabledState() {
        WidgetNode text = WidgetNodePrototypeFactory.create(definition("flutter.material.TextField"), ID);
        PropertyName onChanged = new PropertyName("onChanged");
        assertEquals("<none>", FlutterWidgetEventPropertyEditor.summary(text, onChanged, null));
        assertEquals("null (explicit)", FlutterWidgetEventPropertyEditor.summary(text, onChanged, new PropertyValue.NullValue()));
        assertEquals("<no-op>", FlutterWidgetEventPropertyEditor.summary(text, onChanged, new PropertyValue.StringValue("noop")));
        assertEquals("_changed", FlutterWidgetEventPropertyEditor.summary(text, onChanged, new PropertyValue.CallbackValue("_changed")));
        WidgetNode toggle = WidgetNodePrototypeFactory.create(definition("flutter.material.Switch"), ID);
        assertEquals("<none> (generated no-op)", FlutterWidgetEventPropertyEditor.summary(toggle, onChanged, null));
        var properties = new LinkedHashMap<>(toggle.properties());
        properties.put(new PropertyName("enabled"), new PropertyValue.BooleanValue(false));
        WidgetNode disabled = new WidgetNode(toggle.id(), toggle.type(), properties, toggle.slots(), toggle.extensions());
        assertEquals("<none> (emits null) (widget disabled)", FlutterWidgetEventPropertyEditor.summary(disabled, onChanged, null));
        assertEquals("_changed (widget disabled)", FlutterWidgetEventPropertyEditor.summary(disabled, onChanged, new PropertyValue.CallbackValue("_changed")));
    }

    @Test
    void unavailableContextExplainsReasonWithoutDiscoveringOrMutating() throws Exception {
        var definition = definition("flutter.material.TextField");
        var widget = WidgetNodePrototypeFactory.create(definition, ID);
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition, ignored -> {});
        var context = new TestContext() {
            @Override public String unavailableReason() { return "Cannot edit onChanged: analysis has not completed."; }
            @Override public CompletionStage<List<Handler>> discover(PropertyName event) {
                fail("Unavailable admission must not invoke discovery");
                return super.discover(event);
            }
        };
        node.updateEventsContext(context);
        SwingUtilities.invokeAndWait(() -> {
            Component panel = property(node, "onChanged").getPropertyEditor().getCustomEditor();
            assertFalse(find(panel, FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).isEnabled());
            assertTrue(find(panel, FlutterWidgetEventPropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("analysis has not completed"));
        });
    }

    @Test
    void importedReferenceDraftIsAppliedOnlyThroughExplicitTypedEventAction() throws Exception {
        var definition = definition("flutter.material.TextButton");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var reference = new PropertyValue.DartObjectReferenceValue(
                java.util.Optional.of("package:app/handlers.dart"), "Handlers",
                java.util.Optional.of("pressed"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                java.util.Optional.empty());
        var properties = new LinkedHashMap<>(prototype.properties());
        PropertyName pressed = new PropertyName("onPressed");
        properties.put(pressed, reference);
        var widget = new WidgetNode(ID, definition.typeId(), properties, prototype.slots());
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition,
                ignored -> fail("The Reference action must use its revision-bound Events context."));
        var context = new TestContext();
        node.updateEventsContext(context);
        SwingUtilities.invokeAndWait(() -> {
            var editor = property(node, "onPressed").getPropertyEditor();
            Component panel = editor.getCustomEditor();
            assertEquals("package:app/handlers.dart",
                    find(panel, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME, JTextField.class).getText());
            find(panel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).setText("pressedAgain");
            assertEquals(0, context.changes.get());
            assertEquals(FlutterPropertyCellValue.explicit(reference), editor.getValue());
            find(panel, FlutterWidgetEventPropertyEditor.REFERENCE_APPLY_NAME, JButton.class).doClick();
            assertEquals(1, context.changes.get());
            assertEquals(pressed, context.changedEvent);
            assertEquals(java.util.Optional.of(new PropertyValue.DartObjectReferenceValue(
                    reference.libraryUri(), reference.rootSymbol(), java.util.Optional.of("pressedAgain"),
                    reference.access(), reference.constant())), context.editedValue);
        });
    }

    @Test
    void stateBindingPageOffersExplicitTypedCreateForEachSupportedControlEvent() throws Exception {
        for (var definition : BuiltInWidgetCatalog.getDefault().definitions().stream()
                .filter(WidgetStateBindingCatalog::supports).toList()) {
            var prototype = WidgetNodePrototypeFactory.create(definition, ID);
            var values = new LinkedHashMap<>(prototype.properties());
            if (definition.typeId().value().equals("flutter.material.IconButton")) {
                values.put(new PropertyName("isSelected"), new PropertyValue.BooleanValue(false));
            }
            var widget = new WidgetNode(ID, prototype.type(), values, prototype.slots());
            var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition,
                    ignored -> fail("State creation must be an atomic context action."));
            var context = new TestContext();
            node.updateEventsContext(context);
            SwingUtilities.invokeAndWait(() -> {
                var panel = property(node, WidgetStateBindingCatalog.find(widget).orElseThrow().eventProperty().value()).getPropertyEditor().getCustomEditor();
                assertEquals(0, context.changes.get());
                assertTrue(find(panel, FlutterWidgetEventPropertyEditor.STATE_TYPE_NAME, JLabel.class).getText()
                        .startsWith(WidgetStateBindingCatalog.find(widget).orElseThrow().dartType()));
                assertEquals("Initial value / Canvas preview:",
                        find(panel, "flutter.event.state.previewLabel", JLabel.class).getText());
                assertTrue(find(panel, FlutterWidgetEventPropertyEditor.STATE_GUIDANCE_NAME, JTextArea.class)
                        .getText().contains("only Canvas preview"));
                find(panel, FlutterWidgetEventPropertyEditor.STATE_FIELD_NAME, JTextField.class).setText("_selectedValue");
                find(panel, FlutterWidgetEventPropertyEditor.STATE_HANDLER_NAME, JTextField.class).setText("_changeValue");
                assertFalse(find(panel, FlutterWidgetEventPropertyEditor.STATE_REMOVE_NAME, JButton.class).isEnabled());
                find(panel, FlutterWidgetEventPropertyEditor.STATE_CREATE_NAME, JButton.class).doClick();
                assertEquals("_selectedValue", context.stateField);
                assertEquals("_changeValue", context.stateHandler);
                assertEquals(1, context.changes.get());
            });
        }
        var definition = definition("flutter.material.TextButton");
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, WidgetNodePrototypeFactory.create(definition, ID), definition, ignored -> {});
        node.updateEventsContext(new TestContext());
        SwingUtilities.invokeAndWait(() -> assertThrows(IllegalArgumentException.class, () ->
                find(property(node, "onPressed").getPropertyEditor().getCustomEditor(),
                        FlutterWidgetEventPropertyEditor.STATE_CREATE_NAME, JButton.class)));
    }

    @Test
    void stateBindingRemoveAndUnavailableGuidancePreserveOtherEventActions() throws Exception {
        var definition = definition("flutter.material.Switch");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var binding = WidgetStateBindingCatalog.createBinding(prototype, "_checked", "_changed");
        var widget = new WidgetNode(ID, definition.typeId(), prototype.properties(), prototype.slots(),
                prototype.extensions(), java.util.Optional.of(binding));
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition, ignored -> {});
        var context = new TestContext();
        node.updateEventsContext(context);
        SwingUtilities.invokeAndWait(() -> {
            var panel = property(node, "onChanged").getPropertyEditor().getCustomEditor();
            assertFalse(find(panel, FlutterWidgetEventPropertyEditor.STATE_CREATE_NAME, JButton.class).isEnabled());
            assertEquals("_checked", find(panel, FlutterWidgetEventPropertyEditor.STATE_FIELD_NAME, JTextField.class).getText());
            assertEquals("Canvas preview:", find(panel, "flutter.event.state.previewLabel", JLabel.class).getText());
            find(panel, FlutterWidgetEventPropertyEditor.STATE_REMOVE_NAME, JButton.class).doClick();
            assertEquals(1, context.stateRemovals);
        });
        var unavailable = new TestContext() {
            @Override public String stateBindingUnavailableReason() { return "State binding requires a Stateful form."; }
        };
        node.updateEventsContext(unavailable);
        SwingUtilities.invokeAndWait(() -> {
            var panel = property(node, "onChanged").getPropertyEditor().getCustomEditor();
            assertFalse(find(panel, FlutterWidgetEventPropertyEditor.STATE_REMOVE_NAME, JButton.class).isEnabled());
            assertTrue(find(panel, FlutterWidgetEventPropertyEditor.STATE_GUIDANCE_NAME, JTextArea.class)
                    .getText().contains("requires a Stateful"));
            assertTrue(find(panel, FlutterWidgetEventPropertyEditor.CREATE_NAME, JButton.class).isEnabled());
        });
    }

    @Test
    void boundValueRowsRemainStableEditablePreviewsAndRejectIncompatibleCompanionType() throws Exception {
        var definition = definition("flutter.material.Checkbox");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var binding = WidgetStateBindingCatalog.createBinding(prototype, "_checked", "_changed");
        var widget = new WidgetNode(ID, definition.typeId(), prototype.properties(), prototype.slots(),
                prototype.extensions(), java.util.Optional.of(binding));
        var changes = new java.util.ArrayList<io.github.vgrytsenko2022.designer.command.DesignerCommand>();
        FlutterWidgetPropertiesNode.PropertyMutationHandler mutation = changes::add;
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, prototype, definition, mutation);
        var row = property(node, "value");
        assertFalse(row.getDisplayName().contains("preview"));
        node.refreshPresentation(widget, definition, mutation, null, null, FlutterImageAssetChoices.empty());
        assertSame(row, property(node, "value"));
        assertTrue(row.getDisplayName().contains("preview; _checked"));
        assertTrue(row.getShortDescription().contains("does not change the field initializer"));
        @SuppressWarnings("unchecked") var valueRow = (Node.Property<FlutterPropertyCellValue>) row;
        valueRow.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)));
        assertEquals(1, changes.size());
        assertEquals(new io.github.vgrytsenko2022.designer.command.SetProperty(ID, new PropertyName("value"),
                new PropertyValue.BooleanValue(true)), changes.getFirst());
        @SuppressWarnings("unchecked") var tristate = (Node.Property<FlutterPropertyCellValue>) property(node, "tristate");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> tristate.setValue(
                FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)))).getMessage().contains("Remove the binding"));
        assertEquals(1, changes.size());
        node.refreshPresentation(prototype, definition, mutation, null, null, FlutterImageAssetChoices.empty());
        assertSame(row, property(node, "value"));
        assertFalse(row.getDisplayName().contains("preview"));
    }

    @Test
    void boundSliderBoundsRequireExplicitRemovalWhilePreviewEditsAndNoopsRemainAvailable() throws Exception {
        for (String type : List.of("flutter.material.Slider", "flutter.material.RangeSlider")) {
            var definition = definition(type);
            var prototype = WidgetNodePrototypeFactory.create(definition, ID);
            var properties = new LinkedHashMap<>(prototype.properties());
            properties.put(new PropertyName("min"), new PropertyValue.IntegerValue(java.math.BigInteger.ZERO));
            properties.put(new PropertyName("max"), new PropertyValue.IntegerValue(java.math.BigInteger.ONE));
            var plain = new WidgetNode(ID, definition.typeId(), properties, prototype.slots());
            var binding = WidgetStateBindingCatalog.createBinding(plain, "_value", "_changed");
            var bound = new WidgetNode(ID, plain.type(), properties, plain.slots(), plain.extensions(), java.util.Optional.of(binding));
            var changes = new java.util.ArrayList<io.github.vgrytsenko2022.designer.command.DesignerCommand>();
            FlutterWidgetPropertiesNode.PropertyMutationHandler mutation = changes::add;
            var node = new FlutterWidgetPropertiesNode(Children.LEAF, bound, definition, mutation);
            @SuppressWarnings("unchecked") var minimum = (Node.Property<FlutterPropertyCellValue>) property(node, "min");
            @SuppressWarnings("unchecked") var maximum = (Node.Property<FlutterPropertyCellValue>) property(node, "max");
            minimum.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(java.math.BigInteger.ZERO)));
            maximum.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(java.math.BigInteger.ONE)));
            assertTrue(changes.isEmpty(), "Unchanged explicit bounds remain no-ops.");
            assertTrue(assertThrows(IllegalArgumentException.class, () -> minimum.setValue(
                    FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new java.math.BigDecimal("0.5")))))
                    .getMessage().contains("Remove the State binding"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> maximum.setValue(
                    FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(java.math.BigInteger.TWO))))
                    .getMessage().contains("retained runtime field"));
            assertThrows(IllegalArgumentException.class, minimum::restoreDefaultValue);
            assertThrows(IllegalArgumentException.class, maximum::restoreDefaultValue);
            assertTrue(changes.isEmpty());
            String valueName = type.endsWith("RangeSlider") ? "valuesStart" : "value";
            @SuppressWarnings("unchecked") var preview = (Node.Property<FlutterPropertyCellValue>) property(node, valueName);
            preview.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new java.math.BigDecimal("0.8"))));
            assertEquals(1, changes.size(), "Bound value leaves are still editable Canvas previews.");
            node.refreshPresentation(plain, definition, mutation, null, null, FlutterImageAssetChoices.empty());
            assertSame(minimum, property(node, "min"));
            minimum.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(-1))));
            assertEquals(2, changes.size(), "Bounds are editable again after removing State binding.");
        }
    }

    private static WidgetDefinition definition(String type) {
        return BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow();
    }

    private static Node.Property<?> property(FlutterWidgetPropertiesNode node, String name) {
        return Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties()))
                .filter(property -> property.getName().equals(name)).findFirst().orElseThrow();
    }

    private static <T extends Component> T find(Component component, String name, Class<T> type) {
        if (name.equals(component.getName()) && type.isInstance(component)) return type.cast(component);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                try { return find(child, name, type); } catch (IllegalArgumentException ignored) { }
            }
        }
        throw new IllegalArgumentException("Missing component: " + name);
    }

    private static class TestContext implements FlutterWidgetEventsContext {
        CompletableFuture<List<Handler>> discovered = CompletableFuture.completedFuture(List.of());
        final AtomicInteger changes = new AtomicInteger();
        String createdName;
        PropertyName changedEvent;
        java.util.Optional<PropertyValue> editedValue;
        String stateField;
        String stateHandler;
        int stateRemovals;
        StateBinding.Action stateAction;
        String initialText;
        java.util.Optional<StatePropertyBinding> reusedField;
        java.util.Optional<PropertyValue> selectedValue;
        @Override public String stateBindingUnavailableReason() { return ""; }
        @Override public CompletionStage<Void> createStateBinding(String field, String handler) {
            stateField = field;
            stateHandler = handler;
            return changed(WidgetStateBindingCatalog.ON_CHANGED);
        }
        @Override public CompletionStage<Void> createStateBinding(String field, String handler,
                StateBinding.Action action, java.util.Optional<PropertyValue> selection,
                String text, java.util.Optional<StatePropertyBinding> reused) {
            stateAction = action;
            initialText = text;
            reusedField = reused;
            selectedValue = selection;
            return createStateBinding(field, handler);
        }
        @Override public CompletionStage<Void> removeStateBinding() {
            stateRemovals++;
            return changed(WidgetStateBindingCatalog.ON_CHANGED);
        }
        @Override public CompletionStage<List<Handler>> discover(PropertyName event) { return discovered; }
        @Override public CompletionStage<Void> create(PropertyName event, String name) {
            createdName = name;
            return changed(event);
        }
        @Override public CompletionStage<Void> bind(PropertyName event, Handler handler) { return changed(event); }
        @Override public CompletionStage<Void> editBinding(PropertyName event, java.util.Optional<PropertyValue> value) {
            editedValue = value;
            return changed(event);
        }
        @Override public CompletionStage<Void> navigate(PropertyName event) { return CompletableFuture.completedFuture(null); }
        @Override public CompletionStage<Void> rename(PropertyName event, String name) { return changed(event); }
        @Override public CompletionStage<Void> disconnect(PropertyName event) { return changed(event); }
        private CompletionStage<Void> changed(PropertyName event) {
            changedEvent = event;
            changes.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
    }
}
