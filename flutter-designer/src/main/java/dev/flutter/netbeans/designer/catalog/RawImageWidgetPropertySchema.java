package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete Flutter 3.44.8 RawImage constructor, apart from shared Key. */
public final class RawImageWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.RawImage");
    public static final String DESCRIPTION = "Paints a decoded dart:ui.Image, not a Widget Image or ImageProvider. "
            + "All 16 constructor parameters are editable. Dart owns decoding and disposal of its image handle and animation. "
            + "Null/unset paints no image. Canvas never executes project code and previews source-owned images as empty.";
    public record Field(String name, String label, String group, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("image", "Decoded image", "Image", "Optional dart:ui.Image? reference/getter/zero-argument factory. Not ImageProvider, asset path or Widget Image. Dart owns the image and must dispose its handle only after consumers no longer need it. Null/unset paints nothing; Canvas uses null."),
        new Field("debugImageLabel", "Debug image label", "Image", "Optional diagnostic String, null or unset; this is not a semantic label."),
        new Field("width", "Width", "Layout", "Optional nonnegative logical pixels or double? source; null/unset uses intrinsic image width and parent constraints."),
        new Field("height", "Height", "Layout", "Optional nonnegative logical pixels or double? source; null/unset uses intrinsic image height and parent constraints."),
        new Field("scale", "Scale", "Layout", "Positive finite pixel-to-logical scale or non-null double source. Omission/source preview uses 1. Not a layout transform or decode size."),
        new Field("color", "Color", "Appearance", "Optional ARGB, ColorScheme token, null/unset or Color? source."),
        new Field("opacity", "Opacity animation", "Appearance", "Optional local opacity from 0 to 1 creates AlwaysStoppedAnimation<double>, or Animation<double>? source. Null/unset applies no animation. Dart owns controller lifetime; Canvas previews project animation as null."),
        new Field("colorBlendMode", "Color blend mode", "Appearance", "Optional BlendMode, null or unset. Native color blending defaults to srcIn."),
        new Field("fit", "Fit", "Layout", "Optional BoxFit/null/unset. With centerSlice, none and cover are not supported by Flutter; slice bounds and destination size must fit the actual decoded image."),
        new Field("alignment", "Alignment", "Layout", "Physical/directional coordinates or non-null AlignmentGeometry source. Omission/source preview uses center; directional values resolve against ambient direction."),
        new Field("repeat", "Repeat", "Layout", "Optional ImageRepeat; omission uses noRepeat."),
        new Field("centerSlice", "Center slice", "Layout", "Optional Rect? source/getter/factory, including Rect.fromLTRB/fromLTWH in source. Nine-patch coordinates must fit the decoded image. Null/unset disables it. Canvas cannot inspect source rectangles and previews null."),
        new Field("matchTextDirection", "Match text direction", "Appearance", "Optional checkbox, default false; true mirrors pixels in RTL."),
        new Field("invertColors", "Invert colors", "Appearance", "Optional checkbox, default false; inversion follows the configured color filter."),
        new Field("filterQuality", "Filter quality", "Appearance", "Optional none/low/medium/high; default medium. Explicit null is not allowed."),
        new Field("isAntiAlias", "Anti-alias", "Appearance", "Optional checkbox, default false; smooths transformed image edges."));
    public static List<PropertyDefinition> properties() {
        var shared = FadeInImageWidgetPropertySchema.properties().stream().collect(
                java.util.stream.Collectors.toMap(p -> p.name().value(), p -> p.constraints()));
        var result = new ArrayList<PropertyDefinition>();
        for (int i = 0; i < FIELDS.size(); i++) {
            String name = FIELDS.get(i).name();
            List<PropertyValueConstraint> constraints = switch (name) {
                case "image" -> nullableSource("Image?");
                case "centerSlice" -> nullableSource("Rect?");
                case "debugImageLabel" -> shared.get("imageSemanticLabel");
                case "scale" -> List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ONE, new BigInteger("9007199254740991")),
                        new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, false, null, true),
                        new PropertyValueConstraint.DartObjectReferenceValues("double"));
                case "opacity" -> List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, BigInteger.ONE),
                        new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, true, BigDecimal.ONE, true),
                        new PropertyValueConstraint.DartObjectReferenceValues("Animation<double>?"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "invertColors", "isAntiAlias" -> shared.get("matchTextDirection");
                default -> Objects.requireNonNull(shared.get(name), name);
            };
            result.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(i, false), constraints, Optional.empty()));
        }
        return List.copyOf(result);
    }
    private static List<PropertyValueConstraint> nullableSource(String type) {
        return List.of(new PropertyValueConstraint.DartObjectReferenceValues(type),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
    }
    private RawImageWidgetPropertySchema() {}
}
