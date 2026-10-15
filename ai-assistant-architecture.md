# AI Assistant Prototype — Architecture

> **Status:** prototype / evaluation phase. Everything described here lives behind
> `qbic.assistant.enabled` and a dev/demo-only UI route. Nothing is a production feature yet.
>
> Companions: [`ai-assistant-demo-runbook.md`](ai-assistant-demo-runbook.md) (how to run and demo it),
> [`ai-assistant-prototype-plan.md`](ai-assistant-prototype-plan.md) (evaluation strategy, corpora,
> promote/kill criteria).

---

## 1. Scope

| # | Capability | User-visible outcome | Surface |
|---|---|---|---|
| 1 | **Design + samples drafting** *(headline)* | A paragraph becomes a reviewed experiment design and a registration sheet | "Describe your study" |
| 2 | **Validation advice** | Uploaded metadata errors are explained, with a suggested fix | upload analysis |
| 3 | **Synonym bridge** | Colloquial/multilingual text resolves to a canonical vocabulary term | shared resolver |
| 4 | **Constrained interview** *(secondary)* | Slot-by-slot drafting against an already declared design | interview panel |
| 5 | **Evidence batches** | Scored regression runs for the promote/kill decision | batch buttons |

**Why #1 is the headline:** the real pain point is creating the experimental design *first*
(experiment name, declared species/specimen/analyte, variables, conditions, groups) before any
sample sheet can exist. The assistant therefore **produces the design as output**, instead of
consuming it as a prerequisite.

**Non-goals:** production UI, an assistant identity/permissions, new authoritative data sources.
Database writes exist in exactly one place — a human-confirmed creation step (§5.1, §8).

---

## 2. Principles

1. **LLM = translation layer at the human-language boundary.** It understands phrasing and proposes
   values. It does not own correctness.
2. **Determinism = correctness core.** Slot transitions, composition arithmetic, declaredness,
   ontology existence, domain invariants, sheet assembly. Never delegated to the model.
3. **Extract what was said; validate downstream.** The language layer must not pre-filter by
   declaredness or perform validation — that destroys the error signal.
4. **Every rejection has a visible channel.** A dropped value produces a message naming the value,
   the reason and the allowed alternatives. "Silently skipped" is a defect.
5. **Smallest defensible design, no fabricated science.** Prefer the minimal design that keeps data
   analysable; never invent a hypothesis, treatment, timepoint or variable.
6. **Ground before you show.** Model-proposed terms must be verified against an authoritative source
   before reaching a user.
7. **Review-first.** The assistant produces artifacts for a human. The only write is an explicit,
   human-confirmed action through existing ACL-guarded services.
8. **Degrade, never break.** Every model call is optional; failures render a clear unavailable state
   and leave the manual path usable.

Principles 3 and 4 are empirical: three prototype bugs were silent failure paths (a filtered
composition, an unreconcilable arithmetic mismatch, a discarded report).

---

## 3. Modules and layers

```
┌───────────────────────────────────────────────────────────────────────────────┐
│ datamanager-app — composition root (UI, configuration, dev/demo surface)      │
│                                                                               │
│ configuration/AssistantConfiguration   conditional beans (property gated)      │
│                                                                               │
│ views/demo/                                                                   │
│   AssistantPlaygroundView        @Route dev/assistant-playground (dev|demo)    │
│   PlaygroundTargetService        accessible projects/experiments (read-only)  │
│   DemoExperimentCreationService  the ONLY write path (human-confirmed)         │
│   ValidationTarget               real-context vs. fixture validation          │
│   SheetAnalysisPlan              failure grouping, clean-row skipping         │
│   RunScopedAssistant             per-run circuit breaker                      │
│                                                                               │
│ views/demo/draft/  (drafting bounded area)                                    │
│   ExperimentDesignExtractor      [port, in project-management]                │
│   DesignDraft / DesignDraftRows  verified design → sheet rows                 │
│   DesignVerifier                 ontology + domain + composition grading      │
│   DraftSheetChecker              structural sheet check (pre-creation)        │
│   SampleDraftInterview           constrained interview state machine          │
│   SampleComposition              (species, specimen, analyte) × count         │
│   FreeTextAnswerInterpreter      extraction → verification → reports          │
│   OntologyResolver / BridgedOntologyResolver                                  │
│   AssistantSampleSheetGenerator  production format + draft-format sheet       │
│   evaluation/                    corpora + pure scorers                       │
└──────────────┬───────────────────────────────────┬────────────────────────────┘
               │ depends on                        │ depends on
┌──────────────▼─────────────────────┐  ┌──────────▼─────────────────────────────┐
│ project-management (application)   │  │ project-management-infrastructure       │
│ application/assistant/             │  │ assistant/LiteLlmHttpClient             │
│   AssistantPort                    │  │ assistant/LiteLlmAssistant              │
│   OntologyTermSuggester            │  │ assistant/LiteLlmOntologyTermSuggester  │
│   SlotExtractor                    │  │ assistant/LiteLlmSlotExtractor          │
│   CompositionExtractor             │  │   (one class, two ports)                │
│   ExperimentDesignExtractor        │  │ assistant/LiteLlmExperimentDesignExtr.  │
│   evaluation/ (Task-1 harness)     │  │ assistant/AssistantProperties           │
│                                    │  │ ontology/TIBTerminologyService          │
│ Read-only services used by the UI  │  │ ontology/SpeciesTermRepository          │
│   SampleValidation                 │  │ organisations/CachedOrganisationRepo    │
│   MeasurementNGSValidator          │  │ sample/openbis/OpenBIS connector / mock │
│   SpeciesLookupService             │  │                                         │
│   TerminologyService               │  │                                         │
│   ProjectInformationService        │  │                                         │
│   ExperimentInformationService     │  │                                         │
│   SampleInformationService         │  │                                         │
└────────────────────────────────────┘  └─────────────────────────────────────────┘
```

---

## 4. Ports, adapters, prompt design

| Port (application layer) | Responsibility | Adapter |
|---|---|---|
| `AssistantPort` | Explain + suggest fixes for validation failures | `LiteLlmAssistant` (falls back to `RuleBasedAssistant`) |
| `OntologyTermSuggester` | Free text → candidate canonical terms | `LiteLlmOntologyTermSuggester` |
| `SlotExtractor` | Sentence → unverified scalar slot values | `LiteLlmSlotExtractor` |
| `CompositionExtractor` | Sentence → unverified (species, specimen, analyte, count) combinations | `LiteLlmSlotExtractor` |
| `ExperimentDesignExtractor` | Description → unverified design draft + composition | `LiteLlmExperimentDesignExtractor` |

**Wiring.** Extractor ports are backed by a single `LiteLlmSlotExtractor` bean registered with its
concrete return type, so each port resolves uniquely. Registering it twice makes both ports
ambiguous — pinned by `AssistantConfigurationSpec`.

**Transport.** One package-private `LiteLlmHttpClient` owns the HTTP client, endpoint, headers, body
construction, timeout and retry policy:

```
postChatCompletion(system, user)
  ├─ body: model, temperature 0, response_format json_object, messages[]
  ├─ 2xx  → raw body
  ├─ 5xx / timeout → retry ONCE (500 ms) → still failing → AssistantProxyException(status)
  └─ 4xx  → no retry
```

**Prompt structure (design extractor).** Three clearly separated parts:

```
┌──────────────────────────────────────────────────────────────────────────┐
│ DOMAIN PRIMER      what variables/levels, conditions, groups, replicates,│
│                    species/specimen/analyte, analysis methods, batch are │
│                    + the invariants the draft must satisfy               │
│                    (enumerations injected from code — never hardcoded)   │
├──────────────────────────────────────────────────────────────────────────┤
│ EDITORIAL POLICY   smallest defensible design · never invent a hypothesis│
│                    · exploratory sampling is legitimate · missing info is│
│                    not an error · at most 1-2 clarifying questions       │
├──────────────────────────────────────────────────────────────────────────┤
│ OUTPUT CONTRACT    strict JSON · exact field names · extract-as-spoken · │
│                    best-effort label/CURIE · empty arrays when unknown   │
└──────────────────────────────────────────────────────────────────────────┘
```

The analysis-method list is injected, so the prompt cannot drift from the domain; a test asserts it
is not hardcoded.

---

## 5. Capability flows

### 5.1 Describe → design + samples *(headline)*

```
project ▾  +  "20 samples from a mouse liver study: 10 treated, 10 control,
               DNA extracted; readout proteomics"
                              │
                              ▼  ONE model call (interactive executor)
        ┌───────────────────────────────────────────────────────────────┐
        │ DesignExtraction (unverified)                                 │
        │  experimentName · species/specimens/analytes (as spoken)      │
        │  variables(name, unit, levels[])                              │
        │  groups(name, levels[{variable, level}], replicateCount)      │
        │  composition[(species, specimen, analyte, count)]             │
        │  designComment ("exploratory/provisional" when minimal)       │
        └───────────────────────┬───────────────────────────────────────┘
                                ▼
        ┌───────────────────────────────────────────────────────────────┐
        │ DesignVerifier — deterministic, never throws, always reports  │
        │  1 ontology existence: species → local DB · specimen/analyte  │
        │    → TIB; stable IRI required; unresolved ⇒ report + excluded │
        │  2 domain grading: Experiment.create + addSpecies/addSpecimens │
        │    /addAnalytes, addVariableToDesign, addExperimentalGroup    │
        │    (blank names, empty levels, unit mismatch, unknown var ref,│
        │     duplicate condition, replicate < 1 ⇒ per-item report)     │
        │  3 composition: terms must be resolved; counts ≥ 1; total=sum │
        └───────────────────────┬───────────────────────────────────────┘
                                ▼
        review UI:  Design / Samples / Reports / Actions cards
                    · every element editable (name, terms, variables, groups,
                      analyte definitions) → deterministic RE-VERIFICATION after
                      each change; Download/Create disabled while dirty
                    · re-prompt: "tell the assistant what to change" → sends
                      description + CURRENT draft + bounded feedback (≤3 turns)
                      → returns the full updated draft, re-verified
                      (failure ⇒ the draft stays untouched)
                    [ Download sample registration sheet ]  (structural check)
                    [ Create experiment in <project> ]      (confirm dialog → explicit write)
```

**The sample sheet is a projection of a matrix.** The model is
`groups (condition) × biological replicates 1..n × analyte definitions`, where each analyte
definition carries species, specimen, analyte, **its own analysis** and a per-replicate sample
count:

```
for each group
  for each biological replicate 1..n
    for each analyte definition
      for k in 1..perReplicateCount
        row: species · specimen · analyte · analysis(per analyte)
             condition(group) · biologicalReplicate(replicate index)

reference study: 2 groups × 10 replicates × 2 analytes = 40 rows
  replicates 1..10 shared by the plant's mRNA and peptide row
  mRNA → analysis from the app's AnalysisMethod values
  peptide → analysis UNSET, reported; never guessed (a silent 16S default
            was a real defect)

EXCEL OUTPUT (legacy, preserved): generated FROM the matrix; the registration
sheet remains the current evaluation process while the matrix is the future model.
```

**Sheet generation, two stages.**

```
before creation      AssistantSampleSheetGenerator.generateDraftSheet(rows)
                     → plain xlsx, production "Sample Metadata" header layout
                       (no experiment-derived dropdowns: the template needs a
                        persisted experiment)
                     → DraftSheetChecker: structural completeness

after creation       AssistantSampleSheetGenerator.generate(projectId, experimentId, rows)
                     → production template (dropdowns from the real design)
                     → ProjectValidationTarget: re-validated against the real experiment
                       ✅ "Validates clean against real experiment '<name>'"
```

### 5.2 Synonym bridge

```
free text ─► LOCAL authoritative search (labels only)
                ├─ direct label hit ──► local candidates, no LLM call
                └─ no direct hit
                       ▼
                 OntologyTermSuggester (temperature 0)
                 "human" → {Homo sapiens, NCBITaxon:9606}
                       ▼
                 CONCEPT-EXISTENCE GATE (stable IRI required)
                   species  → local species database
                   specimen/analyte → TIB Terminology Service
                       ├─ not a real concept ──► dropped (counted, never shown)
                       └─ real concept ───────► "via synonym bridge" + human confirm
```

| Actor | Responsibility |
|---|---|
| LLM | Understand the setup; propose candidate concepts |
| System | Guarantee *existence*: real concept, stable IRI, validated against the slot's source |
| Human | Decide *meaning*: is this the concept I intended? |

### 5.3 Constrained interview *(secondary)*

Used when an experiment with a declared design already exists and the sheet is built slot by slot.

```
SAMPLE_COUNT → COMPOSITIONS → CONDITION → ANALYSIS → BATCH (optional)
                    │
        composition = list of (species, specimen, analyte) × count
        satisfied ⇔ sum == stated total
        relation:  UNSTATED | MATCHES | BELOW | ABOVE
                    │
    mismatch ──► [ Update total to N ]  [ Keep total T — I'll add/fix ]  [ Change total ]
    undeclared term ──► per-term report naming the term + available declared values
```

### 5.4 Validation advice (upload analysis)

```
upload XLSX ─► parse + validate (UI thread, user's SecurityContext)
                 ├─ clean rows ────────────► ✅ (no model call)
                 └─ failing rows ─► group by failure signature
                                      └─ ONE call per pattern
                                         (RunScopedAssistant: first degraded response ⇒
                                          rule-based for the rest of the run)
                 ▼
        background thread ── @Push ──► progress + grid
                                      (expandable rows: raw failures + full advice)
```

### 5.5 Evidence batches

| Batch button | Success metric | Trust metric |
|---|---|---|
| Run evaluation batch | field identification, category accuracy | valid auto-fix vs. baseline, hallucination-free |
| Run synonym bridge batch | existence validity, concept correctness | gate resolvability, **unintended-concept rate** |
| Run slot extraction batch | slot-fill rate | **false fills** |
| Run composition extraction batch | combination rate | **wrong combinations** |

Success answers *"does it work?"*; the error metric answers *"can we trust it?"*. Thresholds are
agreed before results are seen.

---

## 6. Design policy (why minimal designs are legitimate)

The domain permits far less structure than people assume (verified against the domain classes):

| Invariant | Consequence |
|---|---|
| `ExperimentalVariable`: ≥ 1 level | a single-level variable is legal |
| `Condition`: ≥ 1 variable level, one per distinct variable | a one-variable, one-level condition is legal |
| `ExperimentalGroup.sampleSize >= 1` (code defers to the PM) | a group of n = 1..N is legal |

So exploratory sampling is representable without inventing science:

```
"20 samples measured along the way"
   variable:   "sampling round"    levels: 2026-01, 2026-02   ← observed, not invented
   conditions: 2026-01 → group "January run"  (n = 12)
               2026-02 → group "February run" (n = 8)
   comment:    "exploratory sampling; design provisional"
```

**Analysis-preserving invariants** (the line that must not be crossed for low friction):

1. every sample belongs to exactly one group
2. groups share the same variable/condition structure (comparable)
3. replicates count independent biological samples, not technical repeats

Species/specimen/analyte, condition, analysis method and batch are still captured, so metadata
substance is preserved even when design sophistication is thin. What is *not* preserved is a
first-class "design maturity" marker — that needs a schema/governance decision and is out of scope
for the prototype (a `designComment` note is used instead).

---

## 7. Failure model and observability

| Failure | Behaviour | Channel to the user |
|---|---|---|
| Proxy 5xx / timeout | retry once, then degrade | "AI assistant temporarily unavailable (HTTP 502 …)" + manual path |
| Extraction unusable | empty draft | visible report + manual path |
| Term not resolvable | excluded from the draft | per-term report naming the term + alternatives |
| Domain invariant violated | not applied to the design | per-item report |
| Composition ≠ total | not satisfied | remaining/exceeded + reconciliation buttons |
| Existing experiment invalidates draft | sheet not downloadable | issue list against the real experiment |

Concurrency:

```
interactiveExecutor (single thread)  ← description drafting + free-text extraction
analysisExecutor    (single thread)  ← the four batches
in-flight guard                      ← controls disabled while a request runs
@Push (long polling)                 ← progress and results streamed to the browser
```

---

## 8. Security

- **No assistant identity.** Reads happen on the UI request thread in the user's `SecurityContext`;
  `@PreAuthorize` / `@PostAuthorize` / ACL enforcement is unchanged.
- **The one write is explicit and boring.** `DemoExperimentCreationService.create(...)` calls the
  same ACL-guarded services the regular UI calls, in the same order
  (`AddExperimentToProjectService.addExperimentToProject` → `addVariableToExperiment` →
  `createExperimentalGroup`), on the Vaadin request thread. Nothing is written without a click;
  failures and partial states are reported per step.
- **Read-only otherwise.** The drafted sheet is downloaded, never persisted.
- **Data minimisation.** Only the description, failure messages and selected context are sent.
- **Dev/demo only.** Route `@Profile({"development","demo"})`; `qbic.assistant.enabled=false` by
  default; never registered in production.

---

## 9. Profiles and configuration

| Integration | `development` | `demo` | `production` |
|---|---|---|---|
| TIB terminology | mock | **real** | real |
| ROR organisations | mock | **real** | real |
| OpenBIS | mock | mock | real |
| Assistant playground | available | available | **not registered** |

```properties
qbic.assistant.enabled=${QBIC_ASSISTANT_ENABLED:false}
qbic.assistant.base-url=${QBIC_ASSISTANT_BASE_URL:https://llm.mlcloud.uni-tuebingen.de/v1}
qbic.assistant.model=${QBIC_ASSISTANT_MODEL:deepseek-ai/DeepSeek-V4.1-Flash}
qbic.assistant.api-key=${QBIC_ASSISTANT_API_KEY:}
qbic.assistant.timeout-ms=${QBIC_ASSISTANT_TIMEOUT_MS:30000}
```

Backend: on-premise university LiteLLM proxy (OpenAI-compatible). No Spring AI — a plain
`java.net.http` adapter avoids a reactive stack in the servlet/Vaadin app; Spring AI remains a
Stage-3 option.

---

## 10. Safety invariants

1. No model-proposed ontology term reaches the UI unless it is a real concept with a stable IRI.
2. Draft terms must resolve against the slot's authoritative source; unresolved terms are reported
   and excluded — never used.
3. The drafted design must satisfy the domain invariants before it can be applied or created.
3b. Sample rows are derived from the matrix (groups × replicates × analytes); biological replicates
   are not confused with sample rows. **Analysis is per analyte and is never guessed** — a missing
   analysis is reported and blocks a clean download.
4. `COMPOSITIONS` (constrained interview) is satisfied only when the sum equals the stated total.
5. Every dropped or rejected value produces a user-visible message.
6. No `@PreAuthorize`-guarded data is read outside the user's security context.
7. The only write is human-confirmed and goes through the existing ACL-guarded services.
8. A failing/unreachable model degrades; it never blocks or crashes a flow.
9. A generated sheet is validated before it is offered (structurally pre-creation, against the real
   experiment post-creation).

---

## 11. Known limitations

- **Condition assignment is a heuristic** when the number of composition elements ≠ number of
  groups; surfaced as a note, not silently resolved.
- **Pre-creation sheet is plain-format** (no experiment dropdowns) until the experiment exists.
- **Interactive extraction is single-threaded**; a very slow model serialises attempts.
- **Specimen bridging** is gated against the TIB whitelist (UBERON not whitelisted).
- **`OntologyTerm.oboId()`** throws for colon-form CURIEs; worked around in new code, domain bug
  untouched.
- **Batches are sequential** (one model call per corpus case).
- **No design-maturity marker**; provisional designs are only noted in a comment string.
- **Deferred:** interactive clarifying-question dialogue and the "why does a condition exist?"
  pedagogy affordance.

---

## 12. Path to production

Transfers unchanged: ports/adapters, the gates and verification, corpora/scorers (as regression
tests), the design policy and the minimal-design pattern.

The real integration point is not the playground but the **experiment-setup and upload error views**
(`ExperimentCreationDialog`, `SampleUploadDisplay`, measurement upload), where the assistant appears
next to the current form/error list and offers reviewable values — subject to the governance chain
(requirement → Feature → Story) before implementation.
