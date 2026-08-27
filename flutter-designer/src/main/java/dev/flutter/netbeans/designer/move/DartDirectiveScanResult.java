package dev.flutter.netbeans.designer.move;

import java.util.List;
import java.util.Objects;

/** Closed result of scanning one bounded Dart source for directive URIs. */
public sealed interface DartDirectiveScanResult permits
        DartDirectiveScanResult.Parsed,
        DartDirectiveScanResult.Rejected {

    record Parsed(List<DartDirectiveReference> references)
            implements DartDirectiveScanResult {
        public Parsed {
            references = List.copyOf(Objects.requireNonNull(references, "references"));
        }
    }

    record Rejected(DartMoveDependencyDiagnostic diagnostic)
            implements DartDirectiveScanResult {
        public Rejected {
            Objects.requireNonNull(diagnostic, "diagnostic");
        }
    }
}
