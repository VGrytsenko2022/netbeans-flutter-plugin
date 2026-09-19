package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class SliverFloatingHeaderPropertyContractTest {
    static final WidgetDefinition DEF=BuiltInWidgetCatalog.getDefault().find(SliverFloatingHeaderWidgetPropertySchema.TYPE).orElseThrow();
    static PropertyName p(String name){return new PropertyName(name);}
    static PropertyValue value(String name){return name.equals("snapMode")?new PropertyValue.EnumValue("FloatingHeaderSnapMode","scroll"):
        name.endsWith("Us")?new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(120000)):
        new PropertyValue.StringValue(name.equals("animationStyle")?"noAnimation":"easeOut");}
    static WidgetNode apply(WidgetNode widget,DesignerCommand command){return RadioPropertyContractTest.apply(widget,command);}
    static FlutterWidgetPropertiesNode node(WidgetNode widget,List<DesignerCommand> commands){return new FlutterWidgetPropertiesNode(Children.LEAF,widget,DEF,commands::add);}

    @Test void allSixTypedRowsRetainIdentityResetAndReopen() throws Exception {
        var base=WidgetNodePrototypeFactory.create(DEF,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=node(base,commands);var sets=node.getPropertySets();
        for(var field:DEF.properties()){
            node.refreshPresentation(base,DEF,commands::add,null,null,FlutterImageAssetChoices.empty());
            var row=TooltipPropertyContractTest.cell(node,field.name().value());var editor=row.getPropertyEditor();
            assertTrue(row.canWrite());assertTrue(row.supportsDefaultValue());assertEquals(FlutterPropertyCellValue.unset(),row.getValue());
            var v=value(field.name().value());editor.setValue(FlutterPropertyCellValue.explicit(v));assertNotNull(editor);
            row.setValue(FlutterPropertyCellValue.explicit(v));assertEquals(1,commands.size());
            var changed=apply(base,commands.removeFirst());assertEquals(v,changed.properties().get(field.name()));assertEquals(base.slots(),changed.slots());
            node.refreshPresentation(changed,DEF,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,field.name().value()));assertArrayEquals(sets,node.getPropertySets());
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(node(changed,commands),field.name().value()).getValue());
            row.restoreDefaultValue();assertEquals(base,apply(changed,commands.removeFirst()));
        }
        assertTrue(commands.isEmpty());
    }
    @Test void wholeAndLocalStyleTransitionsAreSingleAtomicCommandsIncludingExplicitNull() throws Exception {
        var seed=WidgetNodePrototypeFactory.create(DEF,StableId.random());
        var props=new LinkedHashMap<PropertyName,PropertyValue>();props.put(p("snapMode"),value("snapMode"));
        for(String name:SliverFloatingHeaderWidgetPropertySchema.LOCAL_STYLE)props.put(p(name),value(name));
        var local=new WidgetNode(seed.id(),seed.type(),props,seed.slots());
        for(PropertyValue whole:List.of(value("animationStyle"),new PropertyValue.NullValue(),RadioPropertyContractTest.reference("_style"))){
            var commands=new ArrayList<DesignerCommand>();var node=node(local,commands);
            TooltipPropertyContractTest.cell(node,"animationStyle").setValue(FlutterPropertyCellValue.explicit(whole));
            assertEquals(1,commands.size());var replaced=apply(local,commands.removeFirst());
            assertEquals(Map.of(p("snapMode"),value("snapMode"),p("animationStyle"),whole),replaced.properties());
            assertEquals(seed.slots(),replaced.slots());
            for(String name:SliverFloatingHeaderWidgetPropertySchema.LOCAL_STYLE)for(PropertyValue v:List.of(value(name),new PropertyValue.NullValue())){
                node.refreshPresentation(replaced,DEF,commands::add,null,null,FlutterImageAssetChoices.empty());
                TooltipPropertyContractTest.cell(node,name).setValue(FlutterPropertyCellValue.explicit(v));
                assertEquals(1,commands.size());var leaf=apply(replaced,commands.removeFirst());
                assertEquals(Map.of(p("snapMode"),value("snapMode"),p(name),v),leaf.properties());assertEquals(seed.slots(),leaf.slots());
            }
        }
    }
    @Test void bothCurveEditorsOfferAll43PresetsAndRejectRawText() throws Exception {
        var widget=WidgetNodePrototypeFactory.create(DEF,StableId.random());var commands=new ArrayList<DesignerCommand>();var node=node(widget,commands);
        for(String name:List.of("animationStyleCurve","animationStyleReverseCurve")){
            var row=TooltipPropertyContractTest.cell(node,name);var editor=row.getPropertyEditor();editor.setValue(row.getValue());
            var queue=new ArrayDeque<java.awt.Component>();queue.add(editor.getCustomEditor());javax.swing.JComboBox<?> presets=null;
            while(!queue.isEmpty()){var c=queue.removeFirst();if(FlutterPresetDartReferenceEditorComponent.PRESET_NAME.equals(c.getName()))presets=(javax.swing.JComboBox<?>)c;
                if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));}
            assertNotNull(presets);assertEquals(43,presets.getItemCount());
            for(String preset:ExpansionTileWidgetPropertySchema.curvePresets()){
                row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(preset)));
                assertEquals(new PropertyValue.StringValue(preset),apply(widget,commands.removeFirst()).properties().get(p(name)));
            }
            assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("raw()"))));
        }
    }
}

