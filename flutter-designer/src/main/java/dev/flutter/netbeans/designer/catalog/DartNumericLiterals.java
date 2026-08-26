package dev.flutter.netbeans.designer.catalog;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** Cross-target representability rules for numeric literals emitted as Dart source. */
public final class DartNumericLiterals {
    public static final BigInteger MAX_PORTABLE_INTEGER =
            BigInteger.ONE.shiftLeft(53).subtract(BigInteger.ONE);
    public static final BigInteger MIN_PORTABLE_INTEGER = MAX_PORTABLE_INTEGER.negate();

    private DartNumericLiterals() {
    }

    /**
     * Returns whether an integer is exact on both native Dart and JavaScript
     * targets.
     */
    public static boolean isPortableInteger(BigInteger value) {
        Objects.requireNonNull(value, "value");
        return value.compareTo(MIN_PORTABLE_INTEGER) >= 0
                && value.compareTo(MAX_PORTABLE_INTEGER) <= 0;
    }

    /**
     * Returns whether a decimal survives conversion to a finite Dart double
     * without underflow or a value-changing shortest-literal round trip.
     */
    public static boolean isRepresentableDouble(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        double converted = value.doubleValue();
        if (!Double.isFinite(converted)
                || (value.signum() != 0 && converted == 0.0d)) {
            return false;
        }
        return BigDecimal.valueOf(converted).compareTo(value) == 0;
    }
}
