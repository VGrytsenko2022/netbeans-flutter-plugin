package dev.flutter.netbeans.plugin.run;

import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.run.HotRestartFlutterAction")
@ActionRegistration(displayName = "Hot Restart", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 310)
public final class HotRestartFlutterAction extends FlutterProjectCommandAction {
    public HotRestartFlutterAction() {
        super("Hot Restart", FlutterProjectActionProvider.COMMAND_HOT_RESTART);
    }
}
