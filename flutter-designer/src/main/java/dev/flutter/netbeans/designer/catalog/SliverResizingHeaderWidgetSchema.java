package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.List;

/** Full Flutter 3.44.8 constructor: Key identity plus three nullable box slots, no scalar arguments. */
public final class SliverResizingHeaderWidgetSchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverResizingHeader");
    public static final List<String> SLOTS = List.of("minExtentPrototype", "maxExtentPrototype", "child");
    public static final String DESCRIPTION = "Pinned sliver that resizes with scrolling between prototype-measured extents. "
            + "All three box-widget slots are optional. No delegate or native Events are needed. "
            + "The minimum prototype defaults to zero; without a maximum prototype the SDK measures the child's intrinsic size. "
            + "Prototypes are laid out but never painted or focused; edit them in the tree or Slots. "
            + "Use finite SizedBox prototypes for explicit dimensions along the scroll axis. "
            + "An omitted Child uses the SDK's empty box. Key uses the shared stable widget identity.";
    public static String slotDescription(String name) {
        return switch (name) {
            case "minExtentPrototype" -> "Measurement-only box defining the minimum scroll-axis extent. Empty uses zero. "
                    + "Never painted or focused; edit in the widget tree or Slots. No Canvas pointer target.";
            case "maxExtentPrototype" -> "Measurement-only box defining the maximum scroll-axis extent. "
                    + "Empty uses the visible child's intrinsic size. Never painted or focused; edit in the tree or Slots. "
                    + "Use an explicit finite prototype when the child cannot provide an intrinsic size.";
            case "child" -> "Visible box subtree, resized and pinned at the viewport start. Empty uses SizedBox.shrink(). "
                    + "Canvas pointer drops address this slot, never the hidden prototypes.";
            default -> throw new IllegalArgumentException("Unknown SliverResizingHeader slot: " + name);
        };
    }
    private SliverResizingHeaderWidgetSchema() {}
}

