package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class FlowPropertyContractTest {
    @Test void clipCanBeSetAndResetForBothConstructors() throws Exception {
        for (var type : List.of(FlowWidgetPropertySchema.TYPE,FlowWidgetPropertySchema.UNWRAPPED_TYPE)) {
            var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
            var widget=WidgetNodePrototypeFactory.create(d,StableId.random());
            var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            var row=TooltipPropertyContractTest.cell(node,"clipBehavior");
            assertTrue(row.canWrite());assertTrue(row.supportsDefaultValue());
            assertEquals(FlutterPropertyCellValue.unset(),row.getValue());
            for(String clip:List.of("none","hardEdge","antiAlias","antiAliasWithSaveLayer"))
                row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("Clip",clip)));
            assertEquals(4,commands.size());
            assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
            var values=new LinkedHashMap<>(widget.properties());
            values.put(new PropertyName("clipBehavior"),new PropertyValue.EnumValue("Clip","antiAliasWithSaveLayer"));
            widget=new WidgetNode(widget.id(),widget.type(),values,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"clipBehavior"));
            row.restoreDefaultValue();
            assertEquals(new ResetProperty(widget.id(),new PropertyName("clipBehavior")),commands.getLast());
        }
    }
    @Test void requiredPresetAndTypedEditorKeepRowFocusContractAcrossRefreshAndReopen() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(FlowWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());
        var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"delegate");var sets=node.getPropertySets();
        assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());
        assertTrue(editor.supportsCustomEditor());assertNotNull(editor.getCustomEditor());
        for(boolean factory:List.of(false,true)) {
            var reference=new PropertyValue.DartObjectReferenceValue(Optional.empty(),factory?"createLayout":"layoutDelegate",Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            row.setValue(FlutterPropertyCellValue.explicit(reference));
            assertEquals(new SetProperty(widget.id(),new PropertyName("delegate"),reference),commands.removeFirst());
            widget=new WidgetNode(widget.id(),widget.type(),Map.of(new PropertyName("delegate"),reference),Map.of());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"delegate"));assertArrayEquals(sets,node.getPropertySets());
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"delegate").getValue());
        }
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("anything"))));
        row.setValue(FlutterPropertyCellValue.explicit(FlowWidgetPropertySchema.INITIAL_DELEGATE));
        assertEquals(new SetProperty(widget.id(),new PropertyName("delegate"),FlowWidgetPropertySchema.INITIAL_DELEGATE),commands.removeFirst());
        assertTrue(commands.isEmpty());
    }
}
