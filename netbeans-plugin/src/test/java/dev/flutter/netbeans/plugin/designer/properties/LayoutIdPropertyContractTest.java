package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.awt.*;
import java.beans.FeatureDescriptor;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.explorer.propertysheet.*;
import static org.junit.jupiter.api.Assertions.*;

class LayoutIdPropertyContractTest {
    static <T extends Component>T named(Component c,Class<T> type,String name){
        if(type.isInstance(c)&&name.equals(c.getName()))return type.cast(c);
        if(c instanceof Container parent)for(var child:parent.getComponents()){var result=named(child,type,name);if(result!=null)return result;}
        return null;
    }
    @Test void allRequiredIdentityModesAreCancelSafeAndRowSurvivesRefresh()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(LayoutIdWidgetPropertySchema.TYPE).orElseThrow();
        var initial=WidgetNodePrototypeFactory.create(d,StableId.random());
        var commands=new ArrayList<DesignerCommand>();var node=new FlutterWidgetPropertiesNode(Children.LEAF,initial,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"id");var sets=node.getPropertySets();
        assertFalse(row.supportsDefaultValue());assertTrue(row.canWrite());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.OBJECT_TAG,FlutterTypedPropertyEditors.binding(d.properties().getFirst()).orElseThrow().editorKind());
        var values=List.<PropertyValue>of(new PropertyValue.StringValue(""),new PropertyValue.IntegerValue(java.math.BigInteger.ONE),
                new PropertyValue.DoubleValue(new java.math.BigDecimal("1.25")),new PropertyValue.BooleanValue(true),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_identity",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty()));
        var modes=List.of(FlutterObjectTagEditorComponent.STRING,FlutterObjectTagEditorComponent.INTEGER,FlutterObjectTagEditorComponent.DOUBLE,
                FlutterObjectTagEditorComponent.BOOLEAN,FlutterObjectTagEditorComponent.PROJECT);
        for(int i=0;i<values.size();i++){
            var value=values.get(i);var mode=modes.get(i);var editor=row.getPropertyEditor();editor.setValue(FlutterPropertyCellValue.explicit(value));
            SwingUtilities.invokeAndWait(()->{
                var env=PropertyEnv.create(new FeatureDescriptor());((ExPropertyEditor)editor).attachEnv(env);var panel=editor.getCustomEditor();
                var select=named(panel,JComboBox.class,FlutterObjectTagEditorComponent.MODE_NAME);
                assertNotNull(select);assertEquals(5,select.getItemCount());assertEquals(mode,select.getSelectedItem());
                for(int n=0;n<select.getItemCount();n++){assertNotEquals(FlutterObjectTagEditorComponent.NONE,select.getItemAt(n));assertNotEquals(FlutterObjectTagEditorComponent.OMIT,select.getItemAt(n));}
                select.setSelectedItem(FlutterObjectTagEditorComponent.STRING);named(panel,JTextArea.class,FlutterObjectTagEditorComponent.STRING_NAME).setText("changed");
                assertEquals(FlutterPropertyCellValue.explicit(value),editor.getValue());
                env.setState(PropertyEnv.STATE_VALID);
                assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("changed")),editor.getValue());
            });
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(initial.id(),new PropertyName("id"),value),commands.removeFirst());
            var updated=new WidgetNode(initial.id(),initial.type(),Map.of(new PropertyName("id"),value),initial.slots());
            node.refreshPresentation(updated,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"id"));assertArrayEquals(sets,node.getPropertySets());
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,updated,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"id").getValue());
        }
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }
}
