package io.github.vgrytsenko2022.run;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@FunctionalInterface
interface AndroidProcessStarter {
    Process start(
            Path executable,
            Path workingDirectory,
            Map<String, String> environment,
            List<String> arguments) throws IOException;
}
