package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.events.WidgetEventDescriptor;
import io.github.vgrytsenko2022.designer.catalog.FocusWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.NotificationListenerWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.StateBinding;
import io.github.vgrytsenko2022.designer.model.StatePropertyBinding;
import io.github.vgrytsenko2022.designer.state.WidgetStateBindingCatalog;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.beans.PropertyEditorSupport;
import java.beans.PropertyEditor;
import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/** The Events row keeps its normal model value; explicit actions own the edit. */
final class FlutterWidgetEventPropertyEditor extends PropertyEditorSupport {
    static final String HANDLERS_NAME = "flutter.event.handlers";
    static final String HANDLER_NAME = "flutter.event.handlerName";
    static final String STATUS_NAME = "flutter.event.status";
    static final String CREATE_NAME = "flutter.event.create";
    static final String BIND_NAME = "flutter.event.bind";
    static final String NAVIGATE_NAME = "flutter.event.navigate";
    static final String RENAME_NAME = "flutter.event.rename";
    static final String DISCONNECT_NAME = "flutter.event.disconnect";
    static final String REFERENCE_APPLY_NAME = "flutter.event.reference.apply";
    static final String SAVE_GUIDANCE_NAME = "flutter.event.saveGuidance";
    static final String RETURN_GUIDANCE_NAME = "flutter.event.returnGuidance";
    static final String NOTIFICATION_GUIDANCE_NAME = "flutter.event.notificationGuidance";
    static final String STATE_FIELD_NAME = "flutter.event.state.fieldName";
    static final String STATE_HANDLER_NAME = "flutter.event.state.handlerName";
    static final String STATE_TYPE_NAME = "flutter.event.state.type";
    static final String STATE_CREATE_NAME = "flutter.event.state.create";
    static final String STATE_REMOVE_NAME = "flutter.event.state.remove";
    static final String STATE_GUIDANCE_NAME = "flutter.event.state.guidance";
    static final String STATE_MODE_NAME = "flutter.event.state.mode";
    static final String STATE_FIELDS_NAME = "flutter.event.state.fields";
    static final String STATE_ACTION_NAME = "flutter.event.state.action";
    static final String STATE_SELECTION_NAME = "flutter.event.state.selection";
    static final String STATE_SELECTION_TYPE_NAME = "flutter.event.state.selectionType";
    static final String STATE_INITIAL_TEXT_NAME = "flutter.event.state.initialText";
    static final String STATE_RENAME_FIELD_NAME = "flutter.event.state.renameFieldName";
    static final String STATE_RENAME_NAME = "flutter.event.state.rename";
    static final String STATE_NAVIGATE_NAME = "flutter.event.state.navigate";

    private final WidgetNode widget;
    private final WidgetEventDescriptor event;
    private final FlutterWidgetEventsContext context;
    private final Supplier<FlutterWidgetEventsContext> currentContext;
    private final Supplier<WidgetNode> currentWidget;
    private final PropertyEditor referenceEditor;
    private boolean operationCompleted;
    private Component panel;

    FlutterWidgetEventPropertyEditor(WidgetNode widget, WidgetEventDescriptor event,
            FlutterWidgetEventsContext context,
            Supplier<FlutterWidgetEventsContext> currentContext,
            Supplier<WidgetNode> currentWidget,
            PropertyEditor referenceEditor) {
        this.widget = Objects.requireNonNull(widget, "widget");
        this.event = Objects.requireNonNull(event, "event");
        this.context = Objects.requireNonNull(context, "context");
        this.currentContext = Objects.requireNonNull(currentContext, "currentContext");
        this.currentWidget = Objects.requireNonNull(currentWidget, "currentWidget");
        this.referenceEditor = Objects.requireNonNull(referenceEditor, "referenceEditor");
        PropertyValue value = widget.properties().get(event.propertyName());
        super.setValue(value == null ? FlutterPropertyCellValue.unset()
                : FlutterPropertyCellValue.explicit(value));
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof FlutterPropertyCellValue)) {
            throw new IllegalArgumentException("An event row requires a Flutter property value.");
        }
        super.setValue(value);
    }

    @Override
    public Object getValue() {
        if (operationCompleted) {
            PropertyValue value = currentWidget.get().properties().get(event.propertyName());
            return value == null ? FlutterPropertyCellValue.unset() : FlutterPropertyCellValue.explicit(value);
        }
        return super.getValue();
    }

    @Override
    public String getAsText() {
        return summary(widget, event.propertyName(),
                ((FlutterPropertyCellValue) getValue()).explicitValue().orElse(null));
    }

    @Override
    public void setAsText(String text) {
        if (!getAsText().equals(text)) {
            throw new IllegalArgumentException("Use the Events editor to create or choose a handler.");
        }
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    @Override
    public Component getCustomEditor() {
        if (panel == null) {
            panel = new EventPanel();
        }
        return panel;
    }

    static String summary(WidgetNode widget, PropertyName property, PropertyValue value) {
        String text;
        boolean disabled = new PropertyValue.BooleanValue(false).equals(
                widget.properties().get(new PropertyName("enabled")));
        if (value instanceof PropertyValue.NullValue) {
            text = "null (explicit)";
        } else if (value instanceof PropertyValue.StringValue string && string.value().equals("noop")) {
            text = "<no-op>";
        } else if (value == null) {
            String type = widget.type().value();
            String name = property.value();
            boolean controlled = List.of("flutter.material.Checkbox", "flutter.material.Switch",
                    "flutter.material.Slider", "flutter.material.RangeSlider").contains(type)
                    && name.equals("onChanged");
            boolean button = List.of("flutter.material.ElevatedButton", "flutter.material.TextButton",
                    "flutter.material.OutlinedButton", "flutter.material.FilledButton",
                    "flutter.material.IconButton", "flutter.material.FloatingActionButton").contains(type)
                    && name.equals("onPressed")
                    && !widget.properties().containsKey(new PropertyName("onLongPress"));
            boolean refresh = type.equals("flutter.material.RefreshIndicator") && name.equals("onRefresh");
            text = (controlled || button) && disabled ? "<none> (emits null)"
                    : controlled || button || refresh ? "<none> (generated no-op)" : "<none>";
        } else {
            text = PropertyValueFormatter.format(value);
        }
        if (disabled) {
            text += " (widget disabled)";
        }
        if (FocusWidgetPropertySchema.FOCUS_TYPE.equals(widget.type()) && !FocusWidgetPropertySchema.propertyAvailable(widget, property)) {
            text += " (inactive; retained)";
        }
        return text;
    }

    private String staleReason() {
        if (currentContext.get() != context) {
            return "Cannot edit " + event.propertyName().value() + " on widget " + widget.id()
                    + ": the document changed. Close and reopen this event editor.";
        }
        return context.unavailableReason();
    }

    private String inactiveBindingReason() {
        WidgetNode current = currentWidget.get();
        return FocusWidgetPropertySchema.FOCUS_TYPE.equals(current.type())
                && !FocusWidgetPropertySchema.propertyAvailable(current, event.propertyName())
                ? "Inactive in Focus.withExternalFocusNode: this stored callback is retained but not emitted; the external FocusNode supplies it. Select Standard to create or bind a handler. Stored handlers may still be opened, renamed or disconnected."
                : "";
    }

    private final class EventPanel extends JPanel {
        private final DefaultListModel<FlutterWidgetEventsContext.Handler> handlers = new DefaultListModel<>();
        private final JList<FlutterWidgetEventsContext.Handler> choices = new JList<>(handlers);
        private final JTextField handlerName = new JTextField(suggestedName(), 28);
        private final JTextArea status = new JTextArea(3, 52);
        private final List<JButton> buttons = new ArrayList<>();
        private final JButton bind;
        private final JButton navigate;
        private final JButton rename;
        private final JButton disconnect;
        private JButton createState;
        private JButton removeState;
        private JButton renameState;
        private JButton navigateState;
        private JTextField renamedStateField;
        private JTextField stateField;
        private JTextField stateHandler;
        private JComboBox<String> stateMode;
        private JComboBox<StatePropertyBinding> stateFields;
        private JComboBox<StateBinding.Action> stateAction;
        private JTextField stateSelection;
        private JComboBox<StateBinding.Type> selectionType;
        private JTextArea initialText;
        private boolean fieldsLoading;
        private List<StatePropertyBinding> discoveredFields = List.of();
        private WidgetStateBindingCatalog.Descriptor stateContract;
        private JLabel stateType;
        private JTextArea stateGuidance;
        private String stateGuidanceBase;
        private boolean busy;
        private final PropertyEnv referenceEnvironment;

        EventPanel() {
            super(new BorderLayout(8, 8));
            setBorder(new EmptyBorder(10, 10, 10, 10));
            setPreferredSize(new Dimension(780, 570));
            JPanel details = new JPanel(new GridLayout(0, 1, 4, 4));
            details.add(new JLabel(event.propertyName().value() + ": " + event.signature()));
            details.add(new JLabel("Current handler: " + getAsText()));
            details.add(new JLabel("Mutation actions apply the change and close this editor."));
            JLabel saveGuidance = new JLabel("Edit the handler in Source, then Save to analyze the current code. Invalid code stays in the editor unsaved.");
            saveGuidance.setName(SAVE_GUIDANCE_NAME);
            details.add(saveGuidance);
            if (!List.of("void", "Future<void>").contains(event.signature().returnType())) {
                JLabel returns = new JLabel("Returns " + event.signature().returnType() + (java.util.Set.of("AnimatedCrossFadeBuilder", "AnimatedSwitcherTransitionBuilder", "AnimatedSwitcherLayoutBuilder").contains(event.callbackType())
                        ? ". New handlers use the native default builder and retain its children; customize it in Source."
                        : event.callbackType().equals("ShaderCallback")
                        ? ". New handlers return an opaque-white shader for bounds; customize it in Source."
                        : event.callbackType().equals("TransformCallback")
                        ? ". New handlers return Matrix4.identity(); customize the animation-dependent transform in Source."
                        : ". New handlers throw UnimplementedError until you implement an explicit return in Source."));
                returns.setName(RETURN_GUIDANCE_NAME); details.add(returns);
            }
            if (NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE.equals(widget.type())) {
                var notification = new JTextArea("Selected notification type: " + FlutterWidgetPropertiesNode.notificationTypeLabel(widget)
                        + ". The analyzer checks existing/project handlers against this type. Create Handler uses bool onNotification(Notification notification), compatible with every subtype. "
                        + "Return true to stop bubbling; false, an omitted handler or explicit null continues. Implement the generated TODO before running the app; it throws UnimplementedError. "
                        + "Layout notifications can arrive during layout: do not call setState synchronously then. Canvas never executes this handler.", 4, 70);
                notification.setName(NOTIFICATION_GUIDANCE_NAME); notification.setEditable(false); notification.setOpaque(false);
                notification.setLineWrap(true); notification.setWrapStyleWord(true); details.add(notification);
            }
            add(details, BorderLayout.NORTH);
            choices.setName(HANDLERS_NAME);
            choices.getAccessibleContext().setAccessibleName("Source event handler candidates");
            choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            var pages = new JTabbedPane();
            pages.addTab("Form methods", new JScrollPane(choices));
            var referenceDescriptor = new FeatureDescriptor();
            referenceDescriptor.setName(event.propertyName().value());
            referenceDescriptor.setDisplayName(event.propertyName().value());
            referenceEnvironment = PropertyEnv.create(referenceDescriptor);
            referenceEditor.setValue(getValue());
            if (referenceEditor instanceof ExPropertyEditor extended) extended.attachEnv(referenceEnvironment);
            JPanel reference = new JPanel(new BorderLayout(4, 4));
            reference.add(referenceEditor.getCustomEditor(), BorderLayout.CENTER);
            reference.add(button("Apply Reference", REFERENCE_APPLY_NAME, () -> {
                referenceEnvironment.setState(PropertyEnv.STATE_VALID);
                if (!PropertyEnv.STATE_VALID.equals(referenceEnvironment.getState())) {
                    throw new IllegalArgumentException("Correct the reference fields before applying this event binding.");
                }
                return context.editBinding(event.propertyName(),
                        ((FlutterPropertyCellValue) referenceEditor.getValue()).explicitValue());
            }), BorderLayout.SOUTH);
            pages.addTab("Reference", reference);
            if (event.propertyName().equals(stateEventProperty(widget))) {
                WidgetStateBindingCatalog.find(widget).ifPresent(contract ->
                        pages.addTab("State binding", statePanel(contract)));
            }
            if (widget.type().value().equals("flutter.material.IconButton")
                    && event.propertyName().value().equals("onPressed") && WidgetStateBindingCatalog.find(widget).isEmpty()) {
                JPanel unavailableState = new JPanel(new BorderLayout());
                JTextArea explanation = new JTextArea("IconButton State binding is available in toggle mode. "
                        + "Set Is Selected to true or false in Properties, then reopen On Pressed → State binding. "
                        + "Leaving Is Selected unset keeps the ordinary non-toggle button behavior.");
                explanation.setEditable(false);
                explanation.setLineWrap(true);
                explanation.setWrapStyleWord(true);
                explanation.setOpaque(false);
                unavailableState.add(explanation, BorderLayout.NORTH);
                pages.addTab("State binding", unavailableState);
            }
            add(pages, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout(4, 4));
            JPanel naming = new JPanel(new BorderLayout(8, 0));
            JLabel label = new JLabel("Handler name:");
            handlerName.setName(HANDLER_NAME);
            label.setLabelFor(handlerName);
            naming.add(label, BorderLayout.WEST);
            naming.add(handlerName, BorderLayout.CENTER);
            bottom.add(naming, BorderLayout.NORTH);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
            actions.add(button("Create Handler", CREATE_NAME,
                    () -> context.create(event.propertyName(), acceptedName())));
            bind = button("Bind Selected", BIND_NAME, () -> context.bind(event.propertyName(),
                    Objects.requireNonNull(choices.getSelectedValue(), "Choose an existing handler first.")));
            actions.add(bind);
            navigate = button("Go to Handler", NAVIGATE_NAME, () -> context.navigate(event.propertyName()));
            actions.add(navigate);
            rename = button("Rename Handler", RENAME_NAME,
                    () -> context.rename(event.propertyName(), acceptedName()));
            actions.add(rename);
            disconnect = button("Disconnect", DISCONNECT_NAME, () -> context.disconnect(event.propertyName()));
            actions.add(disconnect);
            bottom.add(actions, BorderLayout.CENTER);
            pages.addChangeListener(ignored -> {
                boolean statePage = pages.getTitleAt(pages.getSelectedIndex()).equals("State binding");
                naming.setVisible(!statePage);
                actions.setVisible(!statePage);
            });
            status.setName(STATUS_NAME);
            status.setEditable(false);
            status.setOpaque(false);
            status.setLineWrap(true);
            status.setWrapStyleWord(true);
            status.getAccessibleContext().setAccessibleName("Event operation status");
            bottom.add(status, BorderLayout.SOUTH);
            add(bottom, BorderLayout.SOUTH);
            choices.addListSelectionListener(ignored -> refreshActions());
            String reason = staleReason();
            if (!reason.isEmpty()) {
                status.setText(reason);
                refreshActions();
                return;
            }
            loadHandlers();
            if (stateFields != null && context.stateBindingUnavailableReason().isEmpty()) loadStateFields();
        }

        private JPanel statePanel(WidgetStateBindingCatalog.Descriptor contract) {
            stateContract = contract;
            JPanel result = new JPanel(new BorderLayout(8, 8));
            result.setBorder(new EmptyBorder(10, 4, 10, 4));
            JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
            boolean actionCallback = !stateEventProperty(widget).equals(WidgetStateBindingCatalog.ON_CHANGED);
            JLabel type = new JLabel(contract.dartType() + " — " + stateEventProperty(widget).value()
                    + (actionCallback ? "()" : "(" + contract.callbackParameterType() + " value)"));
            stateType = type;
            type.setName(STATE_TYPE_NAME);
            contract.referenceType().flatMap(PropertyValue.DartObjectReferenceValue::libraryUri).ifPresent(type::setToolTipText);
            fields.add(new JLabel("State field type:"));
            fields.add(type);
            stateMode = new JComboBox<>(new String[] {"Create new field", "Reuse existing field"});
            stateMode.setName(STATE_MODE_NAME);
            fields.add(new JLabel("Field source:"));
            fields.add(stateMode);
            stateFields = new JComboBox<>();
            stateFields.setName(STATE_FIELDS_NAME);
            stateFields.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                        int index, boolean selected, boolean focus) {
                    return super.getListCellRendererComponent(list, value instanceof StatePropertyBinding field
                            ? FlutterWidgetStatePropertyEditor.fieldLabel(field) : "Choose an existing State field", index, selected, focus);
                }
            });
            fields.add(new JLabel("Existing State field:"));
            fields.add(stateFields);
            stateField = new JTextField(widget.stateBinding().map(value -> value.fieldName()).orElseGet(() ->
                    suggestedStateFieldName()), 28);
            stateField.setName(STATE_FIELD_NAME);
            stateHandler = new JTextField(widget.stateBinding().map(value -> value.handlerName()).orElseGet(this::suggestedName), 28);
            stateHandler.setName(STATE_HANDLER_NAME);
            JLabel fieldLabel = new JLabel("Private field name:");
            fieldLabel.setLabelFor(stateField);
            fields.add(fieldLabel);
            fields.add(stateField);
            renamedStateField = new JTextField(widget.stateBinding().map(value -> value.fieldName()).orElse(""), 28);
            renamedStateField.setName(STATE_RENAME_FIELD_NAME);
            JLabel renameFieldLabel = new JLabel("New private State field name:");
            renameFieldLabel.setLabelFor(renamedStateField);
            fields.add(renameFieldLabel);
            fields.add(renamedStateField);
            JLabel handlerLabel = new JLabel("Private handler name:");
            handlerLabel.setLabelFor(stateHandler);
            fields.add(handlerLabel);
            fields.add(stateHandler);
            stateAction = new JComboBox<>(actionCallback
                    ? widget.type().value().equals("flutter.material.ListTile")
                            ? new StateBinding.Action[] {StateBinding.Action.TOGGLE, StateBinding.Action.SELECT}
                            : new StateBinding.Action[] {StateBinding.Action.TOGGLE}
                    : new StateBinding.Action[] {StateBinding.Action.CHANGE});
            stateAction.setName(STATE_ACTION_NAME);
            widget.stateBinding().ifPresent(binding -> stateAction.setSelectedItem(binding.action()));
            fields.add(new JLabel("Update action:"));
            fields.add(stateAction);
            selectionType = new JComboBox<>(new StateBinding.Type[] {
                StateBinding.Type.BOOL, StateBinding.Type.STRING, StateBinding.Type.INT, StateBinding.Type.DOUBLE});
            selectionType.setName(STATE_SELECTION_TYPE_NAME);
            fields.add(new JLabel("New selection field value type:"));
            fields.add(selectionType);
            stateSelection = new JTextField(widget.stateBinding().flatMap(value -> value.selectedValue())
                    .map(FlutterWidgetStatePropertyEditor::comparisonText).orElse("true"), 28);
            stateSelection.setName(STATE_SELECTION_NAME);
            fields.add(new JLabel("Selected value (Select action):"));
            fields.add(stateSelection);
            JLabel previewLabel = new JLabel(widget.stateBinding().isPresent()
                    ? "Canvas preview:" : "Initial value / Canvas preview:");
            previewLabel.setName("flutter.event.state.previewLabel");
            fields.add(previewLabel);
            fields.add(new JLabel(contract.previewProperties().isEmpty() ? "Independent Canvas text; not the runtime initializer"
                    : contract.previewProperties().stream().map(name -> name.value() + " = "
                    + (widget.properties().containsKey(name) ? PropertyValueFormatter.format(widget.properties().get(name)) : "null"))
                    .collect(java.util.stream.Collectors.joining(", "))));
            boolean controller = widget.type().value().equals("flutter.material.TextField");
            if (controller) {
                initialText = new JTextArea(3, 28);
                initialText.setName(STATE_INITIAL_TEXT_NAME);
                fields.add(new JLabel("Initial text (new controller only):"));
                fields.add(new JScrollPane(initialText));
            }
            JTextArea guidance = new JTextArea();
            stateGuidance = guidance;
            guidance.setName(STATE_GUIDANCE_NAME);
            guidance.setEditable(false);
            guidance.setOpaque(false);
            guidance.setLineWrap(true);
            guidance.setWrapStyleWord(true);
            String binding = widget.stateBinding().map(value -> "Bound to " + value.fieldName() + ". ").orElse("No State binding. ");
            stateGuidanceBase = binding + (controller
                    ? "Create adds a TextEditingController, lifecycle initialization/disposal, and a listener that refreshes dependent values when the text changes. "
                            + "Initial text is used once for a new controller; it is independent of Canvas preview. "
                    : "Create adds a private State field and a typed " + stateEventProperty(widget).value()
                    + " handler that updates it with setState. The current preview initializes the field once. ")
                    + "Later value-property edits change only Canvas preview, not the field initializer in Source. "
                    + "Reuse keeps the existing field initializer. Toggle inverts a bool; Select assigns the explicit selected value. "
                    + "For Select values, use true/false, a number, null, or plain text matching the selected field type. "
                    + (widget.type().value().equals("flutter.material.SwitchListTile")
                            ? "Selected and other widget options are preserved. SwitchListTile has no separate Enabled or Tristate: explicit null On changed disables the tile. "
                                    + "Remove keeps your field and handler code and restores the prior No-op, null or reference callback only if it is still the generated handler. "
                            : "Enabled and other widget options are preserved. Remove keeps your field and handler code and restores the prior callback only if it is still the generated handler. ")
                    + "Rename State Field updates its bindings and supported source references throughout this form, including the controller listener name; event handler names stay unchanged. "
                    + "Renaming does not save or navigate automatically. Go to Field only opens the verified declaration. "
                    + context.stateBindingUnavailableReason();
            if (widget.type().value().equals("flutter.material.Slider") || widget.type().value().equals("flutter.material.RangeSlider")) {
                stateGuidanceBase += " Keep reused Slider values within Minimum/Maximum and RangeSlider endpoints ordered within those bounds. "
                        + "Control State bindings do not implicitly clamp values; the field owner is responsible for valid runtime values.";
            }
            if (widget.type().value().equals("flutter.material.RadioListTile")) {
                stateGuidanceBase += " This is the legacy Group value State binding for this RadioListTile. A matching ancestor RadioGroup is preferred for coordinated sibling selection; bind its Group value instead for modern group ownership. "
                        + "RadioGroup selection and legacy callbacks may coexist; a nullable group selection can fall back to the tile's legacy Group value. "
                        + "Remove restores the prior omitted, No-op, null or reference callback without deleting user code or changing Enabled, Selected or the title/subtitle/secondary slots.";
            }
            guidance.setText(stateGuidanceBase);
            JPanel content = new JPanel(new BorderLayout(8, 8));
            content.add(fields, BorderLayout.NORTH);
            content.add(guidance, BorderLayout.CENTER);
            result.add(new JScrollPane(content), BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
            createState = button("Create State Binding", STATE_CREATE_NAME, () -> {
                StateBinding.Action action = (StateBinding.Action) stateAction.getSelectedItem();
                java.util.Optional<StatePropertyBinding> reused = stateMode.getSelectedIndex() == 1
                        ? java.util.Optional.of(Objects.requireNonNull((StatePropertyBinding) stateFields.getSelectedItem(), "Choose an existing State field first."))
                        : java.util.Optional.empty();
                StateBinding.Type valueType = reused.map(StatePropertyBinding::type)
                        .orElse((StateBinding.Type) selectionType.getSelectedItem());
                var selected = action == StateBinding.Action.SELECT
                        ? java.util.Optional.of(FlutterWidgetStatePropertyEditor.parseComparison(valueType, stateSelection.getText()))
                        : java.util.Optional.<PropertyValue>empty();
                return context.createStateBinding(stateField.getText().trim(), stateHandler.getText().trim(),
                        action, selected, initialText == null ? "" : initialText.getText(), reused);
            });
            removeState = button("Remove Binding", STATE_REMOVE_NAME, context::removeStateBinding);
            actions.add(createState);
            actions.add(removeState);
            navigateState = button("Go to Field", STATE_NAVIGATE_NAME, () -> context.navigateToStateField(
                    widget.stateBinding().orElseThrow().fieldName()), false);
            renameState = button("Rename State Field", STATE_RENAME_NAME, () -> context.renameStateField(
                    widget.stateBinding().orElseThrow().fieldName(), acceptedStateFieldName()));
            actions.add(navigateState);
            actions.add(renameState);
            result.add(actions, BorderLayout.SOUTH);
            stateMode.addActionListener(ignored -> refreshActions());
            stateAction.addActionListener(ignored -> { filterStateFields(); refreshActions(); });
            selectionType.addActionListener(ignored -> refreshActions());
            stateFields.addActionListener(ignored -> {
                if (stateMode.getSelectedIndex() == 1 && stateFields.getSelectedItem() instanceof StatePropertyBinding field) {
                    stateField.setText(field.fieldName());
                }
                refreshActions();
            });
            return result;
        }

        private void loadStateFields() {
            fieldsLoading = true;
            refreshActions();
            context.discoverStateFields().whenComplete((fields, error) -> onEdt(() -> {
                fieldsLoading = false;
                String reason = staleReason();
                if (!reason.isEmpty()) status.setText(reason);
                else if (error != null) status.setText("Existing State fields are unavailable: " + errorText(error));
                else {
                    discoveredFields = fields;
                    filterStateFields();
                }
                refreshActions();
            }));
        }

        private void filterStateFields() {
            Object previous = stateFields.getSelectedItem();
            stateFields.removeAllItems();
            boolean select = stateAction.getSelectedItem() == StateBinding.Action.SELECT;
            discoveredFields.stream().filter(field -> select
                    ? field.type() != StateBinding.Type.RANGE_VALUES && field.type() != StateBinding.Type.TEXT_CONTROLLER
                    : field.type() == stateContract.type() && field.referenceType().equals(stateContract.referenceType()))
                    .forEach(stateFields::addItem);
            for (int index = 0; index < stateFields.getItemCount(); index++) {
                if (stateFields.getItemAt(index).equals(previous)) { stateFields.setSelectedIndex(index); break; }
            }
        }

        private String acceptedName() {
            String name = handlerName.getText().trim();
            if (!name.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) {
                throw new IllegalArgumentException("Enter a valid Dart method name.");
            }
            return name;
        }

        private JButton button(String label, String name, Supplier<CompletionStage<Void>> operation) {
            return button(label, name, operation, true);
        }

        private JButton button(String label, String name, Supplier<CompletionStage<Void>> operation, boolean mutation) {
            JButton button = new JButton(label);
            button.setName(name);
            button.addActionListener(ignored -> {
                String reason = inactiveBindingReason();
                if (List.of(CREATE_NAME, BIND_NAME, REFERENCE_APPLY_NAME).contains(name) && !reason.isEmpty()) {
                    status.setText(reason); refreshActions(); return;
                }
                run(operation, mutation);
            });
            buttons.add(button);
            return button;
        }

        private void loadHandlers() {
            busy = true;
            status.setText("Reading form method candidates…");
            refreshActions();
            try {
                context.discover(event.propertyName()).whenComplete((result, error) ->
                        onEdt(() -> {
                            busy = false;
                            String reason = staleReason();
                            if (!reason.isEmpty()) status.setText(reason);
                            else if (error != null) status.setText(errorText(error));
                            else {
                                handlers.clear();
                                result.forEach(handlers::addElement);
                                status.setText(!inactiveBindingReason().isEmpty() ? inactiveBindingReason() : result.isEmpty()
                                        ? "No user-owned form methods. Create a handler, or use Reference for another callable."
                                        : "Select a form method candidate, or enter a name to create one. Dart type compatibility is checked when bound; Reference also supports imported callables.");
                            }
                            refreshActions();
                        }));
            } catch (RuntimeException error) {
                busy = false;
                status.setText(errorText(error));
                refreshActions();
            }
        }

        private void run(Supplier<CompletionStage<Void>> operation, boolean mutation) {
            String reason = staleReason();
            if (!reason.isEmpty()) {
                status.setText(reason);
                refreshActions();
                return;
            }
            if (busy) return;
            busy = true;
            status.setText(mutation ? "Applying event operation…" : "Opening the State field declaration…");
            refreshActions();
            try {
                operation.get().whenComplete((ignored, error) -> onEdt(() -> {
                    busy = false;
                    if (error != null) {
                        status.setText(errorText(error));
                        refreshActions();
                        return;
                    }
                    operationCompleted = true;
                    status.setText("Done.");
                    // Actions are committed by the context, not by the host
                    // PropertySheet's unchanged value on OK/Cancel.
                    Window window = SwingUtilities.getWindowAncestor(this);
                    if (window instanceof Dialog) window.dispose();
                    refreshActions();
                }));
            } catch (RuntimeException error) {
                busy = false;
                status.setText(errorText(error));
                refreshActions();
            }
        }

        private void refreshActions() {
            boolean ready = !busy && !operationCompleted && staleReason().isEmpty();
            buttons.forEach(button -> button.setEnabled(ready));
            choices.setEnabled(ready);
            handlerName.setEnabled(ready);
            bind.setEnabled(ready && choices.getSelectedValue() != null);
            PropertyValue value = ((FlutterPropertyCellValue) getValue()).explicitValue().orElse(null);
            boolean hasHandler = value instanceof PropertyValue.CallbackValue
                    || value instanceof PropertyValue.DartObjectReferenceValue;
            navigate.setEnabled(ready && hasHandler);
            rename.setEnabled(ready && hasHandler);
            disconnect.setEnabled(ready && value != null);
            if (!inactiveBindingReason().isEmpty()) {
                buttons.stream().filter(button -> List.of(CREATE_NAME, BIND_NAME, REFERENCE_APPLY_NAME).contains(button.getName()))
                        .forEach(button -> button.setEnabled(false));
                choices.setEnabled(false);
            }
            if (createState != null) {
                boolean stateReady = ready && context.stateBindingUnavailableReason().isEmpty();
                boolean bound = currentWidget.get().stateBinding().isPresent();
                boolean reuse = stateMode.getSelectedIndex() == 1;
                createState.setEnabled(stateReady && !bound && (!reuse || !fieldsLoading && stateFields.getSelectedItem() != null));
                removeState.setEnabled(stateReady && bound);
                renameState.setEnabled(stateReady && bound);
                navigateState.setEnabled(stateReady && bound);
                renamedStateField.setEnabled(stateReady && bound);
                stateField.setEnabled(stateReady && !bound && !reuse);
                stateHandler.setEnabled(stateReady && !bound);
                stateMode.setEnabled(stateReady && !bound);
                stateFields.setEnabled(stateReady && !bound && reuse && !fieldsLoading);
                stateAction.setEnabled(stateReady && !bound && stateAction.getItemCount() > 1);
                stateSelection.setEnabled(stateReady && !bound && stateAction.getSelectedItem() == StateBinding.Action.SELECT);
                selectionType.setEnabled(stateReady && !bound && !reuse && stateAction.getSelectedItem() == StateBinding.Action.SELECT);
                if (initialText != null) initialText.setEnabled(stateReady && !bound && !reuse);
                if (reuse && stateFields.getSelectedItem() instanceof StatePropertyBinding field) stateField.setText(field.fieldName());
                refreshStateDescription(bound, reuse);
            }
        }

        private String acceptedStateFieldName() {
            String name = renamedStateField.getText().trim();
            if (!name.matches("_[A-Za-z][A-Za-z0-9_]*") || name.length() > 128) {
                throw new IllegalArgumentException("Enter a private Dart State field name starting with an underscore (at most 128 characters).");
            }
            return name;
        }

        private void refreshStateDescription(boolean bound, boolean reuse) {
            String fieldType = stateContract.dartType();
            if (bound) {
                var binding = currentWidget.get().stateBinding().orElseThrow();
                fieldType = WidgetStateBindingCatalog.dartType(binding.type(), binding.referenceType());
            } else if (reuse && stateFields.getSelectedItem() instanceof StatePropertyBinding field) {
                fieldType = WidgetStateBindingCatalog.dartType(field.type(), field.referenceType());
            } else if (stateAction.getSelectedItem() == StateBinding.Action.SELECT) {
                fieldType = WidgetStateBindingCatalog.dartType((StateBinding.Type) selectionType.getSelectedItem(), java.util.Optional.empty()) + "?";
            }
            stateType.setText(fieldType + " — " + stateEventProperty(widget).value()
                    + (stateContract.callbackParameterType().isEmpty() ? "()" : "(" + stateContract.callbackParameterType() + " value)"));
            String guidance = stateGuidanceBase;
            if (!bound && stateAction.getSelectedItem() == StateBinding.Action.SELECT) {
                guidance += reuse ? " Select preserves the reused field's current value and initializer; Selected remains a Canvas preview."
                        : " A new selection field is nullable: it starts with the selected value when the Selected preview is true, otherwise null.";
            }
            stateGuidance.setText(guidance);
        }

        private String suggestedName() {
            String type = widget.type().value();
            type = type.substring(type.lastIndexOf('.') + 1);
            String eventName = event.propertyName().value();
            return "_" + Character.toLowerCase(type.charAt(0)) + type.substring(1)
                    + Character.toUpperCase(eventName.charAt(0)) + eventName.substring(1);
        }

        private String suggestedStateFieldName() {
            String type = widget.type().value();
            type = type.substring(type.lastIndexOf('.') + 1);
            return "_" + Character.toLowerCase(type.charAt(0)) + type.substring(1)
                    + (widget.type().value().equals("flutter.material.TextField") ? "Controller"
                            : stateEventProperty(widget).equals(WidgetStateBindingCatalog.ON_CHANGED) ? "Value" : "Selected");
        }
    }

    static PropertyName stateEventProperty(WidgetNode widget) {
        return WidgetStateBindingCatalog.find(widget).map(WidgetStateBindingCatalog.Descriptor::eventProperty)
                .orElse(WidgetStateBindingCatalog.ON_CHANGED);
    }

    private static void onEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) runnable.run();
        else SwingUtilities.invokeLater(runnable);
    }

    private static String errorText(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) {
            error = error.getCause();
        }
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
