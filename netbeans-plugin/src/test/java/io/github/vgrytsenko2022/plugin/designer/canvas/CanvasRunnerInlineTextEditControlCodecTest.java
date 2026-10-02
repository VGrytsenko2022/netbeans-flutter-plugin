package io.github.vgrytsenko2022.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vgrytsenko2022.designer.canvas.CanvasFrameKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasIntentId;
import io.github.vgrytsenko2022.designer.canvas.CanvasIntentKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasLayoutKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasRevisionKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireLimits;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanvasRunnerInlineTextEditControlCodecTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse(
            "fbb42c4c-9605-4af6-93e8-0506ba967b66");
    private static final StableId WIDGET_ID = StableId.parse(
            "145bd83f-754c-40f0-a0d5-cade98f42723");

    @Test
    void decodesCanonicalUnicodeAndEmptyFinalTextWithExactIdentity()
            throws Exception {
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasRevisionKey revision = new CanvasRevisionKey(
                sessionId, 7, DOCUMENT_ID, 11);
        String unicode = "Привіт 你好 😀 𐐷 e\u0301";
        CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();

        CanvasRunnerRuntimeEvent.TextEditCommit populated = assertInstanceOf(
                CanvasRunnerRuntimeEvent.TextEditCommit.class,
                assertDoesNotThrow(() -> codec.decode(textEditCommit(
                        revision, 13, 17, 19, 23, WIDGET_ID,
                        unicode, false).getBytes(StandardCharsets.UTF_8))));
        CanvasRunnerRuntimeEvent.TextEditCommit empty = assertInstanceOf(
                CanvasRunnerRuntimeEvent.TextEditCommit.class,
                assertDoesNotThrow(() -> codec.decode(textEditCommit(
                        revision, 29, 31, 37, 41, WIDGET_ID,
                        "", true).getBytes(StandardCharsets.UTF_8))));

        assertEquals(new CanvasIntentKey(
                new CanvasIntentId(sessionId, 19),
                new CanvasLayoutKey(new CanvasFrameKey(revision, 13), 17)),
                populated.intentKey());
        assertEquals(23, populated.interactionFenceSequence());
        assertEquals(WIDGET_ID, populated.widgetId());
        assertEquals(unicode, populated.text());
        assertFalse(populated.compositionObserved());
        assertEquals("", empty.text());
        assertTrue(empty.compositionObserved());
    }

    @Test
    void acceptsTheReviewedUtf16AndUnicodeScalarBoundary() throws Exception {
        CanvasRevisionKey revision = revision();
        String exactBoundary = "😀".repeat(
                CanvasWireLimits.DEFAULT_MAX_STRING_CODE_POINTS);

        CanvasRunnerRuntimeEvent.TextEditCommit decoded = assertInstanceOf(
                CanvasRunnerRuntimeEvent.TextEditCommit.class,
                assertDoesNotThrow(() -> new CanvasRunnerControlCodec().decode(
                        textEditCommit(
                                revision, 0, 0, 0, 0, WIDGET_ID,
                                exactBoundary, false)
                                .getBytes(StandardCharsets.UTF_8))));

        assertEquals(CanvasWireLimits.DEFAULT_MAX_STRING_UTF16_UNITS,
                decoded.text().length());
        assertEquals(CanvasWireLimits.DEFAULT_MAX_STRING_CODE_POINTS,
                decoded.text().codePointCount(0, decoded.text().length()));
    }

    @Test
    void rejectsMalformedOversizeExtraAndMissingTextCommitBodies()
            throws Exception {
        CanvasRevisionKey revision = revision();
        String valid = textEditCommit(
                revision, 3, 5, 7, 11, WIDGET_ID, "hello", false);
        String scalarOverflow = textEditCommit(
                revision, 3, 5, 8, 11, WIDGET_ID,
                "a".repeat(CanvasWireLimits.DEFAULT_MAX_STRING_CODE_POINTS + 1),
                false);
        String utf16Overflow = textEditCommit(
                revision, 3, 5, 9, 11, WIDGET_ID,
                "a".repeat(CanvasWireLimits.DEFAULT_MAX_STRING_UTF16_UNITS + 1),
                false);
        List<String> malformed = List.of(
                valid.replace(
                        "\"compositionObserved\":false}",
                        "\"compositionObserved\":false,\"unexpected\":true}"),
                valid.replace(",\"text\":\"hello\"", ""),
                valid.replace(",\"compositionObserved\":false", ""),
                valid.replace("\"text\":\"hello\"", "\"text\":42"),
                valid.replace("\"compositionObserved\":false",
                        "\"compositionObserved\":\"false\""),
                valid.replace(
                        "\"presentationSequence\":"
                                + revision.presentationSequence() + ",",
                        ""),
                valid.replace(
                        ",\"documentId\":\"" + revision.documentId() + "\"",
                        ""),
                valid.replace(
                        ",\"logicalRevisionId\":"
                                + revision.logicalRevisionId(),
                        ""),
                valid.replace(",\"frameSequence\":3", ""),
                valid.replace(",\"layoutSequence\":5", ""),
                valid.replace(",\"interactionFenceSequence\":11", ""),
                valid.replace(",\"intentSequence\":7", ""),
                valid.replace(",\"widgetId\":\"" + WIDGET_ID + "\"", ""),
                valid.replace(WIDGET_ID.toString(), "not-a-stable-id"),
                valid.replace(
                        "\"interactionFenceSequence\":11",
                        "\"interactionFenceSequence\":-1"),
                valid.replace(
                        "\"interactionFenceSequence\":11",
                        "\"interactionFenceSequence\":2.5"),
                valid.replace("\"text\":\"hello\"", "\"text\":\"\\uD800\""),
                valid.replace("\"text\":\"hello\"", "\"text\":\"\\uDC00\""),
                scalarOverflow,
                utf16Overflow);
        CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();

        for (String payload : malformed) {
            assertThrows(
                    CanvasRunnerControlException.class,
                    () -> codec.decode(payload.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static CanvasRevisionKey revision() {
        return new CanvasRevisionKey(
                CanvasSessionId.random(), 2, DOCUMENT_ID, 3);
    }

    private static String textEditCommit(
            CanvasRevisionKey revision,
            long frame,
            long layout,
            long intent,
            long interactionFenceSequence,
            StableId widgetId,
            String text,
            boolean compositionObserved) throws JsonProcessingException {
        return "{\"format\":\"" + CanvasRunnerControlCodec.FORMAT
                + "\",\"protocolVersion\":1,\"sessionId\":\""
                + revision.sessionId() + "\",\"type\":\"runner.textEditCommit\""
                + ",\"body\":{\"presentationSequence\":"
                + revision.presentationSequence() + ",\"documentId\":\""
                + revision.documentId() + "\",\"logicalRevisionId\":"
                + revision.logicalRevisionId() + ",\"frameSequence\":" + frame
                + ",\"layoutSequence\":" + layout
                + ",\"intentSequence\":" + intent
                + ",\"interactionFenceSequence\":"
                + interactionFenceSequence + ",\"widgetId\":\"" + widgetId
                + "\",\"text\":" + JSON.writeValueAsString(text)
                + ",\"compositionObserved\":" + compositionObserved + "}}";
    }
}
