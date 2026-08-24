package dev.flutter.netbeans.plugin.run;

import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.run.StopFlutterAction")
@ActionRegistration(displayName = "Stop Flutter Application", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 400, separatorBefore = 350)
public final class StopFlutterAction extends FlutterProjectCommandAction {
    public StopFlutterAction() {
        super("Stop Flutter Application", FlutterProjectActionProvider.COMMAND_STOP);
    }
}
