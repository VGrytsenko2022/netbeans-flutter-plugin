package io.github.vgrytsenko2022.plugin.designer;

import java.awt.EventQueue;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/** Opens the standard context windows once without taking focus from Design. */
final class FlutterDesignerAuxiliaryWindows {
    static final String PALETTE_ID = "CommonPalette";
    static final String PROPERTIES_ID = "properties";
    private static final Logger LOG = Logger.getLogger(
            FlutterDesignerAuxiliaryWindows.class.getName());
    private static final FlutterDesignerAuxiliaryWindows DEFAULT =
            new FlutterDesignerAuxiliaryWindows(
                    id -> WindowManager.getDefault().findTopComponent(id),
                    TopComponent::open);

    private final Function<String, TopComponent> finder;
    private final Consumer<TopComponent> opener;
    private final AtomicBoolean requested = new AtomicBoolean();

    FlutterDesignerAuxiliaryWindows(
            Function<String, TopComponent> finder,
            Consumer<TopComponent> opener) {
        this.finder = Objects.requireNonNull(finder, "finder");
        this.opener = Objects.requireNonNull(opener, "opener");
    }

    static void openDefaultOnce() {
        DEFAULT.openOnce();
    }

    void openOnce() {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(this::openOnce);
            return;
        }
        if (!requested.compareAndSet(false, true)) {
            return;
        }
        open(PALETTE_ID);
        open(PROPERTIES_ID);
    }

    private void open(String preferredId) {
        TopComponent component = finder.apply(preferredId);
        if (component == null) {
            LOG.log(Level.FINE,
                    "Standard Flutter Designer context window {0} is unavailable",
                    preferredId);
            return;
        }
        try {
            opener.accept(component);
        } catch (RuntimeException failure) {
            LOG.log(Level.INFO,
                    "Could not open standard Flutter Designer context window "
                    + preferredId,
                    failure);
        }
    }
}
