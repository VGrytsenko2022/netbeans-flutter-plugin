package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerWidgetMovePlanner;
import dev.flutter.netbeans.plugin.designer.icons.FlutterWidgetIconRegistry;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.util.ImageUtilities;

/** Transactional native NetBeans custom editor for one exact named slot. */
final class FlutterWidgetSlotPropertyEditor extends PropertyEditorSupport
        implements ExPropertyEditor {
    static final String CURRENT_LIST_NAME = "flutter.slot.current";
    static final String ACTION_NAME = "flutter.slot.action";
    static final String ADD_TYPE_NAME = "flutter.slot.addType";
    static final String MOVE_SOURCE_NAME = "flutter.slot.moveSource";
    static final String POSITION_NAME = "flutter.slot.position";
    static final String STATUS_NAME = "flutter.slot.status";

    private final Model model;
    private PropertyEnv environment;
    private Component customEditor;

    FlutterWidgetSlotPropertyEditor(
            WidgetNode owner,
            WidgetDefinition ownerDefinition,
            SlotDefinition slotDefinition,
            FlutterWidgetSlotEditorContext context) {
        model = new Model(owner, ownerDefinition, slotDefinition, context);
        super.setValue(FlutterWidgetSlotCellValue.current(model.summary()));
    }

    @Override
    public void attachEnv(PropertyEnv candidate) {
        environment = Objects.requireNonNull(candidate, "candidate");
        customEditor = null;
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof FlutterWidgetSlotCellValue candidate)) {
            throw new IllegalArgumentException(
                    "Slot editor requires FlutterWidgetSlotCellValue.");
        }
        candidate.mutation().ifPresent(model::validateIntentTarget);
        super.setValue(candidate);
    }

    @Override
    public String getAsText() {
        return ((FlutterWidgetSlotCellValue) getValue()).summary();
    }

    @Override
    public void setAsText(String text) {
        if (!Objects.equals(getAsText(), text)) {
            throw new IllegalArgumentException(
                    "Use the named-slot editor button to change this slot.");
        }
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    @Override
    public Component getCustomEditor() {
        if (customEditor == null) {
            if (environment == null) {
                FeatureDescriptor descriptor = new FeatureDescriptor();
                descriptor.setName(model.slot().name().value());
                descriptor.setDisplayName(model.displayName());
                environment = PropertyEnv.create(descriptor);
            }
            customEditor = new SlotPanel(this, model, environment);
        }
        return customEditor;
    }

    /** Immutable dialog model derived from the selected revision. */
    static final class Model {
        private final WidgetNode owner;
        private final WidgetDefinition ownerDefinition;
        private final SlotDefinition slot;
        private final FlutterWidgetSlotEditorContext context;
        private final WidgetSlot current;
        private final List<WidgetNode> currentChildren;
        private final List<DefinitionChoice> addChoices;
        private final List<MoveChoice> moveChoices;
        private final Optional<String> structuralProblem;

        Model(
                WidgetNode owner,
                WidgetDefinition ownerDefinition,
                SlotDefinition slot,
                FlutterWidgetSlotEditorContext context) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.ownerDefinition = Objects.requireNonNull(
                    ownerDefinition, "ownerDefinition");
            this.slot = Objects.requireNonNull(slot, "slot");
            this.context = Objects.requireNonNull(context, "context");
            if (!owner.type().equals(ownerDefinition.typeId())) {
                throw new IllegalArgumentException(
                        "Slot owner does not match its catalog definition.");
            }
            if (ownerDefinition.slot(slot.name()).filter(slot::equals).isEmpty()) {
                throw new IllegalArgumentException(
                        "Slot is not declared by the owner definition: "
                        + slot.name().value());
            }
            WidgetNode authoritative = find(context.document().root(), owner.id())
                    .orElseThrow(() -> new IllegalArgumentException(
                    "Slot owner is absent from the bound document: " + owner.id()));
            if (!authoritative.equals(owner)) {
                throw new IllegalArgumentException(
                        "Slot owner snapshot differs from the bound document revision: "
                        + owner.id());
            }
            current = owner.slots().get(slot.name());
            structuralProblem = current != null
                    && current.cardinality() != slot.cardinality()
                    ? Optional.of("The model slot is "
                            + current.cardinality().wireName()
                            + " but the catalog declares "
                            + slot.cardinality().wireName() + ".")
                    : Optional.empty();
            currentChildren = structuralProblem.isPresent()
                    ? List.of() : children(current);
            addChoices = buildAddChoices();
            moveChoices = buildMoveChoices();
        }

        WidgetNode owner() {
            return owner;
        }

        SlotDefinition slot() {
            return slot;
        }

        String displayName() {
            return FlutterWidgetPropertiesNode.displayName(slot.name());
        }

        String summary() {
            if (currentChildren.isEmpty()) {
                return "Empty";
            }
            if (slot.cardinality() == SlotCardinality.SINGLE) {
                WidgetNode child = currentChildren.getFirst();
                return definition(child).palette().displayName();
            }
            return currentChildren.size() == 1
                    ? "1 widget" : currentChildren.size() + " widgets";
        }

        String details() {
            String maximum = Integer.toString(slot.maxChildren());
            return slot.cardinality().wireName() + " slot · "
                    + currentChildren.size() + "/" + maximum
                    + " · minimum " + slot.minChildren();
        }

        Optional<String> structuralProblem() {
            return structuralProblem;
        }

        List<WidgetChoice> currentChoices() {
            return currentChildren.stream().map(this::widgetChoice).toList();
        }

        List<DefinitionChoice> addChoices() {
            return addChoices;
        }

        List<MoveChoice> moveChoices() {
            return moveChoices;
        }

        boolean canAdd() {
            if (structuralProblem.isPresent() || addChoices.isEmpty()) {
                return false;
            }
            int maximum = Math.min(slot.maxChildren(), WidgetSlot.MAX_LIST_CHILDREN);
            return currentChildren.size() < maximum;
        }

        boolean canRemove() {
            return structuralProblem.isEmpty()
                    && currentChildren.size() > slot.minChildren();
        }

        int appendIndex() {
            return slot.cardinality() == SlotCardinality.SINGLE
                    ? 0 : currentChildren.size();
        }

        String unavailableReason() {
            if (structuralProblem.isPresent()) {
                return structuralProblem.orElseThrow();
            }
            if (currentChildren.isEmpty() && addChoices.isEmpty()
                    && moveChoices.isEmpty()) {
                return "No available Palette or existing widget is compatible with this slot.";
            }
            return "Choose one operation. The mutation is applied only after OK.";
        }

        void validateIntentTarget(FlutterWidgetSlotMutation intent) {
            if (!owner.id().equals(intent.ownerId())
                    || !slot.name().equals(intent.slotName())) {
                throw new IllegalArgumentException(
                        "Slot mutation targets another owner or slot.");
            }
        }

        private List<DefinitionChoice> buildAddChoices() {
            if (structuralProblem.isPresent()) {
                return List.of();
            }
            ArrayList<DefinitionChoice> choices = new ArrayList<>();
            for (var type : context.insertableWidgetTypes()) {
                WidgetDefinition definition = context.catalog().find(type).orElseThrow();
                if (slot.acceptance().accepts(definition)) {
                    choices.add(new DefinitionChoice(definition));
                }
            }
            return List.copyOf(choices);
        }

        private List<MoveChoice> buildMoveChoices() {
            if (structuralProblem.isPresent()) {
                return List.of();
            }
            FlutterDesignerWidgetMovePlanner planner =
                    new FlutterDesignerWidgetMovePlanner();
            ArrayList<MoveChoice> choices = new ArrayList<>();
            ArrayList<WidgetNode> nodes = new ArrayList<>();
            collect(context.document().root(), nodes);
            for (WidgetNode candidate : nodes) {
                if (candidate.id().equals(context.document().root().id())) {
                    continue;
                }
                WidgetDefinition candidateDefinition = definition(candidate);
                if (!slot.acceptance().accepts(candidateDefinition)) {
                    continue;
                }
                boolean alreadyInTargetSlot = currentChildren.stream()
                        .anyMatch(child -> child.id().equals(candidate.id()));
                int terminalPostRemovalIndex = slot.cardinality()
                        == SlotCardinality.SINGLE
                        ? 0
                        : currentChildren.size() - (alreadyInTargetSlot ? 1 : 0);
                ArrayList<Integer> acceptedIndexes = new ArrayList<>();
                for (int index = 0; index <= terminalPostRemovalIndex; index++) {
                    var result = planner.plan(
                            context.document(),
                            context.catalog(),
                            candidate.id(),
                            new FlutterDesignerWidgetMovePlanner.IntoSlot(
                                    owner.id(), slot.name(), index));
                    if (result instanceof FlutterDesignerWidgetMovePlanner.Accepted) {
                        acceptedIndexes.add(index);
                    }
                }
                if (!acceptedIndexes.isEmpty()) {
                    List<PositionChoice> positions = acceptedIndexes.stream()
                            .map(index -> new PositionChoice(
                            index, positionLabel(index, terminalPostRemovalIndex)))
                            .toList();
                    choices.add(new MoveChoice(
                            widgetChoice(candidate), positions));
                }
            }
            return List.copyOf(choices);
        }

        private WidgetDefinition definition(WidgetNode node) {
            return context.catalog().find(node.type()).orElseThrow(() ->
                new IllegalArgumentException(
                        "Widget type is absent from the bound catalog: "
                        + node.type().value()));
        }

        private WidgetChoice widgetChoice(WidgetNode node) {
            WidgetDefinition definition = definition(node);
            return new WidgetChoice(
                    node.id(),
                    node.type(),
                    definition.palette().displayName() + " — " + node.id(),
                    definition.palette().displayName());
        }

        private static String positionLabel(int index, int lastCandidateIndex) {
            if (index == 0) {
                return "First (index 0)";
            }
            if (index == lastCandidateIndex) {
                return "Last (index " + index + ")";
            }
            return "Position " + (index + 1) + " (index " + index + ")";
        }

        private static List<WidgetNode> children(WidgetSlot slot) {
            if (slot == null) {
                return List.of();
            }
            return switch (slot) {
                case WidgetSlot.SingleSlot single -> single.child().stream().toList();
                case WidgetSlot.ListSlot list -> list.children();
            };
        }

        private static Optional<WidgetNode> find(WidgetNode root, StableId id) {
            if (root.id().equals(id)) {
                return Optional.of(root);
            }
            for (WidgetNode child : directChildren(root)) {
                Optional<WidgetNode> found = find(child, id);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }

        private static void collect(WidgetNode root, List<WidgetNode> target) {
            target.add(root);
            for (WidgetNode child : directChildren(root)) {
                collect(child, target);
            }
        }

        private static List<WidgetNode> directChildren(WidgetNode owner) {
            ArrayList<WidgetNode> result = new ArrayList<>();
            for (WidgetSlot value : owner.slots().values()) {
                switch (value) {
                    case WidgetSlot.SingleSlot single ->
                        single.child().ifPresent(result::add);
                    case WidgetSlot.ListSlot list -> result.addAll(list.children());
                }
            }
            return result;
        }
    }

    private static final class SlotPanel extends JPanel
            implements PropertyChangeListener {
        private final FlutterWidgetSlotPropertyEditor editor;
        private final Model model;
        private final PropertyEnv environment;
        private final JList<WidgetChoice> current = new JList<>();
        private final JComboBox<ActionChoice> action = new JComboBox<>();
        private final JComboBox<DefinitionChoice> addType = new JComboBox<>();
        private final JComboBox<MoveChoice> moveSource = new JComboBox<>();
        private final JComboBox<PositionChoice> position = new JComboBox<>();
        private final JLabel status = new JLabel();
        private FlutterWidgetSlotCellValue draft;
        private boolean committed;
        private boolean updating;

        SlotPanel(
                FlutterWidgetSlotPropertyEditor editor,
                Model model,
                PropertyEnv environment) {
            this.editor = Objects.requireNonNull(editor, "editor");
            this.model = Objects.requireNonNull(model, "model");
            this.environment = Objects.requireNonNull(environment, "environment");
            draft = (FlutterWidgetSlotCellValue) editor.getValue();
            setLayout(new BorderLayout(0, 10));
            setBorder(new EmptyBorder(10, 10, 10, 10));
            setPreferredSize(new Dimension(640, 410));
            setName("flutter.slot.custom");
            getAccessibleContext().setAccessibleName(
                    model.displayName() + " slot editor");
            getAccessibleContext().setAccessibleDescription(
                    "Manages the exact " + model.ownerDefinition.palette().displayName()
                    + "." + model.slot().name().value() + " slot.");

            JLabel heading = new JLabel(model.displayName() + " — " + model.details());
            heading.getAccessibleContext().setAccessibleDescription(
                    "Exact slot " + model.owner().id() + "."
                    + model.slot().name().value() + ".");
            add(heading, BorderLayout.NORTH);

            current.setName(CURRENT_LIST_NAME);
            current.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            current.setCellRenderer(new WidgetRenderer());
            DefaultListModel<WidgetChoice> currentModel = new DefaultListModel<>();
            model.currentChoices().forEach(currentModel::addElement);
            current.setModel(currentModel);
            if (!currentModel.isEmpty()) {
                current.setSelectedIndex(0);
            }
            current.getAccessibleContext().setAccessibleName("Current slot widgets");
            current.getAccessibleContext().setAccessibleDescription(
                    "Direct children currently stored in the exact named slot.");

            action.setName(ACTION_NAME);
            addType.setName(ADD_TYPE_NAME);
            moveSource.setName(MOVE_SOURCE_NAME);
            position.setName(POSITION_NAME);
            status.setName(STATUS_NAME);
            action.getAccessibleContext().setAccessibleName("Slot operation");
            addType.getAccessibleContext().setAccessibleName("New widget type");
            moveSource.getAccessibleContext().setAccessibleName("Existing widget to move");
            position.getAccessibleContext().setAccessibleName("Destination position");
            status.getAccessibleContext().setAccessibleName("Slot operation status");

            action.addItem(ActionChoice.NONE);
            if (model.canAdd()) {
                action.addItem(ActionChoice.ADD);
            }
            if (!model.moveChoices().isEmpty()) {
                action.addItem(ActionChoice.MOVE);
            }
            if (model.canRemove()) {
                action.addItem(model.slot().cardinality() == SlotCardinality.SINGLE
                        ? ActionChoice.CLEAR : ActionChoice.REMOVE);
            }
            model.addChoices().forEach(addType::addItem);
            model.moveChoices().forEach(moveSource::addItem);
            addType.setRenderer(new DefinitionRenderer());
            moveSource.setRenderer(new MoveRenderer());

            JPanel form = new JPanel(new GridBagLayout());
            int row = 0;
            addRow(form, row++, "Current:", new JScrollPane(current), 1.0, 1.0);
            addRow(form, row++, "Operation:", action, 1.0, 0.0);
            addRow(form, row++, "New type:", addType, 1.0, 0.0);
            addRow(form, row++, "Move widget:", moveSource, 1.0, 0.0);
            addRow(form, row, "Position:", position, 1.0, 0.0);
            add(form, BorderLayout.CENTER);
            add(status, BorderLayout.SOUTH);

            action.addActionListener(ignored -> updateDraft());
            addType.addActionListener(ignored -> updateDraft());
            moveSource.addActionListener(ignored -> {
                refreshPositions();
                updateDraft();
            });
            position.addActionListener(ignored -> updateDraft());
            current.addListSelectionListener(ignored -> {
                if (!ignored.getValueIsAdjusting()) {
                    updateDraft();
                }
            });
            environment.addPropertyChangeListener(this);
            refreshPositions();
            updateDraft();
        }

        private void refreshPositions() {
            updating = true;
            try {
                DefaultComboBoxModel<PositionChoice> positions =
                        new DefaultComboBoxModel<>();
                MoveChoice selected = (MoveChoice) moveSource.getSelectedItem();
                if (selected != null) {
                    selected.positions().forEach(positions::addElement);
                }
                position.setModel(positions);
            } finally {
                updating = false;
            }
        }

        private void updateDraft() {
            if (updating) {
                return;
            }
            ActionChoice selected = Objects.requireNonNullElse(
                    (ActionChoice) action.getSelectedItem(), ActionChoice.NONE);
            addType.setEnabled(selected == ActionChoice.ADD);
            moveSource.setEnabled(selected == ActionChoice.MOVE);
            position.setEnabled(selected == ActionChoice.MOVE);
            current.setEnabled(selected == ActionChoice.REMOVE
                    || selected == ActionChoice.CLEAR
                    || selected == ActionChoice.NONE);
            try {
                draft = switch (selected) {
                    case NONE -> FlutterWidgetSlotCellValue.current(model.summary());
                    case ADD -> addDraft();
                    case MOVE -> moveDraft();
                    case REMOVE, CLEAR -> removeDraft(selected);
                };
                environment.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
                String message = selected == ActionChoice.NONE
                        ? model.unavailableReason()
                        : draft.summary() + ". Click OK to apply one atomic mutation.";
                setStatus(message, false);
            } catch (IllegalArgumentException failure) {
                environment.setState(PropertyEnv.STATE_INVALID);
                setStatus(failure.getMessage(), true);
            }
        }

        private FlutterWidgetSlotCellValue addDraft() {
            DefinitionChoice selected = (DefinitionChoice) addType.getSelectedItem();
            if (selected == null || !model.canAdd()) {
                throw new IllegalArgumentException(
                        "No compatible Palette widget can be added to this slot.");
            }
            FlutterWidgetSlotMutation.Add intent = new FlutterWidgetSlotMutation.Add(
                    model.owner().id(),
                    model.slot().name(),
                    selected.definition().typeId(),
                    model.appendIndex());
            return FlutterWidgetSlotCellValue.staged(
                    "Add " + selected + " at index " + model.appendIndex(), intent);
        }

        private FlutterWidgetSlotCellValue moveDraft() {
            MoveChoice source = (MoveChoice) moveSource.getSelectedItem();
            PositionChoice destination = (PositionChoice) position.getSelectedItem();
            if (source == null || destination == null) {
                throw new IllegalArgumentException(
                        "Choose an existing compatible widget and destination position.");
            }
            FlutterWidgetSlotMutation.Move intent = new FlutterWidgetSlotMutation.Move(
                    model.owner().id(),
                    model.slot().name(),
                    source.widget().id(),
                    destination.index());
            return FlutterWidgetSlotCellValue.staged(
                    "Move " + source.widget().shortLabel() + " to "
                    + destination, intent);
        }

        private FlutterWidgetSlotCellValue removeDraft(ActionChoice selected) {
            WidgetChoice child = current.getSelectedValue();
            if (child == null || !model.canRemove()) {
                throw new IllegalArgumentException(
                        "Choose a removable direct child of this slot.");
            }
            FlutterWidgetSlotMutation.Remove intent =
                    new FlutterWidgetSlotMutation.Remove(
                            model.owner().id(), model.slot().name(), child.id());
            String verb = selected == ActionChoice.CLEAR ? "Clear" : "Remove";
            return FlutterWidgetSlotCellValue.staged(
                    verb + " " + child.shortLabel(), intent);
        }

        private void setStatus(String message, boolean error) {
            status.setText(message);
            status.setToolTipText(message);
            status.putClientProperty("JComponent.outline", error ? "error" : null);
            status.getAccessibleContext().setAccessibleDescription(
                    (error ? "Invalid slot operation. " : "Slot operation. ") + message);
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (PropertyEnv.PROP_STATE.equals(event.getPropertyName())
                    && event.getNewValue() == PropertyEnv.STATE_VALID
                    && !committed
                    && environment.getState() != PropertyEnv.STATE_INVALID) {
                committed = true;
                editor.setValue(draft);
            }
        }

        private static void addRow(
                JPanel panel,
                int row,
                String label,
                JComponent component,
                double weightX,
                double weightY) {
            GridBagConstraints left = new GridBagConstraints();
            left.gridx = 0;
            left.gridy = row;
            left.anchor = GridBagConstraints.NORTHWEST;
            left.insets = new Insets(3, 0, 3, 10);
            panel.add(new JLabel(label), left);

            GridBagConstraints right = new GridBagConstraints();
            right.gridx = 1;
            right.gridy = row;
            right.weightx = weightX;
            right.weighty = weightY;
            right.fill = weightY > 0
                    ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
            right.insets = new Insets(3, 0, 3, 0);
            panel.add(component, right);
        }
    }

    private enum ActionChoice {
        NONE("No change"),
        ADD("Add new widget"),
        MOVE("Move existing widget here"),
        REMOVE("Remove selected widget"),
        CLEAR("Clear slot");

        private final String label;

        ActionChoice(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record DefinitionChoice(WidgetDefinition definition) {
        private DefinitionChoice {
            Objects.requireNonNull(definition, "definition");
        }

        @Override
        public String toString() {
            return definition.palette().displayName();
        }
    }

    private record WidgetChoice(
            StableId id,
            dev.flutter.netbeans.designer.model.WidgetTypeId type,
            String label,
            String shortLabel) {
        private WidgetChoice {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(shortLabel, "shortLabel");
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record PositionChoice(int index, String label) {
        private PositionChoice {
            if (index < 0) {
                throw new IllegalArgumentException("index must be non-negative");
            }
            Objects.requireNonNull(label, "label");
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record MoveChoice(
            WidgetChoice widget,
            List<PositionChoice> positions) {
        private MoveChoice {
            Objects.requireNonNull(widget, "widget");
            positions = List.copyOf(positions);
            if (positions.isEmpty()) {
                throw new IllegalArgumentException("Move choice requires a position.");
            }
        }

        @Override
        public String toString() {
            return widget.toString();
        }
    }

    private static final class WidgetRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focused);
            if (value instanceof WidgetChoice widget) {
                label.setText(widget.label());
                FlutterWidgetIconRegistry.findIconPath(widget.type())
                        .map(path -> ImageUtilities.loadImageIcon(path, false))
                        .ifPresent(label::setIcon);
                label.setToolTipText(widget.id().toString());
            }
            return label;
        }
    }

    private static final class DefinitionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focused);
            if (value instanceof DefinitionChoice choice) {
                label.setText(choice.toString());
                FlutterWidgetIconRegistry.findIconPath(choice.definition().typeId())
                        .map(path -> ImageUtilities.loadImageIcon(path, false))
                        .ifPresent(label::setIcon);
                label.setToolTipText(choice.definition().typeId().value());
            }
            return label;
        }
    }

    private static final class MoveRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focused);
            if (value instanceof MoveChoice choice) {
                label.setText(choice.widget().label());
                FlutterWidgetIconRegistry.findIconPath(choice.widget().type())
                        .map(path -> ImageUtilities.loadImageIcon(path, false))
                        .ifPresent(label::setIcon);
                label.setToolTipText(choice.widget().id().toString());
            }
            return label;
        }
    }
}
