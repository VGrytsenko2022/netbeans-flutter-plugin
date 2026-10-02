package io.github.vgrytsenko2022.designer.codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** Canonical, expansion-bounded JSON number spelling used by the {@code .fd} writer. */
final class CanonicalJsonNumbers {
    private static final int MIN_PLAIN_ADJUSTED_EXPONENT = -6;
    private static final int MAX_PLAIN_ADJUSTED_EXPONENT = 20;

    private CanonicalJsonNumbers() {
    }

    static String integer(
            BigInteger value,
            FdCodecLimits limits,
            String pointer) throws FdEncodeException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(pointer, "pointer");

        int maximumCharacters = effectiveMaximumCharacters(limits);
        int signCharacters = value.signum() < 0 ? 1 : 0;
        int bitLength = value.abs().bitLength();
        if (bitLength > 0) {
            // log10(2) > 0.3. This is a safe lower bound that lets us reject
            // enormous values before BigInteger allocates their decimal text.
            long minimumDigits = ((long) (bitLength - 1) * 3L) / 10L + 1L;
            if (minimumDigits + signCharacters > maximumCharacters) {
                throw lengthLimit(
                        pointer, minimumDigits + signCharacters, limits);
            }
        }

        String token = value.toString();
        requireTokenLength(token, limits, pointer);
        return token;
    }

    static String decimal(
            BigDecimal value,
            FdCodecLimits limits,
            String pointer) throws FdEncodeException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(pointer, "pointer");

        if (value.signum() == 0) {
            return "0";
        }

        BigDecimal normalized = value.stripTrailingZeros();
        int scale = normalized.scale();
        if (scale > limits.maxAbsoluteDecimalScale()
                || scale < -limits.maxAbsoluteDecimalScale()) {
            throw new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                    FdCodecDiagnosticCode.NUMBER_RANGE,
                    pointer,
                    "Decimal scale " + scale + " exceeds the configured absolute limit "
                            + limits.maxAbsoluteDecimalScale()));
        }

        int precision = normalized.precision();
        long adjustedExponent = (long) precision - (long) scale - 1L;
        int effectiveMaximum = effectiveMaximumCharacters(limits);
        long plainLength = plainLength(normalized.signum(), precision, scale);
        if (adjustedExponent >= MIN_PLAIN_ADJUSTED_EXPONENT
                && adjustedExponent <= MAX_PLAIN_ADJUSTED_EXPONENT
                && plainLength <= effectiveMaximum) {
            String token = normalized.toPlainString();
            requireTokenLength(token, limits, pointer);
            return token;
        }

        long scientificLength = scientificLength(
                normalized.signum(), precision, adjustedExponent);
        if (scientificLength > effectiveMaximum) {
            throw lengthLimit(pointer, scientificLength, limits);
        }

        String digits = normalized.unscaledValue().abs().toString();
        StringBuilder token = new StringBuilder((int) scientificLength);
        if (normalized.signum() < 0) {
            token.append('-');
        }
        token.append(digits.charAt(0));
        if (digits.length() > 1) {
            token.append('.').append(digits, 1, digits.length());
        }
        token.append('e').append(adjustedExponent);
        String result = token.toString();
        requireTokenLength(result, limits, pointer);
        return result;
    }

    private static long plainLength(int signum, int precision, int scale) {
        long sign = signum < 0 ? 1L : 0L;
        if (scale <= 0) {
            return sign + (long) precision - (long) scale;
        }
        if (scale < precision) {
            return sign + (long) precision + 1L;
        }
        return sign + 2L + (long) scale;
    }

    private static long scientificLength(
            int signum,
            int precision,
            long adjustedExponent) {
        long sign = signum < 0 ? 1L : 0L;
        long significand = precision + (precision > 1 ? 1L : 0L);
        long exponentSign = adjustedExponent < 0 ? 1L : 0L;
        long exponentDigits = decimalDigits(adjustedExponent);
        return sign + significand + 1L + exponentSign + exponentDigits;
    }

    private static int decimalDigits(long value) {
        long magnitude = value < 0 ? -value : value;
        int digits = 1;
        while (magnitude >= 10L) {
            magnitude /= 10L;
            digits++;
        }
        return digits;
    }

    private static int effectiveMaximumCharacters(FdCodecLimits limits) {
        return Math.min(limits.maxNumberCharacters(), limits.maxDocumentBytes());
    }

    private static void requireTokenLength(
            String token,
            FdCodecLimits limits,
            String pointer) throws FdEncodeException {
        if (token.length() > effectiveMaximumCharacters(limits)) {
            throw lengthLimit(pointer, token.length(), limits);
        }
    }

    private static FdEncodeException lengthLimit(
            String pointer,
            long length,
            FdCodecLimits limits) {
        if (length > limits.maxNumberCharacters()) {
            return tokenLimit(pointer, limits.maxNumberCharacters());
        }
        return new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                FdCodecDiagnosticCode.RESOURCE_LIMIT,
                pointer,
                "Canonical number cannot fit within maxDocumentBytes="
                        + limits.maxDocumentBytes()));
    }

    private static FdEncodeException tokenLimit(String pointer, int maximum) {
        return new FdEncodeException(FdCodecDiagnostic.withoutLocation(
                FdCodecDiagnosticCode.RESOURCE_LIMIT,
                pointer,
                "Canonical number exceeds maxNumberCharacters=" + maximum));
    }
}
