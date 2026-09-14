# MenuBar contract

Flutter Designer targets the pinned Flutter 3.44.8 `material.MenuBar`
constructor as a complete vertical slice.

## Surface

- `const MenuBar` is admitted to the Material palette with a dedicated SVG icon.
- The required `children` list is a real list slot (zero or more widgets,
  maximum 10,000). Menu entries can be inserted, moved, removed and reordered
  without replacing the surrounding widget.
- `controller` is an optional nullable `MenuController` reference. It is
  persisted and generated, but project-owned controller code is never executed
  by the isolated Canvas.
- `clipBehavior` is the typed `Clip` enum and preserves the SDK default
  `Clip.none` when omitted.
- The `style` reference is nullable and exclusive with the 203 flattened local
  `MenuStyle` leaves. Editing either branch is one atomic model command; reset
  and undo restore the complete previous branch.

The 206 typed property rows are grouped into behavior, nine state-specific
style buckets, common density/alignment fields and whole-style reference. The
widget has no direct callback constructor parameters, so it does not invent an
Events tab or global shortcut registration. Events and shortcuts remain owned
by child menu entries and application code.

## Canvas and generation

Canvas mounts the native Flutter `MenuBar` with its actual child widgets and
native layout. It uses a stable preview-owned controller and reports explicit
diagnostics for project controller/style references; values and child IDs stay
unchanged. Non-finite or inconsistent inherited menu geometry is quarantined
to finite SDK-safe defaults for the preview only.

Generated Dart keeps the `MenuBar` constructor, child-list placement, typed
references and local `MenuStyle` synthesis. No project callback, controller,
builder or shortcut registry is invoked during preview.

## Verification

`MenuBarContractTest`, `MenuBarPropertyContractTest` and
`canvas_menu_bar_test.dart` cover the 206-property protocol, required list
slot, palette/drop admission, atomic property edits, native children and
reference diagnostics. The complete Java and Flutter suites are the release
gate for this slice.
