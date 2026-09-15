package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.TableGrid;
import dev.flutter.netbeans.designer.model.*;
import java.util.Objects;

/** One revision-fenced, undoable rectangular edit. Never deletes unrelated columns implicitly. */
public record EditTableGrid(WidgetNode expectedTable, TableGrid.Operation operation,
        int index, int destination, StableId seed) implements DesignerCommand {
    public EditTableGrid {
        Objects.requireNonNull(expectedTable); Objects.requireNonNull(operation); Objects.requireNonNull(seed);
    }
    @Override public DesignerCommandKind kind() { return DesignerCommandKind.EDIT_TABLE_GRID; }
}
