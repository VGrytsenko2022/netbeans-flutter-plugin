package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class MatrixTransitionPropertyContractTest {
    @Test void requiredAnimationEditorHasOnlyLocalAndReferenceModesAndStableRows() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(MatrixTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"animation");assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
        var queue=new ArrayDeque<java.awt.Component>();queue.add(panel);javax.swing.JComboBox<?> modes=null;
        while(!queue.isEmpty()){var c=queue.removeFirst();if(FlutterLocalDartReferenceEditorComponent.MODE_NAME.equals(c.getName()))modes=(javax.swing.JComboBox<?>)c;
            if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));}
        assertNotNull(modes);assertEquals(2,modes.getItemCount());
        for(var x:List.of(new java.math.BigDecimal("-2.5"),java.math.BigDecimal.ONE)){
            PropertyValue value=new PropertyValue.DoubleValue(x);
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(),new PropertyName("animation"),value),commands.removeFirst());
            var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName("animation"),value);
            widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"animation"));
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"animation").getValue());
        }
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_animation",Optional.empty(),
            PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        row.setValue(FlutterPropertyCellValue.explicit(ref));assertEquals(new SetProperty(widget.id(),new PropertyName("animation"),ref),commands.removeFirst());
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }
    @Test void alignmentIsPhysicalOnlyAndQualityRetainsOmittedNullAndAllValues() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(MatrixTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"alignment");
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
        var basis=(javax.swing.JComboBox<?>)find(panel,FlutterContainerPropertyEditorComponents.ALIGNMENT_BASIS_NAME);
        assertEquals(1,basis.getItemCount());assertEquals(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,basis.getItemAt(0));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.AlignmentGeometryValue(
            PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO))));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        var quality=TooltipPropertyContractTest.cell(node,"filterQuality");assertTrue(quality.supportsDefaultValue());
        var sets=node.getPropertySets();
        for(String v:List.of("none","low","medium","high","null")){
            var value=v.equals("null")?new PropertyValue.NullValue():new PropertyValue.EnumValue("FilterQuality",v);
            quality.setValue(FlutterPropertyCellValue.explicit(value));assertEquals(new SetProperty(widget.id(),new PropertyName("filterQuality"),value),commands.removeFirst());
            var updated=new LinkedHashMap<>(widget.properties());updated.put(new PropertyName("filterQuality"),value);
            var snapshot=new WidgetNode(widget.id(),widget.type(),updated,widget.slots());
            node.refreshPresentation(snapshot,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(quality,TooltipPropertyContractTest.cell(node,"filterQuality"));assertArrayEquals(sets,node.getPropertySets());
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,snapshot,d,commands::add);
            assertEquals(quality.getValue(),TooltipPropertyContractTest.cell(reopened,"filterQuality").getValue());
        }
        quality.restoreDefaultValue();assertEquals(new ResetProperty(widget.id(),new PropertyName("filterQuality")),commands.removeFirst());
        var animation=TooltipPropertyContractTest.cell(node,"animation");editor=animation.getPropertyEditor();editor.setValue(animation.getValue());
        panel=editor.getCustomEditor();
        var numericMode=(javax.swing.JComboBox<?>)find(panel,FlutterNullableNumberEditorComponent.MODE_NAME);
        assertEquals(1,numericMode.getItemCount());assertEquals(FlutterNullableNumberEditorComponent.NUMBER,numericMode.getItemAt(0));
        var field=(javax.swing.JTextField)find(panel,FlutterNullableNumberEditorComponent.VALUE_NAME);
        field.setText("-2.5");
        var commit=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new java.math.BigDecimal("-2.5"))),commit.validatedDraftValue());
        assertEquals(animation.getValue(),editor.getValue(),"Draft must not publish into the cell");
        for(String invalid:List.of("NaN","Infinity","null","","someDart()")){
            field.setText(invalid);assertThrows(IllegalArgumentException.class,commit::validatedDraftValue,invalid);
        }
    }
    @Test void transformMatrixDraftsSourceModesAndRowsStayStable()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(MatrixTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"onTransform");assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
        var mode=(javax.swing.JComboBox<?>)find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
        assertEquals(2,mode.getItemCount());assertEquals(FlutterLocalDartReferenceEditorComponent.MATRIX,mode.getSelectedItem());
        var expected=new ArrayList<>(MatrixTransitionWidgetPropertySchema.identity().storage());
        for(int r=0;r<4;r++)for(int c=0;c<4;c++){
            String number=String.valueOf(r*4+c-8);
            ((javax.swing.JTextField)find(panel,"flutter.container.matrix.r"+r+"c"+c)).setText(number);
            expected.set(c*4+r,new java.math.BigDecimal(number));
        }
        var draft=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.Matrix4Value(expected)),draft.validatedDraftValue());
        mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.PROJECT);
        assertThrows(IllegalArgumentException.class,draft::validatedDraftValue);
        mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.MATRIX);
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.Matrix4Value(expected)),draft.validatedDraftValue());
        var first=(javax.swing.JTextField)find(panel,"flutter.container.matrix.r0c0");
        first.setText("NaN");assertThrows(IllegalArgumentException.class,draft::validatedDraftValue);first.setText("-8");
        assertEquals(row.getValue(),editor.getValue());assertTrue(commands.isEmpty());
        var sets=node.getPropertySets();
        var source=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_compute",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        for(PropertyValue value:List.of(new PropertyValue.Matrix4Value(expected),source,MatrixTransitionWidgetPropertySchema.identity())){
            row.setValue(FlutterPropertyCellValue.explicit(value));assertEquals(new SetProperty(widget.id(),new PropertyName("onTransform"),value),commands.removeFirst());
            var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName("onTransform"),value);widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"onTransform"));assertArrayEquals(sets,node.getPropertySets());
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add),"onTransform").getValue());
        }
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertTrue(Arrays.stream(node.getPropertySets()).noneMatch(set->set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)));
    }
    @Test void delegateEditorKeepsLocalMatrixAndSourceActionsWithoutInventingAnEvent()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(MatrixTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,c->fail("Use atomic context actions"));
        var context=new CallbackContext();node.updateEventsContext(context);
        javax.swing.SwingUtilities.invokeAndWait(()->{
            try{
                var row=TooltipPropertyContractTest.cell(node,"onTransform");var editor=row.getPropertyEditor();
                assertInstanceOf(FlutterWidgetEventPropertyEditor.class,editor);editor.setValue(row.getValue());
                var panel=editor.getCustomEditor();
                for(String name:List.of(FlutterWidgetEventPropertyEditor.CREATE_NAME,FlutterWidgetEventPropertyEditor.BIND_NAME,
                    FlutterWidgetEventPropertyEditor.NAVIGATE_NAME,FlutterWidgetEventPropertyEditor.RENAME_NAME,
                    FlutterWidgetEventPropertyEditor.DISCONNECT_NAME,FlutterWidgetEventPropertyEditor.REFERENCE_APPLY_NAME))
                    assertNotNull(find(panel,name),name);
                var guidance=(javax.swing.JLabel)find(panel,FlutterWidgetEventPropertyEditor.RETURN_GUIDANCE_NAME);
                assertTrue(guidance.getText().contains("Matrix4.identity()"));assertFalse(guidance.getText().contains("UnimplementedError"));
                assertNotNull(find(panel,"flutter.container.matrix.r0c0"));
                assertEquals(0,context.changes);
                ((javax.swing.JTextField)find(panel,"flutter.container.matrix.r0c3")).setText("24");
                ((javax.swing.JButton)find(panel,FlutterWidgetEventPropertyEditor.REFERENCE_APPLY_NAME)).doClick();
                assertEquals(1,context.changes);assertEquals(new PropertyName("onTransform"),context.property);
                var matrix=(PropertyValue.Matrix4Value)context.value.orElseThrow();
                assertEquals(new java.math.BigDecimal("24"),matrix.storage().get(12));
                assertTrue(Arrays.stream(node.getPropertySets()).noneMatch(s->s.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)));
            }catch(Exception e){throw new AssertionError(e);}
        });
    }
    private static class CallbackContext implements FlutterWidgetEventsContext{
        int changes;PropertyName property;Optional<PropertyValue> value=Optional.empty();
        public java.util.concurrent.CompletionStage<List<Handler>> discover(PropertyName p){return java.util.concurrent.CompletableFuture.completedFuture(List.of());}
        public java.util.concurrent.CompletionStage<Void> create(PropertyName p,String n){return changed(p);}
        public java.util.concurrent.CompletionStage<Void> bind(PropertyName p,Handler h){return changed(p);}
        public java.util.concurrent.CompletionStage<Void> editBinding(PropertyName p,Optional<PropertyValue> v){value=v;return changed(p);}
        public java.util.concurrent.CompletionStage<Void> navigate(PropertyName p){return java.util.concurrent.CompletableFuture.completedFuture(null);}
        public java.util.concurrent.CompletionStage<Void> rename(PropertyName p,String n){return changed(p);}
        public java.util.concurrent.CompletionStage<Void> disconnect(PropertyName p){return changed(p);}
        private java.util.concurrent.CompletionStage<Void> changed(PropertyName p){property=p;changes++;return java.util.concurrent.CompletableFuture.completedFuture(null);}
    }
    static java.awt.Component find(java.awt.Component root,String name){
        if(name.equals(root.getName()))return root;
        if(root instanceof java.awt.Container parent)for(var child:parent.getComponents()){
            var found=find(child,name);if(found!=null)return found;
        }
        return null;
    }

}
