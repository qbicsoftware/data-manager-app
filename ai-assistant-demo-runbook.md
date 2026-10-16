# AI Assistant Prototype — Demo Runbook

> Audience: whoever drives the stakeholder demo. Companion to
> [`ai-assistant-architecture.md`](ai-assistant-architecture.md) (how it works) and
> [`ai-assistant-prototype-plan.md`](ai-assistant-prototype-plan.md) (what we are evaluating).

---

## 1. What the demo proves

The pain point is not filling a sheet — it is **creating the experimental design first**. So the
demo shows one paragraph becoming a reviewed design *and* a registration sheet:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  BEFORE    project → create experiment → declare species/specimen/analyte →  │
│            define variables → conditions → groups → then finally a sheet     │
│                                                                              │
│  NOW       project + one paragraph → reviewed design + registration sheet    │
│                                                                              │
│  LLM understands and proposes · System verifies and enforces · Human decides │
└──────────────────────────────────────────────────────────────────────────────┘
```

Every act below shows one of the three roles, including the guardrails.

---

## 2. Pre-flight (before the audience arrives)

```
┌────────────┐   ┌─────────────────────┐   ┌──────────────────────────┐
│ MariaDB +  │──►│ Maven install       │──►│ spring-boot:run          │
│ Artemis +  │   │ (REQUIRED — see     │   │ SPRING_PROFILES_ACTIVE=  │
│ SMTP up    │   │  below)             │   │ demo                     │
└────────────┘   └─────────────────────┘   └────────────┬─────────────┘
                                                        ▼
                                          http://localhost:8080/dev/assistant-playground
```

```bash
# 1. External systems up: database, Artemis broker, SMTP (see README)

# 2. REQUIRED: refresh sibling module jars.
#    datamanager-app loads project-management(-infrastructure) from ~/.m2, so prompts and
#    adapters changed in those modules are otherwise NOT picked up.
./mvnw install -DskipTests -Dsigstore.skip=true

# 3. Assistant credentials + profile
export QBIC_ASSISTANT_API_KEY=$(security find-generic-password -ws 'litellm')
export SPRING_PROFILES_ACTIVE=demo     # real TIB + ROR, mocked OpenBIS, playground enabled

# 4. Run
./mvnw spring-boot:run -pl datamanager-app -Pdevelopment
```

| Pre-flight check | Expected |
|---|---|
| Playground opens, no "Assistant bean … not available" in the log | extractors wired |
| Project dropdown lists your projects | ACL-filtered |
| A test draft returns within a few seconds | proxy reachable (else use the fallback script) |

Prepare a **project whose experiment does not yet exist** — that is the point of the demo.

---

## 3. Demo flow (run of show, ~12 minutes)

```
 ACT 1 ──────► ACT 2 ──────► ACT 3 ──────► ACT 4 ──────► ACT 5 ──► ACT 6
 describe +    review the     create the    guardrails     evidence  close
 draft (3)     design (3)     experiment(3) (2)            (2)
```

### Act 1 — Describe and draft (3 min)

The page has three tabs: **Study setup** (this act), **Interview** and **Evaluation** (developer
surfaces, mentioned only in passing).

1. Select **Project**.
2. In **"1. Describe your study"** type (example):

   > *"20 samples from a mouse liver study: 10 treated, 10 control, DNA extracted; the readout is proteomics"*

3. Click **Draft experiment & samples** — one model call, a few seconds.

> Say: *"The researcher writes what they know. There is no wizard and no hypothesis required."*

### Act 2 — Review the design (3 min)

Walk the review panel:

- **Experiment name**
- **Species / specimen / analyte** chips with their CURIEs — *terms were resolved against the species
  database and TIB, not invented*
- **Variables** (e.g. `Treatment: treated, control`)
- **Groups** with biological replicate counts
- **Samples**: combination table with counts and the total
- **Reports** (if any) and the **design note** (`ℹ️`), e.g. *"exploratory sampling; design provisional"*

Point out the two verdict states:

```
✅ Structurally complete — re-validates against the real experiment after creation.
⚠️ N structural issue(s):  …            → download disabled until resolved
```

**Show the matrix and the analysis rule.** The Samples card is a projection, not a list:

```
2 groups × 10 biological replicates × 2 analyte samples = 40 sample rows
control        replicate 1..10   mRNA     (RNA-SEQ)
drought stress replicate 1..10   peptide  (⚠️ choose an analysis — available: …)
```

The same plant's two rows share its biological-replicate number. Point out that **peptide's
analysis is deliberately unset**: the assistant does not know which proteomics methods this
instance offers, so it asks instead of guessing (an earlier silent default to `16S` was a real
defect).

> Say: *"Twenty plants, two measurements each — forty samples. The sheet is a view of the matrix:
> groups × replicates × analytes, and every analyte carries its own analysis. Where the model
> doesn't know the analysis, it leaves it empty and asks."*

**Then correct it — twice, to show both paths.**

*Manual correction (fixes a value):* click ✎ on a composition row, change the count or pick a
different term from the resolver-backed picker, confirm. The reports and the badge re-verify
immediately; Download/Create stay disabled while a re-verification is in flight.

*Re-prompt (fixes the intent):* in the **Refine the draft** card, type what the model misread, e.g.

> *"10 mice and 10 rats per group, liver, DNA"* → **[ Refine draft ]**

Show that the applied request is listed, that only the requested change moved, and that manual
edits survived the refinement (the current draft is the base of the re-prompt).

> Say: *"Two ways to make it right: change a value yourself, or tell it once what you meant and let
> it redo the draft. And if the model is unavailable, the draft you already have is never lost."*

> Say: *"Everything here is verifiable: the terms exist in our vocabularies, the design satisfies
> the same invariants the manual experiment setup enforces, and the sheet is checked before it is
> offered."*

### Act 3 — Create the experiment (3 min, the payoff)

1. Click **Create experiment in \<project\>** — **explicitly labelled as writing real data**.
2. On success the context switches to the created experiment.
3. The sheet is **regenerated through the production template** and **re-validated against the real
   experiment**:

```
✅ Validates clean against real experiment '<name>'
[ Download sample registration sheet ]
```

4. Download it, open the XLSX, point out the production "Sample Metadata" layout and the
   experiment-derived dropdowns.

> Say: *"One paragraph replaced the whole experiment-setup wizard. The creation happened through the
> same permission-checked services the normal UI uses — the assistant was only the trigger, and a
> human confirmed it."*

### Act 4 — Guardrails (2 min)

Two quick negatives, both safe to show live:

**4a. A term that does not exist / cannot be resolved** (e.g. type a nonsense species). Expected:
a per-term report naming the term and listing alternatives or stating it could not be resolved —
the value is excluded from the design, never silently dropped.

**4b. An impossible design** (e.g. a group with 0 replicates, or a condition referencing an
undefined variable). Expected: a per-item report from the domain grader; the draft is not applied.

> Say: *"The model proposes; the domain model decides. Anything that violates the rules comes back
> as a named, explainable report."*

### Act 5 — Evidence batches (2 min, usually pre-run)

| Button | Reports | Read as |
|---|---|---|
| `Run evaluation batch` | field ID, category, valid auto-fix, hallucination-free | fix quality vs. baseline |
| `Run synonym bridge batch` | existence validity, concept correctness, gate resolvability, **unintended concept** | is language→vocabulary mapping safe? |
| `Run slot extraction batch` | slot-fill rate, **false fills** | does it understand a sentence strictly? |
| `Run composition extraction batch` | combination rate, **wrong combinations** | does it get permutations right? |

> Say: *"A demo example proves nothing. These run fixed corpora including traps, and report a
> success number and an error number. The error numbers decide whether this is trustworthy, and the
> thresholds were agreed before we saw the results."*

### Act 6 — Close (2 min)

- The design is now an **output** of the assistant, and the sheet falls out of it.
- LLM proposes, system verifies (ontology existence, domain invariants, arithmetic), human decides.
- Read-only except an explicitly confirmed creation through existing permission-checked services.
- Next: measure (batches), review, then the governance chain before it lands in the real setup and
  upload dialogs.

---

## 4. Figure: what happens behind one paragraph

```
 "20 samples, mouse liver, 10 treated / 10 control, DNA, proteomics"
                              │
                              ▼
              ┌───────────────────────────────┐
              │ ExperimentDesignExtractor      │  LLM: understand + propose
              │ DOMAIN PRIMER / POLICY / JSON  │  extract-as-spoken, never filter
              └──────────────┬────────────────┘
                             ▼
              ┌───────────────────────────────┐
              │ DesignVerifier                 │  deterministic
              │  1 ontology existence (IRI)    │  never throws, always reports
              │  2 domain invariants (grader)  │
              │  3 composition arithmetic      │
              └──────────────┬────────────────┘
                             │
              ┌──────────────┴───────────────┐
              ▼                              ▼
      reports + note                 verified design + composition
              │                              │
              └──────────────┬───────────────┘
                             ▼
                   review UI (human decides)
                             │
        ┌────────────────────┴─────────────────────┐
        ▼                                          ▼
 Download draft sheet                     Create experiment in project
 (plain format, structural check)         (ACL-guarded write, request thread)
                                                     │
                                                     ▼
                                          production template + re-validation
                                                     │
                                                     ▼
                                          Download registration sheet
```

---

## 5. Presenter cheat sheet

| If the audience asks | Answer |
|---|---|
| "Does it invent species or ontology terms?" | No. Every term must exist with a stable IRI in the species DB or TIB; otherwise it is reported and excluded. |
| "Does it invent the experiment?" | It drafts it; the domain invariants decide whether it is valid; you review it; only then is it created. |
| "What if it misread my study?" | Two paths: edit any value directly (variables, groups, sample rows, terms) or tell it once what you meant and let it refine the draft — your manual edits are preserved as the base. A failed refinement leaves the draft untouched. |
| "Why 40 rows for 20 plants?" | The matrix: 2 groups × 10 biological replicates × 2 analytes = 40 sample rows. The two rows of one plant share its replicate number; the Excel sheet is simply that matrix projected into the legacy registration format. |
| "Why is the peptide analysis empty?" | Because the user did not know which methods exist. The assistant shows the available `AnalysisMethod` options and refuses to guess; an earlier silent default to 16S was a defect. |
| "Does it write to the database?" | Only when you press *Create experiment in …*, through the same permission-checked services as the normal setup. |
| "Whose permissions does it use?" | Yours. It has no identity. |
| "What about researchers who sample on the go?" | Supported: the smallest defensible design — e.g. a *sampling round* variable with the observed rounds as levels. No hypothesis is fabricated, and sampling round is a real analysis asset. |
| "Does that sacrifice FAIR?" | No: species, specimen, analyte, condition, analysis method and batch are still captured; only design sophistication shrinks. Provisional designs carry an explicit note. |
| "What if the model is down?" | It degrades with one clear notice; the manual path stays usable. |

**Do not claim:** production readiness, effort saved (not measured), DPA/legal clarity, or final
batch numbers (pending the live evidence run and the thresholds decision).

---

## 6. Fallback scripts

**Proxy unavailable (502 / timeout).** Keep the demo moving:

```
⚠️ The AI assistant is temporarily unavailable (HTTP 502 from the language model proxy).
```

> Say: *"This is what should happen when the model is unavailable: a clear statement and a usable
> path — not a spinner and not a wrong answer."* Then show the manual interview/batches instead.

**A draft with unexpected reports.** Treat it as Act 4: read the report aloud, pick a valid value.

**Creation denied (no WRITE permission).** Expected behaviour — show the clean failure message and
note that permissions are enforced by the service layer, not the assistant.

**Batches too slow.** Show a previous run's report; each corpus case is one sequential model call.

---

## 7. Pre-demo checklist

- [ ] `./mvnw install -DskipTests -Dsigstore.skip=true` after the last pull
- [ ] `SPRING_PROFILES_ACTIVE=demo` and `QBIC_ASSISTANT_API_KEY` exported in the run shell
- [ ] A project prepared **without** the experiment to be drafted
- [ ] One nonsense-species input and one impossible-design input ready for Act 4
- [ ] A downloaded example XLSX open to show the format
- [ ] Batch reports from a previous run available as a fallback
- [ ] Know the project's declared design (for the constrained-interview fallback)
