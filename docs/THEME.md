# Theme

Pinned Flutter 3.44.8: `packages/flutter/lib/src/material/theme.dart`.
[Constructor](https://api.flutter.dev/flutter/material/Theme/Theme.html),
[class](https://api.flutter.dev/flutter/material/Theme-class.html).

## Constructor and Properties

Material palette, order 540. One typed property: required non-null Data
(`ThemeData`), followed by required single Child at constructor order 1.
Flutter Key is not a new editable field; Stable ID remains Designer identity.
The pinned SDK has one Theme constructor, no merge constructor, no callbacks,
builders, animation Duration, Curve or On end.

Data uses the same domain as AnimatedTheme: light, dark, fallback, lightM2,
darkM2, fallbackM2. The first three retain native Material 3 defaults; M2
passes useMaterial3: false. Creation uses light. Null and omission are rejected.
All six factories are non-const; emitted Theme must not be prefixed with const
when its data is a factory call. Const-capable descendants remain supported.

Arbitrary application themes are supported through strict ThemeData references:
current or declared-package imports, top-level or member access, getters,
and zero-argument factories. ThemeData colors, typography, component themes,
extensions and adaptations remain editable in the referenced Dart declaration.
These inner fields are **not individually flattened** into Properties.
There is no unrestricted Dart-expression editor or execution of project code.

The data row uses the shared preset/reference editor and remains the same
property instance across value refreshes. Reopening retains editable values.
Required Child supports atomic existing-child/root wrapping and replacement;
removing or moving away its only child without replacement is rejected.
A detached prototype is not an insertable childless document. No hidden seed
widget is manufactured.

## Dart and Canvas

Theme and ThemeData/factory symbols retain exact material.dart occurrences.
Whole-value static type witnesses also use material.dart. Nullable, dynamic
and wrong-type ThemeData cannot satisfy the strict source proof. Save/reopen
and Undo preserve user-owned definitions.

Canvas mounts real Theme without AnimatedTheme. Data changes apply immediately,
preserving descendant identity. The nearest complete data replaces outer
Material data rather than merging. Native IconTheme, selection-style fallback
and inherited/derived CupertinoTheme behavior are preserved by Flutter Theme.
Theme.of localizes the returned text metrics according to MaterialLocalizations.

Factory presets have exact previews. Source references have presence-only
payloads: the preview displays a diagnostic and uses ThemeData.fallback()
(Material 3 light), **not an inferred parent ThemeData**. Stored data and
generated source are unchanged. The required child remains selectable when
zero-sized. Raw slivers remain outside this box-wrapper placement contract.

## Inventory

192 admitted definitions, 184 typed-property definitions, 7124 writable rows
(7106 outside Scaffold), 159 const-capable definitions, 51 Material entries.
646 BOOLEAN-only rows unchanged. No new Events or builders: 172 native Event
rows across 58 types, 234 callables across 83 types, 48 builders.

Insertion matrix: 33600 candidates, 22348 accepted, 11252 rejected.
Ordinary destinations remain 175, including 156 any-widget destinations.
Required-child wrappers: 36, including 26 root-compatible wrappers.
Formats unchanged: FD 16, Catalog API 15, Canvas model 19, transport 1.

## Verification

Verified on 2026-09-14 in the canonical G: checkout, pinned Flutter 3.44.8.
No running IDE, Flutter SDK, or user Flutter project was modified.

- Full core Java suite passed; selected NetBeans, property, palette, DnD,
  analyzer, source-proof, event and resource regressions passed. Latest fresh
  Surefire reports: 3815 cases in 525 reports, 3741 executed successfully,
  74 conditional skips, zero failures/errors. This is not a claim that the
  unfiltered all-module Java suite or every optional SDK integration test ran.
- Full Canvas suite: 8116 passed, including 17 Theme tests. All six profiles
  in LTR/RTL, immediate updates, child identity, required-slot admission,
  invalid values, inert references, nearest-theme precedence and zero-size
  selection are covered. The 61 AnimatedTheme regressions also passed.
- Flutter analyze: no issues. Release Web Canvas build passed.
- ThemeRealSdkTest executed, not skipped. Six presets, local getters/factories,
  four imported top-level/member getter/factory forms, material.dart proof,
  nullable/dynamic/wrong-type rejection, save/reopen and Undo all passed.
  Generated source passed 26 Flutter runtime tests, including descendant State
  retention, IconTheme, selection fallback and Cupertino inheritance/override.
- AnimatedThemeRealSdkTest was rerun against shared generation changes and
  passed its 21 Flutter runtime cases.
- Source-bundle and actual local Web-artifact checks passed. All four SVGs
  were visually reviewed and passed the icon-registry packaging contract.
- No manual IDE interaction was performed or claimed; the IDE was not restarted.

Logs: ignored `flutter-designer/target/theme-*.log` files. Core command:
`mvn -q -pl flutter-designer -am test`. NetBeans regressions use
`-pl netbeans-plugin -am`; final SDK tests use
`-Dflutter.events.sdk=G:/MyProjects/java/libraries/flutter_windows_3.44.8-stable/flutter`.
The Web resource test uses
`-Dcanvas.runner.web.source=G:/MyProjects/java/project/netbeans-flutter-plugin-starter/flutter-canvas-runner/src/main/flutter`.

Web main.dart.js: 3859361 bytes,
SHA-256 `48d2156fd44590c9e3adba0c71a5e5483f81b7fec1fecaad5eb9a82b52861688`.
Source and Web manifests pin actual bytes without changing their formats.
The generated Web build is not an additional NBM payload.

Packaging commands passed:
`mvn -q -pl netbeans-plugin -am -DskipTests install`,
then `mvn -q -pl netbeans-plugin nbm:cluster`.

The final NBM was inspected: compiled schema/generator/property/evidence classes,
four SVGs and all 40 runner sources match current output and source-manifest
hashes; all 35 local Web files match the packaged Web manifest.
All four development-cluster JARs match the corresponding NBM entries.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8956667 bytes; modified UTC `2026-09-14T08:38:35.4652088Z`;
SHA-256 `dd4b4eea123b667b55c8440195061d348f5ad07bc7eecc8b555797e6d401fc89`.

Next planned palette slice: DefaultTextStyle.
