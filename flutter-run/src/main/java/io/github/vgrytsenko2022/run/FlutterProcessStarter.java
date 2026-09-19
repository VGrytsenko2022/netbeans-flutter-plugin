package io.github.vgrytsenko2022.run;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
interface FlutterProcessStarter {
    Process start(Path workingDirectory, List<String> arguments) throws IOException;
}
