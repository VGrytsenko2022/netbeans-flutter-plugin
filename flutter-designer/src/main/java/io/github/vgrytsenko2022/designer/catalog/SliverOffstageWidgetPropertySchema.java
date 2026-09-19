package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 SliverOffstage constructor. */
public final class SliverOffstageWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverOffstage");
    public static final String DESCRIPTION = "Lays out the sliver while optionally hiding its painting, hit testing, semantics and scroll extent. "
            + "The child remains mounted; animations and focus are not disabled. Hidden content remains editable in the widget tree. "
            + "The optional sliver accepts sliver widgets only. An empty slot generates a zero-extent SliverToBoxAdapter "
            + "because Flutter 3.44.8 cannot lay out a null proxy sliver child.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
            new Field("offstage", "Offstage", "Omission uses true: the child is laid out but does not paint, receive pointer hits, "
                    + "contribute semantics or occupy scroll space. False shows it normally. The child remains mounted, "
                    + "so animations and keyboard focus continue. Edit hidden descendants through the widget tree."));
    public static List<PropertyDefinition> properties() {
        return List.of(new PropertyDefinition(new PropertyName("offstage"), DartParameter.named(0, false),
                List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()));
    }
    private SliverOffstageWidgetPropertySchema() {}
}

