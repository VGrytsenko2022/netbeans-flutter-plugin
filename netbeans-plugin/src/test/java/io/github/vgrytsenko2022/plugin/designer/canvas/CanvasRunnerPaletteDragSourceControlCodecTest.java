package io.github.vgrytsenko2022.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.vgrytsenko2022.designer.canvas.CanvasFrameKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasLayoutKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasRevisionKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CanvasRunnerPaletteDragSourceControlCodecTest {
    private static final CanvasRevisionKey REVISION = new CanvasRevisionKey(
            CanvasSessionId.parse("80ef60ed-b108-4674-99a6-c1f3102f01ab"),
            7,
            StableId.parse("a4b202a7-060a-4d07-ba52-54340b6a80ea"),
            11);
    private static final CanvasLayoutKey LAYOUT = new CanvasLayoutKey(
            new CanvasFrameKey(REVISION, 3), 5);
    private static final String TOKEN =
            "nbfdnd:v1:6a7bab32-9507-4f6d-b986-39f183742017:"
            + "f83e4ad8-e66f-43ae-a5b3-cb057809f17e";

    private final CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();

    @Test
    void encodesExactLayoutBoundSourceWithCanonicalSortedTraits() throws Exception {
        LinkedHashSet<String> traits = new LinkedHashSet<>();
        traits.add("z.example.Last");
        traits.add("flutter.widgets.PreferredSizeWidget");

        String json = new String(codec.encodePaletteDragSource(
                LAYOUT,
                TOKEN,
                new WidgetTypeId("flutter.material.AppBar"),
                traits), StandardCharsets.UTF_8);

        assertEquals("{\"format\":\"netbeans-flutter-canvas-runtime\","
                + "\"protocolVersion\":1,"
                + "\"sessionId\":\"80ef60ed-b108-4674-99a6-c1f3102f01ab\","
                + "\"type\":\"host.paletteDragSource\",\"body\":{"
                + "\"presentationSequence\":7,"
                + "\"documentId\":\"a4b202a7-060a-4d07-ba52-54340b6a80ea\","
                + "\"logicalRevisionId\":11,"
                + "\"frameSequence\":3,\"layoutSequence\":5,"
                + "\"token\":\"" + TOKEN + "\","
                + "\"widgetType\":\"flutter.material.AppBar\","
                + "\"traits\":[\"flutter.widgets.PreferredSizeWidget\","
                + "\"z.example.Last\"]}}", json);
    }

    @Test
    void encodesExpandedWrapperSourceIdentityWithNoSyntheticTraits()
            throws Exception {
        String json = new String(codec.encodePaletteDragSource(
                LAYOUT,
                TOKEN,
                new WidgetTypeId("flutter.widgets.Expanded"),
                Set.of()), StandardCharsets.UTF_8);

        assertEquals("{\"format\":\"netbeans-flutter-canvas-runtime\","
                + "\"protocolVersion\":1,"
                + "\"sessionId\":\"80ef60ed-b108-4674-99a6-c1f3102f01ab\","
                + "\"type\":\"host.paletteDragSource\",\"body\":{"
                + "\"presentationSequence\":7,"
                + "\"documentId\":\"a4b202a7-060a-4d07-ba52-54340b6a80ea\","
                + "\"logicalRevisionId\":11,"
                + "\"frameSequence\":3,\"layoutSequence\":5,"
                + "\"token\":\"" + TOKEN + "\","
                + "\"widgetType\":\"flutter.widgets.Expanded\","
                + "\"traits\":[]}}", json);
    }

    @Test
    void encodesFlexibleWrapperSourceIdentityWithNoSyntheticTraits()
            throws Exception {
        String json = new String(codec.encodePaletteDragSource(
                LAYOUT,
                TOKEN,
                new WidgetTypeId("flutter.widgets.Flexible"),
                Set.of()), StandardCharsets.UTF_8);

        assertEquals("{\"format\":\"netbeans-flutter-canvas-runtime\","
                + "\"protocolVersion\":1,"
                + "\"sessionId\":\"80ef60ed-b108-4674-99a6-c1f3102f01ab\","
                + "\"type\":\"host.paletteDragSource\",\"body\":{"
                + "\"presentationSequence\":7,"
                + "\"documentId\":\"a4b202a7-060a-4d07-ba52-54340b6a80ea\","
                + "\"logicalRevisionId\":11,"
                + "\"frameSequence\":3,\"layoutSequence\":5,"
                + "\"token\":\"" + TOKEN + "\","
                + "\"widgetType\":\"flutter.widgets.Flexible\","
                + "\"traits\":[]}}", json);
    }

    @Test
    void encodesSpacerTerminalSourceIdentityWithNoSyntheticTraits()
            throws Exception {
        String json = new String(codec.encodePaletteDragSource(
                LAYOUT,
                TOKEN,
                new WidgetTypeId("flutter.widgets.Spacer"),
                Set.of()), StandardCharsets.UTF_8);

        assertEquals("{\"format\":\"netbeans-flutter-canvas-runtime\","
                + "\"protocolVersion\":1,"
                + "\"sessionId\":\"80ef60ed-b108-4674-99a6-c1f3102f01ab\","
                + "\"type\":\"host.paletteDragSource\",\"body\":{"
                + "\"presentationSequence\":7,"
                + "\"documentId\":\"a4b202a7-060a-4d07-ba52-54340b6a80ea\","
                + "\"logicalRevisionId\":11,"
                + "\"frameSequence\":3,\"layoutSequence\":5,"
                + "\"token\":\"" + TOKEN + "\","
                + "\"widgetType\":\"flutter.widgets.Spacer\","
                + "\"traits\":[]}}", json);
    }

    @Test
    void rejectsMalformedTokenInvalidTraitAndUnboundedTraitSet() {
        WidgetTypeId appBar = new WidgetTypeId("flutter.material.AppBar");
        assertThrows(IllegalArgumentException.class,
                () -> codec.encodePaletteDragSource(
                        LAYOUT, "nbfdnd:v1:not-a-token", appBar, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> codec.encodePaletteDragSource(
                        LAYOUT, TOKEN, appBar, Set.of("not a trait")));

        LinkedHashSet<String> tooMany = new LinkedHashSet<>();
        for (int index = 0; index < 33; index++) {
            tooMany.add("example.Trait" + index);
        }
        assertThrows(IllegalArgumentException.class,
                () -> codec.encodePaletteDragSource(
                        LAYOUT, TOKEN, appBar, tooMany));
    }
}
