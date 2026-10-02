export 'canvas_runtime_platform_stub.dart'
    if (dart.library.io) 'canvas_runtime_platform_io.dart'
    if (dart.library.js_interop) 'canvas_runtime_platform_web.dart';
