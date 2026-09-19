package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.StateBinding;
import io.github.vgrytsenko2022.designer.model.StatePropertyBinding;
import java.util.Objects;
import java.util.Optional;

/** Renames one verified State field and every retained binding in a single source/model revision. */
public record RenameStateField(StableId widgetId, String fieldName, String newName) implements DesignerCommand {
    public RenameStateField {
        Objects.requireNonNull(widgetId, "widgetId");
        fieldName = privateName(fieldName);
        newName = privateName(newName);
    }

    private static String privateName(String name) {
        return new StatePropertyBinding(name, StateBinding.Type.BOOL, Optional.empty(),
                StatePropertyBinding.Transform.DIRECT).fieldName();
    }

    @Override public DesignerCommandKind kind() { return DesignerCommandKind.RENAME_STATE_FIELD; }
}
