package dev.flutter.netbeans.plugin.run;

import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.run.HotReloadFlutterAction")
@ActionRegistration(displayName = "Hot Reload", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 300)
public final class HotReloadFlutterAction extends FlutterProjectCommandAction {
    public HotReloadFlutterAction() {
        super("Hot Reload", FlutterProjectActionProvider.COMMAND_HOT_RELOAD);
    }
}
