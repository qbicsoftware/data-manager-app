# 0010 — Analysis-method attribution: the sample-level grandfather and its single resolution point

* Status: accepted
* Deciders: Product Owner (explicit approval, project session 2026-11-02)
* Date: 2026-11-02

Related: [FEAT-PROJECT-SUMMARY](https://github.com/qbicsoftware/data-manager-app/issues)
(project landing page status band), `AssayStandingQuery`,
`life.qbic.projectmanagement.application.query.AssayStanding`

## Context and Problem Statement

The project landing page must tell a reader which primary datasets a project holds and how far
along each is. A **primary dataset** is the data of *one experiment* for *one analysis method*.
It is not declared by a human being: it is the consequence of measurements having been done, and
it does not exist before there is data to put in it. Its type is the analysis method, because that
is what a reader selects datasets by when planning an analysis (variant calling, cluster analysis,
…). The omics domain alone (genomics / proteomics / immunopeptidomics) is far too coarse: WES,
RNA-Seq and 16S are all genomics and are not interchangeable.

The analysis method is currently annotated on the **sample** (`sample.analysis_method`, via
`AnalysisMethod`). That is the wrong home: one extract can legitimately yield several analysis
methods, and the method is a property of the measurement, not of the material measured.

**The annotation on the sample is a known mistake, and it cannot simply be moved.**
1. It is populated for existing projects and is the only source that exists for them.
2. Moving it creates a period in which the information sits in **two** places: old rows on the
   sample, new rows on the measurement.
3. Even after the move, the sample-level annotation cannot be deleted without losing the history of
   projects registered before it — so both places will continue to exist.

The question this ADR settles is therefore not *where the annotation belongs* (that is decided: on
the measurement) but **how the application reads it while both places exist**, so that the migration
is one change rather than a change per consumer.

## Decision Drivers

- The landing page and the Datasets page must agree on what a dataset is and how many a project has.
  Two derivations would drift.
- The move to measurement level is scheduled but not imminent; the overview is needed now.
- Every additional call site that resolves the method from the sample becomes an additional
  migration site, and a place where the two sources can silently disagree.
- A measurement can measure samples carrying **different** analysis methods, so the resolution has a
  genuine ambiguity that must be answered explicitly rather than by accident of the SQL.

## Considered Options

1. **Wait for the measurement-level annotation.** No interim feature; the overview stays at omics
   domain grain. Rejected: the domain grain cannot answer the question the page exists to answer,
   and the wait has no defined end.
2. **Resolve the analysis method at each call site.** Each consumer joins to `sample` itself.
   Rejected: it multiplies the migration surface and allows two surfaces to derive different
   answers for the same dataset.
3. **Denormalise the analysis method onto the measurement now, backfilled from the sample.**
   Rejected: it writes a derived value into the measurement's own history and makes the eventual
   move indistinguishable from a data correction. It also pre-empts a product decision about what the
   measurement's annotation means (which may include the layout/assay distinction the sample's enum
   does not carry).
4. **One resolution point behind the application seam.** The read model resolves the method from
   whichever source is authoritative today, and exposes the *result* — never the source — to callers.
   Accepted.

## Decision

**Adopt option 4. The analysis method is resolved in exactly one place, and callers never see where
it came from.**

1. **Single resolution point.** `v_project_assay_standing`
   (`sql/migrations/create-assay-standing-view.sql`) is the only statement that reads
   `sample.analysis_method` to determine a dataset's type. Everything upstream — `AssayStanding`,
   `ProjectPageReadRepository.findAssayStanding`, `ProjectQueryService.getAssayStanding`, and both
   surfaces that display it (the project summary's dataset card and `PrimaryDatasetsComponent`) —
   consumes the resolved `AnalysisMethod` and is agnostic to its origin.

2. **The migration is then a one-file change.** When the measurement carries its own annotation, the
   resolution changes *inside* the view (reading the measurement first, falling back to the sample).
   No Java row mapping, no application service, and no consumer changes — which is why the derivation
   is a view rather than a query string in a repository.

3. **The sample annotation is not deleted.** It is the historical record for projects registered
   before the move and the fallback for any measurement without its own annotation. It is retained as
   a **grandfather relation** — a permanently supported source, not a temporary hack to be cleaned up.

4. **Ambiguous attribution is stated, not hidden.** A measurement that measures samples carrying
   different analysis methods is attributed to **each** of them and counted in each. This follows the
   precedent already documented for measurements spanning experiments (trap 3 in
   `sql/migrations/create-project-page-read-models.sql`). Consequence, which must be documented
   wherever these counts are shown: **the per-dataset measurement counts of a project can sum to more
   than the number of measurement records.** It is not an error.

5. **Samples without an analysis method are excluded** from dataset grouping rather than grouped into
   an "unknown" dataset: a dataset that cannot be typed cannot be chosen for an analysis, and
   materialising an "unknown" dataset would state one that does not exist. The consequence — that
   such measurements are counted in the per-omics-domain progress figures but not in the
   per-analysis-method figures — is deliberate; the two figures answer different questions.

6. **The dataset identity is `(experiment, analysisMethod)`.** Two experiments measured as WES are two
   datasets; one experiment measured as WES and RNA-Seq is also two. A rollup by analysis method
   therefore must carry a *dataset count* alongside its measurement counts, because the method name
   alone loses that distinction.

## Consequences

**Positive**
- The overview can state WES / RNA-Seq / 16S today, at the grain readers choose analyses by.
- The eventual move to measurement-level annotation touches one query and one row mapper. The blast
  radius is bounded and known before the move starts.
- The landing page and the Datasets page share a grain by construction, so they cannot disagree about
  how many datasets a project holds.

**Negative / accepted costs**
- The overview's figures derive from a source the team considers wrong, for an unbounded period.
  Anyone reading `AssayStandingQuery` must be told why (hence this ADR, and the note in the query).
- Per-dataset measurement counts may not sum to the project's measurement total (decision 4). Every
  surface showing these numbers must not present them as summing.
- Samples without an analysis method are invisible to the dataset overview while remaining visible in
  the omics-domain figures (decision 5). A reader comparing the two will see a discrepancy.

**Neutral**
- `AnalysisMethod` remains the controlled vocabulary. It is already an enum covering the NGS,
  proteomics and metabolomics portfolios; the ADR does not change it. Whether it needs a
  layout/assay distinction (WES is an assay, "paired-end" is a layout) is a separate question and is
  **not** decided here.

## Follow-ups (not decided by this ADR)

1. Move the analysis-method annotation to the measurement, and make `AssayStandingQuery` prefer it.
2. Decide whether the vocabulary needs to distinguish assay from sequencing layout.
3. Decide whether the ambiguous-attribution rule (decision 4) becomes "Mixed" in the UI once
   measurement-level annotation exists — at which point ambiguity should become rare and surfacing it
   may be affordable.
