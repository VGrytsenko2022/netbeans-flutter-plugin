package dev.flutter.netbeans.plugin.designer.icons;

import dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SingleChildScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Central mapping between reviewed built-in widget identities and their
 * NetBeans icon bases.
 *
 * <p>Each base has a 16 px SVG, a {@code 32} SVG used for larger palette
 * presentations, and matching {@code _dark} variants. Extension widgets are
 * deliberately left unmapped rather than being mistaken for one of the
 * reviewed core widgets; a validated contributor-icon SPI is a later slice.</p>
 */
public final class FlutterWidgetIconRegistry {
    private static final String ICON_ROOT =
            "dev/flutter/netbeans/plugin/designer/icons/widgets/";

    private static final Map<String, String> REVIEWED_ICON_PATHS = Map.ofEntries(
            Map.entry("flutter.material.Scaffold", ICON_ROOT + "scaffold.svg"),
            Map.entry("flutter.material.AppBar", ICON_ROOT + "appbar.svg"),
            Map.entry("flutter.material.ElevatedButton",
                    ICON_ROOT + "elevatedbutton.svg"),
            Map.entry("flutter.material.TextField", ICON_ROOT + "textfield.svg"),
            Map.entry("flutter.widgets.Column", ICON_ROOT + "column.svg"),
            Map.entry("flutter.widgets.Row", ICON_ROOT + "row.svg"),
            Map.entry("flutter.widgets.Wrap", ICON_ROOT + "wrap.svg"),
            Map.entry("flutter.widgets.Padding", ICON_ROOT + "padding.svg"),
            Map.entry("flutter.widgets.Center", ICON_ROOT + "center.svg"),
            Map.entry("flutter.widgets.Align", ICON_ROOT + "align.svg"),
            Map.entry("flutter.widgets.FractionallySizedBox",
                    ICON_ROOT + "fractionallysizedbox.svg"),
            Map.entry("flutter.widgets.FittedBox", ICON_ROOT + "fittedbox.svg"),
            Map.entry("flutter.widgets.ConstrainedBox",
                    ICON_ROOT + "constrainedbox.svg"),
            Map.entry("flutter.widgets.UnconstrainedBox",
                    ICON_ROOT + "unconstrainedbox.svg"),
            Map.entry("flutter.widgets.LimitedBox", ICON_ROOT + "limitedbox.svg"),
            Map.entry("flutter.widgets.OverflowBox", ICON_ROOT + "overflowbox.svg"),
            Map.entry("flutter.widgets.Stack", ICON_ROOT + "stack.svg"),
            Map.entry("flutter.widgets.IndexedStack",
                    ICON_ROOT + "indexedstack.svg"),
            Map.entry("flutter.widgets.Expanded", ICON_ROOT + "expanded.svg"),
            Map.entry("flutter.widgets.Flexible", ICON_ROOT + "flexible.svg"),
            Map.entry("flutter.widgets.Spacer", ICON_ROOT + "spacer.svg"),
            Map.entry("flutter.widgets.Baseline", ICON_ROOT + "baseline.svg"),
            Map.entry("flutter.widgets.IntrinsicHeight",
                    ICON_ROOT + "intrinsicheight.svg"),
            Map.entry("flutter.widgets.IntrinsicWidth",
                    ICON_ROOT + "intrinsicwidth.svg"),
            Map.entry("flutter.widgets.Offstage", ICON_ROOT + "offstage.svg"),
            Map.entry("flutter.widgets.SizedOverflowBox",
                    ICON_ROOT + "sizedoverflowbox.svg"),
            Map.entry("flutter.widgets.Transform", ICON_ROOT + "transform.svg"),
            Map.entry("flutter.widgets.RotatedBox", ICON_ROOT + "rotatedbox.svg"),
            Map.entry("flutter.widgets.ListBody", ICON_ROOT + "listbody.svg"),
            Map.entry("flutter.widgets.OverflowBar", ICON_ROOT + "overflowbar.svg"),
            Map.entry("flutter.widgets.SafeArea", ICON_ROOT + "safearea.svg"),
            Map.entry("flutter.widgets.ListView", ICON_ROOT + "listview.svg"),
            Map.entry(GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.value(),
                    ICON_ROOT + "gridviewcount.svg"),
            Map.entry(
                    SingleChildScrollViewWidgetPropertySchema
                            .SINGLE_CHILD_SCROLL_VIEW_TYPE.value(),
                    ICON_ROOT + "singlechildscrollview.svg"),
            Map.entry("flutter.widgets.Text", ICON_ROOT + "text.svg"),
            Map.entry("flutter.widgets.Icon", ICON_ROOT + "icon.svg"),
            Map.entry("flutter.widgets.Image", ICON_ROOT + "image.svg"),
            Map.entry("flutter.widgets.ImageIcon", ICON_ROOT + "imageicon.svg"),
            Map.entry("flutter.material.Divider", ICON_ROOT + "divider.svg"),
            Map.entry("flutter.material.VerticalDivider", ICON_ROOT + "verticaldivider.svg"),
            Map.entry("flutter.material.Card", ICON_ROOT + "card.svg"),
            Map.entry("flutter.material.Badge", ICON_ROOT + "badge.svg"),
            Map.entry("flutter.material.CircleAvatar", ICON_ROOT + "circleavatar.svg"),
            Map.entry("flutter.material.LinearProgressIndicator", ICON_ROOT + "linearprogressindicator.svg"),
            Map.entry("flutter.material.CircularProgressIndicator", ICON_ROOT + "circularprogressindicator.svg"),
            Map.entry("flutter.material.RefreshProgressIndicator", ICON_ROOT + "refreshprogressindicator.svg"),
            Map.entry("flutter.material.RefreshIndicator", ICON_ROOT + "refreshindicator.svg"),
            Map.entry("flutter.material.TextButton", ICON_ROOT + "textbutton.svg"),
            Map.entry("flutter.material.OutlinedButton", ICON_ROOT + "outlinedbutton.svg"),
            Map.entry("flutter.material.FilledButton", ICON_ROOT + "filledbutton.svg"),
            Map.entry("flutter.material.FloatingActionButton", ICON_ROOT + "floatingactionbutton.svg"),
            Map.entry("flutter.material.IconButton", ICON_ROOT + "iconbutton.svg"),
            Map.entry("flutter.material.Checkbox", ICON_ROOT + "checkbox.svg"),
            Map.entry("flutter.material.Switch", ICON_ROOT + "switch.svg"),
            Map.entry("flutter.material.Slider", ICON_ROOT + "slider.svg"),
            Map.entry("flutter.material.RangeSlider", ICON_ROOT + "rangeslider.svg"),
            Map.entry("flutter.material.Radio", ICON_ROOT + "radio.svg"),
            Map.entry("flutter.widgets.ColoredBox", ICON_ROOT + "coloredbox.svg"),
            Map.entry("flutter.widgets.Placeholder", ICON_ROOT + "placeholder.svg"),
            Map.entry("flutter.widgets.Directionality", ICON_ROOT + "directionality.svg"),
            Map.entry("flutter.widgets.DecoratedBox", ICON_ROOT + "decoratedbox.svg"),
            Map.entry("flutter.widgets.ClipRect", ICON_ROOT + "cliprect.svg"),
            Map.entry("flutter.widgets.ClipOval", ICON_ROOT + "clipoval.svg"),
            Map.entry("flutter.widgets.ClipRRect", ICON_ROOT + "cliprrect.svg"),
            Map.entry("flutter.widgets.ClipPath", ICON_ROOT + "clippath.svg"),
            Map.entry("flutter.widgets.ClipRSuperellipse", ICON_ROOT + "cliprsuperellipse.svg"),
            Map.entry("flutter.widgets.PhysicalModel", ICON_ROOT + "physicalmodel.svg"),
            Map.entry("flutter.widgets.PhysicalShape", ICON_ROOT + "physicalshape.svg"),
            Map.entry("flutter.widgets.RepaintBoundary", ICON_ROOT + "repaintboundary.svg"),
            Map.entry("flutter.widgets.MergeSemantics", ICON_ROOT + "mergesemantics.svg"),
            Map.entry("flutter.widgets.IndexedSemantics", ICON_ROOT + "indexedsemantics.svg"),
            Map.entry("flutter.widgets.ExcludeFocus", ICON_ROOT + "excludefocus.svg"),
            Map.entry("flutter.widgets.ExcludeFocusTraversal", ICON_ROOT + "excludefocustraversal.svg"),
            Map.entry("flutter.widgets.Visibility", ICON_ROOT + "visibility.svg"),
            Map.entry("flutter.widgets.TickerMode", ICON_ROOT + "tickermode.svg"),
            Map.entry("flutter.widgets.DefaultTextHeightBehavior", ICON_ROOT + "defaulttextheightbehavior.svg"),
            Map.entry("flutter.widgets.DefaultSelectionStyle", ICON_ROOT + "defaultselectionstyle.svg"),
            Map.entry("flutter.widgets.IconTheme", ICON_ROOT + "icontheme.svg"),
            Map.entry("flutter.widgets.IgnorePointer", ICON_ROOT + "ignorepointer.svg"),
            Map.entry("flutter.widgets.AbsorbPointer", ICON_ROOT + "absorbpointer.svg"),
            Map.entry("flutter.widgets.BlockSemantics", ICON_ROOT + "blocksemantics.svg"),
            Map.entry("flutter.widgets.ExcludeSemantics",
                    ICON_ROOT + "excludesemantics.svg"),
            Map.entry("flutter.widgets.SizedBox", ICON_ROOT + "sizedbox.svg"),
            Map.entry("flutter.widgets.AspectRatio", ICON_ROOT + "aspectratio.svg"),
            Map.entry("flutter.widgets.Container", ICON_ROOT + "container.svg"),
            Map.entry("flutter.widgets.Opacity", ICON_ROOT + "opacity.svg"));

    private FlutterWidgetIconRegistry() {
    }

    /**
     * Finds the dedicated icon base for a reviewed built-in widget type.
     * Unknown and extension types safely return an empty result.
     */
    public static Optional<String> findIconPath(WidgetTypeId typeId) {
        Objects.requireNonNull(typeId, "typeId");
        return Optional.ofNullable(REVIEWED_ICON_PATHS.get(typeId.value()));
    }
}
