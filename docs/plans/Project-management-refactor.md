# Status quo research data management of research projects

Currently, the design and experience of managing research projects in the Data Manager app is based
on
an initial version and experience of the development team. Everything follows a linear pattern of
steps
required to fulfill metadata requirements until actual measured data can be registered. This
document shall describe the
status quo in more detail and highlight actual pain points for users. Subsequently, this document
will outline the
desired future state of the project management experience in the Data Manager app.

## The landing page within a project

Currently, users are forced to create a new project with an experimental design and its metadata
before they can
start registering data or even share the project with collaborators. This is not ideal for users who
want to
start working on a project right away. Users are directly prompted with an exhaustive wizard dialog
without actually
knowing what they are doing (and required doing, such as the experimental design). The learning
curve for new users
is too high, and the app does not support in-context guidance to help users understand what they are
doing and why.

When they are finally done with the project creation, they are presented with a landing page that
shows the project metadata but no guidance on how to proceed.
Later in the progression of the project, users are not presented with important information about
the progress status of different experiments (samples registered, measurement metadata registered,
data registered, etc.).
This makes it difficult for users to understand the status of their project and what steps they need
to take next.

Also, users don’t have any way to quickly access the project data or jump into continuation of
metadata registration.

The experiment side-car only carries the experiment name and no contextual information about the
experiment. It also consumes a lot of space in the UI at the same time
without doing anything useful except for showing the experiment name and allow navigation to the
experiment page. The offer side-car is obsolete and should be removed.
The experiment information is an aggregation of all experiments thrown together, which does not help
users at
all.

### Ideas for improvements

The landing page of a project shall give users an immediate overview of the project status and
provide quick access to the project data and metadata registration. The landing page shall also
provide guidance on how to proceed with the project metadata registration.
Modification of project description and metadata such as funding or contacts belong into a project
settings page.

The project goal is an important part of the project metadata and should be displayed prominently on
the landing page. But since
it can carry a lot of prosa, it should be displayed in a more compact way with the possibility to
expand it.

The creation of a new project shall be a one-step process, where users can provide all necessary
information in a form and be a dedicated
route (e.g. projects/create). Instead of dialogues, a dedicated page shall be used to collect all
necessary information. Project contacts such as PI and PM shall remain mandatory fields. Projects
that carry ORCID information about contacts shall be rewarded with a badge, since it promotes good
research data management practices (PIDs as in FAIR data)

---

# AI-assisted analysis and proposal

> **Provenance note.** Everything from this heading down to the next `#` heading was produced by AI
> on 2026-10-10: a code read and proposal by an AI coding agent
> (`deepseek-ai/DeepSeek-V4.1-Flash`, pi coding agent), plus an independent visual review by the
> `ui-review` subagent with the `frontend-design` skill explicitly applied. Both passes used the two
> screenshots `img.png` and `img_2.png`. The human-authored material above ends at the previous
> paragraph. Content below is a **proposal**, not an approved decision, and has not been reviewed by
> a human. Keep this boundary intact when editing: append new AI findings under this heading, and
> mark any human additions/edits explicitly.

## Method and evidence base

| Source | What it contributed |
|---|---|
| Code read | Component/CSS evidence for every structural issue (class names, route params, CSS rules) |
| Screenshots `img.png`, `img_2.png` | Rendered evidence: what actually appears on the page |
| `ui-review` subagent + `frontend-design` skill | Independent visual/UX critique: hierarchy, typography, colour, empty states, copy, generic-default tells |
| Stakeholder direction | Confirmed target: no drawer; the **in-project** navigation becomes a second top row beneath the existing primary navigation; experiment context forms a third, lighter row; centered dynamic-width content |

Findings below are marked **[code]** when grounded in source, **[render]** when observed in a
screenshot, and **[design]** when they come from the `frontend-design` critique. Issues both passes
found carry multiple tags.

## Confirmed direction (stakeholder input, 2026-10-10)

The following decisions were given by the project owner during the analysis and supersede parts of
both the human-authored section and the AI proposal below:

1. **Remove the left navigation drawer entirely.** Project navigation moves into the top area.
2. **Introduce a second navigation row in the top bar.** There is enough horizontal space; the
   project-level navigation becomes a horizontal top row instead of a vertical drawer.
3. **Center content on wide screens with dynamic spacing**, so the user's eye does not have to
   travel far between the nav, the page title and the content.

## What the screenshots show

The right-hand rail in both screenshots is not a single "side-car": it is **three stacked
`PageArea` panels** produced by a fixed CSS grid in
`datamanager-app/frontend/themes/datamanager/components/grid-templates.css`:

```css
.main.project {
  grid-template-columns: minmax(min-content, 80%) minmax(min-content, 25%);
  grid-template-areas:
    "projectdetails experimentlist"
    "projectdetails offerlist"
    "projectdetails qualitycontrollist";
}
```

The left column is `ProjectSummaryComponent`; the right column is `ExperimentListComponent` +
`OfferListComponent` + `QualityControlListComponent`, all added together in the
`ProjectInformationMain` constructor:

- **Experiments rail** — a name-only `ListBox` (`ExperimentListComponent`); selecting an entry routes
  away to the experiment page. No counts, no status, no context.
- **Offers rail** — an empty `VirtualList` plus an upload button (finances concern, unrelated to the
  project summary).
- **Sample QC rail** — an empty `VirtualList` plus an upload button (per-experiment QC reports,
  unrelated to the project summary).

On a freshly created project the right 25% of the viewport is three empty boxes, which is exactly
what `img.png` shows.

## Issue inventory (tagged by evidence source)

### Layout and visual

1. **[code][render][design] Fixed 80/25 rail wastes a third of the page on empty containers.** The
   panels do not collapse to min-content, so an empty `VirtualList` still renders a full bordered
   `PageArea`. Both passes found this. The render shows the two empty panels each holding a heading
   plus one button and then **~300–850 px of blank space** — the emptiest region owns the most room,
   and the full-height rail hairline makes it read as a *broken layout*, not a panel. Action buttons
   right-align to x≈1240 while the content they act on ends ~400–500 px earlier.
2. **[code][render] Nested scroll containers.** `DetailBox` hard-codes a 10 rem content height with
   the `overflow-scroll-height` class (see its javadoc: "the height is currently restricted to 10rem
   … the overflow will trigger a scrollbar"). Species/Specimen/Analytes therefore scroll inside a
   page that already scrolls. The render shows the scrollbar clipping a **third row mid-text**, so an
   ontology ID looks truncated or lost — a second scrollbar plus apparent data loss. This contradicts
   the direction already taken in ADR-0007/ADR-0009 and the pagination requirements.
3. **[code][render][design] Objective is a collapsed grey disclosure.** `buildDesignSection` wraps
   the objective in a `CollapsibleDetails` that is collapsed by default. The render shows a **~760 px
   pale bar containing just the word "Objective" and a chevron** — the most important narrative
   metadata is one click away behind a bar that is 90% dead space, while empty rails own a third of
   the page.
4. **[code][render][design] Weak, inconsistent edit affordances.** Bare `Edit` buttons float at the
   far right of full-width sections, separated from the content they edit. In the render the
   Funding/Contacts `Edit` buttons sit isolated in whitespace, in grey-on-pale-blue that reads as
   *disabled*, and they are shaped identically to `Export Project Summary`, `Add` and `Upload` — so
   the page has **no primary action at all**. `Edit` is reused for three unscoped things.
5. **[code][render] Duplicate project identity.** `ProjectMainLayout` renders "Q2IMMU - Immunotest" in
   the navbar and `buildHeaderSection` renders the same string again as the page heading; the drawer
   compounds it by calling the same project just "Immunotest".
6. **[code] Drawer state differs between project and experiment pages.** `ProjectMainLayout` calls
   `setDrawerOpened(true)`; `ExperimentMainLayout` does not. The two layouts also build different
   navbar DOM, so the chrome shifts when navigating into an experiment. This becomes moot once the
   drawer is removed, but the two layouts still need to converge on one navbar.

### Navigation and information architecture

7. **[code][render][design] Two competing navigation paradigms.** The drawer holds project-level
   items (SUMMARY / DATASETS / ACCESS / EXPERIMENTS / Ontology Search) while a top tab bar holds
   experiment-level items (Design / Samples / Measurements / Raw Data). Nothing signals which
   hierarchy the user is in, and experiments sit as peers of SUMMARY although they *contain* four
   sub-views. This is the core IA problem and the reason the drawer cannot simply be restyled — it
   must be replaced. The primary navigation row already exists (`Projects · Groups · Settings`); the
   change is to promote navigation into a **second, in-project row** (project identity + `Summary ·
   Datasets · Access`) and keep experiment context on a third, lighter row.
8. **[code] The experiment tab bar is a false wizard.** `ExperimentNavigationComponent` inserts
   disabled arrow `Tab`s *between* the four tabs, implying a linear gated workflow, while the tabs are
   freely reachable independent views.
9. **[code] All four experiment tabs share one icon.** Every `RoutingTab` is constructed with
   `VaadinIcon.USER.create()` — Design, Samples, Measurements and Raw Data all render a person icon.
   Copy-paste defect.
10. **[code][design] Tab labels mix verbs and nouns:** "Design Experiment", "View Samples", "View
    Measurements", "Download Raw Data".
11. **[code][render] "SUMMARY" naming.** The drawer labels the landing page `SUMMARY`, the page
    header says "PROJECT", and the route is `/info`. Three names for one concept.
12. **[code] Experiment list is unbounded and contextless** — no counts, no status, no cap for
    projects with many experiments (`createExperimentSection` appends them all).
13. **[code][render] "Ontology Search" is visually orphaned** — last child of the drawer, styled
    primary, project-scoped but positioned after the experiment list as if global. It belongs with
    project tooling.
14. **[code] Home icon and project title both route to the summary** (`HomeLink` + a `RouterLink` to
    `ProjectInformationMain`), while there is no breadcrumb into an experiment sub-view.

### Content and information

15. **[code][design] "Experiment Information" is genuinely meaningless.** `loadExperimentInfo` unions
    species/specimen/analyte ontology terms across *all* experiments and dumps them into three boxes.
    It answers no user question — a user has to already know which experiment a term came from.
16. **[code][design] No progress or next-step information anywhere.** `ProjectOverview` already
    carries `ngsMeasurementCount`, `pxpMeasurementCount`, `ipMeasurementCount`,
    `connectedDatasetCount`, `openDatasetCount`, `restrictedDatasetCount`, `lastConnectedOn`, and
    `SampleInformationService.countSamplesForExperiment` exists — but `ProjectSummaryComponent` only
    uses the three measurement counts to render type tags (`buildTags`). Sample counts and
    per-experiment progress are not surfaced at all.
17. **[code][render] Offers and Sample QC are misfiled.** Both are project-level `PageArea`s inside
    the *summary*. Offers are finances; QC reports are per-experiment artifacts (the domain model
    already carries `QualityControlUpload.experimentId`).

### Code health (relevant because the rework touches this code)

18. **[code] Add-experiment logic is duplicated verbatim** between `ProjectInformationMain` and
    `ProjectSideNavigationComponent` (`showAddExperimentDialog`, `showCancelConfirmationDialog`,
    `onExperimentAddEvent`, `routeToExperiment`, `createExperiment`,
    `displayExperimentCreationSuccess`). Two entry points, two copies of one orchestration.
19. **[code] `ProjectSummaryComponent` is a `@UIScope @SpringComponent` with mutable section/detail-
    box fields**, mutated by async `doOnSuccess` callbacks through `getUI().access`, and rebuilt in
    place by `reloadInformation`. Fragile state management that a rework should not inherit.
20. **[code] Minor:** `renderContactInfo` creates an `Anchor(..., oidcUrl)` with
    `AnchorTarget.BLANK` and no `rel="noopener"`.

### Visual design and interface quality (independent UI review, frontend-design lens)

These issues came from the independent visual review and are not visible from a code read alone.

21. **[design] No centre of gravity; flat typographic hierarchy.** Seven band headings ("Project
    Design", "Experiment Information", "Funding Information", "Project Contacts", "Experiments",
    "Offers", "Sample QC") share one ~20 px slate-grey weight. They compete with each other and lose
    to the H1. Hierarchy rests on *grey-vs-dark* rather than type scale, so a section title reads as a
    secondary label — the opposite of its intent. Effectively one heading size and one body size.
22. **[design] No primary action anywhere.** `Edit`, `Export Project Summary`, `Add` and `Upload` are
    all the same pale pill. There is no visual primary/secondary distinction, so the user cannot tell
    which action matters (see also issue 4).
23. **[design] Empty states carry no message.** Three blank panels give zero guidance and no next
    step. An empty screen should be an *invitation to act*, not dead space with a heading.
24. **[design] Generic-default visual tells.** ALL-CAPS tracked "PROJECT" eyebrow; ALL-CAPS list
    labels (`SUMMARY` / `DATASETS` / `ACCESS` / `EXPERIMENTS`); one border-radius and one pale
    lavender-grey fill applied to every card regardless of hierarchy; monospace-looking micro-labels
    for ontology IDs, emails and ORCIDs; hyphen-joined `CODE - Title` meta. The pastel domain chips
    are the one element with a genuine point of view. (No gradient washes and no "→" appended to
    links, which is a point in the design's favour.)
25. **[design] Copy is grammatically mixed and ambiguous.** Verbs (`Export Project Summary`,
    `Upload`, `Add`, `Edit`) sit beside nouns (`Objective`, `Species`, `SUMMARY`). Navigation mixes
    verbs and nouns. "Experiment Information" labels a code union, not a question a user actually
    asks (see issue 15).
26. **[render] Chips double-encode and encode nothing.** Ontology chips are pale badges with the link
    underline *inside* the chip — two stacked affordances for one target. All terms render in one
    colour, so their origin is indistinguishable, and the three domain-chip accent colours
    (pink/lavender/amber) carry no information.
27. **[render] The test-instance banner outranks the project title.** The amber "Please note that
    you are on a test instance." banner uses warning colour for informational text, repeats on every
    page, and is the loudest element on an otherwise pale screen — so the eye lands on a diagnostic
    notice rather than on the project.
28. **[design] Branding is coupled to the drawer; the navbar carries no identity.** The "Data
    Manager" wordmark lives in the drawer and the navbar has no account name (the avatar shows no
    name or chevron). Removing the drawer deletes the wordmark and leaves the hamburger dead — so the
    rework must move project identity into the new in-project row (the shell's primary row already
    carries what it needs).

## Proposed future state (AI proposal, revised for the confirmed direction)

### Navigation model — no drawer; in-project navigation as a second top row

The app already has a **primary navigation row** rendered in the outer shell by
`TopLevelNavigationComponent`: `Projects · Groups · Settings`, plus the brand and account menu. That
row is **not** part of this change.

- **Row 1 (primary / shell — exists today, unchanged):** brand (Data Manager), `Projects · Groups ·
  Settings`, account menu. Sticky.
- **Row 2 (in-project — new; replaces the drawer):** the project destinations `Summary · Samples ·
  Datasets · Access`, with project tools such as Ontology Search at the trailing edge. Active item marked by
  weight plus a bottom rule. **This row contains nothing project-specific**, so it starts at a fixed
  origin and renders identical geometry in every project.
- **Row 3 (context strip — always visible):** the breadcrumb `Projects / CODE / Project title`,
  extended with the experiment crumb when the user is inside an experiment, plus the three experiment
  views `Design · Samples · Dataset` with count badges and no arrows.
- **One navigation model for both layers:** the user always sees where they are in the
  project → experiment → view hierarchy. No arrows implying a wizard.
- Each experiment view tab gets a distinct, meaningful icon and noun labels with counts.

The project identity moves from the old navbar title (`CODE - Name`, set in `ProjectMainLayout`) to
the context strip, because row 1 is reserved for the shell-level primary navigation and row 2 must
stay position-stable.

**Length discipline (project titles are unbounded for layout purposes).** `ProjectCode` is a fixed
5 characters (`Q2` + 3), but `ProjectTitle` allows up to **180 characters**, which will not fit in a
navigation row. The identity is therefore kept **out of the navigation row entirely**:

- **Why:** if the project identity shared row 2, a long title would shift `Datasets` and `Access` to
  a different x-position in every project. The same target would never be in the same place twice,
  which defeats spatial memory — the main reason a nav row exists. The instability is caused by the
  **coupling**, not by the truncation, so truncating harder does not fix it.
- Row 2 therefore holds navigation only and is stable by construction.
- The project identity lives in the always-visible context strip (row 3) as a crumb, where it may
  truncate with the full text kept in a tooltip; the code is always shown in full.
- The title is also rendered in the page header (`H1`), where it can wrap freely.
- Rejected alternative: a fixed-width identity slot in row 2 would also stabilise the nav, but it
  reserves whitespace for short titles and truncates long ones early, while still coupling the two
  concerns. Kept out unless the PO prefers the identity to be prominent in the chrome.

### Data vocabulary and dataset scope (decided with stakeholders)

The rework surfaced a domain-vocabulary problem that is bigger than the UI and that the project page
cannot be honest without. The current app mixes three axes in one place: platform (measurement
counts), location (connected vs stored) and access (open vs restricted).

**What exists in the code today** (and collides):

| Code / table / UI | Actually is | Keyed by |
|---|---|---|
| `LocalRawDatasetEntry`, table `remote_measurement_data`, UI "Raw Data" | a summary of the **remote files produced by one measurement** (count, types, bytes, sync state) | `measurement_id` |
| `AssociatedDataset` | a **pointer to an external record** (Zenodo / InvenioRDM), optionally affiliated with an experiment | project, optional experiment |

So the same word, "dataset", names two unrelated things — and in the UI it is spent on the
*external* case while the researcher's own measured data is called "Raw Data".

**Decided vocabulary:**

- **Dataset** is the researcher's word and keeps the researcher's meaning: a collection of data.
  A dataset is **1:1 with its experiment**, because the experiment design is what gives the data its
  scope and makes it analysable.
- The experiment's dataset is **of kind `primary`**. "Raw" is deliberately dropped: it is relative
  and already wrong in practice (base-called reads are themselves processed raw signal).
  *Primary data* is standard research-data vocabulary and means the direct output of the measurement.
- **Measurement data** — renamed from `RawDataset` / "raw data" — is the per-measurement file
  summary that a dataset is composed of. It is *not* a dataset, and the class should stop being named
  one (this also fixes the class/table drift: `LocalRawDatasetEntry` vs `remote_measurement_data`).
- **Connected dataset** stays the name for an external record. The relationship is deliberately
  **unclassified**: such a record may be an analysis result, related material, or anything else, and
  "connected" is honest precisely because it does not claim to know which.

**Consequences for the UI:**

- The experiment view is **Dataset** (not "Raw data"); *kind* is a property, not a page name.
- The project **Datasets** page shows both origins, clearly separated: *Produced in this project*
  (one row per experiment dataset, kind `primary`) and *Connected datasets* (external, relationship
  unstated). This is the aggregation that is missing today — `remote_measurement_data` and
  `associated_dataset` never meet in the current UI.
- The project summary's "Where the project stands" must not fold connected datasets into the same
  count row as produced data; they are different facts.
- `RemoteRawDataService.hasRawData(...)` returns a **boolean**, so an experiment with 1 of 24
  measurements having data reports the same as one with 24 of 24. Any completeness statement needs a
  count, and must exclude `deleted = true`.

### Wide-screen layout — centered, dynamically spaced

- Introduce one shared content shell. `--content-shell-max-width: 1400px` already exists in
  `variables.css` and is applied to project-overview and access pages; extend the same shell to the
  project page and experiment pages so **all** main content is centered with `margin-inline: auto`.
- Use dynamic horizontal spacing rather than a fixed gutter: e.g. a fluid padding derived from
  viewport width (clamp) so the content column stays optically centered and the reading line length
  stays bounded on ultrawide monitors.
- Remove the fixed 80/25 project grid. The project page becomes a single centered column; any right
  rails that remain (e.g. quick jump) are intrinsic-width and collapse below a breakpoint.
- Cap text line length (target < 80 characters) for objective/description copy. The render showed a
  760 px-wide "Objective" bar with no prose measure at all.

### Visual design system (independent UI review recommendations)

- **One loud band, one centred column.** Row 1 is the existing primary navigation
  (`Projects · Groups · Settings`) with brand and account (fixes issue 28); row 2 is the new
  in-project navigation — project identity (`CODE · Name`, rendered once) plus project destinations
  as horizontal items marked by **weight plus a bottom rule**, not a filled pill. The objective is
  clamped to ~3 lines with "Show more" — never a grey collapsed bar. Give the page one unmistakable
  focal element and keep surrounding bands quiet.
- **Rails only when they have content.** Offers and Sample QC leave the summary entirely (issues 1,
  17, 23); the page becomes a single centred column with no fixed rail. Empty projects get an
  actionable checklist in sentence case — "Add your first experiment", "Register samples", "Upload
  measurement metadata" — where each line is a link that starts the action.
- **One type scale, one data face.** ~14 px / 1.5 body; one 30–32 px page title; 16 px near-black band
  headings (not grey, per issue 21); 13 px grey meta; a **single monospace face used only for
  identifiers**, so "this is an ID" is learned once. This replaces the grey-vs-dark hierarchy and the
  mono-looking ID micro-labels (issues 21, 24).
- **One name and one weight per action.** Exactly one solid primary action (`Add experiment`);
  everything else is text or outline (issue 22). Scope edit labels to what they edit — "Edit funding",
  "Edit contacts" — and anchor them to their heading. Drop the icon-only "+" (IA issue 7).
- **If a progress band is added, make it a genuine sequence.** Numbered or stepped markers are only
  legitimate when the content really is a sequence; show counts and exceptions ("2 experiments have
  no samples") rather than decorative 01/02/03.
- **Copy pass.** Sentence case, active voice, one job per string; every action keeps the same name
  through the flow; empty states explain what the panel is for and how to fill it (issues 23, 25).

### Project page as a cockpit (not a metadata dump)

Four bands, top to bottom:

1. **Identity band** — code + title (once), status chip, last activity, collaborator avatars +
   Share, primary actions (`Add experiment`, `Export`). The objective sits directly beneath,
   expanded but clamped to ~3 lines with "Show more" — never a collapsed grey bar.
2. **Progress band** — the missing piece. A lightweight read model repurposing/ extending the
   existing counts into per-experiment pipeline stages (experiment designed → samples registered →
   measurements registered → primary data registered → QC uploaded), rendered as a compact step indicator
   with counts and warnings ("2 experiments without samples", "1 measurement without primary data").
3. **Experiments band** — replace the name-only list with cards: name, domain tags, `n` samples /
   `n` measurements / `n` datasets, last activity, QC count, and direct jumps into Samples /
   Measurements / Raw Data. Add-experiment lives here.
4. **Metadata band** — design, funding, contacts, read-only with inline edit affordances; anything
   heavy moves to Settings.

Empty projects get an onboarding checklist instead of three blank sidecar boxes.

### Settings page (`projects/:projectId/settings`)

Description/design, funding, contacts, Offers and project deletion. Left sub-navigation (page-local,
not the removed app drawer). The landing page becomes read-only with quick-edit links. This removes
three of the sidecar boxes at once.

### Quick access and guidance

- "Jump to" strip: latest experiment, datasets, raw-data downloads.
- In-context guidance on first visit instead of a creation wizard; make experimental design
  deferrable (create with title + PI/PM, add the design when adding the first experiment).
- Project creation as a dedicated `projects/create` page with progressive disclosure; PI/PM stay
  mandatory.
- ORCID/PID badge on contacts (and project cards) wherever a contact carries `OidcInformation`,
  rewarding FAIR-aligned PID use.

### Cheap defects to fix in the same sweep

Converge the two layouts on one navbar; distinct per-view tab icons; noun labels; remove the arrow
"wizard" tabs; extract the duplicated add-experiment orchestration into one shared component;
stop using the fixed-height nested-scroll `DetailBox`; add `rel="noopener"` to OIDC anchors.

## Evidence base and source documents

- [`docs/ux-review/project-page-navigation-ui-review.md`](../ux-review/project-page-navigation-ui-review.md)
  — the full independent visual review (`ui-review` subagent, `frontend-design` skill). Its findings
  are merged into the numbered issues above (tags `[render]` / `[design]`) and into
  *Visual design system*; the report retains the raw evidence and severity tags.
- `img.png`, `img_2.png` — the two captures both passes were based on (repo root, currently
  untracked).

The review's own highest-impact recommendation agrees with this proposal: remove the three
right-rail panels and the fixed 80/25 grid so the page is one centred column whose loudest element is
identity plus an expanded-but-clamped objective, followed by a progress/next-step band and an
actionable checklist for empty projects. It also flagged things a single capture cannot settle —
hover and keyboard focus, narrow (<1100 px) and ultrawide behaviour, measured WCAG AA contrast, and
dark mode — carried into the open questions below.

## Open questions (need human/product decisions before implementation)

1. **Row-2 contents:** which project-level destinations belong on the project row, and does
   "Experiments" become a switcher, a list page, or both?
2. **Experiment sub-navigation (resolved 2026-10-10):** ~~third row vs. segmented control~~ — the
   breadcrumb / context strip already covers experiment-level orientation and the second navigation
   level within the project is sufficient. No third navigation row. Remaining detail: breadcrumb vs.
   in-page segmented control for the four views (see *Experiment page — navigation and onboarding*).
3. **Progress semantics:** which stages are authoritative; do we need a new
   `ProjectProgress`/`ExperimentProgress` read model and SQL view, or can it be composed from
   existing services?
4. **Offers:** remove from the project UI, or relocate to Settings? Requires PO/finance
   confirmation — it is the `finances` bounded context.
5. **Sample QC:** per-experiment placement vs. a project-level "Reports" area.
6. **Project creation:** is deferring experimental design acceptable, given the current wizard
   treats it as mandatory?
7. **Scale:** realistic experiments per project — determines whether the experiments area needs
   pagination / "view all" from the start (see FEAT-PAGINATED-LISTS).
8. **Personas:** do data stewards and PIs need different landing-page emphasis (steward =
   throughput/status, PI = metadata completeness)?
9. **Visual identity and accessibility:** the review found near-zero chroma (white / off-white / pale
   lavender / grey) with saturated colour used only for the warning banner and pastel chips. Is there
   an existing brand direction to design against, and do we define a real type scale plus a measured
   WCAG AA contrast budget (grey band headings, pale pill labels, ~11–12 px chips) as part of this
   work? Also unresolved: dark mode has no counterpart for the pale fills and blue chips, and the
   <1100 px / ultrawide behaviour of the old 80/25 grid needs to be replaced by the new centered
   shell rules rather than just deleted.
10. **Test-instance banner:** is a persistent amber warning banner the right treatment (issue 27),
    or should it be a quieter, dismissible environment indicator?
11. **Attention rollup and filtering:** the summary counts attention in *experiments* and the
    per-experiment rows carry the detail. Open questions: should there be a dedicated, filterable
    "attention" view across the project (rather than the inline list), and should the progress rail
    itself break down per experiment instead of showing a single project-wide count per stage? The
    rail currently aggregates (e.g. "54 measurements") which hides which experiment is behind.
12. **Dataset kind:** since a dataset is 1:1 with its experiment and its kind is currently always
    `primary`, is `kind` a property of *every* dataset (so it also classifies connected datasets) or
    only meaningful for the experiment dataset? And is a second kind (e.g. a derived/processed
    dataset that is still part of the experiment) expected later — which would break the 1:1 rule?
13. **Citable dataset identity:** with a 1:1 dataset, does anything need a stable, versioned dataset
    identifier (for publications or DOIs), or is the experiment identifier sufficient?

## Prototype (throwaway, development profile only)

A stakeholder-reviewable prototype of the corrected navigation model and landing page exists as a
self-contained demo view. It is deliberately isolated so it cannot affect production behaviour:

- **View:** `life.qbic.datamanager.views.demo.ProjectPagePrototypeDemo`
- **Route:** `test-view/project-page` — available only under the `development` Spring profile and
  `@AnonymousAllowed`; it renders canned data and touches no service, repository or route in the real
  application.
- **Styles:** `datamanager-app/frontend/styles/views/project-prototype/project-page-prototype.css`,
  loaded via `@CssImport`, with every selector namespaced under `.pt-app` so nothing leaks into the
  production theme.
- **Navigation model it renders:** row 1 = the existing primary navigation (`Projects · Groups ·
  Settings`); row 2 = the new in-project navigation (`Summary · Samples · Datasets · Access`,
  Ontology Search as a tool) with no project-specific content; row 3 = the always-visible context
  strip carrying the project identity plus the breadcrumb and, inside an experiment, the experiment
  views. The
  experiment list lives in the `Summary` experiments band, **not** behind its own navigation item.
- **It is navigable end to end.** The prototype routes in-view between dummy `Samples`,
  `Measurements`, `Dataset`, `Design`, `Datasets`, `Access` and `Ontology search` pages so the
  navigation can be exercised the way the final product would work. No real route or service is
  touched; the routing is component state. The vocabulary follows the decided data model: the
  experiment view is `Dataset` (kind `primary`), and the project page separates *Produced in this
  project* from *Connected datasets*.
- **The Experiments hub gates the pipeline.** With no experiment it opens on an onboarding hero
  ("Create your first experiment") and renders the pipeline `Experiment → Samples → Measurements →
  Primary data → Sample QC` with every downstream step **locked** (neutral dashed, not amber). A
  not-started experiment blocks Measurements with "Register samples first" and Dataset with
  "Register measurements first", each routing to the step that unblocks it. A locked step is a
  prerequisite, never an exception, so it is visually distinct from the per-experiment attention
  badge. Each experiment view also shows its own pipeline, so the user always sees what is done, what
  is next and what is not yet reachable.
- **The attention state is an entry point, not a label.** Every progress-rail stage is clickable and
  routes to its own surface, and each exception callout routes straight to the page where the work
  continues while naming that destination in the row (e.g. “Open measurements”). This is the answer
  to the review finding that the attention label said neither *how to proceed* nor *where to
  continue*.
- **Attention is modelled per experiment, not per project.** Several experiments can carry
  outstanding work at once, and each has its own destination, so the project summary renders **one
  callout row per issue, each naming its experiment** ("Solely Immuno · 12 of 30 measurements have no
  primary data attached · Open measurements"). Each experiment entry also carries its own attention
  badge, and the experiment's measurement page agrees with the count the callout states. A
  not-started experiment is deliberately *not* attention: it is a neutral "Not started" state.
- **Project-level rollups never pick an experiment arbitrarily.** The summary chip counts
  *experiments* ("2 experiments need attention") and reveals the per-experiment list rather than
  jumping into one of them. A rail stage whose problem spans several experiments does the same; only
  when exactly one experiment is affected does the click go straight to it.

The prototype is a discussion aid, not an implementation. It does not discharge the governance gate
below.

## Experiment page — navigation and onboarding (AI proposal, revised per stakeholder input 2026-10-10)

> **Human direction (project owner):** the breadcrumb already covers experiment-level orientation;
> the **second navigation level within the project is sufficient**. No further experiment-level
> navigation row is added. The experiment page must **guide the user to create an experiment first**,
> because samples, measurements and primary data cannot be registered without one.

### Navigation

The three rows already established for the project page are reused unchanged:

1. Row 1 — primary / shell navigation (exists today).
2. Row 2 — in-project navigation `Summary · Samples · Datasets · Access` (new; replaces the drawer). This is
   the **second navigation level** and is enough for project-level destination switching.
3. Context strip / breadcrumb — `Projects / CODE / Project title / Experiment title`, carrying the
   three experiment views (`Design · Samples · Dataset`) when inside an experiment.
   This is the **breadcrumb** that already exists in the prototype and is the only experiment-level
   orientation element.

**No additional navigation level is introduced.** The experiment views must not become a third
navigation row; they stay within the existing context strip.

### Onboarding order is a hard dependency chain

Registration is physically ordered and the UI must not pretend otherwise:

    Experiment  →  Samples  →  Measurements  →  Primary data (Dataset)

- With **no experiment**, samples, measurements and primary data cannot be registered at all. The
  experiment surface leads with **"Create an experiment first"**; the downstream views are shown as
  pending, never as empty registration forms that cannot succeed.
- With an experiment but **no samples**, Samples is the active next step; Measurements and Dataset
  remain pending.
- With samples but **no measurements**, Measurements is active; Dataset remains pending.
- With measurements but **no primary data**, Dataset is active — and is exactly where the
  per-experiment attention callout points.

Every empty state names its prerequisite and its action, mirroring the project page's checklist. A
blocked surface explains the dependency rather than rendering a dead form.

### Attention is local on the experiment page

The project summary rolls attention up to *experiments* ("n experiments need attention"). The
experiment page owns the same information at *view* granularity — e.g. "12 of 30 measurements have
no primary data attached" — and routes to the surface where the work continues. The project rollup
must never pick one experiment arbitrarily; the experiment page is where detail and action live.

### Overviews are inline, never nested (stakeholder review 2026-10-12)

> Human direction (project owner): a user starting from the **project page** or the **experiments
> page** must be able to get a quick overview of samples, measurements and data. Having these only
> behind the experiment-view pills is *nested and buried*.

Therefore the counts on an experiment entry are not enough. Each experiment entry carries an
**inline snapshot** — Samples / Measurements / Primary data — showing the count, a one-line status
and a few representative rows (e.g. `Q2IMMU-0001`, `Q2IMMU-MS-001`). The experiment page repeats the
same snapshot under its pipeline, so status and content are visible without opening a nested view;
clicking a snapshot opens the full view. This is the same principle the project page already applies
to attention (state the fact *and* route to the detail), extended to the data itself.

### One home per question (stakeholder review 2026-10-12)

> Human reaction: the experiment UI on the project page and the one reached from the experiments
> navigation looked like the same thing, so the difference was unclear.

The surfaces must have one job each and must not duplicate each other:

| Surface | Question it answers | Contains |
|---|---|---|
| Project → `Summary` | *How is the project doing, and what is in it?* | Identity, **project rollup**, the **experiment list with inline snapshots** (samples / measurements / primary data) and Add experiment, then project metadata. |
| Project → `Samples` | *Where is a given sample, and what do I have overall?* | One **flat, paginated, read-only** sample list across all experiments, with an Experiment column, **quick-filter buttons per experiment**, and export. |
| Project → `Datasets` | *Where are the measurements and their primary data?* | Measurement metadata and registered primary data shown **together**, plus connected (external) datasets; read-only + export. |
| Experiment (via breadcrumb) | *Where is this one experiment?* | The per-experiment pipeline (done / next / locked) and its own At-a-glance snapshot, plus the experiment's `Design · Samples · Dataset` views. |

The project page is the landing page and owns the experiment list; there is **no separate
"Experiments" navigation destination**. An experiment card with a snapshot is not rendered a second
time anywhere. The Summary's project rollup stays at project scope (aggregate counts and attention)
and does not repeat the per-experiment pipeline, which belongs to the experiment page.

### Project-level data surfaces (stakeholder decisions 2026-10-12)

> Decisions taken together with the data steward and project owner.

- **Samples get a project-level overview.** Row 2 becomes `Summary · Samples · Datasets · Access`.
  The Samples page is a flat, paginated, **read-only** list with an Experiment column and
  **quick-filter buttons per experiment**, plus export. It reuses the paginated sample list
  (FEAT-PAG-LIST-02) at project scope; `SampleInformationService.findSamplesByProjectId` already
  supports the query.
- **Measurements are absorbed into Datasets.** There is no separate Measurements destination:
  measurement metadata and registered primary data are shown together, so a measurement and its data
  are one fact. The experiment view likewise merges its Measurements and Dataset views into one
  `Dataset` view (`Design · Samples · Dataset`). A project-wide measurement filter is a small
  extension of the existing experiment-keyed `MeasurementFilter`.
- **Project-level data is read-only + export.** Registration stays experiment-scoped.
- **One flat list, not grouped sections.** Experiment attribution is a column plus quick-filter
  buttons, never grouping — grouping would recreate the experiment list inside the data list.
- The project rollup counts and attention rows link into these project-level lists.

**Open question (blocking the settings part):** exactly which edits move to a "settings" section —
all sample/measurement metadata editing, or only project metadata (funding, contacts)? And is that
settings surface project-scoped or experiment-scoped?

### How wide records are presented (strategy, 2026-10-12)

Samples carry ~10 user attributes; measurements carry far more and are type-specific (NGS ~12,
proteomics ~15, **immunopeptidomics 22** per the stakeholder spec). A union table would exceed 35
columns and be mostly empty. A flat list is therefore the wrong default.

**Three tiers: Scan → Inspect → Extract**

1. **Scan (list): identity + status only.** The list answers *which record do I need?* — not *what
does this record say?*.
2. **Inspect (detail): the full attribute set**, opened per record in a **side panel** (keeps the
list context). Attributes are grouped by the stakeholder spec's categories: Sample Identity,
Sample Properties, Sample Provenance, Analyte Properties, Organisational Provenance, Measurement
Provenance, Instrument, Run Parameters, Notes.
3. **Extract (export):** the complete record set via the existing Excel/TSV templates.

**Domain-aware, never unioned.** The Datasets surface segments by measurement type using the
**tab approach already implemented in production** (NGS / Proteomics / Immunopeptidomics). Only the
common identity columns are shown; type-specific fields live in the detail panel.

**Power mode (opt-in).** The production paginated grid can be offered as a "show all columns"
mode for power users who want to scan/curate the full table online instead of exporting first.

#### Provisional identity blocks (educated guess — to validate with the data steward)

| Record | List columns |
|---|---|
| Sample | QBiC Sample Id · Sample name · Experiment · Condition (experimental group) · Species · Batch · Biological replicate · **Status** |
| Measurement | QBiC Measurement Id · Measurement name · Experiment · Type · Instrument · # samples · **Primary data** · Registration date |

The guess is: *key + human label + experiment context + 1–2 biological/technical discriminators +
status*. Type-specific fields (e.g. MHC antibody, charge range) are deliberately excluded.

#### Provisional status set (educated guess)

- **Sample:** `Measured` (linked to at least one measurement) vs `Not measured`.
- **Measurement:** `Primary data` attached vs missing; `QC` passed vs pending where available.

Status is the only "attribute-like" thing that earns a list column, because it drives attention.

### Remaining question

- How the experiment views are reached within the existing context strip (the breadcrumb /
  experiment-view pills already prototyped) vs. an in-page segmented control. To be settled against
  the real `ExperimentMainLayout`, but **no new navigation row** is in scope.

## Governance gate (blocking, per AGENTS.md §0 and §12)

This is a new capability, not a refactor of existing behaviour. `docs/requirements.md` currently
contains **zero PROJECT requirements** (`PROJECT-R-01` / `PROJECT-NFR-02` appear only as schema
examples). Before implementation:

1. Author new requirement IDs — e.g. `PROJECT-R-xx` (guided project landing page with progress and
   next steps; separation of project settings), `PROJECT-NFR-xx` (navigation consistency; centered
   content shell; no nested scroll containers), and likely a FAIR-domain requirement for the
   ORCID/PID badge.
2. Create a parent **Feature** issue plus **Stories** (`FEAT-PROJECT-*`) referencing those
   requirements, recorded in `docs/features.md`.
3. Get human approval on the requirement change via a **dedicated requirement-only PR** with the
   mandatory changelog entry (agent rule: requirement changes require human approval).
4. Record the navigation-model decision and the progress read model as **ADRs** and update the ADR
   index (`docs/adr/README.md`).

**Status: analysis plus a development-only prototype. No production behaviour has been changed and
no real route or service has been touched.**
