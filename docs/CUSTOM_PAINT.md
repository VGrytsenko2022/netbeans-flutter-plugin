# CustomPaint

Pinned API: Flutter 3.44.8. This slice admits `flutter.widgets.CustomPaint` to Basic
with its full constructor surface: five typed Properties, shared Key and optional
box Child. The palette prototype leaves arguments omitted, matching native defaults.

## Properties and source ownership

- Painter and Foreground painter accept exact `CustomPainter?` project/package
  references, getters, static members and zero-argument factories, explicit null
  or omission. They paint before and after Child respectively.
- Size offers a transactional width/height editor and a typed non-null `Size`
  source. Local dimensions must be finite and nonnegative. Default is Size.zero;
  Child takes precedence and parent constraints still apply.
- Complex painting and Will change are checkbox cache hints. Known null/unset
  painters with either hint true are rejected atomically. A nullable source must
  supply at least one painter at runtime when a hint is true, as Flutter requires.
- Shared Key, slot replacement/removal, undo/redo and save/reopen use the existing
  model and command pipeline. No new FD kind, catalog API or Canvas version is needed.

A source CustomPainter retains its entire native implementation: paint,
shouldRepaint, repaint Listenable, hitTest, semanticsBuilder and
shouldRebuildSemantics. These are methods of an object, not widget callback
parameters, so no synthetic Events or function stubs are added. Dart owns
painter resources, notifications and disposal. Generated code passes the
exact object; it does not rewrite the source implementation.

The analyzer proves the exact generated expression against a fixed SDK Rendering
CustomPainter? or dart:ui Size witness. Dynamic, unrelated values, callback functions
and nullable Size cannot pass this proof.

## Canvas boundary

The isolated Canvas never executes project code. Each configured painter reference
is represented by an inert native CustomPainter; it paints nothing, has no custom
semantics and returns false from hitTest. Source Size previews as Size.zero.
A tooltip explicitly identifies which source-owned properties cannot be previewed.
Local size, child layout and cache hints still use native CustomPaint.

This is a deliberate preview limitation, not a replacement for the project's
artwork. Test source painting in the generated Flutter application. No implicit
ClipRect or saveLayer is inserted. Source painting should balance canvas save/restore;
clipping and unusual blend-mode layer isolation remain the author's responsibility.
CustomPainter cannot request layout or setState during paint.

## Verification

Contract coverage includes the closed schema, all eight source access shapes for
each object property, exact analyzer evidence, null/default behavior, invalid
cache combinations, size bounds, stable Properties rows, source/local editor,
FD roundtrip, palette/slot parity and four reviewed SVG variants.

Real-SDK scenarios cover generated forms, both text directions, strict negative
types, child insertion/removal, reset, undo/redo, user-source preservation, native
paint order/pixels, repaint notification ownership, hit testing and semantics.
Verified on 2026-09-15:

- 34 generated/native Flutter SDK runtime cases passed (17 saved forms, LTR/RTL).
- Eight source access combinations, both constant painter constructors and strict
  negative source-type scenarios passed analyzer-backed pair-save checks.
- 11,521 Canvas tests passed, including 18 CustomPaint tests.
- Fresh Java reports: 4,048 tests, 3,946 passed, 102 conditionally skipped,
  zero failures/errors (full designer core, selected plugin/analyzer regressions
  and source/Web artifact contracts).
- Flutter analyze reported no issues; Web Canvas release build passed.
- NBM install/package and development cluster passed. Verified exact schema/UI/
  analyzer classes, four icons, all 40 runner sources and 35 Web files;
  all four development-cluster JARs match the NBM. No interactive IDE restart/test.
- FD 17, Catalog API 16, Canvas model 20 and transport 1 are unchanged.
- Inventory: 218 definitions, 7,441 writable rows, 210 typed Properties definitions,
  183 const-capable definitions, Basic 37, 681 non-nullable/53 nullable booleans.
- Optional insertion matrix: 188 destinations (168 any/20 trait), 40,984 candidates,
  27,943 accepted and 13,041 rejected. CustomPaint has no required-child wrapper route.

## Official references

- [CustomPaint](https://api.flutter.dev/flutter/widgets/CustomPaint-class.html)
- [CustomPainter](https://api.flutter.dev/flutter/rendering/CustomPainter-class.html)
- Pinned local source: `packages/flutter/lib/src/widgets/basic.dart` and
  `packages/flutter/lib/src/rendering/custom_paint.dart`.
