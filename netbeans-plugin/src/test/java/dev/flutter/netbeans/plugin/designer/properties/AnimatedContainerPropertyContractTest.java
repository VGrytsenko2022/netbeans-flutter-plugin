package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class AnimatedContainerPropertyContractTest {
    @Test void completeCurveDurationAndCallbackEditorsAreWritableAndTyped() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedContainerWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var curve=TooltipPropertyContractTest.cell(node,"curve");
        var editor=curve.getPropertyEditor();editor.setValue(curve.getValue());
        var queue=new ArrayDeque<java.awt.Component>();queue.add(editor.getCustomEditor());javax.swing.JComboBox<?> presets=null;
        while(!queue.isEmpty()){
            var c=queue.removeFirst();
            if(FlutterPresetDartReferenceEditorComponent.PRESET_NAME.equals(c.getName())) presets=(javax.swing.JComboBox<?>)c;
            if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));
        }
        assertNotNull(presets);assertEquals(43,presets.getItemCount());
        for(String preset:ExpansionTileWidgetPropertySchema.curvePresets()){
            curve.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(preset)));
            assertEquals(new SetProperty(widget.id(),new PropertyName("curve"),new PropertyValue.StringValue(preset)),commands.removeFirst());
        }
        var duration=TooltipPropertyContractTest.cell(node,"durationUs");assertFalse(duration.supportsDefaultValue());
        editor=duration.getPropertyEditor();editor.setValue(duration.getValue());assertNotNull(editor.getCustomEditor());
        for(long us:List.of(0L,1L,300000L)){
            var value=new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(us));
            duration.setValue(FlutterPropertyCellValue.explicit(value));
            if(us==300000L) assertTrue(commands.isEmpty());
            else assertEquals(new SetProperty(widget.id(),new PropertyName("durationUs"),value),commands.removeFirst());
        }
        var end=TooltipPropertyContractTest.cell(node,"onEnd");assertTrue(end.canWrite());assertTrue(end.supportsDefaultValue());
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_handleEnd",Optional.empty(),
            PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        end.setValue(FlutterPropertyCellValue.explicit(ref));
        assertEquals(new SetProperty(widget.id(),new PropertyName("onEnd"),ref),commands.removeFirst());
        assertTrue(commands.isEmpty());
    }


    @Test void allComplexRowsHaveNullableLocalAndReferenceEditors() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedContainerWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        for(String name:List.of("alignment","padding","color","decoration","foregroundDecoration","width","height","constraints","margin","transform","transformAlignment")){
            var row=TooltipPropertyContractTest.cell(node,name);assertTrue(row.canWrite());assertTrue(row.supportsDefaultValue());
            var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();assertNotNull(panel,name);
            var modes=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
            assertNotNull(modes,name);assertEquals(4,modes.getItemCount(),name);
            modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.NULL);
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),((FlutterPropertyEditorComponents.CommitOnValidPanel)panel).validatedDraftValue());
            assertEquals(row.getValue(),editor.getValue(),"Dialog must not commit a draft");
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,name));
        }
    }
    @Test void widthLocalModeAcceptsOnlyReviewedInfinityAndMatrixDraftsRemainLocal() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedContainerWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,ignored->{});
        var row=TooltipPropertyContractTest.cell(node,"width");
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
        var modes=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
        modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.NUMBER);
        var field=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,FlutterNullableNumberEditorComponent.VALUE_NAME);
        var draft=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
        field.setText("Infinity");
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double","infinity")),draft.validatedDraftValue());
        assertEquals(row.getValue(),editor.getValue());
        for(String invalid:List.of("-Infinity","NaN","-1","1e999","someDart()")){
            field.setText(invalid);assertThrows(IllegalArgumentException.class,draft::validatedDraftValue,invalid);
        }
        row=TooltipPropertyContractTest.cell(node,"transform");editor=row.getPropertyEditor();editor.setValue(row.getValue());panel=editor.getCustomEditor();
        modes=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
        modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.MATRIX);
        var table=(javax.swing.JPanel)AnimatedRotationPropertyContractTest.find(panel,FlutterContainerPropertyEditorComponents.MATRIX_TABLE_NAME);
        assertNotNull(table);
        var matrixField=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,"flutter.container.matrix.r0c3");
        matrixField.setText("12");
        assertInstanceOf(PropertyValue.Matrix4Value.class,((FlutterPropertyEditorComponents.CommitOnValidPanel)panel).validatedDraftValue().explicitValue().orElseThrow());
        assertEquals(row.getValue(),editor.getValue());
    }
    @Test void clippingUsesColorAndSwitchingBackgroundIsOneAtomicCommand() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedContainerWidgetPropertySchema.TYPE).orElseThrow();
        var initial=WidgetNodePrototypeFactory.create(d,StableId.random());
        var properties=new LinkedHashMap<>(initial.properties());
        properties.put(new PropertyName("color"),new PropertyValue.ColorValue(0xff123456L));
        properties.put(new PropertyName("clipBehavior"),new PropertyValue.EnumValue("Clip","hardEdge"));
        var widget=new WidgetNode(initial.id(),initial.type(),properties,initial.slots());
        var commands=new ArrayList<DesignerCommand>();var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_decoration",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        TooltipPropertyContractTest.cell(node,"decoration").setValue(FlutterPropertyCellValue.explicit(ref));
        assertEquals(new PatchProperties(widget.id(),List.of(new PatchProperties.ResetPatch(new PropertyName("color")),
            new PatchProperties.SetPatch(new PropertyName("decoration"),ref))),commands.removeFirst());
        TooltipPropertyContractTest.cell(node,"color").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
        assertEquals(new PatchProperties(widget.id(),List.of(new PatchProperties.ResetPatch(new PropertyName("clipBehavior")),
            new PatchProperties.SetPatch(new PropertyName("color"),new PropertyValue.NullValue()))),commands.removeFirst());
    }
}
