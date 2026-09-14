package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class ValueListenableBuilderPropertyContractTest {
    @Test void typeSourceAndBuilderChangeAtomicallyAndRejectStaleOrForeignDrafts() throws Exception {
        for(var type:List.of(ValueListenableBuilderWidgetPropertySchema.TYPE,ValueListenableBuilderWidgetPropertySchema.SLIVER_TYPE)) {
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
            requested.put("valueType",Optional.of(selected));requested.put("valueListenable",Optional.of(source));
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
            var missing=new LinkedHashMap<>(requested);missing.put("valueListenable",Optional.empty());
            assertThrows(IllegalArgumentException.class,()->new FlutterPropertyCellValue.RadioTypeEdit(type,initial.id(),baseline,missing));
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,after,d,commands::add);
            for(String field:baseline.keySet())assertEquals(TooltipPropertyContractTest.cell(node,field).getValue(),TooltipPropertyContractTest.cell(reopened,field).getValue());
            assertTrue(commands.isEmpty());
        }
    }
    @Test void typeDialogExposesBothDependentReferenceEditorsWithoutPublishingUntilCommit() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(()->{
            var type=ValueListenableBuilderWidgetPropertySchema.TYPE;
            var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();var initial=WidgetNodePrototypeFactory.create(d,StableId.random());
            var commands=new ArrayList<DesignerCommand>();var node=new FlutterWidgetPropertiesNode(Children.LEAF,initial,d,commands::add);
            try {
                var row=TooltipPropertyContractTest.cell(node,"valueType");var editor=row.getPropertyEditor();
                editor.setValue(row.getValue());
                ((org.openide.explorer.propertysheet.ExPropertyEditor)editor).attachEnv(org.openide.explorer.propertysheet.PropertyEnv.create(row));
                var panel=(java.awt.Container)editor.getCustomEditor();
                assertEquals("ValueListenableBuilder value type editor",panel.getAccessibleContext().getAccessibleName());
                assertTrue(findNamed(panel,"flutter.radioType.valueListenable"));
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
        for(var type:List.of(ValueListenableBuilderWidgetPropertySchema.TYPE,ValueListenableBuilderWidgetPropertySchema.SLIVER_TYPE))
        for(String property:List.of("valueListenable","builder")) {
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
        row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(property.equals("builder")?"child":"constant")));
        assertEquals(new SetProperty(widget.id(),new PropertyName(property),new PropertyValue.StringValue(property.equals("builder")?"child":"constant")),commands.removeFirst());
        assertTrue(commands.isEmpty());
        }
    }
}





