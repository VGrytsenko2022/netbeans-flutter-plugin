# DefaultTextStyle and DefaultTextStyle.merge

Pinned Flutter 3.44.8: `packages/flutter/lib/src/widgets/text.dart`.
[Constructor](https://api.flutter.dev/flutter/widgets/DefaultTextStyle/DefaultTextStyle.html),
[static merge method](https://api.flutter.dev/flutter/widgets/DefaultTextStyle/merge.html),
[fallback sentinel](https://api.flutter.dev/flutter/widgets/DefaultTextStyle/DefaultTextStyle.fallback.html).

## Complete insertable API projection

Two Basic palette entries, orders 240 and 250, after ImageIcon. Each has 41
writable rows and a required single box Child. These are seven direct
properties, 31 shared local TextStyle fields and three local TextHeightBehavior
fields. Child is the slot, not an extra scalar property. Flutter Key is not a
new editable property; Stable ID remains Designer identity.

The ordinary constructor requires non-null Style, initially Local TextStyle.
The merge entry is the SDK's non-const **static method**, not a named const
constructor. All its seven scalar arguments are optional nullable overrides;
the initial prototype omits Style. The generated invocation and exact merge
symbol retain analyzer evidence. Both support atomic wrapping of an existing
child/root, replacement, save/reopen and Undo; no invalid childless widget or
hidden seed child is inserted. Raw slivers are outside this box projection.

DefaultTextStyle.fallback is deliberately not a palette entry: the SDK
documents it as a sentinel that cannot be inserted into a widget tree, and
its constructor supplies an invalid _NullWidget child. No usable insertable
constructor branch is omitted. Neither entry has callbacks, builders, curve,
duration or completion events.

## Property semantics

- Style accepts Local TextStyle or a strict whole TextStyle reference. Merge
  also accepts TextStyle?, explicit null and unset.
- All 31 shared TextStyle leaves are available: Material text-theme token,
  inherit, colors and Paint, family/fallback/package, size/weight/style,
  letter/word spacing, baseline/height/leading distribution, locale subtags,
  shadows, font features/variations, decoration components/color/style/
  thickness, overflow and debug label.
- Text align, soft wrap, paragraph overflow, positive Max lines and text width
  basis retain SDK defaults/nullability. Max lines also accepts int? source
  references; TextHeightBehavior accepts a whole nullable reference or its
  three local fields.
- Ordinary DefaultTextStyle replaces the inherited defaults immediately:
  empty local style, softWrap true, clip overflow, parent width basis and no
  line limit. It does not silently merge the outer DefaultTextStyle.
- Merge resolves the parent at its actual insertion context. Null/unset means
  inherit, **including Max lines**. To remove a parent's line limit use the
  ordinary constructor, not merge with null.
- A merge local style overlays the parent's style; inherit false replaces it.
  A supplied TextHeightBehavior replaces that whole object: omitted local
  leaves use TextHeightBehavior defaults, not individual ancestor leaves.
  With no DefaultTextStyle height override, Text can still inherit the separate
  DefaultTextHeightBehavior widget; clearing this override does not disable it.
- Switching whole Style to reference/null/unset clears conflicting local
  leaves atomically. Editing a local leaf selects Local TextStyle. Whole
  height behavior and its local fields similarly exclude one another.
  Color/Paint alternatives retain the same shared atomic editor behavior.
- Direct Style cannot be reset to null/unset; merge can. Boolean values use
  the global centered checkbox editor while permitted unset/null is retained.
  Property identities and groups are stable during value refresh; reopening
  restores editable fields.

Whole objects support current-library or declared-package imports, root/member
getters and zero-argument factories. Analyzer proof rejects wrong, dynamic and
inappropriately nullable types. Arbitrary object implementations remain
source-owned; no unrestricted source-expression editor is introduced.

## Canvas and source

Canvas mounts real DefaultTextStyle or DefaultTextStyle.merge, with no
Designer animation wrapper. Local style/paragraph changes apply immediately
and preserve child identity. Required zero-sized content remains selectable,
and dropping on an occupied child follows the shared wrapper contract.

Project-owned Style, Max lines and TextHeightBehavior references are inert,
presence-only in Canvas. A diagnostic names the unavailable values. Ordinary
preview uses empty TextStyle, no maxLines and no whole height override; merge
inherits those fields from the parent. The saved model and generated Dart
retain the actual references. A Material text-theme token uses a non-null
fallback for the ordinary constructor; merge preserves nullable token
semantics. Flutter's internal Builder for merge is not an editable Event.

## Inventory

194 admitted definitions, 186 typed-property definitions, 7206 writable rows
(7188 outside Scaffold), 160 const-capable definitions. Basic contains 26 entries;
Material remains 51. There are 659 BOOLEAN-only rows and 51 BOOLEAN/NULL rows.
Events remain 172 rows across 58 types, 234 callables across 83 types and 48 builders.

Insertion matrix: 33950 candidates, 22660 accepted, 11290 rejected. Ordinary
destinations remain 175, including 156 any-widget destinations. Required-child
wrappers: 38, including 28 root-compatible wrappers.
Formats unchanged: FD 16, Catalog API 15, Canvas model 19, transport 1.

## Verification

Verified on 2026-09-14 in the canonical G: checkout with pinned Flutter 3.44.8.
No running IDE, Flutter SDK or user Flutter project was modified.

- Full core Java suite passed. Selected NetBeans Properties, palette, DnD,
  capability/catalog, source-proof, Events and resource regressions passed.
  Fresh Surefire reports: 3822 cases in 529 reports, 3747 executed successfully,
  75 conditional skips, zero failures/errors. This does not claim that
  the unfiltered all-module suite or every optional SDK integration ran.
- Full Canvas suite: 8135 passed, including 19 new tests for both variants,
  nullable inheritance, local styles, RTL, immediate updates/child identity,
  complex Paint/font features/variations, required-slot drops, zero-size
  selection, validation and inert source references.
- DefaultTextStyleRealSdkTest executed, not skipped: both invocation forms,
  local/imported getters/factories, strict wrong/dynamic/nullable type checks,
  real merge-method symbol evidence, pair-save, reopen and Undo. Its generated
  Dart passed 32 Flutter runtime cases. AnimatedDefaultTextStyleRealSdkTest
  was rerun and passed its 10 generated runtime cases as a shared-code regression.
- Flutter analyze: no issues. Release Web Canvas build passed.
- Source-bundle and actual local Web-artifact contract checks passed.
  Eight new SVG variants were visually reviewed and passed registry checks.
- No manual IDE interaction was performed or claimed. The IDE was not restarted.

Logs: ignored `flutter-designer/target/dts-*.log` files. Core command:
`mvn -q -pl flutter-designer -am test`. NetBeans regressions use
`-pl netbeans-plugin -am`; SDK tests explicitly use
`-Dflutter.events.sdk=G:/MyProjects/java/libraries/flutter_windows_3.44.8-stable/flutter`.
Actual Web checks use
`-Dcanvas.runner.web.source=G:/MyProjects/java/project/netbeans-flutter-plugin-starter/flutter-canvas-runner/src/main/flutter`.

Web main.dart.js: 3862608 bytes,
SHA-256 `a1cfa79e457873b875026a1b82320951bb7cae08999cba50cf2410201e490683`.
Both source and Web manifests were refreshed from actual bytes/hashes without
changing the formats. The generated Web build is not an extra NBM payload.

Packaging commands passed:
`mvn -q -pl netbeans-plugin -am -DskipTests install`, then
`mvn -q -pl netbeans-plugin nbm:cluster`.

Final NBM verification matched current compiled schema/generator/editor/evidence
classes, all eight SVGs, all 40 runner sources and their manifest hashes.
The packaged Web manifest matches all 35 actual local Web files. All four
development-cluster JARs match the NBM. The whitespace diff check passed.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8964982 bytes, built 2026-09-14T09:19:51.8167428Z.
SHA-256: `3654fca0ba996effad0698f4500a9238b291ef7cc4d4872673be6156e8a60b7d`.

Follow-up implemented: [DefaultTextStyleTransition](DEFAULT_TEXT_STYLE_TRANSITION.md),
a separate explicit Animation<TextStyle> API, not an animation option added
to these static entries.
