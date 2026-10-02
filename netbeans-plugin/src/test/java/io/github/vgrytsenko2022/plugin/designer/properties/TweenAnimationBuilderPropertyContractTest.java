package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class TweenAnimationBuilderPropertyContractTest {
    @Test void completeCurveDurationAndCallbackEditorsAreWritableAndTyped() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(TweenAnimationBuilderWidgetPropertySchema.TYPE).orElseThrow();
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

    @Test void optionalBooleanRowKeepCheckboxesStableIdentityAndResetAfterReopen() throws Exception {
        var d = BuiltInWidgetCatalog.getDefault().find(TweenAnimationBuilderWidgetPropertySchema.TYPE).orElseThrow();
        var widget = WidgetNodePrototypeFactory.create(d, StableId.random());
        var commands = new ArrayList<DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
        var sets = node.getPropertySets();
        for (var definition : d.properties().stream().filter(p -> p.name().value().equals("nullableValueType")).toList()) {
            var name = definition.name();
            var row = TooltipPropertyContractTest.cell(node, name.value());
            assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue());
            assertEquals(FlutterPropertyCellValue.unset(), row.getValue());
            for (boolean flag : List.of(true, false)) {
                var value = new PropertyValue.BooleanValue(flag);
                row.setValue(FlutterPropertyCellValue.explicit(value));
                assertEquals(new SetProperty(widget.id(), name, value), commands.removeFirst());
                var props = new LinkedHashMap<>(widget.properties()); props.put(name, value);
                widget = new WidgetNode(widget.id(), widget.type(), props, widget.slots());
                node.refreshPresentation(widget, d, commands::add, null, null, FlutterImageAssetChoices.empty());
                assertSame(row, TooltipPropertyContractTest.cell(node, name.value()));
                assertArrayEquals(sets, node.getPropertySets());
                var editor = row.getPropertyEditor(); editor.setValue(row.getValue());
                assertTrue(editor.isPaintable()); assertNull(editor.getTags());
                var reopened = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
                var reopenedRow = TooltipPropertyContractTest.cell(reopened, name.value());
                assertEquals(row.getValue(), reopenedRow.getValue()); assertTrue(reopenedRow.canWrite());
                reopenedRow.restoreDefaultValue();
                assertEquals(new ResetProperty(widget.id(), name), commands.removeFirst());
            }
            assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
            assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("false"))));
        }
        assertTrue(commands.isEmpty());
    }
    @Test void typeSourceAndBuilderChangeAtomicallyAndRejectStaleOrForeignDrafts() throws Exception {
        for(var type:List.of(TweenAnimationBuilderWidgetPropertySchema.TYPE,TweenAnimationBuilderWidgetPropertySchema.SLIVER_TYPE)) {
            var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
            var initial=WidgetNodePrototypeFactory.create(d,StableId.random());
            var commands=new ArrayList<DesignerCommand>();var node=new FlutterWidgetPropertiesNode(Children.LEAF,initial,d,commands::add);
            var row=TooltipPropertyContractTest.cell(node,"valueType");
            var baseline=FlutterPropertyCellValue.RadioTypeEdit.snapshot(initial);
            var requested=new LinkedHashMap<>(baseline);
            var selected=new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/types.dart"),"Rows",
                    Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
            var source=new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/types.dart"),"rows",
                    Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
            var builder=new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/types.dart"),"buildRows",
                    Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
            requested.put("valueType",Optional.of(selected));requested.put("tween",Optional.of(source));
            requested.put("builder",Optional.of(builder));requested.put("nullableValueType",Optional.of(new PropertyValue.BooleanValue(true)));
            var draft=new FlutterPropertyCellValue(Optional.of(selected),Optional.of(new FlutterPropertyCellValue.RadioTypeEdit(type,initial.id(),baseline,requested)));
            row.setValue(draft);assertEquals(1,commands.size());assertInstanceOf(PatchProperties.class,commands.getFirst());
            var after=RadioPropertyContractTest.apply(initial,commands.removeFirst());assertEquals(requested,FlutterPropertyCellValue.RadioTypeEdit.snapshot(after));
            assertEquals(initial.slots(),after.slots());
            var sets=node.getPropertySets();node.refreshPresentation(after,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"valueType"));assertArrayEquals(sets,node.getPropertySets());
            assertThrows(IllegalArgumentException.class,()->row.setValue(draft),"Stale dialog must not overwrite newer fields");
            assertThrows(IllegalArgumentException.class,()->TooltipPropertyContractTest.cell(node,"builder").setValue(draft));
            var malformed=new LinkedHashMap<>(requested);malformed.put("onChanged",Optional.empty());
            assertThrows(IllegalArgumentException.class,()->new FlutterPropertyCellValue.RadioTypeEdit(type,initial.id(),baseline,malformed));
            var missing=new LinkedHashMap<>(requested);missing.put("tween",Optional.empty());
            assertThrows(IllegalArgumentException.class,()->new FlutterPropertyCellValue.RadioTypeEdit(type,initial.id(),baseline,missing));
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,after,d,commands::add);
            for(String field:baseline.keySet())assertEquals(TooltipPropertyContractTest.cell(node,field).getValue(),TooltipPropertyContractTest.cell(reopened,field).getValue());
            assertTrue(commands.isEmpty());
        }
    }
    @Test void typeDialogExposesBothDependentReferenceEditorsWithoutPublishingUntilCommit() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(()->{
            var type=TweenAnimationBuilderWidgetPropertySchema.TYPE;
            var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();var initial=WidgetNodePrototypeFactory.create(d,StableId.random());
            var commands=new ArrayList<DesignerCommand>();var node=new FlutterWidgetPropertiesNode(Children.LEAF,initial,d,commands::add);
            try {
                var row=TooltipPropertyContractTest.cell(node,"valueType");var editor=row.getPropertyEditor();
                editor.setValue(row.getValue());
                ((org.openide.explorer.propertysheet.ExPropertyEditor)editor).attachEnv(org.openide.explorer.propertysheet.PropertyEnv.create(row));
                var panel=(java.awt.Container)editor.getCustomEditor();
                assertEquals("TweenAnimationBuilder value type editor",panel.getAccessibleContext().getAccessibleName());
                assertTrue(findNamed(panel,"flutter.radioType.tween"));
                assertTrue(findNamed(panel,"flutter.radioType.builder"));
                assertTrue(findNamed(panel,FlutterRadioTypeEditorComponent.NULLABILITY_NAME));
                assertTrue(commands.isEmpty());
            } catch (Exception e) { throw new AssertionError(e); }
        });
    }
    private static boolean findNamed(java.awt.Container parent,String name) {
        if(name.equals(parent.getName())) return true;
        for(var child:parent.getComponents())if(name.equals(child.getName())||child instanceof java.awt.Container c&&findNamed(c,name))return true;
        return false;
    }

    @Test void requiredPresetAndTypedEditorKeepRowFocusContractAcrossRefreshAndReopen() throws Exception {
        for(var type:List.of(TweenAnimationBuilderWidgetPropertySchema.TYPE,TweenAnimationBuilderWidgetPropertySchema.SLIVER_TYPE))
        for(String property:List.of("tween","builder")) {
        var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());
        var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,property);var sets=node.getPropertySets();
        assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());
        assertTrue(editor.supportsCustomEditor());assertNotNull(editor.getCustomEditor());
        for(boolean factory:List.of(false,true)) {
            var reference=new PropertyValue.DartObjectReferenceValue(Optional.empty(),factory?"createBuilder":"buildBox",Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            row.setValue(FlutterPropertyCellValue.explicit(reference));
            assertEquals(new SetProperty(widget.id(),new PropertyName(property),reference),commands.removeFirst());
            var props=new HashMap<>(widget.properties());props.put(new PropertyName(property),reference);
            widget=new WidgetNode(widget.id(),widget.type(),props,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,property));assertArrayEquals(sets,node.getPropertySets());
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,property).getValue());
        }
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("anything"))));
        row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(property.equals("builder")?"child":"default")));
        assertEquals(new SetProperty(widget.id(),new PropertyName(property),new PropertyValue.StringValue(property.equals("builder")?"child":"default")),commands.removeFirst());
        assertTrue(commands.isEmpty());
        }
    }
}





