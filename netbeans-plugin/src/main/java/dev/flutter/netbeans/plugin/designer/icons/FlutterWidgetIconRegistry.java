package dev.flutter.netbeans.plugin.designer.icons;

import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Central mapping between the reviewed CORE_V1 widget identities and their
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

    private static final Map<String, String> CORE_ICON_PATHS = Map.of(
            "flutter.material.Scaffold", ICON_ROOT + "scaffold.svg",
            "flutter.widgets.Column", ICON_ROOT + "column.svg",
            "flutter.widgets.Row", ICON_ROOT + "row.svg",
            "flutter.widgets.Padding", ICON_ROOT + "padding.svg",
            "flutter.widgets.Center", ICON_ROOT + "center.svg",
            "flutter.widgets.Text", ICON_ROOT + "text.svg");

    private FlutterWidgetIconRegistry() {
    }

    /**
     * Finds the dedicated icon base for a reviewed CORE_V1 widget type.
     * Unknown and extension types safely return an empty result.
     */
    public static Optional<String> findIconPath(WidgetTypeId typeId) {
        Objects.requireNonNull(typeId, "typeId");
        return Optional.ofNullable(CORE_ICON_PATHS.get(typeId.value()));
    }
}
