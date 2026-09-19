package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.awt.*;
import java.beans.*;
import java.math.BigInteger;
import java.util.*;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import static org.junit.jupiter.api.Assertions.*;

class ExpansionTileEditorComponentsTest {
    @Test void signedMicrosecondsAreExactDraftsAndRejectExpressionsDecimalsAndPortableOverflow() throws Exception {
        for (String name : ExpansionTileWidgetPropertySchema.animationDurationProperties()) onEdt(() -> {
            var editor = binding(name).createEditor(); var initial = FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(BigInteger.valueOf(250000))); editor.setValue(initial);
            var env = environment(editor); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet()); var panel = editor.getCustomEditor();
            assertTrue(text(panel).contains("ignores reverseDuration")); assertTrue(text(panel).contains("Negative Duration"));
            var input = find(panel, JTextField.class, FlutterDurationReferenceEditorComponent.VALUE_NAME);
            for (String invalid : List.of("", "1.5", "NaN", "250ms", "Duration(seconds: 1)", "9007199254740992", "-9007199254740992")) {
                input.setText(invalid); env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState(), invalid); assertEquals(initial, editor.getValue()); assertEquals(0, commits.get());
            }
            input.setText("-9007199254740991"); assertEquals(initial, editor.getValue()); env.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(new BigInteger("-9007199254740991"))), editor.getValue()); assertEquals(1, commits.get());
            return null;
        });
        for (String name : ExpansionTileWidgetPropertySchema.animationDurationProperties()) for (String integer : List.of("-9007199254740991", "9007199254740991", "-1", "0", "1234567")) roundTrip(name, FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(new BigInteger(integer))));
    }

    @Test void durationOmitNullLocalImportedGetterMemberAndFactoryModesPublishOnlyOnOkAndReopenExactly() throws Exception {
        for (String name : ExpansionTileWidgetPropertySchema.animationDurationProperties()) for (String branch : List.of("omit", "null", "local", "getter", "member", "factory")) onEdt(() -> {
            var editor = binding(name).createEditor(); var initial = FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(BigInteger.ONE)); editor.setValue(initial);
            var env = environment(editor); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet()); var panel = editor.getCustomEditor();
            var mode = find(panel, JComboBox.class, FlutterDurationReferenceEditorComponent.MODE_NAME); FlutterPropertyCellValue expected;
            if (branch.equals("omit")) { mode.setSelectedItem(FlutterDurationReferenceEditorComponent.OMIT); expected = FlutterPropertyCellValue.unset(); }
            else if (branch.equals("null")) { mode.setSelectedItem(FlutterDurationReferenceEditorComponent.NULL); expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()); }
            else { mode.setSelectedItem(FlutterDurationReferenceEditorComponent.PROJECT); expected = setReference(panel, branch); }
            assertEquals(initial, editor.getValue()); assertEquals(0, commits.get(), "Cancel leaves the unpublished draft local."); env.setState(PropertyEnv.STATE_VALID);
            assertEquals(expected, editor.getValue()); assertEquals(1, commits.get()); reopen(name, expected); return null;
        });
    }

    @Test void everyCurvePresetAndNullableAnimationValueKeepsExactTypeAndIndependentDrafts() throws Exception {
        assertEquals(43, ExpansionTileWidgetPropertySchema.curvePresets().size());
        for (String name : ExpansionTileWidgetPropertySchema.animationCurveProperties()) for (String curve : ExpansionTileWidgetPropertySchema.curvePresets()) roundTrip(name, FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(curve)));
        roundTrip("expansionAnimationStyle", FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("noAnimation")));
        for (String name : List.of("expansionAnimationStyle", "expansionAnimationStyleCurve", "expansionAnimationStyleReverseCurve")) {
            roundTrip(name, FlutterPropertyCellValue.unset()); roundTrip(name, FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
            onEdt(() -> {
                var editor = binding(name).createEditor(); var initial = FlutterPropertyCellValue.unset(); editor.setValue(initial); var env = environment(editor); var panel = editor.getCustomEditor();
                find(panel, JComboBox.class, FlutterPresetDartReferenceEditorComponent.MODE_NAME).setSelectedItem(FlutterPresetDartReferenceEditorComponent.PROJECT);
                var expected = setReference(panel, "factory"); assertEquals(initial, editor.getValue()); env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue()); reopen(name, expected);
                assertTrue(text(panel).contains("reverseDuration")); return null;
            });
        }
    }

    @Test void nullableColorsInsetsAndAlignmentKeepLiteralNullOmissionAndReferenceModesSeparate() throws Exception {
        var names = new ArrayList<>(ExpansionTileWidgetPropertySchema.colorProperties()); names.addAll(List.of("tilePadding", "childrenPadding", "expandedAlignment"));
        for (String name : names) {
            roundTrip(name, FlutterPropertyCellValue.explicit(ExpansionTilePropertyContractTest.value(name)));
            for (String branch : List.of("omit", "null", "local", "getter", "member", "factory")) onEdt(() -> {
                var initial = FlutterPropertyCellValue.explicit(ExpansionTilePropertyContractTest.value(name)); var editor = binding(name).createEditor(); editor.setValue(initial);
                var env = environment(editor); var panel = editor.getCustomEditor(); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
                var mode = find(panel, JComboBox.class, FlutterLocalDartReferenceEditorComponent.MODE_NAME); FlutterPropertyCellValue expected;
                if (branch.equals("omit")) { mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.OMIT); expected = FlutterPropertyCellValue.unset(); }
                else if (branch.equals("null")) { mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.NULL); expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()); }
                else { mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.PROJECT); expected = setReference(panel, branch); }
                assertEquals(initial, editor.getValue()); assertEquals(0, commits.get()); env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue()); assertEquals(1, commits.get()); reopen(name, expected); return null;
            });
        }
    }

    @Test void optionalCallbackModesDoNotDisableExpansionAndNullableBooleansKeepCenteredCheckbox() throws Exception {
        for (var value : List.of(FlutterPropertyCellValue.unset(), FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("noop")), FlutterPropertyCellValue.explicit(ExpansionTilePropertyContractTest.reference("_expanded")))) onEdt(() -> {
            var editor = binding("onExpansionChanged").createEditor(); editor.setValue(value); var env = environment(editor); var panel = editor.getCustomEditor();
            assertTrue(text(panel).contains("true when expanding starts and false when collapsing starts")); assertTrue(text(panel).contains("seed")); env.setState(PropertyEnv.STATE_VALID); assertEquals(value, editor.getValue()); return null;
        });
        for (String name : List.of("dense", "enableFeedback")) for (var initial : List.of(FlutterPropertyCellValue.unset(), FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)), FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)))) onEdt(() -> {
            var editor = binding(name).createEditor(); editor.setValue(initial); var env = environment(editor); var panel = editor.getCustomEditor();
            assertEquals(SwingConstants.CENTER, find(panel, JCheckBox.class, FlutterNullableChoiceEditorComponent.VALUE_NAME).getHorizontalAlignment()); env.setState(PropertyEnv.STATE_VALID); assertEquals(initial, editor.getValue()); return null;
        });
    }

    private static FlutterPropertyCellValue setReference(Component panel, String branch) {
        String root = branch.equals("getter") ? "_valueGetter" : branch.equals("member") ? "styles" : branch.equals("factory") ? "buildValue" : "_value";
        Optional<String> library = branch.equals("member") || branch.equals("factory") ? Optional.of("package:app/styles.dart") : Optional.empty();
        if (library.isPresent()) { find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME).setSelectedIndex(1); find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME).setText(library.orElseThrow()); }
        find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText(root);
        if (branch.equals("member")) find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME).setText("value");
        if (branch.equals("factory")) find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
        return FlutterPropertyCellValue.explicit(new PropertyValue.DartObjectReferenceValue(library, root, branch.equals("member") ? Optional.of("value") : Optional.empty(), branch.equals("factory") ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, branch.equals("factory") ? Optional.of(false) : Optional.empty()));
    }
    private static void roundTrip(String name, FlutterPropertyCellValue value) throws Exception { onEdt(() -> { reopen(name, value); return null; }); }
    private static void reopen(String name, FlutterPropertyCellValue value) { var editor = binding(name).createEditor(); editor.setValue(value); var env = environment(editor); editor.getCustomEditor(); env.setState(PropertyEnv.STATE_VALID); assertEquals(value, editor.getValue(), name); }
    private static FlutterTypedPropertyEditors.Binding binding(String name) {
        var presets = name.equals("onExpansionChanged") ? List.of("noop") : name.equals("expansionAnimationStyle") ? ExpansionTileWidgetPropertySchema.animationStylePresets() : ExpansionTileWidgetPropertySchema.animationCurveProperties().contains(name) ? ExpansionTileWidgetPropertySchema.curvePresets() : List.<String>of();
        return FlutterTypedPropertyEditors.binding(ExpansionTilePropertyContractTest.DEF.property(new PropertyName(name)).orElseThrow(), Optional.empty(), false, presets).orElseThrow();
    }
    private static PropertyEnv environment(PropertyEditor editor) { var env = PropertyEnv.create(new FeatureDescriptor()); ((ExPropertyEditor) editor).attachEnv(env); return env; }
    private static String text(Component component) { String result = component instanceof JTextArea area ? area.getText() : ""; if (component instanceof Container container) for (Component child : container.getComponents()) result += text(child); return result; }
    private static <T extends Component> T find(Component component, Class<T> type, String name) { T result = maybe(component, type, name); if (result == null) throw new AssertionError("Missing " + name); return result; }
    private static <T extends Component> T maybe(Component component, Class<T> type, String name) { if (type.isInstance(component) && name.equals(component.getName())) return type.cast(component); if (component instanceof Container container) for (Component child : container.getComponents()) { T result = maybe(child, type, name); if (result != null) return result; } return null; }
    private static <T> T onEdt(java.util.concurrent.Callable<T> operation) throws Exception { var task = new FutureTask<T>(operation); SwingUtilities.invokeAndWait(task); return task.get(); }
}
