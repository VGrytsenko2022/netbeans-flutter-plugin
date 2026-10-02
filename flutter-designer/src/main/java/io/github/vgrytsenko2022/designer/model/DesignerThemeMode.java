package io.github.vgrytsenko2022.designer.model;

/** Theme used for the design-time canvas projection. */
public enum DesignerThemeMode {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    private final String wireName;

    DesignerThemeMode(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}
