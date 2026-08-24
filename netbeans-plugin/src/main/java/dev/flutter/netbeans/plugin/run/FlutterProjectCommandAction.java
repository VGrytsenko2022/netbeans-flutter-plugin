package dev.flutter.netbeans.plugin.run;

import dev.flutter.netbeans.plugin.project.FlutterProject;
import dev.flutter.netbeans.plugin.project.FlutterRunController;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/** Context-aware base for actions in the top-level Flutter menu. */
abstract class FlutterProjectCommandAction extends AbstractAction
        implements LookupListener, ChangeListener, PropertyChangeListener {
    private final String command;
    private final Lookup.Result<Project> contextProjects;
    private volatile FlutterRunController observedController;

    FlutterProjectCommandAction(String displayName, String command) {
        super(displayName);
        this.command = command;
        contextProjects = Utilities.actionsGlobalContext().lookupResult(Project.class);
        contextProjects.addLookupListener(this);
        OpenProjects.getDefault().addPropertyChangeListener(this);
        refresh();
    }

    @Override
    public final void actionPerformed(ActionEvent event) {
        FlutterProject project = activeProject();
        if (project == null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
                    "Cannot perform the Flutter action: no open Flutter project is active. "
                    + "Select a Flutter project in Projects or set it as the main project.",
                    NotifyDescriptor.WARNING_MESSAGE));
            return;
        }
        FlutterRunController controller = project.getLookup().lookup(FlutterRunController.class);
        if (controller == null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
                    "Cannot perform the Flutter action for "
                    + project.getProjectDirectory().getPath()
                    + ": run support is unavailable.",
                    NotifyDescriptor.ERROR_MESSAGE));
            return;
        }
        controller.invoke(command);
    }

    @Override
    public final void resultChanged(LookupEvent event) {
        refresh();
    }

    @Override
    public final void stateChanged(ChangeEvent event) {
        refresh();
    }

    @Override
    public final void propertyChange(PropertyChangeEvent event) {
        refresh();
    }

    private void refresh() {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(this::refresh);
            return;
        }
        FlutterProject project = activeProject();
        FlutterRunController controller = project == null
                ? null
                : project.getLookup().lookup(FlutterRunController.class);
        FlutterRunController previous = observedController;
        if (previous != controller) {
            if (previous != null) {
                previous.removeChangeListener(this);
            }
            observedController = controller;
            if (controller != null) {
                controller.addChangeListener(this);
            }
        }
        setEnabled(controller != null && controller.isCommandEnabled(command));
    }

    private FlutterProject activeProject() {
        for (Project project : contextProjects.allInstances()) {
            if (project instanceof FlutterProject flutterProject) {
                return flutterProject;
            }
        }
        Project main = OpenProjects.getDefault().getMainProject();
        if (main instanceof FlutterProject flutterProject) {
            return flutterProject;
        }
        FlutterProject only = null;
        for (Project project : OpenProjects.getDefault().getOpenProjects()) {
            if (project instanceof FlutterProject flutterProject) {
                if (only != null) {
                    return null;
                }
                only = flutterProject;
            }
        }
        return only;
    }
}
