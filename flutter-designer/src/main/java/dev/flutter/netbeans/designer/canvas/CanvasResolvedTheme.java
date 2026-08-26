package dev.flutter.netbeans.designer.canvas;

import java.util.Objects;

/**
 * Exact resolved theme identity used by the Flutter Canvas runtime.
 *
 * <p>{@code themeIdentity} is a host-issued bounded id or digest for the
 * resolved ThemeData inputs. Brightness alone is deliberately insufficient:
 * two custom light themes may render different pixels.</p>
 */
public record CanvasResolvedTheme(
        String themeIdentity,
        CanvasThemeBrightness brightness) {
    private static final int MAX_IDENTITY_CODE_POINTS = 128;

    public CanvasResolvedTheme {
        Objects.requireNonNull(themeIdentity, "themeIdentity");
        Objects.requireNonNull(brightness, "brightness");
        if (!themeIdentity.equals(themeIdentity.strip())) {
            throw new IllegalArgumentException(
                    "themeIdentity must not contain leading or trailing whitespace");
        }
        int length = themeIdentity.codePointCount(0, themeIdentity.length());
        if (length == 0 || length > MAX_IDENTITY_CODE_POINTS) {
            throw new IllegalArgumentException(
                    "themeIdentity must contain between 1 and "
                    + MAX_IDENTITY_CODE_POINTS + " Unicode code points");
        }
        if (themeIdentity.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "themeIdentity must not contain control characters");
        }
    }
}
