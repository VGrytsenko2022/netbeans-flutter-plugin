package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.command.AddWidget;
import dev.flutter.netbeans.designer.command.ClearSlotChildren;
import dev.flutter.netbeans.designer.command.CreateEventHandler;
import dev.flutter.netbeans.designer.command.CreateMenuAnchorBuilder;
import dev.flutter.netbeans.designer.command.CreateStateBinding;
import dev.flutter.netbeans.designer.command.BindPropertyToState;
import dev.flutter.netbeans.designer.command.RemovePropertyStateBinding;
import dev.flutter.netbeans.designer.command.RemoveStateBinding;
import dev.flutter.netbeans.designer.command.RenameEventHandler;
import dev.flutter.netbeans.designer.command.RenameStateField;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.DesignerCommandKind;
import dev.flutter.netbeans.designer.command.MoveWidget;
import dev.flutter.netbeans.designer.command.PatchProperties;
import dev.flutter.netbeans.designer.command.RemoveWidget;
import dev.flutter.netbeans.designer.command.ReplaceSlotChild;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.command.WrapWidget;
import java.util.Objects;

/** Stable user-facing presentation for all closed designer command variants. */
final class DesignerCommandPresentation {
    private DesignerCommandPresentation() {
    }

    static String title(DesignerCommandKind kind) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case ADD_WIDGET -> "Add Flutter Widget";
            case EDIT_TABLE_GRID -> "Edit Flutter Table Grid";
            case EDIT_DATA_TABLE_GRID -> "Edit Flutter Data Table";
            case REMOVE_WIDGET -> "Remove Flutter Widget";
            case MOVE_WIDGET -> "Move Flutter Widget";
            case REPLACE_SLOT_CHILD -> "Replace Flutter Slot Child";
            case CLEAR_SLOT_CHILDREN -> "Clear Flutter Slot Children";
            case WRAP_WIDGET -> "Wrap Flutter Widget";
            case SET_PROPERTY -> "Set Flutter Property";
            case RESET_PROPERTY -> "Reset Flutter Property";
            case PATCH_PROPERTIES -> "Update Flutter Properties";
            case CREATE_EVENT_HANDLER -> "Create Flutter Event Handler";
            case CREATE_MENU_ANCHOR_BUILDER -> "Create Menu Anchor Builder";
            case RENAME_EVENT_HANDLER -> "Rename Flutter Event Handler";
            case RENAME_STATE_FIELD -> "Rename Flutter State Field";
            case CREATE_STATE_BINDING -> "Create Flutter State Binding";
            case REMOVE_STATE_BINDING -> "Remove Flutter State Binding";
            case BIND_PROPERTY_TO_STATE -> "Bind Flutter Property to State";
            case REMOVE_PROPERTY_STATE_BINDING -> "Remove Flutter Property State Binding";
        };
    }

    static String operation(DesignerCommand command) {
        Objects.requireNonNull(command, "command");
        return switch (command.kind()) {
            case ADD_WIDGET -> "Add Flutter widget";
            case EDIT_TABLE_GRID -> "Edit Flutter table grid";
            case EDIT_DATA_TABLE_GRID -> "Edit Flutter data table";
            case REMOVE_WIDGET -> "Remove Flutter widget";
            case MOVE_WIDGET -> "Move Flutter widget";
            case REPLACE_SLOT_CHILD -> "Replace Flutter slot child";
            case CLEAR_SLOT_CHILDREN -> "Clear Flutter slot children";
            case WRAP_WIDGET -> "Wrap Flutter widget";
            case SET_PROPERTY -> "Set Flutter property";
            case RESET_PROPERTY -> "Reset Flutter property";
            case PATCH_PROPERTIES -> "Update Flutter properties";
            case CREATE_EVENT_HANDLER -> "Create Flutter event handler";
            case CREATE_MENU_ANCHOR_BUILDER -> "Create MenuAnchor builder";
            case RENAME_EVENT_HANDLER -> "Rename Flutter event handler";
            case RENAME_STATE_FIELD -> "Rename Flutter State field";
            case CREATE_STATE_BINDING -> "Create Flutter state binding";
            case REMOVE_STATE_BINDING -> "Remove Flutter state binding";
            case BIND_PROPERTY_TO_STATE -> "Bind Flutter property to State";
            case REMOVE_PROPERTY_STATE_BINDING -> "Remove Flutter property State binding";
        };
    }

    static String target(DesignerCommand command) {
        Objects.requireNonNull(command, "command");
        return switch (command) {
            case dev.flutter.netbeans.designer.command.EditDataTableGrid grid -> grid.expectedTable().id() + " (" + grid.operation() + " at " + grid.index() + ")";
            case dev.flutter.netbeans.designer.command.EditTableGrid grid -> grid.expectedTable().id() + " (" + grid.operation() + " at " + grid.index() + ")";
            case SetProperty set -> set.widgetId() + "." + set.propertyName();
            case ResetProperty reset ->
                reset.widgetId() + "." + reset.propertyName();
            case PatchProperties patch -> patch.widgetId() + " ("
                    + patch.patches().size() + " properties)";
            case CreateEventHandler create -> create.widgetId() + "." + create.event();
            case CreateMenuAnchorBuilder create -> create.widgetId() + ".builder -> " + create.methodName();
            case RenameEventHandler rename -> rename.widgetId() + "." + rename.event();
            case RenameStateField rename -> rename.widgetId() + ": " + rename.fieldName() + " -> " + rename.newName();
            case CreateStateBinding create -> create.widgetId() + " -> " + create.fieldName();
            case RemoveStateBinding remove -> remove.widgetId() + " (state binding)";
            case BindPropertyToState bind -> bind.widgetId() + "." + bind.propertyName();
            case RemovePropertyStateBinding remove -> remove.widgetId() + "." + remove.propertyName();
            case AddWidget add -> add.destination().parentId() + "."
                    + add.destination().slotName() + "["
                    + add.destination().index() + "]";
            case RemoveWidget remove -> remove.widgetId().toString();
            case MoveWidget move -> move.widgetId() + " -> "
                    + move.destination().parentId() + "."
                    + move.destination().slotName() + "["
                    + move.destination().index() + "]";
            case ReplaceSlotChild replace -> replace.ownerId() + "."
                    + replace.slotName() + " (expected "
                    + replace.expectedChildId() + ")";
            case ClearSlotChildren clear -> clear.ownerId() + "."
                    + clear.slotName() + " (" + clear.expectedChildIds().size()
                    + " children)";
            case WrapWidget wrap -> wrap.widgetId().toString();
        };
    }
}
