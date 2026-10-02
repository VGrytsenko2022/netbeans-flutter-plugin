package io.github.vgrytsenko2022.plugin.run;

import org.netbeans.spi.project.ActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "io.github.vgrytsenko2022.plugin.run.RunFlutterAction")
@ActionRegistration(displayName = "Run Flutter Project", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 200)
public final class RunFlutterAction extends FlutterProjectCommandAction {
    public RunFlutterAction() {
        super("Run Flutter Project", ActionProvider.COMMAND_RUN);
    }
}
