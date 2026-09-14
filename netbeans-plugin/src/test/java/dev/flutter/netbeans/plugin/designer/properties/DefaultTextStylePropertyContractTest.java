package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class DefaultTextStylePropertyContractTest {
 static final List<WidgetTypeId> TYPES=List.of(DefaultTextStyleWidgetPropertySchema.TYPE,DefaultTextStyleWidgetPropertySchema.MERGE_TYPE);
 @Test void all82RowsStayWritableAndSourceEditorHonorsRequiredVsNullable()throws Exception{
   for(var type:TYPES){
     var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
     var w=WidgetNodePrototypeFactory.create(d,StableId.random());
     var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,ignored->{});
     for(var p:d.properties()){var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite());assertNotNull(row.getPropertyEditor());}
     var row=TooltipPropertyContractTest.cell(node,"style");
     assertEquals(type.equals(TYPES.getLast()),row.supportsDefaultValue());
     var editor=row.getPropertyEditor();editor.setValue(row.getValue());assertNotNull(editor.getCustomEditor());
     if(type.equals(TYPES.getFirst()))assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
   }
 }
 @Test void referenceNullOmissionAndLocalSwitchAtomicallyRetainPropertyRows()throws Exception{
   for(var type:TYPES){
     var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
     for(var target:List.of(FlutterPropertyCellValue.explicit(AnimatedDefaultTextStylePropertyContractTest.ref("_style")),
         FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),FlutterPropertyCellValue.unset())){
       if(type.equals(TYPES.getFirst())&&!(target.explicitValue().orElse(null) instanceof PropertyValue.DartObjectReferenceValue))continue;
       var properties=new LinkedHashMap<PropertyName,PropertyValue>();
       properties.put(new PropertyName("style"),new PropertyValue.StringValue("local"));
       properties.put(new PropertyName("styleFontSize"),new PropertyValue.DoubleValue(BigDecimal.TEN));
       var w=new WidgetNode(StableId.random(),type,properties,Map.of());var commands=new ArrayList<DesignerCommand>();
       var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);var row=TooltipPropertyContractTest.cell(node,"style");var sets=node.getPropertySets();
       row.setValue(target);
       var command=assertInstanceOf(PatchProperties.class,commands.removeFirst());
       assertTrue(command.patches().contains(new PatchProperties.ResetPatch(new PropertyName("styleFontSize"))));assertEquals(2,command.patches().size());
       var changed=new LinkedHashMap<PropertyName,PropertyValue>();target.explicitValue().ifPresent(v->changed.put(new PropertyName("style"),v));
       w=new WidgetNode(w.id(),type,changed,w.slots());node.refreshPresentation(w,d,commands::add,null,null,FlutterImageAssetChoices.empty());
       assertSame(row,TooltipPropertyContractTest.cell(node,"style"));assertArrayEquals(sets,node.getPropertySets());
       TooltipPropertyContractTest.cell(node,"styleFontSize").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(BigDecimal.TEN)));
       assertEquals(2,assertInstanceOf(PatchProperties.class,commands.removeFirst()).patches().size());
       var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);
       assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"style").getValue());
     }
   }
 }
}
