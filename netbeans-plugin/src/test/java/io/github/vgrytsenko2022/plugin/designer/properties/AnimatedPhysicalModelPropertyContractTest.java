package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedPhysicalModelPropertyContractTest {
 @Test void everyRowWritableAndPhysicalRadiusDraftReferenceUnionPreservesRows()throws Exception{
    var d=BuiltInWidgetCatalog.getDefault().find(AnimatedPhysicalModelWidgetPropertySchema.TYPE).orElseThrow();
    var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
    var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
    for(var p:d.properties()){var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite());assertNotNull(row.getPropertyEditor());}
    for(String name:List.of("color","shadowColor","durationUs"))assertFalse(TooltipPropertyContractTest.cell(node,name).supportsDefaultValue());
    var radius=TooltipPropertyContractTest.cell(node,"borderRadius");var editor=radius.getPropertyEditor();editor.setValue(radius.getValue());
    var panel=editor.getCustomEditor();var modes=(JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
    assertEquals(4,modes.getItemCount());modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.RADIUS);
    var basis=(JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterContainerPropertyEditorComponents.BORDER_RADIUS_BASIS_NAME);assertEquals(1,basis.getItemCount());
    var table=(JTable)AnimatedRotationPropertyContractTest.find(panel,FlutterContainerPropertyEditorComponents.BORDER_RADIUS_TABLE_NAME);assertEquals(4,table.getRowCount());
    var draft=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
    for(int i=0;i<4;i++){table.setValueAt(""+(i+1),i,1);table.setValueAt(""+(i+2),i,2);}
    assertInstanceOf(PropertyValue.BorderRadiusValue.class,draft.validatedDraftValue().explicitValue().orElseThrow());
    assertEquals(radius.getValue(),editor.getValue(),"Cancel keeps the uncommitted original");
    for(String bad:List.of("-1","NaN","Infinity","1e309","Radius.circular(2)")){
       table.setValueAt(bad,0,1);assertThrows(IllegalArgumentException.class,draft::validatedDraftValue,bad);
    }
    modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.NULL);
    assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),draft.validatedDraftValue());
    var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_radius",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
    for(PropertyValue v:List.of(new PropertyValue.NullValue(),ref)){
       radius.setValue(FlutterPropertyCellValue.explicit(v));assertEquals(new SetProperty(widget.id(),new PropertyName("borderRadius"),v),commands.removeFirst());
       var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName("borderRadius"),v);
       widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
       assertSame(radius,TooltipPropertyContractTest.cell(node,"borderRadius"));
       var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
       assertEquals(radius.getValue(),TooltipPropertyContractTest.cell(reopened,"borderRadius").getValue());
    }
    var curve=TooltipPropertyContractTest.cell(node,"curve");editor=curve.getPropertyEditor();editor.setValue(curve.getValue());
    var presets=(JComboBox<?>)AnimatedRotationPropertyContractTest.find(editor.getCustomEditor(),FlutterPresetDartReferenceEditorComponent.PRESET_NAME);assertEquals(43,presets.getItemCount());
    for(String name:List.of("animateColor","animateShadowColor"))for(boolean b:List.of(false,true)){
       var row=TooltipPropertyContractTest.cell(node,name);row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(b)));
       assertEquals(new SetProperty(widget.id(),new PropertyName(name),new PropertyValue.BooleanValue(b)),commands.removeFirst());
       assertNull(row.getPropertyEditor().getTags());
    }
 }
}

