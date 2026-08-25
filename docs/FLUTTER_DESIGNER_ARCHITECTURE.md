# Flutter Designer Architecture

Status: **accepted foundation for 0.1.3**. Details explicitly marked as open
remain design work and are not implementation commitments.

## Product shape

The Flutter Designer follows the NetBeans Matisse interaction model while
respecting Flutter's semantic widget layout:

- an editor with `Design` and `Source` views;
- the standard NetBeans Palette for adding widgets;
- a widget tree published through NetBeans Explorer/Nodes;
- the standard Properties window for editing typed properties;
- a central canvas with synchronized selection, semantic drop targets, zoom,
  device bounds and layout guides;
- standard NetBeans Save, Undo/Redo, Copy/Paste and Delete actions.

Flutter layout is not an absolute-position form. A drop operation selects a
named constructor slot such as `body`, `child`, `children`, `appBar` or
`floatingActionButton`. Row and Column drops select an insertion index. Stack
may additionally expose `Positioned` semantics. The persisted model never
stores incidental canvas coordinates as Flutter layout.

## Paired files and ownership

A designer form is a same-directory, same-basename pair:

```text
home_page.fd       canonical visual model, JSON
home_page.dart     user source plus designer-managed regions
```

The `.fd` file is the source of truth for the visual subtree. The `.dart` file
is the source of truth for all code outside designer-managed regions. A Dart
file without a matching `.fd` file is an ordinary Dart file and is never
claimed by the designer.

NetBeans presents an `.fd` document as one logical designer object. `Design`
edits the `.fd` model; `Source` edits the paired `.dart` file. Rename, Copy,
Move and Delete must operate on the pair and must not silently leave one half
behind. Opening the JSON representation directly is an explicit advanced
action, not the normal `Source` view.

The following pairing invariants are validated before the designer becomes
writable:

1. Both files are local project files in the same directory.
2. Their basenames match exactly, including case where the filesystem exposes
   case-sensitive names.
3. The `.fd` `source.dartFile` value names that exact sibling and contains no
   path separator.
4. The declared Dart class and managed-region markers occur exactly once.
5. Every managed-region hash matches the current Dart payload.

If the Dart file is missing, the designer may offer an explicit regeneration
from `.fd`. It does not regenerate automatically during project scanning.

## Guarded Dart regions

The first accepted source-ownership strategy is Matisse-like guarded regions
inside the primary Dart file. A generated file is structured like this:

```dart
// <netbeans-flutter-designer region="imports">
import 'package:flutter/material.dart';
// </netbeans-flutter-designer>

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  // <netbeans-flutter-designer region="build">
  @override
  Widget build(BuildContext context) {
    return const Scaffold();
  }
  // </netbeans-flutter-designer>

  // User-owned fields, methods and callbacks are written outside the markers.
}
```

Marker spelling is part of the format contract. Markers are ASCII line
comments, cannot nest, and each region id occurs once. Version 1 reserves the
`imports` and `build` ids. A later schema migration may introduce additional
region ids; an older generator must reject them rather than remove them.

For every region, `.fd` stores the SHA-256 of its generated payload. The hash
is computed over UTF-8 bytes after converting CRLF and CR to LF, excluding the
marker lines, and ensuring exactly one trailing LF. Hexadecimal hashes are
stored uppercase. This normalization makes the integrity check independent of
the platform line-ending convention.

The UI should use NetBeans guarded-section support when practical, but the
hash is the security and integrity boundary. Read-only editor decoration alone
is insufficient because a file can be changed by another editor, Git, a build
tool or an external process.

### Source conflicts

The designer enters `SOURCE_CONFLICT` and performs no automatic write when:

- a marker is missing, duplicated, reordered or nested;
- a managed payload hash differs from the `.fd` hash;
- the source file or declared class no longer matches the pair;
- the installed generator cannot understand the document schema.

The conflict UI names the exact file and region. Initial resolution actions
are: open a diff, keep the Dart source and detach the designer, or explicitly
restore the generated region from `.fd`. Importing arbitrary edited Dart back
into the model is not part of the 0.1.3 foundation. No conflict dialog may use
a default action that overwrites Dart code.

Changes outside managed regions are always preserved byte-for-byte by a
designer save. The designer does not run `dart format` on the whole file,
because that would rewrite user-owned code. Generated payloads are formatted
deterministically before insertion; the user's explicit Source-format action
may still format the complete Dart document.

## `.fd` document contract

`.fd` is UTF-8 JSON and conforms to
[`flutter-designer/fd-v1.schema.json`](flutter-designer/fd-v1.schema.json).
The stable format name is `netbeans-flutter-designer`, and version 1 uses an
integer `schemaVersion` so migrations are explicit.

The checked-in [`home_page.fd`](flutter-designer/examples/home_page.fd) and
[`home_page.dart`](flutter-designer/examples/home_page.dart) pair is the first
executable contract example. Future codecs, hash checks and generators use it
as a golden fixture rather than maintaining a separate undocumented example.

The document contains:

- a stable document UUID;
- the paired Dart filename, class kind/name and managed-region hashes;
- optional design-time canvas preferences;
- one root widget node;
- an extension namespace for data not owned by the core schema.

Every widget has a stable UUID, a catalog type id, typed properties and named
slots. A generic unlabelled `children` array is insufficient for Flutter:
`Scaffold.body`, `AppBar.title`, `Padding.child` and `Column.children` have
different constructor semantics. Each slot therefore declares whether it is
single-valued or list-valued.

Property values are typed. Version 1 distinguishes strings, booleans,
integers, doubles, enums, colors, edge insets, asset references, callbacks and
explicit Dart expressions. A widget catalog decides which value kinds and
slots are legal for a particular widget. Omission means "use the Flutter
constructor default"; an empty string is a real value and is not treated as
omission.

Unknown top-level fields are rejected. Vendor or experimental data belongs in
the namespaced `extensions` object. A document with a newer unsupported schema
opens read-only and is never down-saved. Migrations run in memory, retain the
original bytes until an explicit Save, and have golden before/after tests.

## Domain and module boundaries

`flutter-designer` remains NetBeans-independent and owns:

- `DesignerDocument`, widget nodes, named slots and typed property values;
- the built-in widget metadata catalog and its extension SPI;
- JSON decoding/encoding and schema migrations;
- structural and catalog validation;
- deterministic Dart-region generation;
- commands for add, remove, move, wrap and property changes;
- conflict detection inputs and normalized region hashing.

`netbeans-plugin` owns only the NetBeans edge:

- `.fd` MIME resolution, loader/DataObject and paired-file operations;
- the `Design`/`Source` MultiView elements;
- Palette, Explorer/Nodes, Properties and Visual Library adapters;
- `Savable`, file listeners, guarded sections and `UndoRedo.Manager` bridges;
- wizards/actions and user-facing conflict resolution;
- background execution and EDT handoff.

Core Dart editing, project recognition, SDK discovery and Run/Debug do not
depend on `flutter-designer`. Disabling designer UI must leave ordinary Flutter
development functional.

## Editing and Undo/Redo

UI components never mutate widget collections directly. Each edit is a
validated command with an inverse:

- `AddWidget`
- `RemoveWidget`
- `MoveWidget`
- `WrapWidget`
- `SetProperty`
- `ResetProperty`
- `RenameDesignerClass`

One semantic user action produces one undoable edit. Selection changes, zoom
and temporary drag feedback are not document edits. The saved command cursor
defines dirty state; returning to it through Undo clears `Savable`.

Copy/Paste serializes a versioned widget fragment, allocates new stable ids and
validates the destination slot before creating an undoable command. Delete
reports why a required root or required slot cannot be removed.

## Load and save pipeline

Load runs off the Event Dispatch Thread:

1. Read bounded `.fd` and paired Dart snapshots.
2. Decode and validate the schema without executing Dart expressions.
3. Resolve widget definitions through the catalog.
4. Locate markers and verify normalized region hashes.
5. Publish an immutable document snapshot to the EDT.

Save is refused until model and source preconditions pass. It then:

1. Validates the current model and all commands.
2. Generates every managed Dart payload in memory.
3. Re-reads the source and rechecks marker hashes to detect concurrent edits.
4. Replaces only managed payloads, preserving all other bytes.
5. Computes the new hashes and serializes deterministic `.fd` JSON.
6. Writes through NetBeans file locks inside one filesystem atomic action.

The source is written before `.fd`. If the second write fails, the plugin
attempts to restore the captured source bytes. A crash or failed restoration
therefore leaves a detectable hash conflict, never a silently accepted mixed
state. File-change events caused by the owned save are correlated so they do
not trigger a false external-change prompt.

## NetBeans presentation

The active designer publishes its selected widget as a NetBeans Node. That one
selection drives the canvas highlight, widget tree and standard Properties
window. The Palette is supplied through the active MultiView element's Lookup,
so the normal NetBeans Palette window becomes context-sensitive.

The canvas uses the NetBeans Visual Library for selection, zoom, pan, drag/drop
feedback and semantic layout guides. Canvas widgets are projections of the
domain model and never become the persisted model themselves.

The first usable vertical slice contains `Scaffold`, `AppBar`, `Column`, `Row`,
`Padding`, `Center`, `Text`, `Icon`, `SizedBox` and `ElevatedButton`. It must
support create, open, edit, save, reopen, undo/redo and deterministic Dart
generation before additional widgets are added.

## Preview boundary

Pixel-accurate Flutter rendering is not part of the `.fd` ownership contract.
The initial canvas may be a semantic projection, but it must not pretend to be
pixel-accurate. A later preview runner will use the configured project Flutter
SDK, consume the same validated document/catalog, and keep process/tooling code
outside Swing event handlers. Embedding a native Flutter surface, streaming a
rendered surface, and selected-target preview remain open implementation
choices that require a separate ADR.

## Remaining decisions before UI implementation

1. Exact `MultiDataObject` pairing and recovery when one sibling is already
   owned by another NetBeans loader.
2. Guarded-section API compatibility with NetBeans 30.
3. The built-in widget catalog metadata and property-editor SPI.
4. Callback stub creation without modifying user-owned code on later saves.
5. Preview transport and lifecycle.

These decisions must be resolved with focused prototypes and tests; they do
not weaken the accepted `.fd` canonical-model and guarded-Dart-region rule.
