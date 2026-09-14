# DefaultTextStyleTransition

Pinned Flutter 3.44.8: `packages/flutter/lib/src/widgets/transitions.dart`.
[Constructor](https://api.flutter.dev/flutter/widgets/DefaultTextStyleTransition/DefaultTextStyleTransition.html),
[class](https://api.flutter.dev/flutter/widgets/DefaultTextStyleTransition-class.html).

## Complete API projection

Layout palette, order 480, following AnimatedSwitcher. One const-capable
unnamed constructor, required single box Child and **36 writable properties**:
five direct arguments (Style animation, Text align, Soft wrap, Overflow,
Max lines) plus all 31 shared local TextStyle fields.

The style argument is required non-null `Animation<TextStyle>`, not TextStyle,
Listenable, ValueListenable<TextStyle> or Animation<TextStyle?>. Current-library
and declared-package root/member references, getters and zero-argument
factories are supported with strict analyzer proof. Nullable outer animations,
dynamic, wrong generic arguments and plain TextStyle are rejected.

The Local preset stores all shared TextStyle fields and emits
`AlwaysStoppedAnimation<TextStyle>` around the complete local TextStyle.
This is an exact constant animation, **not** an implicit transition on edits.
Color/Paint alternatives, theme typography token, family/fallback/package,
font metrics, locale, shadows, features/variations, decorations, style overflow
and debug label use the existing typed editors and validation.
A nullable Material text-theme token gets a non-null TextStyle fallback before
being wrapped in the animation. Literal-only stopped styles retain const.

The source animation owns its controller, timing, status listeners, lifetime
and disposal. The constructor does not expose Duration, Curve, On end, Builder,
Text width basis or Text height behavior. Its inherited listenable getter is
the same Style animation, not a second argument. No callbacks or animation
controller code are silently generated. All actual constructor arguments are
covered; no named constructor branches exist. Flutter Key is not a new editable
field; Stable ID remains Designer identity.

Max lines accepts a positive portable integer, int? source reference, explicit
null or unset. Null/unset permits all lines. Text align is nullable, while Soft
wrap and Overflow are non-null optional arguments with native true/clip defaults.
The transition replaces outer DefaultTextStyle settings; its internal
DefaultTextStyle uses parent textWidthBasis and null textHeightBehavior.
A separate DefaultTextHeightBehavior ancestor can still supply Text behavior.

## Editing, placement and source

Changing Style to an animation reference clears local style leaves atomically.
Editing a local leaf selects Local stopped style. Color/Paint alternatives
remain atomic. Properties and groups retain identity across value refreshes;
reopening restores editable values. Boolean values use the global centered
checkbox renderer/editor and preserve permitted unset states.

Required-child/root wrapping and replacement use existing reviewed commands.
Removing the only child without replacement is rejected. Detached childless
prototypes are not insertable documents; no hidden seed widget is manufactured.
Raw slivers remain outside this box-wrapper projection.

The generator retains exact constructor, AlwaysStoppedAnimation and TextStyle
symbol occurrences through the selected Flutter Widgets/Material umbrella.
Source animations require strict Animation<TextStyle> proof; int? Max lines
uses the shared nullable proof. Type proof does not execute a getter or prove
its runtime value: project Max lines references must still return null or a
positive integer, and the source owns animation interpolation constraints.
Save/reopen and Undo preserve user-owned code.
This adds no unrestricted Dart-expression editor.

## Canvas

Canvas mounts real DefaultTextStyleTransition. Its Local branch uses the exact
AlwaysStoppedAnimation<TextStyle> value, so edits apply immediately and
preserve native State and descendant identity. Zero-sized text remains
selectable, and required-child drops target the occupied child slot correctly.

Project animations and Max lines references remain presence-only and are
never executed in isolated Canvas. A diagnostic names the unavailable fields:
Style falls back to a stopped empty TextStyle; unknown Max lines previews as
null. Stored values and generated Dart retain the original references.
Live project animation behavior is verified in generated-code SDK tests, not
simulated by invoking project code inside Canvas.

## Inventory

195 admitted definitions, 187 with typed Properties, 7242 writable rows
(7224 outside Scaffold), 161 const-capable definitions. Layout contains 56
entries; Basic remains 26 and Material 51. BOOLEAN-only rows: 664; BOOLEAN/NULL
rows: 51. Events remain 172 rows across 58 types, 234 callables across 83 types
and 48 builders.

Insertion matrix: 34125 candidates, 22816 accepted, 11309 rejected. Ordinary
destinations remain 175, including 156 any-widget destinations. Required-child
wrappers: 39, including 29 root-compatible wrappers.
Formats unchanged: FD 16, Catalog API 15, Canvas model 19, transport 1.

## Verification

Verified on 2026-09-14 in the canonical G: checkout, pinned Flutter 3.44.8.
No running IDE, Flutter SDK or user Flutter project was modified.

- Full core Java suite and selected NetBeans Properties, palette, DnD, Events,
  catalog/capability, source-proof and resource regressions passed.
  Latest fresh Surefire reports: 3829 cases in 533 reports, 3754 executed
  successfully, 75 conditional skips, zero failures/errors. This is not
  a claim that the unfiltered all-module suite or all optional SDK gates ran.
- Full Canvas suite: 8146 passed, including 11 new tests for domains, all
  paragraph arguments, stopped-style/ancestor semantics, RTL, full shared
  complex TextStyle, native State/child identity, inert references, occupied
  required-slot drops and zero-size recovery.
- DefaultTextStyleTransitionRealSdkTest executed, not skipped. All eight
  local/imported root/member getter/factory forms passed, along with stopped
  local/theme styles, nullable paragraph values, strict generic/nullable/
  dynamic/wrong-type rejection, symbol evidence, save/reopen and Undo.
  Its generated Dart passed 26 Flutter runtime cases: 24 profile/RTL cases,
  source replacement/listener detachment/disposal, and actual
  AnimationController/TextStyleTween intermediate frames with State retention.
- DefaultTextStyleRealSdkTest and AnimatedDefaultTextStyleRealSdkTest also
  executed successfully as shared-style regressions (32 and 10 generated
  Flutter runtime cases respectively).
- Flutter analyze: no issues. Release Web Canvas build passed.
- Actual source-bundle/Web-artifact contracts and icon registry checks passed.
  All four new SVG variants were rendered and visually reviewed.
- No manual IDE interaction was performed or claimed; the IDE was not restarted.

Logs: ignored `flutter-designer/target/dtst-*.log` files; the final new-widget
SDK run is `dtst-sdk-final.log`. Core:
`mvn -q -pl flutter-designer -am test`. NetBeans tests use
`-pl netbeans-plugin -am`; SDK cases explicitly use
`-Dflutter.events.sdk=G:/MyProjects/java/libraries/flutter_windows_3.44.8-stable/flutter`.
Actual Web tests use
`-Dcanvas.runner.web.source=G:/MyProjects/java/project/netbeans-flutter-plugin-starter/flutter-canvas-runner/src/main/flutter`.

Web main.dart.js: 3864201 bytes,
SHA-256 `c923e3904fc27550a76b7a431c25882ea3515ee27fcbda809c2abdec4d7fdf71`.
Source and Web manifests were refreshed from actual byte lengths and hashes
without changing their formats. The local generated Web build is not an
additional NBM payload.

Packaging commands passed:
`mvn -q -pl netbeans-plugin -am -DskipTests install`, then
`mvn -q -pl netbeans-plugin nbm:cluster`.

Final inspection matched current compiled schema/generator/editor/evidence
classes, all four SVG variants and all 40 runner sources against the source
manifest. The packaged Web manifest matches all 35 local Web files. All four
development-cluster JARs match the NBM. The whitespace diff check passed.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8971467 bytes, built 2026-09-14T09:46:42.7846279Z.
SHA-256: `873feb27504769d3c628e1689a9b768640dbd28a3bb4ce4e587a051834345bc2`.

Follow-up implemented: [FadeTransition and SliverFadeTransition](FADE_TRANSITION.md),
with their own Animation<double> contract, separate from this text-style transition.
