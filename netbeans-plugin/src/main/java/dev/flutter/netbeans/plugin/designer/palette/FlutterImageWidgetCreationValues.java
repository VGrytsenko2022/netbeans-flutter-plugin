package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.properties.FlutterImageAssetChoices;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Resolves the required Image.image creation value without allocating an id. */
public final class FlutterImageWidgetCreationValues {
    static final WidgetTypeId IMAGE_TYPE =
            new WidgetTypeId("flutter.widgets.Image");
    static final WidgetTypeId IMAGE_ICON_TYPE =
            new WidgetTypeId("flutter.widgets.ImageIcon");
    private static final PropertyName IMAGE_PROPERTY = new PropertyName("image");

    private FlutterImageWidgetCreationValues() {
    }

    /**
     * Creates a slot replacement only after every required Image value has
     * been resolved, so an unavailable inventory never consumes a stable id.
     */
    public static WidgetNode createPrototype(
            WidgetDefinition definition,
            FlutterImageAssetChoices choices,
            Supplier<StableId> idSupplier) {
        Objects.requireNonNull(idSupplier, "idSupplier");
        Result resolved = resolve(definition, choices);
        if (resolved instanceof Unavailable unavailable) {
            throw new IllegalArgumentException(unavailable.reason());
        }
        Available available = (Available) resolved;
        return WidgetNodePrototypeFactory.create(
                definition,
                Objects.requireNonNull(idSupplier.get(), "supplied stable id"),
                available.creationValues());
    }

    static Result resolve(
            WidgetDefinition definition,
            FlutterImageAssetChoices choices) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(choices, "choices");
        boolean imageIcon = IMAGE_ICON_TYPE.equals(definition.typeId());
        if (!IMAGE_TYPE.equals(definition.typeId()) && !imageIcon) {
            return new Available(Map.of(), "No creation-time values are required.");
        }
        if (choices.choices().isEmpty()) {
            if (imageIcon) {
                return new Available(Map.of(IMAGE_PROPERTY, new PropertyValue.NullValue()),
                        "Create ImageIcon with explicit None; no declared asset is required.");
            }
            return new Available(
                    Map.of(IMAGE_PROPERTY,
                            PropertyValue.ImageProviderValue.unresolved()),
                    "Create Image with an unresolved provider until a declared "
                    + "Flutter image asset is selected.");
        }
        FlutterImageAssetChoices.Choice first = choices.choices().getFirst();
        PropertyValue.ImageProviderValue provider =
                new PropertyValue.ImageProviderValue(
                        PropertyValue.ImageProviderValue.ProviderKind.ASSET,
                        first.assetName(),
                        first.packageName(),
                        java.util.Optional.empty(),
                        java.util.Optional.empty());
        return new Available(
                Map.of(IMAGE_PROPERTY, provider),
                "Create " + (imageIcon ? "ImageIcon" : "Image")
                        + " with first declared asset " + first.externalName() + '.');
    }

    sealed interface Result permits Available, Unavailable {
    }

    record Available(
            Map<PropertyName, PropertyValue> creationValues,
            String detail) implements Result {
        Available {
            creationValues = Map.copyOf(
                    Objects.requireNonNull(creationValues, "creationValues"));
            detail = requireReason(detail);
        }
    }

    record Unavailable(String reason) implements Result {
        Unavailable {
            reason = requireReason(reason);
        }
    }

    private static String requireReason(String value) {
        Objects.requireNonNull(value, "value");
        String compact = value.strip().replaceAll("\\s+", " ");
        if (compact.isEmpty()) {
            throw new IllegalArgumentException("Image creation detail must not be blank");
        }
        return compact;
    }
}
