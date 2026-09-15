package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 CustomPaint constructor, apart from shared Key/Child. */
public final class CustomPaintWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.CustomPaint");
    public static final String DESCRIPTION = "Paints before and after Child using typed CustomPainter objects. "
            + "Dart owns paint, repaint notifications, hit testing, semantics and resource lifetime. "
            + "Canvas does not execute project code: painter references use inert delegates and source Size uses zero.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("painter", "Painter", "Optional CustomPainter? reference/getter/zero-argument factory, explicit null or unset. Paints before Child. "
                + "Source owns paint/shouldRepaint, repaint Listenable, hitTest and semanticsBuilder. Canvas uses an inert delegate, not the project's artwork or hit/semantics behavior."),
        new Field("foregroundPainter", "Foreground painter", "Optional CustomPainter? reference/getter/zero-argument factory, explicit null or unset. Paints after Child. "
                + "The source object can implement all CustomPainter APIs; Canvas never executes them."),
        new Field("size", "Size", "Preferred finite nonnegative width/height when Child is absent, or a non-null Size source. "
                + "Omission uses Size.zero. Child size takes precedence; parent constraints still apply. Source dimensions are owned by Dart and preview as zero."),
        new Field("isComplex", "Complex painting", "Raster-cache complexity hint; default false. True requires at least one non-null painter at runtime. "
                + "Known null/unset painters with true are rejected before save."),
        new Field("willChange", "Will change", "Hints that painting will change next frame; default false. True requires at least one non-null painter at runtime. "
                + "A nullable source must supply a painter when either cache hint is true."));
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        for (int i = 0; i < FIELDS.size(); i++) {
            String name = FIELDS.get(i).name();
            List<PropertyValueConstraint> constraints = switch (name) {
                case "painter", "foregroundPainter" -> List.of(
                    new PropertyValueConstraint.DartObjectReferenceValues("CustomPainter?"),
                    new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "size" -> List.of(new PropertyValueConstraint.SizeValues(),
                    new PropertyValueConstraint.DartObjectReferenceValues("Size"));
                default -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN));
            };
            result.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(i, false), constraints, Optional.empty()));
        }
        return List.copyOf(result);
    }
    public static Optional<String> relationshipError(WidgetNode node) {
        boolean hasPainter = List.of("painter", "foregroundPainter").stream()
                .anyMatch(name -> node.properties().get(new PropertyName(name)) instanceof PropertyValue.DartObjectReferenceValue);
        boolean cacheHint = List.of("isComplex", "willChange").stream()
                .anyMatch(name -> new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName(name))));
        return !hasPainter && cacheHint
                ? Optional.of("CustomPaint cache hints require Painter or Foreground painter; both are null/unset.")
                : Optional.empty();
    }
    private CustomPaintWidgetPropertySchema() {}
}
