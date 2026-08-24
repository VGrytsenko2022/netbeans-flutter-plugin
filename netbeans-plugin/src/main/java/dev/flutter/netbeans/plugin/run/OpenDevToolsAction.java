package dev.flutter.netbeans.plugin.run;

import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.run.OpenDevToolsAction")
@ActionRegistration(displayName = "Open DevTools", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 320)
public final class OpenDevToolsAction extends FlutterProjectCommandAction {
    public OpenDevToolsAction() {
        super("Open DevTools", FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
    }
}
