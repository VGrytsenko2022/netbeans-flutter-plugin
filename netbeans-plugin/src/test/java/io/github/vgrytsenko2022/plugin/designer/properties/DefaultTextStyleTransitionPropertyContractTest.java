package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class DefaultTextStyleTransitionPropertyContractTest {
    @Test void everyRowHasAnEditorAndAnimationRemainsRequired() throws Exception {
        var d = BuiltInWidgetCatalog.getDefault().find(DefaultTextStyleTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var w = WidgetNodePrototypeFactory.create(d,StableId.random());
        var node = new FlutterWidgetPropertiesNode(Children.LEAF,w,d,ignored->{});
        assertEquals(36,d.properties().size());
        for (var p:d.properties()) {
            var row=TooltipPropertyContractTest.cell(node,p.name().value());
            assertTrue(row.canWrite());assertNotNull(row.getPropertyEditor());
        }
        var row=TooltipPropertyContractTest.cell(node,"style");
        assertEquals("Style animation",row.getDisplayName());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());assertNotNull(editor.getCustomEditor());
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }
    @Test void localToAnimationAndBackAreAtomicWithStableRowsAndReopen() throws Exception {
        var type=DefaultTextStyleTransitionWidgetPropertySchema.TYPE;
        var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
        var props=Map.<PropertyName,PropertyValue>of(new PropertyName("style"),new PropertyValue.StringValue("local"),
            new PropertyName("styleFontSize"),new PropertyValue.DoubleValue(BigDecimal.TEN));
        var w=new WidgetNode(StableId.random(),type,props,Map.of());
        var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"style");var sets=node.getPropertySets();
        var ref=AnimatedDefaultTextStylePropertyContractTest.ref("_animation");
        row.setValue(FlutterPropertyCellValue.explicit(ref));
        var patch=assertInstanceOf(PatchProperties.class,commands.removeFirst());
        assertEquals(2,patch.patches().size());
        assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName("styleFontSize"))));
        var updated=new WidgetNode(w.id(),type,Map.of(new PropertyName("style"),ref),w.slots());
        node.refreshPresentation(updated,d,commands::add,null,null,FlutterImageAssetChoices.empty());
        assertSame(row,TooltipPropertyContractTest.cell(node,"style"));assertArrayEquals(sets,node.getPropertySets());
        TooltipPropertyContractTest.cell(node,"styleFontSize").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(BigDecimal.TEN)));
        assertTrue(assertInstanceOf(PatchProperties.class,commands.removeFirst()).patches()
            .contains(new PatchProperties.SetPatch(new PropertyName("style"),new PropertyValue.StringValue("local"))));
        var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,updated,d,commands::add);
        assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"style").getValue());
    }
}
