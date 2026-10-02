package io.github.vgrytsenko2022.plugin.tooling;

import io.github.vgrytsenko2022.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "io.github.vgrytsenko2022.plugin.tooling.FlutterAnalyzeAction")
@ActionRegistration(displayName = "Flutter Analyze", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 470)
public final class FlutterAnalyzeAction extends FlutterToolingCommandAction {
    public FlutterAnalyzeAction() {
        super("Flutter Analyze", FlutterProjectActionProvider.COMMAND_ANALYZE);
    }
}
