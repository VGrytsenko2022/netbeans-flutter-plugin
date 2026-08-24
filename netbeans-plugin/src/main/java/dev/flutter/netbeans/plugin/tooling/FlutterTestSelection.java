package dev.flutter.netbeans.plugin.tooling;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.spi.project.SingleMethod;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/** Project-relative Flutter test file and an optional plain test name. */
public record FlutterTestSelection(String relativePath, Optional<String> plainName) {
    private static final Pattern TEST_CALL = Pattern.compile(
            "\\b(?:test|testWidgets)\\s*\\(");
    private static final Pattern TEST_NAME = Pattern.compile(
            "(?s)\\b(?:test|testWidgets)\\s*\\(\\s*(r)?(['\"])((?:\\\\.|(?!\\2).)*)\\2");

    public FlutterTestSelection {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("A project-relative Dart test path is required");
        }
        relativePath = relativePath.replace('\\', '/');
        plainName = plainName == null ? Optional.empty() : plainName
                .map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    public static Optional<FlutterTestSelection> fromContext(
            Path projectRoot,
            Lookup context,
            boolean requireNameAtCaret) {
        Lookup safeContext = context == null ? Lookup.EMPTY : context;
        SingleMethod single = safeContext.lookup(SingleMethod.class);
        if (single != null && single.getFile() != null) {
            return fromFile(
                    projectRoot,
                    single.getFile(),
                    Optional.ofNullable(single.getMethodName()));
        }

        FileObject file = fileFromLookup(safeContext);
        JTextComponent editor = EditorRegistry.lastFocusedComponent();
        if (file == null && editor != null) {
            file = fileFromDocument(editor.getDocument());
        }
        Optional<String> name = Optional.empty();
        if (requireNameAtCaret) {
            if (editor == null || file == null
                    || !file.equals(fileFromDocument(editor.getDocument()))) {
                return Optional.empty();
            }
            name = testNameAt(editor.getDocument(), editor.getCaretPosition());
            if (name.isEmpty()) {
                return Optional.empty();
            }
        }
        return fromFile(projectRoot, file, name);
    }

    static Optional<String> testNameAt(Document document, int caretOffset) {
        if (document == null) {
            return Optional.empty();
        }
        try {
            String text = document.getText(0, document.getLength());
            int offset = Math.max(0, Math.min(caretOffset, text.length()));
            int nearestCall = -1;
            Matcher calls = TEST_CALL.matcher(text);
            while (calls.find() && calls.start() <= offset) {
                nearestCall = calls.start();
            }
            if (nearestCall < 0) {
                return Optional.empty();
            }
            Matcher literal = TEST_NAME.matcher(text);
            while (literal.find()) {
                if (literal.start() == nearestCall) {
                    Optional<String> decoded = literal.group(1) == null
                            ? decodeDartString(literal.group(3))
                            : Optional.of(literal.group(3));
                    return decoded.filter(value -> !value.isBlank());
                }
                if (literal.start() > nearestCall) {
                    break;
                }
            }
            return Optional.empty();
        } catch (javax.swing.text.BadLocationException ex) {
            return Optional.empty();
        }
    }

    private static Optional<FlutterTestSelection> fromFile(
            Path projectRoot,
            FileObject file,
            Optional<String> plainName) {
        if (file == null || !"dart".equalsIgnoreCase(file.getExt())) {
            return Optional.empty();
        }
        java.io.File diskFile = FileUtil.toFile(file);
        if (diskFile == null) {
            return Optional.empty();
        }
        Path root = projectRoot.toAbsolutePath().normalize();
        Path path = diskFile.toPath().toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            return Optional.empty();
        }
        String relative = root.relativize(path).toString().replace('\\', '/');
        return Optional.of(new FlutterTestSelection(relative, plainName));
    }

    private static FileObject fileFromLookup(Lookup context) {
        FileObject file = context.lookup(FileObject.class);
        if (file != null) {
            return file;
        }
        DataObject dataObject = context.lookup(DataObject.class);
        return dataObject == null ? null : dataObject.getPrimaryFile();
    }

    private static FileObject fileFromDocument(Document document) {
        if (document == null) {
            return null;
        }
        Object description = document.getProperty(Document.StreamDescriptionProperty);
        if (description instanceof FileObject file) {
            return file;
        }
        if (description instanceof DataObject dataObject) {
            return dataObject.getPrimaryFile();
        }
        return null;
    }

    private static Optional<String> decodeDartString(String value) {
        StringBuilder decoded = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '$') {
                // An interpolated test name cannot be reconstructed statically.
                return Optional.empty();
            }
            if (current != '\\') {
                decoded.append(current);
                continue;
            }
            if (++index >= value.length()) {
                return Optional.empty();
            }
            char escape = value.charAt(index);
            switch (escape) {
                case 'n' -> decoded.append('\n');
                case 'r' -> decoded.append('\r');
                case 't' -> decoded.append('\t');
                case 'b' -> decoded.append('\b');
                case 'f' -> decoded.append('\f');
                case 'v' -> decoded.append('\u000B');
                case '\\', '\'', '"', '$' -> decoded.append(escape);
                case 'x' -> {
                    int end = index + 3;
                    if (end > value.length()) {
                        return Optional.empty();
                    }
                    Integer codePoint = hex(value.substring(index + 1, end));
                    if (codePoint == null) {
                        return Optional.empty();
                    }
                    decoded.append((char) codePoint.intValue());
                    index = end - 1;
                }
                case 'u' -> {
                    int start = index + 1;
                    int end;
                    if (start < value.length() && value.charAt(start) == '{') {
                        end = value.indexOf('}', start + 1);
                        if (end < 0 || end == start + 1 || end - start > 7) {
                            return Optional.empty();
                        }
                        start++;
                        index = end;
                    } else {
                        end = start + 4;
                        if (end > value.length()) {
                            return Optional.empty();
                        }
                        index = end - 1;
                    }
                    Integer codePoint = hex(value.substring(start, end));
                    if (codePoint == null || !Character.isValidCodePoint(codePoint)) {
                        return Optional.empty();
                    }
                    decoded.appendCodePoint(codePoint);
                }
                default -> {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(decoded.toString());
    }

    private static Integer hex(String value) {
        try {
            return Integer.valueOf(value, 16);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
