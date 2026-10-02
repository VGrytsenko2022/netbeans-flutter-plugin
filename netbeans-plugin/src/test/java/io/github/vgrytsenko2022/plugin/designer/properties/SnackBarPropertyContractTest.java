package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class SnackBarPropertyContractTest {
    @Test void widthEditorStartsValidLocalDraftWithoutChangingOmittedNullOrReferenceValue() throws Exception {
        var def=BuiltInWidgetCatalog.getDefault().find(SnackBarWidgetPropertySchema.TYPE).orElseThrow();
        for(var initial:List.of(FlutterPropertyCellValue.unset(),FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),
                FlutterPropertyCellValue.explicit(new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_width",Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty())))) {
            var seed=WidgetNodePrototypeFactory.create(def,StableId.random());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,seed,def,commands::add);
            var editor=TooltipPropertyContractTest.cell(node,"width").getPropertyEditor();editor.setValue(initial);
            var panel=editor.getCustomEditor();
            var modes=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
            var draft=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
            assertEquals(initial,draft.validatedDraftValue());
            modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.NUMBER);
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(BigInteger.ONE)),draft.validatedDraftValue());
            var field=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,FlutterNullableNumberEditorComponent.VALUE_NAME);
            field.setText("0");assertThrows(IllegalArgumentException.class,draft::validatedDraftValue);
            field.setText("320.5");assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new BigDecimal("320.5"))),draft.validatedDraftValue());
            assertEquals(initial,editor.getValue());assertTrue(commands.isEmpty());
        }
    }
    @Test void all46RowsAreWritableHaveEditorsAndKeepStablePropertySets() throws Exception {
        for(boolean action:List.of(false,true)){
            var def=BuiltInWidgetCatalog.getDefault().find(action?SnackBarWidgetPropertySchema.ACTION:SnackBarWidgetPropertySchema.TYPE).orElseThrow();
            var w=WidgetNodePrototypeFactory.create(def,StableId.random());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,def,commands::add);var sets=node.getPropertySets();
            assertEquals(action?7:39,def.properties().size());
            for(var p:def.properties()) {
                var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite());
                var editor=row.getPropertyEditor();assertNotNull(editor,p.name().value());editor.setValue(row.getValue());
                if(editor.supportsCustomEditor())assertNotNull(editor.getCustomEditor(),p.name().value());
            }
            assertTrue(commands.isEmpty());node.refreshPresentation(w,def,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertArrayEquals(sets,node.getPropertySets());
        }
    }
    @Test void floatingGeometrySwitchesAtomically()throws Exception {
        var def=BuiltInWidgetCatalog.getDefault().find(SnackBarWidgetPropertySchema.TYPE).orElseThrow();
        var seed=WidgetNodePrototypeFactory.create(def,StableId.random());
        var props=new LinkedHashMap<>(seed.properties());props.put(new PropertyName("width"),new PropertyValue.IntegerValue(BigInteger.valueOf(320)));
        props.put(new PropertyName("behavior"),new PropertyValue.EnumValue("SnackBarBehavior","floating"));
        var w=new WidgetNode(seed.id(),seed.type(),props,seed.slots());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,def,commands::add);
        TooltipPropertyContractTest.cell(node,"margin").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.EdgeInsetsValue(BigDecimal.TEN,BigDecimal.ONE,BigDecimal.TEN,BigDecimal.ONE)));
        var patch=assertInstanceOf(PatchProperties.class,commands.getFirst());
        assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName("width"))));
        commands.clear();
        TooltipPropertyContractTest.cell(node,"behavior").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("SnackBarBehavior","fixed")));
        patch=assertInstanceOf(PatchProperties.class,commands.getFirst());
        assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName("width"))));
    }
    @Test void shapeChangesAreAtomicAcrossEveryPair()throws Exception {
        var def=BuiltInWidgetCatalog.getDefault().find(SnackBarWidgetPropertySchema.TYPE).orElseThrow();
        for(String from:CardWidgetPropertySchema.shapeKinds())for(String to:CardWidgetPropertySchema.shapeKinds()) {
            var seed=WidgetNodePrototypeFactory.create(def,StableId.random());var values=new LinkedHashMap<>(seed.properties());
            values.put(new PropertyName("shapeKind"),new PropertyValue.StringValue(from));
            for(String name:CardWidgetPropertySchema.builtInShapePropertyNames())
                if(CardWidgetPropertySchema.isShapeDetailProperty(name)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(name,from))
                    values.put(new PropertyName(name),CardPropertyContractTest.value(name));
            var w=new WidgetNode(seed.id(),seed.type(),values,seed.slots());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,def,commands::add);
            TooltipPropertyContractTest.cell(node,"shapeKind").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(to)));
            assertEquals(from.equals(to)?0:1,commands.size());
        }
    }
}
