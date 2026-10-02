package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.command.*;
import java.util.*;
import org.openide.nodes.Children;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ModalBarrierPropertyContractTest {
 @Test void allPropertiesUseTypedEditorsAndRetainRowsAcrossResetAndReopen()throws Exception{
  var d=BuiltInWidgetCatalog.getDefault().find(ModalBarrierWidgetPropertySchema.TYPE).orElseThrow();
  var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
  var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);var sets=node.getPropertySets();
  var events=Arrays.stream(sets).filter(s->s.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
  assertEquals(List.of("onDismiss"),Arrays.stream(events.getProperties()).map(p->p.getName()).toList());
  var values=new LinkedHashMap<String,List<PropertyValue>>();
  values.put("dismissible",List.of(new PropertyValue.BooleanValue(false),new PropertyValue.BooleanValue(true)));
  values.put("barrierSemanticsDismissible",List.of(new PropertyValue.NullValue(),new PropertyValue.BooleanValue(false),new PropertyValue.BooleanValue(true)));
  values.put("color",List.of(new PropertyValue.NullValue(),new PropertyValue.ColorValue(0),new PropertyValue.ColorValue(0x88223344L),ref("_color")));
  values.put("onDismiss",List.of(new PropertyValue.NullValue(),ref("_dismiss")));
  values.put("clipDetailsNotifier",List.of(new PropertyValue.NullValue(),ref("_clip")));
  for(String p:List.of("semanticsLabel","semanticsOnTapHint"))values.put(p,List.of(new PropertyValue.NullValue(),new PropertyValue.StringValue(""),new PropertyValue.StringValue("Close Діалог")));
  for(var entry:values.entrySet()){
   var row=TooltipPropertyContractTest.cell(node,entry.getKey());assertTrue(row.canWrite());assertTrue(row.supportsDefaultValue());
   for(var value:entry.getValue()){
    row.setValue(FlutterPropertyCellValue.explicit(value));assertEquals(new SetProperty(widget.id(),new PropertyName(entry.getKey()),value),commands.removeFirst());
    var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName(entry.getKey()),value);widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
    node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
    assertSame(row,TooltipPropertyContractTest.cell(node,entry.getKey()));assertArrayEquals(sets,node.getPropertySets());
    var editor=row.getPropertyEditor();editor.setValue(row.getValue());assertNotNull(editor);
    var reopen=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopen,entry.getKey()).getValue());
   }
   row.restoreDefaultValue();assertEquals(new ResetProperty(widget.id(),new PropertyName(entry.getKey())),commands.removeFirst());
  }
 }
 @Test void booleanCellsAreCenteredAndNullableBooleanPreservesNull()throws Exception{
  var d=BuiltInWidgetCatalog.getDefault().find(ModalBarrierWidgetPropertySchema.TYPE).orElseThrow();
  for(String name:List.of("dismissible","barrierSemanticsDismissible")){
   var b=FlutterTypedPropertyEditors.binding(d.property(new PropertyName(name)).orElseThrow()).orElseThrow();
   var editor=b.createEditor();assertNull(editor.getTags());assertTrue(editor.isPaintable());
   javax.swing.SwingUtilities.invokeAndWait(()->{
    editor.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)));
    var inplace=FlutterPropertyEditorComponents.inplaceFactory(b).orElseThrow().getInplaceEditor();
    inplace.connect(editor,org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor()));
    var box=assertInstanceOf(javax.swing.JCheckBox.class,inplace.getComponent());assertEquals(javax.swing.SwingConstants.CENTER,box.getHorizontalAlignment());
    box.doClick();assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)),inplace.getValue());inplace.clear();
   });
  }
 }
 private static PropertyValue.DartObjectReferenceValue ref(String value){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),value,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
}
