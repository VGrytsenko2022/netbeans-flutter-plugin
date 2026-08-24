package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.api.FlutterDevice;
import dev.flutter.netbeans.run.FlutterTargetKind;
import java.util.Objects;
import org.netbeans.spi.project.ProjectConfiguration;

/** A connected Flutter device exposed through NetBeans' native configuration selector. */
final class FlutterTargetConfiguration implements ProjectConfiguration {
    private final FlutterDevice device;

    FlutterTargetConfiguration(FlutterDevice device) {
        this.device = Objects.requireNonNull(device, "device");
        if (device.id() == null || device.id().isBlank()) {
            throw new IllegalArgumentException("Flutter target id must not be blank");
        }
    }

    FlutterDevice device() {
        return device;
    }

    String id() {
        return device.id();
    }

    @Override
    public String getDisplayName() {
        return "[" + FlutterTargetKind.from(device).label() + "] " + device.name();
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof FlutterTargetConfiguration other
                && id().equals(other.id());
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
