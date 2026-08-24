package dev.flutter.netbeans.plugin.tooling;

import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.tooling.FlutterTestAtCaretAction")
@ActionRegistration(displayName = "Test at Caret", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 500)
public final class FlutterTestAtCaretAction extends FlutterToolingCommandAction {
    public FlutterTestAtCaretAction() {
        super("Test at Caret", FlutterProjectActionProvider.COMMAND_TEST_AT_CARET);
    }
}
