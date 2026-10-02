package io.github.vgrytsenko2022.plugin.tooling;

import io.github.vgrytsenko2022.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Flutter", id = "io.github.vgrytsenko2022.plugin.tooling.AddFlutterPlatformsAction")
@ActionRegistration(displayName = "#CTL_AddFlutterPlatformsAction", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 455)
@Messages({
    "CTL_AddFlutterPlatformsAction=Add Flutter Platforms...",
    "LBL_AddFlutterPlatformsOperation=Add Platforms",
    "# {0} - absolute Flutter project path",
    "# {1} - concrete unavailable reason",
    "MSG_AddPlatformsControllerUnavailable=Cannot add Flutter platforms to {0}: {1}.",
    "# {0} - comma-separated missing platform display names",
    "MSG_AddPlatformsMissingDirectories=Flutter exited successfully but did not create the selected platform directories: {0}",
    "TTL_AddPlatformsIncomplete=Flutter Add Platforms incomplete",
    "TTL_AddPlatformsCannotStart=Cannot start Flutter Add Platforms",
    "# {0} - absolute Flutter project path",
    "# {1} - concrete precondition failure",
    "MSG_AddPlatformsCannotStart=Cannot start Flutter Add Platforms for {0}: {1}.",
    "# {0} - absolute Flutter project path",
    "# {1} - concrete verification failure",
    "MSG_AddPlatformsIncomplete=Cannot finish Flutter Add Platforms for {0}: {1}. Check whether the selected platforms are enabled in the configured Flutter SDK, then see the Flutter Add Platforms Output tab for details."
})
public final class AddFlutterPlatformsAction extends FlutterToolingCommandAction {
    public AddFlutterPlatformsAction() {
        super(Bundle.CTL_AddFlutterPlatformsAction(),
                FlutterProjectActionProvider.COMMAND_ADD_PLATFORMS);
    }
}
