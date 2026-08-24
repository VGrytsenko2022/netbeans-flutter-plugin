package dev.flutter.netbeans.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Optional headless integration test against an explicitly supplied real Dart SDK. */
class DartAnalysisServerRealSdkTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    @TempDir
    Path workspace;

    @Test
    void providesDiagnosticsCompletionNavigationRefactoringAndFormatting() throws Exception {
        Path executable = configuredDartExecutable();
        Path lib = Files.createDirectories(workspace.resolve("lib"));
        Files.writeString(workspace.resolve("pubspec.yaml"), """
                name: netbeans_lsp_smoke
                environment:
                  sdk: '>=3.0.0 <4.0.0'
                """, StandardCharsets.UTF_8);

        Path helper = lib.resolve("helper.dart");
        String helperSource = "int answer() => 42;\n";
        Files.writeString(helper, helperSource, StandardCharsets.UTF_8);

        Path main = lib.resolve("main.dart");
        String mainSource = """
                import 'helper.dart';

                void main() {
                  print(answer());
                }
                """;
        Files.writeString(main, mainSource, StandardCharsets.UTF_8);

        List<String> stderr = new ArrayList<>();
        try (DartAnalysisServer server = DartAnalysisServer.start(
                        executable,
                        workspace,
                        line -> {
                            synchronized (stderr) {
                                stderr.add(line);
                            }
                        });
                LspTestClient client = new LspTestClient(server.inputStream(), server.outputStream())) {
            JsonNode initializeResult = client.request("initialize", initializeParams());
            JsonNode capabilities = initializeResult.path("capabilities");
            assertCapability(capabilities, "completionProvider", stderr);
            assertCapability(capabilities, "definitionProvider", stderr);
            assertCapability(capabilities, "referencesProvider", stderr);
            assertCapability(capabilities, "renameProvider", stderr);
            assertCapability(capabilities, "documentFormattingProvider", stderr);
            assertCapability(capabilities, "documentRangeFormattingProvider", stderr);
            assertCapability(capabilities, "codeActionProvider", stderr);
            client.notify("initialized", JSON.createObjectNode());

            client.open(main, mainSource);
            client.awaitAnalysisIdle();

            JsonNode definition = client.request(
                    "textDocument/definition",
                    positionParams(main, 3, 10));
            assertTrue(
                    definition.toString().contains(helper.toUri().toString()),
                    () -> "definition did not point to helper.dart: " + definition);

            ObjectNode referencesParams = positionParams(main, 3, 10);
            referencesParams.set("context", JSON.createObjectNode().put("includeDeclaration", true));
            JsonNode references = client.request("textDocument/references", referencesParams);
            assertTrue(references.isArray() && references.size() >= 2, references::toString);

            ObjectNode renameParams = positionParams(main, 3, 10);
            renameParams.put("newName", "renamedAnswer");
            JsonNode rename = client.request("textDocument/rename", renameParams);
            assertTrue(rename.toString().contains("renamedAnswer"), rename::toString);
            assertTrue(rename.toString().contains(helper.toUri().toString()), rename::toString);
            assertTrue(rename.toString().contains(main.toUri().toString()), rename::toString);

            Path completionFile = lib.resolve("completion.dart");
            String completionSource = """
                    void main() {
                      ans
                    }
                    """;
            Files.writeString(completionFile, completionSource, StandardCharsets.UTF_8);
            client.open(completionFile, completionSource);
            client.awaitAnalysisIdle();
            JsonNode completion = client.request(
                    "textDocument/completion",
                    positionParams(completionFile, 1, 5));
            JsonNode completionItems = completion.isArray() ? completion : completion.path("items");
            assertTrue(completionItems.isArray() && !completionItems.isEmpty(), completion::toString);
            JsonNode answerCompletion = findCompletion(completionItems, "answer");
            assertNotNull(answerCompletion, () -> "completion did not contain answer: " + completion);
            assertTrue(answerCompletion.hasNonNull("textEdit"), answerCompletion::toString);
            assertTrue(answerCompletion.path("data").path("importUris").isArray()
                    && !answerCompletion.path("data").path("importUris").isEmpty(),
                    answerCompletion::toString);
            assertFalse(answerCompletion.path("additionalTextEdits").isArray()
                    && !answerCompletion.path("additionalTextEdits").isEmpty(),
                    answerCompletion::toString);
            ObjectNode netBeans30CompatibleCompletion = answerCompletion.deepCopy();
            JsonNode originalCompletionEdit = netBeans30CompatibleCompletion.remove("textEdit");
            ObjectNode resolveRequest = netBeans30CompatibleCompletion.deepCopy();
            resolveRequest.set("textEdit", originalCompletionEdit);
            JsonNode resolvedCompletion = client.request(
                    "completionItem/resolve",
                    resolveRequest);
            assertTrue(resolvedCompletion.hasNonNull("textEdit"), resolvedCompletion::toString);
            JsonNode additionalTextEdits = resolvedCompletion.path("additionalTextEdits");
            assertTrue(
                    additionalTextEdits.isArray() && !additionalTextEdits.isEmpty(),
                    resolvedCompletion::toString);
            assertTrue(
                    applyTextEdits(completionSource, additionalTextEdits)
                            .contains("import 'helper.dart';"),
                    resolvedCompletion::toString);

            Path formatFile = lib.resolve("format.dart");
            String unformatted = "void main(){print('hello');}\n";
            Files.writeString(formatFile, unformatted, StandardCharsets.UTF_8);
            client.open(formatFile, unformatted);
            client.awaitAnalysisIdle();
            ObjectNode formattingParams = JSON.createObjectNode();
            formattingParams.set("textDocument", textDocument(formatFile));
            formattingParams.set(
                    "options",
                    JSON.createObjectNode().put("tabSize", 2).put("insertSpaces", true));
            JsonNode formatting = client.request("textDocument/formatting", formattingParams);
            assertTrue(formatting.isArray() && !formatting.isEmpty(), formatting::toString);
            String formatted = applyTextEdits(unformatted, formatting);
            assertEquals("void main() {\n  print('hello');\n}\n", formatted);

            Path brokenFile = lib.resolve("broken.dart");
            String brokenSource = """
                    void main() {
                      final value = ;
                    }
                    """;
            Files.writeString(brokenFile, brokenSource, StandardCharsets.UTF_8);
            client.open(brokenFile, brokenSource);
            JsonNode diagnostics = client.awaitDiagnostics(brokenFile, true);
            assertFalse(diagnostics.isEmpty());
            assertTrue(
                    diagnostics.get(0).path("range").has("start"),
                    diagnostics::toString);
            assertFalse(diagnostics.get(0).path("message").asText().isBlank());

            Path importFile = lib.resolve("missing_import.dart");
            String importSource = """
                    void main() {
                      File('missing.txt');
                    }
                    """;
            Files.writeString(importFile, importSource, StandardCharsets.UTF_8);
            client.open(importFile, importSource);
            JsonNode importDiagnostics = client.awaitDiagnostics(importFile, true);
            JsonNode importActions = client.request(
                    "textDocument/codeAction",
                    codeActionParams(
                            importFile,
                            importDiagnostics.get(0).path("range"),
                            importDiagnostics));
            assertTrue(importActions.isArray() && !importActions.isEmpty(), importActions::toString);
            JsonNode importAction = findActionContaining(importActions, "dart:io");
            assertNotNull(
                    importAction,
                    () -> "missing-import fixes did not offer dart:io: " + importActions);
            client.executeCommand(importAction);
            JsonNode importEdit = client.takeLastWorkspaceEdit();
            assertNotNull(importEdit, "missing-import action did not call workspace/applyEdit");
            assertTrue(
                    applyTextEdits(importSource, workspaceTextEdits(importEdit, importFile))
                            .contains("import 'dart:io';"),
                    importEdit::toString);

            Path organizeFile = lib.resolve("organize_imports.dart");
            String organizeSource = """
                    import 'dart:async';
                    import 'dart:math';

                    void main() {
                      print(pi);
                    }
                    """;
            Files.writeString(organizeFile, organizeSource, StandardCharsets.UTF_8);
            client.open(organizeFile, organizeSource);
            client.awaitAnalysisIdle();
            JsonNode organizeActions = client.request(
                    "textDocument/codeAction",
                    sourceActionParams(organizeFile, 4, 2));
            JsonNode organizeAction = findAction(organizeActions, "Organize Imports");
            assertNotNull(
                    organizeAction,
                    () -> "Dart source actions did not offer Organize Imports: " + organizeActions);
            client.executeCommand(organizeAction);
            JsonNode organizeEdit = client.takeLastWorkspaceEdit();
            assertNotNull(organizeEdit, "Organize Imports did not call workspace/applyEdit");
            String organized = applyTextEdits(
                    organizeSource,
                    workspaceTextEdits(organizeEdit, organizeFile));
            assertFalse(organized.contains("dart:async"), organized);
            assertTrue(organized.contains("dart:math"), organized);

            client.request("shutdown", null);
            client.notify("exit", null);
        }
    }

    private Path configuredDartExecutable() {
        String configured = System.getProperty("dart.executable", "").trim();
        assumeTrue(!configured.isEmpty(), "set -Ddart.executable=<path-to-dart>");
        Path executable = Path.of(configured).toAbsolutePath().normalize();
        assumeTrue(Files.isRegularFile(executable), "Dart executable does not exist: " + executable);
        return executable;
    }

    private ObjectNode initializeParams() {
        ObjectNode capabilities = JSON.createObjectNode();
        capabilities.withObject("workspace")
                .put("configuration", true)
                .put("applyEdit", true)
                .withObject("workspaceEdit")
                .put("documentChanges", true);
        capabilities.withObject("textDocument")
                .withObject("completion")
                .withObject("completionItem")
                .put("snippetSupport", true);

        ObjectNode params = JSON.createObjectNode();
        params.putNull("processId");
        params.put("rootUri", workspace.toUri().toString());
        params.set("capabilities", capabilities);
        ArrayNode workspaceFolders = JSON.createArrayNode();
        workspaceFolders.add(JSON.createObjectNode()
                .put("uri", workspace.toUri().toString())
                .put("name", workspace.getFileName().toString()));
        params.set("workspaceFolders", workspaceFolders);
        return params;
    }

    private static ObjectNode positionParams(Path file, int line, int character) {
        ObjectNode params = JSON.createObjectNode();
        params.set("textDocument", textDocument(file));
        params.set(
                "position",
                JSON.createObjectNode().put("line", line).put("character", character));
        return params;
    }

    private static ObjectNode textDocument(Path file) {
        return JSON.createObjectNode().put("uri", file.toUri().toString());
    }

    private static ObjectNode codeActionParams(
            Path file,
            JsonNode range,
            JsonNode diagnostics) {
        ObjectNode params = JSON.createObjectNode();
        params.set("textDocument", textDocument(file));
        params.set("range", range);
        params.set(
                "context",
                JSON.createObjectNode().set("diagnostics", diagnostics));
        return params;
    }

    private static ObjectNode pointRange(int line, int character) {
        ObjectNode range = JSON.createObjectNode();
        range.set(
                "start",
                JSON.createObjectNode().put("line", line).put("character", character));
        range.set(
                "end",
                JSON.createObjectNode().put("line", line).put("character", character));
        return range;
    }

    private static ObjectNode sourceActionParams(Path file, int line, int character) {
        return codeActionParams(
                file,
                pointRange(line, character),
                JSON.createArrayNode());
    }

    private static JsonNode findCompletion(JsonNode items, String label) {
        for (JsonNode item : items) {
            if (label.equals(item.path("filterText").asText())
                    || label.equals(item.path("textEdit").path("newText").asText())
                    || item.path("label").asText().startsWith(label + "(")) {
                return item;
            }
        }
        return null;
    }

    private static JsonNode findAction(JsonNode actions, String title) {
        if (!actions.isArray()) {
            return null;
        }
        for (JsonNode action : actions) {
            if (title.equals(action.path("title").asText())) {
                return action;
            }
        }
        return null;
    }

    private static JsonNode findActionContaining(JsonNode actions, String text) {
        if (actions.isArray()) {
            for (JsonNode action : actions) {
                if (action.toString().contains(text)) {
                    return action;
                }
            }
        }
        return null;
    }

    private static JsonNode workspaceTextEdits(JsonNode workspaceEdit, Path file) {
        String uri = file.toUri().toString();
        JsonNode documentChanges = workspaceEdit.path("documentChanges");
        if (documentChanges.isArray()) {
            for (JsonNode documentChange : documentChanges) {
                if (uri.equals(documentChange.path("textDocument").path("uri").asText())) {
                    return documentChange.path("edits");
                }
            }
        }
        JsonNode changes = workspaceEdit.path("changes").path(uri);
        assertTrue(changes.isArray(), () -> "workspace edit has no edits for " + uri + ": " + workspaceEdit);
        return changes;
    }

    private static void assertCapability(
            JsonNode capabilities,
            String name,
            List<String> stderr) {
        JsonNode value = capabilities.path(name);
        assertTrue(
                value.isObject() || value.asBoolean(false),
                () -> "Dart LSP did not advertise " + name + "; stderr: " + stderrSnapshot(stderr));
    }

    private static String stderrSnapshot(List<String> stderr) {
        synchronized (stderr) {
            return String.join(System.lineSeparator(), stderr);
        }
    }

    private static String applyTextEdits(String source, JsonNode editsNode) {
        List<TextEdit> edits = new ArrayList<>();
        for (JsonNode edit : editsNode) {
            JsonNode range = edit.path("range");
            int start = offsetAt(source, range.path("start"));
            int end = offsetAt(source, range.path("end"));
            edits.add(new TextEdit(start, end, edit.path("newText").asText()));
        }
        edits.sort(Comparator.comparingInt(TextEdit::start).reversed());
        StringBuilder result = new StringBuilder(source);
        for (TextEdit edit : edits) {
            result.replace(edit.start(), edit.end(), edit.newText());
        }
        return result.toString();
    }

    private static int offsetAt(String text, JsonNode position) {
        int requestedLine = position.path("line").asInt();
        int requestedCharacter = position.path("character").asInt();
        int offset = 0;
        for (int line = 0; line < requestedLine; line++) {
            int newline = text.indexOf('\n', offset);
            if (newline < 0) {
                throw new AssertionError("TextEdit line is outside the document: " + position);
            }
            offset = newline + 1;
        }
        int result = offset + requestedCharacter;
        if (result > text.length()) {
            throw new AssertionError("TextEdit character is outside the document: " + position);
        }
        return result;
    }

    private static void writeFrame(OutputStream output, JsonNode json) throws IOException {
        byte[] body = JSON.writeValueAsBytes(json);
        output.write(("Content-Length: " + body.length + "\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII));
        output.write(body);
        output.flush();
    }

    private static JsonNode readFrame(InputStream input) throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        int matched = 0;
        byte[] delimiter = {'\r', '\n', '\r', '\n'};
        while (matched < delimiter.length) {
            int value = input.read();
            if (value < 0) {
                throw new IOException("Dart server closed before an LSP header was received");
            }
            header.write(value);
            matched = value == delimiter[matched]
                    ? matched + 1
                    : value == delimiter[0] ? 1 : 0;
            if (header.size() > 32_768) {
                throw new IOException("Dart server returned an oversized LSP header");
            }
        }

        int contentLength = -1;
        for (String line : header.toString(StandardCharsets.US_ASCII).split("\\r\\n")) {
            int separator = line.indexOf(':');
            if (separator > 0 && line.substring(0, separator).equalsIgnoreCase("Content-Length")) {
                contentLength = Integer.parseInt(line.substring(separator + 1).trim());
            }
        }
        if (contentLength < 0) {
            throw new IOException("Dart server returned an LSP frame without Content-Length");
        }
        byte[] body = input.readNBytes(contentLength);
        if (body.length != contentLength) {
            throw new IOException("Dart server closed during an LSP frame");
        }
        return JSON.readTree(body);
    }

    private record TextEdit(int start, int end, String newText) {
    }

    private static final class LspTestClient implements AutoCloseable {
        private final InputStream input;
        private final OutputStream output;
        private final BlockingQueue<JsonNode> inbound = new LinkedBlockingQueue<>();
        private final List<JsonNode> deferred = new ArrayList<>();
        private final List<JsonNode> workspaceEdits = new ArrayList<>();
        private final AtomicReference<Throwable> readerFailure = new AtomicReference<>();
        private final Thread reader;
        private int nextRequestId = 1;
        private volatile boolean closed;

        LspTestClient(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
            reader = Thread.ofPlatform()
                    .name("dart-lsp-smoke-reader")
                    .daemon(true)
                    .start(this::readMessages);
        }

        JsonNode request(String method, JsonNode params) throws Exception {
            int id = nextRequestId++;
            ObjectNode request = JSON.createObjectNode()
                    .put("jsonrpc", "2.0")
                    .put("id", id)
                    .put("method", method);
            if (params != null) {
                request.set("params", params);
            }
            send(request);
            JsonNode response = await(
                    message -> !message.has("method") && message.path("id").asInt(-1) == id,
                    "response to " + method);
            assertFalse(response.has("error"), response::toString);
            JsonNode result = response.get("result");
            assertNotNull(result, "LSP response did not contain result: " + response);
            return result;
        }

        void notify(String method, JsonNode params) throws IOException {
            ObjectNode notification = JSON.createObjectNode()
                    .put("jsonrpc", "2.0")
                    .put("method", method);
            if (params != null) {
                notification.set("params", params);
            }
            send(notification);
        }

        void open(Path file, String text) throws IOException {
            ObjectNode document = JSON.createObjectNode()
                    .put("uri", file.toUri().toString())
                    .put("languageId", "dart")
                    .put("version", 1)
                    .put("text", text);
            notify(
                    "textDocument/didOpen",
                    JSON.createObjectNode().set("textDocument", document));
        }

        void awaitAnalysisIdle() throws Exception {
            await(
                    message -> "$/analyzerStatus".equals(message.path("method").asText())
                            && !message.path("params").path("isAnalyzing").asBoolean(true),
                    "Dart analyzer idle notification");
        }

        JsonNode awaitDiagnostics(Path file, boolean requireNonEmpty) throws Exception {
            String uri = file.toUri().toString();
            JsonNode notification = await(
                    message -> "textDocument/publishDiagnostics".equals(
                                    message.path("method").asText())
                            && uri.equals(message.path("params").path("uri").asText())
                            && (!requireNonEmpty
                                    || !message.path("params").path("diagnostics").isEmpty()),
                    "diagnostics for " + uri);
            return notification.path("params").path("diagnostics");
        }

        void executeCommand(JsonNode action) throws Exception {
            JsonNode command = action.path("command").isObject()
                    ? action.path("command")
                    : action;
            ObjectNode params = JSON.createObjectNode()
                    .put("command", command.path("command").asText());
            if (command.has("arguments")) {
                params.set("arguments", command.path("arguments"));
            }
            assertFalse(params.path("command").asText().isBlank(), action::toString);
            request("workspace/executeCommand", params);
        }

        JsonNode takeLastWorkspaceEdit() {
            return workspaceEdits.isEmpty()
                    ? null
                    : workspaceEdits.remove(workspaceEdits.size() - 1);
        }

        private JsonNode await(Predicate<JsonNode> predicate, String description) throws Exception {
            for (int index = 0; index < deferred.size(); index++) {
                JsonNode message = deferred.get(index);
                if (predicate.test(message)) {
                    deferred.remove(index);
                    return message;
                }
            }

            long deadline = System.nanoTime() + RESPONSE_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                Throwable failure = readerFailure.get();
                if (failure != null && inbound.isEmpty()) {
                    throw new IOException("Dart LSP reader failed while waiting for " + description, failure);
                }
                long remaining = deadline - System.nanoTime();
                JsonNode message = inbound.poll(
                        Math.min(TimeUnit.NANOSECONDS.toMillis(remaining) + 1, 250),
                        TimeUnit.MILLISECONDS);
                if (message == null) {
                    continue;
                }
                if (message.has("method") && message.has("id")) {
                    answerServerRequest(message);
                } else if (predicate.test(message)) {
                    return message;
                } else {
                    deferred.add(message);
                }
            }
            throw new IOException("Timed out waiting for " + description + "; deferred: " + deferred);
        }

        private void answerServerRequest(JsonNode request) throws IOException {
            ObjectNode response = JSON.createObjectNode().put("jsonrpc", "2.0");
            response.set("id", request.get("id"));
            String method = request.path("method").asText();
            if ("workspace/configuration".equals(method)) {
                ArrayNode configuration = JSON.createArrayNode();
                JsonNode items = request.path("params").path("items");
                for (int index = 0; index < items.size(); index++) {
                    configuration.addNull();
                }
                response.set("result", configuration);
            } else if ("workspace/applyEdit".equals(method)) {
                workspaceEdits.add(request.path("params").path("edit").deepCopy());
                response.set("result", JSON.createObjectNode().put("applied", true));
            } else {
                response.putNull("result");
            }
            send(response);
        }

        private synchronized void send(JsonNode message) throws IOException {
            writeFrame(output, message);
        }

        private void readMessages() {
            try {
                while (!closed) {
                    inbound.put(readFrame(input));
                }
            } catch (Throwable failure) {
                if (!closed) {
                    readerFailure.compareAndSet(null, failure);
                }
            }
        }

        @Override
        public void close() {
            closed = true;
            reader.interrupt();
        }
    }
}
