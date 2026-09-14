package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.StateBinding;
import dev.flutter.netbeans.designer.model.StatePropertyBinding;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.util.Objects;
import java.util.Optional;

/** Creates a typed State field, update handler and widget binding as one analyzed command. */
public record CreateStateBinding(StableId widgetId, String fieldName, String handlerName,
        StateBinding.Action action, Optional<PropertyValue> selectedValue, String initialText,
        Optional<StatePropertyBinding> reusedField)
        implements DesignerCommand {
    public CreateStateBinding {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(selectedValue, "selectedValue");
        Objects.requireNonNull(initialText, "initialText");
        Objects.requireNonNull(reusedField, "reusedField");
        if (initialText.length() > 262144) throw new IllegalArgumentException("Initial controller text exceeds 262144 characters.");
        var names = new StateBinding(fieldName, handlerName, StateBinding.Type.BOOL,
                Optional.empty(), Optional.empty());
        fieldName = names.fieldName();
        handlerName = names.handlerName();
        if (reusedField.isPresent() && (!reusedField.orElseThrow().fieldName().equals(fieldName)
                || reusedField.orElseThrow().transform() != StatePropertyBinding.Transform.DIRECT)) {
            throw new IllegalArgumentException("Reuse requires the exact direct State field descriptor.");
        }
    }

    public CreateStateBinding(StableId widgetId, String fieldName, String handlerName) {
        this(widgetId, fieldName, handlerName, StateBinding.Action.CHANGE, Optional.empty(), "", Optional.empty());
    }

    @Override public DesignerCommandKind kind() { return DesignerCommandKind.CREATE_STATE_BINDING; }
}
