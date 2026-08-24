package dev.flutter.netbeans.plugin.project;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.awt.Actions;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/** Project tree shown in the NetBeans Projects window. */
final class FlutterLogicalViewProvider implements LogicalViewProvider {
    private final FlutterProject project;
    private final FlutterProjectInformation information;

    FlutterLogicalViewProvider(
            FlutterProject project,
            FlutterProjectInformation information) {
        this.project = project;
        this.information = information;
    }

    @Override
    public Node createLogicalView() {
        Node folderNode = DataFolder.findFolder(project.getProjectDirectory()).getNodeDelegate();
        Lookup lookup = new ProxyLookup(
                Lookups.fixed(project, information),
                folderNode.getLookup());
        return new FlutterProjectNode(folderNode, lookup);
    }

    @Override
    public Node findPath(Node root, Object target) {
        FileObject targetFile = toFileObject(target);
        if (targetFile == null) {
            return null;
        }
        FileObject projectDirectory = project.getProjectDirectory();
        if (projectDirectory.equals(targetFile)) {
            return root;
        }
        String relativePath = FileUtil.getRelativePath(projectDirectory, targetFile);
        if (relativePath == null) {
            return null;
        }

        Node current = root;
        FileObject currentFile = projectDirectory;
        for (String segment : relativePath.split("/")) {
            currentFile = currentFile.getFileObject(segment);
            if (currentFile == null) {
                return null;
            }
            current = childFor(current, currentFile);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Node childFor(Node parent, FileObject expected) {
        for (Node child : parent.getChildren().getNodes(true)) {
            FileObject childFile = toFileObject(child);
            if (expected.equals(childFile)) {
                return child;
            }
        }
        return null;
    }

    private static FileObject toFileObject(Object target) {
        if (target instanceof FileObject fileObject) {
            return fileObject;
        }
        if (target instanceof DataObject dataObject) {
            return dataObject.getPrimaryFile();
        }
        if (target instanceof Node node) {
            FileObject fileObject = node.getLookup().lookup(FileObject.class);
            if (fileObject != null) {
                return fileObject;
            }
            DataObject dataObject = node.getLookup().lookup(DataObject.class);
            return dataObject == null ? null : dataObject.getPrimaryFile();
        }
        return null;
    }

    private final class FlutterProjectNode extends FilterNode {
        FlutterProjectNode(Node original, Lookup lookup) {
            super(original, new FilterNode.Children(original), lookup);
        }

        @Override
        public String getName() {
            return information.getName();
        }

        @Override
        public String getDisplayName() {
            return information.getDisplayName();
        }

        @Override
        public String getShortDescription() {
            return "Flutter project at " + project.getProjectDirectory().getPath();
        }

        @Override
        public Image getIcon(int type) {
            return FlutterProjectFactory.projectImage();
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public Action[] getActions(boolean context) {
            List<Action> actions = new ArrayList<>();
            actions.add(CommonProjectActions.newFileAction());
            actions.add(null);
            actions.add(projectCommand(
                    FlutterProjectActionProvider.COMMAND_SELECT_TARGET,
                    "Select Flutter Run Target..."));
            actions.add(projectCommand(
                    FlutterProjectActionProvider.COMMAND_LAUNCH_EMULATOR,
                    "Launch Mobile Emulator..."));
            Action deviceManager = Actions.forID(
                    "Window",
                    "dev.flutter.netbeans.plugin.device.FlutterDeviceManagerTopComponent");
            if (deviceManager != null) {
                actions.add(deviceManager);
            }
            actions.add(null);
            actions.add(projectCommand(
                    FlutterProjectActionProvider.COMMAND_PUB_GET,
                    "Flutter Pub Get"));
            actions.add(projectCommand(
                    FlutterProjectActionProvider.COMMAND_ANALYZE,
                    "Flutter Analyze"));
            actions.add(projectCommand(org.netbeans.spi.project.ActionProvider.COMMAND_TEST,
                    "Flutter Test"));
            actions.add(null);
            actions.add(projectCommand(org.netbeans.spi.project.ActionProvider.COMMAND_RUN, "Run"));
            actions.add(projectCommand(org.netbeans.spi.project.ActionProvider.COMMAND_DEBUG, "Debug"));
            actions.add(projectCommand(
                    FlutterProjectActionProvider.COMMAND_HOT_RELOAD,
                    "Hot Reload"));
            actions.add(projectCommand(
                    FlutterProjectActionProvider.COMMAND_HOT_RESTART,
                    "Hot Restart"));
            actions.add(projectCommand(FlutterProjectActionProvider.COMMAND_STOP, "Stop"));
            actions.add(null);
            actions.add(CommonProjectActions.setAsMainProjectAction());
            actions.add(null);
            actions.add(CommonProjectActions.copyProjectAction());
            actions.add(CommonProjectActions.moveProjectAction());
            actions.add(CommonProjectActions.renameProjectAction());
            actions.add(CommonProjectActions.deleteProjectAction());
            actions.add(null);
            actions.add(CommonProjectActions.closeProjectAction());
            return actions.toArray(Action[]::new);
        }

        private Action projectCommand(String command, String displayName) {
            return ProjectSensitiveActions.projectCommandAction(command, displayName, null);
        }
    }
}
