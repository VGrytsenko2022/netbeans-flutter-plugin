package io.github.vgrytsenko2022.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FlutterTestJsonParserTest {
    private final FlutterTestJsonParser parser = new FlutterTestJsonParser();

    @Test
    void parsesSupportedEventsAndIgnoresAddedFields() throws Exception {
        FlutterTestEvent.SuiteEvent suite = assertInstanceOf(
                FlutterTestEvent.SuiteEvent.class,
                parse("""
                        {"type":"suite","time":1,"futureField":{"x":1},
                         "suite":{"id":3,"platform":"vm","path":"test/a_test.dart","new":true}}
                        """));
        assertEquals(3, suite.suite().id());
        assertEquals("vm", suite.suite().platform());
        assertEquals(Optional.of("test/a_test.dart"), suite.suite().path());

        FlutterTestEvent.TestStartEvent start = assertInstanceOf(
                FlutterTestEvent.TestStartEvent.class,
                parse("""
                        {"type":"testStart","time":2,"test":{"id":7,"name":"adds values",
                         "suiteID":3,"groupIDs":[4,5],"line":12,"column":7,
                         "url":"file:///work/test/a_test.dart","metadata":{"skip":false}}}
                        """));
        assertEquals(7, start.test().id());
        assertEquals(List.of(4L, 5L), start.test().groupIds());
        assertEquals(Optional.of(12), start.test().line());

        FlutterTestEvent.PrintEvent print = assertInstanceOf(
                FlutterTestEvent.PrintEvent.class,
                parse("""
                        {"type":"print","time":3,"testID":7,"messageType":"print",
                         "message":"progress\\n"}
                        """));
        assertEquals(7, print.testId());
        assertEquals("print", print.messageType());
        assertEquals("progress\n", print.message());

        FlutterTestEvent.ErrorEvent error = assertInstanceOf(
                FlutterTestEvent.ErrorEvent.class,
                parse("""
                        {"type":"error","time":5,"testID":7,"error":"Expected 2, got 3",
                         "stackTrace":"test/a_test.dart 12:7","isFailure":true}
                        """));
        assertTrue(error.failure());

        FlutterTestEvent.TestDoneEvent testDone = assertInstanceOf(
                FlutterTestEvent.TestDoneEvent.class,
                parse("""
                        {"type":"testDone","time":6,"testID":7,"result":"failure",
                         "hidden":false,"skipped":false}
                        """));
        assertEquals("failure", testDone.result());
        assertFalse(testDone.hidden());

        FlutterTestEvent.DoneEvent done = assertInstanceOf(
                FlutterTestEvent.DoneEvent.class,
                parse("{" + "\"type\":\"done\",\"time\":7,\"success\":false}"));
        assertEquals(Optional.of(false), done.success());
    }

    @Test
    void preservesUnknownEventsForForwardCompatibility() throws Exception {
        String json = "{\"type\":\"coverage\",\"time\":8,\"payload\":{\"percent\":91.5}}";

        FlutterTestEvent.UnknownEvent event = assertInstanceOf(
                FlutterTestEvent.UnknownEvent.class,
                parse(json));

        assertEquals("coverage", event.type());
        assertEquals(8, event.time());
        assertEquals(json, event.rawJson());
    }

    @Test
    void supportsInterruptedDoneAndBlankLines() throws Exception {
        FlutterTestEvent.DoneEvent done = assertInstanceOf(
                FlutterTestEvent.DoneEvent.class,
                parse("{\"type\":\"done\",\"time\":9,\"success\":null}"));

        assertTrue(done.success().isEmpty());
        assertTrue(parser.parseLine("  ").isEmpty());
    }

    @Test
    void rejectsMalformedJsonAndMalformedKnownEvents() {
        assertThrows(IOException.class, () -> parser.parseLine("not-json"));
        assertThrows(IOException.class, () -> parser.parseLine("{\"type\":\"suite\",\"suite\":{}}"));
        assertThrows(IOException.class,
                () -> parser.parseLine("{\"type\":\"testDone\",\"testID\":1}"));
    }

    private FlutterTestEvent parse(String line) throws IOException {
        return parser.parseLine(line).orElseThrow();
    }
}
