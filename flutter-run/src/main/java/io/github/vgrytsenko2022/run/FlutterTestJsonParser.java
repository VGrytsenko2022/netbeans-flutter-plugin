package io.github.vgrytsenko2022.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Parses the newline-delimited stream emitted by {@code --reporter=json}. */
public final class FlutterTestJsonParser {
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Parses one protocol line. Blank lines do not carry an event.
     *
     * @throws IOException when a JSON object for a known event is malformed
     */
    public Optional<FlutterTestEvent> parseLine(String line) throws IOException {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        final JsonNode root;
        try {
            root = JSON.readTree(line);
        } catch (JsonProcessingException ex) {
            throw new IOException("Invalid Flutter test JSON event: " + ex.getOriginalMessage(), ex);
        }
        if (root == null || !root.isObject()) {
            throw new IOException("Invalid Flutter test JSON event: expected an object");
        }
        String type = requiredText(root, "type", "event");
        long time = optionalLong(root, "time", 0);
        return Optional.of(switch (type) {
            case "suite" -> parseSuite(root, time);
            case "testStart" -> parseTestStart(root, time);
            case "print" -> parsePrint(root, time);
            case "error" -> parseError(root, time);
            case "testDone" -> parseTestDone(root, time);
            case "done" -> parseDone(root, time);
            default -> new FlutterTestEvent.UnknownEvent(type, time, line);
        });
    }

    private static FlutterTestEvent parsePrint(JsonNode root, long time) throws IOException {
        return new FlutterTestEvent.PrintEvent(
                time,
                requiredLong(root, "testID", "print event"),
                requiredText(root, "messageType", "print event"),
                requiredString(root, "message", "print event"));
    }

    private static FlutterTestEvent parseSuite(JsonNode root, long time) throws IOException {
        JsonNode suite = requiredObject(root, "suite", "suite event");
        return new FlutterTestEvent.SuiteEvent(
                time,
                new FlutterTestSuite(
                        requiredLong(suite, "id", "suite"),
                        optionalText(suite, "platform").orElse("unknown"),
                        optionalText(suite, "path")));
    }

    private static FlutterTestEvent parseTestStart(JsonNode root, long time) throws IOException {
        JsonNode test = requiredObject(root, "test", "testStart event");
        List<Long> groupIds = new ArrayList<>();
        JsonNode groups = test.get("groupIDs");
        if (groups != null && !groups.isNull()) {
            if (!groups.isArray()) {
                throw malformed("test", "groupIDs must be an array");
            }
            for (JsonNode group : groups) {
                if (!group.isIntegralNumber() || !group.canConvertToLong()) {
                    throw malformed("test", "groupIDs must contain integer ids");
                }
                groupIds.add(group.longValue());
            }
        }
        return new FlutterTestEvent.TestStartEvent(
                time,
                new FlutterTestCase(
                        requiredLong(test, "id", "test"),
                        requiredText(test, "name", "test"),
                        requiredLong(test, "suiteID", "test"),
                        groupIds,
                        optionalPositiveInteger(test, "line", "test"),
                        optionalPositiveInteger(test, "column", "test"),
                        optionalText(test, "url")));
    }

    private static FlutterTestEvent parseError(JsonNode root, long time) throws IOException {
        return new FlutterTestEvent.ErrorEvent(
                time,
                requiredLong(root, "testID", "error event"),
                requiredText(root, "error", "error event"),
                optionalText(root, "stackTrace").orElse(""),
                optionalBoolean(root, "isFailure", false));
    }

    private static FlutterTestEvent parseTestDone(JsonNode root, long time) throws IOException {
        return new FlutterTestEvent.TestDoneEvent(
                time,
                requiredLong(root, "testID", "testDone event"),
                requiredText(root, "result", "testDone event"),
                optionalBoolean(root, "hidden", false),
                optionalBoolean(root, "skipped", false));
    }

    private static FlutterTestEvent parseDone(JsonNode root, long time) throws IOException {
        JsonNode success = root.get("success");
        if (success == null || success.isNull()) {
            return new FlutterTestEvent.DoneEvent(time, Optional.empty());
        }
        if (!success.isBoolean()) {
            throw malformed("done event", "success must be boolean or null");
        }
        return new FlutterTestEvent.DoneEvent(time, Optional.of(success.booleanValue()));
    }

    private static JsonNode requiredObject(JsonNode node, String field, String owner)
            throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isObject()) {
            throw malformed(owner, field + " must be an object");
        }
        return value;
    }

    private static String requiredText(JsonNode node, String field, String owner)
            throws IOException {
        return optionalText(node, field)
                .orElseThrow(() -> malformed(owner, field + " must be non-empty text"));
    }

    private static String requiredString(JsonNode node, String field, String owner)
            throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw malformed(owner, field + " must be text");
        }
        return value.textValue();
    }

    private static Optional<String> optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual() || value.textValue().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.textValue());
    }

    private static long requiredLong(JsonNode node, String field, String owner) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw malformed(owner, field + " must be an integer");
        }
        long result = value.longValue();
        if (result < 0) {
            throw malformed(owner, field + " cannot be negative");
        }
        return result;
    }

    private static long optionalLong(JsonNode node, String field, long fallback) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw malformed("event", field + " must be an integer");
        }
        return value.longValue();
    }

    private static Optional<Integer> optionalPositiveInteger(
            JsonNode node,
            String field,
            String owner) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 1) {
            throw malformed(owner, field + " must be a positive integer or null");
        }
        return Optional.of(value.intValue());
    }

    private static boolean optionalBoolean(JsonNode node, String field, boolean fallback)
            throws IOException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (!value.isBoolean()) {
            throw malformed("event", field + " must be boolean");
        }
        return value.booleanValue();
    }

    private static IOException malformed(String owner, String reason) {
        return new IOException("Invalid Flutter test " + owner + ": " + reason + ".");
    }
}
