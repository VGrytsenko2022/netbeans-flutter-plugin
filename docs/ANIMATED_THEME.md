# AnimatedTheme

Pinned Flutter 3.44.8: `packages/flutter/lib/src/material/theme.dart`.
[Constructor](https://api.flutter.dev/flutter/material/AnimatedTheme/AnimatedTheme.html),
[class](https://api.flutter.dev/flutter/material/AnimatedTheme-class.html).

## Complete constructor slice

Material palette, order 530. Four Properties: data, curve, durationUs, onEnd.
Required single Child at constructor order 4. Flutter Key is not a new editable
property; Stable ID remains Designer identity, not an inferred application Key.

- Data: required non-null ThemeData. Six closed SDK factory presets:
  light, dark, fallback, lightM2, darkM2, fallbackM2. The first three preserve
  the pinned Material 3 factory defaults; M2 variants pass useMaterial3: false.
  Designer creation uses light. A strict ThemeData reference, getter, member,
  or zero-argument factory supports arbitrary application ThemeData.
- Curve: all 43 pinned Curves presets or strict Curve reference/factory;
  optional, non-null, linear by default.
- Duration: optional nonnegative portable integer microseconds or strict
  non-null Duration reference/factory. Omission uses kThemeAnimationDuration,
  exactly 200000 microseconds. Zero completes immediately. Restore Default
  removes the argument; explicit null is rejected.
- On end: optional nullable VoidCallback via the shared Events editor and
  source lifecycle. Native completion fires on completed changes, not initial
  mount or an unchanged theme. Canvas does not execute the handler.

Required Child uses atomic existing-child/root wrapping and slot replacement.
Detached prototypes intentionally have an empty required slot for assembly;
they cannot be inserted as an invalid childless document. Removal/movement of
the required child without replacement is rejected. No hidden child is generated.

## ThemeData scope and source proof

The constructor's whole ThemeData is supported, including user-defined colors,
typography, component themes, extensions and adaptations through a referenced
ThemeData. These inner ThemeData fields are **not** individually flattened into
this widget's Properties sheet; there is no arbitrary Dart-expression field or
automatic execution of project factories in Designer.

Preset factory calls are emitted as ThemeData.light()/dark()/fallback(), with
the explicit false argument for M2 variants. These factories are not const:
the generator keeps const-capable children but does not emit an invalid const
AnimatedTheme around a runtime ThemeData factory.

Both the ThemeData class and selected factory have exact generated symbol
occurrences. Whole ThemeData expressions are checked using a qualified
material.dart proof import, not widgets.dart. Duration occurrences use trusted
dart:core proof. Wrong types, nullable ThemeData and dynamic ThemeData expressions
are rejected. Source-owned definitions are not overwritten on save/reopen/Undo.

## Canvas semantics and limits

Canvas mounts native AnimatedTheme/ThemeDataTween/Theme. Initial mount immediately
applies the target. Changes interpolate with ThemeData.lerp; its discrete fields
(such as brightness or Material version) follow SDK behavior. New targets retarget
from the current interpolated theme. The nearest data replaces the parent Theme,
not a copyWith merge. Native Theme propagates its IconTheme, selection defaults
and Cupertino theme behavior; child identity is retained during the tween.

Literal factory presets render exactly. Project references are presence-only
and explicitly labeled: unknown Data uses ThemeData.fallback() (light Material 3),
not an inherited parent data guess; unknown Curve uses linear, Duration 200 ms,
On end is inert. Stored values and generated Dart are unchanged. Source-owned
custom themes cannot be previewed by executing them. Zero-size required children
keep Designer selection handles.

## Inventory

191 admitted definitions; 183 typed-property definitions; 7123 writable rows,
7105 outside Scaffold; 158 const-capable definitions; 50 Material entries;
646 BOOLEAN-only rows. Native Events: 172 rows across 58 widget types.
All callables: 234 across 83 types; 48 builders.

Insertion matrix: 33425 candidates, 22192 accepted, 11233 rejected.
Ordinary destinations remain 175 (156 any-widget destinations).
Required-child wrappers: 35, including 25 root-compatible wrappers.
Formats remain FD 16 / Catalog API 15 / Canvas model 19 / transport 1.

## Verification

Verified on 2026-09-14 in the canonical G: checkout, pinned Flutter 3.44.8.
No user project, Flutter SDK or running IDE was modified.

- Full Flutter Canvas runner: 8099 tests passed, including 61 AnimatedTheme
  cases (six presets, LTR/RTL, interpolation, retargeting, all 43 curves,
  typed-reference diagnostics and zero-size selection).
- Flutter analyze: no issues. Release Web Canvas build passed.
- Full core tests plus the selected NetBeans/analyzer/property/palette/event
  and resource regressions: 3808 cases in 521 fresh Surefire reports;
  3734 executed successfully, 74 conditional tests skipped, zero failures/errors.
  This is not an unfiltered all-module Java-suite claim.
- AnimatedThemeRealSdkTest executed (not skipped): all six generated factories,
  strict ThemeData/Curve/Duration/On end references, wrong-type/nullable/dynamic
  rejection, save/reopen/Undo and handler scaffolding passed. Its generated
  Flutter suite passed 21 runtime cases.
- Source-bundle and Web-artifact validation passed, including the actual local
  release build. Four light/dark 16/32-pixel SVGs were visually reviewed; registry
  packaging checks passed.
- Development IDE interaction was not manually repeated; no IDE was restarted.

Commands: `mvn -q -pl flutter-designer -am test`; selected NetBeans regressions
with `-pl netbeans-plugin -am`; the artifact/SDK pass with
`-Dflutter.events.sdk=G:/MyProjects/java/libraries/flutter_windows_3.44.8-stable/flutter`
and `-Dcanvas.runner.web.source=G:/MyProjects/java/project/netbeans-flutter-plugin-starter/flutter-canvas-runner/src/main/flutter`.
Build logs are in the ignored `flutter-designer/target/ath-*.log` files.

Web main.dart.js: 3858717 bytes;
SHA-256 `05c6ae005e6124931a7235b8e68f58c80e096797261bb0a04bde730ffebf6699`.
The refreshed source and Web manifests preserve their formats and pin the actual
bytes; the generated Web build is not an extra NBM payload.

Packaging: `mvn -q -pl netbeans-plugin -am -DskipTests install` and
`mvn -q -pl netbeans-plugin nbm:cluster` both passed. The final NBM was inspected:
schema/generator/property/evidence classes and all four icons match compiled
output; all 40 packaged runner sources match source-manifest bytes; all 35
local Web files match the packaged Web manifest. All four development-cluster
JARs match the corresponding NBM entries.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
— 8953094 bytes, modified UTC `2026-09-14T08:12:13.4342657Z`;
SHA-256 `b76ea6fd55cee60e9e9a0fde12351832ca020174344f19058172cc01892dbefb`.

Next planned palette slice: Theme, the non-animated counterpart.
