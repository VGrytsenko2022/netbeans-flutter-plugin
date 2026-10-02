package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.canvas.CanvasTargetPlatform;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerCanvasBackendSelector.Backend;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FlutterDesignerCanvasBackendSelectorTest {

    @Test
    void productionDefaultKeepsEveryTargetOnTheNativeBackend() {
        FlutterDesignerCanvasBackendSelector selector =
                FlutterDesignerCanvasBackendSelector.production();

        assertSelection(selector, Map.of(
                CanvasTargetPlatform.ANDROID, Backend.NATIVE,
                CanvasTargetPlatform.IOS, Backend.NATIVE,
                CanvasTargetPlatform.WINDOWS, Backend.NATIVE,
                CanvasTargetPlatform.MACOS, Backend.NATIVE,
                CanvasTargetPlatform.LINUX, Backend.NATIVE,
                CanvasTargetPlatform.WEB, Backend.NATIVE));
    }

    @Test
    void enabledTestSeamRoutesOnlyTheWebTargetToTheExactWebBackend() {
        FlutterDesignerCanvasBackendSelector selector =
                new FlutterDesignerCanvasBackendSelector(true);

        assertSelection(selector, Map.of(
                CanvasTargetPlatform.ANDROID, Backend.NATIVE,
                CanvasTargetPlatform.IOS, Backend.NATIVE,
                CanvasTargetPlatform.WINDOWS, Backend.NATIVE,
                CanvasTargetPlatform.MACOS, Backend.NATIVE,
                CanvasTargetPlatform.LINUX, Backend.NATIVE,
                CanvasTargetPlatform.WEB, Backend.EXACT_WEB));
    }

    @Test
    void rejectsNullTargetBeforeSelectingABackend() {
        FlutterDesignerCanvasBackendSelector selector =
                new FlutterDesignerCanvasBackendSelector(true);

        assertThrows(NullPointerException.class, () -> selector.select(null));
    }

    private static void assertSelection(
            FlutterDesignerCanvasBackendSelector selector,
            Map<CanvasTargetPlatform, Backend> expected) {
        assertEquals(CanvasTargetPlatform.values().length, expected.size(),
                "selection table must cover every CanvasTargetPlatform");
        for (CanvasTargetPlatform target : CanvasTargetPlatform.values()) {
            assertEquals(expected.get(target), selector.select(target), target.name());
        }
    }
}
