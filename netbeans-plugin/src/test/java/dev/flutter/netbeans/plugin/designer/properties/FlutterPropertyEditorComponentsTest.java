package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.DartNumericLiterals;
import dev.flutter.netbeans.designer.catalog.MaterialThemeTokenCatalog;
import dev.flutter.netbeans.designer.catalog.MaterialIconRegistry;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyPanel;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class FlutterPropertyEditorComponentsTest {

    @Test
    void circularProgressOptionalConstraintsRejectInvalidDraftAndCommitExpansionOrUnsetOnce() throws Exception {
        var initial = FlutterPropertyCellValue.explicit(CircularProgressIndicatorPropertyContractTest.value("constraints"));
        var editor = binding(property("flutter.material.CircularProgressIndicator", "constraints")).createEditor(); editor.setValue(initial);
        var environment = PropertyEnv.create(descriptor("Constraints", "Optional circular progress constraints."));
        ((ExPropertyEditor) editor).attachEnv(environment); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
        onEdt(() -> {
            var panel = editor.getCustomEditor(); assertAccessibleNameContains(panel, "box constraints");
            var minimum = findNamed(panel, JTextField.class, FlutterContainerPropertyEditorComponents.CONSTRAINTS_MIN_WIDTH_NAME);
            minimum.setText("1000"); assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID); assertEquals(initial, editor.getValue()); assertEquals(0, commits.get());
            minimum.setText("20");
            findNamed(panel, JCheckBox.class, FlutterContainerPropertyEditorComponents.CONSTRAINTS_EXPANDING_HEIGHT_NAME).doClick();
            assertEquals(initial, editor.getValue(), "Cancel keeps all four original bounds");
            environment.setState(PropertyEnv.STATE_VALID); assertEquals(1, commits.get());
            var accepted = assertInstanceOf(PropertyValue.BoxConstraintsValue.class, ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow());
            assertTrue(accepted.expandingHeight()); assertEquals(0, new BigDecimal("20").compareTo(accepted.minWidth().finiteValue().orElseThrow()));
            var acceptedCell = editor.getValue();
            var reopenedEnvironment = PropertyEnv.create(descriptor("Constraints", "Reopened circular constraints."));
            ((ExPropertyEditor) editor).attachEnv(reopenedEnvironment); var reopened = editor.getCustomEditor();
            assertTrue(findNamed(reopened, JCheckBox.class, FlutterContainerPropertyEditorComponents.CONSTRAINTS_EXPANDING_HEIGHT_NAME).isSelected());
            var omit = findByText(reopened, JCheckBox.class, "Use inherited/default value (omit argument)");
            assertNotNull(omit); omit.doClick(); assertEquals(acceptedCell, editor.getValue());
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue()); assertEquals(2, commits.get());
            return null;
        });
    }

    @Test
    void circularProgressOptionalPaddingRetainsAllFourModesAndCancelledDrafts() throws Exception {
        for (String modeLabel : List.of("All sides", "Symmetric", "Physical (left/right)", "Directional (start/end)")) {
            var editor = binding(property("flutter.material.CircularProgressIndicator", "padding")).createEditor();
            var initial = FlutterPropertyCellValue.explicit(CircularProgressIndicatorPropertyContractTest.value("padding")); editor.setValue(initial);
            var environment = PropertyEnv.create(descriptor("Padding", "Optional circular padding.")); ((ExPropertyEditor) editor).attachEnv(environment);
            var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
            onEdt(() -> {
                var panel = editor.getCustomEditor(); selectLabel(findNamed(panel, JComboBox.class, "flutter.edgeInsets.mode"), modeLabel);
                String fieldName = switch (modeLabel) {
                    case "All sides" -> FlutterPropertyEditorComponents.EDGE_ALL_NAME;
                    case "Symmetric" -> "flutter.edgeInsets.horizontal";
                    case "Physical (left/right)" -> "flutter.edgeInsets.left";
                    default -> "flutter.edgeInsets.directional.start";
                };
                var field = findNamed(panel, JTextField.class, fieldName); field.setText("-1");
                assertEquals(PropertyEnv.STATE_INVALID, environment.getState()); assertEquals(initial, editor.getValue());
                field.setText("9.5"); assertEquals(initial, editor.getValue(), "Cancel never publishes the repaired draft");
                environment.setState(PropertyEnv.STATE_VALID); assertEquals(1, commits.get());
                var accepted = editor.getValue(); var reopenedEnvironment = PropertyEnv.create(descriptor("Padding", "Reopened padding."));
                ((ExPropertyEditor) editor).attachEnv(reopenedEnvironment); var reopened = editor.getCustomEditor();
                assertEquals(modeLabel, String.valueOf(findNamed(reopened, JComboBox.class, "flutter.edgeInsets.mode").getSelectedItem()));
                var omit = findByText(reopened, JCheckBox.class, "Use inherited/default value (omit argument)");
                assertNotNull(omit); omit.doClick(); assertEquals(accepted, editor.getValue());
                reopenedEnvironment.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue()); assertEquals(2, commits.get());
                return null;
            });
        }
    }

    @Test
    void bothProgressColorAnimationUnionCommitsEachBranchOnceAndReopensWithoutLosingMode() throws Exception {
        for (String type : List.of("flutter.material.LinearProgressIndicator", "flutter.material.CircularProgressIndicator")) {
            var initial = FlutterPropertyCellValue.explicit(new PropertyValue.ColorValue(0xff102030L));
            for (String branch : List.of("literal", "theme", "null", "project", "unset")) {
                var editor = binding(property(type, "valueColor")).createEditor(); editor.setValue(initial);
                var environment = PropertyEnv.create(descriptor("Value color", "Stopped or project animation."));
                ((ExPropertyEditor) editor).attachEnv(environment); var commits = new AtomicInteger();
                editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
                onEdt(() -> {
                    var panel = editor.getCustomEditor(); assertAccessibleNameContains(panel, "progress indicator", "valuecolor");
                    var mode = findNamed(panel, JComboBox.class, FlutterColorAnimationEditorComponent.MODE_NAME);
                    assertEquals(4, mode.getItemCount());
                    FlutterPropertyCellValue expected;
                    String selected;
                    switch (branch) {
                        case "literal", "theme" -> {
                            selected = FlutterColorAnimationEditorComponent.STOPPED_COLOR; mode.setSelectedItem(selected);
                            var colorMode = findNamed(panel, JComboBox.class, FlutterComplexPropertyEditorComponents.THEME_COLOR_MODE_NAME);
                            if (branch.equals("literal")) {
                                colorMode.setSelectedItem("Literal ARGB");
                                findNamed(panel, JTextField.class, FlutterComplexPropertyEditorComponents.THEME_COLOR_ARGB_NAME).setText("0x80445566");
                                expected = FlutterPropertyCellValue.explicit(new PropertyValue.ColorValue(0x80445566L));
                            } else {
                                colorMode.setSelectedItem("Theme role");
                                findNamed(panel, JComboBox.class, FlutterComplexPropertyEditorComponents.THEME_COLOR_ROLE_NAME).setSelectedItem("Primary");
                                expected = FlutterPropertyCellValue.explicit(new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")));
                            }
                        }
                        case "null" -> { selected = FlutterColorAnimationEditorComponent.STOPPED_NULL; mode.setSelectedItem(selected); expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()); }
                        case "project" -> {
                            selected = FlutterColorAnimationEditorComponent.PROJECT; mode.setSelectedItem(selected);
                            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
                            var root = findNamed(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
                            root.setText("_animatedColor"); expected = FlutterPropertyCellValue.explicit(LinearProgressIndicatorPropertyContractTest.reference("_animatedColor"));
                            assertTrue(findNamed(panel, JTextArea.class, FlutterColorAnimationEditorComponent.NOTE_NAME).getText().contains("not executed"));
                        }
                        default -> { selected = FlutterColorAnimationEditorComponent.UNSET; mode.setSelectedItem(selected); expected = FlutterPropertyCellValue.unset(); }
                    }
                    assertEquals(initial, editor.getValue(), "all nested drafts, including valid ones, remain unpublished"); assertEquals(0, commits.get());
                    environment.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue()); assertEquals(1, commits.get());
                    environment.setState(PropertyEnv.STATE_NEEDS_VALIDATION); environment.setState(PropertyEnv.STATE_VALID); assertEquals(1, commits.get());
                    var reopenedEnv = PropertyEnv.create(descriptor("Value color", "Reopened valueColor.")); ((ExPropertyEditor) editor).attachEnv(reopenedEnv);
                    var reopened = editor.getCustomEditor(); assertEquals(selected, findNamed(reopened, JComboBox.class, FlutterColorAnimationEditorComponent.MODE_NAME).getSelectedItem());
                    findNamed(reopened, JComboBox.class, FlutterColorAnimationEditorComponent.MODE_NAME).setSelectedItem(FlutterColorAnimationEditorComponent.STOPPED_NULL);
                    assertEquals(expected, editor.getValue(), "Cancel after switching the reopened mode preserves the original typed value");
                    return null;
                });
            }
        }
    }

    @Test
    void bothProgressAnimationNestedReferenceRejectsExpressionsAndSupportsImportedFactories() throws Exception {
        for (String type : List.of("flutter.material.LinearProgressIndicator", "flutter.material.CircularProgressIndicator")) {
            var editor = binding(property(type, "valueColor")).createEditor(); editor.setValue(FlutterPropertyCellValue.unset());
            var environment = PropertyEnv.create(descriptor("Value color", "Typed animation reference.")); ((ExPropertyEditor) editor).attachEnv(environment);
            onEdt(() -> {
                var panel = editor.getCustomEditor(); var mode = findNamed(panel, JComboBox.class, FlutterColorAnimationEditorComponent.MODE_NAME);
                mode.setSelectedItem(FlutterColorAnimationEditorComponent.PROJECT);
                var root = findNamed(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
                root.setText("Colors.red + Colors.blue"); assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
                assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                var scope = findNamed(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME); scope.setSelectedIndex(1);
                findNamed(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME).setText("package:progress_ui/animations.dart");
                root.setText("Animations"); findNamed(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME).setText("progressColor");
                findNamed(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
                assertEquals(FlutterPropertyCellValue.unset(), editor.getValue()); environment.setState(PropertyEnv.STATE_VALID);
                var reference = assertInstanceOf(PropertyValue.DartObjectReferenceValue.class, ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow());
                assertEquals(Optional.of("package:progress_ui/animations.dart"), reference.libraryUri()); assertEquals("Animations", reference.rootSymbol());
                assertEquals(Optional.of("progressColor"), reference.member()); assertEquals(PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, reference.access());
                assertEquals(Optional.of(false), reference.constant()); return null;
            });
        }
    }

    @Test
    void circleAvatarOptionalImagesCommitResizeAndUnsetWithoutLeakingCancelledDrafts() throws Exception {
        for (String name : List.of("backgroundImage", "foregroundImage")) {
            for (var policy : PropertyValue.ImageProviderValue.ResizePolicy.values()) {
                PropertyEditor editor = binding(property("flutter.material.CircleAvatar", name)).createEditor();
                editor.setValue(FlutterPropertyCellValue.unset());
                FeatureDescriptor descriptor = descriptor(name, "Optional CircleAvatar image provider.");
                descriptor.setValue(FlutterImageAssetChoices.FEATURE_ATTRIBUTE, new FlutterImageAssetChoices(
                        List.of(new FlutterImageAssetChoices.Choice(Optional.of("ui_kit"), "assets/avatar.png", "Package avatar")), Optional.empty()));
                PropertyEnv environment = PropertyEnv.create(descriptor); ((ExPropertyEditor) editor).attachEnv(environment);
                onEdt(() -> {
                    String prefix = "flutter.circleAvatar." + name;
                    Component panel = editor.getCustomEditor();
                    JCheckBox unset = findNamed(panel, JCheckBox.class, prefix + ".unset");
                    assertTrue(unset.isSelected());
                    assertAccessibleNameContains(panel, "circleavatar", name.toLowerCase(java.util.Locale.ROOT));
                    var asset = findNamed(panel, JComboBox.class, prefix + ".asset");
                    assertFalse(asset.isEnabled()); unset.doClick(); assertTrue(asset.isEnabled());
                    findNamed(panel, JComboBox.class, prefix + ".provider").setSelectedItem(PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET);
                    findNamed(panel, JTextField.class, prefix + ".exactScale").setText("2");
                    findNamed(panel, JCheckBox.class, prefix + ".resize.enabled").doClick();
                    findNamed(panel, JTextField.class, prefix + ".resize.width").setText("-1");
                    assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
                    assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                    findNamed(panel, JTextField.class, prefix + ".resize.width").setText("48");
                    findNamed(panel, JTextField.class, prefix + ".resize.height").setText("64");
                    findNamed(panel, JComboBox.class, prefix + ".resize.policy").setSelectedItem(policy);
                    findNamed(panel, JCheckBox.class, prefix + ".resize.allowUpscaling").doClick();
                    assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                    environment.setState(PropertyEnv.STATE_VALID);
                    var provider = assertInstanceOf(PropertyValue.ImageProviderValue.class,
                            ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow());
                    assertEquals(Optional.of("ui_kit"), provider.packageName());
                    assertEquals(Optional.of(BigDecimal.valueOf(2)), provider.exactScale());
                    assertEquals(Optional.of(48), provider.resize().orElseThrow().width());
                    assertEquals(Optional.of(64), provider.resize().orElseThrow().height());
                    assertEquals(policy, provider.resize().orElseThrow().policy());
                    assertTrue(provider.resize().orElseThrow().allowUpscaling());
                    var reopenedEnvironment = PropertyEnv.create(descriptor); ((ExPropertyEditor) editor).attachEnv(reopenedEnvironment);
                    var reopened = editor.getCustomEditor();
                    findNamed(reopened, JCheckBox.class, prefix + ".unset").doClick();
                    assertEquals(provider, ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow());
                    reopenedEnvironment.setState(PropertyEnv.STATE_VALID);
                    assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                    return null;
                });
            }
        }
    }

    @Test
    void circleAvatarUnsetImagesNeedNoAssetsAndKeepStoredMissingOrUnresolvedProviders() throws Exception {
        for (String name : List.of("backgroundImage", "foregroundImage")) {
            for (var initial : List.of(FlutterPropertyCellValue.unset(),
                    FlutterPropertyCellValue.explicit(PropertyValue.ImageProviderValue.asset("assets/removed.png")),
                    FlutterPropertyCellValue.explicit(PropertyValue.ImageProviderValue.unresolved()))) {
                PropertyEditor editor = binding(property("flutter.material.CircleAvatar", name)).createEditor(); editor.setValue(initial);
                PropertyEnv environment = PropertyEnv.create(descriptor(name, "Optional image without assets."));
                ((ExPropertyEditor) editor).attachEnv(environment);
                onEdt(() -> {
                    Component panel = editor.getCustomEditor();
                    var unset = findNamed(panel, JCheckBox.class, "flutter.circleAvatar." + name + ".unset");
                    assertEquals(initial.explicitValue().isEmpty(), unset.isSelected());
                    assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
                    if (initial.explicitValue().isEmpty()) {
                        unset.doClick(); assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
                        assertEquals(initial, editor.getValue()); unset.doClick();
                    }
                    environment.setState(PropertyEnv.STATE_VALID); assertEquals(initial, editor.getValue());
                    return null;
                });
            }
        }
    }

    @Test
    void circleAvatarInfinityRemainsAnInlineNumericControlWithUnsetSupport() throws Exception {
        for (String name : List.of("radius", "minRadius", "maxRadius")) {
            var binding = binding(property("flutter.material.CircleAvatar", name));
            var editor = binding.createEditor(); editor.setValue(FlutterPropertyCellValue.unset());
            var inplace = FlutterPropertyEditorComponents.inplaceFactory(binding).orElseThrow().getInplaceEditor();
            onEdt(() -> {
                inplace.connect(editor, PropertyEnv.create(descriptor(name, "Radius or Infinity.")));
                var input = assertInstanceOf(JTextField.class, inplace.getComponent());
                assertEquals(FlutterPropertyEditorComponents.NUMERIC_COMPONENT_NAME, input.getName());
                input.setText("Infinity"); editor.setAsText((String) inplace.getValue());
                assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", "infinity")), editor.getValue());
                input.setText("<not set>"); editor.setAsText((String) inplace.getValue());
                assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                return null;
            });
        }
    }

    @Test
    void badgeAllRichPropertyRoutesExposeTransactionalCustomEditorsAndNoUnexpectedWidgets() throws Exception {
        var widget = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.Badge")).orElseThrow();
        for (String name : List.of("backgroundColor", "textColor", "padding", "alignment", "offset", "textStyleColor",
                "textStyleBackgroundColor", "textStyleForeground", "textStyleBackground", "textStyleShadows",
                "textStyleFontFeatures", "textStyleFontVariations", "textStyleDecorationColor", "textStyleFontFamilyFallback")) {
            var binding = FlutterTypedPropertyEditors.binding(widget.property(new PropertyName(name)).orElseThrow(),
                    dev.flutter.netbeans.designer.catalog.BadgeWidgetPropertySchema.textStyleBinding(new PropertyName(name))).orElseThrow();
            var editor = binding.createEditor(); assertTrue(editor.supportsCustomEditor(), name);
            var initial = FlutterPropertyCellValue.explicit(BadgePropertyContractTest.value(name));
            editor.setValue(initial); var env = PropertyEnv.create(descriptor(name, "Badge optional typed field."));
            ((ExPropertyEditor) editor).attachEnv(env); AtomicInteger commits = new AtomicInteger();
            editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
            onEdt(() -> {
                var component = editor.getCustomEditor(); assertNotNull(component, name);
                assertEquals(initial, editor.getValue(), "Opening/cancelling " + name + " must not mutate its value");
                assertEquals(0, commits.get()); return null;
            });
            editor.setValue(FlutterPropertyCellValue.unset()); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
        }
    }

    @Test
    void badgeTextStyleShadowEditorPreservesStableIdsAndSupportsTransactionalEditsEmptyAndUnset() throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(property("flutter.material.Badge", "textStyleShadows"));
        PropertyValue.ShadowListValue initialValue = new PropertyValue.ShadowListValue(
                List.of(new PropertyValue.ShadowListValue.Shadow(
                        StableId.parse("f0bf1d8a-c780-4654-9770-91d53600e770"),
                        new ColorSource.Theme(new ThemeToken(
                                "material.colorScheme.shadow")),
                        BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3))));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(initialValue);
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Shadows", "Ordered typed shadows."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTable table = findNamed(panel, JTable.class,
                    FlutterComplexPropertyEditorComponents.SHADOW_TABLE_NAME);
            assertNotNull(table);
            assertTrue(table.getAccessibleContext().getAccessibleDescription()
                    .contains("Color accepts"));
            assertNotNull(findButton(panel, "Add"));
            assertNotNull(findButton(panel, "Remove"));
            assertNotNull(findButton(panel, "Up"));
            assertNotNull(findButton(panel, "Down"));

            table.getModel().setValueAt("1E-400", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            table.getModel().setValueAt("1", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertTrue(table.editCellAt(0, 3));
            JTextField activeCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            activeCell.setText("-1");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(initial, editor.getValue());
            assertTrue(table.editCellAt(0, 3));
            JTextField validActiveCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            validActiveCell.setText("4.5");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_VALID, environment.getState(),
                    "a valid active cell must commit with one OK validation");
            PropertyValue.ShadowListValue accepted = assertInstanceOf(
                    PropertyValue.ShadowListValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(new BigDecimal("4.5"), accepted.items().getFirst().blurRadius());
            assertEquals(initialValue.items().getFirst().id(), accepted.items().getFirst().id());
            assertInstanceOf(ColorSource.Theme.class,
                    accepted.items().getFirst().color());
            return null;
        });
        editor.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.ShadowListValue(List.of())));
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.ShadowListValue(List.of())), editor.getValue());
        editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT);
        assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
    }

    @Test
    void iconThemeShadowEditorPreservesStableIdsAndSupportsTransactionalEditsEmptyAndUnset() throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(property("flutter.widgets.IconTheme", "shadows"));
        PropertyValue.ShadowListValue initialValue = new PropertyValue.ShadowListValue(
                List.of(new PropertyValue.ShadowListValue.Shadow(
                        StableId.parse("f0bf1d8a-c780-4654-9770-91d53600e770"),
                        new ColorSource.Theme(new ThemeToken(
                                "material.colorScheme.shadow")),
                        BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3))));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(initialValue);
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Shadows", "Ordered typed shadows."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTable table = findNamed(panel, JTable.class,
                    FlutterComplexPropertyEditorComponents.SHADOW_TABLE_NAME);
            assertNotNull(table);
            assertTrue(table.getAccessibleContext().getAccessibleDescription()
                    .contains("Color accepts"));
            assertNotNull(findButton(panel, "Add"));
            assertNotNull(findButton(panel, "Remove"));
            assertNotNull(findButton(panel, "Up"));
            assertNotNull(findButton(panel, "Down"));

            table.getModel().setValueAt("1E-400", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            table.getModel().setValueAt("1", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertTrue(table.editCellAt(0, 3));
            JTextField activeCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            activeCell.setText("-1");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(initial, editor.getValue());
            assertTrue(table.editCellAt(0, 3));
            JTextField validActiveCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            validActiveCell.setText("4.5");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_VALID, environment.getState(),
                    "a valid active cell must commit with one OK validation");
            PropertyValue.ShadowListValue accepted = assertInstanceOf(
                    PropertyValue.ShadowListValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(new BigDecimal("4.5"), accepted.items().getFirst().blurRadius());
            assertEquals(initialValue.items().getFirst().id(), accepted.items().getFirst().id());
            assertInstanceOf(ColorSource.Theme.class,
                    accepted.items().getFirst().color());
            return null;
        });
        editor.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.ShadowListValue(List.of())));
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.ShadowListValue(List.of())), editor.getValue());
        editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT);
        assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
    }

    @Test
    void tickerModeRequiredEnabledCheckboxTogglesButNeverAcceptsUnset() throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(property("flutter.widgets.TickerMode", "enabled"));
        assertFalse(binding.optional());
        assertEquals(Optional.of(new PropertyValue.BooleanValue(true)), binding.definition().creationDefault());
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)));
        assertThrows(IllegalArgumentException.class, () -> editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT));
        assertThrows(IllegalArgumentException.class, () -> editor.setValue(FlutterPropertyCellValue.unset()));
        onEdt(() -> {
            InplaceEditor inplace = FlutterPropertyEditorComponents.inplaceFactory(binding).orElseThrow().getInplaceEditor();
            inplace.connect(editor, PropertyEnv.create(descriptor("enabled", "Required enabled")));
            JCheckBox checkbox = assertInstanceOf(JCheckBox.class, inplace.getComponent());
            assertTrue(checkbox.isSelected());
            assertEquals(SwingConstants.CENTER, checkbox.getHorizontalAlignment());
            assertFalse(checkbox.getAccessibleContext().getAccessibleDescription().contains("Restore Default"));
            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)), inplace.getValue());
            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)), inplace.getValue());
            assertThrows(IllegalArgumentException.class, () -> inplace.setValue(FlutterPropertyCellValue.unset()));
            inplace.clear();
            return null;
        });
    }

    @Test
    void containerStructuredPropertiesUseTransactionalTypedEditors()
            throws Exception {
        FlutterTypedPropertyEditors.Binding alignmentBinding = binding(
                property("flutter.widgets.Container", "alignment"));
        FlutterTypedPropertyEditors.Binding constraintsBinding = binding(
                property("flutter.widgets.Container", "constraints"));
        FlutterTypedPropertyEditors.Binding matrixBinding = binding(
                property("flutter.widgets.Container", "transform"));
        FlutterTypedPropertyEditors.Binding decorationBinding = binding(
                property("flutter.widgets.Container", "decoration"));

        assertEquals(FlutterTypedPropertyEditors.EditorKind.ALIGNMENT_GEOMETRY,
                alignmentBinding.editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.BOX_CONSTRAINTS,
                constraintsBinding.editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.MATRIX4,
                matrixBinding.editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.BOX_DECORATION,
                decorationBinding.editorKind());
        assertTrue(alignmentBinding.createEditor().supportsCustomEditor());
        assertTrue(constraintsBinding.createEditor().supportsCustomEditor());
        assertTrue(matrixBinding.createEditor().supportsCustomEditor());
        assertTrue(decorationBinding.createEditor().supportsCustomEditor());

        PropertyEditor editor = alignmentBinding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Alignment", "Container child alignment."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use inherited/default value (omit argument)");
            JTextField horizontal = findNamed(panel, JTextField.class,
                    FlutterContainerPropertyEditorComponents.ALIGNMENT_HORIZONTAL_NAME);
            JTextField vertical = findNamed(panel, JTextField.class,
                    FlutterContainerPropertyEditorComponents.ALIGNMENT_VERTICAL_NAME);
            JComboBox<?> basis = findNamed(panel, JComboBox.class,
                    FlutterContainerPropertyEditorComponents.ALIGNMENT_BASIS_NAME);
            assertNotNull(useDefault);
            assertNotNull(horizontal);
            assertNotNull(vertical);
            assertNotNull(basis);
            assertTrue(useDefault.isSelected());
            assertEquals("0", horizontal.getText(),
                    "unset presentation text must never leak into the editor");

            useDefault.doClick();
            basis.setSelectedItem(
                    PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL);
            horizontal.setText("0.25");
            vertical.setText("-1");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(),
                    "structured changes remain a local draft until dialog OK");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                    new PropertyValue.AlignmentGeometryValue(
                            PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                            new BigDecimal("0.25"), BigDecimal.ONE.negate())),
                    editor.getValue());
            return null;
        });

        assertStructuredEditorHasNamedComponent(
                constraintsBinding,
                FlutterContainerPropertyEditorComponents.CONSTRAINTS_MIN_WIDTH_NAME,
                JTextField.class);
        assertStructuredEditorHasNamedComponent(
                matrixBinding,
                FlutterContainerPropertyEditorComponents.MATRIX_TABLE_NAME,
                Component.class);
        assertStructuredEditorHasNamedComponent(
                decorationBinding,
                FlutterContainerPropertyEditorComponents.DECORATION_TABS_NAME,
                javax.swing.JTabbedPane.class);
    }

    @Test
    void containerStructuredEditorsCanResetAnInvalidDraftAndFlushActiveTableOnOk()
            throws Exception {
        FlutterTypedPropertyEditors.Binding constraintsBinding = binding(
                property("flutter.widgets.Container", "constraints"));
        PropertyEditor constraintsEditor = constraintsBinding.createEditor();
        constraintsEditor.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.BoxConstraintsValue(
                        BigDecimal.ZERO, java.util.Optional.of(BigDecimal.valueOf(100)),
                        BigDecimal.ZERO, java.util.Optional.of(BigDecimal.valueOf(100)))));
        PropertyEnv constraintsEnvironment = PropertyEnv.create(descriptor(
                "Constraints", "Container constraints."));
        ((ExPropertyEditor) constraintsEditor).attachEnv(constraintsEnvironment);
        onEdt(() -> {
            Component panel = constraintsEditor.getCustomEditor();
            JTextField minimum = findNamed(panel, JTextField.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_MIN_WIDTH_NAME);
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use inherited/default value (omit argument)");
            assertNotNull(minimum);
            assertNotNull(useDefault);
            minimum.setText("-1");
            assertEquals(PropertyEnv.STATE_INVALID, constraintsEnvironment.getState());
            useDefault.doClick();
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    constraintsEnvironment.getState(),
                    "reset must not parse an obsolete invalid structured draft");
            constraintsEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), constraintsEditor.getValue());
            return null;
        });

        FlutterTypedPropertyEditors.Binding decorationBinding = binding(
                property("flutter.widgets.Container", "decoration"));
        PropertyValue.BoxDecorationValue.BorderSide side =
                new PropertyValue.BoxDecorationValue.BorderSide(
                        new ColorSource.Literal(0xFF000000L), BigDecimal.ONE,
                        PropertyValue.BoxDecorationValue.BorderStyle.SOLID,
                        BigDecimal.ONE.negate());
        PropertyValue.BoxDecorationValue initialDecoration =
                new PropertyValue.BoxDecorationValue(
                        java.util.Optional.empty(),
                        java.util.Optional.of(
                                new PropertyValue.BoxDecorationValue.PhysicalBorder(
                                        side, side, side, side)),
                        java.util.Optional.empty(), List.of(),
                        java.util.Optional.empty(), java.util.Optional.empty(),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
        PropertyEditor decorationEditor = decorationBinding.createEditor();
        decorationEditor.setValue(FlutterPropertyCellValue.explicit(initialDecoration));
        PropertyEnv decorationEnvironment = PropertyEnv.create(descriptor(
                "Decoration", "Container BoxDecoration."));
        ((ExPropertyEditor) decorationEditor).attachEnv(decorationEnvironment);
        onEdt(() -> {
            Component panel = decorationEditor.getCustomEditor();
            JTable border = findNamed(panel, JTable.class,
                    FlutterContainerPropertyEditorComponents.DECORATION_BORDER_TABLE_NAME);
            assertNotNull(border);
            assertTrue(border.editCellAt(0, 2));
            JTextField active = assertInstanceOf(
                    JTextField.class, border.getEditorComponent());
            active.setText("2.5");
            assertTrue(border.isEditing());
            assertEquals("2.5", border.getValueAt(0, 2),
                    "the local structured draft must track the active cell immediately");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    decorationEnvironment.getState(),
                    ((javax.swing.JComponent) panel).getToolTipText());

            decorationEnvironment.setState(PropertyEnv.STATE_VALID);
            assertFalse(border.isEditing(), "OK must finish the active table cell");
            assertEquals("2.5", border.getValueAt(0, 2));
            PropertyValue.BoxDecorationValue committed = assertInstanceOf(
                    PropertyValue.BoxDecorationValue.class,
                    ((FlutterPropertyCellValue) decorationEditor.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.PhysicalBorder committedBorder =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.PhysicalBorder.class,
                            committed.border().orElseThrow());
            assertEquals(new BigDecimal("2.5"), committedBorder.top().width());
            return null;
        });
    }

    @Test
    void containerDecorationBorderBasisRemapsAsymmetricSidesSemantically()
            throws Exception {
        PropertyValue.BoxDecorationValue.BorderSide top = borderSide("1");
        PropertyValue.BoxDecorationValue.BorderSide right = borderSide("2");
        PropertyValue.BoxDecorationValue.BorderSide bottom = borderSide("3");
        PropertyValue.BoxDecorationValue.BorderSide left = borderSide("4");
        PropertyValue.BoxDecorationValue initial = decoration(
                java.util.Optional.of(new PropertyValue.BoxDecorationValue.PhysicalBorder(
                        top, right, bottom, left)),
                java.util.Optional.empty());
        PropertyEditor editor = decorationEditor(initial);
        PropertyEnv environment = attachedEnvironment(editor, "Decoration");

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTable border = findNamed(panel, JTable.class,
                    FlutterContainerPropertyEditorComponents.DECORATION_BORDER_TABLE_NAME);
            JComboBox<?> basis = findComboWithItems(
                    panel, "Physical: left / right", "Directional: start / end");
            assertNotNull(border);
            assertNotNull(basis);
            assertTableColumn(border, 0, "Top", "Right", "Bottom", "Left");
            assertTableColumn(border, 2, "1", "2", "3", "4");

            basis.setSelectedIndex(1);
            assertTableColumn(border, 0, "Top", "Start", "End", "Bottom");
            assertTableColumn(border, 2, "1", "4", "2", "3");

            basis.setSelectedIndex(0);
            assertTableColumn(border, 0, "Top", "Right", "Bottom", "Left");
            assertTableColumn(border, 2, "1", "2", "3", "4");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.BoxDecorationValue committed = committedDecoration(editor);
            PropertyValue.BoxDecorationValue.PhysicalBorder committedBorder =
                    assertInstanceOf(PropertyValue.BoxDecorationValue.PhysicalBorder.class,
                            committed.border().orElseThrow());
            assertEquals(top, committedBorder.top());
            assertEquals(right, committedBorder.right());
            assertEquals(bottom, committedBorder.bottom());
            assertEquals(left, committedBorder.left());
            return null;
        });
    }

    @Test
    void containerDecorationDisablingRadialFocalClearsItsRadiusOnCommit()
            throws Exception {
        PropertyValue.AlignmentGeometryValue center = alignment("0", "0");
        PropertyValue.AlignmentGeometryValue focal = alignment("0.25", "-0.5");
        PropertyValue.BoxDecorationValue.RadialGradient radial =
                new PropertyValue.BoxDecorationValue.RadialGradient(
                        center, new BigDecimal("0.75"), java.util.Optional.of(focal),
                        new BigDecimal("0.2"), gradientStops(),
                        PropertyValue.BoxDecorationValue.TileMode.CLAMP,
                        java.util.Optional.empty());
        PropertyEditor editor = decorationEditor(decoration(
                java.util.Optional.empty(), java.util.Optional.of(radial)));
        PropertyEnv environment = attachedEnvironment(editor, "Decoration");

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JCheckBox focalEnabled = findByText(
                    panel, JCheckBox.class, "Use second alignment");
            JTextField focalRadius = findAccessibleName(
                    panel, JTextField.class, "Radial focal radius");
            assertNotNull(focalEnabled);
            assertNotNull(focalRadius);
            assertTrue(focalEnabled.isSelected());
            assertEquals("0.2", focalRadius.getText());

            focalEnabled.doClick();
            assertFalse(focalEnabled.isSelected());
            assertFalse(focalRadius.isEnabled());
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.BoxDecorationValue.RadialGradient committed = assertInstanceOf(
                    PropertyValue.BoxDecorationValue.RadialGradient.class,
                    committedDecoration(editor).gradient().orElseThrow());
            assertTrue(committed.focal().isEmpty());
            assertEquals(BigDecimal.ZERO, committed.focalRadius());
            return null;
        });
    }

    @Test
    void containerDecorationEnablesTopOnlyBorderFromPaintSafeDefaults()
            throws Exception {
        PropertyEditor editor = decorationEditor(decoration(
                java.util.Optional.empty(), java.util.Optional.empty()));
        PropertyEnv environment = attachedEnvironment(editor, "Decoration");

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JCheckBox enabled = findByText(panel, JCheckBox.class, "Enable border");
            JTable border = findNamed(panel, JTable.class,
                    FlutterContainerPropertyEditorComponents.DECORATION_BORDER_TABLE_NAME);
            assertNotNull(enabled);
            assertNotNull(border);
            assertFalse(enabled.isSelected());

            enabled.doClick();
            assertTableColumn(border, 4, "-1", "-1", "-1", "-1");
            border.setValueAt("1", 0, 2);
            border.setValueAt("solid", 0, 3);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.BoxDecorationValue.PhysicalBorder committed = assertInstanceOf(
                    PropertyValue.BoxDecorationValue.PhysicalBorder.class,
                    committedDecoration(editor).border().orElseThrow());
            assertEquals(PropertyValue.BoxDecorationValue.BorderStyle.SOLID,
                    committed.top().style());
            assertEquals(BigDecimal.ONE, committed.top().width());
            for (PropertyValue.BoxDecorationValue.BorderSide side : List.of(
                    committed.right(), committed.bottom(), committed.left())) {
                assertEquals(PropertyValue.BoxDecorationValue.BorderStyle.NONE, side.style());
                assertEquals(BigDecimal.ONE.negate(), side.strokeAlign());
            }
            return null;
        });
    }

    @Test
    void containerStructuredEditorRestoresAccessibleDescriptionAfterInvalidDraft()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Container", "transform"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(identityMatrix()));
        PropertyEnv environment = attachedEnvironment(editor, "Transform");

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTextField first = findNamed(panel, JTextField.class,
                    "flutter.container.matrix.r0c0");
            assertNotNull(first);
            String description = panel.getAccessibleContext().getAccessibleDescription();
            assertEquals(
                    "Edits a 4 by 4 Matrix4; values are persisted in Flutter column-major order.",
                    description);

            first.setText("not-a-number");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .startsWith("Invalid value."));

            first.setText("1");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(description,
                    panel.getAccessibleContext().getAccessibleDescription());
            return null;
        });
    }

    @Test
    void containerMatrixConvenienceNumbersRejectInvalidOrLossyDartDoubles() {
        for (String text : List.of("NaN", "not-a-number")) {
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> FlutterContainerPropertyEditorComponents.parseConvenienceNumber(
                            text, "X"));
            assertTrue(failure.getMessage().contains("finite decimal number"),
                    failure.getMessage());
        }
        for (String text : List.of(
                "1e9999", "1e-9999", "0.1000000000000000000000001")) {
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> FlutterContainerPropertyEditorComponents.parseConvenienceNumber(
                            text, "X"));
            assertTrue(failure.getMessage().contains(
                    "exactly representable as a finite Dart double"),
                    failure.getMessage());
        }
        assertEquals(new BigDecimal("12.5"),
                FlutterContainerPropertyEditorComponents.parseConvenienceNumber(
                        " 12.5 ", "X"));
    }

    @Test
    void containerDecorationBasisLabelsAreAssociatedAndAccessible()
            throws Exception {
        PropertyEditor editor = decorationEditor(decoration(
                java.util.Optional.empty(), java.util.Optional.empty()));
        attachedEnvironment(editor, "Decoration");

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTabbedPane tabs = findNamed(panel, JTabbedPane.class,
                    FlutterContainerPropertyEditorComponents.DECORATION_TABS_NAME);
            assertNotNull(tabs);

            Component borderPanel = tabs.getComponentAt(tabs.indexOfTab("Border"));
            JComboBox<?> borderBasis = findComboWithItems(
                    borderPanel, "Physical: left / right", "Directional: start / end");
            JLabel borderLabel = findLabel(borderPanel, "Basis:");
            assertNotNull(borderBasis);
            assertNotNull(borderLabel);
            assertSame(borderBasis, borderLabel.getLabelFor());
            assertAccessibleNameContains(borderBasis, "border", "basis");

            Component radiusPanel = tabs.getComponentAt(tabs.indexOfTab("Radius"));
            JComboBox<?> radiusBasis = findComboWithItems(
                    radiusPanel, "Physical corners", "Directional corners");
            JLabel radiusLabel = findLabel(radiusPanel, "Basis:");
            assertNotNull(radiusBasis);
            assertNotNull(radiusLabel);
            assertSame(radiusBasis, radiusLabel.getLabelFor());
            assertAccessibleNameContains(radiusBasis, "radius", "basis");
            return null;
        });
    }

    @Test
    void constrainedBoxRequiredConstraintsEditorSupportsIndependentExpandingAxes()
            throws Exception {
        assertRequiredConstrainedBoxExpansion(true);
        assertRequiredConstrainedBoxExpansion(false);
    }

    @Test
    void constrainedBoxConstraintsEditorRejectsInvertedRangeWithoutPartialCommit()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.ConstrainedBox", "constraints"));
        PropertyValue.BoxConstraintsValue initial = new PropertyValue.BoxConstraintsValue(
                BigDecimal.ZERO, java.util.Optional.of(BigDecimal.valueOf(100)),
                BigDecimal.ZERO, java.util.Optional.empty());
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initial));
        AtomicInteger commits = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Constraints", "Required ConstrainedBox constraints."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            JComponent panel = assertInstanceOf(JComponent.class, editor.getCustomEditor());
            JTextField minWidth = findNamed(panel, JTextField.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_MIN_WIDTH_NAME);
            JTextField maxWidth = findNamed(panel, JTextField.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_MAX_WIDTH_NAME);
            assertNotNull(minWidth);
            assertNotNull(maxWidth);

            minWidth.setText("100");
            maxWidth.setText("50");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", panel.getClientProperty("JComponent.outline"));
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue());
            assertEquals(0, commits.get());

            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "an inverted local draft must never partially replace the value");
            assertEquals(0, commits.get());

            maxWidth.setText("150");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertNull(panel.getClientProperty("JComponent.outline"));
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue());
            environment.setState(PropertyEnv.STATE_VALID);

            assertEquals(FlutterPropertyCellValue.explicit(
                    new PropertyValue.BoxConstraintsValue(
                            BigDecimal.valueOf(100),
                            java.util.Optional.of(BigDecimal.valueOf(150)),
                            BigDecimal.ZERO, java.util.Optional.empty())), editor.getValue());
            assertEquals(1, commits.get(), "the repaired four-bound draft commits atomically");
            return null;
        });
    }

    @Test
    void decorationImageUsesDeclaredTypedChoicesAndCommitsEveryDependentDomain()
            throws Exception {
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(
                        new FlutterImageAssetChoices.Choice(
                                java.util.Optional.empty(),
                                "assets/background.png",
                                "App: assets/background.png"),
                        new FlutterImageAssetChoices.Choice(
                                java.util.Optional.of("ui_kit"),
                                "assets/panel.webp",
                                "Package ui_kit: assets/panel.webp")),
                java.util.Optional.empty());
        PropertyEditor editor = decorationEditor(decoration(
                java.util.Optional.empty(), java.util.Optional.empty()));
        FeatureDescriptor descriptor = descriptor(
                "Decoration", "Typed Container DecorationImage editor.");
        descriptor.setValue(
                FlutterImageAssetChoices.FEATURE_ATTRIBUTE, choices);
        PropertyEnv environment = PropertyEnv.create(descriptor);
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTabbedPane tabs = findNamed(panel, JTabbedPane.class,
                    FlutterContainerPropertyEditorComponents.DECORATION_TABS_NAME);
            assertNotNull(tabs);
            assertTrue(tabs.getAccessibleContext().getAccessibleDescription()
                    .contains("image"));
            Component imagePanel = tabs.getComponentAt(tabs.indexOfTab("Image"));
            JCheckBox enabled = findNamed(
                    imagePanel,
                    JCheckBox.class,
                    FlutterContainerPropertyEditorComponents
                            .DECORATION_IMAGE_ENABLED_NAME);
            JComboBox<?> asset = findNamed(
                    imagePanel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents
                            .DECORATION_IMAGE_ASSET_NAME);
            JComboBox<?> provider = findNamed(
                    imagePanel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents
                            .DECORATION_IMAGE_PROVIDER_NAME);
            JComboBox<?> colorFilter = findNamed(
                    imagePanel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents
                            .DECORATION_IMAGE_FILTER_NAME);
            assertNotNull(enabled);
            assertNotNull(asset);
            assertNotNull(provider);
            assertNotNull(colorFilter);
            assertEquals(2, asset.getItemCount());
            assertFalse(asset.isEditable(),
                    "arbitrary asset paths must not be accepted");
            assertEquals("ExactAssetImage", assertInstanceOf(
                    JLabel.class,
                    renderedComboCell(
                            provider,
                            PropertyValue.ImageProviderValue.ProviderKind
                                    .EXACT_ASSET)).getText());
            assertFalse(enabled.isSelected());
            assertFalse(asset.isEnabled());

            enabled.doClick();
            asset.setSelectedIndex(1);
            provider.setSelectedItem(
                    PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET);
            JTextField exactScale = findAccessibleName(
                    imagePanel, JTextField.class, "ExactAssetImage scale");
            assertNotNull(exactScale);
            assertTrue(exactScale.isEnabled());
            exactScale.setText("2");

            JCheckBox resize = findByText(
                    imagePanel, JCheckBox.class, "Wrap with ResizeImage");
            JTextField resizeWidth = findAccessibleName(
                    imagePanel, JTextField.class, "ResizeImage cache width");
            JComboBox<?> resizePolicy = findAccessibleName(
                    imagePanel, JComboBox.class, "ResizeImage policy");
            assertNotNull(resize);
            assertNotNull(resizeWidth);
            assertNotNull(resizePolicy);
            resize.doClick();
            resizeWidth.setText("320");
            resizePolicy.setSelectedItem(
                    PropertyValue.ImageProviderValue.ResizePolicy.FIT);
            findByText(imagePanel, JCheckBox.class, "Allow upscaling").doClick();

            JCheckBox onError = findByText(
                    imagePanel,
                    JCheckBox.class,
                    "Use typed image-error handler");
            JTextField handler = findAccessibleName(
                    imagePanel,
                    JTextField.class,
                    "Typed image error handler identifier");
            onError.doClick();
            handler.setText("_handleDecorationImageError");

            colorFilter.setSelectedItem("Saturation");
            findAccessibleName(
                    imagePanel,
                    JTextField.class,
                    "ColorFilter saturation amount").setText("0.75");
            JComboBox<?> fit = findAccessibleName(
                    imagePanel, JComboBox.class, "DecorationImage BoxFit");
            JCheckBox centerSlice = findByText(
                    imagePanel,
                    JCheckBox.class,
                    "Use center slice (nine-patch)");
            fit.setSelectedItem("cover");
            centerSlice.doClick();
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState(),
                    "centerSlice must fail closed for BoxFit.cover");
            fit.setSelectedItem("contain");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    environment.getState());

            findAccessibleName(
                    imagePanel,
                    JTextField.class,
                    "DecorationImage paint scale").setText("1.25");
            findAccessibleName(
                    imagePanel,
                    JTextField.class,
                    "DecorationImage opacity").setText("0.6");
            findAccessibleName(
                    imagePanel,
                    JComboBox.class,
                    "DecorationImage repeat").setSelectedItem(
                            PropertyValue.DecorationImageValue.ImageRepeat.REPEAT_X);
            findAccessibleName(
                    imagePanel,
                    JComboBox.class,
                    "DecorationImage filter quality").setSelectedItem(
                            PropertyValue.PaintValue.FilterQuality.HIGH);
            findByText(
                    imagePanel,
                    JCheckBox.class,
                    "Mirror for text direction").doClick();
            findByText(
                    imagePanel,
                    JCheckBox.class,
                    "Invert colors").doClick();
            findByText(
                    imagePanel,
                    JCheckBox.class,
                    "Anti-alias image edges").doClick();

            environment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.DecorationImageValue committed =
                    committedDecoration(editor).image().orElseThrow();
            assertEquals(
                    PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET,
                    committed.image().providerKind());
            assertEquals("assets/panel.webp", committed.image().assetName());
            assertEquals(java.util.Optional.of("ui_kit"),
                    committed.image().packageName());
            assertEquals(java.util.Optional.of(new BigDecimal("2")),
                    committed.image().exactScale());
            var committedResize = committed.image().resize().orElseThrow();
            assertEquals(java.util.Optional.of(320), committedResize.width());
            assertEquals(java.util.Optional.empty(), committedResize.height());
            assertEquals(PropertyValue.ImageProviderValue.ResizePolicy.FIT,
                    committedResize.policy());
            assertTrue(committedResize.allowUpscaling());
            assertEquals("_handleDecorationImageError",
                    committed.onError().orElseThrow().handler());
            assertEquals(new BigDecimal("0.75"), assertInstanceOf(
                    PropertyValue.DecorationImageValue.Saturation.class,
                    committed.colorFilter().orElseThrow()).value());
            assertEquals(java.util.Optional.of(
                    PropertyValue.DecorationImageValue.BoxFit.CONTAIN),
                    committed.fit());
            assertTrue(committed.centerSlice().isPresent());
            assertEquals(PropertyValue.DecorationImageValue.ImageRepeat.REPEAT_X,
                    committed.repeat());
            assertTrue(committed.matchTextDirection());
            assertEquals(new BigDecimal("1.25"), committed.scale());
            assertEquals(new BigDecimal("0.6"), committed.opacity());
            assertEquals(PropertyValue.PaintValue.FilterQuality.HIGH,
                    committed.filterQuality());
            assertTrue(committed.invertColors());
            assertTrue(committed.isAntiAlias());
            return null;
        });
    }

    @Test
    void nullableImageIconEditorCommitsTypedProviderAndNoneTransactionally() throws Exception {
        for (var policy : PropertyValue.ImageProviderValue.ResizePolicy.values()) {
            PropertyEditor editor = binding(property("flutter.widgets.ImageIcon", "image")).createEditor();
            var initial = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
            editor.setValue(initial);
            FeatureDescriptor descriptor = descriptor("ImageIcon", "Required nullable ImageIcon.image.");
            descriptor.setValue(FlutterImageAssetChoices.FEATURE_ATTRIBUTE, new FlutterImageAssetChoices(
                    List.of(new FlutterImageAssetChoices.Choice(java.util.Optional.of("ui_kit"), "assets/a.png", "Package image")),
                    java.util.Optional.empty()));
            PropertyEnv environment = PropertyEnv.create(descriptor);
            ((ExPropertyEditor) editor).attachEnv(environment);
            onEdt(() -> {
                Component panel = editor.getCustomEditor();
                JCheckBox none = findNamed(panel, JCheckBox.class, "flutter.imageIcon.imageProvider.none");
                assertTrue(none.isSelected());
                JComboBox<?> asset = findNamed(panel, JComboBox.class, "flutter.imageIcon.imageProvider.asset");
                assertFalse(asset.isEnabled());
                none.doClick();
                assertTrue(asset.isEnabled());
                findNamed(panel, JComboBox.class, "flutter.imageIcon.imageProvider.provider")
                        .setSelectedItem(PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET);
                findNamed(panel, JTextField.class, "flutter.imageIcon.imageProvider.exactScale").setText("2");
                findNamed(panel, JCheckBox.class, "flutter.imageIcon.imageProvider.resize.enabled").doClick();
                findNamed(panel, JTextField.class, "flutter.imageIcon.imageProvider.resize.width").setText("-1");
                assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
                assertEquals(initial, editor.getValue(), "an invalid or cancelled draft must not commit");
                findNamed(panel, JTextField.class, "flutter.imageIcon.imageProvider.resize.width").setText("48");
                findNamed(panel, JTextField.class, "flutter.imageIcon.imageProvider.resize.height").setText("64");
                findNamed(panel, JComboBox.class, "flutter.imageIcon.imageProvider.resize.policy").setSelectedItem(policy);
                findNamed(panel, JCheckBox.class, "flutter.imageIcon.imageProvider.resize.allowUpscaling").doClick();
                assertEquals(initial, editor.getValue(), "valid drafts also wait for dialog commit");
                environment.setState(PropertyEnv.STATE_VALID);
                var provider = assertInstanceOf(PropertyValue.ImageProviderValue.class,
                        ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow());
                assertEquals(java.util.Optional.of("ui_kit"), provider.packageName());
                assertEquals(java.util.Optional.of(new BigDecimal("2")), provider.exactScale());
                assertEquals(java.util.Optional.of(48), provider.resize().orElseThrow().width());
                assertEquals(java.util.Optional.of(64), provider.resize().orElseThrow().height());
                assertEquals(policy, provider.resize().orElseThrow().policy());
                assertTrue(provider.resize().orElseThrow().allowUpscaling());
                // One dialog has one commit lease; reopen before editing the committed provider.
                PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor);
                ((ExPropertyEditor) editor).attachEnv(reopenedEnvironment);
                Component reopened = editor.getCustomEditor();
                JCheckBox reopenedNone = findNamed(reopened, JCheckBox.class,
                        "flutter.imageIcon.imageProvider.none");
                assertFalse(reopenedNone.isSelected());
                reopenedNone.doClick();
                assertEquals(provider, ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow());
                reopenedEnvironment.setState(PropertyEnv.STATE_VALID);
                assertEquals(initial, editor.getValue());
                return null;
            });
        }
    }

    @Test
    void nullableImageIconPreservesNullStoredMissingAndImportedUnresolvedWithoutAssets() throws Exception {
        for (PropertyValue initial : List.of(new PropertyValue.NullValue(),
                PropertyValue.ImageProviderValue.asset("assets/removed.png"),
                PropertyValue.ImageProviderValue.unresolved())) {
            PropertyEditor editor = binding(property("flutter.widgets.ImageIcon", "image")).createEditor();
            var initialCell = FlutterPropertyCellValue.explicit(initial);
            editor.setValue(initialCell);
            PropertyEnv environment = PropertyEnv.create(descriptor("ImageIcon", "Required nullable provider."));
            ((ExPropertyEditor) editor).attachEnv(environment);
            onEdt(() -> {
                Component panel = editor.getCustomEditor();
                JCheckBox none = findNamed(panel, JCheckBox.class, "flutter.imageIcon.imageProvider.none");
                assertEquals(initial instanceof PropertyValue.NullValue, none.isSelected());
                assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
                assertEquals(initialCell, editor.getValue());
                if (initial instanceof PropertyValue.NullValue) {
                    none.doClick();
                    assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
                    assertEquals(initialCell, editor.getValue());
                    none.doClick();
                } else {
                    none.doClick();
                    assertEquals(initialCell, editor.getValue(), "None draft cancellation is non-mutating");
                    none.doClick();
                }
                environment.setState(PropertyEnv.STATE_VALID);
                assertEquals(initialCell, editor.getValue(), "retains stored provider settings after toggling None");
                return null;
            });
        }
    }

    @Test
    void directImageProviderEditorReusesDeclaredAssetProviderControls()
            throws Exception {
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(
                        new FlutterImageAssetChoices.Choice(
                                java.util.Optional.empty(),
                                "assets/a.png",
                                "App: assets/a.png"),
                        new FlutterImageAssetChoices.Choice(
                                java.util.Optional.of("ui_kit"),
                                "assets/panel.webp",
                                "Package ui_kit: assets/panel.webp")),
                java.util.Optional.empty());
        PropertyEditor editor = binding(
                property("flutter.widgets.Image", "image")).createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(
                PropertyValue.ImageProviderValue.asset("assets/a.png")));
        FeatureDescriptor descriptor = descriptor(
                "Image", "Required Image.image provider editor.");
        descriptor.setValue(FlutterImageAssetChoices.FEATURE_ATTRIBUTE, choices);
        PropertyEnv environment = PropertyEnv.create(descriptor);
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("required Image.image"));
            JComboBox<?> asset = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX + ".asset");
            JComboBox<?> provider = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX + ".provider");
            assertNotNull(asset);
            assertNotNull(provider);
            assertFalse(asset.isEditable());
            assertEquals(2, asset.getItemCount());

            asset.setSelectedIndex(1);
            provider.setSelectedItem(
                    PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET);
            findAccessibleName(panel, JTextField.class,
                    "ExactAssetImage scale").setText("2");
            findByText(panel, JCheckBox.class,
                    "Wrap with ResizeImage").doClick();
            findAccessibleName(panel, JTextField.class,
                    "ResizeImage cache width").setText("320");
            environment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.ImageProviderValue committed = assertInstanceOf(
                    PropertyValue.ImageProviderValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals("assets/panel.webp", committed.assetName());
            assertEquals(java.util.Optional.of("ui_kit"), committed.packageName());
            assertEquals(java.util.Optional.of(new BigDecimal("2")),
                    committed.exactScale());
            assertEquals(java.util.Optional.of(320),
                    committed.resize().orElseThrow().width());
            return null;
        });
    }

    @Test
    void reopenedUnresolvedImageEditorOffersOnlyRealAssetsAndCommitsSelection()
            throws Exception {
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(
                        new FlutterImageAssetChoices.Choice(
                                java.util.Optional.empty(),
                                "assets/a.png",
                                "App: assets/a.png"),
                        new FlutterImageAssetChoices.Choice(
                                java.util.Optional.of("ui_kit"),
                                "assets/panel.webp",
                                "Package ui_kit: assets/panel.webp")),
                java.util.Optional.empty());
        PropertyEditor editor = binding(
                property("flutter.widgets.Image", "image")).createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(
                PropertyValue.ImageProviderValue.unresolved()));
        FeatureDescriptor descriptor = descriptor(
                "Image", "Required Image.image provider editor.");
        descriptor.setValue(FlutterImageAssetChoices.FEATURE_ATTRIBUTE, choices);
        PropertyEnv environment = PropertyEnv.create(descriptor);
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> asset = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX + ".asset");
            JComboBox<?> provider = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX + ".provider");
            JLabel status = findNamed(
                    panel,
                    JLabel.class,
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX + ".assetStatus");

            assertEquals(2, asset.getItemCount(),
                    "the reserved unresolved provider must not become an asset choice");
            assertNull(asset.getSelectedItem());
            assertEquals(
                    FlutterImageProviderEditorComponent.UNRESOLVED_SELECTION_TEXT,
                    renderedNullSelectionText(asset));
            for (int index = 0; index < asset.getItemCount(); index++) {
                FlutterImageAssetChoices.Choice choice = assertInstanceOf(
                        FlutterImageAssetChoices.Choice.class,
                        asset.getItemAt(index));
                assertFalse(PropertyValue.ImageProviderValue
                        .asset(choice.assetName()).isUnresolved());
            }
            assertFalse(provider.isEnabled());
            assertTrue(status.getText().contains(
                    "Keeping the editable image placeholder"));
            assertTrue(status.getText().contains("Choose one of 2"));
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    environment.getState());

            asset.setSelectedIndex(1);
            assertTrue(provider.isEnabled());
            environment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.ImageProviderValue committed = assertInstanceOf(
                    PropertyValue.ImageProviderValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertFalse(committed.isUnresolved());
            assertEquals("assets/panel.webp", committed.assetName());
            assertEquals(java.util.Optional.of("ui_kit"), committed.packageName());
            return null;
        });
    }

    @Test
    void unresolvedImageEditorWithoutAssetsShowsAndRetainsPlaceholder()
            throws Exception {
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(),
                java.util.Optional.of(
                        "The current pubspec declares no safe image asset."));
        PropertyEditor editor = binding(
                property("flutter.widgets.Image", "image")).createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(
                PropertyValue.ImageProviderValue.unresolved()));
        FeatureDescriptor descriptor = descriptor(
                "Image", "Required Image.image provider editor.");
        descriptor.setValue(FlutterImageAssetChoices.FEATURE_ATTRIBUTE, choices);
        PropertyEnv environment = PropertyEnv.create(descriptor);
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> asset = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX + ".asset");
            JComboBox<?> provider = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX + ".provider");
            JLabel status = findNamed(
                    panel,
                    JLabel.class,
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX + ".assetStatus");

            assertEquals(0, asset.getItemCount(),
                    "the unresolved sentinel must remain outside the asset model");
            assertNull(asset.getSelectedItem());
            assertEquals(
                    FlutterImageProviderEditorComponent.UNRESOLVED_SELECTION_TEXT,
                    renderedNullSelectionText(asset));
            assertFalse(asset.isEnabled());
            assertFalse(provider.isEnabled());
            assertTrue(status.getText().contains(
                    "Keeping the editable image placeholder"));
            assertTrue(status.getText().contains("pubspec"));
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    environment.getState());

            environment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.ImageProviderValue committed = assertInstanceOf(
                    PropertyValue.ImageProviderValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertTrue(committed.isUnresolved());
            assertEquals(PropertyValue.ImageProviderValue.unresolved(), committed);
            return null;
        });
    }

    @Test
    void unavailableInventoryRetainsStoredResolvedImageProvider() throws Exception {
        PropertyValue.ImageProviderValue initial =
                PropertyValue.ImageProviderValue.asset("assets/removed.png");
        PropertyEditor editor = binding(
                property("flutter.widgets.Image", "image")).createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initial));
        FeatureDescriptor descriptor = descriptor(
                "Image", "Required Image.image provider editor.");
        descriptor.setValue(
                FlutterImageAssetChoices.FEATURE_ATTRIBUTE,
                new FlutterImageAssetChoices(
                        List.of(),
                        java.util.Optional.of(
                                "The declared image inventory is temporarily unavailable.")));
        PropertyEnv environment = PropertyEnv.create(descriptor);
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> asset = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX + ".asset");
            JLabel status = findNamed(
                    panel,
                    JLabel.class,
                    FlutterImageProviderEditorComponent.DIRECT_PREFIX + ".assetStatus");

            assertEquals(1, asset.getItemCount());
            assertNotNull(asset.getSelectedItem());
            assertTrue(status.getText().contains("stored image asset remains selected"));
            environment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.ImageProviderValue committed = assertInstanceOf(
                    PropertyValue.ImageProviderValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(initial, committed);
            assertFalse(committed.isUnresolved());
            return null;
        });
    }

    @Test
    void decorationImageWithoutAssetsStillRejectsEnablingImage() throws Exception {
        PropertyValue.BoxDecorationValue initial = decoration(
                java.util.Optional.empty(), java.util.Optional.empty());
        PropertyEditor editor = decorationEditor(initial);
        FeatureDescriptor descriptor = descriptor(
                "Decoration", "Typed Container DecorationImage editor.");
        descriptor.setValue(
                FlutterImageAssetChoices.FEATURE_ATTRIBUTE,
                new FlutterImageAssetChoices(
                        List.of(),
                        java.util.Optional.of(
                                "The current pubspec declares no safe image asset.")));
        PropertyEnv environment = PropertyEnv.create(descriptor);
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTabbedPane tabs = findNamed(
                    panel,
                    JTabbedPane.class,
                    FlutterContainerPropertyEditorComponents.DECORATION_TABS_NAME);
            Component imagePanel = tabs.getComponentAt(tabs.indexOfTab("Image"));
            JCheckBox enabled = findNamed(
                    imagePanel,
                    JCheckBox.class,
                    FlutterContainerPropertyEditorComponents
                            .DECORATION_IMAGE_ENABLED_NAME);
            JComboBox<?> asset = findNamed(
                    imagePanel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents
                            .DECORATION_IMAGE_ASSET_NAME);

            assertFalse(enabled.isSelected());
            assertEquals(0, asset.getItemCount());
            enabled.doClick();

            assertTrue(enabled.isSelected());
            assertFalse(asset.isEnabled());
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "DecorationImage must not acquire the direct-Image placeholder");
            return null;
        });
    }

    @Test
    void realNetBeansPropertyPanelInstallsBooleanCheckboxWithoutCombo()
            throws Exception {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
        WidgetNode widget = new WidgetNode(
                StableId.parse("fd96a765-01c9-45cb-aeb8-fe5613db4f79"),
                definition.typeId(),
                Map.of(
                        new PropertyName("data"),
                        new PropertyValue.StringValue("Text"),
                        new PropertyName("softWrap"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(),
                Extensions.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });
        Node.Property<?> softWrap = findProperty(node, "softWrap");

        onEdt(() -> {
            PropertyPanel panel = new PropertyPanel(
                    softWrap,
                    PropertyPanel.PREF_INPUT_STATE | PropertyPanel.PREF_TABLEUI);
            panel.setSize(320, 28);
            panel.addNotify();
            panel.doLayout();
            try {
                JCheckBox checkbox = findNamed(panel, JCheckBox.class,
                        FlutterPropertyEditorComponents.BOOLEAN_COMPONENT_NAME);
                assertNotNull(checkbox,
                        "NetBeans PropertyPanel must honor ExPropertyEditor.attachEnv");
                assertFalse(checkbox.isSelected());
                assertEquals("", checkbox.getText());
                assertNull(findFirst(panel, JComboBox.class),
                        "a boolean editor must never fall back to a combo box");
            } finally {
                panel.removeNotify();
            }
            return null;
        });
    }

    @Test
    void realNetBeansRendererModeNeverExposesBooleanCombo()
            throws Exception {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
        WidgetNode widget = new WidgetNode(
                StableId.parse("0a5d3e99-8c01-4c0d-87ec-2dcacb1531c5"),
                definition.typeId(),
                Map.of(
                        new PropertyName("data"),
                        new PropertyValue.StringValue("Text"),
                        new PropertyName("softWrap"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(),
                Extensions.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });
        Node.Property<?> softWrap = findProperty(node, "softWrap");

        onEdt(() -> {
            PropertyPanel panel = new PropertyPanel(
                    softWrap, PropertyPanel.PREF_TABLEUI);
            panel.setSize(320, 28);
            panel.addNotify();
            panel.doLayout();
            try {
                assertNull(findFirst(panel, JComboBox.class),
                        "the inactive NetBeans renderer must not expose a boolean combo box");
                assertTrue(softWrap.getPropertyEditor().isPaintable(),
                        "the inactive value cell must delegate to the checkbox painter");
            } finally {
                panel.removeNotify();
            }
            return null;
        });
    }

    @Test
    void fullPropertySheetUsesPaintedBooleanAndCheckboxEditor()
            throws Exception {
        assertFullPropertySheetBoolean(false,
                "aa05f63a-04ec-4713-b0fe-cc367a2bd2a6");
        assertFullPropertySheetBoolean(true,
                "aa05f63a-04ec-4713-b0fe-cc367a2bd2a7");
    }

    private static void assertFullPropertySheetBoolean(
            boolean explicitValue,
            String stableId) throws Exception {
        WidgetTypeId type = new WidgetTypeId("flutter.material.TextField");
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(type).orElseThrow();
        WidgetNode widget = new WidgetNode(
                StableId.parse(stableId),
                type,
                Map.of(new PropertyName("onTapAlwaysCalled"),
                        new PropertyValue.BooleanValue(explicitValue)),
                Map.of(),
                Extensions.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });
        Node.Property<?> booleanProperty = findProperty(
                node, "onTapAlwaysCalled");

        PropertySheet sheet = onEdt(() -> {
            PropertySheet result = new PropertySheet();
            result.setDescriptionAreaVisible(false);
            result.setSize(520, 900);
            result.addNotify();
            result.setNodes(new Node[]{node});
            return result;
        });
        try {
            JTable table = onEdt(() -> findFirst(sheet, JTable.class));
            assertNotNull(table);
            int row = awaitPropertyRow(table, "onTapAlwaysCalled");
            onEdt(() -> {
                Object value = table.getValueAt(row, 1);
                TableCellRenderer provider = table.getCellRenderer(row, 1);
                Component rendered = provider.getTableCellRendererComponent(
                        table, value, false, false, row, 1);
                assertEquals(
                        "org.openide.explorer.propertysheet.RendererFactory$StringRenderer",
                        rendered.getClass().getName());
                assertFalse(rendered instanceof JComboBox,
                        "the inactive boolean row must not render as a combo box");

                int width = Math.max(160,
                        table.getColumnModel().getColumn(1).getWidth());
                int height = table.getRowHeight(row);
                int[] expectedPixels = paintExpectedBooleanRenderer(
                        rendered, booleanProperty, width, height);
                int[] actualPixels = paintRenderer(rendered, width, height);
                assertTrue(java.util.Arrays.equals(
                                expectedPixels, actualPixels),
                        "the real PropertySheet renderer must invoke the "
                        + "boolean checkbox painter for explicit " + explicitValue);

                assertTrue(table.editCellAt(row, 1));
                JCheckBox editor = assertInstanceOf(
                        JCheckBox.class, table.getEditorComponent());
                assertEquals(FlutterPropertyEditorComponents.BOOLEAN_COMPONENT_NAME,
                        editor.getName());
                assertEquals(explicitValue, editor.isSelected());
                assertEquals(SwingConstants.CENTER,
                        editor.getHorizontalAlignment());
                assertEquals(SwingConstants.CENTER,
                        editor.getVerticalAlignment());
                assertNull(findFirst(editor, JComboBox.class));
                table.getCellEditor().cancelCellEditing();
                return null;
            });
        } finally {
            onEdt(() -> {
                sheet.removeNotify();
                return null;
            });
        }
    }

    @Test
    void realNetBeansPropertyPanelUsesEnumComboAndBoundedNumericField()
            throws Exception {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
        WidgetNode widget = new WidgetNode(
                StableId.parse("62913f77-08a1-46eb-91f3-a63dd9c009a6"),
                definition.typeId(),
                Map.of(new PropertyName("data"),
                        new PropertyValue.StringValue("Text")),
                Map.of(),
                Extensions.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });
        Node.Property<?> textAlign = findProperty(node, "textAlign");
        Node.Property<?> maxLines = findProperty(node, "maxLines");

        onEdt(() -> {
            PropertyPanel enumPanel = new PropertyPanel(
                    textAlign,
                    PropertyPanel.PREF_INPUT_STATE | PropertyPanel.PREF_TABLEUI);
            PropertyPanel numericPanel = new PropertyPanel(
                    maxLines,
                    PropertyPanel.PREF_INPUT_STATE | PropertyPanel.PREF_TABLEUI);
            enumPanel.setSize(320, 28);
            numericPanel.setSize(320, 28);
            enumPanel.addNotify();
            numericPanel.addNotify();
            enumPanel.doLayout();
            numericPanel.doLayout();
            try {
                JComboBox<?> combo = findFirst(enumPanel, JComboBox.class);
                assertNotNull(combo);
                assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT,
                        combo.getItemAt(0));
                assertTrue(combo.getItemCount() > 2,
                        "catalog enum values must be offered by the real combo");
                JTextField number = findNamed(
                        numericPanel, JTextField.class,
                        FlutterPropertyEditorComponents.NUMERIC_COMPONENT_NAME);
                assertNotNull(number);
                assertEquals("", number.getText(),
                        "an unset numeric value must enter edit mode as an empty draft");
                assertTrue(number.getToolTipText().contains("integer range 1"));
            } finally {
                enumPanel.removeNotify();
                numericPanel.removeNotify();
            }
            return null;
        });
    }

    @Test
    void optionalBooleanLeavesUnsetThenTogglesExplicitValues()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "softWrap"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();
        FeatureDescriptor descriptor = descriptor(
                "Soft Wrap", "Controls whether text may wrap.");
        PropertyEnv environment = PropertyEnv.create(descriptor);
        AtomicInteger events = new AtomicInteger();

        onEdt(() -> {
            inplace.addActionListener(event -> {
                assertSame(inplace, event.getSource());
                assertEquals(InplaceEditor.COMMAND_SUCCESS,
                        event.getActionCommand());
                events.incrementAndGet();
            });
            inplace.connect(editor, environment);
            JCheckBox checkbox = assertInstanceOf(
                    JCheckBox.class, inplace.getComponent());
            assertEquals(FlutterPropertyEditorComponents.BOOLEAN_COMPONENT_NAME,
                    checkbox.getName());
            assertEquals("Soft Wrap",
                    checkbox.getAccessibleContext().getAccessibleName());
            assertEquals(AccessibleRole.CHECK_BOX,
                    checkbox.getAccessibleContext().getAccessibleRole());
            assertTrue(checkbox.getAccessibleContext()
                    .getAccessibleDescription().contains("Restore Default"));
            assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT,
                    checkbox.getText());
            assertFalse(checkbox.getAccessibleContext().getAccessibleStateSet()
                    .contains(AccessibleState.CHECKED));

            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(true)),
                    inplace.getValue());
            assertTrue(checkbox.isSelected());
            assertTrue(checkbox.getAccessibleContext().getAccessibleStateSet()
                    .contains(AccessibleState.CHECKED));
            assertEquals("", checkbox.getText());
            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(false)),
                    inplace.getValue());
            assertFalse(checkbox.isSelected());
            assertFalse(checkbox.getAccessibleContext().getAccessibleStateSet()
                    .contains(AccessibleState.CHECKED));
            assertEquals("", checkbox.getText());
            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(true)),
                    inplace.getValue(),
                    "Restore Default, not a third checkbox click, returns to unset");
            assertEquals(3, events.get());
            inplace.clear();
            assertNull(inplace.getPropertyEditor());
            assertEquals("", checkbox.getText());
            assertFalse(checkbox.isSelected());
            assertNull(checkbox.getClientProperty("JButton.selectedState"));
            return null;
        });
    }

    @Test
    void requiredBooleanNeverProducesUnset() throws Exception {
        PropertyDefinition definition = new PropertyDefinition(
                new PropertyName("enabled"),
                DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.AnyValue(
                        PropertyValueKind.BOOLEAN)),
                java.util.Optional.empty());
        FlutterTypedPropertyEditors.Binding binding = binding(definition);
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.BooleanValue(false)));
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();

        onEdt(() -> {
            inplace.connect(editor, PropertyEnv.create(descriptor(
                    "Enabled", "Required boolean.")));
            JCheckBox checkbox = (JCheckBox) inplace.getComponent();
            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(true)),
                    inplace.getValue());
            checkbox.doClick();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(false)),
                    inplace.getValue());
            return null;
        });
    }

    @Test
    void boundedIntegerControlRejectsZeroAndCommitsOneExactly()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "maxLines"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Max Lines", "Positive line limit."));
        AtomicInteger commits = new AtomicInteger();

        onEdt(() -> {
            inplace.addActionListener(ignored -> {
                editor.setAsText((String) inplace.getValue());
                commits.incrementAndGet();
            });
            inplace.connect(editor, environment);
            JTextField field = assertInstanceOf(
                    JTextField.class, inplace.getComponent());
            assertEquals(FlutterPropertyEditorComponents.NUMERIC_COMPONENT_NAME,
                    field.getName());
            assertEquals("", field.getText());
            assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT, editor.getAsText(),
                    "the inactive property presentation must remain <not set>");
            assertTrue(field.getAccessibleContext()
                    .getAccessibleDescription().contains("integer range 1"));

            field.setText("0");
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(0, commits.get());
            assertEquals("0", inplace.getValue(),
                    "InplaceEditor must expose the invalid draft so NetBeans can reject it");
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());

            field.setText("3");
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_VALID, environment.getState());
            assertEquals(1, commits.get());
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.IntegerValue(BigInteger.valueOf(3))),
                    editor.getValue());

            field.setText(DartNumericLiterals.MAX_PORTABLE_INTEGER.toString());
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_VALID, environment.getState());
            assertEquals(2, commits.get());
            field.setText(DartNumericLiterals.MAX_PORTABLE_INTEGER
                    .add(BigInteger.ONE).toString());
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(2, commits.get());
            return null;
        });
    }

    @Test
    void indexedStackIndexEditorKeepsOmittedNullAndIntegerDistinct()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.IndexedStack", "index"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_INTEGER,
                binding.editorKind());
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.NULL),
                binding.definition().acceptedKinds());
        assertTrue(binding.optional());

        PropertyEditor editor = binding.createEditor();
        assertTrue(editor.supportsCustomEditor());
        editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT);
        assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
        editor.setAsText("NULL");
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),
                editor.getValue());
        assertEquals("null", editor.getAsText());
        editor.setAsText("3");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.IntegerValue(BigInteger.valueOf(3))),
                editor.getValue());
        assertThrows(IllegalArgumentException.class, () -> editor.setAsText("-1"));
        assertThrows(IllegalArgumentException.class, () -> editor.setAsText("1.5"));

        editor.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();
        PropertyEnv inplaceEnvironment = PropertyEnv.create(descriptor(
                "Index", "Nullable zero-based child index."));
        onEdt(() -> {
            inplace.addActionListener(ignored ->
                    editor.setAsText((String) inplace.getValue()));
            inplace.connect(editor, inplaceEnvironment);
            JTextField field = assertInstanceOf(
                    JTextField.class, inplace.getComponent());
            assertEquals(FlutterPropertyEditorComponents.NUMERIC_COMPONENT_NAME,
                    field.getName());
            assertEquals("null", field.getText());

            field.setText("2");
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_VALID, inplaceEnvironment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                    editor.getValue());
            field.setText("-1");
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_INVALID, inplaceEnvironment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                    editor.getValue(), "an invalid sentinel must not replace the value");
            field.setText("null");
            field.postActionEvent();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.NullValue()),
                    editor.getValue());
            field.setText("");
            field.postActionEvent();
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
            inplace.clear();
            return null;
        });

        PropertyEditor custom = binding.createEditor();
        FlutterPropertyCellValue initialNull = FlutterPropertyCellValue.explicit(
                new PropertyValue.NullValue());
        custom.setValue(initialNull);
        PropertyEnv customEnvironment = PropertyEnv.create(descriptor(
                "Index", "Omitted, explicit null, or non-negative child index."));
        ((ExPropertyEditor) custom).attachEnv(customEnvironment);
        onEdt(() -> {
            Component panel = custom.getCustomEditor();
            assertSame(panel, custom.getCustomEditor());
            JComboBox<?> mode = findNamed(
                    panel, JComboBox.class,
                    FlutterPropertyEditorComponents.NULLABLE_INTEGER_MODE_NAME);
            JTextField value = findNamed(
                    panel, JTextField.class,
                    FlutterPropertyEditorComponents.NULLABLE_INTEGER_VALUE_NAME);
            assertNotNull(mode);
            assertNotNull(value);
            assertEquals(List.of(
                    "Use Flutter default 0 (omit argument)",
                    "Show no child (explicit null)",
                    "Show child at index"), comboLabels(mode));
            assertEquals("Show no child (explicit null)",
                    String.valueOf(mode.getSelectedItem()));
            assertFalse(value.isEnabled());

            selectLabel(mode, "Show child at index");
            value.setText("1");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    customEnvironment.getState());
            assertEquals(initialNull, custom.getValue(),
                    "custom editing must remain a local draft until OK");
            customEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.IntegerValue(BigInteger.ONE)),
                    custom.getValue());
            return null;
        });

        PropertyEditor reset = binding.createEditor();
        reset.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.IntegerValue(BigInteger.ONE)));
        PropertyEnv resetEnvironment = PropertyEnv.create(descriptor(
                "Index", "Reset nullable child index."));
        ((ExPropertyEditor) reset).attachEnv(resetEnvironment);
        onEdt(() -> {
            Component panel = reset.getCustomEditor();
            JComboBox<?> mode = findNamed(
                    panel, JComboBox.class,
                    FlutterPropertyEditorComponents.NULLABLE_INTEGER_MODE_NAME);
            selectLabel(mode, "Use Flutter default 0 (omit argument)");
            resetEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), reset.getValue());
            return null;
        });
    }

    @Test
    void doubleControlRetainsDoubleKindAndNonNegativeConstraint()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Column", "spacing"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Spacing", "Non-negative spacing."));

        onEdt(() -> {
            inplace.addActionListener(ignored ->
                    editor.setAsText((String) inplace.getValue()));
            inplace.connect(editor, environment);
            JTextField field = (JTextField) inplace.getComponent();
            field.setText("2");
            field.postActionEvent();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.DoubleValue(BigDecimal.valueOf(2))),
                    editor.getValue());
            field.setText("-0.01");
            field.postActionEvent();
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("-0.01", inplace.getValue());
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.DoubleValue(BigDecimal.valueOf(2))),
                    editor.getValue(),
                    "an invalid draft must not replace the last valid value");
            return null;
        });
    }

    @Test
    void elevatedButtonStrokeAlignEditorAcceptsFiniteValuesBeyondPresetConstants() {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.material.ElevatedButton",
                        "stylePressedSideStrokeAlign"));
        PropertyEditor editor = binding.createEditor();

        editor.setAsText("-3.5");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.DoubleValue(new BigDecimal("-3.5"))),
                editor.getValue());
        editor.setAsText("12.25");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.DoubleValue(new BigDecimal("12.25"))),
                editor.getValue());
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("NaN"));
    }

    @Test
    void nullableDoubleAcceptsZeroAndRestoresUnsetWithoutChangingItsKind()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Center", "widthFactor"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.DoubleValue(BigDecimal.ONE)));
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();

        onEdt(() -> {
            inplace.addActionListener(ignored ->
                    editor.setAsText((String) inplace.getValue()));
            inplace.connect(editor, PropertyEnv.create(descriptor(
                    "Width Factor", "Optional non-negative factor.")));
            JTextField field = (JTextField) inplace.getComponent();

            field.setText("0");
            field.postActionEvent();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                    editor.getValue());
            field.setText("0.5");
            field.postActionEvent();
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.DoubleValue(new BigDecimal("0.5"))),
                    editor.getValue());
            field.setText("");
            field.postActionEvent();
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
            return null;
        });
    }

    @Test
    void numericEditorClearRemovesInvalidDraftBeforeReuse() throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "maxLines"));
        PropertyEditor firstEditor = binding.createEditor();
        firstEditor.setValue(FlutterPropertyCellValue.unset());
        InplaceEditor inplace = FlutterPropertyEditorComponents
                .inplaceFactory(binding).orElseThrow().getInplaceEditor();

        onEdt(() -> {
            inplace.connect(firstEditor, PropertyEnv.create(descriptor(
                    "Max Lines", "Positive line limit.")));
            JTextField field = (JTextField) inplace.getComponent();
            assertEquals("", field.getText());
            firstEditor.setValue(FlutterPropertyCellValue.explicit(
                    new PropertyValue.IntegerValue(BigInteger.valueOf(4))));
            inplace.reset();
            assertEquals("4", field.getText());
            firstEditor.setValue(FlutterPropertyCellValue.unset());
            inplace.reset();
            assertEquals("", field.getText(),
                    "resetting an active numeric editor to default must clear its draft");
            field.setText("0");
            assertEquals("error", field.getClientProperty("JComponent.outline"));

            inplace.clear();
            assertNull(inplace.getPropertyEditor());
            assertEquals("", field.getText());
            assertNull(field.getClientProperty("JComponent.outline"));
            assertNull(field.getToolTipText());
            assertNull(field.getAccessibleContext().getAccessibleName());

            PropertyEditor secondEditor = binding.createEditor();
            secondEditor.setValue(FlutterPropertyCellValue.unset());
            inplace.connect(secondEditor, PropertyEnv.create(descriptor(
                    "Max Lines", "Positive line limit.")));
            assertEquals("", field.getText(),
                    "a reused editor must not restore the presentation placeholder");
            secondEditor.setValue(FlutterPropertyCellValue.explicit(
                    new PropertyValue.IntegerValue(BigInteger.valueOf(4))));
            inplace.reset();
            assertEquals("4", field.getText());
            inplace.setValue("7");
            assertEquals("7", inplace.getValue(),
                    "setValue must accept every type returned by getValue");
            return null;
        });
    }

    @Test
    void colorChooserIsTransactionalAndPreservesExactAlpha() throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "selectionColor"));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                PropertyValue.ColorValue.fromWireArgb("0x80112233"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Selection Color", "Exact Flutter ARGB selection color."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JColorChooser chooser = findNamed(panel, JColorChooser.class,
                    FlutterPropertyEditorComponents.COLOR_CHOOSER_NAME);
            JSpinner alpha = findNamed(panel, JSpinner.class,
                    FlutterPropertyEditorComponents.COLOR_ALPHA_NAME);
            JTextField argb = findNamed(panel, JTextField.class,
                    FlutterPropertyEditorComponents.COLOR_ARGB_NAME);
            assertNotNull(chooser);
            assertNotNull(alpha);
            assertNotNull(argb);
            JTextField alphaText = ((JSpinner.DefaultEditor) alpha.getEditor())
                    .getTextField();
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    environment.getState());

            chooser.setColor(new Color(0xAA, 0xBB, 0xCC));
            alphaText.setText("64");
            assertEquals(initial, editor.getValue(),
                    "chooser preview events must remain a local draft");
            assertEquals(0, committedChanges.get());
            assertEquals("0x40AABBCC", argb.getText());

            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                    PropertyValue.ColorValue.fromWireArgb("0x40AABBCC")),
                    editor.getValue());
            assertEquals(1, committedChanges.get());
            environment.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(1, committedChanges.get(),
                    "one accepted dialog may publish exactly one value");

            argb.setText("#AABBCC");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(
                            PropertyValue.ColorValue.fromWireArgb("0x40AABBCC")),
                    editor.getValue());

            JComboBox<?> sourceMode = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.THEME_COLOR_MODE_NAME);
            assertNotNull(sourceMode);
            sourceMode.setSelectedItem("Not set");
            assertFalse(argb.isEnabled());
            assertNull(argb.getClientProperty("JComponent.outline"));
            assertTrue(argb.getAccessibleContext()
                    .getAccessibleDescription().contains("0xAARRGGBB"));
            return null;
        });
    }

    @Test
    void manuallyTypedColorAlphaValidatesBoundsAndRecoversBeforeCommit()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "selectionColor"));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                PropertyValue.ColorValue.fromWireArgb("0xFF112233"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Selection Color", "Exact Flutter ARGB selection color."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JColorChooser chooser = findNamed(panel, JColorChooser.class,
                    FlutterPropertyEditorComponents.COLOR_CHOOSER_NAME);
            JSpinner alpha = findNamed(panel, JSpinner.class,
                    FlutterPropertyEditorComponents.COLOR_ALPHA_NAME);
            assertNotNull(chooser);
            assertNotNull(alpha);
            JTextField alphaText = ((JSpinner.DefaultEditor) alpha.getEditor())
                    .getTextField();

            alphaText.setText("999");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error",
                    alphaText.getClientProperty("JComponent.outline"));
            assertEquals(initial, editor.getValue());

            chooser.setColor(new Color(0x44, 0x55, 0x66));
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState(),
                    "RGB changes must not hide an invalid alpha draft");
            alphaText.setText("0");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    environment.getState());
            assertNull(alphaText.getClientProperty("JComponent.outline"));

            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            PropertyValue.ColorValue.fromWireArgb("0x00445566")),
                    editor.getValue());
            return null;
        });
    }

    @Test
    void edgeInsetsDialogExposesStableComponentsAndRestoresEveryPaddingMode()
            throws Exception {
        assertEdgeInsetsDialogRoundTrip(edge("8", "8", "8", "8"), "All sides");
        assertEdgeInsetsDialogRoundTrip(edge("4", "8", "4", "8"), "Symmetric");
        assertEdgeInsetsDialogRoundTrip(edge("1", "2", "3", "4"),
                "Physical (left/right)");
        assertEdgeInsetsDialogRoundTrip(directionalEdge("1", "2", "3", "4"),
                "Directional (start/end)");
    }

    @Test
    void safeAreaMinimumOffersOnlyConcretePhysicalEdgeInsetsWhilePaddingKeepsDirectional()
            throws Exception {
        FlutterTypedPropertyEditors.Binding safeArea = binding(
                property("flutter.widgets.SafeArea", "minimum"));
        FlutterTypedPropertyEditors.Binding padding = binding(
                property("flutter.widgets.Padding", "padding"));
        assertFalse(safeArea.directionalEdgeInsetsAllowed());
        assertTrue(padding.directionalEdgeInsetsAllowed());

        PropertyEditor editor = safeArea.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Minimum", "Concrete physical EdgeInsets for SafeArea."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> mode = findNamed(
                    panel, JComboBox.class, "flutter.edgeInsets.mode");
            assertEquals(List.of(
                    "All sides",
                    "Symmetric",
                    "Physical (left/right)"), comboLabels(mode));
            assertNull(findNamed(panel, JTextField.class,
                    "flutter.edgeInsets.directional.start"));
            assertNull(findNamed(panel, JTextField.class,
                    "flutter.edgeInsets.directional.end"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("directional start/end values are not accepted"));
            return null;
        });

        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("directional: 1, 2, 3, 4"));
        PropertyEditor legacy = padding.createEditor();
        legacy.setAsText("directional: 1, 2, 3, 4");
        assertEquals(FlutterPropertyCellValue.explicit(
                directionalEdge("1", "2", "3", "4")), legacy.getValue());
    }

    @Test
    void edgeInsetsDialogCommitsAllSymmetricPhysicalAndDirectionalAtomically()
            throws Exception {
        assertEdgeInsetsDialogCommit(
                "All sides",
                Map.of(FlutterPropertyEditorComponents.EDGE_ALL_NAME, "8.5"),
                edge("8.5", "8.5", "8.5", "8.5"));
        assertEdgeInsetsDialogCommit(
                "Symmetric",
                Map.of(
                        "flutter.edgeInsets.horizontal", "3.25",
                        "flutter.edgeInsets.vertical", "7.5"),
                edge("3.25", "7.5", "3.25", "7.5"));
        assertEdgeInsetsDialogCommit(
                "Physical (left/right)",
                Map.of(
                        "flutter.edgeInsets.left", "1",
                        "flutter.edgeInsets.top", "2.5",
                        "flutter.edgeInsets.right", "3",
                        "flutter.edgeInsets.bottom", "4.25"),
                edge("1", "2.5", "3", "4.25"));
        assertEdgeInsetsDialogCommit(
                "Directional (start/end)",
                Map.of(
                        "flutter.edgeInsets.directional.start", "5",
                        "flutter.edgeInsets.directional.top", "6.5",
                        "flutter.edgeInsets.directional.end", "7",
                        "flutter.edgeInsets.directional.bottom", "8.25"),
                directionalEdge("5", "6.5", "7", "8.25"));
    }

    @Test
    void edgeInsetsDialogRejectsMalformedAndNegativeDraftsInEveryMode()
            throws Exception {
        assertEdgeInsetsDialogInvalid(
                "All sides", FlutterPropertyEditorComponents.EDGE_ALL_NAME, "not-a-number");
        assertEdgeInsetsDialogInvalid(
                "Symmetric", "flutter.edgeInsets.horizontal", "-1");
        assertEdgeInsetsDialogInvalid(
                "Physical (left/right)", "flutter.edgeInsets.top", "");
        assertEdgeInsetsDialogInvalid(
                "Directional (start/end)",
                "flutter.edgeInsets.directional.end", "-0.25");
    }

    @Test
    void optionalStringCustomEditorKeepsLiteralNotSetDistinctFromReset()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "semanticsLabel"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Semantics Label", "Optional accessibility text."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use inherited/default value (omit argument)");
            JTextArea text = findNamed(panel, JTextArea.class,
                    "flutter.string.text");
            assertNotNull(useDefault);
            assertNotNull(text);
            assertTrue(useDefault.isSelected());
            assertFalse(text.isEnabled());

            useDefault.doClick();
            text.setText(FlutterPropertyCellValue.NOT_SET_TEXT);
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.StringValue(
                                    FlutterPropertyCellValue.NOT_SET_TEXT)),
                    editor.getValue());
            return null;
        });
    }

    @Test
    void callbackCustomEditorStartsEmptyAndCommitsOnlyAValidatedIdentifier()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.material.ElevatedButton", "onPressed"));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "On pressed", "Optional validated Dart callback identifier."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use inherited/default value (omit argument)");
            JTextField handler = findNamed(panel, JTextField.class,
                    FlutterPropertyEditorComponents.CALLBACK_TEXT_NAME);
            assertNotNull(useDefault);
            assertNotNull(handler);
            assertTrue(useDefault.isSelected());
            assertFalse(handler.isEnabled());
            assertEquals("", handler.getText(),
                    "the <not set> presentation token must not leak into edit mode");

            useDefault.doClick();
            assertTrue(handler.isEnabled());
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            handler.setText("() => arbitraryDart()");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(),
                    "invalid callback drafts must stay local");

            handler.setText("_handlePress");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.CallbackValue("_handlePress")),
                    editor.getValue());
            return null;
        });
    }

    @Test
    void fontFallbackCustomEditorKeepsOneFamilyPerLineAndCommitsAtomically()
            throws Exception {
        PropertyDefinition definition = property(
                "flutter.widgets.Text", "styleFontFamilyFallback");
        FlutterTypedPropertyEditors.Binding binding = FlutterTypedPropertyEditors
                .binding(definition, TextWidgetPropertySchema.find(definition.name()))
                .orElseThrow();
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                new PropertyValue.StringValue("Roboto\nNoto Sans"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Font fallbacks", "Fallback font families, one per line."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTextArea text = findNamed(panel, JTextArea.class,
                    FlutterPropertyEditorComponents.NEWLINE_LIST_TEXT_NAME);
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use inherited/default value (omit argument)");
            assertNotNull(text);
            assertNotNull(useDefault);
            assertFalse(useDefault.isSelected());
            assertTrue(text.isEnabled());
            assertEquals("Roboto\nNoto Sans", text.getText());

            text.setText(" Inter \r\n\r\n Noto Color Emoji ");
            assertEquals(initial, editor.getValue(),
                    "font-list document events must remain a local draft");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.StringValue(
                                    "Inter\nNoto Color Emoji")),
                    editor.getValue());
            return null;
        });
    }

    @Test
    void themeAwareColorOffersTypedModesAndCommitsOnlyOnDialogOk()
            throws Exception {
        PropertyDefinition definition = new PropertyDefinition(
                new PropertyName("styleColor"),
                DartParameter.named(0, false),
                List.of(
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR),
                        new PropertyValueConstraint.ThemeTokenValues(List.of(
                                "material.colorScheme.primary",
                                "material.colorScheme.error"))),
                java.util.Optional.empty());
        FlutterTypedPropertyEditors.Binding binding = binding(definition);
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                PropertyValue.ColorValue.fromWireArgb("0xFF112233"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Color", "Literal or Material ColorScheme role."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger commits = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> mode = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.THEME_COLOR_MODE_NAME);
            JComboBox<?> role = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.THEME_COLOR_ROLE_NAME);
            assertNotNull(mode);
            assertNotNull(role);
            assertEquals(2, role.getItemCount());
            assertEquals("Primary", role.getItemAt(0));
            assertEquals("Error", role.getItemAt(1));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("semantic Material ColorScheme role"));

            mode.setSelectedItem("Theme role");
            role.setSelectedItem("Primary");
            assertEquals(initial, editor.getValue(),
                    "theme selection remains a local dialog draft");
            assertEquals(0, commits.get());

            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                    new PropertyValue.ThemeTokenValue(
                            new ThemeToken("material.colorScheme.primary"))),
                    editor.getValue());
            assertEquals(1, commits.get());
            environment.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(1, commits.get(), "one OK publishes one immutable value");
            return null;
        });

        PropertyEditor cancelled = binding.createEditor();
        cancelled.setValue(initial);
        PropertyEnv cancelEnvironment = PropertyEnv.create(descriptor(
                "Color", "Literal or Material ColorScheme role."));
        ((ExPropertyEditor) cancelled).attachEnv(cancelEnvironment);
        onEdt(() -> {
            Component panel = cancelled.getCustomEditor();
            JComboBox<?> mode = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.THEME_COLOR_MODE_NAME);
            JComboBox<?> role = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.THEME_COLOR_ROLE_NAME);
            mode.setSelectedItem("Theme role");
            role.setSelectedItem("Error");
            // A NetBeans Cancel closes the custom editor without STATE_VALID.
            assertEquals(initial, cancelled.getValue());
            return null;
        });
    }

    @Test
    void textThemeRoleUsesAClosedAccessibleComboAndSupportsReset() {
        PropertyDefinition subsetDefinition = new PropertyDefinition(
                new PropertyName("styleThemeTextStyle"),
                DartParameter.named(0, false),
                List.of(new PropertyValueConstraint.ThemeTokenValues(List.of(
                        "material.textTheme.bodyMedium",
                        "material.textTheme.titleLarge"))),
                java.util.Optional.empty());
        FlutterTypedPropertyEditors.Binding binding = binding(subsetDefinition);
        PropertyEditor editor = binding.createEditor();

        assertEquals(List.of(
                FlutterPropertyCellValue.NOT_SET_TEXT,
                "Body Medium", "Title Large"), List.of(editor.getTags()));
        editor.setAsText("Body Medium");
        assertEquals(FlutterPropertyCellValue.explicit(
                new PropertyValue.ThemeTokenValue(
                        new ThemeToken("material.textTheme.bodyMedium"))),
                editor.getValue());
        editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT);
        assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("madeUpRole"));
    }

    @Test
    void iconDataEditorSearchesTheFullRegistryAndNeverOffersArbitraryMetadata()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Icon", "icon"));
        PropertyValue.IconDataValue star = new PropertyValue.IconDataValue(
                java.util.Optional.of(0xE5F9),
                java.util.Optional.of("MaterialIcons"),
                java.util.Optional.empty(), false, List.of());
        PropertyEditor registryEditor = binding.createEditor();
        registryEditor.setValue(FlutterPropertyCellValue.explicit(star));
        PropertyEnv registryEnvironment = PropertyEnv.create(descriptor(
                "Icon data", "Typed nullable IconData metadata."));
        ((ExPropertyEditor) registryEditor).attachEnv(registryEnvironment);

        onEdt(() -> {
            Component panel = registryEditor.getCustomEditor();
            JTextField search = findNamed(panel, JTextField.class,
                    FlutterPropertyEditorComponents.MATERIAL_ICON_SEARCH_NAME);
            JList<?> results = findNamed(panel, JList.class,
                    FlutterPropertyEditorComponents.MATERIAL_ICON_RESULTS_NAME);
            JCheckBox none = findNamed(panel, JCheckBox.class,
                    FlutterPropertyEditorComponents.MATERIAL_ICON_NONE_NAME);
            javax.swing.JLabel status = findNamed(panel, javax.swing.JLabel.class,
                    FlutterPropertyEditorComponents.MATERIAL_ICON_STATUS_NAME);
            javax.swing.JLabel requirement = findNamed(panel, javax.swing.JLabel.class,
                    FlutterPropertyEditorComponents.MATERIAL_ICON_REQUIREMENT_NAME);
            assertNotNull(search);
            assertNotNull(results);
            assertNotNull(none);
            assertNotNull(status);
            assertNotNull(requirement);
            assertEquals("star", search.getText());
            MaterialIconRegistry.MaterialIcon selected = assertInstanceOf(
                    MaterialIconRegistry.MaterialIcon.class,
                    results.getSelectedValue());
            assertEquals("star", selected.name());
            assertFalse(none.isSelected());
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("no arbitrary IconData"));
            assertNull(findNamed(panel, JTextField.class,
                    "flutter.iconData.codePoint"));
            assertNull(findNamed(panel, JTextField.class,
                    "flutter.iconData.fontFamily"));
            assertNull(findFirst(panel, JComboBox.class));
            assertEquals(results.getModel().getSize()
                    + " results · 8,825 total · Flutter 3.44.8",
                    status.getText());
            assertEquals("Material icon search status: " + status.getText(),
                    status.getAccessibleContext().getAccessibleName());
            assertEquals(status.getText(),
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals("Requires flutter.uses-material-design: true",
                    requirement.getText());

            search.setText("arrow back");
            assertTrue(results.getModel().getSize() > 0);
            MaterialIconRegistry.MaterialIcon arrowBack = assertInstanceOf(
                    MaterialIconRegistry.MaterialIcon.class,
                    results.getModel().getElementAt(0));
            assertEquals("arrow_back", arrowBack.name());
            assertTrue(arrowBack.matchTextDirection());
            Component rendered = renderedListCell(results, arrowBack);
            javax.swing.JLabel renderedLabel = assertInstanceOf(
                    javax.swing.JLabel.class, rendered);
            assertTrue(renderedLabel.getText().endsWith("RTL"));
            assertNotNull(renderedLabel.getIcon(),
                    "every Material icon result must expose the shared preview");
            assertEquals(FlutterPropertyValuePreview.MATERIAL_ICON_LIST_SIZE,
                    renderedLabel.getIcon().getIconWidth());
            assertEquals(FlutterPropertyValuePreview.MATERIAL_ICON_LIST_SIZE,
                    renderedLabel.getIcon().getIconHeight());
            assertEquals(results.getModel().getSize()
                    + " results · 8,825 total · Flutter 3.44.8",
                    status.getText());
            assertEquals("Material icon search status: " + status.getText(),
                    status.getAccessibleContext().getAccessibleName());
            assertEquals(status.getText(),
                    status.getAccessibleContext().getAccessibleDescription());
            results.setSelectedIndex(0);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    registryEnvironment.getState());
            registryEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                    new PropertyValue.IconDataValue(
                            java.util.Optional.of(arrowBack.codePoint()),
                            java.util.Optional.of("MaterialIcons"),
                            java.util.Optional.empty(),
                            arrowBack.matchTextDirection(),
                            List.of())), registryEditor.getValue());
            return null;
        });

        PropertyEditor noneEditor = binding.createEditor();
        noneEditor.setValue(FlutterPropertyCellValue.explicit(star));
        PropertyEnv noneEnvironment = PropertyEnv.create(descriptor(
                "Icon data", "Typed nullable IconData metadata."));
        ((ExPropertyEditor) noneEditor).attachEnv(noneEnvironment);
        onEdt(() -> {
            Component panel = noneEditor.getCustomEditor();
            JCheckBox none = findNamed(panel, JCheckBox.class,
                    FlutterPropertyEditorComponents.MATERIAL_ICON_NONE_NAME);
            JTextField search = findNamed(panel, JTextField.class,
                    FlutterPropertyEditorComponents.MATERIAL_ICON_SEARCH_NAME);
            JList<?> results = findNamed(panel, JList.class,
                    FlutterPropertyEditorComponents.MATERIAL_ICON_RESULTS_NAME);
            none.doClick();
            assertFalse(search.isEnabled());
            assertFalse(results.isEnabled());
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    noneEnvironment.getState());
            noneEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            PropertyValue.IconDataValue.none()),
                    noneEditor.getValue());
            return null;
        });
    }

    @Test
    void paintEditorCommitsTheCompleteStructuredDraftAndCanBeEditedAgain()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(complexProperty(
                "styleForeground", PropertyValueKind.PAINT));
        PropertyValue.PaintValue initialPaint = PropertyValue.PaintValue.defaults(
                new ColorSource.Literal(0xFF010203L));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initialPaint));
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Foreground paint", "Structured dart:ui Paint."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> style = findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.PAINT_STYLE_NAME);
            JTextField width = findNamed(panel, JTextField.class,
                    FlutterComplexPropertyEditorComponents.PAINT_STROKE_WIDTH_NAME);
            assertNotNull(style);
            assertNotNull(width);
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("atomically"));

            style.setSelectedItem("stroke");
            width.setText("1E+400");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(initialPaint),
                    editor.getValue());
            width.setText("2.5");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(initialPaint),
                    editor.getValue());
            environment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.PaintValue accepted = assertInstanceOf(
                    PropertyValue.PaintValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(PropertyValue.PaintValue.Style.STROKE, accepted.style());
            assertEquals(new BigDecimal("2.5"), accepted.strokeWidth());
            return null;
        });

        PropertyEditor repeated = binding.createEditor();
        repeated.setValue(editor.getValue());
        PropertyEnv repeatedEnvironment = PropertyEnv.create(descriptor(
                "Foreground paint", "Structured dart:ui Paint."));
        ((ExPropertyEditor) repeated).attachEnv(repeatedEnvironment);
        onEdt(() -> {
            Component panel = repeated.getCustomEditor();
            assertEquals("stroke", findNamed(panel, JComboBox.class,
                    FlutterComplexPropertyEditorComponents.PAINT_STYLE_NAME)
                    .getSelectedItem());
            assertEquals("2.5", findNamed(panel, JTextField.class,
                    FlutterComplexPropertyEditorComponents.PAINT_STROKE_WIDTH_NAME)
                    .getText());
            return null;
        });
    }

    @Test
    void shadowTableIsOrderedTypedAndTransactional() throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(complexProperty(
                "styleShadows", PropertyValueKind.SHADOW_LIST));
        PropertyValue.ShadowListValue initialValue = new PropertyValue.ShadowListValue(
                List.of(new PropertyValue.ShadowListValue.Shadow(
                        StableId.parse("f0bf1d8a-c780-4654-9770-91d53600e770"),
                        new ColorSource.Theme(new ThemeToken(
                                "material.colorScheme.shadow")),
                        BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3))));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(initialValue);
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Shadows", "Ordered typed shadows."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JTable table = findNamed(panel, JTable.class,
                    FlutterComplexPropertyEditorComponents.SHADOW_TABLE_NAME);
            assertNotNull(table);
            assertTrue(table.getAccessibleContext().getAccessibleDescription()
                    .contains("Color accepts"));
            assertNotNull(findButton(panel, "Add"));
            assertNotNull(findButton(panel, "Remove"));
            assertNotNull(findButton(panel, "Up"));
            assertNotNull(findButton(panel, "Down"));

            table.getModel().setValueAt("1E-400", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            table.getModel().setValueAt("1", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertTrue(table.editCellAt(0, 3));
            JTextField activeCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            activeCell.setText("-1");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals(initial, editor.getValue());
            assertTrue(table.editCellAt(0, 3));
            JTextField validActiveCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            validActiveCell.setText("4.5");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_VALID, environment.getState(),
                    "a valid active cell must commit with one OK validation");
            PropertyValue.ShadowListValue accepted = assertInstanceOf(
                    PropertyValue.ShadowListValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(new BigDecimal("4.5"), accepted.items().getFirst().blurRadius());
            assertInstanceOf(ColorSource.Theme.class,
                    accepted.items().getFirst().color());
            return null;
        });
    }

    @Test
    void featureAndVariationTablesValidateTagsRangesAndExposePresets()
            throws Exception {
        PropertyValue.FontFeatureListValue featureValue =
                new PropertyValue.FontFeatureListValue(List.of(
                        new PropertyValue.FontFeatureListValue.FontFeature(
                                StableId.parse("2f41fc98-28a5-4698-972e-c7f3c2c53b38"),
                                "liga", 1)));
        PropertyEditor featureEditor = binding(complexProperty(
                "styleFontFeatures", PropertyValueKind.FONT_FEATURE_LIST)).createEditor();
        featureEditor.setValue(FlutterPropertyCellValue.explicit(featureValue));
        PropertyEnv featureEnvironment = PropertyEnv.create(descriptor(
                "Font features", "OpenType features."));
        ((ExPropertyEditor) featureEditor).attachEnv(featureEnvironment);

        onEdt(() -> {
            Component panel = featureEditor.getCustomEditor();
            JTable table = findNamed(panel, JTable.class,
                    FlutterComplexPropertyEditorComponents.FONT_FEATURE_TABLE_NAME);
            assertNotNull(findNamed(panel, JComboBox.class,
                    "flutter.fontFeatures.preset"));
            table.getModel().setValueAt("bad", 0, 0);
            assertEquals(PropertyEnv.STATE_INVALID, featureEnvironment.getState());
            table.getModel().setValueAt("smcp", 0, 0);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    featureEnvironment.getState());
            featureEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_VALID, featureEnvironment.getState(),
                    "an idle valid table must remain valid after OK");
            PropertyValue.FontFeatureListValue accepted = assertInstanceOf(
                    PropertyValue.FontFeatureListValue.class,
                    ((FlutterPropertyCellValue) featureEditor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals("smcp", accepted.items().getFirst().tag());
            return null;
        });

        PropertyValue.FontVariationListValue variationValue =
                new PropertyValue.FontVariationListValue(List.of(
                        new PropertyValue.FontVariationListValue.FontVariation(
                                StableId.parse("e3b1b676-ed66-4caf-94c5-fbe6b19ff8b1"),
                                "wght", BigDecimal.valueOf(400))));
        PropertyEditor variationEditor = binding(complexProperty(
                "styleFontVariations", PropertyValueKind.FONT_VARIATION_LIST)).createEditor();
        variationEditor.setValue(FlutterPropertyCellValue.explicit(variationValue));
        PropertyEnv variationEnvironment = PropertyEnv.create(descriptor(
                "Font variations", "Variable font axes."));
        ((ExPropertyEditor) variationEditor).attachEnv(variationEnvironment);

        onEdt(() -> {
            Component panel = variationEditor.getCustomEditor();
            JTable table = findNamed(panel, JTable.class,
                    FlutterComplexPropertyEditorComponents.FONT_VARIATION_TABLE_NAME);
            assertNotNull(findNamed(panel, JComboBox.class,
                    "flutter.fontVariations.preset"));
            table.getModel().setValueAt("700.000000000000000001", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, variationEnvironment.getState());
            table.getModel().setValueAt("0", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, variationEnvironment.getState());
            table.getModel().setValueAt("700", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    variationEnvironment.getState());
            variationEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_VALID, variationEnvironment.getState(),
                    "a valid variation table must remain valid after OK");
            PropertyValue.FontVariationListValue accepted = assertInstanceOf(
                    PropertyValue.FontVariationListValue.class,
                    ((FlutterPropertyCellValue) variationEditor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(0, BigDecimal.valueOf(700).compareTo(
                    accepted.items().getFirst().value()));
            return null;
        });
    }

    private static void assertEdgeInsetsDialogRoundTrip(
            PropertyValue initialInsets,
            String expectedMode) throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Padding", "padding"));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(initialInsets);
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Padding", "Non-negative physical or directional edge insets."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertEquals("flutter.edgeInsets.custom", panel.getName());
            JComboBox<?> mode = findNamed(
                    panel, JComboBox.class, "flutter.edgeInsets.mode");
            assertNotNull(mode);
            assertEquals(List.of(
                    "All sides",
                    "Symmetric",
                    "Physical (left/right)",
                    "Directional (start/end)"), comboLabels(mode));
            assertEquals(expectedMode, String.valueOf(mode.getSelectedItem()));

            for (String name : List.of(
                    FlutterPropertyEditorComponents.EDGE_ALL_NAME,
                    "flutter.edgeInsets.horizontal",
                    "flutter.edgeInsets.vertical",
                    "flutter.edgeInsets.left",
                    "flutter.edgeInsets.top",
                    "flutter.edgeInsets.right",
                    "flutter.edgeInsets.bottom",
                    "flutter.edgeInsets.directional.start",
                    "flutter.edgeInsets.directional.top",
                    "flutter.edgeInsets.directional.end",
                    "flutter.edgeInsets.directional.bottom")) {
                assertNotNull(findNamed(panel, JTextField.class, name), name);
            }
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(initial, editor.getValue(),
                    "opening and accepting the " + expectedMode
                    + " editor must preserve its exact typed value");
            return null;
        });
    }

    private static void assertEdgeInsetsDialogCommit(
            String modeLabel,
            Map<String, String> fieldValues,
            PropertyValue expected) throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Padding", "padding"));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                edge("16", "16", "16", "16"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Padding", "Non-negative physical or directional edge insets."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> mode = findNamed(
                    panel, JComboBox.class, "flutter.edgeInsets.mode");
            assertNotNull(mode);
            selectLabel(mode, modeLabel);
            for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
                JTextField field = findNamed(
                        panel, JTextField.class, entry.getKey());
                assertNotNull(field, entry.getKey());
                field.setText(entry.getValue());
            }

            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(initial, editor.getValue(),
                    "document events must keep the " + modeLabel + " value as a local draft");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue());
            return null;
        });
    }

    private static void assertEdgeInsetsDialogInvalid(
            String modeLabel,
            String fieldName,
            String invalidText) throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Padding", "padding"));
        PropertyEditor editor = binding.createEditor();
        FlutterPropertyCellValue initial = FlutterPropertyCellValue.explicit(
                edge("16", "16", "16", "16"));
        editor.setValue(initial);
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Padding", "Non-negative physical or directional edge insets."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JComboBox<?> mode = findNamed(
                    panel, JComboBox.class, "flutter.edgeInsets.mode");
            assertNotNull(mode);
            selectLabel(mode, modeLabel);
            JTextField field = findNamed(panel, JTextField.class, fieldName);
            assertNotNull(field, fieldName);
            field.setText(invalidText);

            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", field.getClientProperty("JComponent.outline"));
            assertTrue(field.getAccessibleContext().getAccessibleDescription()
                    .startsWith("Invalid value."));
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(initial, editor.getValue(),
                    "an invalid " + modeLabel + " draft must never commit");
            return null;
        });
    }

    private static List<String> comboLabels(JComboBox<?> combo) {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (int index = 0; index < combo.getItemCount(); index++) {
            labels.add(String.valueOf(combo.getItemAt(index)));
        }
        return List.copyOf(labels);
    }

    private static void assertRequiredConstrainedBoxExpansion(boolean expandWidth)
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.ConstrainedBox", "constraints"));
        assertFalse(binding.optional());
        PropertyValue.BoxConstraintsValue initial = new PropertyValue.BoxConstraintsValue(
                BigDecimal.ZERO, java.util.Optional.empty(),
                BigDecimal.ZERO, java.util.Optional.empty());
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initial));
        AtomicInteger commits = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Constraints", "Required ConstrainedBox constraints."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertNull(findByText(panel, JCheckBox.class,
                    "Use inherited/default value (omit argument)"),
                    "a required constructor argument must not expose omit/reset UI");
            JCheckBox unboundedWidth = findNamed(panel, JCheckBox.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_UNBOUNDED_WIDTH_NAME);
            JCheckBox expandingWidth = findNamed(panel, JCheckBox.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_EXPANDING_WIDTH_NAME);
            JCheckBox unboundedHeight = findNamed(panel, JCheckBox.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_UNBOUNDED_HEIGHT_NAME);
            JCheckBox expandingHeight = findNamed(panel, JCheckBox.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_EXPANDING_HEIGHT_NAME);
            JTextField minWidth = findNamed(panel, JTextField.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_MIN_WIDTH_NAME);
            JTextField maxWidth = findNamed(panel, JTextField.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_MAX_WIDTH_NAME);
            JTextField minHeight = findNamed(panel, JTextField.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_MIN_HEIGHT_NAME);
            JTextField maxHeight = findNamed(panel, JTextField.class,
                    FlutterContainerPropertyEditorComponents.CONSTRAINTS_MAX_HEIGHT_NAME);
            assertNotNull(unboundedWidth);
            assertNotNull(expandingWidth);
            assertNotNull(unboundedHeight);
            assertNotNull(expandingHeight);
            assertNotNull(minWidth);
            assertNotNull(maxWidth);
            assertNotNull(minHeight);
            assertNotNull(maxHeight);
            assertEquals("Expanding width",
                    expandingWidth.getAccessibleContext().getAccessibleName());
            assertEquals("Expanding height",
                    expandingHeight.getAccessibleContext().getAccessibleName());

            PropertyValue.BoxConstraintsValue expected;
            if (expandWidth) {
                expandingWidth.doClick();
                assertTrue(unboundedWidth.isSelected(),
                        "expanding width must force an infinite maximum");
                assertFalse(unboundedWidth.isEnabled());
                assertFalse(minWidth.isEnabled());
                assertFalse(maxWidth.isEnabled());
                assertTrue(expandingHeight.isEnabled());

                unboundedHeight.doClick();
                minHeight.setText("24");
                maxHeight.setText("120");
                expected = new PropertyValue.BoxConstraintsValue(
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        new PropertyValue.BoxConstraintBound.Finite(BigDecimal.valueOf(24)),
                        new PropertyValue.BoxConstraintBound.Finite(BigDecimal.valueOf(120)));
            } else {
                expandingHeight.doClick();
                assertTrue(unboundedHeight.isSelected(),
                        "expanding height must force an infinite maximum");
                assertFalse(unboundedHeight.isEnabled());
                assertFalse(minHeight.isEnabled());
                assertFalse(maxHeight.isEnabled());
                assertTrue(expandingWidth.isEnabled());

                unboundedWidth.doClick();
                minWidth.setText("10");
                maxWidth.setText("100");
                expected = new PropertyValue.BoxConstraintsValue(
                        new PropertyValue.BoxConstraintBound.Finite(BigDecimal.TEN),
                        new PropertyValue.BoxConstraintBound.Finite(BigDecimal.valueOf(100)),
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE);
            }

            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "all four bounds must remain a local draft until dialog OK");
            assertEquals(0, commits.get());
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue());
            assertEquals(1, commits.get());

            environment.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(1, commits.get(), "one editor dialog may commit only once");
            return null;
        });
    }

    @Test
    void everyCatalogBooleanUsesPaintedCheckboxAndNoValueTags()
            throws Exception {
        List<String> booleanProperties = BuiltInWidgetCatalog.getDefault()
                .definitions().stream()
                .flatMap(widget -> widget.properties().stream()
                        .filter(property -> property.acceptedKinds().equals(
                                Set.of(PropertyValueKind.BOOLEAN)))
                        .map(property -> widget.typeId().value() + "."
                                + property.name().value()))
                .toList();
        assertEquals(136, booleanProperties.size(),
                "every current built-in BOOLEAN-only property is covered");
        assertTrue(booleanProperties.contains(
                "flutter.widgets.ExcludeSemantics.excluding"));
        assertTrue(booleanProperties.contains("flutter.widgets.IgnorePointer.ignoring"));
        assertTrue(booleanProperties.contains("flutter.widgets.IgnorePointer.ignoringSemantics"));
        assertTrue(booleanProperties.contains("flutter.widgets.AbsorbPointer.absorbing"));
        assertTrue(booleanProperties.contains("flutter.widgets.AbsorbPointer.ignoringSemantics"));
        assertTrue(booleanProperties.contains("flutter.widgets.BlockSemantics.blocking"));
        assertTrue(booleanProperties.contains("flutter.widgets.ExcludeFocus.excluding"));
        assertTrue(booleanProperties.contains("flutter.widgets.ExcludeFocusTraversal.excluding"));
        assertTrue(booleanProperties.contains("flutter.widgets.TickerMode.enabled"));
        assertTrue(booleanProperties.contains("flutter.widgets.TickerMode.forceFrames"));
        assertTrue(booleanProperties.contains("flutter.widgets.DefaultTextHeightBehavior.textHeightApplyFirstAscent"));
        assertTrue(booleanProperties.contains("flutter.widgets.DefaultTextHeightBehavior.textHeightApplyLastDescent"));
        assertTrue(booleanProperties.contains("flutter.widgets.DefaultSelectionStyle.merge"));
        assertTrue(booleanProperties.contains("flutter.widgets.IconTheme.applyTextScaling"));
        assertTrue(booleanProperties.contains("flutter.widgets.IconTheme.merge"));
        for (String name : List.of("visible", "maintainState", "maintainAnimation", "maintainSize",
                "maintainSemantics", "maintainInteractivity", "maintainFocusability")) {
            assertTrue(booleanProperties.contains("flutter.widgets.Visibility." + name));
        }
        assertTrue(booleanProperties.contains(
                "flutter.widgets.SingleChildScrollView.reverse"));
        assertTrue(booleanProperties.contains(
                "flutter.widgets.SingleChildScrollView.primary"));
        assertTrue(booleanProperties.contains(
                "flutter.widgets.ColoredBox.isAntiAlias"));
        assertTrue(booleanProperties.contains("flutter.widgets.SafeArea.left"));
        assertTrue(booleanProperties.contains("flutter.widgets.SafeArea.top"));
        assertTrue(booleanProperties.contains("flutter.widgets.SafeArea.right"));
        assertTrue(booleanProperties.contains("flutter.widgets.SafeArea.bottom"));
        assertTrue(booleanProperties.contains(
                "flutter.widgets.SafeArea.maintainBottomViewPadding"));

        onEdt(() -> {
            for (String qualifiedName : booleanProperties) {
                int separator = qualifiedName.lastIndexOf('.');
                FlutterTypedPropertyEditors.Binding binding = binding(property(
                        qualifiedName.substring(0, separator),
                        qualifiedName.substring(separator + 1)));
                PropertyEditor editor = binding.createEditor();
                editor.setValue(FlutterPropertyCellValue.explicit(
                        new PropertyValue.BooleanValue(false)));

                assertEquals(FlutterTypedPropertyEditors.EditorKind.BOOLEAN,
                        binding.editorKind(), qualifiedName);
                assertNull(editor.getTags(), qualifiedName
                        + " must not advertise combo-box tags");
                assertTrue(editor.isPaintable(), qualifiedName
                        + " must use the checkbox cell renderer");
                InplaceEditor inplace = FlutterPropertyEditorComponents
                        .inplaceFactory(binding).orElseThrow().getInplaceEditor();
                inplace.connect(editor, PropertyEnv.create(descriptor(
                        qualifiedName, qualifiedName)));
                JCheckBox checkBox = assertInstanceOf(
                        JCheckBox.class, inplace.getComponent(), qualifiedName);
                assertEquals(SwingConstants.CENTER,
                        checkBox.getHorizontalAlignment(), qualifiedName
                        + " must center the checkbox horizontally");
                assertEquals(SwingConstants.CENTER,
                        checkBox.getVerticalAlignment(), qualifiedName
                        + " must center the checkbox vertically");
                inplace.clear();
                if (binding.optional()) {
                    editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT);
                    assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(), qualifiedName);
                } else {
                    assertThrows(IllegalArgumentException.class,
                            () -> editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT), qualifiedName);
                    assertThrows(IllegalArgumentException.class,
                            () -> editor.setValue(FlutterPropertyCellValue.unset()), qualifiedName);
                }
            }
            return null;
        });
    }

    @Test
    void booleanCellPainterDistinguishesUnsetUncheckedAndChecked()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "softWrap"));
        PropertyEditor editor = binding.createEditor();

        onEdt(() -> {
            int[] unset = paintBoolean(editor, FlutterPropertyCellValue.unset());
            int[] unchecked = paintBoolean(editor, FlutterPropertyCellValue.explicit(
                    new PropertyValue.BooleanValue(false)));
            int[] checked = paintBoolean(editor, FlutterPropertyCellValue.explicit(
                    new PropertyValue.BooleanValue(true)));

            assertFalse(java.util.Arrays.equals(unset, unchecked),
                    "<not set> text and an unchecked checkbox must render differently");
            assertFalse(java.util.Arrays.equals(unchecked, checked),
                    "checked and unchecked checkbox glyphs must render differently");
            return null;
        });
    }

    @Test
    void placeholderEditorsUseThemeColorAndFiniteNonNegativeNumberDomains() {
        FlutterTypedPropertyEditors.Binding color = binding(
                property("flutter.widgets.Placeholder", "color"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.THEME_COLOR,
                color.editorKind());
        assertTrue(color.optional());
        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                color.definition().acceptedKinds());

        for (String name : List.of(
                "strokeWidth", "fallbackWidth", "fallbackHeight")) {
            FlutterTypedPropertyEditors.Binding numeric = binding(
                    property("flutter.widgets.Placeholder", name));
            assertEquals(FlutterTypedPropertyEditors.EditorKind.NUMBER,
                    numeric.editorKind(), name);
            assertTrue(numeric.optional(), name);
            assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    numeric.definition().acceptedKinds(), name);

            PropertyEditor editor = numeric.createEditor();
            editor.setAsText("0");
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.IntegerValue(BigInteger.ZERO)),
                    editor.getValue(), name);
            editor.setAsText("2.5");
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.DoubleValue(new BigDecimal("2.5"))),
                    editor.getValue(), name);
            assertThrows(IllegalArgumentException.class,
                    () -> editor.setAsText("-0.01"), name);
            assertThrows(IllegalArgumentException.class,
                    () -> editor.setAsText("NaN"), name);
            assertThrows(IllegalArgumentException.class,
                    () -> editor.setAsText("Infinity"), name);
            editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT);
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(), name);
        }
    }

    @Test
    void requiredSizeEditorKeepsAValidatedLocalDraftAndNeverOffersUnset()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.SizedOverflowBox", "size"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.SIZE,
                binding.editorKind());
        assertTrue(binding.createEditor().supportsCustomEditor());
        assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty());

        PropertyEditor editor = binding.createEditor();
        PropertyValue.SizeValue initial = new PropertyValue.SizeValue(
                BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        editor.setValue(FlutterPropertyCellValue.explicit(initial));
        assertEquals("100 × 100", editor.getAsText());
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT));

        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Size", "Required finite non-negative width and height."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertSame(panel, editor.getCustomEditor(),
                    "NetBeans may ask for the active custom editor repeatedly");
            assertNull(findByText(panel, JCheckBox.class,
                    "Use inherited/default value (omit argument)"));
            JTextField width = findNamed(
                    panel,
                    JTextField.class,
                    FlutterSizePropertyEditorComponents.WIDTH_COMPONENT_NAME);
            JTextField height = findNamed(
                    panel,
                    JTextField.class,
                    FlutterSizePropertyEditorComponents.HEIGHT_COMPONENT_NAME);
            assertNotNull(width);
            assertNotNull(height);
            assertEquals("Size width",
                    width.getAccessibleContext().getAccessibleName());
            assertEquals("Size height",
                    height.getAccessibleContext().getAccessibleName());
            assertEquals("100", width.getText());
            assertEquals("100", height.getText());

            width.setText("-1");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", width.getClientProperty("JComponent.outline"));
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "invalid local typing must not replace the selected property");
            assertEquals(0, committedChanges.get());

            width.setText("1e400");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", width.getClientProperty("JComponent.outline"));
            assertTrue(width.getToolTipText().contains("finite Dart double"));
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "non-finite local typing must not replace the selected property");
            assertEquals(0, committedChanges.get());

            width.setText("0");
            height.setText("32.5");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "valid local typing remains a draft until dialog OK");
            assertEquals(0, committedChanges.get());

            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.SizeValue(
                                    BigDecimal.ZERO, new BigDecimal("32.5"))),
                    editor.getValue());
            assertEquals(1, committedChanges.get());
            return null;
        });

        PropertyEditor reopened = binding.createEditor();
        reopened.setValue(editor.getValue());
        PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor(
                "Size", "Required finite non-negative width and height."));
        ((ExPropertyEditor) reopened).attachEnv(reopenedEnvironment);
        onEdt(() -> {
            Component panel = reopened.getCustomEditor();
            JTextField width = findNamed(
                    panel,
                    JTextField.class,
                    FlutterSizePropertyEditorComponents.WIDTH_COMPONENT_NAME);
            JTextField height = findNamed(
                    panel,
                    JTextField.class,
                    FlutterSizePropertyEditorComponents.HEIGHT_COMPONENT_NAME);
            assertEquals("0", width.getText());
            assertEquals("32.5", height.getText());
            width.setText("48");
            height.setText("64");
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.SizeValue(
                                    BigDecimal.valueOf(48), BigDecimal.valueOf(64))),
                    reopened.getValue(),
                    "a reopened Size editor must accept a further edit");
            return null;
        });
    }

    @Test
    void optionalOffsetEditorPreservesUnsetAndAcceptsFiniteSignedCoordinates()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Transform", "origin"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.OFFSET,
                binding.editorKind());
        assertTrue(binding.createEditor().supportsCustomEditor());
        assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty());

        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT, editor.getAsText());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Origin", "Optional finite signed Transform origin."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertSame(panel, editor.getCustomEditor(),
                    "NetBeans may ask for the active Offset editor repeatedly");
            assertEquals("Flutter Offset editor",
                    panel.getAccessibleContext().getAccessibleName());
            JCheckBox useDefault = findNamed(
                    panel,
                    JCheckBox.class,
                    FlutterOffsetPropertyEditorComponents.USE_DEFAULT_COMPONENT_NAME);
            JTextField dx = findNamed(
                    panel,
                    JTextField.class,
                    FlutterOffsetPropertyEditorComponents.DX_COMPONENT_NAME);
            JTextField dy = findNamed(
                    panel,
                    JTextField.class,
                    FlutterOffsetPropertyEditorComponents.DY_COMPONENT_NAME);
            assertNotNull(useDefault);
            assertNotNull(dx);
            assertNotNull(dy);
            assertTrue(useDefault.isSelected());
            assertFalse(dx.isEnabled());
            assertFalse(dy.isEnabled());
            assertEquals("0", dx.getText(),
                    "unset presentation text must never leak into the Offset fields");
            assertEquals("0", dy.getText());
            assertEquals("Offset horizontal delta",
                    dx.getAccessibleContext().getAccessibleName());
            assertEquals("Offset vertical delta",
                    dy.getAccessibleContext().getAccessibleName());

            useDefault.doClick();
            assertTrue(dx.isEnabled());
            assertTrue(dy.isEnabled());
            dx.setText("-12.5");
            dy.setText("not-a-number");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", dy.getClientProperty("JComponent.outline"));
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(),
                    "invalid local typing must not replace the selected property");
            assertEquals(0, committedChanges.get());

            dy.setText("1e400");
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertTrue(dy.getToolTipText().contains("finite Dart double"));
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());

            dy.setText("7.25");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(),
                    "valid signed coordinates remain a local draft until dialog OK");
            environment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.OffsetValue(
                                    new BigDecimal("-12.5"), new BigDecimal("7.25"))),
                    editor.getValue());
            assertEquals(1, committedChanges.get());
            return null;
        });

        PropertyEditor reopened = binding.createEditor();
        reopened.setValue(editor.getValue());
        PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor(
                "Origin", "Optional finite signed Transform origin."));
        ((ExPropertyEditor) reopened).attachEnv(reopenedEnvironment);
        onEdt(() -> {
            Component panel = reopened.getCustomEditor();
            JCheckBox useDefault = findNamed(
                    panel,
                    JCheckBox.class,
                    FlutterOffsetPropertyEditorComponents.USE_DEFAULT_COMPONENT_NAME);
            JTextField dx = findNamed(
                    panel,
                    JTextField.class,
                    FlutterOffsetPropertyEditorComponents.DX_COMPONENT_NAME);
            JTextField dy = findNamed(
                    panel,
                    JTextField.class,
                    FlutterOffsetPropertyEditorComponents.DY_COMPONENT_NAME);
            assertFalse(useDefault.isSelected());
            assertEquals("-12.5", dx.getText());
            assertEquals("7.25", dy.getText());
            dx.setText("4");
            dy.setText("-8.5");
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(
                            new PropertyValue.OffsetValue(
                                    BigDecimal.valueOf(4), new BigDecimal("-8.5"))),
                    reopened.getValue(),
                    "a reopened Offset editor must accept a further signed edit");
            return null;
        });

        PropertyEditor reset = binding.createEditor();
        reset.setValue(reopened.getValue());
        PropertyEnv resetEnvironment = PropertyEnv.create(descriptor(
                "Origin", "Optional finite signed Transform origin."));
        ((ExPropertyEditor) reset).attachEnv(resetEnvironment);
        onEdt(() -> {
            JCheckBox useDefault = findNamed(
                    reset.getCustomEditor(),
                    JCheckBox.class,
                    FlutterOffsetPropertyEditorComponents.USE_DEFAULT_COMPONENT_NAME);
            useDefault.doClick();
            resetEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), reset.getValue(),
                    "the optional Offset editor must preserve explicit not-set intent");
            return null;
        });
    }

    @Test
    void clipRRectBorderRadiusEditorIsTypedTransactionalAndReopenable()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.ClipRRect", "borderRadius"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.BORDER_RADIUS,
                binding.editorKind());
        assertTrue(binding.optional());
        assertTrue(binding.createEditor().supportsCustomEditor());
        assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty());

        PropertyValue.BoxDecorationValue.Radius oneByTwo = radius("1", "2");
        PropertyValue.BoxDecorationValue.Radius threeByFour = radius("3", "4");
        PropertyValue.BoxDecorationValue.Radius fiveBySix = radius("5", "6");
        PropertyValue.BoxDecorationValue.Radius sevenByEight = radius("7", "8");
        PropertyValue.BorderRadiusValue initial = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        oneByTwo, threeByFour, fiveBySix, sevenByEight));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initial));
        assertEquals("physical corners [1×2, 3×4, 5×6, 7×8]", editor.getAsText());

        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Border radius", "ClipRRect typed border radius geometry."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertSame(panel, editor.getCustomEditor(),
                    "NetBeans may request the active BorderRadius editor repeatedly");
            assertEquals("Flutter border radius editor",
                    panel.getAccessibleContext().getAccessibleName());
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default BorderRadius.zero (omit argument)");
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            assertNotNull(useDefault);
            assertNotNull(basis);
            assertNotNull(table);
            assertFalse(useDefault.isSelected());
            assertEquals("Physical corners", basis.getSelectedItem());
            assertEquals("BorderRadius corner radii",
                    table.getAccessibleContext().getAccessibleName());
            assertTableColumn(table, 0,
                    "Top left", "Top right", "Bottom right", "Bottom left");
            assertTableColumn(table, 1, "1", "3", "5", "7");
            assertTableColumn(table, 2, "2", "4", "6", "8");

            table.setValueAt("-1", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", table.getClientProperty("JComponent.outline"));
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "an invalid radius draft must not replace the selected property");
            assertEquals(0, committedChanges.get());

            table.setValueAt("1e400", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertTrue(table.getToolTipText().contains("finite non-negative"));
            table.setValueAt("1", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());

            assertTrue(table.editCellAt(3, 2));
            JTextField activeCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            activeCell.setText("8.5");
            environment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.PhysicalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.PhysicalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("1"), geometry.topLeft().x());
            assertEquals(new BigDecimal("8.5"), geometry.bottomLeft().y(),
                    "OK must flush the active table-cell editor");
            assertEquals(1, committedChanges.get());
            return null;
        });

        PropertyEditor reopened = binding.createEditor();
        reopened.setValue(editor.getValue());
        PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Reopened ClipRRect border radius."));
        ((ExPropertyEditor) reopened).attachEnv(reopenedEnvironment);
        onEdt(() -> {
            Component panel = reopened.getCustomEditor();
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            basis.setSelectedItem("Directional corners");
            assertTableColumn(table, 0,
                    "Top start", "Top end", "Bottom end", "Bottom start");
            String[][] values = {
                {"11", "12"}, {"13", "14"}, {"15", "16"}, {"17", "18"}
            };
            for (int row = 0; row < values.length; row++) {
                table.setValueAt(values[row][0], row, 1);
                table.setValueAt(values[row][1], row, 2);
            }
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) reopened.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.DirectionalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.DirectionalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("11"), geometry.topStart().x());
            assertEquals(new BigDecimal("12"), geometry.topStart().y());
            assertEquals(new BigDecimal("13"), geometry.topEnd().x());
            assertEquals(new BigDecimal("14"), geometry.topEnd().y());
            assertEquals(new BigDecimal("15"), geometry.bottomEnd().x());
            assertEquals(new BigDecimal("16"), geometry.bottomEnd().y());
            assertEquals(new BigDecimal("17"), geometry.bottomStart().x());
            assertEquals(new BigDecimal("18"), geometry.bottomStart().y());
            return null;
        });

        PropertyEditor reset = binding.createEditor();
        reset.setValue(reopened.getValue());
        PropertyEnv resetEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Optional ClipRRect border radius."));
        ((ExPropertyEditor) reset).attachEnv(resetEnvironment);
        onEdt(() -> {
            Component panel = reset.getCustomEditor();
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default BorderRadius.zero (omit argument)");
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            useDefault.doClick();
            assertFalse(table.isEnabled());
            resetEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), reset.getValue(),
                    "the optional editor must preserve explicit default omission");
            return null;
        });

        PropertyEditor cancelled = binding.createEditor();
        cancelled.setValue(reopened.getValue());
        PropertyEnv cancelledEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Cancelled ClipRRect border radius edit."));
        ((ExPropertyEditor) cancelled).attachEnv(cancelledEnvironment);
        onEdt(() -> {
            JTable table = findNamed(
                    cancelled.getCustomEditor(),
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            table.setValueAt("99", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    cancelledEnvironment.getState());
            assertEquals(reopened.getValue(), cancelled.getValue(),
                    "closing without STATE_VALID must discard the local radius draft");
            return null;
        });
    }

    @Test
    void cardRadiusEditorIsTypedTransactionalAndReopenable()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.material.Card", "shapeRadius"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.BORDER_RADIUS,
                binding.editorKind());
        assertTrue(binding.optional());
        assertTrue(binding.createEditor().supportsCustomEditor());
        assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty());

        PropertyValue.BoxDecorationValue.Radius oneByTwo = radius("1", "2");
        PropertyValue.BoxDecorationValue.Radius threeByFour = radius("3", "4");
        PropertyValue.BoxDecorationValue.Radius fiveBySix = radius("5", "6");
        PropertyValue.BoxDecorationValue.Radius sevenByEight = radius("7", "8");
        PropertyValue.BorderRadiusValue initial = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        oneByTwo, threeByFour, fiveBySix, sevenByEight));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initial));
        assertEquals("physical corners [1×2, 3×4, 5×6, 7×8]", editor.getAsText());

        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Border radius", "Card typed border radius geometry."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertSame(panel, editor.getCustomEditor(),
                    "NetBeans may request the active BorderRadius editor repeatedly");
            assertEquals("Flutter border radius editor",
                    panel.getAccessibleContext().getAccessibleName());
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default (omit argument)");
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            assertNotNull(useDefault);
            assertNotNull(basis);
            assertNotNull(table);
            assertFalse(useDefault.isSelected());
            assertEquals("Physical corners", basis.getSelectedItem());
            assertEquals("BorderRadius corner radii",
                    table.getAccessibleContext().getAccessibleName());
            assertTableColumn(table, 0,
                    "Top left", "Top right", "Bottom right", "Bottom left");
            assertTableColumn(table, 1, "1", "3", "5", "7");
            assertTableColumn(table, 2, "2", "4", "6", "8");

            table.setValueAt("-1", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", table.getClientProperty("JComponent.outline"));
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "an invalid radius draft must not replace the selected property");
            assertEquals(0, committedChanges.get());

            table.setValueAt("1e400", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertTrue(table.getToolTipText().contains("finite non-negative"));
            table.setValueAt("1", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());

            assertTrue(table.editCellAt(3, 2));
            JTextField activeCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            activeCell.setText("8.5");
            environment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.PhysicalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.PhysicalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("1"), geometry.topLeft().x());
            assertEquals(new BigDecimal("8.5"), geometry.bottomLeft().y(),
                    "OK must flush the active table-cell editor");
            assertEquals(1, committedChanges.get());
            return null;
        });

        PropertyEditor reopened = binding.createEditor();
        reopened.setValue(editor.getValue());
        PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Reopened Card border radius."));
        ((ExPropertyEditor) reopened).attachEnv(reopenedEnvironment);
        onEdt(() -> {
            Component panel = reopened.getCustomEditor();
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            basis.setSelectedItem("Directional corners");
            assertTableColumn(table, 0,
                    "Top start", "Top end", "Bottom end", "Bottom start");
            String[][] values = {
                {"11", "12"}, {"13", "14"}, {"15", "16"}, {"17", "18"}
            };
            for (int row = 0; row < values.length; row++) {
                table.setValueAt(values[row][0], row, 1);
                table.setValueAt(values[row][1], row, 2);
            }
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) reopened.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.DirectionalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.DirectionalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("11"), geometry.topStart().x());
            assertEquals(new BigDecimal("12"), geometry.topStart().y());
            assertEquals(new BigDecimal("13"), geometry.topEnd().x());
            assertEquals(new BigDecimal("14"), geometry.topEnd().y());
            assertEquals(new BigDecimal("15"), geometry.bottomEnd().x());
            assertEquals(new BigDecimal("16"), geometry.bottomEnd().y());
            assertEquals(new BigDecimal("17"), geometry.bottomStart().x());
            assertEquals(new BigDecimal("18"), geometry.bottomStart().y());
            return null;
        });

        PropertyEditor reset = binding.createEditor();
        reset.setValue(reopened.getValue());
        PropertyEnv resetEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Optional Card border radius."));
        ((ExPropertyEditor) reset).attachEnv(resetEnvironment);
        onEdt(() -> {
            Component panel = reset.getCustomEditor();
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default (omit argument)");
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            useDefault.doClick();
            assertFalse(table.isEnabled());
            resetEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), reset.getValue(),
                    "the optional editor must preserve explicit default omission");
            return null;
        });

        PropertyEditor cancelled = binding.createEditor();
        cancelled.setValue(reopened.getValue());
        PropertyEnv cancelledEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Cancelled Card border radius edit."));
        ((ExPropertyEditor) cancelled).attachEnv(cancelledEnvironment);
        onEdt(() -> {
            JTable table = findNamed(
                    cancelled.getCustomEditor(),
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            table.setValueAt("99", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    cancelledEnvironment.getState());
            assertEquals(reopened.getValue(), cancelled.getValue(),
                    "closing without STATE_VALID must discard the local radius draft");
            return null;
        });
    }

    @Test
    void verticalDividerRadiusEditorIsTypedTransactionalAndReopenable()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.material.VerticalDivider", "radius"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.BORDER_RADIUS,
                binding.editorKind());
        assertTrue(binding.optional());
        assertTrue(binding.createEditor().supportsCustomEditor());
        assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty());

        PropertyValue.BoxDecorationValue.Radius oneByTwo = radius("1", "2");
        PropertyValue.BoxDecorationValue.Radius threeByFour = radius("3", "4");
        PropertyValue.BoxDecorationValue.Radius fiveBySix = radius("5", "6");
        PropertyValue.BoxDecorationValue.Radius sevenByEight = radius("7", "8");
        PropertyValue.BorderRadiusValue initial = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        oneByTwo, threeByFour, fiveBySix, sevenByEight));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initial));
        assertEquals("physical corners [1×2, 3×4, 5×6, 7×8]", editor.getAsText());

        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Border radius", "VerticalDivider typed border radius geometry."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertSame(panel, editor.getCustomEditor(),
                    "NetBeans may request the active BorderRadius editor repeatedly");
            assertEquals("Flutter border radius editor",
                    panel.getAccessibleContext().getAccessibleName());
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default (omit argument)");
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            assertNotNull(useDefault);
            assertNotNull(basis);
            assertNotNull(table);
            assertFalse(useDefault.isSelected());
            assertEquals("Physical corners", basis.getSelectedItem());
            assertEquals("BorderRadius corner radii",
                    table.getAccessibleContext().getAccessibleName());
            assertTableColumn(table, 0,
                    "Top left", "Top right", "Bottom right", "Bottom left");
            assertTableColumn(table, 1, "1", "3", "5", "7");
            assertTableColumn(table, 2, "2", "4", "6", "8");

            table.setValueAt("-1", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", table.getClientProperty("JComponent.outline"));
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "an invalid radius draft must not replace the selected property");
            assertEquals(0, committedChanges.get());

            table.setValueAt("1e400", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertTrue(table.getToolTipText().contains("finite non-negative"));
            table.setValueAt("1", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());

            assertTrue(table.editCellAt(3, 2));
            JTextField activeCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            activeCell.setText("8.5");
            environment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.PhysicalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.PhysicalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("1"), geometry.topLeft().x());
            assertEquals(new BigDecimal("8.5"), geometry.bottomLeft().y(),
                    "OK must flush the active table-cell editor");
            assertEquals(1, committedChanges.get());
            return null;
        });

        PropertyEditor reopened = binding.createEditor();
        reopened.setValue(editor.getValue());
        PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Reopened VerticalDivider border radius."));
        ((ExPropertyEditor) reopened).attachEnv(reopenedEnvironment);
        onEdt(() -> {
            Component panel = reopened.getCustomEditor();
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            basis.setSelectedItem("Directional corners");
            assertTableColumn(table, 0,
                    "Top start", "Top end", "Bottom end", "Bottom start");
            String[][] values = {
                {"11", "12"}, {"13", "14"}, {"15", "16"}, {"17", "18"}
            };
            for (int row = 0; row < values.length; row++) {
                table.setValueAt(values[row][0], row, 1);
                table.setValueAt(values[row][1], row, 2);
            }
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) reopened.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.DirectionalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.DirectionalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("11"), geometry.topStart().x());
            assertEquals(new BigDecimal("12"), geometry.topStart().y());
            assertEquals(new BigDecimal("13"), geometry.topEnd().x());
            assertEquals(new BigDecimal("14"), geometry.topEnd().y());
            assertEquals(new BigDecimal("15"), geometry.bottomEnd().x());
            assertEquals(new BigDecimal("16"), geometry.bottomEnd().y());
            assertEquals(new BigDecimal("17"), geometry.bottomStart().x());
            assertEquals(new BigDecimal("18"), geometry.bottomStart().y());
            return null;
        });

        PropertyEditor reset = binding.createEditor();
        reset.setValue(reopened.getValue());
        PropertyEnv resetEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Optional VerticalDivider border radius."));
        ((ExPropertyEditor) reset).attachEnv(resetEnvironment);
        onEdt(() -> {
            Component panel = reset.getCustomEditor();
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default (omit argument)");
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            useDefault.doClick();
            assertFalse(table.isEnabled());
            resetEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), reset.getValue(),
                    "the optional editor must preserve explicit default omission");
            return null;
        });

        PropertyEditor cancelled = binding.createEditor();
        cancelled.setValue(reopened.getValue());
        PropertyEnv cancelledEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Cancelled VerticalDivider border radius edit."));
        ((ExPropertyEditor) cancelled).attachEnv(cancelledEnvironment);
        onEdt(() -> {
            JTable table = findNamed(
                    cancelled.getCustomEditor(),
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            table.setValueAt("99", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    cancelledEnvironment.getState());
            assertEquals(reopened.getValue(), cancelled.getValue(),
                    "closing without STATE_VALID must discard the local radius draft");
            return null;
        });
    }

    @Test
    void dividerRadiusEditorIsTypedTransactionalAndReopenable()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.material.Divider", "radius"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.BORDER_RADIUS,
                binding.editorKind());
        assertTrue(binding.optional());
        assertTrue(binding.createEditor().supportsCustomEditor());
        assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty());

        PropertyValue.BoxDecorationValue.Radius oneByTwo = radius("1", "2");
        PropertyValue.BoxDecorationValue.Radius threeByFour = radius("3", "4");
        PropertyValue.BoxDecorationValue.Radius fiveBySix = radius("5", "6");
        PropertyValue.BoxDecorationValue.Radius sevenByEight = radius("7", "8");
        PropertyValue.BorderRadiusValue initial = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        oneByTwo, threeByFour, fiveBySix, sevenByEight));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initial));
        assertEquals("physical corners [1×2, 3×4, 5×6, 7×8]", editor.getAsText());

        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Border radius", "Divider typed border radius geometry."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertSame(panel, editor.getCustomEditor(),
                    "NetBeans may request the active BorderRadius editor repeatedly");
            assertEquals("Flutter border radius editor",
                    panel.getAccessibleContext().getAccessibleName());
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default (omit argument)");
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            assertNotNull(useDefault);
            assertNotNull(basis);
            assertNotNull(table);
            assertFalse(useDefault.isSelected());
            assertEquals("Physical corners", basis.getSelectedItem());
            assertEquals("BorderRadius corner radii",
                    table.getAccessibleContext().getAccessibleName());
            assertTableColumn(table, 0,
                    "Top left", "Top right", "Bottom right", "Bottom left");
            assertTableColumn(table, 1, "1", "3", "5", "7");
            assertTableColumn(table, 2, "2", "4", "6", "8");

            table.setValueAt("-1", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", table.getClientProperty("JComponent.outline"));
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "an invalid radius draft must not replace the selected property");
            assertEquals(0, committedChanges.get());

            table.setValueAt("1e400", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertTrue(table.getToolTipText().contains("finite non-negative"));
            table.setValueAt("1", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());

            assertTrue(table.editCellAt(3, 2));
            JTextField activeCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            activeCell.setText("8.5");
            environment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.PhysicalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.PhysicalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("1"), geometry.topLeft().x());
            assertEquals(new BigDecimal("8.5"), geometry.bottomLeft().y(),
                    "OK must flush the active table-cell editor");
            assertEquals(1, committedChanges.get());
            return null;
        });

        PropertyEditor reopened = binding.createEditor();
        reopened.setValue(editor.getValue());
        PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Reopened Divider border radius."));
        ((ExPropertyEditor) reopened).attachEnv(reopenedEnvironment);
        onEdt(() -> {
            Component panel = reopened.getCustomEditor();
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            basis.setSelectedItem("Directional corners");
            assertTableColumn(table, 0,
                    "Top start", "Top end", "Bottom end", "Bottom start");
            String[][] values = {
                {"11", "12"}, {"13", "14"}, {"15", "16"}, {"17", "18"}
            };
            for (int row = 0; row < values.length; row++) {
                table.setValueAt(values[row][0], row, 1);
                table.setValueAt(values[row][1], row, 2);
            }
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) reopened.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.DirectionalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.DirectionalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("11"), geometry.topStart().x());
            assertEquals(new BigDecimal("12"), geometry.topStart().y());
            assertEquals(new BigDecimal("13"), geometry.topEnd().x());
            assertEquals(new BigDecimal("14"), geometry.topEnd().y());
            assertEquals(new BigDecimal("15"), geometry.bottomEnd().x());
            assertEquals(new BigDecimal("16"), geometry.bottomEnd().y());
            assertEquals(new BigDecimal("17"), geometry.bottomStart().x());
            assertEquals(new BigDecimal("18"), geometry.bottomStart().y());
            return null;
        });

        PropertyEditor reset = binding.createEditor();
        reset.setValue(reopened.getValue());
        PropertyEnv resetEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Optional Divider border radius."));
        ((ExPropertyEditor) reset).attachEnv(resetEnvironment);
        onEdt(() -> {
            Component panel = reset.getCustomEditor();
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default (omit argument)");
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            useDefault.doClick();
            assertFalse(table.isEnabled());
            resetEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), reset.getValue(),
                    "the optional editor must preserve explicit default omission");
            return null;
        });

        PropertyEditor cancelled = binding.createEditor();
        cancelled.setValue(reopened.getValue());
        PropertyEnv cancelledEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Cancelled Divider border radius edit."));
        ((ExPropertyEditor) cancelled).attachEnv(cancelledEnvironment);
        onEdt(() -> {
            JTable table = findNamed(
                    cancelled.getCustomEditor(),
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            table.setValueAt("99", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    cancelledEnvironment.getState());
            assertEquals(reopened.getValue(), cancelled.getValue(),
                    "closing without STATE_VALID must discard the local radius draft");
            return null;
        });
    }

    @Test
    void clipRSuperellipseBorderRadiusEditorIsTypedTransactionalAndReopenable()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.ClipRSuperellipse", "borderRadius"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.BORDER_RADIUS,
                binding.editorKind());
        assertTrue(binding.optional());
        assertTrue(binding.createEditor().supportsCustomEditor());
        assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty());

        PropertyValue.BoxDecorationValue.Radius oneByTwo = radius("1", "2");
        PropertyValue.BoxDecorationValue.Radius threeByFour = radius("3", "4");
        PropertyValue.BoxDecorationValue.Radius fiveBySix = radius("5", "6");
        PropertyValue.BoxDecorationValue.Radius sevenByEight = radius("7", "8");
        PropertyValue.BorderRadiusValue initial = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        oneByTwo, threeByFour, fiveBySix, sevenByEight));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initial));
        assertEquals("physical corners [1×2, 3×4, 5×6, 7×8]", editor.getAsText());

        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Border radius", "ClipRSuperellipse typed border radius geometry."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertSame(panel, editor.getCustomEditor(),
                    "NetBeans may request the active BorderRadius editor repeatedly");
            assertEquals("Flutter border radius editor",
                    panel.getAccessibleContext().getAccessibleName());
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default BorderRadius.zero (omit argument)");
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            assertNotNull(useDefault);
            assertNotNull(basis);
            assertNotNull(table);
            assertFalse(useDefault.isSelected());
            assertEquals("Physical corners", basis.getSelectedItem());
            assertEquals("BorderRadius corner radii",
                    table.getAccessibleContext().getAccessibleName());
            assertTableColumn(table, 0,
                    "Top left", "Top right", "Bottom right", "Bottom left");
            assertTableColumn(table, 1, "1", "3", "5", "7");
            assertTableColumn(table, 2, "2", "4", "6", "8");

            table.setValueAt("-1", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", table.getClientProperty("JComponent.outline"));
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "an invalid radius draft must not replace the selected property");
            assertEquals(0, committedChanges.get());

            table.setValueAt("1e400", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertTrue(table.getToolTipText().contains("finite non-negative"));
            table.setValueAt("1", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());

            assertTrue(table.editCellAt(3, 2));
            JTextField activeCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            activeCell.setText("8.5");
            environment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.PhysicalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.PhysicalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("1"), geometry.topLeft().x());
            assertEquals(new BigDecimal("8.5"), geometry.bottomLeft().y(),
                    "OK must flush the active table-cell editor");
            assertEquals(1, committedChanges.get());
            return null;
        });

        PropertyEditor reopened = binding.createEditor();
        reopened.setValue(editor.getValue());
        PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Reopened ClipRSuperellipse border radius."));
        ((ExPropertyEditor) reopened).attachEnv(reopenedEnvironment);
        onEdt(() -> {
            Component panel = reopened.getCustomEditor();
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            basis.setSelectedItem("Directional corners");
            assertTableColumn(table, 0,
                    "Top start", "Top end", "Bottom end", "Bottom start");
            String[][] values = {
                {"11", "12"}, {"13", "14"}, {"15", "16"}, {"17", "18"}
            };
            for (int row = 0; row < values.length; row++) {
                table.setValueAt(values[row][0], row, 1);
                table.setValueAt(values[row][1], row, 2);
            }
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) reopened.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.DirectionalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.DirectionalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("11"), geometry.topStart().x());
            assertEquals(new BigDecimal("12"), geometry.topStart().y());
            assertEquals(new BigDecimal("13"), geometry.topEnd().x());
            assertEquals(new BigDecimal("14"), geometry.topEnd().y());
            assertEquals(new BigDecimal("15"), geometry.bottomEnd().x());
            assertEquals(new BigDecimal("16"), geometry.bottomEnd().y());
            assertEquals(new BigDecimal("17"), geometry.bottomStart().x());
            assertEquals(new BigDecimal("18"), geometry.bottomStart().y());
            return null;
        });

        PropertyEditor reset = binding.createEditor();
        reset.setValue(reopened.getValue());
        PropertyEnv resetEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Optional ClipRSuperellipse border radius."));
        ((ExPropertyEditor) reset).attachEnv(resetEnvironment);
        onEdt(() -> {
            Component panel = reset.getCustomEditor();
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default BorderRadius.zero (omit argument)");
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            useDefault.doClick();
            assertFalse(table.isEnabled());
            resetEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), reset.getValue(),
                    "the optional editor must preserve explicit default omission");
            return null;
        });

        PropertyEditor cancelled = binding.createEditor();
        cancelled.setValue(reopened.getValue());
        PropertyEnv cancelledEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Cancelled ClipRSuperellipse border radius edit."));
        ((ExPropertyEditor) cancelled).attachEnv(cancelledEnvironment);
        onEdt(() -> {
            JTable table = findNamed(
                    cancelled.getCustomEditor(),
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            table.setValueAt("99", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    cancelledEnvironment.getState());
            assertEquals(reopened.getValue(), cancelled.getValue(),
                    "closing without STATE_VALID must discard the local radius draft");
            return null;
        });
    }

    @Test
    void physicalModelBorderRadiusEditorIsTypedTransactionalAndReopenable()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.PhysicalModel", "borderRadius"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.BORDER_RADIUS,
                binding.editorKind());
        assertTrue(binding.optional());
        assertFalse(binding.directionalBorderRadiusAllowed());
        assertTrue(binding(property("flutter.widgets.ClipRSuperellipse", "borderRadius"))
                .directionalBorderRadiusAllowed());
        PropertyValue.BoxDecorationValue.Radius testRadius = radius("1", "2");
        assertThrows(IllegalArgumentException.class, () -> binding.validate(
                FlutterPropertyCellValue.explicit(new PropertyValue.BorderRadiusValue(
                        new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                                testRadius, testRadius, testRadius, testRadius)))));
        assertTrue(binding.createEditor().supportsCustomEditor());
        assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty());

        PropertyValue.BoxDecorationValue.Radius oneByTwo = radius("1", "2");
        PropertyValue.BoxDecorationValue.Radius threeByFour = radius("3", "4");
        PropertyValue.BoxDecorationValue.Radius fiveBySix = radius("5", "6");
        PropertyValue.BoxDecorationValue.Radius sevenByEight = radius("7", "8");
        PropertyValue.BorderRadiusValue initial = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        oneByTwo, threeByFour, fiveBySix, sevenByEight));
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(initial));
        assertEquals("physical corners [1×2, 3×4, 5×6, 7×8]", editor.getAsText());

        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Border radius", "PhysicalModel typed border radius geometry."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertSame(panel, editor.getCustomEditor(),
                    "NetBeans may request the active BorderRadius editor repeatedly");
            assertEquals("Flutter border radius editor",
                    panel.getAccessibleContext().getAccessibleName());
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default (omit argument)");
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            assertNotNull(useDefault);
            assertNotNull(basis);
            assertNotNull(table);
            assertFalse(useDefault.isSelected());
            assertEquals("Physical corners", basis.getSelectedItem());
            assertEquals("BorderRadius corner radii",
                    table.getAccessibleContext().getAccessibleName());
            assertTableColumn(table, 0,
                    "Top left", "Top right", "Bottom right", "Bottom left");
            assertTableColumn(table, 1, "1", "3", "5", "7");
            assertTableColumn(table, 2, "2", "4", "6", "8");

            table.setValueAt("-1", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertEquals("error", table.getClientProperty("JComponent.outline"));
            assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue(),
                    "an invalid radius draft must not replace the selected property");
            assertEquals(0, committedChanges.get());

            table.setValueAt("1e400", 0, 1);
            assertEquals(PropertyEnv.STATE_INVALID, environment.getState());
            assertTrue(table.getToolTipText().contains("finite non-negative"));
            table.setValueAt("1", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());

            assertTrue(table.editCellAt(3, 2));
            JTextField activeCell = assertInstanceOf(
                    JTextField.class, table.getEditorComponent());
            activeCell.setText("8.5");
            environment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.PhysicalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.PhysicalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("1"), geometry.topLeft().x());
            assertEquals(new BigDecimal("8.5"), geometry.bottomLeft().y(),
                    "OK must flush the active table-cell editor");
            assertEquals(1, committedChanges.get());
            return null;
        });

        PropertyEditor reopened = binding.createEditor();
        reopened.setValue(editor.getValue());
        PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Reopened PhysicalModel border radius."));
        ((ExPropertyEditor) reopened).attachEnv(reopenedEnvironment);
        onEdt(() -> {
            Component panel = reopened.getCustomEditor();
            JComboBox<?> basis = findNamed(
                    panel,
                    JComboBox.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            assertEquals(1, basis.getItemCount());
            assertEquals("Physical corners", basis.getItemAt(0));
            basis.setSelectedItem("Directional corners");
            assertEquals("Physical corners", basis.getSelectedItem(),
                    "a disallowed directional selection cannot corrupt the radius draft");
            assertTableColumn(table, 0,
                    "Top left", "Top right", "Bottom right", "Bottom left");
            String[][] values = {
                {"11", "12"}, {"13", "14"}, {"15", "16"}, {"17", "18"}
            };
            for (int row = 0; row < values.length; row++) {
                table.setValueAt(values[row][0], row, 1);
                table.setValueAt(values[row][1], row, 2);
            }
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID);
            PropertyValue.BorderRadiusValue committed = assertInstanceOf(
                    PropertyValue.BorderRadiusValue.class,
                    ((FlutterPropertyCellValue) reopened.getValue())
                            .explicitValue().orElseThrow());
            PropertyValue.BoxDecorationValue.PhysicalBorderRadius geometry =
                    assertInstanceOf(
                            PropertyValue.BoxDecorationValue.PhysicalBorderRadius.class,
                            committed.geometry());
            assertEquals(new BigDecimal("11"), geometry.topLeft().x());
            assertEquals(new BigDecimal("12"), geometry.topLeft().y());
            assertEquals(new BigDecimal("13"), geometry.topRight().x());
            assertEquals(new BigDecimal("14"), geometry.topRight().y());
            assertEquals(new BigDecimal("15"), geometry.bottomRight().x());
            assertEquals(new BigDecimal("16"), geometry.bottomRight().y());
            assertEquals(new BigDecimal("17"), geometry.bottomLeft().x());
            assertEquals(new BigDecimal("18"), geometry.bottomLeft().y());
            return null;
        });

        PropertyEditor reset = binding.createEditor();
        reset.setValue(reopened.getValue());
        PropertyEnv resetEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Optional PhysicalModel border radius."));
        ((ExPropertyEditor) reset).attachEnv(resetEnvironment);
        onEdt(() -> {
            Component panel = reset.getCustomEditor();
            JCheckBox useDefault = findByText(panel, JCheckBox.class,
                    "Use Flutter default (omit argument)");
            JTable table = findNamed(
                    panel,
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            useDefault.doClick();
            assertFalse(table.isEnabled());
            resetEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), reset.getValue(),
                    "the optional editor must preserve explicit default omission");
            return null;
        });

        PropertyEditor cancelled = binding.createEditor();
        cancelled.setValue(reopened.getValue());
        PropertyEnv cancelledEnvironment = PropertyEnv.create(descriptor(
                "Border radius", "Cancelled PhysicalModel border radius edit."));
        ((ExPropertyEditor) cancelled).attachEnv(cancelledEnvironment);
        onEdt(() -> {
            JTable table = findNamed(
                    cancelled.getCustomEditor(),
                    JTable.class,
                    FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);
            table.setValueAt("99", 0, 1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    cancelledEnvironment.getState());
            assertEquals(reopened.getValue(), cancelled.getValue(),
                    "closing without STATE_VALID must discard the local radius draft");
            return null;
        });
    }

    @Test
    void clipRectAndClipOvalClippersReuseTheGenericRectReferenceEditor()
            throws Exception {
        for (String widgetType : List.of(
                "flutter.widgets.ClipRect", "flutter.widgets.ClipOval")) {
            FlutterTypedPropertyEditors.Binding binding = binding(
                    property(widgetType, "clipper"));
            assertEquals(FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE,
                    binding.editorKind(), widgetType);
            assertTrue(binding.optional(), widgetType);
            assertTrue(binding.createEditor().supportsCustomEditor(), widgetType);
            assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty(),
                    widgetType);

            PropertyEditor editor = binding.createEditor();
            editor.setValue(FlutterPropertyCellValue.unset());
            PropertyEnv environment = PropertyEnv.create(descriptor(
                    "Clipper", widgetType + " project clipper."));
            ((ExPropertyEditor) editor).attachEnv(environment);
            onEdt(() -> {
                Component panel = editor.getCustomEditor();
                String description = panel.getAccessibleContext()
                        .getAccessibleDescription();
                assertTrue(description.contains("CustomClipper<Rect>"), widgetType);
                assertTrue(description.contains("Dart analyzer"), widgetType);
                assertTrue(description.contains("isolated Canvas"), widgetType);
                assertTrue(description.contains("preview-unavailable"), widgetType);
                assertFalse(description.contains("borderRadius"),
                        "the generic Rect editor must not mention ClipRRect geometry");

                JCheckBox useDefault = findNamed(
                        panel, JCheckBox.class,
                        FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME);
                JTextField rootSymbol = findNamed(
                        panel, JTextField.class,
                        FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
                useDefault.doClick();
                rootSymbol.setText("_rectClipper");
                assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                        environment.getState());
                environment.setState(PropertyEnv.STATE_VALID);
                PropertyValue.DartObjectReferenceValue committed = assertInstanceOf(
                        PropertyValue.DartObjectReferenceValue.class,
                        ((FlutterPropertyCellValue) editor.getValue())
                                .explicitValue().orElseThrow());
                assertEquals("_rectClipper", committed.rootSymbol());
                assertEquals(PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        committed.access());
                return null;
            });
        }
    }

    @Test
    void clipRRectClipperEditorIsTypedTransactionalAccessibleAndReopenable()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.ClipRRect", "clipper"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE,
                binding.editorKind());
        assertTrue(binding.optional());
        assertTrue(binding.createEditor().supportsCustomEditor());
        assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty());

        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Clipper", "Project-declared CustomClipper<RRect>."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        PropertyValue.DartObjectReferenceValue currentLibraryReference = onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertSame(panel, editor.getCustomEditor(),
                    "NetBeans may request the active Dart-reference editor repeatedly");
            JCheckBox useDefault = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME);
            JComboBox<?> scope = findNamed(
                    panel, JComboBox.class,
                    FlutterDartObjectReferenceEditorComponent.SCOPE_NAME);
            JTextField libraryUri = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME);
            JTextField rootSymbol = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            JTextField member = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.MEMBER_NAME);
            JComboBox<?> access = findNamed(
                    panel, JComboBox.class,
                    FlutterDartObjectReferenceEditorComponent.ACCESS_NAME);
            JCheckBox constant = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.CONSTANT_NAME);
            JLabel preview = findNamed(
                    panel, JLabel.class,
                    FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME);

            assertNotNull(useDefault);
            assertNotNull(scope);
            assertNotNull(libraryUri);
            assertNotNull(rootSymbol);
            assertNotNull(member);
            assertNotNull(access);
            assertNotNull(constant);
            assertNotNull(preview);
            assertTrue(useDefault.isSelected());
            assertEquals(FlutterDartObjectReferenceEditorComponent.CURRENT_LIBRARY_TEXT,
                    String.valueOf(scope.getSelectedItem()));
            assertFalse(scope.isEnabled());
            assertFalse(libraryUri.isEnabled());
            assertFalse(rootSymbol.isEnabled());
            assertFalse(member.isEnabled());
            assertFalse(access.isEnabled());
            assertFalse(constant.isEnabled());
            assertTrue(preview.getText().contains("Flutter default null"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("CustomClipper<RRect>"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("isolated Canvas"));
            assertAccessibleNameContains(rootSymbol, "dart", "root", "symbol");
            assertAccessibleNameContains(preview, "clipper", "preview");

            useDefault.doClick();
            assertTrue(scope.isEnabled());
            assertFalse(libraryUri.isEnabled(),
                    "a current-library reference must not accept an import URI");
            assertTrue(rootSymbol.isEnabled());
            assertTrue(member.isEnabled());
            assertTrue(access.isEnabled());
            assertFalse(constant.isEnabled(),
                    "const applies only to an invocation");
            rootSymbol.setText("_localClipper");
            assertTrue(member.getText().isEmpty(),
                    "the optional member must be allowed to remain absent");
            assertEquals(FlutterDartObjectReferenceEditorComponent.EXISTING_VALUE_TEXT,
                    String.valueOf(access.getSelectedItem()));
            assertTrue(preview.getText().contains("_localClipper"));
            assertTrue(preview.getText().contains("current Dart library"));
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.DartObjectReferenceValue committed = assertInstanceOf(
                    PropertyValue.DartObjectReferenceValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(Optional.empty(), committed.libraryUri());
            assertEquals("_localClipper", committed.rootSymbol());
            assertEquals(Optional.empty(), committed.member());
            assertEquals(PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    committed.access());
            assertEquals(Optional.empty(), committed.constant());
            assertEquals(1, committedChanges.get());
            return committed;
        });

        PropertyEditor reopened = binding.createEditor();
        reopened.setValue(FlutterPropertyCellValue.explicit(currentLibraryReference));
        PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor(
                "Clipper", "Reopened project-declared CustomClipper<RRect>."));
        ((ExPropertyEditor) reopened).attachEnv(reopenedEnvironment);
        PropertyValue.DartObjectReferenceValue importedInvocation = onEdt(() -> {
            Component panel = reopened.getCustomEditor();
            JCheckBox useDefault = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME);
            JComboBox<?> scope = findNamed(
                    panel, JComboBox.class,
                    FlutterDartObjectReferenceEditorComponent.SCOPE_NAME);
            JTextField libraryUri = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME);
            JTextField rootSymbol = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            JTextField member = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.MEMBER_NAME);
            JComboBox<?> access = findNamed(
                    panel, JComboBox.class,
                    FlutterDartObjectReferenceEditorComponent.ACCESS_NAME);
            JCheckBox constant = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.CONSTANT_NAME);
            JLabel preview = findNamed(
                    panel, JLabel.class,
                    FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME);

            assertFalse(useDefault.isSelected());
            assertEquals("_localClipper", rootSymbol.getText());
            assertTrue(member.getText().isEmpty());
            scope.setSelectedIndex(1);
            assertEquals(FlutterDartObjectReferenceEditorComponent.IMPORTED_LIBRARY_TEXT,
                    String.valueOf(scope.getSelectedItem()));
            assertTrue(libraryUri.isEnabled());
            libraryUri.setText("lib/clippers.dart");
            rootSymbol.setText("RoundedClipper");
            member.setText("compact");
            access.setSelectedIndex(1);
            assertEquals(FlutterDartObjectReferenceEditorComponent.INVOCATION_TEXT,
                    String.valueOf(access.getSelectedItem()));
            assertTrue(constant.isEnabled());
            constant.doClick();
            assertEquals(PropertyEnv.STATE_INVALID, reopenedEnvironment.getState());
            assertEquals(
                    FlutterPropertyCellValue.explicit(currentLibraryReference),
                    reopened.getValue(),
                    "an invalid package URI must not replace the committed reference");

            libraryUri.setText("package:app/clippers.dart");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    reopenedEnvironment.getState());
            rootSymbol.requestFocusInWindow();
            rootSymbol.setText("RoundedClipperFactory");
            assertTrue(preview.getText().contains(
                    "const RoundedClipperFactory.compact()"));
            assertTrue(preview.getText().contains("package:app/clippers.dart"));
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.DartObjectReferenceValue committed = assertInstanceOf(
                    PropertyValue.DartObjectReferenceValue.class,
                    ((FlutterPropertyCellValue) reopened.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(Optional.of("package:app/clippers.dart"),
                    committed.libraryUri());
            assertEquals("RoundedClipperFactory", committed.rootSymbol(),
                    "OK must commit the text field that still owns active focus");
            assertEquals(Optional.of("compact"), committed.member());
            assertEquals(
                    PropertyValue.DartObjectReferenceValue.Access
                            .ZERO_ARGUMENT_INVOCATION,
                    committed.access());
            assertEquals(Optional.of(true), committed.constant());
            return committed;
        });

        PropertyEditor reset = binding.createEditor();
        reset.setValue(FlutterPropertyCellValue.explicit(importedInvocation));
        PropertyEnv resetEnvironment = PropertyEnv.create(descriptor(
                "Clipper", "Reset optional project clipper."));
        ((ExPropertyEditor) reset).attachEnv(resetEnvironment);
        onEdt(() -> {
            Component panel = reset.getCustomEditor();
            JCheckBox useDefault = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME);
            JTextField rootSymbol = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            useDefault.doClick();
            assertFalse(rootSymbol.isEnabled());
            resetEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), reset.getValue());
            return null;
        });

        PropertyEditor cancelled = binding.createEditor();
        cancelled.setValue(FlutterPropertyCellValue.explicit(importedInvocation));
        PropertyEnv cancelledEnvironment = PropertyEnv.create(descriptor(
                "Clipper", "Cancelled project clipper edit."));
        ((ExPropertyEditor) cancelled).attachEnv(cancelledEnvironment);
        onEdt(() -> {
            JTextField rootSymbol = findNamed(
                    cancelled.getCustomEditor(), JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            rootSymbol.setText("DiscardedClipper");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    cancelledEnvironment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(importedInvocation),
                    cancelled.getValue(),
                    "closing without STATE_VALID must discard the local reference draft");
            return null;
        });
    }

    @Test
    void clipRSuperellipseClipperEditorIsTypedTransactionalAccessibleAndReopenable()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.ClipRSuperellipse", "clipper"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE,
                binding.editorKind());
        assertTrue(binding.optional());
        assertTrue(binding.createEditor().supportsCustomEditor());
        assertTrue(FlutterPropertyEditorComponents.inplaceFactory(binding).isEmpty());

        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Clipper", "Project-declared CustomClipper<RSuperellipse>."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        AtomicInteger committedChanges = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> committedChanges.incrementAndGet());

        PropertyValue.DartObjectReferenceValue currentLibraryReference = onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertSame(panel, editor.getCustomEditor(),
                    "NetBeans may request the active Dart-reference editor repeatedly");
            JCheckBox useDefault = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME);
            JComboBox<?> scope = findNamed(
                    panel, JComboBox.class,
                    FlutterDartObjectReferenceEditorComponent.SCOPE_NAME);
            JTextField libraryUri = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME);
            JTextField rootSymbol = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            JTextField member = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.MEMBER_NAME);
            JComboBox<?> access = findNamed(
                    panel, JComboBox.class,
                    FlutterDartObjectReferenceEditorComponent.ACCESS_NAME);
            JCheckBox constant = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.CONSTANT_NAME);
            JLabel preview = findNamed(
                    panel, JLabel.class,
                    FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME);

            assertNotNull(useDefault);
            assertNotNull(scope);
            assertNotNull(libraryUri);
            assertNotNull(rootSymbol);
            assertNotNull(member);
            assertNotNull(access);
            assertNotNull(constant);
            assertNotNull(preview);
            assertTrue(useDefault.isSelected());
            assertEquals(FlutterDartObjectReferenceEditorComponent.CURRENT_LIBRARY_TEXT,
                    String.valueOf(scope.getSelectedItem()));
            assertFalse(scope.isEnabled());
            assertFalse(libraryUri.isEnabled());
            assertFalse(rootSymbol.isEnabled());
            assertFalse(member.isEnabled());
            assertFalse(access.isEnabled());
            assertFalse(constant.isEnabled());
            assertTrue(preview.getText().contains("Flutter default null"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("CustomClipper<RSuperellipse>"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("ClipRSuperellipse.borderRadius"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("isolated Canvas"));
            assertAccessibleNameContains(rootSymbol, "dart", "root", "symbol");
            assertAccessibleNameContains(preview, "clipper", "preview");

            useDefault.doClick();
            assertTrue(scope.isEnabled());
            assertFalse(libraryUri.isEnabled(),
                    "a current-library reference must not accept an import URI");
            assertTrue(rootSymbol.isEnabled());
            assertTrue(member.isEnabled());
            assertTrue(access.isEnabled());
            assertFalse(constant.isEnabled(),
                    "const applies only to an invocation");
            rootSymbol.setText("_localClipper");
            assertTrue(member.getText().isEmpty(),
                    "the optional member must be allowed to remain absent");
            assertEquals(FlutterDartObjectReferenceEditorComponent.EXISTING_VALUE_TEXT,
                    String.valueOf(access.getSelectedItem()));
            assertTrue(preview.getText().contains("_localClipper"));
            assertTrue(preview.getText().contains("current Dart library"));
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.DartObjectReferenceValue committed = assertInstanceOf(
                    PropertyValue.DartObjectReferenceValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(Optional.empty(), committed.libraryUri());
            assertEquals("_localClipper", committed.rootSymbol());
            assertEquals(Optional.empty(), committed.member());
            assertEquals(PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    committed.access());
            assertEquals(Optional.empty(), committed.constant());
            assertEquals(1, committedChanges.get());
            return committed;
        });

        PropertyEditor reopened = binding.createEditor();
        reopened.setValue(FlutterPropertyCellValue.explicit(currentLibraryReference));
        PropertyEnv reopenedEnvironment = PropertyEnv.create(descriptor(
                "Clipper", "Reopened project-declared CustomClipper<RSuperellipse>."));
        ((ExPropertyEditor) reopened).attachEnv(reopenedEnvironment);
        PropertyValue.DartObjectReferenceValue importedInvocation = onEdt(() -> {
            Component panel = reopened.getCustomEditor();
            JCheckBox useDefault = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME);
            JComboBox<?> scope = findNamed(
                    panel, JComboBox.class,
                    FlutterDartObjectReferenceEditorComponent.SCOPE_NAME);
            JTextField libraryUri = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME);
            JTextField rootSymbol = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            JTextField member = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.MEMBER_NAME);
            JComboBox<?> access = findNamed(
                    panel, JComboBox.class,
                    FlutterDartObjectReferenceEditorComponent.ACCESS_NAME);
            JCheckBox constant = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.CONSTANT_NAME);
            JLabel preview = findNamed(
                    panel, JLabel.class,
                    FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME);

            assertFalse(useDefault.isSelected());
            assertEquals("_localClipper", rootSymbol.getText());
            assertTrue(member.getText().isEmpty());
            scope.setSelectedIndex(1);
            assertEquals(FlutterDartObjectReferenceEditorComponent.IMPORTED_LIBRARY_TEXT,
                    String.valueOf(scope.getSelectedItem()));
            assertTrue(libraryUri.isEnabled());
            libraryUri.setText("lib/clippers.dart");
            rootSymbol.setText("RoundedClipper");
            member.setText("compact");
            access.setSelectedIndex(1);
            assertEquals(FlutterDartObjectReferenceEditorComponent.INVOCATION_TEXT,
                    String.valueOf(access.getSelectedItem()));
            assertTrue(constant.isEnabled());
            constant.doClick();
            assertEquals(PropertyEnv.STATE_INVALID, reopenedEnvironment.getState());
            assertEquals(
                    FlutterPropertyCellValue.explicit(currentLibraryReference),
                    reopened.getValue(),
                    "an invalid package URI must not replace the committed reference");

            libraryUri.setText("package:app/clippers.dart");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    reopenedEnvironment.getState());
            rootSymbol.requestFocusInWindow();
            rootSymbol.setText("RoundedClipperFactory");
            assertTrue(preview.getText().contains(
                    "const RoundedClipperFactory.compact()"));
            assertTrue(preview.getText().contains("package:app/clippers.dart"));
            reopenedEnvironment.setState(PropertyEnv.STATE_VALID);

            PropertyValue.DartObjectReferenceValue committed = assertInstanceOf(
                    PropertyValue.DartObjectReferenceValue.class,
                    ((FlutterPropertyCellValue) reopened.getValue())
                            .explicitValue().orElseThrow());
            assertEquals(Optional.of("package:app/clippers.dart"),
                    committed.libraryUri());
            assertEquals("RoundedClipperFactory", committed.rootSymbol(),
                    "OK must commit the text field that still owns active focus");
            assertEquals(Optional.of("compact"), committed.member());
            assertEquals(
                    PropertyValue.DartObjectReferenceValue.Access
                            .ZERO_ARGUMENT_INVOCATION,
                    committed.access());
            assertEquals(Optional.of(true), committed.constant());
            return committed;
        });

        PropertyEditor reset = binding.createEditor();
        reset.setValue(FlutterPropertyCellValue.explicit(importedInvocation));
        PropertyEnv resetEnvironment = PropertyEnv.create(descriptor(
                "Clipper", "Reset optional project clipper."));
        ((ExPropertyEditor) reset).attachEnv(resetEnvironment);
        onEdt(() -> {
            Component panel = reset.getCustomEditor();
            JCheckBox useDefault = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME);
            JTextField rootSymbol = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            useDefault.doClick();
            assertFalse(rootSymbol.isEnabled());
            resetEnvironment.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), reset.getValue());
            return null;
        });

        PropertyEditor cancelled = binding.createEditor();
        cancelled.setValue(FlutterPropertyCellValue.explicit(importedInvocation));
        PropertyEnv cancelledEnvironment = PropertyEnv.create(descriptor(
                "Clipper", "Cancelled project clipper edit."));
        ((ExPropertyEditor) cancelled).attachEnv(cancelledEnvironment);
        onEdt(() -> {
            JTextField rootSymbol = findNamed(
                    cancelled.getCustomEditor(), JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            rootSymbol.setText("DiscardedClipper");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    cancelledEnvironment.getState());
            assertEquals(FlutterPropertyCellValue.explicit(importedInvocation),
                    cancelled.getValue(),
                    "closing without STATE_VALID must discard the local reference draft");
            return null;
        });
    }

    @Test
    void cardShapeReferenceEditorIsTransactionalWithNeutralOmission()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.material.Card", "shape"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE,
                binding.editorKind());
        assertTrue(binding.optional());

        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Shape", "Project-declared ShapeBorder."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        PropertyValue.DartObjectReferenceValue committed = onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JCheckBox useDefault = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME);
            JTextField rootSymbol = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            JComboBox<?> access = findNamed(
                    panel, JComboBox.class,
                    FlutterDartObjectReferenceEditorComponent.ACCESS_NAME);
            JCheckBox constant = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.CONSTANT_NAME);
            JLabel preview = findNamed(
                    panel, JLabel.class,
                    FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME);

            assertTrue(useDefault.getText().contains("omit shape"));
            assertAccessibleNameContains(useDefault, "without", "shape");
            assertAccessibleNameContains(preview, "shape", "preview");
            assertEquals("shape: <Flutter default; argument omitted>",
                    preview.getText());
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("ShapeBorder"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("mutually exclusive shape configuration"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("widget's Flutter/theme default"));

            useDefault.doClick();
            rootSymbol.setText("TicketShape");
            access.setSelectedIndex(1);
            constant.doClick();
            assertTrue(preview.getText().contains("shape: const TicketShape()"));
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);

            return assertInstanceOf(
                    PropertyValue.DartObjectReferenceValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
        });

        assertAll(
                () -> assertEquals("TicketShape", committed.rootSymbol()),
                () -> assertEquals(PropertyValue.DartObjectReferenceValue.Access
                        .ZERO_ARGUMENT_INVOCATION, committed.access()),
                () -> assertEquals(Optional.of(true), committed.constant()),
                () -> assertEquals(Optional.empty(), committed.libraryUri()),
                () -> assertEquals(Optional.empty(), committed.member()));

        FlutterTypedPropertyEditors.Binding clipperBinding = binding(
                property("flutter.widgets.ClipPath", "clipper"));
        PropertyEditor clipperEditor = clipperBinding.createEditor();
        clipperEditor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv clipperEnvironment = PropertyEnv.create(descriptor(
                "Clipper", "Project-declared CustomClipper<Path>."));
        ((ExPropertyEditor) clipperEditor).attachEnv(clipperEnvironment);
        onEdt(() -> {
            Component panel = clipperEditor.getCustomEditor();
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("CustomClipper<Path>"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("clears Shape"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("unnamed ClipPath constructor"));
            return null;
        });
    }

    @Test
    void clipPathShapeEditorUsesGenericLabelsAndExplainsStaticHelperBranch()
            throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.ClipPath", "shape"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE,
                binding.editorKind());
        assertTrue(binding.optional());

        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                "Shape", "Project-declared ShapeBorder."));
        ((ExPropertyEditor) editor).attachEnv(environment);

        PropertyValue.DartObjectReferenceValue committed = onEdt(() -> {
            Component panel = editor.getCustomEditor();
            JCheckBox useDefault = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME);
            JTextField rootSymbol = findNamed(
                    panel, JTextField.class,
                    FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            JComboBox<?> access = findNamed(
                    panel, JComboBox.class,
                    FlutterDartObjectReferenceEditorComponent.ACCESS_NAME);
            JCheckBox constant = findNamed(
                    panel, JCheckBox.class,
                    FlutterDartObjectReferenceEditorComponent.CONSTANT_NAME);
            JLabel preview = findNamed(
                    panel, JLabel.class,
                    FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME);

            assertTrue(useDefault.getText().contains("omit shape"));
            assertAccessibleNameContains(useDefault, "without", "shape");
            assertAccessibleNameContains(preview, "shape", "preview");
            assertEquals("shape: <Flutter default; argument omitted>",
                    preview.getText());
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("ShapeBorder"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("mutually exclusive shape configuration"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("widget's Flutter/theme default"));

            useDefault.doClick();
            rootSymbol.setText("TicketShape");
            access.setSelectedIndex(1);
            constant.doClick();
            assertTrue(preview.getText().contains("shape: const TicketShape()"));
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            environment.setState(PropertyEnv.STATE_VALID);

            return assertInstanceOf(
                    PropertyValue.DartObjectReferenceValue.class,
                    ((FlutterPropertyCellValue) editor.getValue())
                            .explicitValue().orElseThrow());
        });

        assertAll(
                () -> assertEquals("TicketShape", committed.rootSymbol()),
                () -> assertEquals(PropertyValue.DartObjectReferenceValue.Access
                        .ZERO_ARGUMENT_INVOCATION, committed.access()),
                () -> assertEquals(Optional.of(true), committed.constant()),
                () -> assertEquals(Optional.empty(), committed.libraryUri()),
                () -> assertEquals(Optional.empty(), committed.member()));

        FlutterTypedPropertyEditors.Binding clipperBinding = binding(
                property("flutter.widgets.ClipPath", "clipper"));
        PropertyEditor clipperEditor = clipperBinding.createEditor();
        clipperEditor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv clipperEnvironment = PropertyEnv.create(descriptor(
                "Clipper", "Project-declared CustomClipper<Path>."));
        ((ExPropertyEditor) clipperEditor).attachEnv(clipperEnvironment);
        onEdt(() -> {
            Component panel = clipperEditor.getCustomEditor();
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("CustomClipper<Path>"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("clears Shape"));
            assertTrue(panel.getAccessibleContext().getAccessibleDescription()
                    .contains("unnamed ClipPath constructor"));
            return null;
        });
    }

    @Test
    void booleanCellPainterCentersExplicitCheckboxGlyph() throws Exception {
        FlutterTypedPropertyEditors.Binding binding = binding(
                property("flutter.widgets.Text", "softWrap"));
        PropertyEditor editor = binding.createEditor();

        onEdt(() -> {
            int width = 140;
            int height = 24;
            int[] checked = paintBoolean(editor, FlutterPropertyCellValue.explicit(
                    new PropertyValue.BooleanValue(true)));
            Rectangle glyph = nonBackgroundBounds(
                    checked, width, Color.WHITE.getRGB());

            assertNotNull(glyph, "the checked checkbox glyph must be painted");
            assertEquals(width / 2.0, glyph.getCenterX(), 4.0,
                    "the checkbox glyph must be horizontally centered");
            assertEquals(height / 2.0, glyph.getCenterY(), 4.0,
                    "the checkbox glyph must be vertically centered");
            return null;
        });
    }

    private static int[] paintBoolean(
            PropertyEditor editor,
            FlutterPropertyCellValue value) {
        int width = 140;
        int height = 24;
        BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.BLACK);
            editor.setValue(value);
            editor.paintValue(graphics, new Rectangle(0, 0, width, height));
        } finally {
            graphics.dispose();
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    private static Rectangle nonBackgroundBounds(
            int[] pixels,
            int width,
            int backgroundRgb) {
        int minX = width;
        int minY = pixels.length / width;
        int maxX = -1;
        int maxY = -1;
        for (int index = 0; index < pixels.length; index++) {
            if (pixels[index] == backgroundRgb) {
                continue;
            }
            int x = index % width;
            int y = index / width;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return maxX < minX
                ? null
                : new Rectangle(minX, minY,
                        maxX - minX + 1, maxY - minY + 1);
    }

    private static int[] paintExpectedBooleanRenderer(
            Component renderer,
            Node.Property<?> property,
            int width,
            int height) throws Exception {
        FlutterPropertyCellValue value = assertInstanceOf(
                FlutterPropertyCellValue.class, property.getValue());
        BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(renderer.getBackground());
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(renderer.getForeground());
            // RELEASE300's table StringRenderer reserves the same three-pixel
            // value margin before delegating to PropertyEditor.paintValue().
            FlutterPropertyEditorComponents.paintBooleanValue(
                    graphics, new Rectangle(3, 0, width - 3, height), value);
        } finally {
            graphics.dispose();
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    private static int[] paintRenderer(
            Component renderer,
            int width,
            int height) {
        renderer.setBounds(0, 0, width, height);
        BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        try {
            renderer.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    private static void selectLabel(JComboBox<?> combo, String label) {
        for (int index = 0; index < combo.getItemCount(); index++) {
            if (label.equals(String.valueOf(combo.getItemAt(index)))) {
                combo.setSelectedIndex(index);
                return;
            }
        }
        throw new AssertionError("Missing combo item " + label);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String renderedNullSelectionText(JComboBox<?> combo) {
        ListCellRenderer renderer = combo.getRenderer();
        Component component = renderer.getListCellRendererComponent(
                new JList<>(), null, -1, false, false);
        return assertInstanceOf(JLabel.class, component).getText();
    }

    private static PropertyDefinition property(String widgetType, String name) {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId(widgetType)).orElseThrow();
        return definition.properties().stream()
                .filter(property -> property.name().value().equals(name))
                .findFirst().orElseThrow();
    }

    private static <T extends Component> void assertStructuredEditorHasNamedComponent(
            FlutterTypedPropertyEditors.Binding binding,
            String componentName,
            Class<T> componentType) throws Exception {
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.unset());
        PropertyEnv environment = PropertyEnv.create(descriptor(
                binding.definition().name().value(),
                "Typed Container structured property."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        onEdt(() -> {
            Component panel = editor.getCustomEditor();
            assertNotNull(findNamed(panel, componentType, componentName), componentName);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            return null;
        });
    }

    private static PropertyEditor decorationEditor(PropertyValue.BoxDecorationValue value) {
        PropertyEditor editor = binding(
                property("flutter.widgets.Container", "decoration")).createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(value));
        return editor;
    }

    private static PropertyEnv attachedEnvironment(PropertyEditor editor, String displayName) {
        PropertyEnv environment = PropertyEnv.create(descriptor(
                displayName, "Container " + displayName + " editor."));
        ((ExPropertyEditor) editor).attachEnv(environment);
        return environment;
    }

    private static PropertyValue.BoxDecorationValue decoration(
            java.util.Optional<PropertyValue.BoxDecorationValue.BoxBorder> border,
            java.util.Optional<PropertyValue.BoxDecorationValue.BoxGradient> gradient) {
        return new PropertyValue.BoxDecorationValue(
                java.util.Optional.empty(), border, java.util.Optional.empty(), List.of(),
                gradient, java.util.Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }

    private static PropertyValue.BoxDecorationValue.BorderSide borderSide(String width) {
        return new PropertyValue.BoxDecorationValue.BorderSide(
                new ColorSource.Literal(0xFF000000L), new BigDecimal(width),
                PropertyValue.BoxDecorationValue.BorderStyle.SOLID,
                BigDecimal.ONE.negate());
    }

    private static PropertyValue.AlignmentGeometryValue alignment(String x, String y) {
        return new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                new BigDecimal(x), new BigDecimal(y));
    }

    private static PropertyValue.BoxDecorationValue.Radius radius(String x, String y) {
        return new PropertyValue.BoxDecorationValue.Radius(
                new BigDecimal(x), new BigDecimal(y));
    }

    private static List<PropertyValue.BoxDecorationValue.GradientStop> gradientStops() {
        return List.of(
                new PropertyValue.BoxDecorationValue.GradientStop(
                        StableId.parse("04981217-b9cb-46b2-a532-a11509105518"),
                        new ColorSource.Literal(0xFF000000L), BigDecimal.ZERO),
                new PropertyValue.BoxDecorationValue.GradientStop(
                        StableId.parse("21323221-5377-45e9-a986-c71f51c64e4b"),
                        new ColorSource.Literal(0xFFFFFFFFL), BigDecimal.ONE));
    }

    private static PropertyValue.Matrix4Value identityMatrix() {
        java.util.ArrayList<BigDecimal> storage = new java.util.ArrayList<>(16);
        for (int index = 0; index < 16; index++) {
            storage.add(index % 5 == 0 ? BigDecimal.ONE : BigDecimal.ZERO);
        }
        return new PropertyValue.Matrix4Value(storage);
    }

    private static PropertyValue.BoxDecorationValue committedDecoration(PropertyEditor editor) {
        return assertInstanceOf(
                PropertyValue.BoxDecorationValue.class,
                ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow());
    }

    private static void assertTableColumn(
            JTable table, int column, String... expectedValues) {
        assertEquals(expectedValues.length, table.getRowCount());
        for (int row = 0; row < expectedValues.length; row++) {
            assertEquals(expectedValues[row], String.valueOf(table.getValueAt(row, column)),
                    "row " + row + ", column " + column);
        }
    }

    private static void assertAccessibleNameContains(Component component, String... words) {
        String name = component.getAccessibleContext().getAccessibleName();
        assertNotNull(name);
        String normalized = name.toLowerCase(java.util.Locale.ROOT);
        for (String word : words) {
            assertTrue(normalized.contains(word), name);
        }
    }

    private static int awaitPropertyRow(JTable table, String name)
            throws Exception {
        long deadline = System.nanoTime()
                + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            int row = onEdt(() -> {
                for (int index = 0; index < table.getRowCount(); index++) {
                    Object value = table.getValueAt(index, 1);
                    if (value instanceof FeatureDescriptor descriptor
                            && name.equals(descriptor.getName())) {
                        return index;
                    }
                }
                return -1;
            });
            if (row >= 0) {
                return row;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("PropertySheet row not loaded: " + name);
    }

    private static Node.Property<?> findProperty(
            FlutterWidgetPropertiesNode node, String name) {
        return java.util.Arrays.stream(node.getPropertySets())
                .flatMap(set -> java.util.Arrays.stream(set.getProperties()))
                .filter(property -> property.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private static FlutterTypedPropertyEditors.Binding binding(
            PropertyDefinition definition) {
        return FlutterTypedPropertyEditors.binding(definition).orElseThrow();
    }

    private static PropertyDefinition complexProperty(
            String name, PropertyValueKind... kinds) {
        List<PropertyValueConstraint> constraints;
        Set<PropertyValueKind> shape = Set.of(kinds);
        List<String> colors = MaterialThemeTokenCatalog.colorRoles()
                .keySet().stream().sorted().toList();
        if (shape.equals(Set.of(
                PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN))) {
            constraints = List.of(
                    new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR),
                    new PropertyValueConstraint.ThemeTokenValues(colors));
        } else if (shape.equals(Set.of(PropertyValueKind.THEME_TOKEN))) {
            constraints = List.of(new PropertyValueConstraint.ThemeTokenValues(
                    MaterialThemeTokenCatalog.textStyleRoles().keySet().stream()
                            .sorted().toList()));
        } else if (shape.equals(Set.of(PropertyValueKind.PAINT))) {
            constraints = List.of(new PropertyValueConstraint.PaintValues(colors));
        } else if (shape.equals(Set.of(PropertyValueKind.SHADOW_LIST))) {
            constraints = List.of(new PropertyValueConstraint.ShadowListValues(colors));
        } else if (shape.equals(Set.of(PropertyValueKind.FONT_VARIATION_LIST))) {
            constraints = List.of(new PropertyValueConstraint.FontVariationListValues());
        } else {
            constraints = java.util.Arrays.stream(kinds)
                    .map(kind -> (PropertyValueConstraint)
                    new PropertyValueConstraint.AnyValue(kind)).toList();
        }
        return new PropertyDefinition(
                new PropertyName(name),
                DartParameter.named(0, false),
                constraints,
                java.util.Optional.empty());
    }

    private static FeatureDescriptor descriptor(
            String displayName, String description) {
        FeatureDescriptor descriptor = new FeatureDescriptor();
        descriptor.setName(displayName.replace(' ', '_'));
        descriptor.setDisplayName(displayName);
        descriptor.setShortDescription(description);
        return descriptor;
    }

    private static PropertyValue.EdgeInsetsValue edge(
            String left, String top, String right, String bottom) {
        return new PropertyValue.EdgeInsetsValue(
                new BigDecimal(left), new BigDecimal(top),
                new BigDecimal(right), new BigDecimal(bottom));
    }

    private static PropertyValue.EdgeInsetsDirectionalValue directionalEdge(
            String start, String top, String end, String bottom) {
        return new PropertyValue.EdgeInsetsDirectionalValue(
                new BigDecimal(start), new BigDecimal(top),
                new BigDecimal(end), new BigDecimal(bottom));
    }

    private static <T extends Component> T findNamed(
            Component root, Class<T> type, String name) {
        if (type.isInstance(root) && name.equals(root.getName())) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findNamed(child, type, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Component renderedListCell(JList<?> list, Object value) {
        javax.swing.ListCellRenderer renderer = list.getCellRenderer();
        return renderer.getListCellRendererComponent(
                list, value, 0, false, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Component renderedComboCell(
            JComboBox<?> combo,
            Object value) {
        javax.swing.ListCellRenderer renderer = combo.getRenderer();
        return renderer.getListCellRendererComponent(
                new JList<>(), value, 0, false, false);
    }

    private static <T extends Component> T findFirst(
            Component root, Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findFirst(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JComboBox<?> findComboWithItems(Component root, Object... items) {
        if (root instanceof JComboBox<?> combo && combo.getItemCount() == items.length) {
            boolean matches = true;
            for (int index = 0; index < items.length; index++) {
                matches &= items[index].equals(combo.getItemAt(index));
            }
            if (matches) {
                return combo;
            }
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JComboBox<?> found = findComboWithItems(child, items);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JLabel findLabel(Component root, String text) {
        if (root instanceof JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel found = findLabel(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T findAccessibleName(
            Component root, Class<T> type, String name) {
        if (type.isInstance(root)
                && name.equals(root.getAccessibleContext().getAccessibleName())) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findAccessibleName(child, type, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T findByText(
            Component root, Class<T> type, String text) {
        if (type.isInstance(root)
                && root instanceof JCheckBox checkBox
                && text.equals(checkBox.getText())) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findByText(child, type, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton findButton(Component root, String text) {
        if (root instanceof JButton button && text.equals(button.getText())) {
            return button;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButton(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T onEdt(Callable<T> operation) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return operation.call();
        }
        FutureTask<T> task = new FutureTask<>(operation);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
