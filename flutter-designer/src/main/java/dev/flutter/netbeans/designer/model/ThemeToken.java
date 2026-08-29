package dev.flutter.netbeans.designer.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Syntactically valid semantic Material theme token.
 *
 * <p>The model deliberately validates only the stable namespace and role
 * spelling. Whether a particular role is supported by the active Flutter SDK
 * is a catalog concern rather than a persistence concern.</p>
 */
public record ThemeToken(String wireId) {
    private static final Pattern WIRE_ID = Pattern.compile(
            "material\\.(?:colorScheme|textTheme)\\.[a-z][A-Za-z0-9]*");
    private static final String COLOR_SCHEME_PREFIX = "material.colorScheme.";
    private static final String TEXT_THEME_PREFIX = "material.textTheme.";

    public ThemeToken {
        Objects.requireNonNull(wireId, "wireId");
        if (wireId.length() > 128 || !WIRE_ID.matcher(wireId).matches()) {
            throw new IllegalArgumentException(
                    "Theme token must use material.colorScheme.<role> or "
                    + "material.textTheme.<role>: " + wireId);
        }
    }

    public boolean isColorSchemeToken() {
        return wireId.startsWith(COLOR_SCHEME_PREFIX);
    }

    public boolean isTextThemeToken() {
        return wireId.startsWith(TEXT_THEME_PREFIX);
    }

    public String role() {
        int separator = wireId.lastIndexOf('.');
        return wireId.substring(separator + 1);
    }
}
