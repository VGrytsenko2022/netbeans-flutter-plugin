package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.command.*;
import java.util.*;
import java.beans.*;
import java.awt.*;
import javax.swing.*;
import org.openide.nodes.Children;
import org.openide.explorer.propertysheet.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShaderMaskPropertyContractTest {
    static <T extends Component> T find(Component root, Class<T> type, java.util.function.Predicate<T> test) {
        var queue=new ArrayDeque<Component>();queue.add(root);
        while(!queue.isEmpty()){var c=queue.removeFirst();if(type.isInstance(c)&&test.test(type.cast(c)))return type.cast(c);if(c instanceof Container parent)queue.addAll(Arrays.asList(parent.getComponents()));}
        throw new AssertionError("Missing "+type);
    }
    @Test void bothRowsKeepIdentityAndRequiredGradientHasOnlyGradientControls() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
          try {
            var d=BuiltInWidgetCatalog.getDefault().find(ShaderMaskWidgetPropertySchema.TYPE).orElseThrow();
            var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);var sets=node.getPropertySets();
            for(var field:d.properties()){
                var row=TooltipPropertyContractTest.cell(node,field.name().value());assertTrue(row.canWrite());
                PropertyValue value=field.name().value().equals("shaderCallback")?new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_shader", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()):new PropertyValue.EnumValue("BlendMode","src");
                row.setValue(FlutterPropertyCellValue.explicit(value));assertEquals(new SetProperty(widget.id(),field.name(),value),commands.removeFirst());
                var values=new LinkedHashMap<>(widget.properties());values.put(field.name(),value);widget=new WidgetNode(widget.id(),widget.type(),values,widget.slots());
                node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
                assertSame(row,TooltipPropertyContractTest.cell(node,field.name().value()));assertArrayEquals(sets,node.getPropertySets());
                assertEquals(row.getValue(),TooltipPropertyContractTest.cell(new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add),field.name().value()).getValue());
            }
            var binding=FlutterTypedPropertyEditors.binding(d.property(new PropertyName("shaderCallback")).orElseThrow()).orElseThrow();
            assertEquals(FlutterTypedPropertyEditors.EditorKind.GRADIENT_REFERENCE,binding.editorKind());
            var editor=binding.createEditor();var original=FlutterPropertyCellValue.explicit(ShaderMaskWidgetPropertySchema.neutral());editor.setValue(original);
            var env=PropertyEnv.create(new FeatureDescriptor());((ExPropertyEditor)editor).attachEnv(env);
            var panel=editor.getCustomEditor();
            var tabs=find(panel,JTabbedPane.class,t->t.getAccessibleContext().getAccessibleName().equals("Gradient settings"));
            assertEquals(1,tabs.getTabCount());assertEquals("Gradient",tabs.getTitleAt(0));
            var family=find(panel,JComboBox.class,t->"Gradient type".equals(t.getAccessibleContext().getAccessibleName()));
            assertEquals(3,family.getItemCount());
            family.setSelectedItem("Radial");assertEquals(original,editor.getValue(),"Draft must not leak before OK");
            env.setState(PropertyEnv.STATE_VALID);
            var committed=assertInstanceOf(PropertyValue.GradientValue.class,((FlutterPropertyCellValue)editor.getValue()).explicitValue().orElseThrow());
            assertInstanceOf(PropertyValue.BoxDecorationValue.RadialGradient.class,committed.gradient());
            assertEquals(ShaderMaskWidgetPropertySchema.neutral().gradient().stops(),committed.gradient().stops());
          }catch(Exception e){throw new RuntimeException(e);}
        });
    }
}
