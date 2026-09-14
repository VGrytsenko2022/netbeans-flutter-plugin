package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 default and maintain constructors. */
public final class SliverVisibilityWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverVisibility");
    public static final WidgetTypeId MAINTAIN_TYPE = new WidgetTypeId("flutter.widgets.SliverVisibility.maintain");
    public static final String DESCRIPTION = "Shows the required Sliver or its optional Replacement sliver. "
            + "Hidden branches remain editable in the widget tree. With Maintain state=false, the hidden runtime child is disposed; "
            + "otherwise replacement is ignored. Changing maintenance flags can discard runtime state; normally only Visible changes. "
            + "Wrap an existing sliver. The required child cannot be cleared or moved out; replace it atomically.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
            new Field("visible", "Visible", "Omission uses true. False hides the main sliver. Both stored branches remain editable in the tree."),
            new Field("maintainState", "Maintain state", "Omission uses false. True keeps hidden child state and ignores replacement. Required by Maintain animation. SliverVisibility has no maintainFocusability parameter; retained slivers may still receive keyboard focus."),
            new Field("maintainAnimation", "Maintain animation", "Omission uses false. True requires Maintain state and keeps hidden tickers running. False mutes them while hidden; retaining animations consumes resources."),
            new Field("maintainSize", "Maintain size", "Omission uses false. True requires Maintain animation and Maintain state and retains hidden scroll/layout extent."),
            new Field("maintainSemantics", "Maintain semantics", "Omission uses false. True requires Maintain size and exposes hidden accessibility semantics."),
            new Field("maintainInteractivity", "Maintain interactivity", "Omission uses false. True requires Maintain size and permits pointer interaction with the hidden runtime child. Designer geometry remains limited to visible branches."));
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || MAINTAIN_TYPE.equals(type); }
    public static List<Field> fields(boolean maintain) { return maintain ? FIELDS.subList(0, 1) : FIELDS; }
    public static List<PropertyDefinition> properties(boolean maintain) {
        var result = new ArrayList<PropertyDefinition>();
        for (var field : fields(maintain)) {
            result.add(new PropertyDefinition(new PropertyName(field.name()), DartParameter.named(result.size() + 2, false),
                    List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()));
        }
        return List.copyOf(result);
    }
    private SliverVisibilityWidgetPropertySchema() {}
}

