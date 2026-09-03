package dev.flutter.netbeans.plugin.designer.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.AddWidget;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.properties.FlutterImageAssetChoices;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FlutterImagePaletteDropPlannerTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId IMAGE =
            new WidgetTypeId("flutter.widgets.Image");
    private static final WidgetTypeId COLUMN =
            new WidgetTypeId("flutter.widgets.Column");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final StableId ROOT_ID = StableId.parse(
            "31250a0e-e136-4cc6-887a-cddb78d09db8");
    private static final StableId NEW_ID = StableId.parse(
            "b05ba7b5-2584-4be4-a278-c45237a3a802");

    @Test
    void emptyImageInventoryDoesNotBlockNonImageCreation() {
        assertTrue(FlutterImageWidgetCreationValues.creationUnavailableReason(
                CATALOG.find(COLUMN).orElseThrow(),
                FlutterImageAssetChoices.empty()).isEmpty());
    }

    @Test
    void selectsFirstSortedDeclaredAssetAndPassesRequiredCreationValue() {
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(
                        new FlutterImageAssetChoices.Choice(
                                Optional.empty(), "assets/z.png", "Z"),
                        new FlutterImageAssetChoices.Choice(
                                Optional.empty(), "assets/a.png", "A")),
                Optional.empty());
        AtomicInteger allocations = new AtomicInteger();

        assertTrue(FlutterImageWidgetCreationValues.creationUnavailableReason(
                CATALOG.find(IMAGE).orElseThrow(), choices).isEmpty());

        FlutterDesignerPaletteDropPlanner.Result result =
                new FlutterDesignerPaletteDropPlanner().plan(
                        document(),
                        CATALOG,
                        IMAGE,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        choices,
                        () -> {
                            allocations.incrementAndGet();
                            return NEW_ID;
                        });

        AddWidget command = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Accepted.class, result).command();
        PropertyValue.ImageProviderValue provider = assertInstanceOf(
                PropertyValue.ImageProviderValue.class,
                command.widget().properties().get(new PropertyName("image")));
        assertEquals(1, allocations.get());
        assertEquals("assets/a.png", provider.assetName());
        assertEquals(Optional.empty(), provider.packageName());
        assertEquals(PropertyValue.ImageProviderValue.ProviderKind.ASSET,
                provider.providerKind());
    }

    @Test
    void rejectsUnavailableInventoryBeforeStableIdAllocation() {
        AtomicInteger allocations = new AtomicInteger();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(),
                Optional.of("Resolve declared image choices: pubspec has no safe assets."));

        Optional<String> preflightReason =
                FlutterImageWidgetCreationValues.creationUnavailableReason(
                        CATALOG.find(IMAGE).orElseThrow(), choices);
        assertTrue(preflightReason.isPresent());
        assertTrue(preflightReason.orElseThrow().contains("required property 'image'"));
        assertTrue(preflightReason.orElseThrow().contains("pubspec has no safe assets"));

        FlutterDesignerPaletteDropPlanner.Result result =
                new FlutterDesignerPaletteDropPlanner().plan(
                        document(),
                        CATALOG,
                        IMAGE,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        choices,
                        () -> {
                            allocations.incrementAndGet();
                            return NEW_ID;
                        });

        FlutterDesignerPaletteDropPlanner.Rejected rejected = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class, result);
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode
                        .REQUIRED_CREATION_VALUE_UNAVAILABLE,
                rejected.code());
        assertTrue(rejected.reason().contains("required property 'image'"));
        assertTrue(rejected.reason().contains("pubspec has no safe assets"));
        assertTrue(rejected.reason().contains(ROOT_ID.toString()));
        assertEquals(0, allocations.get());
    }

    private static DesignerDocument document() {
        WidgetNode root = WidgetNodePrototypeFactory.create(
                CATALOG.find(COLUMN).orElseThrow(), ROOT_ID);
        ManagedRegion emptyHash = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(
                StableId.parse("a153f8ae-79aa-4d3b-9f36-1fd2cc3b6985"),
                new DartSourceDescriptor(
                        "image_page.dart",
                        "ImagePage",
                        WidgetClassKind.STATELESS,
                        Optional.empty(),
                        new ManagedRegions(emptyHash, emptyHash)),
                root);
    }
}
