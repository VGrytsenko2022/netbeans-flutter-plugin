package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class FlexibleSpaceBarSettingsPropertyContractTest {
    @Test void sixStableRowsKeepRequiredNumbersAndNullableCheckboxesDistinct() throws Exception {
        var definition=BuiltInWidgetCatalog.getDefault().find(FlexibleSpaceBarSettingsWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(definition,StableId.random());
        var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,definition,commands::add);
        var sets=node.getPropertySets();
        var values=Map.<String,PropertyValue>of("toolbarOpacity",n(0.5),"minExtent",n(20),"maxExtent",n(300),"currentExtent",n(120),
                "isScrolledUnder",new PropertyValue.BooleanValue(true),"hasLeading",new PropertyValue.BooleanValue(false));
        for(var field:FlexibleSpaceBarSettingsWidgetPropertySchema.FIELDS) {
            var row=TooltipPropertyContractTest.cell(node,field.name());assertTrue(row.canWrite());assertNotNull(row.getPropertyEditor());
            boolean required=definition.property(new PropertyName(field.name())).orElseThrow().parameter().required();
            assertEquals(!required,row.supportsDefaultValue());
            row.setValue(FlutterPropertyCellValue.explicit(values.get(field.name())));
            assertEquals(1,commands.size());widget=RadioPropertyContractTest.apply(widget,commands.removeFirst());
            node.refreshPresentation(widget,definition,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,field.name()));assertArrayEquals(sets,node.getPropertySets());
            assertEquals(FlutterPropertyCellValue.explicit(values.get(field.name())),row.getValue());
            if(!required) {
                row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
                widget=RadioPropertyContractTest.apply(widget,commands.removeFirst());
                node.refreshPresentation(widget,definition,commands::add,null,null,FlutterImageAssetChoices.empty());
                assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),row.getValue());
                row.restoreDefaultValue();widget=RadioPropertyContractTest.apply(widget,commands.removeFirst());
                node.refreshPresentation(widget,definition,commands::add,null,null,FlutterImageAssetChoices.empty());
                assertEquals(FlutterPropertyCellValue.unset(),row.getValue());
            }
        }
    }
    private static PropertyValue.DoubleValue n(double v){return new PropertyValue.DoubleValue(BigDecimal.valueOf(v));}
}

