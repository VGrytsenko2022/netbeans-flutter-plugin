# Scrollbar contract

Flutter Designer targets the pinned Flutter 3.44.8
`material.Scrollbar` constructor as a complete vertical slice.

## Properties and slots

The Material palette entry exposes all eight constructor properties in SDK order:

- `controller`: nullable `ScrollController` reference;
- `thumbVisibility` and `trackVisibility`: nullable booleans;
- `thickness`: nullable non-negative logical-pixel value;
- `radius`: nullable `Radius` reference;
- `notificationPredicate`: nullable `ScrollNotificationPredicate` reference;
- `interactive`: nullable boolean;
- `scrollbarOrientation`: nullable `ScrollbarOrientation` (`left`, `right`,
  `top` or `bottom`).

The `child` slot is required and accepts one reviewed widget. The model keeps the
slot required (`1..1`) while palette and Slots-editor operations perform an
atomic wrap around an existing subtree. No placeholder child is fabricated.

Explicit nullable booleans use the centered checkbox editor in NetBeans
Properties. Resetting a boolean is distinct from `false`: `<not set>` omits the
constructor argument and preserves `ScrollbarThemeData`/platform defaults.
`left` and `right` place a scrollbar beside vertical scrolling; `top` and
`bottom` place it above or below horizontal scrolling. Flutter remains the
authority for incompatible orientation/axis combinations.

## Canvas and generation

Generated Dart preserves the exact constructor fields, typed imports and child
identity. The native Canvas mounts Flutter's real `Scrollbar`; it does not run
project controllers, notification predicates or object references. Those values
remain in the model/source and receive a concrete diagnostic in the isolated
preview.

An arbitrary required child does not necessarily own a `ScrollPosition`. To
avoid Flutter's SDK assertion in that case, the isolated preview places the
child in a preview-only `SingleChildScrollView` with the axis implied by the
selected orientation. This wrapper is not serialized and does not alter the
generated application tree. The mounted child remains selectable and retains
its state across ordinary property edits.

## Verification

`ScrollbarContractTest` covers catalog/schema/projection/generation parity;
`canvas_scrollbar_test.dart` covers schema payloads, native fields, required
child rendering and project-reference diagnostics. Palette/icon registry,
Properties, Slots, persistence and DnD suites cover the host integration. The
packaged source manifest is hash-verified after the runner changes, and the
full Java and Flutter suites are the release gate for this slice.

CJK IME and Linux/macOS Canvas providers remain deliberately deferred until the
complete palette inventory is finished.
