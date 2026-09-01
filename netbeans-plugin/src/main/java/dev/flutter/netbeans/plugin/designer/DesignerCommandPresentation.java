package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.command.AddWidget;
import dev.flutter.netbeans.designer.command.ClearSlotChildren;
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
            case REMOVE_WIDGET -> "Remove Flutter Widget";
            case MOVE_WIDGET -> "Move Flutter Widget";
            case REPLACE_SLOT_CHILD -> "Replace Flutter Slot Child";
            case CLEAR_SLOT_CHILDREN -> "Clear Flutter Slot Children";
            case WRAP_WIDGET -> "Wrap Flutter Widget";
            case SET_PROPERTY -> "Set Flutter Property";
            case RESET_PROPERTY -> "Reset Flutter Property";
            case PATCH_PROPERTIES -> "Update Flutter Properties";
        };
    }

    static String operation(DesignerCommand command) {
        Objects.requireNonNull(command, "command");
        return switch (command.kind()) {
            case ADD_WIDGET -> "Add Flutter widget";
            case REMOVE_WIDGET -> "Remove Flutter widget";
            case MOVE_WIDGET -> "Move Flutter widget";
            case REPLACE_SLOT_CHILD -> "Replace Flutter slot child";
            case CLEAR_SLOT_CHILDREN -> "Clear Flutter slot children";
            case WRAP_WIDGET -> "Wrap Flutter widget";
            case SET_PROPERTY -> "Set Flutter property";
            case RESET_PROPERTY -> "Reset Flutter property";
            case PATCH_PROPERTIES -> "Update Flutter properties";
        };
    }

    static String target(DesignerCommand command) {
        Objects.requireNonNull(command, "command");
        return switch (command) {
            case SetProperty set -> set.widgetId() + "." + set.propertyName();
            case ResetProperty reset ->
                reset.widgetId() + "." + reset.propertyName();
            case PatchProperties patch -> patch.widgetId() + " ("
                    + patch.patches().size() + " properties)";
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
