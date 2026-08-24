package dev.flutter.netbeans.plugin.tooling;

import org.netbeans.spi.project.ActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.tooling.FlutterTestAction")
@ActionRegistration(displayName = "Flutter Test", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 480)
public final class FlutterTestAction extends FlutterToolingCommandAction {
    public FlutterTestAction() {
        super("Flutter Test", ActionProvider.COMMAND_TEST);
    }
}
