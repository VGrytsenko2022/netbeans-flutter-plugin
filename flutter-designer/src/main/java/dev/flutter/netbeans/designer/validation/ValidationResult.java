package dev.flutter.netbeans.designer.validation;

import java.util.List;
import java.util.Objects;

/** Immutable result of validating one designer document. */
public record ValidationResult(List<ValidationIssue> issues) {

    public ValidationResult {
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public boolean valid() {
        return issues.stream()
                .noneMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
    }

    public List<ValidationIssue> errors() {
        return issues.stream()
                .filter(issue -> issue.severity() == ValidationSeverity.ERROR)
                .toList();
    }

    public List<ValidationIssue> warnings() {
        return issues.stream()
                .filter(issue -> issue.severity() == ValidationSeverity.WARNING)
                .toList();
    }
}
