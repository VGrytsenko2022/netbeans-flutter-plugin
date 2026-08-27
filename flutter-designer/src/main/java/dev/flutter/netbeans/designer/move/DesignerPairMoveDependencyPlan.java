package dev.flutter.netbeans.designer.move;

import java.util.List;
import java.util.Objects;

/** Immutable proof summary for one project-wide safe move dependency check. */
public record DesignerPairMoveDependencyPlan(
        String packageName,
        String originalLibRelativePath,
        String targetLibRelativePath,
        List<String> inspectedProjectRelativePaths,
        long inspectedSourceBytes,
        int inspectedDirectiveUris) {

    public DesignerPairMoveDependencyPlan {
        packageName = requireNonBlank(packageName, "packageName");
        originalLibRelativePath = requireNonBlank(
                originalLibRelativePath, "originalLibRelativePath");
        targetLibRelativePath = requireNonBlank(
                targetLibRelativePath, "targetLibRelativePath");
        inspectedProjectRelativePaths = List.copyOf(Objects.requireNonNull(
                inspectedProjectRelativePaths,
                "inspectedProjectRelativePaths"));
        if (originalLibRelativePath.equals(targetLibRelativePath)) {
            throw new IllegalArgumentException("Move paths must be different");
        }
        if (inspectedSourceBytes < 0) {
            throw new IllegalArgumentException(
                    "inspectedSourceBytes must not be negative");
        }
        if (inspectedDirectiveUris < 0) {
            throw new IllegalArgumentException(
                    "inspectedDirectiveUris must not be negative");
        }
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
