package dev.flutter.netbeans.designer.command;

/**
 * One immutable semantic Flutter Designer edit.
 *
 * <p>Commands contain intent only. They perform no I/O and acquire no write
 * authority. {@link DesignerCommandSession#apply(DesignerCommand)} is the
 * bounded validation and candidate-construction boundary.</p>
 */
public sealed interface DesignerCommand permits
        AddWidget,
        RemoveWidget,
        MoveWidget,
        ReplaceSlotChild,
        ClearSlotChildren,
        WrapWidget,
        SetProperty,
        ResetProperty {

    DesignerCommandKind kind();
}
