package io.github.vgrytsenko2022.plugin.run;

import io.github.vgrytsenko2022.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "io.github.vgrytsenko2022.plugin.run.SelectFlutterTargetAction")
@ActionRegistration(displayName = "Select Run Target...", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 100, separatorAfter = 150)
public final class SelectFlutterTargetAction extends FlutterProjectCommandAction {
    public SelectFlutterTargetAction() {
        super("Select Run Target...", FlutterProjectActionProvider.COMMAND_SELECT_TARGET);
    }
}
