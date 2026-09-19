package io.github.vgrytsenko2022.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class FlutterAnalyzeParserTest {
    private final FlutterAnalyzeParser parser = new FlutterAnalyzeParser();

    @Test
    void parsesModernOutputFromRightHandLocation() {
        FlutterAnalyzeIssue issue = parser.parseLine(
                "warning - Prefer x - y when possible - C:\\work\\demo\\lib\\main.dart:12:7 - prefer_final_locals")
                .orElseThrow();

        assertEquals(FlutterAnalyzeSeverity.WARNING, issue.severity());
        assertEquals("Prefer x - y when possible", issue.message());
        assertEquals("C:\\work\\demo\\lib\\main.dart", issue.file());
        assertEquals(12, issue.line());
        assertEquals(7, issue.column());
        assertEquals(Optional.of("prefer_final_locals"), issue.code());
    }

    @Test
    void parsesLegacyOutputWithoutCode() {
        FlutterAnalyzeIssue issue = parser.parseLine(
                "[error] Undefined name 'missing' (lib/main.dart:4:9)")
                .orElseThrow();

        assertEquals(FlutterAnalyzeSeverity.ERROR, issue.severity());
        assertEquals("Undefined name 'missing'", issue.message());
        assertEquals("lib/main.dart", issue.file());
        assertEquals(4, issue.line());
        assertEquals(9, issue.column());
        assertTrue(issue.code().isEmpty());
    }

    @Test
    void acceptsAnsiSeverityAndIgnoresNonIssueLines() {
        FlutterAnalyzeIssue issue = parser.parseLine(
                "\u001B[34minfo\u001B[0m - Use a const constructor - lib/a.dart:2:3 - prefer_const_constructors")
                .orElseThrow();

        assertEquals(FlutterAnalyzeSeverity.INFO, issue.severity());
        assertTrue(parser.parseLine("Analyzing demo...").isEmpty());
        assertTrue(parser.parseLine("No issues found!").isEmpty());
        assertTrue(parser.parseLine("").isEmpty());
    }
}
