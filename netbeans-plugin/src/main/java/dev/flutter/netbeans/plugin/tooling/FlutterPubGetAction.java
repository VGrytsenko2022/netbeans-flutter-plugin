package dev.flutter.netbeans.plugin.tooling;

import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "dev.flutter.netbeans.plugin.tooling.FlutterPubGetAction")
@ActionRegistration(displayName = "Flutter Pub Get", lazy = false)
@ActionReference(path = "Menu/Flutter", position = 460, separatorBefore = 450)
public final class FlutterPubGetAction extends FlutterToolingCommandAction {
    public FlutterPubGetAction() {
        super("Flutter Pub Get", FlutterProjectActionProvider.COMMAND_PUB_GET);
    }
}
