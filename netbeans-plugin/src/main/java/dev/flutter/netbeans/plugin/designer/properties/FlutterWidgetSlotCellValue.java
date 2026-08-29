package dev.flutter.netbeans.plugin.designer.properties;

import java.util.Objects;
import java.util.Optional;

/** Transactional current value or one staged named-slot mutation. */
record FlutterWidgetSlotCellValue(
        String summary,
        Optional<FlutterWidgetSlotMutation> mutation) {

    FlutterWidgetSlotCellValue {
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(mutation, "mutation");
        if (summary.isBlank()) {
            throw new IllegalArgumentException("Slot summary must not be blank.");
        }
    }

    static FlutterWidgetSlotCellValue current(String summary) {
        return new FlutterWidgetSlotCellValue(summary, Optional.empty());
    }

    static FlutterWidgetSlotCellValue staged(
            String summary,
            FlutterWidgetSlotMutation mutation) {
        return new FlutterWidgetSlotCellValue(
                summary, Optional.of(Objects.requireNonNull(mutation, "mutation")));
    }

    @Override
    public String toString() {
        return summary;
    }
}
