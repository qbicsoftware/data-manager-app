# UI review — experiment-scoped Samples page (`img_15.png`)

**Status:** review of the first rendered snapshot of the rework (`feature/sample-page-rework`,
commit `3fa41f96f`). Capture is 1841×1056.
**Reviewer:** `ui-review` subagent, read against the design intent in
[`sample-page-ux-draft.md`](sample-page-ux-draft.md) rather than against generic taste.
**Outcome:** three blocking defects found, all in the new code, all confirmed by reading the
source. Fixed in the commits that follow this document — see "Fixes applied" at the end.

---

## 1. What the capture rendered

- **Chrome:** breadcrumb `Home / Q2IMMU / Immunotest / Smoke Test Edit Immunopeptidomics`, tab strip
  with **Samples** active. Orange test-instance notice above the page.
- **Band A:** `H1` "Samples" with `299 samples registered` running directly into it; `+ Register
  samples` (solid blue) far right.
- **Band B:** absent — correct, samples exist.
- **Band C:** wide search field, `Export` (pale/disabled), `Edit`, `Delete` (grey), `Show/Hide
  Columns`. No filter chips (expected: that is the draft's steps 4–5, not implemented).
- **Band D:** header checkbox, `Sample ID ^` (sorted), `Sample name`, `Condition` (blue pill chips),
  `Batch`, then an unlabelled chevron column. 12 rows.
- **Band E:** **no pager visible.**
- **Band F:** present, but as a **full-width card stacked below the grid**, not a right-hand column.
  Cut off by the viewport bottom.

## 2. Defects

### Blocking

| # | Defect | Root cause (verified in source) |
|---|---|---|
| 1 | Panel rendered as a full-width block **under** the grid instead of a right-hand column at ≥1100px | `sample-page.css` used `var(--sample-detail-panel-width)` as a grid track, but **the token was never defined**. An undefined custom property makes the whole `grid-template-columns` declaration invalid at computed-value time, so it collapsed to a single implicit column. |
| 2 | No pager anywhere | Consequence of #1: the panel grew full-width and tall, pushing `paginationBar` (correctly ordered after `detailLayout`) below the fold. Not a separate bug. |
| 3 | `Samples299 samples registered` — H1 and status line welded together | `.sample-header-title-block` and `.sample-status-line` were applied in Java but **never styled**. Two inline `Span`s in a `Div` run together without an explicit `flex-direction: column`. |

### Non-blocking, fixed alongside

- **No visual link between the open record and its row.** The absence of a selection tint is
  *intentional* (draft §10: opening a record must not add it to the bulk selection, or a later
  `Delete` hits an inspected-only sample). But the reviewer's underlying point held: nothing
  anchored the panel to its row. Added a dedicated `open-detail-row` part, distinct from selection.
- **Panel hierarchy inverted:** bold values read nearly as large as the panel `H2`, and the group
  headings were the palest text on screen. Values now take normal weight; headings take
  `--field-label-text-color`.
- **Label/value rhythm:** the label→value gap equalled the value→next-label gap, so pairs did not
  group.
- **Load-failure banner** was styled nowhere (`.sample-error-state` had no rules).

## 3. Accepted as by-design / out of scope

- **Unchecked row while the panel is open** — intentional, see above.
- **No quick-filter chips** — the draft's steps 4–5, which need backend changes to
  `SamplePreviewFilter`/`SamplePreviewSpecs`.
- **Panel subtitle shows only the sample code**, not `${sampleCode} · ${experimentName}`. The draft
  specified the experiment name, but `SamplePreview` carries no experiment name and the page is
  experiment-scoped, so it would repeat a constant already in the breadcrumb. **Left as code only;
  open for the product owner.**
- `Batch = "What a Save!"` on every row is test data, not a rendering fault.

## 4. Confirmed working

The reviewer's verdict on the core premise: *"The identity-first premise does read in Band D: ID →
name → condition chips → batch scans quickly, and dropping the constant columns genuinely
de-clutters."* The single primary action is unambiguous.

## 5. Not verifiable from a static capture

Row hover/focus, the `<1100px` drawer branch, `Show/Hide Columns` reachability of the eight retired
fields, the Band B states (neither visible here), dark mode, and whether the panel scrolls
independently of the pager.

## 6. Fixes applied

1. Defined `--sample-detail-panel-width` (and documented why its absence was silent).
2. Styled `.sample-header-title-block` (column flex) and `.sample-status-line`.
3. Added `Grid.setPartNameGenerator` → `open-detail-row`, consumed as
   `vaadin-grid::part(open-detail-row)`, refreshed on open/close.
4. Value weight → normal; group headings → `--field-label-text-color`; field/group gap corrected.
5. Styled `.sample-error-state`.

**Regression guard:** `SamplePageStylesheetSpec` cross-checks every custom property the sample page
consumes against the properties the theme defines. Verified to fail, naming
`--sample-detail-panel-width`, when the definition is removed — i.e. it reproduces this exact bug.

**Not re-verified visually:** the fixes are confirmed by compile + unit tests only. This sandbox
cannot run the app (no MariaDB/Artemis; Vaadin cannot write `~/.vaadin`, so the frontend bundle does
not build). A fresh capture is required to confirm the panel is now a right-hand column.
