package dev.flutter.netbeans.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class FlutterTestRunModelTest {
    private final FlutterTestJsonParser parser = new FlutterTestJsonParser();

    @Test
    void correlatesInterleavedTestsByIdAndAppliesLateErrors() throws Exception {
        FlutterTestRunModel model = new FlutterTestRunModel();
        accept(model, "{\"type\":\"suite\",\"time\":1,\"suite\":{\"id\":1,\"platform\":\"vm\",\"path\":\"test/a.dart\"}}");
        accept(model, "{\"type\":\"suite\",\"time\":2,\"suite\":{\"id\":2,\"platform\":\"chrome\",\"path\":\"test/b.dart\"}}");
        accept(model, start(10, "first", 1));
        accept(model, start(20, "second", 2));
        accept(model, "{\"type\":\"print\",\"time\":4,\"testID\":20,\"messageType\":\"print\",\"message\":\"working\\n\"}");
        accept(model, "{\"type\":\"error\",\"time\":5,\"testID\":10,\"error\":\"expected true\",\"stackTrace\":\"a.dart 3:4\",\"isFailure\":true}");
        accept(model, "{\"type\":\"testDone\",\"time\":6,\"testID\":20,\"result\":\"success\",\"hidden\":true,\"skipped\":false}");
        accept(model, "{\"type\":\"testDone\",\"time\":7,\"testID\":10,\"result\":\"failure\",\"hidden\":false,\"skipped\":false}");

        assertEquals(FlutterTestOutcome.FAILURE, model.test(10).orElseThrow().outcome());
        assertEquals(FlutterTestOutcome.SUCCESS, model.test(20).orElseThrow().outcome());
        assertTrue(model.test(20).orElseThrow().hidden());

        // The protocol allows an error after a successful testDone.
        accept(model, "{\"type\":\"error\",\"time\":8,\"testID\":20,\"error\":\"late async error\",\"stackTrace\":\"b.dart 8:2\",\"isFailure\":false}");
        FlutterTestCaseState second = model.test(20).orElseThrow();
        assertEquals(FlutterTestOutcome.ERROR, second.outcome());
        assertFalse(second.hidden());
        assertEquals(Optional.of(3L), second.durationMillis());
        assertEquals("working\n", second.messages().getFirst().message());
        assertEquals(1, second.errors().size());

        accept(model, "{\"type\":\"done\",\"time\":9,\"success\":false}");
        assertTrue(model.done());
        assertEquals(Optional.of(false), model.success());
        assertEquals(2, model.suites().size());
        assertEquals(2, model.tests().size());
    }

    @Test
    void acceptsReferenceEventsBeforeMetadataAndRetainsUnknownEvents() throws Exception {
        FlutterTestRunModel model = new FlutterTestRunModel();
        accept(model, "{\"type\":\"error\",\"time\":1,\"testID\":42,\"error\":\"load failed\",\"isFailure\":false}");
        accept(model, start(42, "loading test/a.dart", 5));
        accept(model, "{\"type\":\"newProtocolEvent\",\"time\":3,\"data\":1}");

        FlutterTestCaseState state = model.test(42).orElseThrow();
        assertTrue(state.test().isPresent());
        assertEquals(FlutterTestOutcome.ERROR, state.outcome());
        assertEquals(1, model.unknownEvents().size());
        assertEquals("newProtocolEvent", model.unknownEvents().getFirst().type());
    }

    @Test
    void recordsSkippedAndUnknownFutureResults() throws Exception {
        FlutterTestRunModel model = new FlutterTestRunModel();
        accept(model, start(1, "skipped", 1));
        accept(model, start(2, "future", 1));
        accept(model, "{\"type\":\"testDone\",\"testID\":1,\"result\":\"success\",\"hidden\":false,\"skipped\":true}");
        accept(model, "{\"type\":\"testDone\",\"testID\":2,\"result\":\"flaky\",\"hidden\":false,\"skipped\":false}");

        assertEquals(FlutterTestOutcome.SKIPPED, model.test(1).orElseThrow().outcome());
        assertEquals(FlutterTestOutcome.UNKNOWN, model.test(2).orElseThrow().outcome());
    }

    private void accept(FlutterTestRunModel model, String json) throws Exception {
        model.accept(parser.parseLine(json).orElseThrow());
    }

    private static String start(long id, String name, long suiteId) {
        return "{\"type\":\"testStart\",\"time\":3,\"test\":{"
                + "\"id\":" + id + ",\"name\":\"" + name + "\","
                + "\"suiteID\":" + suiteId + ",\"groupIDs\":[]}}";
    }
}
