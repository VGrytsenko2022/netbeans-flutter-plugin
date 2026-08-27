package dev.flutter.netbeans.designer.move;

import java.util.Objects;

/** Closed result of the project-wide Dart dependency gate for pair Move. */
public sealed interface DesignerPairMoveDependencyResult permits
        DesignerPairMoveDependencyResult.Safe,
        DesignerPairMoveDependencyResult.Rejected {

    record Safe(DesignerPairMoveDependencyPlan plan)
            implements DesignerPairMoveDependencyResult {
        public Safe {
            Objects.requireNonNull(plan, "plan");
        }
    }

    record Rejected(DartMoveDependencyDiagnostic diagnostic)
            implements DesignerPairMoveDependencyResult {
        public Rejected {
            Objects.requireNonNull(diagnostic, "diagnostic");
        }
    }
}
