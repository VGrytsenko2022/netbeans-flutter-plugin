package dev.flutter.netbeans.designer.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ValidationResultTest {

    @Test
    void copiesIssuesAndSplitsErrorsFromWarnings() {
        ValidationIssue error = issue("designer.error", ValidationSeverity.ERROR);
        ValidationIssue warning = issue("designer.warning", ValidationSeverity.WARNING);
        List<ValidationIssue> mutable = new ArrayList<>(List.of(error, warning));

        ValidationResult result = new ValidationResult(mutable);
        mutable.clear();

        assertEquals(List.of(error, warning), result.issues());
        assertEquals(List.of(error), result.errors());
        assertEquals(List.of(warning), result.warnings());
        assertFalse(result.valid());
        assertThrows(UnsupportedOperationException.class,
                () -> result.issues().add(error));
    }

    @Test
    void warningsDoNotInvalidateTheDocument() {
        ValidationResult result = new ValidationResult(List.of(
                issue("designer.warning", ValidationSeverity.WARNING)));

        assertTrue(result.valid());
    }

    @Test
    void validatesIssueStructure() {
        assertThrows(IllegalArgumentException.class, () -> new ValidationIssue(
                " ", ValidationSeverity.ERROR, "/root", Optional.empty(), "message"));
        assertThrows(NullPointerException.class, () -> new ValidationIssue(
                "code", null, "/root", Optional.empty(), "message"));
        assertThrows(IllegalArgumentException.class, () -> new ValidationIssue(
                "code", ValidationSeverity.ERROR, "root", Optional.empty(), "message"));
        assertThrows(NullPointerException.class, () -> new ValidationIssue(
                "code", ValidationSeverity.ERROR, "/root", null, "message"));
        assertThrows(IllegalArgumentException.class, () -> new ValidationIssue(
                "code", ValidationSeverity.ERROR, "/root", Optional.empty(), " "));
    }

    private static ValidationIssue issue(String code, ValidationSeverity severity) {
        return new ValidationIssue(code, severity, "/root", Optional.empty(), code);
    }
}
