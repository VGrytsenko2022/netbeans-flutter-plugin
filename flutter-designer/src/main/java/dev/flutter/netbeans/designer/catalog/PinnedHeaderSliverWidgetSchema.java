package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.WidgetTypeId;

/** Full pinned Flutter constructor: stable Key identity and one nullable box child. */
public final class PinnedHeaderSliverWidgetSchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.PinnedHeaderSliver");
    public static final String DESCRIPTION = "Sliver that pins its box child at the viewport start while scrolling. "
            + "The optional Child determines the scroll-axis extent and may change size. "
            + "No delegate, explicit extent or native Events are needed. "
            + "Both scroll axes, reverse and RTL use native Flutter layout and semantics. "
            + "An empty Child retains the native zero-extent header. Key uses the shared stable widget identity.";
    public static final String CHILD_DESCRIPTION = "Visible box subtree pinned at the viewport start. "
            + "Its laid-out size determines the header extent; edits automatically resize the header. "
            + "Empty retains the native null Child. Accepts box widgets, not Slivers.";
    private PinnedHeaderSliverWidgetSchema() {}
}

