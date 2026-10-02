package io.github.vgrytsenko2022.plugin.tooling;

import io.github.vgrytsenko2022.api.FlutterSdk;
import java.io.IOException;

/** Injectable Flutter SDK lookup used by the project tooling controller. */
@FunctionalInterface
interface FlutterSdkResolver {
    FlutterSdk resolve(String operation) throws IOException;
}
