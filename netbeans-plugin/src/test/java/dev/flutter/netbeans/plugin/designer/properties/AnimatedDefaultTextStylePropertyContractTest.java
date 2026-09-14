package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import java.math.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedDefaultTextStylePropertyContractTest {
 static final WidgetDefinition D=BuiltInWidgetCatalog.getDefault().find(AnimatedDefaultTextStyleWidgetPropertySchema.TYPE).orElseThrow();
 static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
 @Test void everyRowHasTypedWritableEditorAndMaxLinesDraftsAreExact()throws Exception{
    var widget=WidgetNodePrototypeFactory.create(D,StableId.random());var commands=new ArrayList<DesignerCommand>();
    var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,D,commands::add);
    for(var p:D.properties()){
        var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite(),p.name().value());
        assertNotNull(row.getPropertyEditor());
    }
    var row=TooltipPropertyContractTest.cell(node,"maxLines");var editor=row.getPropertyEditor();editor.setValue(row.getValue());
    var panel=editor.getCustomEditor();
    var mode=(JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
    assertEquals(4,mode.getItemCount());mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.NUMBER);
    var input=(JTextField)AnimatedRotationPropertyContractTest.find(panel,FlutterNullableNumberEditorComponent.VALUE_NAME);
    var commit=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
    assertEquals("1",input.getText());
    for(String value:List.of("1","2","9007199254740991")){
       input.setText(value);assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(new BigInteger(value))),commit.validatedDraftValue());
       assertEquals(row.getValue(),editor.getValue());
    }
    for(String value:List.of("0","-1","1.5","9007199254740992","int.parse('2')","")){
       input.setText(value);assertThrows(IllegalArgumentException.class,commit::validatedDraftValue,value);
    }
    mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.NULL);
    assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),commit.validatedDraftValue());
    assertTrue(commands.isEmpty());
    var curve=TooltipPropertyContractTest.cell(node,"curve");editor=curve.getPropertyEditor();editor.setValue(curve.getValue());
    var presets=(JComboBox<?>)AnimatedRotationPropertyContractTest.find(editor.getCustomEditor(),FlutterPresetDartReferenceEditorComponent.PRESET_NAME);
    assertEquals(43,presets.getItemCount());
 }
 @Test void colorAndPaintPeersSwitchInOnePatch()throws Exception{
    var paint=new PropertyValue.PaintValue(new ColorSource.Literal(0xff112233L),
       PropertyValue.PaintValue.BlendMode.SRC_OVER,PropertyValue.PaintValue.Style.FILL,BigDecimal.ZERO,
       PropertyValue.PaintValue.StrokeCap.BUTT,PropertyValue.PaintValue.StrokeJoin.MITER,BigDecimal.valueOf(4),
       true,PropertyValue.PaintValue.FilterQuality.NONE,false,Optional.empty());
    for(var pair:List.of(List.of("styleColor","styleForeground"),List.of("styleBackgroundColor","styleBackground"))){
       for(boolean toPaint:List.of(false,true)){
          var p=new LinkedHashMap<>(WidgetNodePrototypeFactory.create(D,StableId.random()).properties());
          var oldName=new PropertyName(pair.get(toPaint?0:1));var newName=new PropertyName(pair.get(toPaint?1:0));
          var color=new PropertyValue.ColorValue(0xff223344L);
          p.put(oldName,toPaint?color:paint);
          var widget=new WidgetNode(StableId.random(),D.typeId(),p,Map.of());var commands=new ArrayList<DesignerCommand>();
          var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,D,commands::add);
          TooltipPropertyContractTest.cell(node,newName.value()).setValue(FlutterPropertyCellValue.explicit(toPaint?paint:color));
          var command=assertInstanceOf(PatchProperties.class,commands.removeFirst());
          assertEquals(Set.of(new PatchProperties.ResetPatch(oldName),new PatchProperties.SetPatch(newName,toPaint?paint:color)),Set.copyOf(command.patches()));
       }
    }
 }
 @Test void wholeAndLocalSwitchesAreAtomicAndRefreshRetainsRows()throws Exception{
    var p=new LinkedHashMap<>(WidgetNodePrototypeFactory.create(D,StableId.random()).properties());
    p.put(new PropertyName("styleFontSize"),new PropertyValue.DoubleValue(BigDecimal.valueOf(20)));
    p.put(new PropertyName("styleColor"),new PropertyValue.ColorValue(0xff112233L));
    var widget=new WidgetNode(StableId.random(),D.typeId(),p,Map.of());var commands=new ArrayList<DesignerCommand>();
    var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,D,commands::add);
    var row=TooltipPropertyContractTest.cell(node,"style");
    row.setValue(FlutterPropertyCellValue.explicit(ref("_style")));
    var command=assertInstanceOf(PatchProperties.class,commands.removeFirst());
    assertEquals(Set.of(new PatchProperties.SetPatch(new PropertyName("style"),ref("_style")),
       new PatchProperties.ResetPatch(new PropertyName("styleFontSize")),new PatchProperties.ResetPatch(new PropertyName("styleColor"))),Set.copyOf(command.patches()));
    p=new LinkedHashMap<>(Map.of(new PropertyName("style"),ref("_style"),new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(300000))));
    widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());node.refreshPresentation(widget,D,commands::add,null,null,FlutterImageAssetChoices.empty());
    assertSame(row,TooltipPropertyContractTest.cell(node,"style"));
    TooltipPropertyContractTest.cell(node,"styleFontSize").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(BigDecimal.TEN)));
    command=assertInstanceOf(PatchProperties.class,commands.removeFirst());
    assertEquals(Set.of(new PatchProperties.SetPatch(new PropertyName("style"),new PropertyValue.StringValue("local")),
       new PatchProperties.SetPatch(new PropertyName("styleFontSize"),new PropertyValue.DoubleValue(BigDecimal.TEN))),Set.copyOf(command.patches()));
    var height=TooltipPropertyContractTest.cell(node,"textHeightBehavior");
    height.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
    assertInstanceOf(SetProperty.class,commands.removeFirst());
    p.put(new PropertyName("textHeightBehavior"),new PropertyValue.NullValue());
    widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());node.refreshPresentation(widget,D,commands::add,null,null,FlutterImageAssetChoices.empty());
    TooltipPropertyContractTest.cell(node,"textHeightApplyFirstAscent").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)));
    assertEquals(2,assertInstanceOf(PatchProperties.class,commands.removeFirst()).patches().size());
    var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,D,commands::add);
    assertEquals(height.getValue(),TooltipPropertyContractTest.cell(reopened,"textHeightBehavior").getValue());
 }
}
