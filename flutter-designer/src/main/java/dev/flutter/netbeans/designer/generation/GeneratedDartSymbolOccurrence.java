package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.model.StableId;
import java.util.Objects;
import java.util.Optional;

/**
 * One generator-owned Dart symbol occurrence inside a managed-region payload.
 *
 * <p>Offsets and lengths use Java/Dart UTF-16 code units. The occurrence is
 * deliberately relative to its generated payload: a later candidate mapper
 * must bind it to the exact marker-bearing source revision before analyzer
 * navigation is requested.</p>
 */
public record GeneratedDartSymbolOccurrence(
        String id,
        DartManagedRegionId region,
        int offset,
        int length,
        String symbolName,
        String libraryUri,
        String modelPath,
        Optional<StableId> widgetId) {

    public GeneratedDartSymbolOccurrence {
        id = requireText(id, "id");
        Objects.requireNonNull(region, "region");
        if (offset < 0 || length <= 0) {
            throw new IllegalArgumentException(
                    "symbol occurrence offset must be non-negative and length positive");
        }
        symbolName = requireText(symbolName, "symbolName");
        libraryUri = requireText(libraryUri, "libraryUri");
        modelPath = Objects.requireNonNull(modelPath, "modelPath");
        if (!modelPath.startsWith("/")) {
            throw new IllegalArgumentException("modelPath must be an absolute JSON pointer");
        }
        widgetId = Objects.requireNonNull(widgetId, "widgetId");
        if (length != symbolName.length()) {
            throw new IllegalArgumentException(
                    "symbol occurrence length must equal the UTF-16 symbol-name length");
        }
    }

    public int endOffset() {
        return Math.addExact(offset, length);
    }

    public GeneratedDartSymbolOccurrence shifted(int delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("symbol occurrence shift must not be negative");
        }
        return new GeneratedDartSymbolOccurrence(
                id,
                region,
                Math.addExact(offset, delta),
                length,
                symbolName,
                libraryUri,
                modelPath,
                widgetId);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
