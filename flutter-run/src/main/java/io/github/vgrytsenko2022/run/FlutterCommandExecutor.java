package io.github.vgrytsenko2022.run;

import io.github.vgrytsenko2022.api.ProcessResult;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

@FunctionalInterface
interface FlutterCommandExecutor {
    ProcessResult execute(Path workingDirectory, Duration timeout, String... arguments)
            throws IOException, InterruptedException;
}
