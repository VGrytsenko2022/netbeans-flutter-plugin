package dev.flutter.netbeans.designer.canvas;

import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Canonical resolved locale identity for one Canvas presentation. */
public record CanvasLocale(String languageTag) {
    private static final int MAX_LANGUAGE_TAG_CODE_POINTS = 64;
    private static final Pattern SUPPORTED_LANGUAGE_TAG = Pattern.compile(
            "[A-Za-z]{2,8}(?:[-_][A-Za-z0-9]{2,8})*");

    public CanvasLocale {
        Objects.requireNonNull(languageTag, "languageTag");
        if (!languageTag.equals(languageTag.strip())) {
            throw new IllegalArgumentException(
                    "languageTag must not contain leading or trailing whitespace");
        }
        int length = languageTag.codePointCount(0, languageTag.length());
        if (length == 0 || length > MAX_LANGUAGE_TAG_CODE_POINTS) {
            throw new IllegalArgumentException(
                    "languageTag must contain between 1 and "
                    + MAX_LANGUAGE_TAG_CODE_POINTS + " Unicode code points");
        }
        if (!SUPPORTED_LANGUAGE_TAG.matcher(languageTag).matches()) {
            throw new IllegalArgumentException(
                    "languageTag must be a supported BCP-47-style locale tag: "
                    + languageTag);
        }
        try {
            languageTag = new Locale.Builder()
                    .setLanguageTag(languageTag.replace('_', '-'))
                    .build()
                    .toLanguageTag();
        } catch (IllformedLocaleException exception) {
            throw new IllegalArgumentException(
                    "languageTag must be a well-formed locale tag: " + languageTag,
                    exception);
        }
    }
}
