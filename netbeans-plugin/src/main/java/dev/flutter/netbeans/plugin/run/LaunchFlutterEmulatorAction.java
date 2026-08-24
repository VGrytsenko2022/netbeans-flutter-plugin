package dev.flutter.netbeans.plugin.run;

import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.run.LaunchFlutterEmulatorAction")
@ActionRegistration(displayName = "Launch Mobile Emulator...", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 120)
public final class LaunchFlutterEmulatorAction extends FlutterProjectCommandAction {
    public LaunchFlutterEmulatorAction() {
        super("Launch Mobile Emulator...", FlutterProjectActionProvider.COMMAND_LAUNCH_EMULATOR);
    }
}
