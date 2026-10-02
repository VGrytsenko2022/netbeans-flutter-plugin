package io.github.vgrytsenko2022.project;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** One canonical platform accepted by {@code flutter create --platforms}. */
public enum FlutterProjectPlatform {
    ANDROID("android", "Android"),
    IOS("ios", "iOS"),
    WEB("web", "Web"),
    WINDOWS("windows", "Windows"),
    MACOS("macos", "macOS"),
    LINUX("linux", "Linux");

    private final String id;
    private final String displayName;

    FlutterProjectPlatform(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Returns every supported project platform in canonical CLI/UI order. */
    public static Set<FlutterProjectPlatform> all() {
        return Collections.unmodifiableSet(EnumSet.allOf(FlutterProjectPlatform.class));
    }

    /** Returns an immutable defensive copy and rejects an empty selection. */
    public static Set<FlutterProjectPlatform> copyOf(
            Collection<FlutterProjectPlatform> platforms) {
        Objects.requireNonNull(platforms, "platforms");
        if (platforms.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one Flutter project platform must be selected.");
        }
        EnumSet<FlutterProjectPlatform> copy = EnumSet.noneOf(FlutterProjectPlatform.class);
        for (FlutterProjectPlatform platform : platforms) {
            copy.add(Objects.requireNonNull(platform, "platform"));
        }
        return Collections.unmodifiableSet(copy);
    }

    /** Returns the selected platforms in the enum's stable canonical order. */
    public static List<FlutterProjectPlatform> ordered(
            Collection<FlutterProjectPlatform> platforms) {
        Set<FlutterProjectPlatform> selected = copyOf(platforms);
        return Stream.of(values()).filter(selected::contains).toList();
    }

    /** Formats a deterministic comma-separated value for Flutter's CLI. */
    public static String cliArgument(Collection<FlutterProjectPlatform> platforms) {
        return ordered(platforms).stream()
                .map(FlutterProjectPlatform::id)
                .collect(Collectors.joining(","));
    }
}
