package dev.flutter.netbeans.plugin.project.wizard;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.project.FlutterProjectPlatform;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;
import org.openide.awt.Mnemonics;
import org.openide.WizardDescriptor;

class FlutterProjectPlatformsWizardTest {
    @Test
    void defaultsToAllAndAppliesCanonicalPresets() throws Exception {
        AtomicInteger changes = new AtomicInteger();
        FlutterProjectPlatformsVisual visual =
                onEdt(() -> new FlutterProjectPlatformsVisual(changes::incrementAndGet));

        onEdt(() -> visual.setSelectedPlatforms(FlutterProjectPlatform.all()));
        assertEquals(FlutterProjectPlatform.all(), onEdt(visual::selectedPlatforms));
        assertEquals(FlutterProjectPlatformsVisual.Preset.ALL,
                onEdt(visual::selectedPreset));

        onEdt(() -> visual.selectPreset(FlutterProjectPlatformsVisual.Preset.MOBILE));
        assertEquals(Set.of(
                FlutterProjectPlatform.ANDROID,
                FlutterProjectPlatform.IOS), onEdt(visual::selectedPlatforms));

        onEdt(() -> visual.selectPreset(FlutterProjectPlatformsVisual.Preset.DESKTOP));
        assertEquals(Set.of(
                FlutterProjectPlatform.WINDOWS,
                FlutterProjectPlatform.MACOS,
                FlutterProjectPlatform.LINUX), onEdt(visual::selectedPlatforms));

        onEdt(() -> visual.selectPreset(FlutterProjectPlatformsVisual.Preset.WEB));
        assertEquals(Set.of(FlutterProjectPlatform.WEB),
                onEdt(visual::selectedPlatforms));
        assertTrue(changes.get() >= 4);
        assertNotNull(onEdt(() -> visual.getAccessibleContext().getAccessibleName()));
        assertNotNull(onEdt(() -> visual.getAccessibleContext().getAccessibleDescription()));
    }

    @Test
    void manualSelectionBecomesCustomAndEmptySelectionIsInvalid() throws Exception {
        FlutterProjectPlatformsWizardPanel panel =
                onEdt(FlutterProjectPlatformsWizardPanel::new);
        WizardDescriptor wizard = descriptor(panel);

        onEdt(() -> panel.readSettings(wizard));
        FlutterProjectPlatformsVisual visual =
                (FlutterProjectPlatformsVisual) onEdt(panel::getComponent);
        onEdt(() -> visual.setPlatformSelected(FlutterProjectPlatform.IOS, false));

        assertEquals(FlutterProjectPlatformsVisual.Preset.CUSTOM,
                onEdt(visual::selectedPreset));
        assertTrue(onEdt(panel::isValid));

        onEdt(() -> visual.setSelectedPlatforms(Set.of()));
        assertFalse(onEdt(panel::isValid));
        assertTrue(String.valueOf(wizard.getProperty(
                WizardDescriptor.PROP_ERROR_MESSAGE)).contains("at least one"));

        onEdt(() -> panel.storeSettings(wizard));
        assertEquals(Set.of(), wizard.getProperty(
                FlutterProjectPlatformsWizardPanel.PROP_PLATFORMS));
    }

    @Test
    void platformControlsHaveUniqueMnemonicsLocalizedAccessibleNamesAndBoundedWidth()
            throws Exception {
        FlutterProjectPlatformsVisual visual =
                onEdt(() -> new FlutterProjectPlatformsVisual(() -> { }));

        PlatformUiSnapshot snapshot = onEdt(() -> snapshot(visual));

        assertEquals(6, snapshot.checkboxCount());
        assertEquals(snapshot.mnemonics().size(),
                new HashSet<>(snapshot.mnemonics()).size(),
                "Platform controls must not compete for the same mnemonic");
        assertTrue(snapshot.mnemonics().stream().allMatch(mnemonic -> mnemonic != 0),
                "Every platform control must expose a mnemonic");
        assertEquals(Set.of(
                localizedControlText(Bundle.LBL_PlatformAndroid()),
                localizedControlText(Bundle.LBL_PlatformIos()),
                localizedControlText(Bundle.LBL_PlatformWeb()),
                localizedControlText(Bundle.LBL_PlatformWindows()),
                localizedControlText(Bundle.LBL_PlatformMacos()),
                localizedControlText(Bundle.LBL_PlatformLinux())),
                snapshot.accessibleNames());
        assertTrue(snapshot.preferredWidth() <= 560,
                "The platform hint must not make the wizard excessively wide: "
                + snapshot.preferredWidth());
    }

    @Test
    void platformSelectionPersistsAcrossRealWizardBackAndNext() throws Exception {
        FlutterProjectPlatformsWizardPanel platforms =
                onEdt(FlutterProjectPlatformsWizardPanel::new);
        FlutterProjectPlatformsWizardPanel followingPanel =
                onEdt(FlutterProjectPlatformsWizardPanel::new);
        WizardDescriptor wizard = initializedDescriptor(platforms, followingPanel);
        FlutterProjectPlatformsVisual visual =
                (FlutterProjectPlatformsVisual) onEdt(platforms::getComponent);
        Set<FlutterProjectPlatform> selected = Set.of(
                FlutterProjectPlatform.ANDROID,
                FlutterProjectPlatform.WEB);

        onEdt(() -> visual.setSelectedPlatforms(selected));
        onEdt(wizard::doNextClick);
        assertEquals(selected, wizard.getProperty(
                FlutterProjectPlatformsWizardPanel.PROP_PLATFORMS));

        // Prove that returning to the panel reads the stored wizard state instead
        // of merely preserving the component's in-memory selection.
        onEdt(() -> visual.setSelectedPlatforms(FlutterProjectPlatform.all()));
        onEdt(wizard::doPreviousClick);
        assertEquals(selected, onEdt(visual::selectedPlatforms));
    }

    @Test
    void iteratorExposesTwoNumberedStepsAndChecksNavigationBounds() throws Exception {
        FlutterProjectWizardIterator iterator = new FlutterProjectWizardIterator();
        FlutterProjectPlatformsWizardPanel placeholder =
                new FlutterProjectPlatformsWizardPanel();
        WizardDescriptor wizard = descriptor(placeholder);

        onEdt(() -> iterator.initialize(wizard));
        JComponent first = (JComponent) onEdt(() -> iterator.current().getComponent());
        assertEquals(Bundle.LBL_FlutterProjectWizardStep(), first.getName());
        assertArrayEquals(new String[]{
            Bundle.LBL_FlutterProjectWizardStep(),
            Bundle.LBL_FlutterProjectPlatformsWizardStep()
        }, (String[]) first.getClientProperty(WizardDescriptor.PROP_CONTENT_DATA));
        assertEquals(0, first.getClientProperty(
                WizardDescriptor.PROP_CONTENT_SELECTED_INDEX));
        assertTrue(iterator.hasNext());
        assertFalse(iterator.hasPrevious());
        assertThrows(NoSuchElementException.class, iterator::previousPanel);

        onEdt(iterator::nextPanel);
        JComponent second = (JComponent) onEdt(() -> iterator.current().getComponent());
        assertEquals(Bundle.LBL_FlutterProjectPlatformsWizardStep(), second.getName());
        assertArrayEquals((String[]) first.getClientProperty(
                WizardDescriptor.PROP_CONTENT_DATA), (String[]) second.getClientProperty(
                WizardDescriptor.PROP_CONTENT_DATA));
        assertEquals(1, second.getClientProperty(
                WizardDescriptor.PROP_CONTENT_SELECTED_INDEX));
        assertFalse(iterator.hasNext());
        assertTrue(iterator.hasPrevious());
        assertThrows(NoSuchElementException.class, iterator::nextPanel);

        onEdt(iterator::previousPanel);
        assertEquals(first, onEdt(() -> iterator.current().getComponent()));
        onEdt(() -> iterator.uninitialize(wizard));
        assertThrows(NoSuchElementException.class, iterator::current);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @SafeVarargs
    private static WizardDescriptor descriptor(
            WizardDescriptor.Panel<WizardDescriptor>... panels) throws Exception {
        return onEdt(() -> new WizardDescriptor(
                panels));
    }

    @SafeVarargs
    private static WizardDescriptor initializedDescriptor(
            WizardDescriptor.Panel<WizardDescriptor>... panels) throws Exception {
        return onEdt(() -> {
            TestWizardDescriptor descriptor = new TestWizardDescriptor(panels);
            descriptor.initializeForTest();
            return descriptor;
        });
    }

    private static PlatformUiSnapshot snapshot(FlutterProjectPlatformsVisual visual) {
        List<JCheckBox> checkBoxes = descendants(visual, JCheckBox.class);
        List<Integer> mnemonics = new ArrayList<>();
        Set<String> accessibleNames = new HashSet<>();
        for (JCheckBox checkBox : checkBoxes) {
            mnemonics.add(checkBox.getMnemonic());
            accessibleNames.add(
                    checkBox.getAccessibleContext().getAccessibleName());
        }
        JLabel presetLabel = descendants(visual, JLabel.class).stream()
                .filter(label -> label.getLabelFor() instanceof JComboBox<?>)
                .findFirst()
                .orElseThrow();
        mnemonics.add(presetLabel.getDisplayedMnemonic());
        return new PlatformUiSnapshot(
                checkBoxes.size(),
                List.copyOf(mnemonics),
                Set.copyOf(accessibleNames),
                visual.getPreferredSize().width);
    }

    private static String localizedControlText(String localizedText) {
        JCheckBox checkBox = new JCheckBox();
        Mnemonics.setLocalizedText(checkBox, localizedText);
        return checkBox.getText();
    }

    private static <T extends Component> List<T> descendants(
            Component component,
            Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(component)) {
            result.add(type.cast(component));
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                result.addAll(descendants(child, type));
            }
        }
        return result;
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        onEdt(() -> {
            runnable.run();
            return null;
        });
    }

    private static <T> T onEdt(Callable<T> callable) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return callable.call();
        }
        Holder<T> value = new Holder<>();
        Holder<Throwable> failure = new Holder<>();
        try {
            EventQueue.invokeAndWait(() -> {
                try {
                    value.value = callable.call();
                } catch (Throwable throwable) {
                    failure.value = throwable;
                }
            });
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (InvocationTargetException exception) {
            throw new AssertionError(exception.getCause());
        }
        if (failure.value instanceof Exception exception) {
            throw exception;
        }
        if (failure.value != null) {
            throw new AssertionError(failure.value);
        }
        return value.value;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class Holder<T> {
        private T value;
    }

    private static final class TestWizardDescriptor extends WizardDescriptor {
        @SuppressWarnings({"rawtypes", "unchecked"})
        private TestWizardDescriptor(
                WizardDescriptor.Panel<WizardDescriptor>[] panels) {
            super(panels);
        }

        private void initializeForTest() {
            super.initialize();
        }
    }

    private record PlatformUiSnapshot(
            int checkboxCount,
            List<Integer> mnemonics,
            Set<String> accessibleNames,
            int preferredWidth) {
    }
}
