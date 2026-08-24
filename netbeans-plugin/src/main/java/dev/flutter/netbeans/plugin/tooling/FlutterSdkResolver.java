package dev.flutter.netbeans.plugin.tooling;

import dev.flutter.netbeans.api.FlutterSdk;
import java.io.IOException;

/** Injectable Flutter SDK lookup used by the project tooling controller. */
@FunctionalInterface
interface FlutterSdkResolver {
    FlutterSdk resolve(String operation) throws IOException;
}
