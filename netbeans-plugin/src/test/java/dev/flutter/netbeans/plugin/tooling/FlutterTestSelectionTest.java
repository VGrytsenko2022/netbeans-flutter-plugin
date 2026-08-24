package dev.flutter.netbeans.plugin.tooling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.text.PlainDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.Lookups;

class FlutterTestSelectionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesAProjectRelativeDartTestPath() throws Exception {
        Path projectRoot = Files.createDirectories(temporaryDirectory.resolve("project"));
        Path testFile = createDartFile(projectRoot.resolve("test/widgets/login_test.dart"));

        FlutterTestSelection selection = FlutterTestSelection.fromContext(
                        projectRoot,
                        Lookups.singleton(fileObject(testFile)),
                        false)
                .orElseThrow();

        assertEquals("test/widgets/login_test.dart", selection.relativePath());
        assertTrue(selection.plainName().isEmpty());
    }

    @Test
    void rejectsADartFileOutsideTheProject() throws Exception {
        Path projectRoot = Files.createDirectories(temporaryDirectory.resolve("project"));
        Path outsideFile = createDartFile(temporaryDirectory.resolve("outside_test.dart"));

        assertTrue(FlutterTestSelection.fromContext(
                projectRoot,
                Lookups.singleton(fileObject(outsideFile)),
                false).isEmpty());
    }

    @Test
    void findsTheNearestTestNameAtTheCaret() throws Exception {
        PlainDocument document = document("""
                void main() {
                  test('adds two values', () {
                    expect(1 + 1, 2);
                  });
                }
                """);
        int caret = text(document).indexOf("expect");

        assertEquals(
                "adds two values",
                FlutterTestSelection.testNameAt(document, caret).orElseThrow());
    }

    @Test
    void findsATestWidgetsNameAndUnescapesItsQuoteAtTheCaret() throws Exception {
        PlainDocument document = document("""
                void main() {
                  testWidgets("renders the \\"Save\\" button", (tester) async {
                    await tester.pumpWidget(const App());
                  });
                }
                """);
        int caret = text(document).indexOf("pumpWidget");

        assertEquals(
                "renders the \"Save\" button",
                FlutterTestSelection.testNameAt(document, caret).orElseThrow());
    }

    @Test
    void preservesBackslashesInARawTestName() throws Exception {
        PlainDocument document = document("""
                void main() {
                  test(r'matches \\d+', () {
                    expect(true, isTrue);
                  });
                }
                """);

        assertEquals(
                "matches \\d+",
                FlutterTestSelection.testNameAt(
                        document,
                        text(document).indexOf("expect")).orElseThrow());
    }

    @Test
    void returnsEmptyWhenNoLiteralTestNamePrecedesTheCaret() throws Exception {
        PlainDocument document = document("""
                void main() {
                  const dynamicName = 'generated';
                  test(dynamicName, () {});
                }
                """);

        assertTrue(FlutterTestSelection.testNameAt(
                document,
                document.getLength()).isEmpty());
    }

    private static Path createDartFile(Path path) throws Exception {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, "void main() {}\n");
    }

    private static FileObject fileObject(Path path) {
        FileUtil.refreshFor(path.toFile());
        FileObject file = FileUtil.toFileObject(path.toFile());
        if (file == null) {
            throw new AssertionError("No FileObject for " + path);
        }
        return file;
    }

    private static PlainDocument document(String text) throws Exception {
        PlainDocument document = new PlainDocument();
        document.insertString(0, text, null);
        return document;
    }

    private static String text(PlainDocument document) throws Exception {
        return document.getText(0, document.getLength());
    }
}
