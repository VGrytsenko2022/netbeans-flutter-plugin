package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;
import java.util.Optional;

/** Inserts one reviewed, user-owned menu opener and binds it in the same analyzed edit. */
public record CreateMenuAnchorBuilder(StableId widgetId, String methodName) implements DesignerCommand {
    public CreateMenuAnchorBuilder {
        Objects.requireNonNull(widgetId, "widgetId");
        methodName = new PropertyValue.DartObjectReferenceValue(Optional.empty(), methodName,
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                Optional.empty()).rootSymbol();
    }

    @Override public DesignerCommandKind kind() {
        return DesignerCommandKind.CREATE_MENU_ANCHOR_BUILDER;
    }

    /** Fixed template only: callers cannot supply executable Dart fragments. */
    String declaration() {
        return "Widget " + methodName + "(BuildContext context, MenuController controller, Widget? child) {\n"
                + "  return TextButton(\n"
                + "    onPressed: () {\n"
                + "      if (controller.isOpen) {\n"
                + "        controller.close();\n"
                + "      } else {\n"
                + "        controller.open();\n"
                + "      }\n"
                + "    },\n"
                + "    child: child ?? const Text('Menu'),\n"
                + "  );\n"
                + "}";
    }
}
