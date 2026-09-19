package io.github.vgrytsenko2022.plugin.run;

import io.github.vgrytsenko2022.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "io.github.vgrytsenko2022.plugin.run.HotReloadFlutterAction")
@ActionRegistration(displayName = "Hot Reload", lazy = false)
@ActionReferences({
    @ActionReference(path = "Menu/Flutter", position = 300),
    @ActionReference(path = "Toolbars/Build", position = 360, separatorBefore = 355)
})
public final class HotReloadFlutterAction extends FlutterProjectCommandAction {
    static final String ICON_BASE =
            "io/github/vgrytsenko2022/plugin/run/hotReload.svg";

    public HotReloadFlutterAction() {
        super("Hot Reload", FlutterProjectActionProvider.COMMAND_HOT_RELOAD, ICON_BASE);
    }
}
