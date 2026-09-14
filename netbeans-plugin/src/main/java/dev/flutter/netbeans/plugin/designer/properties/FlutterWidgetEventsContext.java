package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StateBinding;
import dev.flutter.netbeans.designer.model.StatePropertyBinding;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Revision-bound event operations for one selected widget. Implementations
 * perform analyzer admission and paired source/model staging before their
 * returned stages complete; opening an editor never mutates a document.
 */
public interface FlutterWidgetEventsContext {
    /** A source method candidate; Dart assignability is checked when it is bound. */
    record Handler(String name, String expression, String signature) {
        public Handler {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(expression, "expression");
            Objects.requireNonNull(signature, "signature");
        }

        @Override
        public String toString() {
            return name + " — " + signature;
        }
    }

    CompletionStage<List<Handler>> discover(PropertyName event);

    CompletionStage<Void> create(PropertyName event, String handlerName);

    CompletionStage<Void> bind(PropertyName event, Handler handler);

    /** Preserves the full typed reference editor, including imported/qualified bindings. */
    CompletionStage<Void> editBinding(PropertyName event, Optional<PropertyValue> value);

    CompletionStage<Void> navigate(PropertyName event);

    CompletionStage<Void> rename(PropertyName event, String newName);

    /** Disconnects the event while preserving the user's handler body. */
    CompletionStage<Void> disconnect(PropertyName event);

    /** A scoped MenuAnchor builder template, not a native Event or arbitrary source edit. */
    default CompletionStage<Void> createMenuAnchorBuilder(String methodName) {
        return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException(
                "Create Menu Builder is not available in this editor context."));
    }

    /** Opens the current direct user-owned MenuAnchor builder without changing or saving it. */
    default CompletionStage<Void> navigateToMenuAnchorBuilder() {
        return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException(
                "Go to Menu Builder is not available in this editor context."));
    }

    /** Creates a new private field and updating handler as one analyzed source/model operation. */
    default CompletionStage<Void> createStateBinding(String fieldName, String handlerName) {
        return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException(stateBindingUnavailableReason()));
    }

    /** A closed state operation; initial text is data, never a Dart expression. */
    default CompletionStage<Void> createStateBinding(String fieldName, String handlerName,
            StateBinding.Action action, Optional<PropertyValue> selectedValue,
            String initialText, Optional<StatePropertyBinding> reusedField) {
        if (action == StateBinding.Action.CHANGE && selectedValue.isEmpty()
                && initialText.isEmpty() && reusedField.isEmpty()) {
            return createStateBinding(fieldName, handlerName);
        }
        return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException(stateBindingUnavailableReason()));
    }

    /** Current direct initialized private State fields; discovery never changes source. */
    default CompletionStage<List<StatePropertyBinding>> discoverStateFields() {
        return java.util.concurrent.CompletableFuture.completedFuture(List.of());
    }

    /** Renames a field already bound by this widget throughout the form, without saving or navigating. */
    default CompletionStage<Void> renameStateField(String fieldName, String newName) {
        return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException(
                "Rename State Field is not available in this editor context."));
    }

    /** Read-only navigation to the verified declaration of an already-bound field. */
    default CompletionStage<Void> navigateToStateField(String fieldName) {
        return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException(
                "Go to State Field is not available in this editor context."));
    }

    default CompletionStage<Void> bindPropertyToState(PropertyName property, StatePropertyBinding binding) {
        return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException(propertyStateBindingUnavailableReason(property)));
    }

    default CompletionStage<Void> removePropertyStateBinding(PropertyName property) {
        return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException(propertyStateBindingUnavailableReason(property)));
    }

    default String propertyStateBindingUnavailableReason(PropertyName property) {
        return "Property State binding actions are not available in this editor context.";
    }

    /** Removes only the typed relationship, retaining user-owned State members. */
    default CompletionStage<Void> removeStateBinding() {
        return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException(stateBindingUnavailableReason()));
    }

    default String stateBindingUnavailableReason() {
        return "State binding actions are not available in this editor context.";
    }

    /** Concrete admission reason, or an empty string when operations are ready. */
    default String unavailableReason() {
        return "";
    }
}
