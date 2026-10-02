package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.TableGrid;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.Objects;

/** One snapshot-fenced DataTable edit, preserving rectangularity in history and pair-save. */
public record EditDataTableGrid(WidgetNode expectedTable, TableGrid.Operation operation,
        int index, int destination, StableId seed) implements DesignerCommand {
    public EditDataTableGrid {
        Objects.requireNonNull(expectedTable);Objects.requireNonNull(operation);Objects.requireNonNull(seed);
    }
    @Override public DesignerCommandKind kind(){return DesignerCommandKind.EDIT_DATA_TABLE_GRID;}
}
