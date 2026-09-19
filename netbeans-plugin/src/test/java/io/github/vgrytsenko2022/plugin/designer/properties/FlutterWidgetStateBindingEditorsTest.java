package io.github.vgrytsenko2022.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.StateBinding;
import io.github.vgrytsenko2022.designer.model.StatePropertyBinding;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.state.WidgetStateBindingCatalog;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class FlutterWidgetStateBindingEditorsTest {
    private static final StableId ID = StableId.parse("49a8a94e-07b5-4ce9-80ae-029d66c59b9c");
    private static StatePropertyBinding source(String name, StateBinding.Type type) {
        return new StatePropertyBinding(name, type, Optional.empty(), StatePropertyBinding.Transform.DIRECT, Optional.empty());
    }

    @Test
    void genericPropertyEditorPreservesLiteralDraftAndSubmitsOnlyExplicitTypedBinding() throws Exception {
        var context = new Context();
        var field = source("_amount", StateBinding.Type.DOUBLE);
        context.fields = CompletableFuture.completedFuture(List.of(field));
        var node = node("flutter.widgets.Text", context);
        var row = property(node, "data");
        var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor());
        var before = editor.getValue();
        SwingUtilities.invokeAndWait(() -> {
            Component panel = editor.getCustomEditor();
            assertEquals(0, context.changes);
            find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class)
                    .setSelectedItem(StatePropertyBinding.Transform.TO_STRING);
            assertEquals(before, editor.getValue(), "Choosing binding options does not overwrite the literal draft.");
            find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
            assertEquals(1, context.changes);
            assertEquals(new PropertyName("data"), context.property);
            assertEquals(new StatePropertyBinding("_amount", StateBinding.Type.DOUBLE, Optional.empty(),
                    StatePropertyBinding.Transform.TO_STRING, Optional.empty()), context.binding);
        });
    }

    @Test
    void tooltipFiveStateConsumersPreserveContentCallbackAndAnchorWithoutVisibilityProducer() throws Exception {
        for (String name : List.of("preferBelow", "excludeFromSemantics", "enableTapToDismiss", "enableFeedback", "ignorePointer")) {
            for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
                var context = new Context(); var field = source("_tooltipFlag", StateBinding.Type.BOOL); context.fields = CompletableFuture.completedFuture(List.of(field));
                var node = node("flutter.material.Tooltip", context); var row = property(node, name);
                SwingUtilities.invokeAndWait(() -> {
                    var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor()); var before = editor.getValue(); var panel = editor.getCustomEditor();
                    find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                    assertEquals(before, editor.getValue()); assertEquals(0, context.changes); find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                    assertEquals(1, context.changes); assertEquals(new PropertyName(name), context.property);
                    assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform), context.binding);
                });
                var plain = TooltipPropertyContractTest.prototype(); var properties = new java.util.LinkedHashMap<>(plain.properties()); var literal = new PropertyValue.BooleanValue(false);
                properties.put(new PropertyName(name), literal); properties.put(new PropertyName("onTriggered"), new PropertyValue.StringValue("noop"));
                var bound = new WidgetNode(ID, plain.type(), properties, plain.slots(), plain.extensions(), Optional.empty(), Map.of(new PropertyName(name), context.binding));
                node.refreshPresentation(bound, TooltipPropertyContractTest.DEF, ignored -> {}, null, null, FlutterImageAssetChoices.empty()); node.updateEventsContext(context);
                assertSame(row, property(node, name)); assertEquals(FlutterPropertyCellValue.explicit(literal), row.getPropertyEditor().getValue()); assertTrue(row.getPropertyEditor().isPaintable()); assertNull(row.getPropertyEditor().getTags());
                assertFalse(property(node, "message").getPropertyEditor() instanceof FlutterWidgetStatePropertyEditor);
                assertTrue(WidgetStateBindingCatalog.find(bound).isEmpty()); assertTrue(bound.stateBinding().isEmpty()); assertEquals(plain.slots(), bound.slots());
                assertEquals(plain.properties().get(new PropertyName("message")), bound.properties().get(new PropertyName("message"))); assertEquals(new PropertyValue.StringValue("noop"), bound.properties().get(new PropertyName("onTriggered")));
            }
        }
    }

    @Test
    void tooltipThemeThreeStateConsumersKeepNullableLiteralAndRejectStaleOrCancelledDrafts() throws Exception {
        for (String name : List.of("preferBelow", "excludeFromSemantics", "enableFeedback"))
        for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
            var context = new Context(); var field = source("_themeBelow", StateBinding.Type.BOOL);
            context.fields = CompletableFuture.completedFuture(List.of(field));
            var plain = TooltipThemePropertyContractTest.prototype();
            var node = new FlutterWidgetPropertiesNode(Children.LEAF, plain, TooltipThemePropertyContractTest.DEF, ignored -> fail("Binding must not dispatch a literal mutation."));
            node.updateEventsContext(context); var row = property(node, name);
            SwingUtilities.invokeAndWait(() -> {
                var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor());
                var original = editor.getValue(); var cancelled = editor.getCustomEditor();
                find(cancelled, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                assertEquals(0, context.changes); assertEquals(original, editor.getValue());
                var panel = row.getPropertyEditor().getCustomEditor();
                find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(new PropertyName(name), context.property);
                assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform), context.binding);
            });
            var literal = new PropertyValue.BooleanValue(false);
            var bound = new WidgetNode(plain.id(), plain.type(), Map.of(new PropertyName(name), literal), plain.slots(), plain.extensions(), Optional.empty(), Map.of(new PropertyName(name), context.binding));
            node.refreshPresentation(bound, TooltipThemePropertyContractTest.DEF, ignored -> {}, null, null, FlutterImageAssetChoices.empty()); node.updateEventsContext(context);
            assertSame(row, property(node, name)); assertEquals(FlutterPropertyCellValue.explicit(literal), row.getPropertyEditor().getValue());
            assertTrue(row.getPropertyEditor().isPaintable()); assertNull(row.getPropertyEditor().getTags()); assertTrue(row.supportsDefaultValue());
            assertEquals(plain.slots(), bound.slots()); assertTrue(bound.stateBinding().isEmpty());
            SwingUtilities.invokeAndWait(() -> {
                var stale = row.getPropertyEditor().getCustomEditor(); var next = new Context();
                node.updateEventsContext(next);
                find(stale, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(0, next.changes);
                assertTrue(find(stale, FlutterWidgetStatePropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
            });
        }
    }

    @Test
    void menuItemButtonFourStateConsumersKeepLiteralActivationAndRejectStaleOrCancelledDrafts() throws Exception {
        for (String name : List.of("enabled", "autofocus", "requestFocusOnHover", "closeOnActivate"))
        for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
            var context = new Context(); var field = source("_menuEnabled", StateBinding.Type.BOOL);
            context.fields = CompletableFuture.completedFuture(List.of(field));
            var plain = MenuItemButtonPropertyContractTest.prototype();
            var node = new FlutterWidgetPropertiesNode(Children.LEAF, plain, MenuItemButtonPropertyContractTest.DEF, ignored -> fail("Binding must not dispatch a literal mutation."));
            node.updateEventsContext(context); var row = property(node, name);
            SwingUtilities.invokeAndWait(() -> {
                var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor());
                var original = editor.getValue(); var cancelled = editor.getCustomEditor();
                find(cancelled, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                assertEquals(0, context.changes); assertEquals(original, editor.getValue());
                var panel = row.getPropertyEditor().getCustomEditor();
                find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(new PropertyName(name), context.property);
                assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform), context.binding);
            });
            var literal = new PropertyValue.BooleanValue(false);
            var bound = new WidgetNode(plain.id(), plain.type(), menuStateLiteralProperties(plain, name, literal), plain.slots(), plain.extensions(), Optional.empty(), Map.of(new PropertyName(name), context.binding));
            node.refreshPresentation(bound, MenuItemButtonPropertyContractTest.DEF, ignored -> {}, null, null, FlutterImageAssetChoices.empty()); node.updateEventsContext(context);
            assertSame(row, property(node, name)); assertEquals(FlutterPropertyCellValue.explicit(literal), row.getPropertyEditor().getValue());
            assertTrue(row.getPropertyEditor().isPaintable()); assertNull(row.getPropertyEditor().getTags()); assertEquals(!name.equals("enabled"), row.supportsDefaultValue());
            assertEquals(plain.slots(), bound.slots()); assertTrue(bound.stateBinding().isEmpty());
            SwingUtilities.invokeAndWait(() -> {
                var stale = row.getPropertyEditor().getCustomEditor(); var next = new Context();
                node.updateEventsContext(next);
                find(stale, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(0, next.changes);
                assertTrue(find(stale, FlutterWidgetStatePropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
            });
        }
    }

    @Test
    void menuAnchorFourEffectiveStateConsumersKeepLiteralFocusAndRejectStaleOrCancelledDrafts() throws Exception {
        for (String name : List.of("consumeOutsideTap", "crossAxisUnconstrained", "useRootOverlay", "animated"))
        for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
            var context = new Context(); var field = source("_menuEnabled", StateBinding.Type.BOOL);
            context.fields = CompletableFuture.completedFuture(List.of(field));
            var plain = MenuAnchorPropertyContractTest.prototype();
            var node = new FlutterWidgetPropertiesNode(Children.LEAF, plain, MenuAnchorPropertyContractTest.DEF, ignored -> fail("Binding must not dispatch a literal mutation."));
            node.updateEventsContext(context); var row = property(node, name);
            SwingUtilities.invokeAndWait(() -> {
                var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor());
                var original = editor.getValue(); var cancelled = editor.getCustomEditor();
                find(cancelled, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                assertEquals(0, context.changes); assertEquals(original, editor.getValue());
                var panel = row.getPropertyEditor().getCustomEditor();
                find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(new PropertyName(name), context.property);
                assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform), context.binding);
            });
            var literal = new PropertyValue.BooleanValue(false);
            var bound = new WidgetNode(plain.id(), plain.type(), menuStateLiteralProperties(plain, name, literal), plain.slots(), plain.extensions(), Optional.empty(), Map.of(new PropertyName(name), context.binding));
            node.refreshPresentation(bound, MenuAnchorPropertyContractTest.DEF, ignored -> {}, null, null, FlutterImageAssetChoices.empty()); node.updateEventsContext(context);
            assertSame(row, property(node, name)); assertEquals(FlutterPropertyCellValue.explicit(literal), row.getPropertyEditor().getValue());
            assertTrue(row.getPropertyEditor().isPaintable()); assertNull(row.getPropertyEditor().getTags()); assertTrue(row.supportsDefaultValue());
            assertEquals(plain.slots(), bound.slots()); assertTrue(bound.stateBinding().isEmpty());
            SwingUtilities.invokeAndWait(() -> {
                var stale = row.getPropertyEditor().getCustomEditor(); var next = new Context();
                node.updateEventsContext(next);
                find(stale, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(0, next.changes);
                assertTrue(find(stale, FlutterWidgetStatePropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
            });
        }
    }

    @Test
    void submenuTwoNativeBooleanConsumersKeepLiteralFocusAndRejectStaleOrCancelledDrafts() throws Exception {
        for (String name : List.of("useRootOverlay", "animated"))
        for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
            var context = new Context(); var field = source("_menuEnabled", StateBinding.Type.BOOL);
            context.fields = CompletableFuture.completedFuture(List.of(field));
            var plain = SubmenuButtonPropertyContractTest.prototype();
            var node = new FlutterWidgetPropertiesNode(Children.LEAF, plain, SubmenuButtonPropertyContractTest.DEF, ignored -> fail("Binding must not dispatch a literal mutation."));
            node.updateEventsContext(context); var row = property(node, name);
            SwingUtilities.invokeAndWait(() -> {
                var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor());
                var original = editor.getValue(); var cancelled = editor.getCustomEditor();
                find(cancelled, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                assertEquals(0, context.changes); assertEquals(original, editor.getValue());
                var panel = row.getPropertyEditor().getCustomEditor();
                find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(new PropertyName(name), context.property);
                assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform), context.binding);
            });
            var literal = new PropertyValue.BooleanValue(false);
            var bound = new WidgetNode(plain.id(), plain.type(), menuStateLiteralProperties(plain, name, literal), plain.slots(), plain.extensions(), Optional.empty(), Map.of(new PropertyName(name), context.binding));
            node.refreshPresentation(bound, SubmenuButtonPropertyContractTest.DEF, ignored -> {}, null, null, FlutterImageAssetChoices.empty()); node.updateEventsContext(context);
            assertSame(row, property(node, name)); assertEquals(FlutterPropertyCellValue.explicit(literal), row.getPropertyEditor().getValue());
            assertTrue(row.getPropertyEditor().isPaintable()); assertNull(row.getPropertyEditor().getTags()); assertTrue(row.supportsDefaultValue());
            assertEquals(plain.slots(), bound.slots()); assertTrue(bound.stateBinding().isEmpty());
            SwingUtilities.invokeAndWait(() -> {
                var stale = row.getPropertyEditor().getCustomEditor(); var next = new Context();
                node.updateEventsContext(next);
                find(stale, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(0, next.changes);
                assertTrue(find(stale, FlutterWidgetStatePropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
            });
        }
    }

    private static Map<PropertyName, PropertyValue> menuStateLiteralProperties(WidgetNode base, String name, PropertyValue value) {
        var properties = new java.util.LinkedHashMap<>(base.properties()); properties.put(new PropertyName(name), value); return properties;
    }

    @Test
    void tooltipVisibilityStateConsumerKeepsRequiredLiteralAndRejectsStaleOrCancelledDrafts() throws Exception {
        for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
            var context = new Context(); var field = source("_tooltipsVisible", StateBinding.Type.BOOL);
            context.fields = CompletableFuture.completedFuture(List.of(field));
            var plain = TooltipVisibilityPropertyContractTest.prototype();
            var node = new FlutterWidgetPropertiesNode(Children.LEAF, plain, TooltipVisibilityPropertyContractTest.DEF, ignored -> fail("Binding must not dispatch a literal mutation."));
            node.updateEventsContext(context); var row = property(node, "visible");
            SwingUtilities.invokeAndWait(() -> {
                var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor());
                var original = editor.getValue(); var cancelled = editor.getCustomEditor();
                find(cancelled, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                assertEquals(0, context.changes); assertEquals(original, editor.getValue());
                var panel = row.getPropertyEditor().getCustomEditor();
                find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(new PropertyName("visible"), context.property);
                assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform), context.binding);
            });
            var literal = new PropertyValue.BooleanValue(false);
            var bound = new WidgetNode(plain.id(), plain.type(), Map.of(new PropertyName("visible"), literal), plain.slots(), plain.extensions(), Optional.empty(), Map.of(new PropertyName("visible"), context.binding));
            node.refreshPresentation(bound, TooltipVisibilityPropertyContractTest.DEF, ignored -> {}, null, null, FlutterImageAssetChoices.empty()); node.updateEventsContext(context);
            assertSame(row, property(node, "visible")); assertEquals(FlutterPropertyCellValue.explicit(literal), row.getPropertyEditor().getValue());
            assertTrue(row.getPropertyEditor().isPaintable()); assertNull(row.getPropertyEditor().getTags()); assertFalse(row.supportsDefaultValue());
            assertEquals(plain.slots(), bound.slots()); assertTrue(bound.stateBinding().isEmpty());
            SwingUtilities.invokeAndWait(() -> {
                var stale = row.getPropertyEditor().getCustomEditor(); var next = new Context();
                node.updateEventsContext(next);
                find(stale, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(0, next.changes);
                assertTrue(find(stale, FlutterWidgetStatePropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
            });
        }
    }

    @Test
    void expansionTileSixStateConsumersPreserveInitialSeedCallbackAndAllSlotChildren() throws Exception {
        for (String name : List.of("showTrailingIcon", "maintainState", "dense", "enableFeedback", "enabled", "internalAddSemanticForOnTap")) {
            for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
                var context = new Context(); var field = source("_tileFlag", StateBinding.Type.BOOL); context.fields = CompletableFuture.completedFuture(List.of(field));
                var node = node("flutter.material.ExpansionTile", context); var row = property(node, name);
                SwingUtilities.invokeAndWait(() -> {
                    var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor()); var before = editor.getValue(); var panel = editor.getCustomEditor();
                    find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                    assertEquals(before, editor.getValue()); assertEquals(0, context.changes);
                    find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                    assertEquals(1, context.changes); assertEquals(new PropertyName(name), context.property);
                    assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform), context.binding);
                });
                var plain = ExpansionTilePropertyContractTest.prototype(); var properties = new java.util.LinkedHashMap<>(plain.properties());
                var literal = new PropertyValue.BooleanValue(false); properties.put(new PropertyName(name), literal);
                properties.put(new PropertyName("initiallyExpanded"), new PropertyValue.BooleanValue(true));
                properties.put(new PropertyName("onExpansionChanged"), new PropertyValue.StringValue("noop"));
                var bound = new WidgetNode(ID, plain.type(), properties, plain.slots(), plain.extensions(), Optional.empty(), Map.of(new PropertyName(name), context.binding));
                node.refreshPresentation(bound, ExpansionTilePropertyContractTest.DEF, ignored -> {}, null, null, FlutterImageAssetChoices.empty()); node.updateEventsContext(context);
                assertSame(row, property(node, name)); assertEquals(FlutterPropertyCellValue.explicit(literal), row.getPropertyEditor().getValue());
                assertTrue(row.getPropertyEditor().isPaintable()); assertNull(row.getPropertyEditor().getTags()); assertTrue(row.getShortDescription().contains("Canvas preview only"));
                assertFalse(property(node, "initiallyExpanded").getPropertyEditor() instanceof FlutterWidgetStatePropertyEditor);
                assertTrue(WidgetStateBindingCatalog.find(bound).isEmpty()); assertTrue(bound.stateBinding().isEmpty()); assertEquals(plain.slots(), bound.slots());
                assertEquals(new PropertyValue.BooleanValue(true), bound.properties().get(new PropertyName("initiallyExpanded")));
                assertEquals(new PropertyValue.StringValue("noop"), bound.properties().get(new PropertyName("onExpansionChanged")));
            }
        }
    }

    @Test
    void radioListTileEightIndependentStateConsumersKeepControlledValueAndCenteredPreviewRows() throws Exception {
        for (String name : List.of("toggleable", "autofocus", "selected", "dense", "enableFeedback", "enabled", "internalAddSemanticForOnTap", "useCupertinoCheckmarkStyle")) {
            for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
                var context = new Context(); var field = source("_tileFlag", StateBinding.Type.BOOL); context.fields = CompletableFuture.completedFuture(List.of(field));
                var node = node("flutter.material.RadioListTile", context); var row = property(node, name);
                SwingUtilities.invokeAndWait(() -> {
                    var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor()); var before = editor.getValue(); var panel = editor.getCustomEditor();
                    find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                    assertEquals(before, editor.getValue()); assertEquals(0, context.changes);
                    find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                    assertEquals(1, context.changes); assertEquals(new PropertyName(name), context.property);
                    assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform), context.binding);
                });
                var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.RadioListTile")).orElseThrow();
                var plain = WidgetNodePrototypeFactory.create(definition, ID); var properties = new java.util.LinkedHashMap<>(plain.properties());
                var literal = new PropertyValue.BooleanValue(false); properties.put(new PropertyName(name), literal);
                var bound = new WidgetNode(ID, plain.type(), properties, plain.slots(), plain.extensions(), Optional.empty(), Map.of(new PropertyName(name), context.binding));
                node.refreshPresentation(bound, definition, ignored -> {}, null, null, FlutterImageAssetChoices.empty()); node.updateEventsContext(context);
                assertSame(row, property(node, name)); assertEquals(FlutterPropertyCellValue.explicit(literal), row.getPropertyEditor().getValue());
                assertTrue(row.getPropertyEditor().isPaintable()); assertNull(row.getPropertyEditor().getTags());
                assertTrue(row.getShortDescription().contains(name.equals("useCupertinoCheckmarkStyle") ? "retained but not generated" : "Canvas preview only"));
                assertEquals(plain.properties().get(new PropertyName("value")), bound.properties().get(new PropertyName("value")));
                assertEquals(plain.properties().get(new PropertyName("onChanged")), bound.properties().get(new PropertyName("onChanged")));
                assertTrue(WidgetStateBindingCatalog.find(bound).isPresent());
            }
        }
    }

    @Test
    void switchListTileSixIndependentStateConsumersKeepControlledValueAndCenteredPreviewRows() throws Exception {
        for (String name : List.of("autofocus", "selected", "dense", "enableFeedback", "internalAddSemanticForOnTap", "applyCupertinoTheme")) {
            for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
                var context = new Context(); var field = source("_tileFlag", StateBinding.Type.BOOL); context.fields = CompletableFuture.completedFuture(List.of(field));
                var node = node("flutter.material.SwitchListTile", context); var row = property(node, name);
                SwingUtilities.invokeAndWait(() -> {
                    var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor()); var before = editor.getValue(); var panel = editor.getCustomEditor();
                    find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                    assertEquals(before, editor.getValue()); assertEquals(0, context.changes);
                    find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                    assertEquals(1, context.changes); assertEquals(new PropertyName(name), context.property);
                    assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform), context.binding);
                });
                var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.SwitchListTile")).orElseThrow();
                var plain = WidgetNodePrototypeFactory.create(definition, ID); var properties = new java.util.LinkedHashMap<>(plain.properties());
                var literal = new PropertyValue.BooleanValue(false); properties.put(new PropertyName(name), literal);
                var bound = new WidgetNode(ID, plain.type(), properties, plain.slots(), plain.extensions(), Optional.empty(), Map.of(new PropertyName(name), context.binding));
                node.refreshPresentation(bound, definition, ignored -> {}, null, null, FlutterImageAssetChoices.empty()); node.updateEventsContext(context);
                assertSame(row, property(node, name)); assertEquals(FlutterPropertyCellValue.explicit(literal), row.getPropertyEditor().getValue());
                assertTrue(row.getPropertyEditor().isPaintable()); assertNull(row.getPropertyEditor().getTags());
                assertTrue(row.getShortDescription().contains(name.equals("applyCupertinoTheme") ? "retained but not generated" : "Canvas preview only"));
                assertEquals(plain.properties().get(new PropertyName("value")), bound.properties().get(new PropertyName("value")));
                assertEquals(plain.properties().get(new PropertyName("onChanged")), bound.properties().get(new PropertyName("onChanged")));
                assertTrue(WidgetStateBindingCatalog.find(bound).isPresent());
            }
        }
    }

    @Test
    void switchListTileControlStateHelpExplainsNativeNullDisableWithoutChangingSwitchGuidance() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var context = new Context();
            var tilePanel = property(node("flutter.material.SwitchListTile", context), "onChanged").getPropertyEditor().getCustomEditor();
            var tileHelp = find(tilePanel, FlutterWidgetEventPropertyEditor.STATE_GUIDANCE_NAME, JTextArea.class).getText();
            for (String expected : List.of("Selected and other widget options are preserved", "no separate Enabled or Tristate",
                    "explicit null On changed disables", "restores the prior No-op, null or reference callback", "keeps your field and handler code", "only if it is still the generated handler"))
                assertTrue(tileHelp.contains(expected), tileHelp);
            assertFalse(tileHelp.contains("Enabled and other widget options are preserved"));
            assertEquals(0, context.changes);
            var switchPanel = property(node("flutter.material.Switch", new Context()), "onChanged").getPropertyEditor().getCustomEditor();
            var switchHelp = find(switchPanel, FlutterWidgetEventPropertyEditor.STATE_GUIDANCE_NAME, JTextArea.class).getText();
            assertTrue(switchHelp.contains("Enabled and other widget options are preserved"));
            assertFalse(switchHelp.contains("no separate Enabled or Tristate"));
        });
    }

    @Test
    void gestureBooleanConfigurationUsesSharedStateConsumersAndKeepsCheckboxPreviewRows() throws Exception {
        for (String name : List.of("excludeFromSemantics", "trackpadScrollCausesScale")) {
            for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
                var context = new Context();
                var field = source("_gestureFlag", StateBinding.Type.BOOL);
                context.fields = CompletableFuture.completedFuture(List.of(field));
                var node = node("flutter.widgets.GestureDetector", context);
                var row = property(node, name);
                assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor());
                SwingUtilities.invokeAndWait(() -> {
                    var editor = row.getPropertyEditor(); var before = editor.getValue();
                    var panel = editor.getCustomEditor();
                    find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                    assertEquals(before, editor.getValue(), "Choosing State does not execute the field or change its Canvas preview.");
                    assertEquals(0, context.changes);
                    find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                    assertEquals(1, context.changes);
                    assertEquals(new PropertyName(name), context.property);
                    assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform), context.binding);
                });
                var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.GestureDetector")).orElseThrow();
                var plain = WidgetNodePrototypeFactory.create(definition, ID);
                var literal = new PropertyValue.BooleanValue(false);
                var bound = new WidgetNode(ID, plain.type(), Map.of(new PropertyName(name), literal), plain.slots(), plain.extensions(),
                        Optional.empty(), Map.of(new PropertyName(name), context.binding));
                node.refreshPresentation(bound, definition, ignored -> {}, null, null, FlutterImageAssetChoices.empty());
                node.updateEventsContext(context);
                assertSame(row, property(node, name));
                var editor = row.getPropertyEditor();
                assertEquals(FlutterPropertyCellValue.explicit(literal), editor.getValue());
                assertTrue(editor.isPaintable()); assertNull(editor.getTags());
                assertTrue(row.getShortDescription().contains("Canvas preview only"));
                assertTrue(WidgetStateBindingCatalog.find(bound).isEmpty());
            }
        }
    }

    @Test
    void mouseRegionOpaqueSharesBooleanStateTransformsWithoutChangingPreviewOrRowIdentity() throws Exception {
        for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT, StatePropertyBinding.Transform.EQUALS)) {
            var context = new Context();
            var field = source("_opaqueFlag", StateBinding.Type.BOOL);
            context.fields = CompletableFuture.completedFuture(List.of(field));
            var node = node("flutter.widgets.MouseRegion", context);
            var row = property(node, "opaque");
            SwingUtilities.invokeAndWait(() -> {
                var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor());
                var before = editor.getValue(); var panel = editor.getCustomEditor();
                find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class).setSelectedItem(transform);
                if (transform == StatePropertyBinding.Transform.EQUALS) find(panel, FlutterWidgetStatePropertyEditor.COMPARISON_NAME, JTextField.class).setText("true");
                assertEquals(before, editor.getValue()); assertEquals(0, context.changes);
                find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
                assertEquals(1, context.changes); assertEquals(new PropertyName("opaque"), context.property);
                assertEquals(new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform,
                        transform == StatePropertyBinding.Transform.EQUALS ? Optional.of(new PropertyValue.BooleanValue(true)) : Optional.empty()), context.binding);
            });
            var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.MouseRegion")).orElseThrow();
            var plain = WidgetNodePrototypeFactory.create(definition, ID);
            var literal = new PropertyValue.BooleanValue(false);
            var bound = new WidgetNode(ID, plain.type(), Map.of(new PropertyName("opaque"), literal), plain.slots(), plain.extensions(),
                    Optional.empty(), Map.of(new PropertyName("opaque"), context.binding));
            node.refreshPresentation(bound, definition, ignored -> {}, null, null, FlutterImageAssetChoices.empty()); node.updateEventsContext(context);
            assertSame(row, property(node, "opaque"));
            var editor = row.getPropertyEditor();
            assertEquals(FlutterPropertyCellValue.explicit(literal), editor.getValue());
            assertTrue(editor.isPaintable()); assertNull(editor.getTags());
            assertTrue(row.getShortDescription().contains("Canvas preview only"));
            assertTrue(WidgetStateBindingCatalog.find(bound).isEmpty());
        }
    }

    @Test
    void genericBindingDiscoveryRejectsStaleEditorAndRetainsRows() throws Exception {
        var context = new Context();
        context.fields = new CompletableFuture<>();
        var node = node("flutter.widgets.Text", context);
        var row = property(node, "data");
        Component[] panel = new Component[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = row.getPropertyEditor().getCustomEditor());
        node.updateEventsContext(new Context());
        context.fields.complete(List.of(source("_text", StateBinding.Type.STRING)));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(find(panel[0], FlutterWidgetStatePropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
            assertFalse(find(panel[0], FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).isEnabled());
            assertEquals(0, find(panel[0], FlutterWidgetStatePropertyEditor.FIELDS_NAME, JComboBox.class).getItemCount());
        });
        assertSame(row, property(node, "data"));
        assertEquals(0, context.changes);
    }

    @Test
    void boundBooleanRowsKeepCheckboxPaintingAndStableEditablePreview() throws Exception {
        var context = new Context();
        var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.ListTile")).orElseThrow();
        var plain = WidgetNodePrototypeFactory.create(definition, ID);
        var dependency = source("_selected", StateBinding.Type.BOOL);
        var bound = new WidgetNode(ID, plain.type(), plain.properties(), plain.slots(), plain.extensions(),
                Optional.empty(), Map.of(new PropertyName("selected"), dependency));
        var changes = new java.util.ArrayList<io.github.vgrytsenko2022.designer.command.DesignerCommand>();
        FlutterWidgetPropertiesNode.PropertyMutationHandler mutation = changes::add;
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, plain, definition, mutation);
        node.updateEventsContext(context);
        var row = property(node, "selected");
        node.refreshPresentation(bound, definition, mutation, null, null, FlutterImageAssetChoices.empty());
        node.updateEventsContext(context);
        assertSame(row, property(node, "selected"));
        assertTrue(row.getDisplayName().contains("preview; _selected"));
        var editor = assertInstanceOf(FlutterWidgetStatePropertyEditor.class, row.getPropertyEditor());
        editor.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)));
        assertTrue(editor.isPaintable());
        assertNull(editor.getTags(), "The global checkbox must not regress to a true/false dropdown.");
        var environment = PropertyEnv.create(row);
        ((ExPropertyEditor) editor).attachEnv(environment);
        var image = new BufferedImage(100, 24, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { editor.paintValue(graphics, new Rectangle(0, 0, 100, 24)); } finally { graphics.dispose(); }
        @SuppressWarnings("unchecked") var typed = (Node.Property<FlutterPropertyCellValue>) row;
        typed.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)));
        assertEquals(1, changes.size());
        assertTrue(row.getShortDescription().contains("Canvas preview only"));
    }

    @Test
    void textControllerInitialTextAndListTileReuseSelectionAreExplicitActions() throws Exception {
        var textContext = new Context();
        var text = node("flutter.material.TextField", textContext);
        SwingUtilities.invokeAndWait(() -> {
            var panel = property(text, "onChanged").getPropertyEditor().getCustomEditor();
            find(panel, FlutterWidgetEventPropertyEditor.STATE_INITIAL_TEXT_NAME, JTextArea.class).setText("Hello\n$world");
            find(panel, FlutterWidgetEventPropertyEditor.STATE_CREATE_NAME, JButton.class).doClick();
            assertEquals("Hello\n$world", textContext.initialText);
            assertEquals(StateBinding.Action.CHANGE, textContext.action);
            assertEquals(1, textContext.changes);
        });
        var selectionContext = new Context();
        var choice = source("_choice", StateBinding.Type.STRING);
        selectionContext.fields = CompletableFuture.completedFuture(List.of(choice));
        var tile = node("flutter.material.ListTile", selectionContext);
        SwingUtilities.invokeAndWait(() -> {
            var panel = property(tile, "onTap").getPropertyEditor().getCustomEditor();
            find(panel, FlutterWidgetEventPropertyEditor.STATE_MODE_NAME, JComboBox.class).setSelectedIndex(1);
            find(panel, FlutterWidgetEventPropertyEditor.STATE_ACTION_NAME, JComboBox.class).setSelectedItem(StateBinding.Action.SELECT);
            assertTrue(find(panel, FlutterWidgetEventPropertyEditor.STATE_TYPE_NAME, JLabel.class).getText().startsWith("String —"),
                    "A reused String field must not be labelled bool.");
            find(panel, FlutterWidgetEventPropertyEditor.STATE_SELECTION_NAME, JTextField.class).setText("first row");
            assertEquals("_choice", find(panel, FlutterWidgetEventPropertyEditor.STATE_FIELD_NAME, JTextField.class).getText());
            assertFalse(find(panel, FlutterWidgetEventPropertyEditor.STATE_FIELD_NAME, JTextField.class).isEnabled());
            assertEquals(0, selectionContext.changes);
            find(panel, FlutterWidgetEventPropertyEditor.STATE_CREATE_NAME, JButton.class).doClick();
            assertEquals(Optional.of(choice), selectionContext.reused);
            assertEquals(Optional.of(new PropertyValue.StringValue("first row")), selectionContext.selected);
            assertEquals(StateBinding.Action.SELECT, selectionContext.action);
        });
    }

    @Test
    void comparisonParserUsesClosedTypedLiteralsAndNeverDartSource() {
        assertEquals(new PropertyValue.StringValue("setState(() {});"),
                FlutterWidgetStatePropertyEditor.parseComparison(StateBinding.Type.STRING, "setState(() {});"));
        assertEquals(new PropertyValue.NullValue(), FlutterWidgetStatePropertyEditor.parseComparison(StateBinding.Type.NULLABLE_BOOL, "null"));
        assertEquals(new PropertyValue.BooleanValue(false), FlutterWidgetStatePropertyEditor.parseComparison(StateBinding.Type.BOOL, "false"));
        assertThrows(IllegalArgumentException.class, () -> FlutterWidgetStatePropertyEditor.parseComparison(StateBinding.Type.DOUBLE, "print('x')"));
        assertThrows(IllegalArgumentException.class, () -> FlutterWidgetStatePropertyEditor.parseComparison(StateBinding.Type.BOOL, "1"));
    }

    @Test
    void boundedNumericPropertyOffersOnlyCompatibleClampBinding() throws Exception {
        var context = new Context();
        context.fields = CompletableFuture.completedFuture(List.of(
                source("_count", StateBinding.Type.INT), source("_opacity", StateBinding.Type.DOUBLE)));
        var node = node("flutter.widgets.Opacity", context);
        SwingUtilities.invokeAndWait(() -> {
            var panel = property(node, "opacity").getPropertyEditor().getCustomEditor();
            var fields = find(panel, FlutterWidgetStatePropertyEditor.FIELDS_NAME, JComboBox.class);
            assertEquals(1, fields.getItemCount(), "The int field is not offered for a double-only bounded property.");
            var transforms = find(panel, FlutterWidgetStatePropertyEditor.TRANSFORM_NAME, JComboBox.class);
            assertEquals(1, transforms.getItemCount());
            assertEquals(StatePropertyBinding.Transform.CLAMP, transforms.getSelectedItem());
            find(panel, FlutterWidgetStatePropertyEditor.APPLY_NAME, JButton.class).doClick();
            assertEquals(new PropertyName("opacity"), context.property);
            assertEquals(StatePropertyBinding.Transform.CLAMP, context.binding.transform());
        });
    }

    @Test
    void listTileNewSelectionFieldHasAnExplicitLiteralType() throws Exception {
        var context = new Context();
        var node = node("flutter.material.ListTile", context);
        SwingUtilities.invokeAndWait(() -> {
            var panel = property(node, "onTap").getPropertyEditor().getCustomEditor();
            find(panel, FlutterWidgetEventPropertyEditor.STATE_ACTION_NAME, JComboBox.class).setSelectedItem(StateBinding.Action.SELECT);
            find(panel, FlutterWidgetEventPropertyEditor.STATE_SELECTION_TYPE_NAME, JComboBox.class).setSelectedItem(StateBinding.Type.STRING);
            assertTrue(find(panel, FlutterWidgetEventPropertyEditor.STATE_TYPE_NAME, JLabel.class).getText().startsWith("String? —"),
                    "The new Select field's displayed type matches nullable generated source.");
            assertTrue(find(panel, FlutterWidgetEventPropertyEditor.STATE_GUIDANCE_NAME, JTextArea.class).getText().contains("otherwise null"));
            find(panel, FlutterWidgetEventPropertyEditor.STATE_SELECTION_NAME, JTextField.class).setText("first row");
            find(panel, FlutterWidgetEventPropertyEditor.STATE_CREATE_NAME, JButton.class).doClick();
            assertEquals(Optional.empty(), context.reused);
            assertEquals(Optional.of(new PropertyValue.StringValue("first row")), context.selected);
        });
    }

    @Test
    void sliderReuseExplainsRuntimeBoundsWithoutClaimingImplicitClamp() throws Exception {
        var context = new Context();
        var node = node("flutter.material.Slider", context);
        SwingUtilities.invokeAndWait(() -> {
            var panel = property(node, "onChanged").getPropertyEditor().getCustomEditor();
            String guidance = find(panel, FlutterWidgetEventPropertyEditor.STATE_GUIDANCE_NAME, JTextArea.class).getText();
            assertTrue(guidance.contains("Minimum/Maximum"));
            assertTrue(guidance.contains("do not implicitly clamp"));
        });
    }

    @Test
    void controlFieldNavigationAndRenameAreSeparateFromCallbackNamesAndOrdinaryPropertyEdits() throws Exception {
        var context = new Context();
        var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.Switch")).orElseThrow();
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var binding = WidgetStateBindingCatalog.createBinding(prototype, "_selected", "_onChanged");
        var widget = new WidgetNode(ID, prototype.type(), prototype.properties(), prototype.slots(), prototype.extensions(), Optional.of(binding));
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition, ignored -> fail("State field management must not use a literal property edit."));
        node.updateEventsContext(context);
        SwingUtilities.invokeAndWait(() -> {
            var navigationEditor = property(node, "onChanged").getPropertyEditor();
            Object original = navigationEditor.getValue();
            var panel = navigationEditor.getCustomEditor();
            find(panel, FlutterWidgetEventPropertyEditor.STATE_NAVIGATE_NAME, JButton.class).doClick();
            assertEquals("_selected", context.navigatedField);
            assertEquals(1, context.fieldNavigations);
            assertEquals(0, context.changes, "Go to Field is read-only.");
            assertEquals(original, navigationEditor.getValue());
            panel = property(node, "onChanged").getPropertyEditor().getCustomEditor();
            assertTrue(find(panel, FlutterWidgetEventPropertyEditor.STATE_GUIDANCE_NAME, JTextArea.class).getText().contains("throughout this form"));
            find(panel, FlutterWidgetEventPropertyEditor.STATE_RENAME_FIELD_NAME, JTextField.class).setText("_renamedSelected");
            find(panel, FlutterWidgetEventPropertyEditor.STATE_RENAME_NAME, JButton.class).doClick();
            assertEquals("_selected", context.renamedField);
            assertEquals("_renamedSelected", context.newFieldName);
            assertEquals(1, context.changes);
            assertEquals(1, context.fieldNavigations, "Rename must not navigate automatically.");
            assertEquals("_onChanged", widget.stateBinding().orElseThrow().handlerName(), "The callback name is independent of the State field name.");
        });
    }

    @Test
    void dependentPropertyFieldManagementUsesBoundFieldNotDropdownAndNavigationDiscardsOnlyLocalLiteralDraft() throws Exception {
        var context = new Context();
        var bound = source("_amount", StateBinding.Type.INT);
        var unrelated = source("_otherText", StateBinding.Type.STRING);
        context.fields = CompletableFuture.completedFuture(List.of(bound, unrelated));
        var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var dependency = new StatePropertyBinding(bound.fieldName(), bound.type(), bound.referenceType(), StatePropertyBinding.Transform.TO_STRING, Optional.empty());
        var widget = new WidgetNode(ID, prototype.type(), prototype.properties(), prototype.slots(), prototype.extensions(), Optional.empty(), Map.of(new PropertyName("data"), dependency));
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition, ignored -> fail("Field management must not submit an ordinary property draft."));
        node.updateEventsContext(context);
        SwingUtilities.invokeAndWait(() -> {
            var editor = property(node, "data").getPropertyEditor();
            Object original = editor.getValue();
            var panel = editor.getCustomEditor();
            find(panel, FlutterWidgetStatePropertyEditor.FIELDS_NAME, JComboBox.class).setSelectedItem(unrelated);
            editor.setAsText("Uncommitted literal draft");
            find(panel, FlutterWidgetStatePropertyEditor.NAVIGATE_NAME, JButton.class).doClick();
            assertEquals("_amount", context.navigatedField);
            assertEquals(0, context.changes);
            assertEquals(original, editor.getValue(), "A read-only navigation action cannot implicitly apply a local preview draft on close.");
            panel = property(node, "data").getPropertyEditor().getCustomEditor();
            find(panel, FlutterWidgetStatePropertyEditor.FIELDS_NAME, JComboBox.class).setSelectedItem(unrelated);
            assertTrue(find(panel, FlutterWidgetStatePropertyEditor.BOUND_FIELD_NAME, JLabel.class).getText().startsWith("_amount"));
            find(panel, FlutterWidgetStatePropertyEditor.RENAME_FIELD_NAME, JTextField.class).setText("_renamedAmount");
            find(panel, FlutterWidgetStatePropertyEditor.RENAME_NAME, JButton.class).doClick();
            assertEquals("_amount", context.renamedField);
            assertEquals("_renamedAmount", context.newFieldName);
            assertEquals(1, context.changes);
            assertEquals(1, context.fieldNavigations);
        });
    }

    @Test
    void unboundAndStaleEditorsCannotManageUnrelatedStateFields() throws Exception {
        var context = new Context();
        context.fields = CompletableFuture.completedFuture(List.of(source("_available", StateBinding.Type.STRING)));
        var unbound = node("flutter.widgets.Text", context);
        SwingUtilities.invokeAndWait(() -> {
            var panel = property(unbound, "data").getPropertyEditor().getCustomEditor();
            assertFalse(find(panel, FlutterWidgetStatePropertyEditor.RENAME_NAME, JButton.class).isEnabled());
            assertFalse(find(panel, FlutterWidgetStatePropertyEditor.NAVIGATE_NAME, JButton.class).isEnabled());
            assertFalse(find(panel, FlutterWidgetStatePropertyEditor.RENAME_FIELD_NAME, JTextField.class).isEnabled());
        });
        var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.Switch")).orElseThrow();
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var widget = new WidgetNode(ID, prototype.type(), prototype.properties(), prototype.slots(), prototype.extensions(),
                Optional.of(WidgetStateBindingCatalog.createBinding(prototype, "_selected", "_changed")));
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition, ignored -> fail("Unexpected mutation"));
        node.updateEventsContext(context);
        Component[] oldPanel = new Component[1];
        SwingUtilities.invokeAndWait(() -> oldPanel[0] = property(node, "onChanged").getPropertyEditor().getCustomEditor());
        node.updateEventsContext(new Context());
        SwingUtilities.invokeAndWait(() -> {
            find(oldPanel[0], FlutterWidgetEventPropertyEditor.STATE_RENAME_FIELD_NAME, JTextField.class).setText("_renamed");
            find(oldPanel[0], FlutterWidgetEventPropertyEditor.STATE_RENAME_NAME, JButton.class).doClick();
            assertTrue(find(oldPanel[0], FlutterWidgetEventPropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
            assertFalse(find(oldPanel[0], FlutterWidgetEventPropertyEditor.STATE_NAVIGATE_NAME, JButton.class).isEnabled());
        });
        assertEquals(0, context.changes);
        assertEquals(0, context.fieldNavigations);
    }

    @Test
    void radioTileStateHelpExplainsLegacyGroupValueAndStaleEditorsCannotSubmitBindings() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var context = new Context(); var node = node("flutter.material.RadioListTile", context);
            var row = property(node, "onChanged"); var panel = row.getPropertyEditor().getCustomEditor();
            String help = find(panel, FlutterWidgetEventPropertyEditor.STATE_GUIDANCE_NAME, JTextArea.class).getText();
            for (String expected : List.of("legacy Group value State binding", "RadioGroup", "coordinated sibling selection", "nullable group selection", "prior omitted, No-op, null or reference callback", "title/subtitle/secondary")) assertTrue(help.contains(expected), help);
            assertFalse(help.contains("no separate Enabled or Tristate")); assertEquals(0, context.changes);
            node.updateEventsContext(new Context()); assertSame(row, property(node, "onChanged"));
            find(panel, FlutterWidgetEventPropertyEditor.STATE_CREATE_NAME, JButton.class).doClick();
            assertEquals(0, context.changes); assertTrue(find(panel, FlutterWidgetEventPropertyEditor.STATUS_NAME, JTextArea.class).getText().contains("document changed"));
        });
    }

    private static FlutterWidgetPropertiesNode node(String type, Context context) {
        var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF,
                WidgetNodePrototypeFactory.create(definition, ID), definition, ignored -> fail("Unexpected ordinary property mutation"));
        node.updateEventsContext(context);
        return node;
    }
    private static Node.Property<?> property(FlutterWidgetPropertiesNode node, String name) {
        return Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties()))
                .filter(row -> row.getName().equals(name)).findFirst().orElseThrow();
    }
    private static <T extends Component> T find(Component component, String name, Class<T> type) {
        if (name.equals(component.getName()) && type.isInstance(component)) return type.cast(component);
        if (component instanceof Container container) for (var child : container.getComponents()) {
            try { return find(child, name, type); } catch (IllegalArgumentException ignored) { }
        }
        throw new IllegalArgumentException("Missing component " + name);
    }
    private static final class Context implements FlutterWidgetEventsContext {
        CompletableFuture<List<StatePropertyBinding>> fields = CompletableFuture.completedFuture(List.of());
        int changes;
        PropertyName property;
        StatePropertyBinding binding;
        String initialText;
        StateBinding.Action action;
        Optional<PropertyValue> selected;
        Optional<StatePropertyBinding> reused;
        String renamedField;
        String newFieldName;
        String navigatedField;
        int fieldNavigations;
        @Override public CompletionStage<Void> renameStateField(String field, String newName) {
            renamedField = field; newFieldName = newName; return changed();
        }
        @Override public CompletionStage<Void> navigateToStateField(String field) {
            navigatedField = field; fieldNavigations++; return CompletableFuture.completedFuture(null);
        }
        @Override public String stateBindingUnavailableReason() { return ""; }
        @Override public String propertyStateBindingUnavailableReason(PropertyName value) { return ""; }
        @Override public CompletionStage<List<StatePropertyBinding>> discoverStateFields() { return fields; }
        @Override public CompletionStage<Void> bindPropertyToState(PropertyName value, StatePropertyBinding state) {
            property = value; binding = state; return changed();
        }
        @Override public CompletionStage<Void> removePropertyStateBinding(PropertyName value) { property = value; return changed(); }
        @Override public CompletionStage<Void> createStateBinding(String field, String handler, StateBinding.Action value,
                Optional<PropertyValue> selection, String text, Optional<StatePropertyBinding> reuse) {
            action = value; selected = selection; initialText = text; reused = reuse; return changed();
        }
        @Override public CompletionStage<List<Handler>> discover(PropertyName event) { return CompletableFuture.completedFuture(List.of()); }
        @Override public CompletionStage<Void> create(PropertyName event, String handler) { return changed(); }
        @Override public CompletionStage<Void> bind(PropertyName event, Handler handler) { return changed(); }
        @Override public CompletionStage<Void> editBinding(PropertyName event, Optional<PropertyValue> value) { return changed(); }
        @Override public CompletionStage<Void> navigate(PropertyName event) { return CompletableFuture.completedFuture(null); }
        @Override public CompletionStage<Void> rename(PropertyName event, String name) { return changed(); }
        @Override public CompletionStage<Void> disconnect(PropertyName event) { return changed(); }
        private CompletionStage<Void> changed() { changes++; return CompletableFuture.completedFuture(null); }
    }
}
