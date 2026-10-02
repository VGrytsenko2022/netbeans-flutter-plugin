package io.github.vgrytsenko2022.plugin.run;

import org.netbeans.spi.project.ActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "io.github.vgrytsenko2022.plugin.run.DebugFlutterAction")
@ActionRegistration(displayName = "Debug Flutter Project", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 210, separatorAfter = 250)
public final class DebugFlutterAction extends FlutterProjectCommandAction {
    public DebugFlutterAction() {
        super("Debug Flutter Project", ActionProvider.COMMAND_DEBUG);
    }
}
