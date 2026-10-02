# Windows WebView2 adapter build

The packaged adapter is a reproducible Windows x64 Release build. Its source
ABI v3 is declared in `nb_webview2_host.h`; Java verifies that ABI after loading.

Pinned inputs:

- Microsoft NuGet package: `Microsoft.Web.WebView2` `1.0.4191.47`
- package SHA-256: `f492bbf547d0da329553b6727435b677579b1e9f91cc9e4a1ad029366d5f23d0`
- minimum compatible Runtime: `100.0.1185.39` (also set explicitly on the
  native environment options; COM interface probes remain authoritative)
- CMake 3.25 or newer
- Visual Studio x64 C++ toolchain with the Windows SDK

After independently verifying and extracting the NuGet package, configure and
build from the repository root:

```powershell
$webView2Sdk = 'C:\absolute\path\to\the\extracted\package'
cmake -S netbeans-plugin/src/main/native/webview2/windows `
  -B netbeans-plugin/target/webview2-native-build `
  -A x64 `
  -DWEBVIEW2_SDK_ROOT="$webView2Sdk" `
  -DNBWV2_BUILD_SMOKE=ON
cmake --build netbeans-plugin/target/webview2-native-build `
  --config Release `
  --target nb_flutter_webview2_host nb_flutter_webview2_smoke
```

Repeat the Release build in a second clean build directory and require the two
adapter DLLs to be byte-identical. Copy only the reproduced DLL into the
`win-x64` resource directory, then update its exact size/SHA-256 in both
`native.manifest` and `PluginPackageMetadataIT`. The manifest-pinned extraction
also validates PE32+ AMD64 structure, required exports and the expected
`WebView2Loader.dll` imports before JNA loads native code.

The opt-in native smoke accepts a validated Flutter Web artifact and a fresh
user-data directory. It requires the exact authenticated bridge-ready envelope,
performs a real NBFC hello round trip with frame-digest verification, validates
controller bounds/visibility through native read-back, invokes focus, fences
and reparents away from the AWT parent before peer destruction, attempts a
one-millisecond bounded destroy and verifies exact-handle retention if that
deadline expires, verifies a matching-PID browser resource-release
event for both normal and failed exit kinds, and exercises close-during-start
before proving deadline-bounded close:

```powershell
netbeans-plugin/target/webview2-native-build/Release/nb_flutter_webview2_smoke.exe `
  flutter-canvas-runner/src/main/flutter/build/web `
  netbeans-plugin/target/webview2-smoke-udf
```

Do not commit the build directory or smoke user-data directory.
