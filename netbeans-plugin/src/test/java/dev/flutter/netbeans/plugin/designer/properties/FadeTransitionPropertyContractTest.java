package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class FadeTransitionPropertyContractTest {
    @Test void animationEditorHasOnlyTwoSourcesAndStagesDraftsUntilCommit() throws Exception {
        for(var type:List.of(FadeTransitionWidgetPropertySchema.TYPE,FadeTransitionWidgetPropertySchema.SLIVER_TYPE)){
            var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,WidgetNodePrototypeFactory.create(d,StableId.random()),d,ignored->{});
            var row=TooltipPropertyContractTest.cell(node,"opacity");assertTrue(row.canWrite());
            var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
            var modes=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
            assertEquals(2,modes.getItemCount());assertEquals(FlutterLocalDartReferenceEditorComponent.NUMBER,modes.getItemAt(0));
            assertEquals(FlutterLocalDartReferenceEditorComponent.PROJECT,modes.getItemAt(1));
            var field=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,FlutterNullableNumberEditorComponent.VALUE_NAME);
            var draft=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
            for(String value:List.of("0","0.5","1")){field.setText(value);assertTrue(draft.validatedDraftValue().isExplicit());}
            for(String value:List.of("-0.1","1.01","NaN","Infinity","1e309","","1+1")){
                field.setText(value);assertThrows(IllegalArgumentException.class,draft::validatedDraftValue);
            }
            modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.PROJECT);
            var root=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            root.setText("_fade");
            assertInstanceOf(PropertyValue.DartObjectReferenceValue.class,draft.validatedDraftValue().explicitValue().orElseThrow());
            assertEquals(row.getValue(),editor.getValue(),"Cancel never publishes drafts");
        }
    }
    @Test void requiredAnimationEditorAndNullableCheckboxRemainEditableAcrossRefreshAndReopen() throws Exception {
        for(var type:List.of(FadeTransitionWidgetPropertySchema.TYPE,FadeTransitionWidgetPropertySchema.SLIVER_TYPE)) {
            var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
            var w=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);
            var opacity=TooltipPropertyContractTest.cell(node,"opacity");var sets=node.getPropertySets();
            assertEquals("Opacity animation",opacity.getDisplayName());assertFalse(opacity.supportsDefaultValue());
            var editor=opacity.getPropertyEditor();editor.setValue(opacity.getValue());assertNotNull(editor.getCustomEditor());
            var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_fade",Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
            opacity.setValue(FlutterPropertyCellValue.explicit(ref));
            assertEquals(ref,assertInstanceOf(SetProperty.class,commands.removeFirst()).value());
            var updated=new WidgetNode(w.id(),type,Map.of(new PropertyName("opacity"),ref),w.slots());
            node.refreshPresentation(updated,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(opacity,TooltipPropertyContractTest.cell(node,"opacity"));assertArrayEquals(sets,node.getPropertySets());
            var bool=TooltipPropertyContractTest.cell(node,"alwaysIncludeSemantics");
            assertTrue(bool.supportsDefaultValue());assertNotNull(bool.getPropertyEditor());
            bool.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)));assertEquals(new PropertyValue.BooleanValue(true),assertInstanceOf(SetProperty.class,commands.removeFirst()).value());
            var flagged=new WidgetNode(w.id(),type,Map.of(new PropertyName("opacity"),ref,new PropertyName("alwaysIncludeSemantics"),new PropertyValue.BooleanValue(true)),w.slots());
            node.refreshPresentation(flagged,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            var checkEditor=bool.getPropertyEditor();checkEditor.setValue(bool.getValue());assertTrue(checkEditor.isPaintable());assertNull(checkEditor.getTags());
            bool.restoreDefaultValue();assertInstanceOf(ResetProperty.class,commands.removeFirst());
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,updated,d,commands::add);
            assertTrue(TooltipPropertyContractTest.cell(reopened,"opacity").canWrite());
            assertEquals(opacity.getValue(),TooltipPropertyContractTest.cell(reopened,"opacity").getValue());
        }
    }
}
