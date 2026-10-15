# AI Assistant Playground — UX/UI Proposal

> Produced by the `ui-review` subagent (read-only, no login → reviewed from source + structure).
> Verified facts added by the parent are marked **[verified]**. Implementation status is tracked at
> the end.

---

## Verification of the reviewer's open questions **[verified]**

| Question | Answer |
|---|---|
| Is `TabSheet` available? | Yes — Vaadin **25.2.5**; `TabSheet` is already used (`FilterGridTabSheet`, `ExperimentDetailsComponent`). |
| Is `GridPro` available? | Not used anywhere in the codebase → use `Grid` + `AppDialog`/inline toggle. |
| Does `AlertDialog` exist? | Yes — `views/general/dialog/AlertDialog` (app component, used by `ProjectAccessComponent`, `UiExceptionHandler`). |
| Duplicate download trigger? | **Confirmed bug**: `downloadForDesign` adds a new `DownloadComponent` to the draft area on every click → accumulating buttons. |

---

## A. Information architecture

**Diagnosis.** The page is a flat vertical scroll of ~5 unrelated sections. The headline flow
(describe → review → create) competes with the interview panel, the XLSX upload and four batch
buttons. After drafting, the review is read-only — there is no correction.

**Proposed structure — three tabs** (`Tabs`/`TabSheet`):

| Tab | Contents | Audience | Priority |
|---|---|---|---|
| **Study setup** | Project selector → describe → draft → **editable review** → confirm → download/create | demo audience | **primary** |
| **Interview** | existing guided slot-by-slot interview (prefix + chat + composition editor) | developers | secondary |
| **Evaluation** | XLSX upload + 4 batch buttons + results + progress | developers | tertiary |

**Moves**

- Project selector + validation-context label → shared header above the tabs.
- Experiment selector → **Interview** tab only (the primary flow explicitly does not need an
  existing experiment).
- Guided interview → Interview tab.
- XLSX upload + batches → Evaluation tab.
- Design review area → stays in Study setup, becomes **editable**.

**Remove / demote**

| Item | Action | Reason |
|---|---|---|
| Four batch buttons on the main surface | move to Evaluation | developer tools; demo audience never uses them |
| XLSX upload on the main surface | move to Evaluation | competes with the primary flow |
| "Active assistant: LiteLlmAssistant" | demote to footer/tooltip | implementation detail |
| `<hr>` separators | delete | tabs replace them |
| Experiment selector at the top | move to Interview | not needed by the primary flow |
| Accumulating "Download pre-filled sheet" buttons | **fix** (one entry point) | confirmed bug |

---

## B. Wireframes

### B1. First screen — Study setup, empty

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  AI Assistant Playground                                                    │
│  ┌─────────────┬────────────┬──────────────┐                               │
│  │ Study setup │ Interview  │ Evaluation   │                               │
│  └═════════════┴────────────┴──────────────┘                               │
│                                                                             │
│  Project: [▾ Select a project …                                     ]      │
│  Validation context: <label>                                                │
│                                                                             │
│  ┌─── Describe your study ─────────────────────────────────────────────┐   │
│  │  Describe the study in a few sentences — samples, species, tissue,  │   │
│  │  analytes, treatment arms.                                          │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │  e.g. 20 samples from a mouse liver study: 10 treated,      │   │   │
│  │  │  10 control, DNA extracted; the readout is proteomics        │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  │  [ Draft experiment & samples ]                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### B2. Draft review with correction affordances

```
┌─── Experiment draft: MouseLiver_Treatment ── review ────────────────────┐
│  ℹ️ Exploratory sampling; design provisional                            │
│                                                                          │
│  ┌─ Design ───────────────────────────────────────────────────────────┐ │
│  │  Experiment name   [MouseLiver_Treatment        ] [✎]             │ │
│  │  Species      🏷 Homo sapiens [NCBITaxon:9606]  [✎] [×] [+ add]   │ │
│  │  Specimens    🏷 liver [UBERON:0002107]         [✎] [×] [+ add]   │ │
│  │  Analytes     🏷 DNA [CHEBI:16991]              [✎] [×] [+ add]   │ │
│  │                                                                    │ │
│  │  Variables                                                         │ │
│  │  ┌────────────┬──────────┬──────────────────┬───────┐             │ │
│  │  │ Name       │ Unit     │ Levels           │       │             │ │
│  │  │ Treatment  │ —        │ treated, control │ [✎] 🗑│             │ │
│  │  └────────────┴──────────┴──────────────────┴───────┘             │ │
│  │  [+ Add variable]                                                  │ │
│  │                                                                    │ │
│  │  Groups                                                            │ │
│  │  ┌────────────┬──────────────────────┬───────────┬───────┐        │ │
│  │  │ Name       │ Condition            │ Replicates│       │        │ │
│  │  │ Treated    │ Treatment=treated    │ 10        │ [✎] 🗑│        │ │
│  │  │ Control    │ Treatment=control    │ 10        │ [✎] 🗑│        │ │
│  │  └────────────┴──────────────────────┴───────────┴───────┘        │ │
│  │  [+ Add group]                                                     │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌─ Samples ──────────────────────────────────────────────────────────┐ │
│  │  20 samples total                                                  │ │
│  │  ┌───────┬──────────────┬───────────┬────────────┬───────┐        │ │
│  │  │ Count │ Species      │ Specimen  │ Analyte    │       │        │ │
│  │  │ 10    │ Homo sapiens │ liver     │ DNA        │ [✎] 🗑│        │ │
│  │  │ 10    │ Homo sapiens │ liver     │ DNA        │ [✎] 🗑│        │ │
│  │  └───────┴──────────────┴───────────┴────────────┴───────┘        │ │
│  │  [+ Add combination]        Σ Sum: 20 / 20 ✅                      │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌─ Verification reports ─────────────────────────────────────────────┐ │
│  │  ✅ All values verified   — OR —                                   │ │
│  │  ⚠️ "mouse" could not be resolved as a species term.               │ │
│  │     Alternatives: Homo sapiens [NCBITaxon:9606],                   │ │
│  │     Mus musculus [NCBITaxon:10090]                                 │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌─ Actions ──────────────────────────────────────────────────────────┐ │
│  │  ✅ Structurally complete                                          │ │
│  │  [ Download sample registration sheet ]                            │ │
│  │  [ Create experiment in QBiC-001 ]   ← opens confirm dialog        │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

### B3. Pre-write confirmation (`AlertDialog`, warning)

```
┌─ ⚠️ Create experiment ───────────────────────────────┐
│  This will write real data to project QBiC-001:      │
│  • Experiment: MouseLiver_Treatment                  │
│  • 1 variable (Treatment), 2 groups, 20 samples      │
│  • Species: Homo sapiens · Specimen: liver · Analyte: DNA │
│  This uses your permissions. The assistant has no    │
│  identity of its own.                                │
│           [ Cancel ]   [ Create experiment ]         │
└──────────────────────────────────────────────────────┘
```

### B4. Degraded state (contained banner, not a toast)

```
┌─── Describe your study ────────────────────────────────────────────────┐
│  ⚠️ The AI assistant is temporarily unavailable (HTTP 502 …).          │
│     You can still use the Interview tab to set up samples manually.    │
│     [ Try again ]                                                      │
│  [ Draft experiment & samples ]  ← re-enabled                          │
└────────────────────────────────────────────────────────────────────────┘
```

---

## C. Editing model

| Edit target | Component |
|---|---|
| Experiment name | inline `TextField` (✎ toggle) |
| Species / specimen / analyte | chip with `[✎] [×]`; edit opens a resolver-backed `ComboBox`; `[+ add]` appends |
| Variables | `Grid` + `AppDialog` for add/edit (name, unit, levels); `[✎] 🗑` actions |
| Groups | `Grid` + `AppDialog` (name, condition constrained to variables, replicates ≥ 1) |
| Composition | `Grid` (count, species, specimen, analyte) with inline add row; reuse the existing `CompositionRowEditor` pattern |

**Add / remove / edit:** dialog or inline toggle → model mutation → grid refresh → **immediate
re-verification**. Removal uses `AlertDialog` confirmation.

**Term correction:** `ComboBox` fed by the existing `OntologyTermResolver` /
`BridgedOntologyResolver` (species/specimen/analyte). Unresolvable input shows helper text; the
resolver is the same infrastructure the interview already uses.

**Dirty state + re-verification:** after every edit, run `DesignVerifier.verify(...)` on the
`interactiveExecutor`; show "Re-verifying…"; keep Download/Create disabled while dirty or in
flight; the reports section stays always visible.

**Inline validation:** replicates ≥ 1, counts ≥ 1, sum indicator `Σ X / Y`, condition levels must
reference existing variables (deleting a variable produces reports for dependent groups).

### C6. Re-prompt / refine the draft (**additional MUST**)

Manual editing fixes *values*; re-prompting fixes *intent*. If the model misread the study
("10 per group, not 10 in total"; "it is rat, not mouse"), editing every cell is the wrong
interaction — the user should state the correction once and have the draft regenerated.

```
┌─ Refine ─────────────────────────────────────────────────────────────┐
│  Previously applied: "10 per group" · "use rat"                      │
│  Tell the assistant what to change…                                   │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │ 10 mice and 10 rats per group, liver, DNA                    │    │
│  └──────────────────────────────────────────────────────────────┘    │
│  [ Refine draft ]                                                    │
└──────────────────────────────────────────────────────────────────────┘
```

Semantics: the request carries the **original description + the current draft (JSON, including
manual edits) + the new change request**; the model returns the **full updated draft** applying
only the requested changes. The current draft is the base, so refinement and manual editing
compose rather than overwrite. Deterministic re-verification, reports, badges and dirty-gating
behave exactly as after a manual edit. Feedback history is bounded (last ~3 turns).

**Failure safety:** on a failed/degraded refine the **current draft stays untouched and usable** —
the degraded notice is shown and the manual path remains. Never clear the draft on failure.

---

## D. States

| State | User sees | Primary action |
|---|---|---|
| Empty | project selector + describe card | Draft enabled when project + text |
| Loading | "Drafting experiment design and samples…" | disabled |
| Verified clean | review + ✅ | Download + Create enabled |
| Verified with reports | review + ⚠️ reports | download disabled on structural issues; create disabled on domain issues |
| Dirty | "Re-verifying…" | Download/Create disabled until done |
| Degraded | banner in describe card + Try again + Interview pointer | Draft re-enabled |
| Write in progress | confirm dialog, disabled confirm + spinner | — |
| Write success | "✅ Validates clean against real experiment 'X'" + context switched | download |
| Write partial failure | per-step reports (created / failed) | re-enabled |

---

## E. Prioritised recommendations

**MUST** — 1. correction affordances on the draft (name, terms, variables, groups, composition:
add/edit/remove); 2. re-verify after every edit; 3. confirmation dialog before Create;
4. tab the page (Study setup first); **4b. re-prompt / refine the draft with a change request
(see §C6) — manual edits fix values, re-prompting fixes intent**.

**SHOULD** — 5. resolver-backed term editing; 6. degraded state as a contained banner with
"Try again"; 7. dirty-state indicator; 8. fix the accumulating download component; 9. visual
grouping of the review area (Design / Samples / Reports / Actions).

**COULD** — 10. inline editing for scalars; 11. chips with per-chip edit buttons; 12. collapsible
reports (collapsed when clean).

**Remove/demote** — see table in §A.

---

## F. Overall impression

> The backend architecture is excellent — the separation of LLM proposal, deterministic
> verification and human-confirmed write is well designed. The UI fails to communicate it because
> everything is stacked on one scroll, the primary flow competes with developer tools, and —
> critically — there is no way to correct a wrong draft. The highest-impact improvement is adding
> edit affordances with automatic re-verification, because without it the loop "LLM proposes,
> system verifies, human decides" is broken at the last step: the human can only accept as-is or
> start over.

---

## Implementation status

- [ ] MUST 1 — correction affordances
- [ ] MUST 2 — re-verify after every edit
- [ ] MUST 3 — pre-write confirmation dialog
- [ ] MUST 4 — tabbed page
- [ ] MUST 4b — re-prompt / refine the draft (§C6)
- [ ] SHOULD 5–9
- [ ] COULD 10–12
