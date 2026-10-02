package io.github.vgrytsenko2022.plugin.tooling.test;

import io.github.vgrytsenko2022.plugin.tooling.FlutterTestSessionBridge;
import io.github.vgrytsenko2022.plugin.tooling.FlutterTestSessionFactory;
import io.github.vgrytsenko2022.run.FlutterToolCommand;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.netbeans.api.project.Project;
import org.openide.util.lookup.ServiceProvider;

/** Lookup service that creates standard Test Results sessions for Flutter tests. */
@ServiceProvider(service = FlutterTestSessionFactory.class)
public final class NetBeansFlutterTestSessionFactory implements FlutterTestSessionFactory {
    public NetBeansFlutterTestSessionFactory() {
    }

    @Override
    public FlutterTestSessionBridge create(
            Project project,
            Path projectRoot,
            String displayName,
            FlutterToolCommand command,
            Consumer<FlutterToolCommand> rerun) {
        return new NetBeansFlutterTestSession(
                project,
                projectRoot,
                displayName,
                command,
                rerun);
    }
}
