package io.github.vgrytsenko2022.designer.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Optional, non-semantic design-time canvas preferences. */
public record CanvasPreferences(
        Optional<String> preset,
        Optional<BigDecimal> logicalWidth,
        Optional<BigDecimal> logicalHeight,
        Optional<BigDecimal> devicePixelRatio,
        Optional<CanvasOrientation> orientation,
        Optional<DesignerThemeMode> themeMode,
        Optional<BigDecimal> textScaleFactor,
        Optional<String> locale) {

    private static final BigDecimal MAX_LOGICAL_SIZE = new BigDecimal("10000");
    private static final BigDecimal MAX_DEVICE_PIXEL_RATIO = new BigDecimal("10");
    private static final BigDecimal MAX_TEXT_SCALE_FACTOR = new BigDecimal("5");
    private static final Pattern LOCALE = Pattern.compile(
            "[A-Za-z]{2,8}(?:[-_][A-Za-z0-9]{2,8})*");

    public CanvasPreferences {
        Objects.requireNonNull(preset, "preset");
        Objects.requireNonNull(logicalWidth, "logicalWidth");
        Objects.requireNonNull(logicalHeight, "logicalHeight");
        Objects.requireNonNull(devicePixelRatio, "devicePixelRatio");
        Objects.requireNonNull(orientation, "orientation");
        Objects.requireNonNull(themeMode, "themeMode");
        Objects.requireNonNull(textScaleFactor, "textScaleFactor");
        Objects.requireNonNull(locale, "locale");

        preset = preset.map(value -> ModelConstraints.codePointLength(value, "preset", 1, 80));
        logicalWidth = logicalWidth.map(value -> ModelConstraints.normalizedNumber(value, "logicalWidth"));
        logicalHeight = logicalHeight.map(value -> ModelConstraints.normalizedNumber(value, "logicalHeight"));
        devicePixelRatio = devicePixelRatio.map(value ->
                ModelConstraints.normalizedNumber(value, "devicePixelRatio"));
        textScaleFactor = textScaleFactor.map(value ->
                ModelConstraints.normalizedNumber(value, "textScaleFactor"));
        logicalWidth.ifPresent(value -> requirePositiveAtMost(value, MAX_LOGICAL_SIZE, "logicalWidth"));
        logicalHeight.ifPresent(value -> requirePositiveAtMost(value, MAX_LOGICAL_SIZE, "logicalHeight"));
        devicePixelRatio.ifPresent(value ->
                requirePositiveAtMost(value, MAX_DEVICE_PIXEL_RATIO, "devicePixelRatio"));
        textScaleFactor.ifPresent(value ->
                requirePositiveAtMost(value, MAX_TEXT_SCALE_FACTOR, "textScaleFactor"));
        locale = locale.map(value -> ModelConstraints.matching(value, "locale", LOCALE));
    }

    public static CanvasPreferences empty() {
        return new CanvasPreferences(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static void requirePositiveAtMost(BigDecimal value, BigDecimal maximum, String label) {
        Objects.requireNonNull(value, label);
        if (value.signum() <= 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(label + " must be greater than zero and at most " + maximum.toPlainString());
        }
    }
}
