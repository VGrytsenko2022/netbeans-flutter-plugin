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

class TooltipEditorComponentsTest {
    @Test void allTwelveReferenceFamiliesSupportExactImportedGetterMemberFactoryNullAndOmitDrafts() throws Exception {
        for (String name : List.of("richMessage", "constraints", "padding", "margin", "decoration", "textStyle", "waitDurationUs", "showDurationUs", "exitDurationUs", "onTriggered", "mouseCursor", "positionDelegate"))
            for (String branch : List.of("omit", "null", "local", "getter", "member", "factory")) onEdt(() -> {
                var editor = binding(name).createEditor(); var initial = FlutterPropertyCellValue.explicit(TooltipPropertyContractTest.value(name)); editor.setValue(initial);
                var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor(); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
                String modeName; String omit; String project; String nullMode = "Explicit null";
                if (TooltipWidgetPropertySchema.durationProperties().contains(name)) { modeName = FlutterDurationReferenceEditorComponent.MODE_NAME; omit = FlutterDurationReferenceEditorComponent.OMIT; project = FlutterDurationReferenceEditorComponent.PROJECT; }
                else if (List.of("constraints", "padding", "margin", "decoration").contains(name)) { modeName = FlutterLocalDartReferenceEditorComponent.MODE_NAME; omit = FlutterLocalDartReferenceEditorComponent.OMIT; project = FlutterLocalDartReferenceEditorComponent.PROJECT; }
                else if (List.of("mouseCursor", "onTriggered").contains(name)) { modeName = FlutterPresetDartReferenceEditorComponent.MODE_NAME; omit = FlutterPresetDartReferenceEditorComponent.OMIT; project = FlutterPresetDartReferenceEditorComponent.PROJECT; if (name.equals("mouseCursor")) nullMode = FlutterPresetDartReferenceEditorComponent.NULL; }
                else { modeName = FlutterNullableDartReferenceEditorComponent.MODE_NAME; omit = FlutterNullableDartReferenceEditorComponent.OMIT; project = FlutterNullableDartReferenceEditorComponent.PROJECT; }
                var mode = find(panel, JComboBox.class, modeName); FlutterPropertyCellValue expected;
                if (branch.equals("omit")) { mode.setSelectedItem(omit); expected = FlutterPropertyCellValue.unset(); }
                else if (branch.equals("null")) { mode.setSelectedItem(nullMode); expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()); }
                else { mode.setSelectedItem(project); expected = setReference(panel, branch); }
                assertEquals(initial, editor.getValue()); assertEquals(0, commits.get(), "Cancel discards the nested draft."); env.setState(PropertyEnv.STATE_VALID);
                assertEquals(expected, editor.getValue(), name + " " + branch); assertEquals(1, commits.get()); reopen(name, expected); return null;
            });
    }

    @Test void allThreeDurationEditorsPreserveSignedPortableMicrosecondsAndExplainDistinctSdkTimers() throws Exception {
        for (String name : TooltipWidgetPropertySchema.durationProperties()) {
            for (String integer : List.of("-9007199254740991", "9007199254740991", "-1", "0", "1234567")) onEdt(() -> { reopen(name, FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(new BigInteger(integer)))); return null; });
            onEdt(() -> {
                var editor = binding(name).createEditor(); var initial = FlutterPropertyCellValue.explicit(TooltipPropertyContractTest.value(name)); editor.setValue(initial);
                var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor(); var field = find(panel, JTextField.class, FlutterDurationReferenceEditorComponent.VALUE_NAME);
                assertFalse(text(panel).contains("ExpansionTile")); assertFalse(text(panel).contains("AnimationStyle"));
                assertTrue(text(panel).contains(name.equals("waitDurationUs") ? "hover delay" : name.equals("showDurationUs") ? "not mouse hover" : "stops hovering"));
                for (String invalid : List.of("", "NaN", "1.5", "Duration(seconds: 1)", "9007199254740992", "-9007199254740992")) {
                    field.setText(invalid); env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue());
                }
                field.setText("+1234567"); env.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(BigInteger.valueOf(1234567))), editor.getValue()); return null;
            });
        }
    }

    @Test void nestedDecorationRetainsOwningProjectAssetsAndConstraintsRetainTheirStructuredControls() throws Exception {
        onEdt(() -> {
            var descriptor = new FeatureDescriptor(); var choice = new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/tooltip.png", "Tooltip image");
            descriptor.setValue(FlutterImageAssetChoices.FEATURE_ATTRIBUTE, new FlutterImageAssetChoices(List.of(choice), Optional.empty()));
            var editor = binding("decoration").createEditor(); var initial = FlutterPropertyCellValue.explicit(TooltipPropertyContractTest.value("decoration")); editor.setValue(initial);
            var env = environment(editor, descriptor); var panel = editor.getCustomEditor(); var tabs = find(panel, JTabbedPane.class, FlutterContainerPropertyEditorComponents.DECORATION_TABS_NAME);
            assertEquals(List.of("Fill", "Image", "Border", "Radius", "Shadows", "Gradient"), java.util.stream.IntStream.range(0, tabs.getTabCount()).mapToObj(tabs::getTitleAt).toList());
            var assets = find(panel, JComboBox.class, FlutterContainerPropertyEditorComponents.DECORATION_IMAGE_ASSET_NAME);
            assertTrue(assets.getItemCount() > 0, "Nested BoxDecoration must keep the owning project's declared image choices.");
            assertTrue(java.util.stream.IntStream.range(0, assets.getItemCount()).mapToObj(assets::getItemAt).anyMatch(choice::equals));
            env.setState(PropertyEnv.STATE_VALID); assertEquals(initial, editor.getValue()); return null;
        });
        for (String name : List.of("constraints", "decoration", "padding", "margin")) onEdt(() -> { reopen(name, FlutterPropertyCellValue.explicit(TooltipPropertyContractTest.value(name))); return null; });
        onEdt(() -> {
            var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Container")).orElseThrow().property(new PropertyName("decoration")).orElseThrow();
            var existing = FlutterTypedPropertyEditors.binding(definition).orElseThrow(); assertEquals(FlutterTypedPropertyEditors.EditorKind.BOX_DECORATION, existing.editorKind());
            assertThrows(IllegalArgumentException.class, () -> existing.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
            return null;
        });
    }

    @Test void richSpanAndPositionDelegateHelpDoesNotPromiseCanvasExecutionOrInventEventActions() throws Exception {
        for (String name : List.of("richMessage", "positionDelegate", "onTriggered")) onEdt(() -> {
            var editor = binding(name).createEditor(); editor.setValue(FlutterPropertyCellValue.explicit(TooltipPropertyContractTest.value(name))); environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor();
            String help = text(panel);
            if (name.equals("richMessage")) { assertTrue(help.contains("TextSpan, WidgetSpan")); assertTrue(help.contains("last non-null")); assertTrue(help.contains("never executes")); }
            if (name.equals("positionDelegate")) { assertTrue(help.contains("Offset Function(TooltipPositionContext context)")); assertTrue(help.contains("not a native Event")); }
            if (name.equals("onTriggered")) { assertTrue(help.contains("not mouse hover or ensureTooltipVisible()")); assertTrue(help.contains("do not disable")); }
            return null;
        });
    }

    @Test void allCursorPresetsNullableBooleansAndEmptyPlainMessageReopenExactly() throws Exception {
        for (String cursor : DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets()) onEdt(() -> { reopen("mouseCursor", FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(cursor))); return null; });
        for (String name : List.of("preferBelow", "excludeFromSemantics", "enableFeedback", "ignorePointer"))
            for (var initial : List.of(FlutterPropertyCellValue.unset(), FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)), FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)))) onEdt(() -> {
                var editor = binding(name).createEditor(); editor.setValue(initial); var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor();
                assertEquals(SwingConstants.CENTER, find(panel, JCheckBox.class, FlutterNullableChoiceEditorComponent.VALUE_NAME).getHorizontalAlignment()); env.setState(PropertyEnv.STATE_VALID); assertEquals(initial, editor.getValue()); return null;
            });
        for (var value : List.of(FlutterPropertyCellValue.unset(), FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("")), FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("  line one\nline two  ")))) onEdt(() -> { reopen("message", value); return null; });
    }

    private static FlutterPropertyCellValue setReference(Component panel, String branch) {
        String root = branch.equals("getter") ? "_valueGetter" : branch.equals("member") ? "styles" : branch.equals("factory") ? "buildValue" : "_value";
        Optional<String> library = branch.equals("member") || branch.equals("factory") ? Optional.of("package:app/tooltip_values.dart") : Optional.empty();
        if (library.isPresent()) { find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME).setSelectedIndex(1); find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME).setText(library.orElseThrow()); }
        find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText(root);
        if (branch.equals("member")) find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME).setText("value");
        if (branch.equals("factory")) find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
        return FlutterPropertyCellValue.explicit(new PropertyValue.DartObjectReferenceValue(library, root, branch.equals("member") ? Optional.of("value") : Optional.empty(), branch.equals("factory") ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, branch.equals("factory") ? Optional.of(false) : Optional.empty()));
    }
    private static void reopen(String name, FlutterPropertyCellValue value) { var editor = binding(name).createEditor(); editor.setValue(value); var env = environment(editor, new FeatureDescriptor()); editor.getCustomEditor(); env.setState(PropertyEnv.STATE_VALID); assertEquals(value, editor.getValue(), name); }
    private static FlutterTypedPropertyEditors.Binding binding(String name) { return FlutterTypedPropertyEditors.binding(TooltipPropertyContractTest.DEF.property(new PropertyName(name)).orElseThrow(), TooltipWidgetPropertySchema.textStyleBinding(new PropertyName(name)), false, name.equals("onTriggered") ? List.of("noop") : name.equals("mouseCursor") ? DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets() : List.of()).orElseThrow(); }
    private static PropertyEnv environment(PropertyEditor editor, FeatureDescriptor descriptor) { var env = PropertyEnv.create(descriptor); ((ExPropertyEditor) editor).attachEnv(env); return env; }
    private static String text(Component component) { String result = component instanceof JTextArea area ? area.getText() : ""; if (component instanceof Container container) for (Component child : container.getComponents()) result += text(child); return result; }
    private static <T extends Component> T find(Component component, Class<T> type, String name) { T result = maybe(component, type, name); if (result == null) throw new AssertionError("Missing " + name); return result; }
    private static <T extends Component> T maybe(Component component, Class<T> type, String name) { if (type.isInstance(component) && name.equals(component.getName())) return type.cast(component); if (component instanceof Container container) for (Component child : container.getComponents()) { T result = maybe(child, type, name); if (result != null) return result; } return null; }
    private static <T> T onEdt(java.util.concurrent.Callable<T> operation) throws Exception { var task = new FutureTask<T>(operation); SwingUtilities.invokeAndWait(task); return task.get(); }
}
