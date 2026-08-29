package dev.flutter.netbeans.designer.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** A designer color resolved either from an ARGB literal or a Material theme role. */
public sealed interface ColorSource permits ColorSource.Literal, ColorSource.Theme {
    record Literal(long argb) implements ColorSource {
        private static final long MAX_ARGB = 0xFFFF_FFFFL;
        private static final Pattern WIRE_ARGB = Pattern.compile("0x[0-9A-F]{8}");

        public Literal {
            if (argb < 0 || argb > MAX_ARGB) {
                throw new IllegalArgumentException(
                        "ARGB color must be between 0x00000000 and 0xFFFFFFFF");
            }
        }

        public static Literal fromWireArgb(String value) {
            Objects.requireNonNull(value, "value");
            if (!WIRE_ARGB.matcher(value).matches()) {
                throw new IllegalArgumentException(
                        "ARGB color must use the form 0xAARRGGBB: " + value);
            }
            return new Literal(Long.parseUnsignedLong(value.substring(2), 16));
        }

        public String wireArgb() {
            return String.format(Locale.ROOT, "0x%08X", argb);
        }
    }

    record Theme(ThemeToken token) implements ColorSource {
        public Theme {
            Objects.requireNonNull(token, "token");
            if (!token.isColorSchemeToken()) {
                throw new IllegalArgumentException(
                        "A color source requires a material.colorScheme theme token");
            }
        }
    }
}
