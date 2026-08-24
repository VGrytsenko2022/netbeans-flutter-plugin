package dev.flutter.netbeans.plugin.tooling;

import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.tooling.FlutterTestFileAction")
@ActionRegistration(displayName = "Test Current Dart File", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 490)
public final class FlutterTestFileAction extends FlutterToolingCommandAction {
    public FlutterTestFileAction() {
        super("Test Current Dart File", FlutterProjectActionProvider.COMMAND_TEST_FILE);
    }
}
