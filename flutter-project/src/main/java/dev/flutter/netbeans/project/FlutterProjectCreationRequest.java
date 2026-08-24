package dev.flutter.netbeans.project;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/** Values used to create a standard Flutter application. */
public record FlutterProjectCreationRequest(
        Path parentDirectory,
        String projectName,
        String organization,
        String description) {

    private static final Pattern PROJECT_NAME = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern ORGANIZATION = Pattern.compile(
            "[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+");

    public FlutterProjectCreationRequest {
        parentDirectory = Objects.requireNonNull(parentDirectory, "parentDirectory")
                .toAbsolutePath().normalize();
        projectName = Objects.requireNonNull(projectName, "projectName").trim();
        organization = Objects.requireNonNull(organization, "organization").trim();
        description = Objects.requireNonNull(description, "description").trim();

        if (!isValidProjectName(projectName)) {
            throw new IllegalArgumentException(
                    "Flutter project name must start with a lowercase letter and contain only lowercase letters, digits, and underscores.");
        }
        if (!isValidOrganization(organization)) {
            throw new IllegalArgumentException(
                    "Flutter organization must be a reverse-domain identifier such as com.example.");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("Flutter project description is required.");
        }
    }

    public Path targetDirectory() {
        return parentDirectory.resolve(projectName).normalize();
    }

    public static boolean isValidProjectName(String value) {
        return value != null && PROJECT_NAME.matcher(value.trim()).matches();
    }

    public static boolean isValidOrganization(String value) {
        return value != null && ORGANIZATION.matcher(value.trim()).matches();
    }
}
