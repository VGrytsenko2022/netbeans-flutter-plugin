package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class AnimatedSizePropertyContractTest {
    @Test void completeCurveDurationAndCallbackEditorsAreWritableAndTyped() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedSizeWidgetPropertySchema.TYPE).orElseThrow();
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


    @Test void reverseDurationHasFourModesExactDraftValidationAndStableRows() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedSizeWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        for(String name:List.of("durationUs","reverseDurationUs")){
            var row=TooltipPropertyContractTest.cell(node,name);assertTrue(row.canWrite());
            var editor=row.getPropertyEditor();editor.setValue(row.getValue());
            var panel=editor.getCustomEditor();
            var modes=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterDurationReferenceEditorComponent.MODE_NAME);
            assertNotNull(modes);assertEquals(name.equals("durationUs")?2:4,modes.getItemCount());
            modes.setSelectedItem(FlutterDurationReferenceEditorComponent.LITERAL);
            var input=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,FlutterDurationReferenceEditorComponent.VALUE_NAME);
            var commit=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
            for(String value:List.of("0","1","400000","9007199254740991")){
                input.setText(value);
                assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(new java.math.BigInteger(value))),commit.validatedDraftValue());
                assertEquals(row.getValue(),editor.getValue(),"Cancel/draft must not publish");
            }
            for(String bad:List.of("-1","9007199254740992","1.5","","null","Duration()")){
                input.setText(bad);assertThrows(IllegalArgumentException.class,commit::validatedDraftValue,bad);
            }
        }
        var reverse=TooltipPropertyContractTest.cell(node,"reverseDurationUs");
        assertTrue(reverse.supportsDefaultValue());
        for(PropertyValue v:List.of(new PropertyValue.NullValue(),new PropertyValue.IntegerValue(java.math.BigInteger.ZERO),
            new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_reverse",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty()))){
            reverse.setValue(FlutterPropertyCellValue.explicit(v));
            assertEquals(new SetProperty(widget.id(),new PropertyName("reverseDurationUs"),v),commands.removeFirst());
            var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName("reverseDurationUs"),v);
            widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(reverse,TooltipPropertyContractTest.cell(node,"reverseDurationUs"));
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(reverse.getValue(),TooltipPropertyContractTest.cell(reopened,"reverseDurationUs").getValue());
        }
        reverse.restoreDefaultValue();assertEquals(new ResetProperty(widget.id(),new PropertyName("reverseDurationUs")),commands.removeFirst());
    }
    @Test void alignmentSupportsBothBasesAndClipSupportsEveryNativeMode() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedSizeWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"alignment");
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());
        var basis=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(editor.getCustomEditor(),FlutterContainerPropertyEditorComponents.ALIGNMENT_BASIS_NAME);
        assertEquals(2,basis.getItemCount());
        for(var b:PropertyValue.AlignmentGeometryValue.HorizontalBasis.values()){
            var value=new PropertyValue.AlignmentGeometryValue(b,new java.math.BigDecimal("-2"),new java.math.BigDecimal("3"));
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(),new PropertyName("alignment"),value),commands.removeFirst());
        }
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        var clip=TooltipPropertyContractTest.cell(node,"clipBehavior");
        for(String c:List.of("none","hardEdge","antiAlias","antiAliasWithSaveLayer")){
            var value=new PropertyValue.EnumValue("Clip",c);clip.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(),new PropertyName("clipBehavior"),value),commands.removeFirst());
        }
    }
}

