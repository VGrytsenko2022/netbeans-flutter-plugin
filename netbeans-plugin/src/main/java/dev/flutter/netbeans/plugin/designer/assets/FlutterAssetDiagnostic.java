package dev.flutter.netbeans.plugin.designer.assets;

import java.util.Objects;

/** Concrete resolver failure naming the operation, target, and known reason. */
public record FlutterAssetDiagnostic(
        String operation,
        String target,
        String reason)
        implements Comparable<FlutterAssetDiagnostic> {

    public FlutterAssetDiagnostic {
        operation = required(operation, "operation");
        target = required(target, "target");
        reason = required(reason, "reason");
    }

    @Override
    public int compareTo(FlutterAssetDiagnostic other) {
        int operationOrder = operation.compareTo(other.operation);
        if (operationOrder != 0) {
            return operationOrder;
        }
        int targetOrder = target.compareTo(other.target);
        return targetOrder != 0 ? targetOrder : reason.compareTo(other.reason);
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
