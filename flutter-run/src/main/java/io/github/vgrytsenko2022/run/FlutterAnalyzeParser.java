package io.github.vgrytsenko2022.run;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses individual streaming lines produced by {@code flutter analyze}. */
public final class FlutterAnalyzeParser {
    private static final Pattern ANSI_ESCAPE =
            Pattern.compile("\\x1B(?:\\[[0-?]*[ -/]*[@-~]|\\][^\\x07]*(?:\\x07|\\x1B\\\\))");
    private static final Pattern MODERN = Pattern.compile(
            "^(error|warning|info)\\s+-\\s+(.*)\\s+-\\s+(.+):(\\d+):(\\d+)\\s+-\\s+(\\S+)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY = Pattern.compile(
            "^\\[(error|warning|info)]\\s+(.*)\\s+\\((.+):(\\d+):(\\d+)\\)\\s*$",
            Pattern.CASE_INSENSITIVE);

    public Optional<FlutterAnalyzeIssue> parseLine(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return Optional.empty();
        }
        String line = ANSI_ESCAPE.matcher(rawLine).replaceAll("").strip();
        Matcher modern = MODERN.matcher(line);
        if (modern.matches()) {
            return issue(
                    modern.group(1),
                    modern.group(2),
                    modern.group(3),
                    modern.group(4),
                    modern.group(5),
                    modern.group(6));
        }
        Matcher legacy = LEGACY.matcher(line);
        if (legacy.matches()) {
            return issue(
                    legacy.group(1),
                    legacy.group(2),
                    legacy.group(3),
                    legacy.group(4),
                    legacy.group(5),
                    null);
        }
        return Optional.empty();
    }

    private static Optional<FlutterAnalyzeIssue> issue(
            String severity,
            String message,
            String file,
            String line,
            String column,
            String code) {
        return FlutterAnalyzeSeverity.parse(severity).map(value -> new FlutterAnalyzeIssue(
                value,
                message,
                file,
                Integer.parseInt(line),
                Integer.parseInt(column),
                Optional.ofNullable(code)));
    }
}
