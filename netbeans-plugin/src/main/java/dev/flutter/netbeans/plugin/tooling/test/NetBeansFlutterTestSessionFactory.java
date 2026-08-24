package dev.flutter.netbeans.plugin.tooling.test;

import dev.flutter.netbeans.plugin.tooling.FlutterTestSessionBridge;
import dev.flutter.netbeans.plugin.tooling.FlutterTestSessionFactory;
import dev.flutter.netbeans.run.FlutterToolCommand;
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
