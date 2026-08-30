package dev.flutter.netbeans.project.theme;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Closed schema-v5 catalog of component-theme color leaves.
 *
 * <p>The declaration order is the canonical JSON, editor and Dart-generation
 * order.  No arbitrary component or Dart-expression key is admitted.</p>
 */
public enum FlutterThemeComponentColorRole {
    SCAFFOLD_BACKGROUND("scaffold.backgroundColor", "Scaffold", "Background"),

    APP_BAR_BACKGROUND("appBar.backgroundColor", "AppBar", "Background"),
    APP_BAR_FOREGROUND("appBar.foregroundColor", "AppBar", "Foreground"),
    APP_BAR_SHADOW("appBar.shadowColor", "AppBar", "Shadow"),
    APP_BAR_SURFACE_TINT("appBar.surfaceTintColor", "AppBar", "Surface tint"),

    ICON_COLOR("icon.color", "Icon", "Color"),

    ELEVATED_BUTTON_BACKGROUND_DEFAULT("elevatedButton.backgroundColor.default", "ElevatedButton", "Background / default"),
    ELEVATED_BUTTON_BACKGROUND_DISABLED("elevatedButton.backgroundColor.disabled", "ElevatedButton", "Background / disabled"),
    ELEVATED_BUTTON_BACKGROUND_PRESSED("elevatedButton.backgroundColor.pressed", "ElevatedButton", "Background / pressed"),
    ELEVATED_BUTTON_BACKGROUND_HOVERED("elevatedButton.backgroundColor.hovered", "ElevatedButton", "Background / hovered"),
    ELEVATED_BUTTON_BACKGROUND_FOCUSED("elevatedButton.backgroundColor.focused", "ElevatedButton", "Background / focused"),
    ELEVATED_BUTTON_FOREGROUND_DEFAULT("elevatedButton.foregroundColor.default", "ElevatedButton", "Foreground / default"),
    ELEVATED_BUTTON_FOREGROUND_DISABLED("elevatedButton.foregroundColor.disabled", "ElevatedButton", "Foreground / disabled"),
    ELEVATED_BUTTON_FOREGROUND_PRESSED("elevatedButton.foregroundColor.pressed", "ElevatedButton", "Foreground / pressed"),
    ELEVATED_BUTTON_FOREGROUND_HOVERED("elevatedButton.foregroundColor.hovered", "ElevatedButton", "Foreground / hovered"),
    ELEVATED_BUTTON_FOREGROUND_FOCUSED("elevatedButton.foregroundColor.focused", "ElevatedButton", "Foreground / focused"),
    ELEVATED_BUTTON_OVERLAY_DEFAULT("elevatedButton.overlayColor.default", "ElevatedButton", "Overlay / default"),
    ELEVATED_BUTTON_OVERLAY_DISABLED("elevatedButton.overlayColor.disabled", "ElevatedButton", "Overlay / disabled"),
    ELEVATED_BUTTON_OVERLAY_PRESSED("elevatedButton.overlayColor.pressed", "ElevatedButton", "Overlay / pressed"),
    ELEVATED_BUTTON_OVERLAY_HOVERED("elevatedButton.overlayColor.hovered", "ElevatedButton", "Overlay / hovered"),
    ELEVATED_BUTTON_OVERLAY_FOCUSED("elevatedButton.overlayColor.focused", "ElevatedButton", "Overlay / focused"),
    ELEVATED_BUTTON_SHADOW_DEFAULT("elevatedButton.shadowColor.default", "ElevatedButton", "Shadow / default"),
    ELEVATED_BUTTON_SHADOW_DISABLED("elevatedButton.shadowColor.disabled", "ElevatedButton", "Shadow / disabled"),
    ELEVATED_BUTTON_SHADOW_PRESSED("elevatedButton.shadowColor.pressed", "ElevatedButton", "Shadow / pressed"),
    ELEVATED_BUTTON_SHADOW_HOVERED("elevatedButton.shadowColor.hovered", "ElevatedButton", "Shadow / hovered"),
    ELEVATED_BUTTON_SHADOW_FOCUSED("elevatedButton.shadowColor.focused", "ElevatedButton", "Shadow / focused"),
    ELEVATED_BUTTON_SURFACE_TINT_DEFAULT("elevatedButton.surfaceTintColor.default", "ElevatedButton", "Surface tint / default"),
    ELEVATED_BUTTON_SURFACE_TINT_DISABLED("elevatedButton.surfaceTintColor.disabled", "ElevatedButton", "Surface tint / disabled"),
    ELEVATED_BUTTON_SURFACE_TINT_PRESSED("elevatedButton.surfaceTintColor.pressed", "ElevatedButton", "Surface tint / pressed"),
    ELEVATED_BUTTON_SURFACE_TINT_HOVERED("elevatedButton.surfaceTintColor.hovered", "ElevatedButton", "Surface tint / hovered"),
    ELEVATED_BUTTON_SURFACE_TINT_FOCUSED("elevatedButton.surfaceTintColor.focused", "ElevatedButton", "Surface tint / focused"),
    ELEVATED_BUTTON_ICON_DEFAULT("elevatedButton.iconColor.default", "ElevatedButton", "Icon / default"),
    ELEVATED_BUTTON_ICON_DISABLED("elevatedButton.iconColor.disabled", "ElevatedButton", "Icon / disabled"),
    ELEVATED_BUTTON_ICON_PRESSED("elevatedButton.iconColor.pressed", "ElevatedButton", "Icon / pressed"),
    ELEVATED_BUTTON_ICON_HOVERED("elevatedButton.iconColor.hovered", "ElevatedButton", "Icon / hovered"),
    ELEVATED_BUTTON_ICON_FOCUSED("elevatedButton.iconColor.focused", "ElevatedButton", "Icon / focused");

    public static final int LEAF_COUNT = 36;

    static {
        if (values().length != LEAF_COUNT) {
            throw new ExceptionInInitializerError(
                    "Component color catalog must contain exactly " + LEAF_COUNT + " leaves");
        }
    }

    private final String wireName;
    private final String componentLabel;
    private final String leafLabel;

    FlutterThemeComponentColorRole(
            String wireName, String componentLabel, String leafLabel) {
        this.wireName = Objects.requireNonNull(wireName, "wireName");
        this.componentLabel = Objects.requireNonNull(componentLabel, "componentLabel");
        this.leafLabel = Objects.requireNonNull(leafLabel, "leafLabel");
    }

    public String wireName() {
        return wireName;
    }

    public String componentLabel() {
        return componentLabel;
    }

    public String leafLabel() {
        return leafLabel;
    }

    public String displayName() {
        return componentLabel + " — " + leafLabel;
    }

    public static FlutterThemeComponentColorRole fromWireName(String value) {
        Objects.requireNonNull(value, "value");
        return Arrays.stream(values())
                .filter(role -> role.wireName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported component color role: " + value));
    }

    /** ElevatedButton ButtonStyle property, or {@code null} for other components. */
    public String elevatedButtonProperty() {
        if (!wireName.startsWith("elevatedButton.")) {
            return null;
        }
        return wireName.substring("elevatedButton.".length(), wireName.lastIndexOf('.'));
    }

    /** ElevatedButton state wire name, or {@code null} for other components. */
    public String elevatedButtonState() {
        if (!wireName.startsWith("elevatedButton.")) {
            return null;
        }
        return wireName.substring(wireName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
