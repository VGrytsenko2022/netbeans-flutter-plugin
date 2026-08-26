package dev.flutter.netbeans.plugin.run;

import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.run.HotRestartFlutterAction")
@ActionRegistration(displayName = "Hot Restart", lazy = false)
@ActionReferences({
    @ActionReference(path = "Menu/Flutter", position = 310),
    @ActionReference(path = "Toolbars/Build", position = 370)
})
public final class HotRestartFlutterAction extends FlutterProjectCommandAction {
    static final String ICON_BASE =
            "dev/flutter/netbeans/plugin/run/hotRestart.svg";

    public HotRestartFlutterAction() {
        super("Hot Restart", FlutterProjectActionProvider.COMMAND_HOT_RESTART, ICON_BASE);
    }
}
