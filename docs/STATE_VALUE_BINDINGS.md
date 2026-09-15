# State/value bindings

## CalendarDatePicker lifecycle (2026-09-15)

No new controlled State consumers. Native initialDate/initialCalendarMode do not
reset calendar state on a same-key rebuild. Change Key to recreate it; use the
selection/month Events to update application State. initialDate is required but
nullable: null means no selected day. Canvas intentionally remounts when the
Designer model changes, without executing application callbacks.
The inventory remains 178 fields across 60 types.
See [CalendarDatePicker](CALENDAR_DATE_PICKER.md).


## DateRangePickerDialog lifecycle (2026-09-15)

No new controlled State consumers. Initial range/mode belong to native restorable
dialog state; await the Navigator DateTimeRange/null result to update application
State. Typed range and calendar sources remain user-owned. The inventory stays
at 178 fields across 60 types. See [DateRangePickerDialog](DATE_RANGE_PICKER_DIALOG.md).


## DatePickerDialog lifecycle (2026-09-15)

No new State consumers are added. initialDate, currentDate and initial modes are
native initialization/restoration inputs, not controlled fields. Selected dates
return through the Navigator route; update application State after awaiting
that result. Typed DateTime sources and CalendarDelegate remain editable but
are not evaluated in Canvas. The inventory stays at 178 fields across 60 types.
See [DatePickerDialog](DATE_PICKER_DIALOG.md).


## PaginatedDataTable consumers (2026-09-15)

Add six reviewed fields: rowsPerPage, sortColumnIndex, sortAscending,
showCheckboxColumn, showFirstLastButtons and showEmptyRows. Page size and sort
index use direct int/int? bindings. Application handlers must keep page size
positive and within availableRowsPerPage while its callback is enabled; sort
index must remain null or within the column range. Column edits reject a bound
sort index until explicitly detached. initialFirstRowIndex is intentionally
initial-only, not a reactive consumer. See [PaginatedDataTable](PAGINATED_DATA_TABLE.md).


## User workflow

Create a Stateful Flutter Designer Form, select a supported control, open its
On Changed, On Pressed or On Tap editor under Events, and select **State binding**.
Choose a new private field or reuse a compatible existing field from the verified
State class. Supply a distinct private handler name and use **Create State Binding**.
An independently configured callback must be disconnected explicitly first; its
source is retained. Each widget keeps its own handler even when fields are shared.
Scalar field discovery reads explicit initialized declarations. A project class or
enum field can be reused when its exact type identity already belongs to this
form's admitted metadata, for example a RadioGroup binding. The editor does not
guess custom type identities from arbitrary names or imports.

Creation is one analyzed semantic command: it creates an initialized field and
an updating handler in the verified State class, changes the runtime value
argument to a field reference, and binds On Changed. The generated handler uses
synchronous `setState`; for a non-tristate Checkbox it explicitly ignores a null
callback argument rather than inventing a false value. Flutter requires the
parent to provide the changed value on rebuild:
[Checkbox](https://api.flutter.dev/flutter/material/Checkbox/onChanged.html),
[RangeSlider](https://api.flutter.dev/flutter/material/RangeSlider/onChanged.html),
[setState](https://api.flutter.dev/flutter/widgets/State/setState.html).

| Supported control | Runtime argument | State type |
| --- | --- | --- |
| Checkbox, CheckboxListTile | value | bool; bool? when tristate is true |
| Switch, SwitchListTile | value | bool |
| Slider | value | double |
| RangeSlider | values | one RangeValues containing both endpoints |
| RadioGroup | groupValue | selected concrete T?, including imported class/enum/typedef types |
| RadioListTile (legacy selection) | groupValue | selected concrete T?; option value remains independent |
| TextField | controller | owned final TextEditingController; On Changed receives String |
| IconButton (explicit toggle mode) | isSelected | bool; On Pressed toggles the field |
| ListTile | selected | toggle bool or compare a shared scalar selection with a closed value |

Existing standard/adaptive constructor variants are retained. RadioGroup supports
String, int, double, num, bool, Object and the same closed project type references
as its existing property editor; T and T? constructor variants both have a
nullable group value and callback argument. This matches the
[RadioGroup API](https://api.flutter.dev/flutter/widgets/RadioGroup-class.html).

RadioListTile also exposes its deprecated legacy groupValue as a nullable
controlled State field for the same six builtin/project types. Prefer a matching
RadioGroup for modern ownership. The tile's effective group value is the
registry's non-null group value, otherwise the configured legacy groupValue;
modern null selection can therefore fall back to a legacy value. Activation
notifies RadioGroup first and then the optional legacy callback. This is not a
fabricated groupRegistry argument or a change to standalone Radio ownership.

IconButton needs an explicit boolean Is Selected preview value before its toggle
binding is available. A null/omitted value remains an ordinary push button; the
toggle presentation is a Material 3 feature, as documented by
[IconButton.isSelected](https://api.flutter.dev/flutter/material/IconButton/isSelected.html).
ListTile supports independent boolean toggles and exclusive selection: bind
several tiles to one field, assigning each a distinct Select value. Creating a
new scalar selection field uses a nullable type and starts at null unless that
tile is selected in the preview. Reusing a field preserves its initializer.
Selection/comparison values are closed null, boolean, string, integer or double
values, not arbitrary Dart expressions. Legacy Radio selection belongs to its
RadioGroup rather than a second per-Radio state owner.

## DataTable consumers

DataTable.sortAscending and selected on DataRow/DataRow.byIndex accept boolean
State fields (or the reviewed boolean transforms). DataTable.sortColumnIndex
accepts a direct int/int? State field. Keep it null or in the current column
range. Column edits are blocked while that binding is active: detach it, edit
the grid, then update the State index before rebinding. Designer does not rewrite
a user-owned State field.

These are consumers, not automatic sorting/selection producers. Create or select
native event handlers and implement setState in their editable bodies. The
onSort handler receives (int columnIndex, bool ascending); selection handlers
receive bool?. FD rows/columns remain a structurally edited list.
See [DataTable](DATA_TABLE.md). The current reviewed consumer inventory is
172 properties across 59 definition types.

## Dependent properties

Reviewed properties expose **Literal / State** pages in their existing editors.
Choose a compatible verified field and a closed transform:

| Transform | Runtime result |
| --- | --- |
| Direct | The exact field value |
| To string | `field.toString()` |
| Text | `controller.text` |
| Equals | Comparison with a closed scalar value |
| Not | Negation of a nonnullable boolean field |
| Clamp to valid range | A double bounded to 0..1, or a safe IndexedStack child index |

This allows TextField text to feed Text, Switch/IconButton state to control
Visibility/Offstage, and ListTile selection to select an IndexedStack child.
The shared reviewed catalog exposes **166 properties across 55 widget types**,
including the five numeric targets below. It also covers direct boolean/string constructor
arguments such as labels, semantics, enabled/selected flags, and scrolling flags;
it does not infer support merely because a property happens to contain a boolean.
Synthetic constructor selectors and callbacks are excluded, apart from the
explicitly lowered MenuItemButton Enabled activation consumer described below. Nested property
leaves are excluded except the three explicitly reviewed TooltipThemeData
booleans described below. Known structural companion flags such as isThreeLine
are excluded.
Reviewed enabled consumers retain their documented SDK runtime activation
requirements: RadioListTile enabled true requires a legacy callback or matching
RadioGroup at mount time. State binding does not prove arbitrary ancestry.
Its eight consumers are toggleable, dense, selected, autofocus, enableFeedback,
enabled, internalAddSemanticForOnTap and useCupertinoCheckmarkStyle. The latter
remains retained but ungenerated in Standard, just like the literal value.

MenuAnchor adds four non-null boolean consumers: consumeOutsideTap,
crossAxisUnconstrained, useRootOverlay and animated. DIRECT/NOT/EQUALS retain the
same verified State-field contract and restore literal/omitted values on detach.
The deprecated anchorTapClosesMenu argument is inert in pinned Flutter and is
explicitly not a consumer. There is no controlled open-state producer: lifecycle
belongs to MenuController and native Events. See [MenuAnchor](MENU_ANCHOR.md).

MenuItemButton adds four non-null boolean consumers: enabled, autofocus,
requestFocusOnHover and closeOnActivate. DIRECT/NOT/EQUALS use the existing
verified State-field contract. Enabled lowers to `onPressed: flag ? handler : null`
(or an enabled no-op when no handler is stored), not an SDK `enabled` parameter.
The preview literal and disabled handler remain stored; binding removal restores
literal activation. There is no State producer, shortcut registration or inferred
binding for shortcut modifiers or local style leaves. Native disabled-focus and
hover behavior stays with Flutter. See [MenuItemButton](MENU_ITEM_BUTTON.md).

ExpansionTile adds six consumers: showTrailingIcon, maintainState, dense,
enableFeedback, enabled and internalAddSemanticForOnTap. Its initiallyExpanded
field is explicitly excluded because it is read only during initialization, not
as a controlled runtime value. There is no ExpansionTile State producer:
onExpansionChanged is an ordinary user-owned Event and programmatic expansion
uses a project-owned ExpansibleController. Disabled headers can still respond to
that controller; maintainState changes child lifecycle, not expansion ownership.

Tooltip adds five consumers: preferBelow, excludeFromSemantics,
enableTapToDismiss, enableFeedback and ignorePointer. These are actual boolean
constructor arguments; no tooltip visibility State producer is introduced.
The overlay's rich-content hit testing, hover behavior and programmatic
visibility remain SDK/source-owned, while onTriggered is a separate native Event.

TooltipVisibility adds the required nonnullable boolean visible consumer, with
Direct, Not and Equals transforms. It controls inherited Material Tooltip policy,
not an individual tooltip's runtime visibility state; there is no producer or
Event. The nearest scope wins rather than AND-combining ancestor bindings.
False removes the pinned SDK RawTooltip subtree and its overlay while retaining
the anchor's layout and child semantics. It does not promise retention of the
Tooltip message annotation, arbitrary child runtime state or focus across that
SDK topology change. See [TooltipVisibility](TOOLTIP_VISIBILITY.md).

TooltipTheme adds nullable preferBelow, excludeFromSemantics and enableFeedback
consumers. Direct supports bool/bool?; Not uses bool and Equals a reviewed scalar
comparison. These fields are generated inside the fresh TooltipThemeData object,
not as nonexistent outer widget arguments, retaining their original binding
paths and analyzer field evidence. Whole Data and any local binding conflict;
no theme reference or local value is silently removed. Resetting a binding
restores its stored preview literal, or omission if none exists. The nearest
theme replaces the entire parent data, not a per-field merge. This is a bounded
three-field admission, not general State support for nested object graphs.
There is no Event or producer. See [TooltipTheme](TOOLTIP_THEME.md).

Numeric Clamp is explicit and limited to Opacity.opacity, the value of
LinearProgressIndicator/CircularProgressIndicator/RefreshProgressIndicator, and
IndexedStack.index. Progress Value cannot be bound while an Animation Controller
is configured. An empty IndexedStack receives a null runtime index; adding or
removing children recomputes the bounds. Other numeric arguments are not broadly
enabled without their own reviewed runtime constraints.

## Text controller lifecycle

TextField creation accepts an initial runtime string, creates an owned final
TextEditingController and a separate field-derived listener, attaches that listener
in initState, and removes it and disposes the controller in dispose. Both typed
input and programmatic controller changes rebuild dependent properties. On Changed
remains an editable String callback for user logic; the listener never writes
controller.text, selection or composing state.

Existing conventional lifecycle methods retain their user code. Attachment follows
the initial super.initState call; cleanup precedes the existing dispose body, which
must end with super.dispose. Missing lifecycle methods are inserted. Ambiguous,
async/arrow or structurally unproved lifecycle methods are rejected with a reason
instead of being rewritten. Reuse accepts only a controller whose direct ownership,
listener and cleanup are proved, and never reinitializes or disposes it a second
time. This follows the subscription/cleanup requirements in
[State.initState](https://api.flutter.dev/flutter/widgets/State/initState.html) and
[State.dispose](https://api.flutter.dev/flutter/widgets/State/dispose.html).

## Preview and user-owned source

The existing value properties remain design-time previews, including supported
omitted and null values. Bound rows
are labeled accordingly; editing them changes the Canvas preview, not the Dart
field initializer. For RangeSlider, valuesStart and valuesEnd remain the two
preview leaves while the runtime reads a single field. Existing preview
validation, bounds, nullability and required-property checks remain in force.
Generic State-bound properties likewise retain their Literal value for preview and
for later removal of the binding. TextField's existing isolated Canvas text is not
the runtime controller's initial string; user State/controller code is never run
in the Canvas process.

Use Source to change the runtime initializer or handler body outside the managed
regions. Their bytes survive layout regeneration, Save/reopen and Undo/Redo.
Go to Handler and Rename Handler operate as before; rename also preserves the
binding association. You may edit Source before the first paired Save; Save
analyzes the current code.

**Remove Binding** restores runtime literal arguments from the current preview
values. If the control callback still points to the associated generated handler, removal
restores its previous omitted, null or no-op state. A later independent callback
choice is preserved. Removal never deletes fields, methods or their required
user-owned imports. Deleting the widget likewise retains user source.
For controllers this includes the listener and lifecycle code: removing a widget
does not assume that no other user code still uses its controller. Removing a
dependent property binding affects only that property, not its field or other users.

On Changed can be independently rebound or disconnected while a value remains
bound: the user then owns how that field is updated. Enabled, selected, bounds,
labels and other companions are not silently changed. Changing Tristate or the
selected value type incompatibly requires removing and recreating the binding.
Changing Slider or RangeSlider min/max while bound is also rejected: the preview
cannot establish that a user-owned runtime field will satisfy the new bounds.
Remove the binding, adjust bounds and preview, then create the new binding with
new member names. The old field and handler remain user-owned.
Reused Slider/RangeSlider fields must satisfy that control's min/max contract in
the user's runtime code. Unlike the explicit generic Clamp transform, these
existing control bindings do not silently clamp or rewrite user-owned values.

## Persistence and admission

Schema 15 extends the fixed-shape `stateBinding` with an action and optional closed
selection value, and adds a `propertyBindings` map keyed by property name. It stores
private member names, closed types/transforms, optional reviewed reference types
and previous callback values. No raw Dart expression, declaration, runtime
initializer or handler body is stored in this metadata. Frozen schemas 1–14 are
unchanged; prior-format files use the existing bounded migration workflow,
retaining their original bytes until an authorized save updates the format.

The generator replaces only the reviewed constructor argument, before computing
const eligibility. Each field read has an exact symbol occurrence, a FIELD
navigation requirement and a strict static-type proof. Radio references retain
the same concrete type identity as the constructor. Nullable-scalar proof syntax
is narrowly extended; arbitrary type expressions remain prohibited.

Admission requires a verified StatefulWidget/State owner and a direct initialized
mutable scalar/RangeValues field, or a proved owned final TextEditingController,
outside managed regions. Getters, inherited/static/late fields, missing members and
mismatched types are rejected. New names are checked against current
generated references and user-owned source, preventing accidental shadowing of
other widgets or top-level references. Candidate analysis, exact source identity,
paired transactions and external-change fences are not bypassed.
Producer and consumer fields are rechecked after Source edits, on reopen and on
cold Source Save. A consumer-only controller reference still requires valid
ownership and cleanup. Shared fields require identical type identity; handlers
must remain distinct. A widget cannot independently bind the same argument through
both a control action and a generic property binding.

Canvas receives exactly the existing literal-preview representation. Binding
identifiers and user source are not sent to or executed in the runner, so Canvas
protocol 18 is unchanged. Runtime state updates belong to Flutter Run/Debug.

## State field management

The State binding page of a control and the State page of a dependent property's
editor provide **Go to Field** and **Rename State Field** for the field currently
bound to that widget/property. Selecting a different discovery candidate does not
silently change the target of these actions. Unbound or stale editors cannot rename
an arbitrary source field.

Go to Field is explicit, read-only navigation to the verified declaration's exact
UTF-16 location in the current source snapshot. It closes the dialog without
committing pending literal or binding drafts; no intermediate Save is required.

Rename State Field is one analyzed whole-form command. It updates the direct
user-owned field declaration, unambiguous user-source references, every control
binding and dependent-property binding sharing that field, and exact local typed
model references. Imported references, plain strings, comments, preview values,
transforms and widget IDs are retained. Event handler names do not change. An owned
TextEditingController's field-derived listener is renamed in the same transaction,
including addListener/removeListener and disposal references.

The command remains in Designer and does not autosave. Undo/Redo restores the
source and metadata together; Save/reopen keeps shared bindings editable. Existing
field/member names and conflicting local model references are rejected instead of
being merged. The exact candidate still requires analyzer acceptance and the
existing paired-source identity, conflict and history checks.

History retains ordered exact source-operation steps. A previously authored
State-field rename can be re-proved against an older handler-body version; this
keeps both the original stub and later user-written body reversible after Save.
Observed Source edits and member creation/removal retain the strict overlapping
byte-edit rejection. Rename replay rechecks field type, source owner, references,
collisions and controller lifecycle, preserves managed bytes, and grants no write
authority. Retained step envelopes count toward the existing history byte limit.

This is a bounded form operation, not unrestricted Dart symbol refactoring.
Shadowed/ambiguous names, unresolved receivers, references outside the verified
owner, Symbol literals and string interpolation are rejected without applying the command. A library
containing `part`/`part of` directives is also rejected because Dart private names
are library-scoped and another part may contain unseen references; see
[Dart libraries and imports](https://dart.dev/language/libraries).
Go to Field remains available when exact declaration ownership can be proved.

## Boundaries

The eleven control families and reviewed dependent properties are supported, not
every conceivable State/controller relationship. Automatic Stateless-to-Stateful
conversion, arbitrary expression/data-source
binding and general AnimationController/ScrollController lifecycle graphs remain
outside this implementation. Existing unproved controller lifecycle code must be
adapted in Source before it can be reused. No unsupported palette widgets or
platform providers are introduced here.

## Verification

Focused tests cover the closed catalog, schema migration and malformed metadata,
generation/type proofs, preview-only Canvas payloads, atomic commands, member
collisions, source preservation, nullable values and UI dispatch. Integration
tests cover first Save after a manual handler edit, chronological Undo/Redo,
preview-only edits followed by rename, removal/reopen and rejected cold Source
Save after changing a bound field to final.
Expanded tests also cover controller lifecycle insertion/reuse/shadowing, shared
selection, dependent transforms and numeric bounds, consumer-only source guards,
and native/semantic history across controller creation and property binding.

The opt-in `StateBindingRealSdkTest` uses Flutter 3.44.8 in an isolated temporary
project: 25 cases cover all six families, standard and iOS adaptive branches,
tristate nulls, all RadioGroup builtin types and an imported enum, with both
nullable and non-nullable RadioGroup generic arguments. It checks exact analyzer
evidence and invokes callbacks on mounted widgets, observing new runtime values
after rebuild. It also analyzes source after binding/widget removal to verify
retained imports. The complete 25-case workflow is repeated after field rename,
including nullable and imported enum identities, unchanged event method names and
dependent enum consumers. These are callback/rebuild tests, not a claim of end-user
gesture testing or physical NetBeans desktop UI verification.

`StateExpansionRealSdkTest` checks TextField typed input and programmatic changes,
shared IconButton/Switch state, exclusive ListTile selection, Text/Visibility/Offstage
consumers, all five bounded numeric targets, disposal, empty-stack generation and
source after controller/widget removal against the real Flutter SDK. It repeats
the composite workflow after renaming its controller, shared boolean, selection
and numeric fields, checking analyzer evidence and runtime behavior under the new
names while preserving user methods.

`DartStateFieldRenameSourceTest` covers exact source edits, UTF-16 navigation,
LF/CRLF/BOM and unsafe-reference rejection. `RenameStateFieldCommandSessionTest`
covers whole-form metadata/reference changes, collisions and reversible history.
The UI/bridge suites verify bound-field action scope, stale contexts and read-only
navigation. The mutation-controller integration suite covers controller rename
through a consumer, native source comments/strings, no autosave, semantic
Undo/Redo, Save/reopen, a rejected collision and subsequent edits/renaming.

`ListTileStateBindingRealSdkTest` exercises 21 toggle/selection cases, including
new nullable scalar fields, already-selected previews, shared initialized fields
and dependent Text. The project-type cases in `StateBindingRealSdkTest` also bind
Text to an enum field and analyze the remaining consumer after RadioGroup removal.

### Previous six-control regression — 2026-09-08

The complete `flutter-designer` suite passed: **1,825 tests, zero failures,
errors or skips**, including the five bound Slider/RangeSlider range tests.
The broader NetBeans regression run passed **620 tests across 17 suites**, with
zero failures, errors or skips. This covers mutation/history, paired and
metadata-only persistence, Source bridges, guards, property rows, Events and
wizard/accessibility behavior. Adversarial tests intentionally log synthetic
failure traces; the recorded totals are the Maven/Surefire results, not a
claim that no warnings appeared.

The final `verify` rerun also passed the updated 10-test Events UI suite, the
25-case State binding SDK test, the Stateful form SDK test and all 9 NBM package
metadata checks. `install` and both root/module `nbm:cluster` commands succeeded.
The built module, packaged NBM and both development clusters have identical
1,612-entry payloads excluding the packaging-adjusted manifest; bundled Designer
and Dart analysis dependency JARs match their current built artifacts exactly.
Schema 14 and the new binding classes are present in the packaged Designer JAR.
No running IDE was restarted; physical desktop verification remains separate.

### Expanded schema-15 verification — 2026-09-08

The complete Designer core run passed **1,855 tests, zero failures, errors or
skips**, with the opt-in Flutter SDK tests enabled. This includes the 120-property
coverage contract, all closed transformations, strict migration/metadata tests,
and guarded-import LF/CRLF/BOM boundaries.

A broad 633-test NetBeans run exercised all 156 mutation-controller integration
cases. It exposed two obsolete UI assertions: the future-schema version message
and the assumption that the checkbox must be the editor's outermost component.
Both were updated without relaxing behavior: the reopened real PropertySheet
checkbox must be visible, enabled, centered and support commit/Undo/Redo inside
NetBeans' custom-editor button container. The two controller/consumer save and
reopen tests passed, and the three affected mutation scenarios passed in the
subsequent focused rerun. The retained-import placement defect itself is fixed
in source insertion, not by weakening native guarded-section checks.

The final `verify` run passed **486 tests across 22 NetBeans/SDK suites**, with
zero failures, errors or skips, plus **all 9 NBM package checks**. It reran the
17 non-mutation-controller regression suites, the three affected mutation
scenarios, and all four real-SDK suites. The latter include the original 25
controlled-value cases (now also checking typed enum consumers), the 21 ListTile
cases, the composite controller/shared-state test and the Stateful form test.

`install` and both root/module `nbm:cluster` commands succeeded. The built plugin,
NBM and both development clusters have matching **1,619-entry module payloads**
(excluding the packaging-adjusted manifest). Designer and Dart analysis
dependency JARs match exactly; schema 15 and both State metadata classes are
present in the packaged Designer JAR. No running IDE was restarted, and no
physical desktop UI verification is claimed.

### State field management verification — 2026-09-08

- Full Designer core: **1,875 tests across 217 suites**, zero failures, errors
  or skips, with opt-in Flutter 3.44.8 SDK contracts enabled.
- Final NetBeans/SDK regression: **648 tests across 22 suites**, zero failures,
  errors or skips. This includes all **157 mutation-controller integration
  tests**, source restaging, history budgets, paired persistence, native guarded
  sections, property editors, Events, wizard and accessibility contracts.
- Real SDK checks repeat the 25-case controlled-value workflow and the composite
  controller/shared-state workflow before and after field rename. The 21 ListTile
  cases and Stateful callback/lifecycle check also pass.
- **Nine NBM metadata/runtime-content checks** pass, now explicitly requiring the
  State editors, binding/rename/projection classes and schema 15 in the package.
- `verify`, local Maven `install`, and both root/module `nbm:cluster` builds pass.
  Built module, NBM and both clusters match all **1,619 module payload entries**
  excluding the packaging-adjusted manifest. Designer and Dart analysis dependency
  JARs match byte-for-byte. NBM: **8,099,931 bytes**, built in the canonical
  `G:/MyProjects/java/project/netbeans-flutter-plugin-starter` checkout.

The new integration test initially exposed a real Save failure after a renamed
controller referenced user-added handler code. The ordered, authored-step history
fix resolves it without loosening generic overlapping Source-edit rejection; the
full regression above passed after that fix. No running IDE was restarted, no user
Flutter project was modified, and physical desktop verification is not claimed.

## Quick desktop check for State field management

1. Launch the rebuilt plugin and open a Stateful form with a bound control and
   a dependent property, for example TextField and Text using the same controller.
2. Open the control's event editor, select **State binding**, enter a new private
   name and click **Rename State Field**. The dialog should close without saving
   or navigating; both bindings should now show the new field name.
3. Open either binding again and click **Go to Field**. Source should open at the
   field declaration. The callback method name should be unchanged; controller
   listener and lifecycle references should use the new field name.
4. Verify Undo/Redo, Save, close/reopen and another property edit. For a dependent
   property's editor, selecting a different dropdown candidate must not retarget
   Rename State Field or Go to Field away from its currently bound field.

These are manual desktop checks, distinct from the automated native PropertySheet,
source/history and Flutter runtime tests.
