package dev.flutter.netbeans.designer.canvas;

import java.util.Arrays;
import java.util.Objects;

/** Closed protocol-v9 component-theme color leaf catalog. */
public enum CanvasThemeComponentColorRole {
    SCAFFOLD_BACKGROUND("scaffold.backgroundColor"),
    APP_BAR_BACKGROUND("appBar.backgroundColor"),
    APP_BAR_FOREGROUND("appBar.foregroundColor"),
    APP_BAR_SHADOW("appBar.shadowColor"),
    APP_BAR_SURFACE_TINT("appBar.surfaceTintColor"),
    ICON_COLOR("icon.color"),
    ELEVATED_BUTTON_BACKGROUND_DEFAULT("elevatedButton.backgroundColor.default"),
    ELEVATED_BUTTON_BACKGROUND_DISABLED("elevatedButton.backgroundColor.disabled"),
    ELEVATED_BUTTON_BACKGROUND_PRESSED("elevatedButton.backgroundColor.pressed"),
    ELEVATED_BUTTON_BACKGROUND_HOVERED("elevatedButton.backgroundColor.hovered"),
    ELEVATED_BUTTON_BACKGROUND_FOCUSED("elevatedButton.backgroundColor.focused"),
    ELEVATED_BUTTON_FOREGROUND_DEFAULT("elevatedButton.foregroundColor.default"),
    ELEVATED_BUTTON_FOREGROUND_DISABLED("elevatedButton.foregroundColor.disabled"),
    ELEVATED_BUTTON_FOREGROUND_PRESSED("elevatedButton.foregroundColor.pressed"),
    ELEVATED_BUTTON_FOREGROUND_HOVERED("elevatedButton.foregroundColor.hovered"),
    ELEVATED_BUTTON_FOREGROUND_FOCUSED("elevatedButton.foregroundColor.focused"),
    ELEVATED_BUTTON_OVERLAY_DEFAULT("elevatedButton.overlayColor.default"),
    ELEVATED_BUTTON_OVERLAY_DISABLED("elevatedButton.overlayColor.disabled"),
    ELEVATED_BUTTON_OVERLAY_PRESSED("elevatedButton.overlayColor.pressed"),
    ELEVATED_BUTTON_OVERLAY_HOVERED("elevatedButton.overlayColor.hovered"),
    ELEVATED_BUTTON_OVERLAY_FOCUSED("elevatedButton.overlayColor.focused"),
    ELEVATED_BUTTON_SHADOW_DEFAULT("elevatedButton.shadowColor.default"),
    ELEVATED_BUTTON_SHADOW_DISABLED("elevatedButton.shadowColor.disabled"),
    ELEVATED_BUTTON_SHADOW_PRESSED("elevatedButton.shadowColor.pressed"),
    ELEVATED_BUTTON_SHADOW_HOVERED("elevatedButton.shadowColor.hovered"),
    ELEVATED_BUTTON_SHADOW_FOCUSED("elevatedButton.shadowColor.focused"),
    ELEVATED_BUTTON_SURFACE_TINT_DEFAULT("elevatedButton.surfaceTintColor.default"),
    ELEVATED_BUTTON_SURFACE_TINT_DISABLED("elevatedButton.surfaceTintColor.disabled"),
    ELEVATED_BUTTON_SURFACE_TINT_PRESSED("elevatedButton.surfaceTintColor.pressed"),
    ELEVATED_BUTTON_SURFACE_TINT_HOVERED("elevatedButton.surfaceTintColor.hovered"),
    ELEVATED_BUTTON_SURFACE_TINT_FOCUSED("elevatedButton.surfaceTintColor.focused"),
    ELEVATED_BUTTON_ICON_DEFAULT("elevatedButton.iconColor.default"),
    ELEVATED_BUTTON_ICON_DISABLED("elevatedButton.iconColor.disabled"),
    ELEVATED_BUTTON_ICON_PRESSED("elevatedButton.iconColor.pressed"),
    ELEVATED_BUTTON_ICON_HOVERED("elevatedButton.iconColor.hovered"),
    ELEVATED_BUTTON_ICON_FOCUSED("elevatedButton.iconColor.focused");

    public static final int LEAF_COUNT = 36;

    static {
        if (values().length != LEAF_COUNT) {
            throw new ExceptionInInitializerError(
                    "Canvas component color catalog must contain exactly "
                    + LEAF_COUNT + " leaves");
        }
    }

    private final String wireName;

    CanvasThemeComponentColorRole(String wireName) {
        this.wireName = Objects.requireNonNull(wireName, "wireName");
    }

    public String wireName() {
        return wireName;
    }

    public static CanvasThemeComponentColorRole fromWireName(String value) {
        Objects.requireNonNull(value, "value");
        return Arrays.stream(values())
                .filter(role -> role.wireName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Canvas component color role: " + value));
    }
}
