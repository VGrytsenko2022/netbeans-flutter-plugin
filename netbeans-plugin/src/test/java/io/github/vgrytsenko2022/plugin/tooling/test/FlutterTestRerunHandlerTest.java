package io.github.vgrytsenko2022.plugin.tooling.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.run.FlutterToolCommand;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.modules.gsf.testrunner.api.RerunType;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.Testcase;

class FlutterTestRerunHandlerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void buildsAnAnchoredLiteralRegexAndGroupsSelectionsByPath() throws Exception {
        TestSession session = session();
        FlutterNetBeansTestcase second = testcase(
                2, "uses [a-z].* \\ path", "test/b_test.dart", session);
        FlutterNetBeansTestcase first = testcase(
                1, "adds (1+1)?", "test/a_test.dart", session);
        FlutterNetBeansTestcase duplicateName = testcase(
                3, "adds (1+1)?", "test/a_test.dart", session);
        Testcase unrelated = new Testcase("other", "other", session);

        FlutterTestRerunHandler.SelectedRequest request =
                FlutterTestRerunHandler.selectedRequest(
                        Set.of(second, first, duplicateName, unrelated));

        assertEquals(2, request.targets().size());
        FlutterTestRerunHandler.SelectedTarget firstTarget = request.targets().get(0);
        assertEquals("test/a_test.dart", firstTarget.projectRelativePath());
        assertEquals(List.of("adds (1+1)?"), firstTarget.testNames());
        Pattern firstPattern = Pattern.compile(firstTarget.exactNamePattern());
        assertTrue(firstPattern.matcher("adds (1+1)?").matches());
        assertFalse(firstPattern.matcher("prefix adds (1+1)?").matches());

        FlutterTestRerunHandler.SelectedTarget secondTarget = request.targets().get(1);
        Pattern secondPattern = Pattern.compile(secondTarget.exactNamePattern());
        assertTrue(secondPattern.matcher("uses [a-z].* \\ path").matches());
        assertFalse(secondPattern.matcher("uses azzz path").matches());
    }

    @Test
    void invokesAllAndSelectedCallbacksOnlyWhileEnabled() throws Exception {
        FlutterToolCommand original = FlutterToolCommand.test("test");
        AtomicReference<FlutterTestRerunHandler.RerunRequest> callback =
                new AtomicReference<>();
        FlutterTestRerunHandler handler = new FlutterTestRerunHandler(
                original, callback::set);

        assertFalse(handler.enabled(RerunType.ALL));
        handler.setEnabled(true);
        handler.rerun();

        FlutterTestRerunHandler.AllRequest all = assertInstanceOf(
                FlutterTestRerunHandler.AllRequest.class, callback.get());
        assertEquals(original, all.command());
        assertFalse(handler.enabled(RerunType.ALL));

        callback.set(null);
        handler.rerun();
        assertEquals(null, callback.get());

        handler.setEnabled(true);
        handler.rerun(Set.of(testcase(
                7, "opens menu", "test/menu_test.dart", session())));

        FlutterTestRerunHandler.SelectedRequest selected = assertInstanceOf(
                FlutterTestRerunHandler.SelectedRequest.class, callback.get());
        assertEquals("test/menu_test.dart", selected.targets().getFirst().projectRelativePath());
        assertEquals(List.of("opens menu"), selected.targets().getFirst().testNames());
        assertFalse(handler.enabled(RerunType.CUSTOM));
    }

    private TestSession session() throws Exception {
        TestProject project = TestProject.create(temporaryDirectory.resolve("project"));
        return new TestSession("Flutter tests", project, TestSession.SessionType.TEST);
    }

    private static FlutterNetBeansTestcase testcase(
            long id,
            String name,
            String path,
            TestSession session) {
        return new FlutterNetBeansTestcase(
                id, name, session, Optional.of(path), Optional.empty());
    }
}
