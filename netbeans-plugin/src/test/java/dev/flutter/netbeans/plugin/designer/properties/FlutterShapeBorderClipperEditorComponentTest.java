package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.awt.Component;
import java.awt.Container;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import static org.junit.jupiter.api.Assertions.*;

class FlutterShapeBorderClipperEditorComponentTest {
    private static final PropertyValue.ShapeBorderClipperValue DEFAULT =
            PropertyValue.ShapeBorderClipperValue.defaultValue();

    @Test void requiredUnionCannotBeUnsetAndDoesNotAcceptRawDart() {
        var binding = binding();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.SHAPE_BORDER_CLIPPER, binding.editorKind());
        assertFalse(binding.optional());
        assertThrows(IllegalArgumentException.class,
                () -> binding.validate(FlutterPropertyCellValue.unset()));
        var editor = binding.createEditor();
        assertTrue(editor.supportsCustomEditor());
        assertThrows(IllegalArgumentException.class, () -> editor.setAsText("ShapeBorderClipper()"));
    }

    @Test void allSixShapesCommitExactlyOnceAndReopenWithoutOmittingIgnoredGeometry() throws Exception {
        for (var shape : PropertyValue.ShapeBorderClipperValue.Shape.values()) {
            var configured = edt(() -> {
                var fixture = fixture(DEFAULT);
                selectShape(fixture.panel(), shape);
                JTable table = named(fixture.panel(), FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME, JTable.class);
                assertTrue(table.editCellAt(0, 1));
                ((JTextField) table.getEditorComponent()).setText("12.5");
                assertEquals(FlutterPropertyCellValue.explicit(DEFAULT), fixture.editor().getValue());
                fixture.env().setState(PropertyEnv.STATE_VALID);
                var value = (PropertyValue.ShapeBorderClipperValue) value(fixture);
                assertEquals(shape, value.shape());
                assertEquals(new BigDecimal("12.5"), ((PropertyValue.BoxDecorationValue.PhysicalBorderRadius)
                        value.borderRadius()).topLeft().x());
                assertEquals(1, fixture.changes().get());
                fixture.env().setState(PropertyEnv.STATE_NEEDS_VALIDATION);
                fixture.env().setState(PropertyEnv.STATE_VALID);
                assertEquals(1, fixture.changes().get());
                return value;
            });
            edt(() -> {
                var reopened = fixture(configured);
                assertEquals(shape, named(reopened.panel(), FlutterShapeBorderClipperEditorComponent.SHAPE_NAME, JComboBox.class).getSelectedItem());
                selectShape(reopened.panel(), PropertyValue.ShapeBorderClipperValue.Shape.ROUNDED_RECTANGLE);
                reopened.env().setState(PropertyEnv.STATE_VALID);
                assertEquals(configured.borderRadius(), ((PropertyValue.ShapeBorderClipperValue) value(reopened)).borderRadius());
                return null;
            });
        }
    }

    @Test void directionalRoundedGeometryRequiresExplicitDirectionAndInvalidActiveCellCannotCommit() throws Exception {
        edt(() -> {
            var fixture = fixture(DEFAULT);
            named(fixture.panel(), FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME, JComboBox.class).setSelectedIndex(1);
            fixture.env().setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_INVALID, fixture.env().getState());
            assertEquals(0, fixture.changes().get());
            named(fixture.panel(), FlutterShapeBorderClipperEditorComponent.DIRECTION_NAME, JComboBox.class).setSelectedIndex(2);
            JTable table = named(fixture.panel(), FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME, JTable.class);
            assertTrue(table.editCellAt(3, 2));
            ((JTextField) table.getEditorComponent()).setText("-8");
            fixture.env().setState(PropertyEnv.STATE_VALID);
            assertEquals(PropertyEnv.STATE_INVALID, fixture.env().getState());
            assertEquals(0, fixture.changes().get());
            assertTrue(table.editCellAt(3, 2));
            ((JTextField) table.getEditorComponent()).setText("8");
            fixture.env().setState(PropertyEnv.STATE_VALID);
            var value = (PropertyValue.ShapeBorderClipperValue) value(fixture);
            assertEquals(Optional.of(PropertyValue.ShapeBorderClipperValue.TextDirection.RTL), value.textDirection());
            assertInstanceOf(PropertyValue.BoxDecorationValue.DirectionalBorderRadius.class, value.borderRadius());
            return null;
        });
    }

    @Test void circleRetainsDirectionalRadiiWithoutDirectionAndSwitchBackRequiresIt() throws Exception {
        edt(() -> {
            var fixture = fixture(DEFAULT);
            selectShape(fixture.panel(), PropertyValue.ShapeBorderClipperValue.Shape.CIRCLE);
            named(fixture.panel(), FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME, JComboBox.class).setSelectedIndex(1);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, fixture.env().getState());
            selectShape(fixture.panel(), PropertyValue.ShapeBorderClipperValue.Shape.BEVELED_RECTANGLE);
            assertEquals(PropertyEnv.STATE_INVALID, fixture.env().getState());
            selectShape(fixture.panel(), PropertyValue.ShapeBorderClipperValue.Shape.STADIUM);
            fixture.env().setState(PropertyEnv.STATE_VALID);
            var value = (PropertyValue.ShapeBorderClipperValue) value(fixture);
            assertTrue(value.textDirection().isEmpty());
            assertInstanceOf(PropertyValue.BoxDecorationValue.DirectionalBorderRadius.class, value.borderRadius());
            return null;
        });
    }

    @Test void branchSwitchAndCancelNeverPublishDraftAndUnusedInvalidReferenceDoesNotBlockPreset() throws Exception {
        edt(() -> {
            var fixture = fixture(DEFAULT);
            JComboBox<?> mode = named(fixture.panel(), FlutterShapeBorderClipperEditorComponent.MODE_NAME, JComboBox.class);
            mode.setSelectedIndex(1);
            fixture.env().setState(PropertyEnv.STATE_VALID);
            assertEquals(0, fixture.changes().get());
            assertEquals(PropertyEnv.STATE_INVALID, fixture.env().getState());
            named(fixture.panel(), FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).setText("_projectClipper");
            mode.setSelectedIndex(0);
            selectShape(fixture.panel(), PropertyValue.ShapeBorderClipperValue.Shape.CIRCLE);
            mode.setSelectedIndex(1);
            assertEquals("_projectClipper", named(fixture.panel(), FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).getText());
            assertEquals(0, fixture.changes().get(), "Cancel: no STATE_VALID means no model value changes");
            var reopened = fixture(DEFAULT);
            assertEquals(0, named(reopened.panel(), FlutterShapeBorderClipperEditorComponent.MODE_NAME, JComboBox.class).getSelectedIndex());
            reopened.env().setState(PropertyEnv.STATE_VALID);
            assertEquals(DEFAULT, value(reopened));
            return null;
        });
    }

    @Test void currentAndImportedReferencesPreserveMemberAccessAndConstnessAcrossReopen() throws Exception {
        for (boolean imported : new boolean[]{false, true}) {
            for (boolean constant : new boolean[]{false, true}) {
                edt(() -> {
                    var fixture = fixture(DEFAULT);
                    named(fixture.panel(), FlutterShapeBorderClipperEditorComponent.MODE_NAME, JComboBox.class).setSelectedIndex(1);
                    assertFalse(hasNamed(fixture.panel(), FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME));
                    named(fixture.panel(), FlutterDartObjectReferenceEditorComponent.SCOPE_NAME, JComboBox.class).setSelectedIndex(imported ? 1 : 0);
                    named(fixture.panel(), FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME, JTextField.class).setText("package:app/clippers.dart");
                    named(fixture.panel(), FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).setText("PathClipper");
                    named(fixture.panel(), FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).setText("compact");
                    named(fixture.panel(), FlutterDartObjectReferenceEditorComponent.ACCESS_NAME, JComboBox.class).setSelectedIndex(1);
                    named(fixture.panel(), FlutterDartObjectReferenceEditorComponent.CONSTANT_NAME, JCheckBox.class).setSelected(constant);
                    JTextArea note = named(fixture.panel(), FlutterShapeBorderClipperEditorComponent.NOTE_NAME, JTextArea.class);
                    assertTrue(note.getText().contains("fill and shadow preview is unavailable"));
                    fixture.env().setState(PropertyEnv.STATE_VALID);
                    var reference = (PropertyValue.DartObjectReferenceValue) value(fixture);
                    assertEquals(imported ? Optional.of("package:app/clippers.dart") : Optional.empty(), reference.libraryUri());
                    assertEquals(Optional.of("compact"), reference.member());
                    assertEquals(Optional.of(constant), reference.constant());
                    var reopened = fixture(reference);
                    assertEquals(1, named(reopened.panel(), FlutterShapeBorderClipperEditorComponent.MODE_NAME, JComboBox.class).getSelectedIndex());
                    reopened.env().setState(PropertyEnv.STATE_VALID);
                    assertEquals(reference, value(reopened));
                    return null;
                });
            }
        }
    }

    @Test void explicitDirectionAndRadiusAreRetainedWhenChangingThroughIgnoredShapes() throws Exception {
        edt(() -> {
            var fixture = fixture(DEFAULT);
            named(fixture.panel(), FlutterShapeBorderClipperEditorComponent.DIRECTION_NAME, JComboBox.class).setSelectedIndex(2);
            named(fixture.panel(), FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME, JComboBox.class).setSelectedIndex(1);
            selectShape(fixture.panel(), PropertyValue.ShapeBorderClipperValue.Shape.CIRCLE);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, fixture.env().getState());
            selectShape(fixture.panel(), PropertyValue.ShapeBorderClipperValue.Shape.STADIUM);
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, fixture.env().getState());
            selectShape(fixture.panel(), PropertyValue.ShapeBorderClipperValue.Shape.ROUNDED_RECTANGLE);
            fixture.env().setState(PropertyEnv.STATE_VALID);
            var value = (PropertyValue.ShapeBorderClipperValue) value(fixture);
            assertEquals(Optional.of(PropertyValue.ShapeBorderClipperValue.TextDirection.RTL), value.textDirection());
            assertInstanceOf(PropertyValue.BoxDecorationValue.DirectionalBorderRadius.class, value.borderRadius());
            return null;
        });
    }

    private static FlutterTypedPropertyEditors.Binding binding() {
        return FlutterTypedPropertyEditors.binding(BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.PhysicalShape")).orElseThrow()
                .property(new PropertyName("clipper")).orElseThrow()).orElseThrow();
    }
    private static Fixture fixture(PropertyValue value) {
        PropertyEditor editor = binding().createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(value));
        PropertyEnv env = PropertyEnv.create(new FeatureDescriptor());
        ((ExPropertyEditor) editor).attachEnv(env);
        AtomicInteger changes = new AtomicInteger();
        editor.addPropertyChangeListener(ignored -> changes.incrementAndGet());
        return new Fixture(editor, env, editor.getCustomEditor(), changes);
    }
    private static PropertyValue value(Fixture fixture) {
        return ((FlutterPropertyCellValue) fixture.editor().getValue()).explicitValue().orElseThrow();
    }
    private static void selectShape(Component panel, PropertyValue.ShapeBorderClipperValue.Shape shape) {
        named(panel, FlutterShapeBorderClipperEditorComponent.SHAPE_NAME, JComboBox.class).setSelectedItem(shape);
    }
    private static boolean hasNamed(Component component, String name) {
        if (name.equals(component.getName())) { return true; }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) { if (hasNamed(child, name)) { return true; } }
        }
        return false;
    }
    private static <T extends Component> T named(Component component, String name, Class<T> type) {
        if (name.equals(component.getName()) && type.isInstance(component)) { return type.cast(component); }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (hasNamed(child, name)) { return named(child, name, type); }
            }
        }
        throw new AssertionError("Missing component " + name);
    }
    private static <T> T edt(Callable<T> operation) throws Exception {
        FutureTask<T> task = new FutureTask<>(operation);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
    private record Fixture(PropertyEditor editor, PropertyEnv env, Component panel, AtomicInteger changes) { }
}
