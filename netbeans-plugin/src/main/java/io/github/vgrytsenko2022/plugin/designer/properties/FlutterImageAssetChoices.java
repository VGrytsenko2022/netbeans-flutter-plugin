package io.github.vgrytsenko2022.plugin.designer.properties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable declared-asset choices exposed to structured image editors. */
public record FlutterImageAssetChoices(
        List<Choice> choices,
        Optional<String> unavailableReason) {
    static final String FEATURE_ATTRIBUTE =
            "flutter.designer.imageAssetChoices";

    public FlutterImageAssetChoices {
        Objects.requireNonNull(choices, "choices");
        Objects.requireNonNull(unavailableReason, "unavailableReason");
        ArrayList<Choice> ordered = new ArrayList<>(choices);
        ordered.sort(Comparator.comparing(Choice::externalName));
        if (ordered.size() != ordered.stream()
                .map(Choice::externalName).distinct().count()) {
            throw new IllegalArgumentException(
                    "Flutter image asset choices must be unique");
        }
        choices = List.copyOf(ordered);
        unavailableReason = unavailableReason.map(value -> {
            String compact = Objects.requireNonNull(value, "unavailableReason")
                    .strip().replaceAll("\\s+", " ");
            if (compact.isEmpty() || compact.length() > 1024) {
                throw new IllegalArgumentException(
                        "Asset inventory reason must contain 1..1024 characters");
            }
            return compact;
        });
    }

    public static FlutterImageAssetChoices empty() {
        return new FlutterImageAssetChoices(
                List.of(),
                Optional.of(
                        "The owning Flutter project's declared image asset inventory "
                        + "is not available."));
    }

    public Optional<Choice> find(
            Optional<String> packageName,
            String assetName) {
        Objects.requireNonNull(packageName, "packageName");
        Objects.requireNonNull(assetName, "assetName");
        return choices.stream().filter(choice ->
                choice.packageName().equals(packageName)
                && choice.assetName().equals(assetName)).findFirst();
    }

    public record Choice(
            Optional<String> packageName,
            String assetName,
            String displayName) {
        public Choice {
            Objects.requireNonNull(packageName, "packageName");
            Objects.requireNonNull(assetName, "assetName");
            Objects.requireNonNull(displayName, "displayName");
            if (assetName.isBlank() || displayName.isBlank()) {
                throw new IllegalArgumentException(
                        "Asset choice identity and display name must not be blank");
            }
        }

        public String externalName() {
            return packageName
                    .map(value -> "package:" + value + ':' + assetName)
                    .orElseGet(() -> "app:" + assetName);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
