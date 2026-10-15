# AI-Assisted Project Management — Prototype & Evaluation Plan

> **Status:** working draft, local/untracked, not yet governed. Before any implementation:
> requirement + Feature + Story per AGENTS.md §0/§12 (human approval required).
> **Scope:** evaluation strategy for Spring AI-based assistance in the Data Manager App.
> **Last updated:** 2026-10-08 (draft)

---

## 1. Problem statement

The project-creation journey is a chain of five disjoint, form-heavy interactions:

1. **Create project** — `views/projects/create/AddProjectDialog.java` (4-step stepper:
   Project Design → Funding → Collaborators → Experimental Information)
2. **Describe experiments** — `views/projects/project/experiments/` (variables, conditions,
   groups, all manual)
3. **Register samples** — XLSX template download → offline edit → upload → validate → fix →
   re-upload loop (`views/projects/project/samples/registration/batch/`,
   `SampleValidationService`)
4. **Register measurements** — same template/upload/validate loop, NGS + proteomics variants
   (`views/projects/project/measurements/registration/MeasurementUpload.java`)
5. **Submit raw data** — `views/projects/project/rawdata/RawDataMain.java`

Friction points: no guidance inside steps, unforgiving batch validation with machine-oriented
error messages (`ValidationResult` string lists), ontology/CURIE burden on users, and design
errors made in step 2 propagating into every downstream sample row.

## 2. Strategy (one sentence)

**Prove value with an offline-capable prototype on self-hosted models (zero legal surface),
then a small internal live beta, and only invest in a DPA with a European provider once
evidence shows users actually want the feature.**

| Stage | What | Data/legal surface | Gate |
|---|---|---|---|
| **0. Governance prep** | Requirement + Feature + Story drafts for human approval (AGENTS.md §0/§12). New capability → traceability chain mandatory. | None | Human approves scope |
| **1. Prototype + evaluation** | Dev-only playground UI in the app with built-in scored evaluation harness. Two AI tasks: (a) metadata-error explanation/auto-fix, (b) experimental-design drafting. Self-hosted models (Ollama: Mistral Small, Qwen, …), temp 0, structured JSON output. | Internal only; seed corpus from test instance, anonymized | Pre-agreed kill/promote metrics (§6) |
| **2. Live beta** | Feature-flagged assistant in real dialogs (`SampleUploadDisplay`, `MeasurementUpload`, `ExperimentalInformationLayout`). Still self-hosted. DPO informed. **Metrics wired from day one** (usage, error-resolution rate, "faster than blank"). | Internal processing, no DPA | Evidence: usage + measured benefit |
| **3. DPA + hosted provider** | Execute DPA (Mistral: self-serve addendum incorporated into La Plateforme agreement, or negotiated via `sales@mistral.ai` + university DPO/legal; Aleph Alpha as sovereign fallback). Provider swap = config change via port abstraction. | External processor under DPA; expect weeks–months, hence last | Production rollout; re-evaluate Tier 3 (agentic tool-calling) |

## 3. The two prototype AI tasks

### Task 1 — metadata-error explanation & auto-fix (XLSX sample + measurement registration)

- **Input (as in production):** real `ValidationResult.failures()` strings + offending row
  payload + bounded context block (template column schema, valid value domains: project
  conditions, sample ids, instrument lists, ontology hints via `TerminologyService`).
- **Output contract (structured JSON):**
  `{ field, category, explanation, suggested_fix, confidence, needs_user_input }`
- **Ground truth by construction:** mutate known-valid sheets (see §5), run through the real
  validators, score the model output against the known mutation.
- Failure space is enumerable from code:
  - `SampleValidation`: missing sample name/batch/condition/species/specimen/analyte,
    missing CURIE, unknown species/specimen/analyte/condition/analysis method/experiment/
    confounding variable/sample id (~15 message shapes)
  - `measurement/validation/*` (NGS/PxP/IP): property-enum-driven messages
    (`NGS_PROPERTY`: qbic sample id, organisation id, instrument, sequencing read type,
    library kit, flow cell, index i7/i5, pool group, …), pooling rule violations
  - `files/parsing/*`: structural errors (header count, unknown headers, empty cells)

### Task 2 — experimental-design drafting (natural language → design draft)

- **Input:** realistic sloppy scientist phrasing ("colorectal cancer, tumor + matched
  healthy, anti-PD1 vs control, 3 time points, 4 patients/arm, proteomics"), plus
  deliberately ambiguous variants to test clarification behavior.
- **Output contract:** JSON mirroring the domain: variables (name, levels, unit),
  conditions (linear combinations of variable levels), groups (name, condition, replicates),
  species/specimens/analytes (CURIE-anchored), `ambiguities[]`, `needs_clarification`.
- **Executable grader:** the domain classes ARE the validator —
  `ExperimentalVariable.create` (unit consistency), `Condition.create` (distinct declared
  variables), `Experiment.addVariableToDesign` / `addExperimentalGroup` (replicates > 0,
  group/variable guards). Structural validity is objective yes/no.
- **Ontology check:** suggested CURIEs must resolve via real `TerminologyService` /
  `SpeciesLookupService`.
- **Bonus signal:** design-quality linting (single-replicate groups, missing control,
  partial factorial without comment) — scored separately, not a gate.

### Pipeline realism bonus

Chain both tasks: use Task 2's drafted design (conditions, variables) as the context block
for Task 1's sample-metadata correction. Validates that design drafts are consumable
downstream — pre-validates the whole AI-assisted wizard concept.

### Task 4 (added 2026-10-03, stakeholder-demo driven): Guided sheet drafting via interview

**Motivation:** Task 1 live tests showed advice-only adds little value — the app already
lists the errors. Real value = resolution and prevention. Stakeholder-demo priority.

**Concept:** the assistant interviews the user (sample count, species, facility,
organisation, …), resolves each answer against real vocabularies, and generates a complete,
pre-filled sample registration sheet for download + human review. The assistant never
writes to the DB — review-first via the normal upload flow.

**Prototype scope (deliberately basic):**

- Deterministic interview state machine (slot-filling: which slots filled, resolver
  candidates per slot, what's missing) — NOT free-form LLM chat state. The LLM only does
  NLU/phrasing; the state machine keeps it demo-proof.
- Resolver-backed slots: species/specimen/analyte via `TerminologyService` /
  `SpeciesLookupService`, organisation via `OrganisationLookupService` (ROR), condition
  matched against the selected experiment's real groups (reuses the playground's project
  dropdown), facility from the controlled list.
- Ambiguity = follow-up question with clickable candidates, never a guess.
- Generation via the production `SampleInformationXLSXProvider` → headers/formats correct
  by construction.
- Self-checking output: generated sheet is run through the same validators before download;
  UI badge "✓ validates clean against project X / experiment Y".
- Start: sample registration sheet only, single experiment context, English.

**Shared backbone:** the same resolver layer also powers Task 1's actionable fix
suggestions (correct ROR URL for a wrong organisation value, closest condition for an
unknown condition, typo detection on sample codes via edit distance) — measure **resolver
coverage** (% of mutated cases fixed deterministically, proven by re-validation) as a
hard, LLM-independent eval metric.

**Iteration note — where the LLM actually kicks in (added 2026-10-03, after live testing):**

Observation from live use: the interview state machine does the heavy lifting and the LLM
was invisible (no `AssistantPort` usage in the `draft/` package). Honest division of labor:

- LLM = **translation layer at the human-language boundary**:
  1. free text → canonical value (synonym bridge: "human" → NCBITaxon:9606; "about 20" → 20)
  2. disambiguation/ranking of resolver candidates using conversation context
  3. compound free-text answers filling multiple slots; corrections ("actually 24 samples")
  4. natural-language explanation (marginal value — Task 1 only)
- Deterministic = **correctness core**: slot transitions, required-slot completeness,
  validation gate, CURIE-existence checks, sheet assembly. Never delegated to the LLM.

**Design decision — composition model for species/specimen/analyte (added 2026-10-03, live gap found):**

The product declares species, specimens and analytes as **lists on the experiment**
(`Experiment.species`, `Experiment.specimens`, `Experiment.analytes`) so the legacy metadata
sheets can offer dropdowns of valid selections. That legacy flow must not be broken.

Therefore the interview must not ask for species, specimen and analyte as independent
single-valued slots. It asks for the **permutation (combination) of values already declared in
the experimental design, with a sample count per combination**, e.g.:

> 20 samples total — 5 with mouse, liver, DNA; 5 with human, blood, RNA; …

Consequences:

- Allowed values are constrained to the selected experiment's declared lists; the local-existence
  gate for bridged terms is strengthened to "must be in the experiment's declared set".
- The sample count is the **sum** of the combination counts (consistency-checked), not an
  independent slot.
- The assistant remains **read-only**: it consumes the declared design; if a user wants a value
  that is not declared, they must extend the experiment design via the legacy flow (the assistant
  may point this out and offer the declared alternatives).
- The proposal is then expressed as a composition table in the UI (species | specimen | analyte |
  count, add/remove, constrained dropdowns) plus a free-text import that fills rows from a sentence.

Next iteration to be dispatched after the synonym bridge (Task 4b) lands:
**free-text multi-slot parsing + corrections**, so the LLM's contribution is visible in the
stakeholder demo (form wizard → assistant). New metric: **slot-fill accuracy from free
text** scored against a small authored corpus of messy sentences, in the same harness style
as Task 1.

Synonym bridge rationale (Task 4b, verified empirically): the local NCBITaxon dump stores
labels only (`ontology_classes` has no synonym column); TIB does not host NCBITaxon at all
(0 hits) and OLS4 does not rank `human` → NCBITaxon:9606 (not in top 200). Both external
services cannot bridge the gap → LLM with a **hard local-existence gate** is the only
working route. Product-level fix (synonym column + ingest) noted as a recommendation, but
unlikely in the near term.

**Governance note:** distinct capability from Task 1 (drafting vs. correction) — needs its
own requirement + Feature + Story pair at Stage 0.

## Demo pivot (2026-10-04): design as output, not prerequisite

**Problem with the previous demo:** it was convoluted, the benefit was not visible, and it did not
show a speed-up. The single biggest pain point is creating the experimental design first (experiment
name, declared species/specimen/analyte, variables, conditions, groups) before any sample sheet can
exist. Requiring a fully declared experiment made the assistant look like a gate, not a helper.

**New demo definition (primary playground section):**

> The user selects a project and writes a short free-text description of the study. The assistant
drafts the **experiment design AND the samples** in one step and presents a sample registration
sheet for immediate review.

Consequences:

- The declared design is no longer an input constraint for the drafting flow; it is a *proposal*
  the human reviews (the assistant still never writes — review-first).
- Validation split:
  - ontology terms must be real concepts with a stable IRI (species -> local species database;
    specimen/analyte -> TIB) — existence, not declaredness;
  - the drafted design is graded by the domain classes (variables/conditions/groups invariants);
  - the sample composition must be arithmetically consistent;
  - the generated sheet is validated structurally and against the drafted design.
- The existing constrained interview (declaredness gate, reconciliation) remains as a secondary
  surface and as the regression suite for those invariants.
- One LLM call for the whole draft, so the demo is fast.

**Open decision for the PO:** whether the demo should also offer a one-click "create this
experiment in the project" action (a write through the existing ACL-guarded application service,
human-confirmed). Until decided, the flow is review-only (design + sheet download).

**Decision (2026-10-04): YES — the one-click creation is in scope.** After the user reviews the
draft, an explicit "Create experiment in <project>" action calls the existing ACL-guarded
application services (experiment + variables/conditions/groups) on the Vaadin request thread in the
user's `SecurityContext`, then re-validates the drafted sheet against the newly created experiment.
The assistant never writes automatically: the human confirms, the service layer enforces WRITE
permissions. The review-only path stays available for anyone who does not click it.

---

**Model decision (2026-10-04): the sample sheet is a PROJECTION of a matrix.**

Confirmed by the PO after a live run produced a wrong sheet (analytes distributed across samples
instead of within each biological replicate; every row repeated replicate "1"; one global analysis
applied to all rows):

- **Model:** groups (condition) × biological replicates (1..n) × analytes (each analyte with its
own species/specimen, per-replicate count, and **its own analysis**).
- **Rows:** one per group × replicate × analyte sample; the biological-replicate number is shared by
the analyte rows of the same replicate (one plant → an mRNA row and a peptide row with the same
replicate number).
- **Analysis is per analyte and is never guessed.** Options come from the app's real
  `AnalysisMethod` values; when the user is unsure the analysis stays unset and is reported as
  "choose an analysis for X" (a silent default to 16S was a real defect).
- **Plants ≠ rows.** "Two groups of ten plants, two analytes each" is 20 biological replicates and
  40 sample rows; the arithmetic is shown, and a stated total must not force a wrong row count.
- **Legacy preserved:** the Excel registration sheet remains the current evaluation/legacy process
  and is kept as a download — it is now generated *from* the matrix. The matrix is expected to
  replace the Excel list in the future, but that is not this prototype's call.

---

## 4. Playground design (the "small testing feature with a simple UI")

Dev-profile-only Vaadin route, hidden, never shipped to production:

```
/dev/assistant-playground   (development profile only)
├── [Drop XLSX] → runs real validator → shows raw errors
│                 next to AI explanation/fix                 (interactive demo)
├── [Describe experiment…] text field → renders drafted design
│                 as variables / conditions / groups tables
└── [Run evaluation batch] → runs all seeded cases (task 1 + 2)
                             → score table per model/prompt    (decision instrument)
```

- UI builds intuition and supports internal demos; the **batch scorer makes the
  promote/kill decision** — a UI without batch scoring is judging by vibes.
- Living inside the app gives realistic context assembly for free (real services, real
  templates, fixture project).
- Follows existing convention: `development` profile mocks external systems (OpenBIS/TIB);
  the playground and a stub/echo `ChatModel` join that mechanism.

### How to read the playground (demo narrative + evidence package)

The playground has two kinds of surfaces: **interactive features** (build intuition, support
stakeholder demos) and **batch buttons** (produce the scored numbers that inform the
promote/kill decision). A UI demo shows one lucky case; the batch shows behaviour across many
fixed cases — including adversarial ones.

Run it with real integrations and the live proxy:

```bash
export QBIC_ASSISTANT_API_KEY=$(security find-generic-password -ws 'litellm')
export SPRING_PROFILES_ACTIVE=demo   # real TIB/ROR, mocked OpenBIS, playground enabled
./mvnw spring-boot:run -pl datamanager-app -Pdevelopment
# → http://localhost:8080/dev/assistant-playground
```

**Feature 1 — validation advice (Task 1).** Drop an XLSX → real validators run against the
selected project/experiment → each failure gets an explanation and a suggested fix. Identical
failure patterns are deduplicated (one LLM call per pattern); clean rows are skipped; a run-scoped
circuit breaker degrades to the rule-based assistant if the proxy fails, so nothing waits out
repeated timeouts.

| Button | Reports | Watch for |
|---|---|---|
| **Run evaluation batch** | field-identification rate, category rate, valid auto-fix rate, hallucination-free rate | valid auto-fix rate vs. the rule-based baseline (20%); any confident-but-wrong fix |

**Feature 2 — synonym bridge (Task 4b).** Free text → canonical ontology term, because the local
dump stores labels only (verified: TIB hosts no NCBITaxon; OLS4 cannot rank `human` →
NCBITaxon:9606). Order: local label search first → LLM suggests a candidate CURIE → **hard local
gate** → bridged candidates shown with provenance ("matched via synonym bridge, please confirm").

| Button | Reports | Watch for |
|---|---|---|
| **Run synonym bridge batch** | existence validity (real concept, stable IRI), concept-correctness rate, **gate resolvability** (did a correct concept survive validation?), **unintended-concept rate** | the unintended-concept rate: a real (valid-IRI) concept that is not what the input meant. Existence is machine-verifiable; meaning is the human's call, so every bridged candidate is confirmed. A correct concept dropped by the gate is a fixable infrastructure gap (e.g. an ontology not reachable via TIB), reported separately — never a model error. |

Corpus categories: English synonyms (`human`, `man`), multilingual (`Mensch`), typo (`mous`),
paraphrase (`whole blood`), domain breadth (`banana` → *Musa acuminata*, `tomato`), and traps.

**Trap semantics (corrected 2026-10-03):** a trap must be an input that *cannot belong to the
slot's ontology type* — not merely an odd word. `banana` is a valid taxon (NCBITaxon:4641) and
`serum` is a valid specimen term (BTO:0001239); both were mislabelled traps and inflated the
error metric with *correct* behaviour. Valid traps: species → `water`, `blood`, `DNA`;
specimen → `human`, `July`; analyte → `liver`, `human`.

**Feature 3 — slot extraction / interview (Task 4).** A whole sentence fills several interview
slots at once ("we have about 20 human blood samples" → count, species, specimen), corrections
work ("actually make that 24"), and the machine then asks only what is missing. Every extracted
value is verified downstream (ontology → bridge + gate, numbers → bounds check) and shown in an
"understood: … correct?" summary before acceptance. The LLM is the language boundary; the state
machine and resolvers remain the correctness core.

| Button | Reports | Watch for |
|---|---|---|
| **Run slot extraction batch** | slot-fill accuracy, false fills | false fills: a slot filled with a wrong value (e.g. "six patients per group" becoming a sample count) |

Corpus categories: multi-slot, single-slot, German (`Menschenblut`), correction, and traps
("I don't know yet", "six patients per group" → must extract nothing).

**Reading the numbers.** Success/accuracy rates answer "does it work?"; the trust rates
(unintended concept, false fill) answer "can we trust it?". A high success rate with a
non-trivial trust-rate error is disqualifying for a user-facing flow. Thresholds
(promote/kill) are agreed before results are seen — see the criteria section.

## 5. Seed corpus from the test instance

**Verified mechanics:** rejected uploads are NOT persisted (Vaadin uploads are in-memory;
validation services don't log failures) — no archive of real user errors exists. But valid
registered metadata IS exportable: `SampleInformationXLSXProvider`
(`views/projects/project/samples/download/`) re-renders registered samples as XLSX; measurement
templates come from the existing template generation.

### Checklist

- [ ] Export valid sample sheets (NGS + proteomics flows) via `SampleInformationXLSXProvider`
- [ ] Export/generate valid measurement sheets per template type (NGS, PxP,
      immunopeptidomics per `docs/stakeholder-artifacts/measurement-metadata/`)
- [ ] Inventory real project archetypes on the instance → source for Task 2 gold scenarios
- [ ] Anonymize: strip collaborator names/emails, project titles, offer references → corpus
      may circulate on dev machines without becoming a data-protection topic
- [ ] Sanity-check instance content (developer junk vs. realistic data); if junk-dominated,
      author seeds from known production archetypes instead
- [ ] Counter selection bias: ensure both NGS and proteomics/IP coverage, not just the
      instance's dominant flow
- [ ] Target: ~15–25 gold design scenarios (archetypes × ambiguity variants),
      ~100–200 mutated error cases across failure categories
- [ ] Optional: ask 1–2 power users to manually break sheets "the way users actually do"
      (wrong columns, mixed template versions) — covers error space mutations won't invent

## 6. Metrics & kill/promote criteria (agree BEFORE looking at results)

### Task 1 (metadata correction)

- Automated: field-identification accuracy; fix-category accuracy; **auto-fix validity**
  (suggested fix passes the real validator — gold metric); hallucination guard (no invented
  sample ids/conditions).
- Human (~30 cases, 2 raters): clarity vs. raw error (1–5); actionable by non-power-user
  (y/n); wrong-but-confident (disqualifier).
- **Promote if:** ≥85% field ID, ≥60% fully-valid auto-fix, clarity ≥4/5 (vs. ~2.5 raw
  baseline), <5% confident-wrong.

### Task 2 (design drafting)

- Automated: structural validity (domain constructors pass); CURIE validity; clarification
  triggered on seeded ambiguities.
- Human: expert verdict "correcting the draft is faster than starting blank" (y/n + time
  estimate).
- **Promote if:** ≥80% structural validity, ≥70% CURIE validity, clarification on ≥80% of
  seeded ambiguities, "faster than blank" on ≥70% of drafts.

### Stage 2 beta metrics (decide now, wire then)

Usage rate; error-resolution rate per upload with vs. without assistant; retention across
sessions; "faster than blank" survey. Example evidence bar for Stage 3: "68% of upload
errors resolved on first retry with assistant vs. 31% without."

## 7. Architecture commitments (valid regardless of AI outcome)

- **`AssistantPort`** interface in `project-management/application/` (mirrors the
  `TerminologySelect` port pattern); provider adapters in
  `project-management-infrastructure/`.
- **Spring AI** (`ChatClient`/`ChatModel`, structured output). Compatible: BOM pins Spring
  Boot 4.1.1 / Vaadin 25.2.5; Spring AI 1.1.x supports Boot 4. First-class
  `spring-ai-mistral-ai` integration exists for Stage 3.
- **BOM discipline:** `spring-ai-bom` import in `datamanager-bom/pom.xml`; no `<version>`
  in child modules; PGP signatures for `sigcheck` profile.
- **Config:** `qbic.assistant.<property>` in `application.properties`; provider selected by
  config (`ollama` now, `mistral` later); stub/echo model for `development` profile.
- **Security:** all assistant-triggered operations go through `@PreAuthorize`-guarded
  application services (never repositories) — ACL cannot be bypassed by the assistant.
- **Exceptions:** wrap model/IO failures at infrastructure boundary per
  `ExceptionHandling.md` (`SERVICE_FAILED`-style); no log-and-rethrow; Vaadin
  `UiExceptionHandler` stays the UI safety net.
- **§12 approvals needed along the way:** requirement changes (dedicated PR), possibly new
  Maven module (or place in existing infrastructure module to avoid this), ADRs for
  provider/data-governance and tool-calling security model.

## 8. European provider landscape (for Stage 3)

- **Mistral AI (FR)** — most mature; Spring AI first-class support; standard DPA
  incorporated into La Plateforme agreement (self-serve) or negotiated
  (`sales@mistral.ai`); subprocessor list + certifications published.
- **Aleph Alpha (DE, Heidelberg)** — PhariaAI sovereign platform, German jurisdiction,
  BSI C5; easiest DPA conversation in DE.
- **EU inference hosts** (IONOS, Scaleway, OVHcloud; Lyceum, Berget AI, GreenPT, Regolo,
  Nebius) — OpenAI-compatible endpoints → Spring AI OpenAI integration with custom base-url.
- **Self-hosting** — Mistral open weights, Teuken-7B (OpenGPT-X/Fraunhofer); possibly
  German academic GPU infrastructure (KISSKI, bwForCluster) — sidesteps commercial DPAs.
- DPA route for a public university: DPO involvement, possibly DPIA (Art. 35), procurement
  check; frame processing purpose narrowly ("AI-assisted metadata validation/explanation").
- Caveat: market shifts monthly — re-verify certifications/pricing at decision time.

## 9. Open decisions (need human/team alignment)

1. Beta Feature scope: both tasks, or Task 1 first?
2. Gold-scenario authoring: ~2–3 days of team domain time — who?
3. Kill/promote thresholds in §6 — finalize before running.
4. Module placement: existing infrastructure module vs. new `assistant/` module (§12
   approval).
5. Test-instance data quality check (§5) — owner + date?

## 10. Immediate next steps

1. Draft requirement + Feature + Story (human review) — Stage 0.
2. Seed-corpus checklist execution (§5).
3. Playground route skeleton + `AssistantPort` + Ollama adapter (dev profile).
4. Mutation engine + batch scorer behind the playground's [Run evaluation batch] button.
5. Run candidate models → score table → promote/kill decision.
