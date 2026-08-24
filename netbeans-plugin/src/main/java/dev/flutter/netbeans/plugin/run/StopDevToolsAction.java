package dev.flutter.netbeans.plugin.run;

import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.run.StopDevToolsAction")
@ActionRegistration(displayName = "Stop DevTools", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 330)
public final class StopDevToolsAction extends FlutterProjectCommandAction {
    public StopDevToolsAction() {
        super("Stop DevTools", FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS);
    }
}
