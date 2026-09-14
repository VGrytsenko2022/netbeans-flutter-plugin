package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListWheelScrollViewDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsWheelGeometryPhysicsAndNoopSelectionCallback() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("physics"), new PropertyValue.StringValue("bouncing"));
        properties.put(name("diameterRatio"), new PropertyValue.DoubleValue(new BigDecimal("1.5")));
        properties.put(name("perspective"), new PropertyValue.DoubleValue(new BigDecimal("0.003")));
        properties.put(name("offAxisFraction"), new PropertyValue.DoubleValue(new BigDecimal("-0.25")));
        properties.put(name("useMagnifier"), new PropertyValue.BooleanValue(true));
        properties.put(name("magnification"), new PropertyValue.DoubleValue(new BigDecimal("1.2")));
        properties.put(name("overAndUnderCenterOpacity"), new PropertyValue.DoubleValue(new BigDecimal("0.7")));
        properties.put(name("itemExtent"), new PropertyValue.IntegerValue(BigInteger.valueOf(64)));
        properties.put(name("squeeze"), new PropertyValue.DoubleValue(new BigDecimal("1.1")));
        properties.put(name("onSelectedItemChanged"), new PropertyValue.StringValue("noop"));
        properties.put(name("renderChildrenOutsideViewport"), new PropertyValue.BooleanValue(true));
        properties.put(name("clipBehavior"), new PropertyValue.EnumValue("Clip", "none"));
        properties.put(name("hitTestBehavior"), new PropertyValue.EnumValue("HitTestBehavior", "translucent"));
        properties.put(name("restorationId"), new PropertyValue.StringValue("wheel"));
        properties.put(name("dragStartBehavior"), new PropertyValue.EnumValue("DragStartBehavior", "start"));
        properties.put(name("changeReportingBehavior"), new PropertyValue.EnumValue("ChangeReportingBehavior", "onScrollEnd"));
        WidgetNode wheel = new WidgetNode(StableId.random(),
                new WidgetTypeId("flutter.widgets.ListWheelScrollView"), properties,
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(text("One"), text("Two")))),
                Extensions.empty());

        GeneratedDartRegions generated = generate(wheel);
        String build = generated.build().payload();
        assertTrue(build.contains("child: ListWheelScrollView("), build);
        assertTrue(build.contains("physics: const BouncingScrollPhysics()"), build);
        assertTrue(build.contains("diameterRatio: 1.5"), build);
        assertTrue(build.contains("perspective: 0.003"), build);
        assertTrue(build.contains("itemExtent: 64"), build);
        assertTrue(build.contains("onSelectedItemChanged: (_) {}"), build);
        assertTrue(build.contains("renderChildrenOutsideViewport: true"), build);
        assertTrue(build.contains("children: ["), build);
    }

    @Test
    void emitsTypedProjectCallbackAndPreservesOmittedOptionalValues() {
        WidgetNode wheel = new WidgetNode(StableId.random(),
                new WidgetTypeId("flutter.widgets.ListWheelScrollView"),
                Map.of(name("onSelectedItemChanged"), new PropertyValue.CallbackValue("handleIndex"),
                        name("itemExtent"), new PropertyValue.IntegerValue(BigInteger.valueOf(50))),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())), Extensions.empty());
        String callbackBuild = generate(wheel).build().payload();
        assertTrue(callbackBuild.contains("onSelectedItemChanged: handleIndex"), callbackBuild);
        assertTrue(callbackBuild.contains("itemExtent: 50"), callbackBuild);
        assertFalse(callbackBuild.contains("physics:"), callbackBuild);
        assertFalse(callbackBuild.contains("diameterRatio:"), callbackBuild);
    }

    private static GeneratedDartRegions generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(name("data"), new PropertyValue.StringValue(value)), Map.of(), Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root) {
        return new DesignerDocument(Optional.empty(), StableId.random(),
                new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(),
                        new ManagedRegions(new ManagedRegion(ZERO_HASH), new ManagedRegion(ZERO_HASH))),
                Optional.empty(), root, Extensions.empty());
    }

    private static PropertyName name(String value) { return new PropertyName(value); }
}
