package io.github.vgrytsenko2022.project;

import io.github.vgrytsenko2022.api.FlutterDevice;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Resolves configured project platforms and maps Flutter devices to them. */
public final class FlutterProjectPlatformResolver {
    private FlutterProjectPlatformResolver() {
    }

    /**
     * Returns platforms that have a real generated platform directory in the
     * project. A similarly named file or symbolic link is not accepted as a
     * configured Flutter platform.
     */
    public static Set<FlutterProjectPlatform> configuredPlatforms(Path projectRoot) {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        EnumSet<FlutterProjectPlatform> configured =
                EnumSet.noneOf(FlutterProjectPlatform.class);
        for (FlutterProjectPlatform platform : FlutterProjectPlatform.values()) {
            if (Files.isDirectory(
                    root.resolve(platform.id()),
                    LinkOption.NOFOLLOW_LINKS)) {
                configured.add(platform);
            }
        }
        return Collections.unmodifiableSet(configured);
    }

    /** Maps one Flutter device target to the project platform it requires. */
    public static Optional<FlutterProjectPlatform> platformFor(FlutterDevice device) {
        Objects.requireNonNull(device, "device");
        String platform = normalize(device.platform());
        String id = normalize(device.id());

        if (platform.startsWith("web-") || platform.equals("web")) {
            return Optional.of(FlutterProjectPlatform.WEB);
        }
        if (platform.startsWith("android")) {
            return Optional.of(FlutterProjectPlatform.ANDROID);
        }
        if (platform.startsWith("ios")) {
            return Optional.of(FlutterProjectPlatform.IOS);
        }
        if (platform.startsWith("windows")) {
            return Optional.of(FlutterProjectPlatform.WINDOWS);
        }
        if (platform.startsWith("linux")) {
            return Optional.of(FlutterProjectPlatform.LINUX);
        }
        if (platform.startsWith("darwin") || platform.startsWith("macos")) {
            return Optional.of(FlutterProjectPlatform.MACOS);
        }
        if (platform.isBlank() || platform.equals("unknown")) {
            return switch (id) {
                case "chrome", "edge", "web-server" ->
                    Optional.of(FlutterProjectPlatform.WEB);
                case "windows" -> Optional.of(FlutterProjectPlatform.WINDOWS);
                case "linux" -> Optional.of(FlutterProjectPlatform.LINUX);
                case "macos" -> Optional.of(FlutterProjectPlatform.MACOS);
                default -> Optional.empty();
            };
        }
        return Optional.empty();
    }

    /** Returns whether the device's required platform exists in this project. */
    public static boolean supports(Path projectRoot, FlutterDevice device) {
        Set<FlutterProjectPlatform> configured = configuredPlatforms(projectRoot);
        return platformFor(device).filter(configured::contains).isPresent();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
