package dev.flutter.netbeans.run;

import dev.flutter.netbeans.api.ProcessResult;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@FunctionalInterface
interface AndroidCommandExecutor {
    ProcessResult execute(
            Path executable,
            Path workingDirectory,
            Duration timeout,
            Map<String, String> environment,
            String standardInput,
            List<String> arguments) throws IOException, InterruptedException;
}
