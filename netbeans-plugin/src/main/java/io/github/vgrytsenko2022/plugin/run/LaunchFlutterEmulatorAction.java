package io.github.vgrytsenko2022.plugin.run;

import io.github.vgrytsenko2022.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "io.github.vgrytsenko2022.plugin.run.LaunchFlutterEmulatorAction")
@ActionRegistration(displayName = "Launch Mobile Emulator...", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 120)
public final class LaunchFlutterEmulatorAction extends FlutterProjectCommandAction {
    public LaunchFlutterEmulatorAction() {
        super("Launch Mobile Emulator...", FlutterProjectActionProvider.COMMAND_LAUNCH_EMULATOR);
    }
}
