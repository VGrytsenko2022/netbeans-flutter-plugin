package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Atomically sets and resets an ordered group of properties on one widget.
 *
 * <p>Every declaration and set value is checked before a candidate document is
 * built. Cross-property validation therefore observes only the complete patch,
 * which permits safe transitions such as {@code color -> decoration} without
 * exposing an invalid intermediate revision.</p>
 */
public record PatchProperties(
        StableId widgetId,
        List<Patch> patches) implements DesignerCommand {

    public PatchProperties {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(patches, "patches");
        patches = List.copyOf(patches);
        if (patches.isEmpty()) {
            throw new IllegalArgumentException("patches must not be empty");
        }
        if (patches.size() > 512) {
            throw new IllegalArgumentException(
                    "patches cannot exceed 512");
        }
        HashSet<PropertyName> names = new HashSet<>();
        for (Patch patch : patches) {
            Objects.requireNonNull(patch, "patch");
            if (!names.add(patch.propertyName())) {
                throw new IllegalArgumentException(
                        "Each property may be patched at most once: "
                        + patch.propertyName().value());
            }
        }
    }

    @Override
    public DesignerCommandKind kind() {
        return DesignerCommandKind.PATCH_PROPERTIES;
    }

    /** One closed, immutable property mutation. */
    public sealed interface Patch permits SetPatch, ResetPatch {
        PropertyName propertyName();
    }

    /** Sets one property to a typed value. */
    public record SetPatch(
            PropertyName propertyName,
            PropertyValue value) implements Patch {
        public SetPatch {
            Objects.requireNonNull(propertyName, "propertyName");
            Objects.requireNonNull(value, "value");
        }
    }

    /** Removes one optional property. */
    public record ResetPatch(PropertyName propertyName) implements Patch {
        public ResetPatch {
            Objects.requireNonNull(propertyName, "propertyName");
        }
    }
}
