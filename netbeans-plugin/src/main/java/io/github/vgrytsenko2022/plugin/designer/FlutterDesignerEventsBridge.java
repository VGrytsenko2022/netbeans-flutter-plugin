package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.catalog.PropertyDefinition;
import io.github.vgrytsenko2022.designer.catalog.FocusWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetDefinition;
import io.github.vgrytsenko2022.designer.command.CreateEventHandler;
import io.github.vgrytsenko2022.designer.command.CreateMenuAnchorBuilder;
import io.github.vgrytsenko2022.designer.command.CreateStateBinding;
import io.github.vgrytsenko2022.designer.command.RemoveStateBinding;
import io.github.vgrytsenko2022.designer.command.BindPropertyToState;
import io.github.vgrytsenko2022.designer.command.RemovePropertyStateBinding;
import io.github.vgrytsenko2022.designer.command.DesignerCommand;
import io.github.vgrytsenko2022.designer.command.RenameEventHandler;
import io.github.vgrytsenko2022.designer.command.RenameStateField;
import io.github.vgrytsenko2022.designer.command.ResetProperty;
import io.github.vgrytsenko2022.designer.command.SetProperty;
import io.github.vgrytsenko2022.designer.events.DartEventHandlerSource;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.events.WidgetEventDescriptor;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.StateBinding;
import io.github.vgrytsenko2022.designer.model.StatePropertyBinding;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.state.WidgetStateBindingCatalog;
import io.github.vgrytsenko2022.designer.state.WidgetStatePropertyBindingCatalog;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityScanner;
import io.github.vgrytsenko2022.plugin.designer.properties.FlutterWidgetEventsContext;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/** Revision-bound Events operations; every mutation uses the ordinary analyzer admission. */
final class FlutterDesignerEventsBridge implements FlutterWidgetEventsContext {
    interface Operations {
        String unavailableReason();
        CompletionStage<byte[]> sourceBytes();
        CompletionStage<Void> submit(DesignerCommand command, String target);
        CompletionStage<Void> navigate(byte[] expectedSource, int utf16Offset);
    }

    private final WidgetNode widget;
    private final WidgetDefinition definition;
    private final DartSourceDescriptor sourceDescriptor;
    private final Operations operations;
    private final List<StatePropertyBinding> knownStateFields;

    FlutterDesignerEventsBridge(WidgetNode widget, WidgetDefinition definition,
            DartSourceDescriptor sourceDescriptor, Operations operations) {
        this(widget, definition, sourceDescriptor, widget, operations);
    }

    FlutterDesignerEventsBridge(WidgetNode widget, WidgetDefinition definition,
            DartSourceDescriptor sourceDescriptor, WidgetNode revisionRoot, Operations operations) {
        this.widget = Objects.requireNonNull(widget, "widget");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.sourceDescriptor = Objects.requireNonNull(sourceDescriptor, "sourceDescriptor");
        this.operations = Objects.requireNonNull(operations, "operations");
        this.knownStateFields = knownStateFields(Objects.requireNonNull(revisionRoot, "revisionRoot"));
        if (!widget.type().equals(definition.typeId())) {
            throw new IllegalArgumentException("Event widget and catalog type differ.");
        }
    }

    @Override
    public String unavailableReason() {
        return operations.unavailableReason();
    }

    @Override
    public String stateBindingUnavailableReason() {
        String reason = unavailableReason();
        if (!reason.isEmpty()) return reason;
        if (sourceDescriptor.widgetKind() != WidgetClassKind.STATEFUL) {
            return "State binding requires a Stateful form. This Stateless form will not be converted automatically.";
        }
        if (!WidgetStateBindingCatalog.supports(definition)) {
            return "State binding is not supported for " + widget.type().value() + ".";
        }
        if (WidgetStateBindingCatalog.find(widget).isEmpty()) {
            return widget.type().value().equals("flutter.material.IconButton")
                    ? "Set Is Selected to true or false before creating an IconButton toggle State binding."
                    : "State binding is not available for this widget's current property configuration.";
        }
        return "";
    }

    @Override
    public CompletionStage<Void> createStateBinding(String fieldName, String handlerName) {
        return createStateBinding(fieldName, handlerName,
                widget.type().value().equals("flutter.material.IconButton")
                        || widget.type().value().equals("flutter.material.ListTile")
                        ? StateBinding.Action.TOGGLE : StateBinding.Action.CHANGE,
                Optional.empty(), "", Optional.empty());
    }

    @Override
    public CompletionStage<Void> createStateBinding(String fieldName, String handlerName,
            StateBinding.Action action, Optional<PropertyValue> selectedValue,
            String initialText, Optional<StatePropertyBinding> reusedField) {
        return attempt(() -> {
            requireStateBindingAvailable();
            WidgetStateBindingCatalog.createBinding(widget, fieldName, handlerName, action, selectedValue, reusedField);
            CreateStateBinding command = new CreateStateBinding(widget.id(), fieldName, handlerName,
                    action, selectedValue, initialText, reusedField);
            if (reusedField.isEmpty()) return operations.submit(command, stateTarget() + ", create State binding");
            return discoverStateFields().thenCompose(fields -> {
                requireCurrent();
                if (!fields.contains(reusedField.orElseThrow())) {
                    throw new IllegalArgumentException("Cannot reuse State field: the selected field is not a current direct initialized State field.");
                }
                return operations.submit(command, stateTarget() + ", reuse State field");
            });
        });
    }

    @Override
    public CompletionStage<List<StatePropertyBinding>> discoverStateFields() {
        return attempt(() -> {
            requireStateful();
            return operations.sourceBytes().thenApply(source -> {
                requireCurrent();
                return stateFields(source, verifiedOwner(source));
            });
        });
    }

    private List<StatePropertyBinding> stateFields(byte[] source, String owner) {
        var fields = new java.util.LinkedHashMap<String, StatePropertyBinding>();
        DartEventHandlerSource.discoverStateFields(source, owner).forEach(field -> fields.put(field.fieldName(), field));
        // Custom type identity comes only from admitted model metadata,
        // never a guessed import or a name parsed from arbitrary text.
        for (StatePropertyBinding hint : knownStateFields) {
            if (fields.containsKey(hint.fieldName())) continue;
            try {
                DartEventHandlerSource.requirePropertyBindingFields(source, owner, List.of(hint));
                fields.put(hint.fieldName(), hint);
            } catch (IllegalArgumentException unavailable) {
                // Stale/missing/non-owned hinted fields are not selectable.
            }
        }
        return List.copyOf(fields.values());
    }

    @Override
    public CompletionStage<Void> renameStateField(String fieldName, String newName) {
        return attempt(() -> {
            requireStateful();
            StatePropertyBinding bound = boundStateField(fieldName);
            var command = new RenameStateField(widget.id(), fieldName, newName);
            return operations.sourceBytes().thenCompose(source -> {
                requireCurrent();
                requireCurrentBoundField(source, verifiedOwner(source), bound);
                return operations.submit(command, widget.type().value() + " " + widget.id()
                        + ", rename State field " + fieldName + " to " + newName + " throughout the form");
            });
        });
    }

    @Override
    public CompletionStage<Void> navigateToStateField(String fieldName) {
        return attempt(() -> {
            requireStateful();
            StatePropertyBinding bound = boundStateField(fieldName);
            return operations.sourceBytes().thenCompose(source -> {
                requireCurrent();
                String owner = verifiedOwner(source);
                requireCurrentBoundField(source, owner, bound);
                int offset = DartEventHandlerSource.stateFieldNameOffset(source, owner, bound);
                return operations.navigate(source, offset);
            });
        });
    }

    private StatePropertyBinding boundStateField(String name) {
        Objects.requireNonNull(name, "fieldName");
        boolean bound = widget.stateBinding().filter(value -> value.fieldName().equals(name)).isPresent()
                || widget.propertyBindings().values().stream().anyMatch(value -> value.fieldName().equals(name));
        if (!bound) throw new IllegalArgumentException("Cannot manage State field " + name + ": widget " + widget.id()
                + " has no current binding to this field. Bind the field before renaming or navigating from this editor.");
        return knownStateFields.stream().filter(field -> field.fieldName().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The bound State field metadata is unavailable for " + name + "."));
    }

    private void requireCurrentBoundField(byte[] source, String owner, StatePropertyBinding bound) {
        if (!stateFields(source, owner).contains(bound)) {
            throw new IllegalArgumentException("Cannot manage State field " + bound.fieldName()
                    + ": its current direct declaration no longer matches the selected widget's binding.");
        }
    }

    private static List<StatePropertyBinding> knownStateFields(WidgetNode root) {
        var fields = new java.util.LinkedHashMap<String, StatePropertyBinding>();
        var pending = new java.util.ArrayDeque<WidgetNode>();
        pending.add(root);
        while (!pending.isEmpty()) {
            WidgetNode node = pending.removeFirst();
            node.stateBinding().ifPresent(binding -> retainFieldHint(fields, new StatePropertyBinding(
                    binding.fieldName(), binding.type(), binding.referenceType(), StatePropertyBinding.Transform.DIRECT, Optional.empty())));
            node.propertyBindings().values().forEach(binding -> retainFieldHint(fields, new StatePropertyBinding(
                    binding.fieldName(), binding.type(), binding.referenceType(), StatePropertyBinding.Transform.DIRECT, Optional.empty())));
            for (WidgetSlot slot : node.slots().values()) {
                if (slot instanceof WidgetSlot.SingleSlot single) single.child().ifPresent(pending::addLast);
                else if (slot instanceof WidgetSlot.ListSlot list) pending.addAll(list.children());
            }
        }
        return List.copyOf(fields.values());
    }

    private static void retainFieldHint(java.util.Map<String, StatePropertyBinding> fields, StatePropertyBinding hint) {
        StatePropertyBinding previous = fields.putIfAbsent(hint.fieldName(), hint);
        if (previous != null && !previous.equals(hint)) {
            throw new IllegalArgumentException("The selected Designer revision contains conflicting State field types for " + hint.fieldName() + ".");
        }
    }

    @Override
    public String propertyStateBindingUnavailableReason(PropertyName property) {
        String reason = unavailableReason();
        if (!reason.isEmpty()) return reason;
        if (sourceDescriptor.widgetKind() != WidgetClassKind.STATEFUL) {
            return "State binding requires a Stateful form. This Stateless form will not be converted automatically.";
        }
        return WidgetStatePropertyBindingCatalog.find(widget, property).isPresent() ? ""
                : "State binding is not supported for property " + property.value() + " on " + widget.type().value() + ".";
    }

    @Override
    public CompletionStage<Void> bindPropertyToState(PropertyName property, StatePropertyBinding binding) {
        return attempt(() -> {
            String reason = propertyStateBindingUnavailableReason(property);
            if (!reason.isEmpty()) throw new IllegalStateException(reason);
            return discoverStateFields().thenCompose(fields -> {
                requireCurrent();
                boolean currentField = fields.stream().anyMatch(field -> field.fieldName().equals(binding.fieldName())
                        && field.type() == binding.type() && field.referenceType().equals(binding.referenceType()));
                if (!currentField) throw new IllegalArgumentException("Cannot bind property " + property.value()
                        + ": field " + binding.fieldName() + " is not a current direct initialized State field.");
                return operations.submit(new BindPropertyToState(widget.id(), property, binding),
                        widget.type().value() + " " + widget.id() + ", property " + property.value() + ", bind State field " + binding.fieldName());
            });
        });
    }

    @Override
    public CompletionStage<Void> removePropertyStateBinding(PropertyName property) {
        return attempt(() -> {
            String reason = propertyStateBindingUnavailableReason(property);
            if (!reason.isEmpty()) throw new IllegalStateException(reason);
            if (!widget.propertyBindings().containsKey(property)) {
                throw new IllegalArgumentException("Cannot remove State binding: property " + property.value() + " has no State binding.");
            }
            return operations.submit(new RemovePropertyStateBinding(widget.id(), property),
                    widget.type().value() + " " + widget.id() + ", property " + property.value() + ", remove State binding");
        });
    }

    private void requireStateful() {
        if (sourceDescriptor.widgetKind() != WidgetClassKind.STATEFUL) {
            throw new IllegalStateException("State binding requires a Stateful form. This Stateless form will not be converted automatically.");
        }
    }

    private String stateTarget() {
        return target(WidgetStateBindingCatalog.find(widget).orElseThrow().eventProperty());
    }

    @Override
    public CompletionStage<Void> removeStateBinding() {
        return attempt(() -> {
            requireStateBindingAvailable();
            if (widget.stateBinding().isEmpty()) {
                throw new IllegalArgumentException("Cannot remove State binding: this widget has no State binding.");
            }
            return operations.submit(new RemoveStateBinding(widget.id()),
                    stateTarget() + ", remove State binding");
        });
    }

    private void requireStateBindingAvailable() {
        String reason = stateBindingUnavailableReason();
        if (!reason.isEmpty()) throw new IllegalStateException(reason);
    }

    @Override
    public CompletionStage<List<Handler>> discover(PropertyName event) {
        return attempt(() -> {
            descriptor(event);
            return operations.sourceBytes().thenApply(source -> {
                requireCurrent();
                return candidates(source, verifiedOwner(source));
            });
        });
    }

    /** Lexical candidates only. Dart assignability is established by candidate analysis on bind. */
    static List<Handler> candidates(byte[] source, String ownerClass) {
        return DartEventHandlerSource.methods(source, ownerClass).stream()
                .filter(method -> !method.generated() && method.hasBody())
                .map(method -> new Handler(method.name(), method.name(), method.signature()))
                .toList();
    }

    @Override
    public CompletionStage<Void> create(PropertyName event, String handlerName) {
        return attempt(() -> {
            requireBindingAvailable(descriptor(event));
            return operations.submit(new CreateEventHandler(widget.id(), event, handlerName), target(event));
        });
    }

    @Override
    public CompletionStage<Void> createMenuAnchorBuilder(String methodName) {
        return attempt(() -> {
            requireMenuAnchor();
            PropertyValue existing = widget.properties().get(new PropertyName("builder"));
            if (existing != null && !(existing instanceof PropertyValue.NullValue)) {
                throw new IllegalArgumentException("Cannot create Menu Builder for " + widget.id()
                        + ": a builder is already bound. Reset Builder first; its user-owned method will be retained.");
            }
            return operations.submit(new CreateMenuAnchorBuilder(widget.id(), methodName),
                    widget.type().value() + " " + widget.id() + ", create Menu Builder " + methodName);
        });
    }

    @Override
    public CompletionStage<Void> navigateToMenuAnchorBuilder() {
        return attempt(() -> {
            requireMenuAnchor();
            String method = localHandler(widget.properties().get(new PropertyName("builder")), "MenuAnchor Builder");
            return operations.sourceBytes().thenCompose(source -> {
                requireMenuAnchor();
                return operations.navigate(source, handlerOffset(source, verifiedOwner(source), method));
            });
        });
    }

    private void requireMenuAnchor() {
        requireCurrent();
        if (!widget.type().value().equals("flutter.material.MenuAnchor")) {
            throw new IllegalArgumentException("Menu Builder actions require a selected MenuAnchor, not " + widget.type().value() + ".");
        }
    }

    @Override
    public CompletionStage<Void> bind(PropertyName event, Handler handler) {
        return attempt(() -> {
            Objects.requireNonNull(handler, "handler");
            WidgetEventDescriptor descriptor = descriptor(event);
            requireBindingAvailable(descriptor);
            PropertyValue value = descriptor.bindingValue(handler.expression(), property(event));
            return operations.sourceBytes().thenCompose(source -> {
                requireCurrent();
                String ownerClass = verifiedOwner(source);
                if (!candidates(source, ownerClass).contains(handler)) {
                    throw new IllegalArgumentException("Cannot bind " + target(event)
                            + ": the selected handler is not a current user-owned method of " + ownerClass + ".");
                }
                if (value.equals(widget.properties().get(event))) return CompletableFuture.completedFuture(null);
                return operations.submit(new SetProperty(widget.id(), event, value), target(event));
            });
        });
    }

    @Override
    public CompletionStage<Void> navigate(PropertyName event) {
        return attempt(() -> {
            descriptor(event);
            String handler = localHandler(widget.properties().get(event), target(event));
            return operations.sourceBytes().thenCompose(source -> {
                requireCurrent();
                return operations.navigate(source, handlerOffset(source, verifiedOwner(source), handler));
            });
        });
    }

    @Override
    public CompletionStage<Void> editBinding(PropertyName event, Optional<PropertyValue> value) {
        return attempt(() -> {
            WidgetEventDescriptor descriptor = descriptor(event);
            Objects.requireNonNull(value, "value");
            if (value.isEmpty()) return disconnect(event);
            PropertyValue accepted = value.orElseThrow();
            if (property(event).constraints().stream().noneMatch(constraint -> constraint.accepts(accepted))) {
                throw new IllegalArgumentException("Cannot edit " + target(event) + ": the value violates its callback type.");
            }
            if (!(accepted instanceof PropertyValue.NullValue)
                    && !(accepted instanceof PropertyValue.StringValue)) requireBindingAvailable(descriptor);
            return accepted.equals(widget.properties().get(event)) ? CompletableFuture.completedFuture(null)
                    : operations.submit(new SetProperty(widget.id(), event, accepted), target(event));
        });
    }

    @Override
    public CompletionStage<Void> rename(PropertyName event, String newName) {
        return attempt(() -> {
            descriptor(event);
            localHandler(widget.properties().get(event), target(event));
            return operations.submit(new RenameEventHandler(widget.id(), event, newName), target(event));
        });
    }

    @Override
    public CompletionStage<Void> disconnect(PropertyName event) {
        return attempt(() -> {
            WidgetEventDescriptor descriptor = descriptor(event);
            PropertyValue current = widget.properties().get(event);
            if (!descriptor.required()) {
                return current == null ? CompletableFuture.completedFuture(null)
                        : operations.submit(new ResetProperty(widget.id(), event), target(event));
            }
            PropertyValue replacement = descriptor.creationDefault().orElseGet(() -> {
                if (descriptor.allowsExplicitNull()) return new PropertyValue.NullValue();
                throw new IllegalArgumentException("Cannot disconnect " + target(event)
                        + ": this required event has no reviewed disconnected value.");
            });
            return replacement.equals(current) ? CompletableFuture.completedFuture(null)
                    : operations.submit(new SetProperty(widget.id(), event, replacement), target(event));
        });
    }

    static int handlerOffset(byte[] source, String ownerClass, String handler) {
        return DartEventHandlerSource.methods(source, ownerClass).stream()
                .filter(method -> method.name().equals(handler) && !method.generated() && method.hasBody())
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Cannot navigate to handler " + handler + ": no user-owned method exists in " + ownerClass + "."))
                .bodyStart();
    }

    private String verifiedOwner(byte[] source) {
        var integrity = new DartSourceIntegrityScanner().scan(source, sourceDescriptor);
        return integrity.verifiedMemberClassName().orElseThrow(() -> new IllegalArgumentException(
                "Cannot inspect event handlers: the Designer build-member owner is not verified in the current Dart source."));
    }

    private static String localHandler(PropertyValue value, String target) {
        if (value instanceof PropertyValue.CallbackValue callback && !callback.handler().contains(".")) {
            return callback.handler();
        }
        if (value instanceof PropertyValue.DartObjectReferenceValue reference
                && reference.libraryUri().isEmpty() && reference.member().isEmpty()
                && reference.access() == PropertyValue.DartObjectReferenceValue.Access.REFERENCE) {
            return reference.rootSymbol();
        }
        throw new IllegalArgumentException("Cannot navigate or rename " + target
                + ": the binding is not a direct method of this form. Use Dart source navigation for external references.");
    }

    private void requireBindingAvailable(WidgetEventDescriptor event) {
        if (!event.availableVariants().isEmpty()) {
            PropertyValue variant = widget.properties().get(new PropertyName("variant"));
            String selected = FocusWidgetPropertySchema.FOCUS_TYPE.equals(widget.type())
                    ? FocusWidgetPropertySchema.variant(widget) : variant instanceof PropertyValue.StringValue string ? string.value() : "";
            if (!event.availableVariants().contains(selected)) {
                throw new IllegalArgumentException("Cannot bind " + target(event.propertyName())
                        + ": select constructor " + String.join(" or ", event.availableVariants()) + " first.");
            }
        }
        for (PropertyName companion : event.requiredCompanionProperties()) {
            PropertyValue value = widget.properties().get(companion);
            if (value == null || value instanceof PropertyValue.NullValue) {
                throw new IllegalArgumentException("Cannot bind " + target(event.propertyName())
                        + ": set " + companion.value() + " first; its error callback requires that image provider.");
            }
        }
    }

    private WidgetEventDescriptor descriptor(PropertyName event) {
        Objects.requireNonNull(event, "event");
        return WidgetEventCatalog.eventsFor(definition).stream()
                .filter(value -> value.supportsHandlerActions()
                        && value.propertyName().equals(event)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No admitted event " + target(event) + "."));
    }

    private PropertyDefinition property(PropertyName event) {
        return definition.property(event).orElseThrow();
    }

    private String target(PropertyName event) {
        return widget.type().value() + " " + widget.id() + ", event " + event.value();
    }

    private void requireCurrent() {
        String reason = unavailableReason();
        if (!reason.isEmpty()) throw new IllegalStateException(reason);
    }

    private <T> CompletionStage<T> attempt(Supplier<CompletionStage<T>> operation) {
        try {
            requireCurrent();
            return operation.get();
        } catch (RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }
}
