package io.github.vgrytsenko2022.plugin.project;

import io.github.vgrytsenko2022.project.FlutterProjectPlatform;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle.Messages;

/** Selection UI and filesystem detection for adding missing Flutter platforms. */
@Messages({
    "TTL_AddFlutterPlatforms=Add Flutter Platforms",
    "CTL_AddPlatformsProjectAction=Add Flutter Platforms...",
    "# {0} - absolute Flutter project path",
    "LBL_AddPlatformsProject=Project: {0}",
    "# {0} - comma-separated existing platform display names",
    "LBL_AddPlatformsPresent=Existing or occupied platform paths: {0}",
    "LBL_AddPlatformsNonePresent=Existing or occupied platform paths: none",
    "LBL_SelectMissingPlatforms=Select one or more platforms to add:",
    "LBL_PlatformAndroid=&Android",
    "LBL_PlatformIos=&iOS",
    "LBL_PlatformWeb=&Web",
    "LBL_PlatformWindows=Wi&ndows",
    "LBL_PlatformMacos=&macOS",
    "LBL_PlatformLinux=&Linux",
    "ACN_AddPlatformsPanel=Missing Flutter platforms",
    "ACD_AddPlatformsPanel=Select missing platform scaffolding to generate in the current Flutter project.",
    "# {0} - platform display name",
    "ACD_AddPlatformChoice=Generate the missing {0} platform files in this Flutter project.",
    "# {0} - absolute Flutter project path",
    "MSG_AllPlatformsPresent=Cannot add platforms to {0}: all supported platform paths already exist or are occupied.",
    "# {0} - absolute Flutter project path",
    "MSG_PlatformSelectionChanged=Cannot add the selected platforms to {0}: their paths appeared while the dialog was open. Open Add Flutter Platforms again to refresh the selection.",
    "# {0} - Flutter project path",
    "# {1} - concrete unavailable reason",
    "MSG_AddPlatformsUnavailable=Cannot add Flutter platforms to {0}: {1}."
})
final class FlutterAddPlatformsPanel extends JPanel {
    private final Map<FlutterProjectPlatform, JCheckBox> choices =
            new EnumMap<>(FlutterProjectPlatform.class);
    private DialogDescriptor descriptor;

    private FlutterAddPlatformsPanel(
            Path projectRoot,
            Set<FlutterProjectPlatform> present,
            Set<FlutterProjectPlatform> missing) {
        super(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.add(new JLabel(Bundle.LBL_AddPlatformsProject(projectRoot.toString())));
        summary.add(Box.createVerticalStrut(4));
        summary.add(new JLabel(present.isEmpty()
                ? Bundle.LBL_AddPlatformsNonePresent()
                : Bundle.LBL_AddPlatformsPresent(displayNames(present))));
        summary.add(Box.createVerticalStrut(8));
        summary.add(new JLabel(Bundle.LBL_SelectMissingPlatforms()));
        add(summary, BorderLayout.NORTH);

        JPanel platformChoices = new JPanel(new GridLayout(0, 2, 16, 6));
        for (FlutterProjectPlatform platform : FlutterProjectPlatform.ordered(missing)) {
            JCheckBox choice = new JCheckBox();
            Mnemonics.setLocalizedText(choice, localizedPlatformLabel(platform));
            choice.getAccessibleContext().setAccessibleName(
                    choice.getText());
            choice.getAccessibleContext().setAccessibleDescription(
                    Bundle.ACD_AddPlatformChoice(choice.getText()));
            choice.addActionListener(ignored -> updateValidity());
            choices.put(platform, choice);
            platformChoices.add(choice);
        }
        add(platformChoices, BorderLayout.CENTER);

        getAccessibleContext().setAccessibleName(Bundle.ACN_AddPlatformsPanel());
        getAccessibleContext().setAccessibleDescription(Bundle.ACD_AddPlatformsPanel());
    }

    static Optional<Set<FlutterProjectPlatform>> choose(Path projectRoot) {
        Path root = normalizedProjectRoot(projectRoot);
        Set<FlutterProjectPlatform> present = detectPresentPlatforms(root);
        Set<FlutterProjectPlatform> missing = missingPlatforms(present);
        if (missing.isEmpty()) {
            NotifyDescriptor message = new NotifyDescriptor.Message(
                    Bundle.MSG_AllPlatformsPresent(root.toString()),
                    NotifyDescriptor.INFORMATION_MESSAGE);
            message.setTitle(Bundle.TTL_AddFlutterPlatforms());
            DialogDisplayer.getDefault().notify(message);
            return Optional.empty();
        }

        FlutterAddPlatformsPanel panel = new FlutterAddPlatformsPanel(root, present, missing);
        DialogDescriptor dialog = new DialogDescriptor(
                panel,
                Bundle.TTL_AddFlutterPlatforms(),
                true,
                DialogDescriptor.OK_CANCEL_OPTION,
                DialogDescriptor.OK_OPTION,
                null);
        panel.attach(dialog);
        Object result = DialogDisplayer.getDefault().notify(dialog);
        return result == DialogDescriptor.OK_OPTION
                ? Optional.of(panel.selectedPlatforms())
                : Optional.empty();
    }

    static Set<FlutterProjectPlatform> detectPresentPlatforms(Path projectRoot) {
        Path root = normalizedProjectRoot(projectRoot);
        EnumSet<FlutterProjectPlatform> present = EnumSet.noneOf(FlutterProjectPlatform.class);
        for (FlutterProjectPlatform platform : FlutterProjectPlatform.all()) {
            Path platformPath = root.resolve(platform.id());
            // Any occupied canonical path is treated as present. This prevents
            // the add operation from targeting a user file or symbolic link.
            if (Files.exists(platformPath, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(platformPath)) {
                present.add(platform);
            }
        }
        return Collections.unmodifiableSet(present);
    }

    static Set<FlutterProjectPlatform> stillMissingSelection(
            Path projectRoot,
            Set<FlutterProjectPlatform> requested) {
        Set<FlutterProjectPlatform> selected = FlutterProjectPlatform.copyOf(requested);
        EnumSet<FlutterProjectPlatform> result = EnumSet.copyOf(selected);
        result.removeAll(detectPresentPlatforms(projectRoot));
        return Collections.unmodifiableSet(result);
    }

    private static Set<FlutterProjectPlatform> missingPlatforms(
            Set<FlutterProjectPlatform> present) {
        EnumSet<FlutterProjectPlatform> missing = EnumSet.allOf(FlutterProjectPlatform.class);
        missing.removeAll(present);
        return Collections.unmodifiableSet(missing);
    }

    private void attach(DialogDescriptor descriptor) {
        this.descriptor = descriptor;
        updateValidity();
    }

    private void updateValidity() {
        if (descriptor != null) {
            descriptor.setValid(choices.values().stream().anyMatch(JCheckBox::isSelected));
        }
    }

    private Set<FlutterProjectPlatform> selectedPlatforms() {
        EnumSet<FlutterProjectPlatform> selected = EnumSet.noneOf(FlutterProjectPlatform.class);
        choices.forEach((platform, choice) -> {
            if (choice.isSelected()) {
                selected.add(platform);
            }
        });
        return FlutterProjectPlatform.copyOf(selected);
    }

    private static Path normalizedProjectRoot(Path projectRoot) {
        Path root = java.util.Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException(
                    "Flutter project root is not a safe directory: " + root);
        }
        return root;
    }

    private static String displayNames(Set<FlutterProjectPlatform> platforms) {
        return FlutterProjectPlatform.ordered(platforms).stream()
                .map(FlutterProjectPlatform::displayName)
                .collect(Collectors.joining(", "));
    }

    private static String localizedPlatformLabel(FlutterProjectPlatform platform) {
        return switch (platform) {
            case ANDROID -> Bundle.LBL_PlatformAndroid();
            case IOS -> Bundle.LBL_PlatformIos();
            case WEB -> Bundle.LBL_PlatformWeb();
            case WINDOWS -> Bundle.LBL_PlatformWindows();
            case MACOS -> Bundle.LBL_PlatformMacos();
            case LINUX -> Bundle.LBL_PlatformLinux();
        };
    }
}
