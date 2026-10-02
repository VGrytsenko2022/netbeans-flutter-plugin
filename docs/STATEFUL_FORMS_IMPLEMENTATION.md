# Stateful Designer forms

## Implemented prerequisite

The Flutter Designer Form wizard offers an explicit Widget kind choice:
Stateless or Stateful. Stateless remains the default and existing forms are not
converted implicitly. This prerequisite reused the existing `widgetKind: stateful`
schema-13 value without adding a schema or Canvas protocol version. The subsequent
[State/value binding feature](STATE_VALUE_BINDINGS.md) adds binding metadata in
schema 14, extended by shared control/dependent-property bindings in schema 15,
while retaining the Canvas protocol.

A new Stateful form contains an immutable widget, its direct `createState()`
factory and a separate State class. The existing managed build region belongs
to the State class. Flutter defines mutable data and rebuilding in State rather
than in the immutable StatefulWidget:
[StatefulWidget](https://api.flutter.dev/flutter/widgets/StatefulWidget-class.html),
[setState](https://api.flutter.dev/flutter/widgets/State/setState.html).

Events discovery, create, bind, rename and Go to Handler operate on the verified
State class. The class name is discovered from source; it is not guessed from
the widget name. Fields, helper methods and event bodies remain user-owned
outside the two managed regions. An event body can be edited before the first
Save, including a synchronous `setState` call, through the analyzed Source
restaging path. Designer property mutations, Save/reopen and native Undo/Redo
preserve those user-owned bytes.

## Source and analysis boundary

The source scanner must establish all of these facts together:

- One unambiguous top-level widget class extends unqualified StatefulWidget.
- One unambiguous top-level State class extends unqualified `State<ThatWidget>`.
- A direct synchronous zero-argument `createState` returns a new instance of
  that State class, via an arrow or a simple return block. The return type may
  be `State<ThatWidget>` or the concrete State class.
- The build markers wrap a direct member of that State class; imports remain
  library-level before the widget. Comments, strings, nested declarations and
  unrelated State classes cannot establish ownership.

Only a clean scan publishes member ownership and two exact source-token proofs.
Before mutation/save the real Dart analyzer must accept the complete candidate
and resolve both StatefulWidget and State to classes in the trusted Flutter SDK.
Missing, substituted, reordered, locally shadowed or untrusted evidence is not
accepted. The State probe consumes an additional slot within the existing
candidate limit; the maximum is not increased and the shared budget identity
remains unchanged.

Conservative unsupported source shapes include generic widget/State classes,
mixin/implements headers, qualified framework bases, indirect/async/named or
argument-taking State factories, and ambiguous ownership. These are explicit
write-safety boundaries, not claims that Flutter does not support them.
Changing the State owner or Stateless/Stateful kind through an ordinary source
projection remains forbidden. Automatic conversion needs its own typed,
analyzed, reversible source-owner transition.

## State/value binding follow-up

The initial Stateful prerequisite did not add value binding. The subsequent
[State/value binding slice](STATE_VALUE_BINDINGS.md) now adds Create State Binding
for nine reviewed control families, including owned TextEditingController,
IconButton toggles and ListTile selection. It creates a private typed field or
reuses a compatible verified field and binds an updating handler atomically.
Reviewed dependent properties can read the same field through closed transforms.
Literal properties remain design-time previews. Go to Field and Rename State Field
manage currently bound fields, with whole-form source/metadata rename and explicit
ambiguous-source rejection.

Automatic conversion of existing Stateless forms and arbitrary expressions remain
separate from that closed binding workflow.
Removing a binding retains user source; it does not delete a field or handler.

Canvas continues to render semantic model values and select widgets. It does
not execute user handlers or run the user's State lifecycle. Runtime event
execution belongs to Flutter Run/Debug, not the Designer preview.

## Verification

Core source, generation, template, Events command and source-projection tests
cover State ownership and preservation. Plugin tests cover both superclass
evidence, the wizard kind choice, Events navigation and full
create/body-first-Save/Undo/Redo/rename/property-edit/reopen/disconnect behavior.

The opt-in `StatefulFormRealSdkTest` uses `flutter.events.sdk`, creates an isolated
temporary project, resolves dependencies offline, checks both superclass probes
with the real analyzer and executes a Flutter widget test. That test invokes a
generated event callback on a mounted State and observes a user-owned counter
updated by `setState`; it does not claim value-property binding or Canvas event
execution. No user Flutter project is modified by this test.

Physical interactive NetBeans UI behavior requires a separate manual check.

To check the installed build, create a **Flutter Designer Form** and select
**Widget Kind: Stateful**. Select a widget with native events, create a handler,
and use **Go to Handler**: the method must be in the associated State class.
Edit its body outside the managed regions, Save, close/reopen the form, and
confirm that Events and ordinary Designer properties remain editable. Existing
Stateless forms must remain Stateless unless a future explicit conversion is
requested.

### Verified build, 2026-09-08

- Full `flutter-designer` test run: 1,788 tests, zero failures/errors/skips;
  opt-in Events SDK contracts enabled against Flutter 3.44.8.
- Focused plugin regression run: 546 tests across 16 suites, zero
  failures/errors/skips, including all 152 mutation-controller integration cases.
- `StatefulFormRealSdkTest`: one additional passing test, real analyzer plus
  mounted Flutter callback/`setState` execution; rerun successfully during verify.
- Package verification: nine passing `PluginPackageMetadataIT` tests.
- Maven verify/install and both root/module `nbm:cluster` builds succeeded.
  The resulting NBM and both development clusters match all 1,612 module payload
  entries (including 1,209 classes, excluding the archive manifest). Both cluster
  Designer-core dependencies also match the built core JAR byte-for-byte.
- All work and builds used `G:/MyProjects/java/project/netbeans-flutter-plugin-starter`.
  No running IDE was restarted and no user Flutter project was changed.
