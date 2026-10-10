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
| Stakeholder direction | Confirmed target: no drawer, two-row top navigation, centered dynamic-width content |

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
   must be replaced by a two-row top navigation (project row + experiment row).
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
    Manager" wordmark lives in the drawer and the navbar has no app identity, search or account name
    (the avatar shows no name or chevron). Removing the drawer deletes the wordmark and leaves the
    hamburger dead — so the rework must move identity into row 1 of the new navigation.

## Proposed future state (AI proposal, revised for the confirmed direction)

### Navigation model — two-row top navigation, no drawer

- **Row 1 (global / shell):** Data Manager home link, project selector (code + title, dropdown for
  recent projects), global search, account menu. Sticky.
- **Row 2 (project scope):** project-level destinations as a horizontal row — `Overview`,
  `Experiments` / experiment switcher, `Datasets`, `Access`, `Settings`, plus project tools such as
  ontology search. When the user is inside an experiment, row 2 gains a clear experiment context
  (breadcrumb + experiment switcher) and the four experiment views `Design · Samples ·
  Measurements · Raw Data` appear as a third, lighter row (or as an inline segmented control with
  count badges and no arrows).
- **One navigation model for both layers:** the user always sees where they are in the
  project → experiment → view hierarchy. No arrows implying a wizard.
- Each experiment view tab gets a distinct, meaningful icon and noun labels with counts.

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

- **One loud band, one centred column.** Row 1: wordmark + project dropdown + search + account (fixes
  issue 28). Row 2: project destinations as horizontal items marked by **weight plus a bottom rule**,
  not a filled pill. Render code + title once. The objective is clamped to ~3 lines with "Show more"
  — never a grey collapsed bar. Give the page one unmistakable focal element and keep surrounding
  bands quiet.
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
   measurements registered → raw data available → QC uploaded), rendered as a compact step indicator
   with counts and warnings ("2 experiments without samples", "1 measurement without raw data").
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
2. **Experiment sub-navigation:** third row vs. segmented control inside the page — which reads
   better once row 2 is present?
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

**Status: no code has been written. This document is analysis only.**
