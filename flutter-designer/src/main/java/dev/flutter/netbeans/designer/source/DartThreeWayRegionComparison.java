package dev.flutter.netbeans.designer.source;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Immutable normalized-hash evidence for one schema-version-1 managed region. */
public record DartThreeWayRegionComparison(
        String id,
        Optional<DartManagedRegionSnapshot> actual,
        String declaredNormalizedSha256,
        Optional<String> generatedNormalizedSha256) {

    private static final Pattern SHA_256 = Pattern.compile("[0-9A-F]{64}");

    public DartThreeWayRegionComparison {
        if (!DartSourceIntegrityScanner.IMPORTS_REGION.equals(id)
                && !DartSourceIntegrityScanner.BUILD_REGION.equals(id)) {
            throw new IllegalArgumentException(
                    "id must be a schema-version-1 managed region");
        }
        actual = Objects.requireNonNull(actual, "actual");
        actual.ifPresent(snapshot -> {
            if (!snapshot.id().equals(id)) {
                throw new IllegalArgumentException(
                        "actual snapshot id must equal comparison id");
            }
        });
        declaredNormalizedSha256 = requireSha256(
                declaredNormalizedSha256, "declaredNormalizedSha256");
        generatedNormalizedSha256 = Objects.requireNonNull(
                generatedNormalizedSha256, "generatedNormalizedSha256")
                .map(value -> requireSha256(value, "generatedNormalizedSha256"));
    }

    public boolean actualMatchesDeclared() {
        return actual.map(DartManagedRegionSnapshot::normalizedSha256)
                .map(declaredNormalizedSha256::equals)
                .orElse(false);
    }

    public boolean declaredMatchesGenerated() {
        return generatedNormalizedSha256
                .map(declaredNormalizedSha256::equals)
                .orElse(false);
    }

    public boolean allNormalizedHashesMatch() {
        return actualMatchesDeclared() && declaredMatchesGenerated();
    }

    private static String requireSha256(String value, String name) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be uppercase SHA-256");
        }
        return value;
    }
}
