package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class DecoratedBoxTransitionPropertyContractTest {
    @Test void requiredDecorationOffersOnlyLocalOrTypedProjectSourceAndDoesNotCommitDrafts()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(DecoratedBoxTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"decoration");assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();assertNotNull(panel);
        var modes=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
        assertEquals(2,modes.getItemCount());assertEquals(FlutterLocalDartReferenceEditorComponent.DECORATION,modes.getItemAt(0));
        assertEquals(FlutterLocalDartReferenceEditorComponent.PROJECT,modes.getItemAt(1));
        var draft=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
        assertEquals(row.getValue(),draft.validatedDraftValue());
        assertNotNull(AnimatedRotationPropertyContractTest.find(panel,FlutterContainerPropertyEditorComponents.DECORATION_TABS_NAME));
        assertNotNull(AnimatedRotationPropertyContractTest.find(panel,FlutterContainerPropertyEditorComponents.DECORATION_IMAGE_ENABLED_NAME));
        modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.PROJECT);
        assertThrows(IllegalArgumentException.class,draft::validatedDraftValue);
        modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.DECORATION);
        assertEquals(row.getValue(),draft.validatedDraftValue());assertEquals(row.getValue(),editor.getValue());assertTrue(commands.isEmpty());
        node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
        assertSame(row,TooltipPropertyContractTest.cell(node,"decoration"));
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_decoration",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        row.setValue(FlutterPropertyCellValue.explicit(ref));
        assertEquals(new SetProperty(widget.id(),new PropertyName("decoration"),ref),commands.removeFirst());
        var changed=new WidgetNode(widget.id(),widget.type(),Map.of(new PropertyName("decoration"),ref),widget.slots());
        node.refreshPresentation(changed,d,commands::add,null,null,FlutterImageAssetChoices.empty());
        editor=TooltipPropertyContractTest.cell(node,"decoration").getPropertyEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(ref));panel=editor.getCustomEditor();
        assertEquals(FlutterPropertyCellValue.explicit(ref),((FlutterPropertyEditorComponents.CommitOnValidPanel)panel).validatedDraftValue());
    }
    @Test void positionSupportsBothNativeValuesAndResetWithoutReplacingRows()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(DecoratedBoxTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);var row=TooltipPropertyContractTest.cell(node,"position");
        assertTrue(row.supportsDefaultValue());assertTrue(row.canWrite());
        for(String position:List.of("background","foreground")){
            var value=new PropertyValue.EnumValue("DecorationPosition",position);
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(),new PropertyName("position"),value),commands.removeFirst());
        }
        var changed=new WidgetNode(widget.id(),widget.type(),Map.of(
            new PropertyName("decoration"),DecoratedBoxTransitionWidgetPropertySchema.emptyDecoration(),
            new PropertyName("position"),new PropertyValue.EnumValue("DecorationPosition","foreground")),widget.slots());
        node.refreshPresentation(changed,d,commands::add,null,null,FlutterImageAssetChoices.empty());assertSame(row,TooltipPropertyContractTest.cell(node,"position"));
        row.restoreDefaultValue();
        assertEquals(new ResetProperty(widget.id(),new PropertyName("position")),commands.removeFirst());
        assertTrue(commands.isEmpty());
    }
}
