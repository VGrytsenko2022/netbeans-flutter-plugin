package dev.flutter.netbeans.plugin.tooling;

import dev.flutter.netbeans.plugin.project.FlutterProject;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.project.ActionProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/** Context-aware base for one-shot commands in the top-level Flutter menu. */
abstract class FlutterToolingCommandAction extends AbstractAction
        implements LookupListener, ChangeListener, PropertyChangeListener {
    private final String command;
    private final Lookup.Result<Project> contextProjects;
    private final Lookup.Result<FileObject> contextFiles;
    private final Lookup.Result<DataObject> contextDataObjects;
    private volatile FlutterToolingController observedController;

    FlutterToolingCommandAction(String displayName, String command) {
        super(displayName);
        this.command = command;
        contextProjects = Utilities.actionsGlobalContext().lookupResult(Project.class);
        contextProjects.addLookupListener(this);
        contextFiles = Utilities.actionsGlobalContext().lookupResult(FileObject.class);
        contextFiles.addLookupListener(this);
        contextDataObjects = Utilities.actionsGlobalContext().lookupResult(DataObject.class);
        contextDataObjects.addLookupListener(this);
        OpenProjects.getDefault().addPropertyChangeListener(this);
        refresh();
    }

    @Override
    public final void actionPerformed(ActionEvent event) {
        FlutterProject project = activeProject();
        if (project == null) {
            showUnavailable("no open Flutter project is active");
            return;
        }
        ActionProvider actions = project.getLookup().lookup(ActionProvider.class);
        if (actions == null || !actions.isActionEnabled(command, Utilities.actionsGlobalContext())) {
            showUnavailable("Flutter tooling is busy or unavailable for "
                    + project.getProjectDirectory().getPath());
            return;
        }
        actions.invokeAction(command, Utilities.actionsGlobalContext());
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
        FlutterToolingController controller = project == null
                ? null
                : project.getLookup().lookup(FlutterToolingController.class);
        FlutterToolingController previous = observedController;
        if (previous != controller) {
            if (previous != null) {
                previous.removeChangeListener(this);
            }
            observedController = controller;
            if (controller != null) {
                controller.addChangeListener(this);
            }
        }
        ActionProvider actions = project == null
                ? null
                : project.getLookup().lookup(ActionProvider.class);
        setEnabled(controller != null && actions != null
                && actions.isActionEnabled(command, Utilities.actionsGlobalContext()));
    }

    private FlutterProject activeProject() {
        for (Project project : contextProjects.allInstances()) {
            if (project instanceof FlutterProject flutterProject) {
                return flutterProject;
            }
        }
        Lookup globalContext = Utilities.actionsGlobalContext();
        FileObject activeFile = globalContext.lookup(FileObject.class);
        if (activeFile == null) {
            DataObject dataObject = globalContext.lookup(DataObject.class);
            activeFile = dataObject == null ? null : dataObject.getPrimaryFile();
        }
        if (activeFile != null) {
            Project owner = FileOwnerQuery.getOwner(activeFile);
            if (owner instanceof FlutterProject flutterProject) {
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

    private static void showUnavailable(String reason) {
        NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                "Cannot perform the Flutter tooling action: " + reason + ".",
                NotifyDescriptor.WARNING_MESSAGE);
        descriptor.setTitle("Flutter action unavailable");
        DialogDisplayer.getDefault().notify(descriptor);
    }
}
