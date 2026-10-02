package io.github.vgrytsenko2022.designer.model;

import java.util.regex.Pattern;

/** Named Flutter constructor slot stored on a widget node. */
public record SlotName(String value) {
    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public SlotName {
        value = ModelConstraints.matching(value, "slot name", NAME);
    }

    @Override
    public String toString() {
        return value;
    }
}
