package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.canvas.CanvasZoomMode;
import dev.flutter.netbeans.designer.model.StableId;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanvasRunnerViewportControlCodecTest {
    private static final CanvasRevisionKey REVISION = new CanvasRevisionKey(
            CanvasSessionId.parse("80ef60ed-b108-4674-99a6-c1f3102f01ab"),
            7,
            StableId.parse("a4b202a7-060a-4d07-ba52-54340b6a80ea"),
            11);

    private final CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();

    @Test
    void encodesCanonicalHostViewportWithoutChangingRuntimeVersion()
            throws Exception {
        String json = new String(codec.encodeViewport(
                REVISION,
                42,
                CanvasViewportPresentation.manual(
                        1_250_000, 400_000, 600_000)),
                StandardCharsets.UTF_8);

        assertEquals("{\"format\":\"netbeans-flutter-canvas-runtime\","
                + "\"protocolVersion\":1,"
                + "\"sessionId\":\"80ef60ed-b108-4674-99a6-c1f3102f01ab\","
                + "\"type\":\"host.viewport\",\"body\":{"
                + "\"presentationSequence\":7,"
                + "\"documentId\":\"a4b202a7-060a-4d07-ba52-54340b6a80ea\","
                + "\"logicalRevisionId\":11,\"commandSequence\":42,"
                + "\"mode\":\"manual\","
                + "\"zoomMicros\":1250000,"
                + "\"horizontalScrollMicros\":400000,"
                + "\"verticalScrollMicros\":600000}}", json);
    }

    @Test
    void decodesExactRunnerViewportMetrics() throws Exception {
        CanvasRunnerRuntimeEvent.ViewportMetrics event = assertInstanceOf(
                CanvasRunnerRuntimeEvent.ViewportMetrics.class,
                codec.decode(runnerViewport().getBytes(StandardCharsets.UTF_8)));
        CanvasViewportMetrics metrics = event.metrics();

        assertEquals(REVISION, metrics.revisionKey());
        assertEquals(42, metrics.commandSequence());
        assertEquals(CanvasZoomMode.MANUAL, metrics.presentation().mode());
        assertEquals(1_250_000, metrics.presentation().zoomMicros());
        assertEquals(400_000,
                metrics.presentation().horizontalScrollMicros());
        assertEquals(600_000,
                metrics.presentation().verticalScrollMicros());
        assertEquals(1_250_000, metrics.effectiveScaleMicros());
        assertTrue(metrics.horizontalScrollable());
        assertFalse(metrics.verticalScrollable());

        CanvasViewportMetrics narrowFit = assertInstanceOf(
                CanvasRunnerRuntimeEvent.ViewportMetrics.class,
                codec.decode(runnerViewport().replace(
                        "\"effectiveScaleMicros\":1250000",
                        "\"effectiveScaleMicros\":125000")
                        .getBytes(StandardCharsets.UTF_8))).metrics();
        assertEquals(125_000, narrowFit.effectiveScaleMicros());
        CanvasViewportMetrics initialFit = assertInstanceOf(
                CanvasRunnerRuntimeEvent.ViewportMetrics.class,
                codec.decode(runnerViewport().replace(
                        "\"commandSequence\":42",
                        "\"commandSequence\":0")
                        .getBytes(StandardCharsets.UTF_8))).metrics();
        assertEquals(0, initialFit.commandSequence());
    }

    @Test
    void rejectsUnknownMissingMistypedAndOutOfRangeViewportFields() {
        String valid = runnerViewport();
        List<String> malformed = List.of(
                valid.replace(
                        "\"verticalScrollable\":false}",
                        "\"verticalScrollable\":false,\"unknown\":0}"),
                valid.replace(",\"verticalScrollMicros\":600000", ""),
                valid.replace(",\"commandSequence\":42", ""),
                valid.replace(
                        "\"commandSequence\":42",
                        "\"commandSequence\":-1"),
                valid.replace(
                        "\"commandSequence\":42",
                        "\"commandSequence\":9007199254740992"),
                valid.replace("\"mode\":\"manual\"", "\"mode\":\"auto\""),
                valid.replace("\"zoomMicros\":1250000", "\"zoomMicros\":249999"),
                valid.replace(
                        "\"horizontalScrollMicros\":400000",
                        "\"horizontalScrollMicros\":1000001"),
                valid.replace(
                        "\"effectiveScaleMicros\":1250000",
                        "\"effectiveScaleMicros\":2000001"),
                valid.replace(
                        "\"effectiveScaleMicros\":1250000",
                        "\"effectiveScaleMicros\":0"),
                valid.replace(
                        "\"horizontalScrollable\":true",
                        "\"horizontalScrollable\":1"));

        for (String payload : malformed) {
            assertThrows(
                    CanvasRunnerControlException.class,
                    () -> codec.decode(payload.getBytes(StandardCharsets.UTF_8)),
                    payload);
        }
    }

    @Test
    void rejectsHostCommandSequenceOutsideTheExactJsonIntegerRange() {
        assertThrows(
                CanvasRunnerControlException.class,
                () -> codec.encodeViewport(
                        REVISION,
                        0,
                        CanvasViewportPresentation.fit()));
        assertThrows(
                CanvasRunnerControlException.class,
                () -> codec.encodeViewport(
                        REVISION,
                        9_007_199_254_740_992L,
                        CanvasViewportPresentation.fit()));
    }

    private static String runnerViewport() {
        return "{\"format\":\"netbeans-flutter-canvas-runtime\","
                + "\"protocolVersion\":1,"
                + "\"sessionId\":\"" + REVISION.sessionId() + "\","
                + "\"type\":\"runner.viewport\",\"body\":{"
                + "\"presentationSequence\":"
                + REVISION.presentationSequence() + ','
                + "\"documentId\":\"" + REVISION.documentId() + "\","
                + "\"logicalRevisionId\":" + REVISION.logicalRevisionId() + ','
                + "\"commandSequence\":42,"
                + "\"mode\":\"manual\",\"zoomMicros\":1250000,"
                + "\"horizontalScrollMicros\":400000,"
                + "\"verticalScrollMicros\":600000,"
                + "\"effectiveScaleMicros\":1250000,"
                + "\"horizontalScrollable\":true,"
                + "\"verticalScrollable\":false}}";
    }
}
