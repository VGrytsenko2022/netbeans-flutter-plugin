package io.github.vgrytsenko2022.plugin.theme;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

@ActionID(
        category = "Flutter",
        id = "io.github.vgrytsenko2022.plugin.theme.EditFlutterThemesAction")
@ActionRegistration(displayName = "#CTL_EditFlutterThemesAction", lazy = false)
@ActionReferences({
    @ActionReference(path = "Menu/Flutter", position = 440, separatorBefore = 430),
    @ActionReference(path = "Projects/Actions", position = 440)
})
@Messages({
    "CTL_EditFlutterThemesAction=Edit Flutter Themes...",
    "TTL_CannotEditFlutterThemes=Cannot edit Flutter project themes",
    "# {0} - concrete project-resolution reason",
    "MSG_CannotResolveFlutterThemeProject=Edit Flutter project themes failed. Reason: {0}."
})
public final class EditFlutterThemesAction extends AbstractAction
        implements ContextAwareAction {
    private final Lookup context;

    public EditFlutterThemesAction() {
        this(Utilities.actionsGlobalContext());
    }

    private EditFlutterThemesAction(Lookup context) {
        super(Bundle.CTL_EditFlutterThemesAction());
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        FlutterThemeProjectResolver.Resolution resolution =
                new FlutterThemeProjectResolver(context).resolve();
        if (!resolution.resolved()) {
            NotifyDescriptor message = new NotifyDescriptor.Message(
                    Bundle.MSG_CannotResolveFlutterThemeProject(resolution.reason()),
                    NotifyDescriptor.ERROR_MESSAGE);
            message.setTitle(Bundle.TTL_CannotEditFlutterThemes());
            DialogDisplayer.getDefault().notify(message);
            return;
        }
        FlutterThemesEditorOpener.openProject(resolution.projectRoot());
    }

    @Override
    public boolean isEnabled() {
        return new FlutterThemeProjectResolver(context).resolve().resolved();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new EditFlutterThemesAction(actionContext);
    }
}
