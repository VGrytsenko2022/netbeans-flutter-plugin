package io.github.vgrytsenko2022.plugin.tooling;

import io.github.vgrytsenko2022.run.FlutterToolCommand;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.netbeans.api.project.Project;

/** Creates one project-owned Test Results session for one machine test process. */
@FunctionalInterface
public interface FlutterTestSessionFactory {
    FlutterTestSessionBridge create(
            Project project,
            Path projectRoot,
            String displayName,
            FlutterToolCommand command,
            Consumer<FlutterToolCommand> rerun);
}
