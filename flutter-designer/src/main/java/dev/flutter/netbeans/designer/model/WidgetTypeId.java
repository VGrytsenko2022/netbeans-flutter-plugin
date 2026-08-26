package dev.flutter.netbeans.designer.model;

import java.util.regex.Pattern;

/** Stable widget catalog type identifier. */
public record WidgetTypeId(String value) {
    private static final Pattern TYPE_ID = Pattern.compile("[A-Za-z][A-Za-z0-9_.:/#-]{0,254}");

    public WidgetTypeId {
        value = ModelConstraints.matching(value, "widget type id", TYPE_ID);
    }

    @Override
    public String toString() {
        return value;
    }
}
