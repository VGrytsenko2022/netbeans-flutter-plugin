package dev.flutter.netbeans.designer.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

final class ModelConstraints {
    static final Pattern DART_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    static final Pattern QUALIFIED_DART_IDENTIFIER = Pattern.compile(
            "[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*");
    static final Pattern CALLBACK_HANDLER = Pattern.compile("_?[A-Za-z][A-Za-z0-9_]*");

    private ModelConstraints() {
    }

    static String matching(String value, String label, Pattern pattern) {
        Objects.requireNonNull(value, label);
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " does not match the Flutter Designer document contract: " + value);
        }
        return value;
    }

    static String codePointLength(String value, String label, int minimum, int maximum) {
        Objects.requireNonNull(value, label);
        int length = value.codePointCount(0, value.length());
        if (length < minimum || length > maximum) {
            throw new IllegalArgumentException(
                    label + " must contain between " + minimum + " and " + maximum + " Unicode code points");
        }
        return value;
    }

    static BigDecimal normalizedNumber(BigDecimal value, String label) {
        Objects.requireNonNull(value, label);
        if (value.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return value.stripTrailingZeros();
    }

    static <K, V> Map<K, V> immutableLinkedMap(Map<K, V> values, String label) {
        Objects.requireNonNull(values, label);
        LinkedHashMap<K, V> copy = new LinkedHashMap<>(values.size());
        values.forEach((key, value) -> copy.put(
                Objects.requireNonNull(key, label + " key"),
                Objects.requireNonNull(value, label + " value")));
        return Collections.unmodifiableMap(copy);
    }
}
