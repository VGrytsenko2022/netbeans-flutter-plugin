package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.windows.TopComponent;

class FlutterDesignerAuxiliaryWindowsTest {

    @Test
    void opensPaletteAndPropertiesExactlyOnceWithoutRequestingActivation()
            throws Exception {
        Map<String, TopComponent> components = new LinkedHashMap<>();
        components.put(FlutterDesignerAuxiliaryWindows.PALETTE_ID,
                new TopComponent());
        components.put(FlutterDesignerAuxiliaryWindows.PROPERTIES_ID,
                new TopComponent());
        List<TopComponent> opened = new ArrayList<>();
        FlutterDesignerAuxiliaryWindows windows =
                new FlutterDesignerAuxiliaryWindows(components::get, opened::add);

        SwingUtilities.invokeAndWait(() -> {
            windows.openOnce();
            windows.openOnce();
        });

        assertEquals(List.of(
                components.get(FlutterDesignerAuxiliaryWindows.PALETTE_ID),
                components.get(FlutterDesignerAuxiliaryWindows.PROPERTIES_ID)),
                opened);
    }

    @Test
    void missingPaletteDoesNotPreventPropertiesFromOpening() throws Exception {
        TopComponent properties = new TopComponent();
        List<TopComponent> opened = new ArrayList<>();
        FlutterDesignerAuxiliaryWindows windows =
                new FlutterDesignerAuxiliaryWindows(
                        id -> FlutterDesignerAuxiliaryWindows.PROPERTIES_ID.equals(id)
                                ? properties : null,
                        opened::add);

        SwingUtilities.invokeAndWait(windows::openOnce);

        assertEquals(List.of(properties), opened);
    }
}
