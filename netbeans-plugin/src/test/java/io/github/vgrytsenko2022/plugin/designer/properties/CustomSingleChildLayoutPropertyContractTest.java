package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class CustomSingleChildLayoutPropertyContractTest {
    @Test void requiredPresetAndTypedEditorKeepRowFocusContractAcrossRefreshAndReopen() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(CustomSingleChildLayoutWidgetPropertySchema.TYPE).orElseThrow();
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
        row.setValue(FlutterPropertyCellValue.explicit(CustomSingleChildLayoutWidgetPropertySchema.INITIAL_DELEGATE));
        assertEquals(new SetProperty(widget.id(),new PropertyName("delegate"),CustomSingleChildLayoutWidgetPropertySchema.INITIAL_DELEGATE),commands.removeFirst());
        assertTrue(commands.isEmpty());
    }
}
