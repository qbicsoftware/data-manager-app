# Task Plan: Build and Publish Lane — Make the GitHub Release Self-Contained

> **Scope:** Nothing in this plan requires Nexus, an external registry, or any network
> location outside GitHub.
> **Companion plan:** `docs/plans/PLAN-openbis-artifact-hosting.md` (inbound dependency lane).
> **Status:** Proposed — not started.
> **Written:** 2026-11-25, agent session, following an audit of the failing workflows.

---

## 1. Problem

The Data Manager build is self-contained: all 18 modules build in-reactor, and
`datamanager-bom` is imported at `${project.version}`, so it resolves from the reactor.
Nevertheless both publish workflows deploy to QBiC Nexus, which is no longer reachable
from GitHub Actions egress.

`create-release.yml` deploys **before** it creates the GitHub release:

| Step | Action | Effect when Nexus is unreachable |
|---|---|---|
| 6 | `mvn -B package -Pproduction` (line 51) | succeeds |
| 7 | `actions/attest-build-provenance` (line 55) | succeeds |
| 8 | `mvn ... deploy` to Nexus (lines 60–64) | **fails after ~18 × 10 s connect timeouts** |
| 9 | `Collect release assets` | never reached |
| 11 | `Create or update GitHub Release` | never reached |
| 12 | version-bump branch / commit / PR | never reached |

Evidence: run [45400605244](https://github.com/qbicsoftware/data-manager-app/actions/runs/45400605244)
(Deploy Snapshot, 2026-11-24) fails with 172 transfer errors of the form
`Connect to qbic-repo.qbic.uni-tuebingen.de:443 [134.2.24.115] failed: Connect timed out`.

**Consequence:** the release workflow cannot produce a release at all — not a degraded
one, none. Release `1.20.4` (2026-11-13) predates the outage and has not been
reproducible since.

### 1.1 Why the deploy is dead weight

- `life.qbic.datamanager:*` has **no consumers outside this repository**. An org-wide
  code search of `qbicsoftware` returns only this project's own sources plus
  `THIRD-PARTY.txt`, which is a generated report rather than a dependency.
- The release already IS the distribution channel: `1.20.4` carries 36 assets,
  151 MB in total, including `datamanager-app-1.20.4.jar` at **149 MB** — the Spring
  Boot fat jar, i.e. the runnable artifact.
- Signing and provenance are unaffected by removing the deploy:
  `attest-build-provenance` and the `.sigstore.json` bundles stay exactly as they are.

---

## 2. Goals

1. `create-release.yml` completes end-to-end with no network destination outside GitHub,
   and creates the GitHub release **before** any optional step can fail.
2. No workflow spends runner minutes attempting a connection to a host known to be
   unreachable.
3. The released artifact set documents its own dependency graph (SBOM attached).
4. Removing the publish lane is **reversible** with `git revert` — a future reachable
   registry becomes a small re-add, not an archaeology exercise.

## 3. Non-Goals

- Hosting `life.qbic:zzzz-openbis-api` — see the companion inbound plan.
- Replacing Nexus with GitHub Packages, CodeArtifact, Cloudsmith, or a tunnel. This plan
  deliberately chooses **no registry** on the outbound side.
- Changing application runtime behaviour, versioning scheme, or release cadence.

---

## 4. Decisions (settled 2026-11-25 by the project owner)

- [x] **No external consumer of published artifacts.** Nothing outside `qbicsoftware`
      resolves `life.qbic.datamanager:*`. The outbound lane is therefore *deleted*, not
      replaced.
- [x] **Snapshot publishing is retired.** Builds are introduced later, and releases are
      tagged on a chosen commit of `main`. `nexus-publish-snapshots.yml` is deleted
      outright, not parked.
- [x] **GitHub Packages is the fallback if publishing is ever needed again.** It is a
      per-repository Maven registry, so no external host is involved. It is *not* adopted
      now because reads require a token even for a public repository.
- [ ] Note for the implementing agent: `main` is public. Do not put registry credentials
      in commit messages, PR bodies, or logs.

---

## 5. Task Breakdown

Work in order. Tasks 1–6 can land as one PR; Task 7 lands separately because it changes
release semantics.

### Task 1 — Remove the deploy destination from the root POM

**File:** `pom.xml`

- Delete the whole `<distributionManagement>` block, **lines 141–154**, which pins
  `nexus-releases` / `nexus-snapshots` to
  `https://qbic-repo.qbic.uni-tuebingen.de/repository/...`.
- Leave `<repositories>` (lines 73–123) and `<pluginRepositories>` (lines 125–140)
  **alone in this plan** — the companion plan owns that block. See §9, inventory conflict.
- Leave `central`, `vaadin-prereleases`, and `Vaadin Directory` untouched.

### Task 2 — Remove the deploy destination from the BOM

**File:** `datamanager-bom/pom.xml`

- Delete the trailing `<distributionManagement>` block, **lines 267–280**, together with
  the comment on line 266 that explains why it exists.
- No other change: the BOM declares no `<repositories>`.

### Task 3 — (Recommended) Park the Nexus coordinates behind an env-var profile

**File:** `pom.xml`

This is the cheap escape hatch if the university restores access. Adding it costs nothing
at build time because it is never activated without the environment variable:

```xml
<profile>
  <id>qbic-nexus</id>
  <activation>
    <property><name>env.NEXUS_BASE_URL</name></property>
  </activation>
  <repositories>
    <repository>
      <id>nexus-releases</id>
      <url>${env.NEXUS_BASE_URL}/repository/maven-releases</url>
      <releases><enabled>true</enabled><checksumPolicy>fail</checksumPolicy></releases>
      <snapshots><enabled>false</enabled></snapshots>
    </repository>
    <repository>
      <id>nexus-snapshots</id>
      <url>${env.NEXUS_BASE_URL}/repository/maven-snapshots</url>
      <releases><enabled>false</enabled></releases>
      <snapshots><enabled>true</enabled><checksumPolicy>fail</checksumPolicy></snapshots>
    </repository>
  </repositories>
</profile>
```

**Omit `<updatePolicy>always</updatePolicy>`**, which the current entries carry at
`pom.xml` lines 106 and 116. That single setting is what converts an unreachable host
from a one-time cost into a per-build stall, and it must not be reintroduced.

### Task 4 — Remove the deploy step from the release workflow

**File:** `.github/workflows/create-release.yml`

- Delete the step `Publish artefact to QBiC Nexus Repository`, **lines 60–64**, including
  its `MAVEN_REPO_USERNAME` / `MAVEN_REPO_PASSWORD` env block.
- Delete `settings-path: ${{ github.workspace }}` from the `Set up JDK 21` step
  (**line 38**). It exists only to place `.github.settings.xml`; with the deploy gone it
  is a no-op that writes a stray `settings.xml` into the checkout.
- **Blast-radius note (corrected during implementation).** An earlier revision of this
  plan said to move `Create or update GitHub Release` above `Collect release assets`.
  That is impossible: the release step consumes `release-assets.txt`, which the
  collection step produces, so the order must stay collection → release. The real win is
  simply that the deploy step no longer sits between the build and the release — after
  this change only a failed build or a failed attestation can block a release.
  - Keep the upload idempotent — `--clobber` is already present, so a re-run after a
    partial failure still works.
- Leave the version-bump branch / commit / PR steps at the end; they are cheap and local.

### Task 5 — Retire the snapshot publish workflow

**File:** `.github/workflows/nexus-publish-snapshots.yml`

- `git rm` the file. Once Tasks 1, 2 and 4 land it has no purpose, and its `on:` triggers
  (every push to `development`, every PR to `main`) make it the largest single source of
  wasted runner minutes in the repository.
- If the team wants to keep a snapshot channel for a future registry, **park it disabled**
  with `if: ${{ vars.ENABLE_SNAPSHOT_PUBLISH == 'true' }}` on the job rather than
  leaving it pointing at an unreachable host.

### Task 6 — Delete the committed settings file and retire the secrets

**File:** `.github.settings.xml` (delete)

- The file contains only `<servers>` credentials for the two Nexus IDs. After Task 4 it is
  referenced by nothing — verify with
  `grep -rn 'github.settings.xml' . --exclude-dir=.git` (expected: no hits).
- Drop `settings-path` from the remaining workflows that carry it:
  `.github/workflows/codeql-analysis.yml` (line ~53) and
  `.github/workflows/run_tests.yml` (line ~30). `sonarcloud.yml` and
  `build_package.yml` never set it — use them as the reference shape.
- **Manual follow-up, for a human with credentials:** delete the repository secrets
  `NEXUS_USERNAME` and `NEXUS_PASSWORD` under Settings → Secrets and variables → Actions.
  An agent must not attempt this.

### Task 7 — Attach the SBOM to the release

**File:** `.github/workflows/create-release.yml`

`Collect release assets` currently globs only `**/target/*.jar` and
`**/target/*.jar.sigstore.json`, so the CycloneDX SBOM produced by the `sigcheck` profile
(`${project.artifactId}-${project.version}-sbom.json`) is **never attached**.
Verified: release `1.20.4` has no `*bom*` asset.

**Correction to an earlier revision of this plan: there is no aggregate SBOM to attach.**
The `sigcheck` profile activates on `<file><exists>src/main</exists></file>`, and the root
aggregator has no `src/`, so the top-level `aggregate-bom`/`module-bom` executions never run
there. Each of the 18 modules emits its own `<artifactId>-<version>-sbom.json` (schema 1.5)
plus a `bom.json`. The CI log also shows a real hazard:
`[WARNING] artifact ... already attached, replace previous instance` — `bom.json` and
CycloneDX's default `application.cdx.json` contend for the same classifier.

- Collect only `*-sbom.json`, whose names are unique per module, and exclude `bom.json`
  explicitly: `find . -path '*/target/*-sbom.json' -not -name 'bom.json'`.
- Do **not** collect `bom.json.sigstore.json` either — same colliding basename.
- Reference `docs/security/vulnerability-management.md` and `.github/grype.yaml` in the PR
  body, since a shipped SBOM is what those processes assume.

---

## 6. Acceptance Criteria

| # | Criterion | How to verify |
|---|---|---|
| 1 | No file references `qbic-repo.qbic.uni-tuebingen.de` | `grep -rn 'qbic-repo' . --exclude-dir=.git` returns only historical docs, if any |
| 2 | No file references `.github.settings.xml` | `grep -rn 'github.settings.xml' . --exclude-dir=.git` returns nothing |
| 3 | Neither POM declares `<distributionManagement>` | `grep -n distributionManagement pom.xml datamanager-bom/pom.xml` returns nothing |
| 4 | `mvn deploy` is invoked by no workflow | `grep -rn 'deploy' .github/workflows/` shows no `mvn ... deploy` |
| 5 | The release is created before any optional step | Structural review of step order, or a `workflow_dispatch` dry run in a fork |
| 6 | Release assets include one SBOM per module | Next release: `gh release view <tag> --json assets` lists 18 `*-sbom.json` entries and no plain `bom.json` |
| 7 | Offline reactor build still succeeds with a warm cache | `./mvnw -B -o clean verify -DskipTests` |

## 7. Verification Sequence (run before opening the PR)

```bash
# 1. structural checks
grep -rn 'qbic-repo\|distributionManagement\|github.settings.xml' \
  pom.xml datamanager-bom/pom.xml .github/ || echo "clean"

# 2. offline build proves no external registry is needed (warm cache required)
./mvnw -B -o clean verify -DskipTests

# 3. workflow YAML still parses
for f in .github/workflows/*.yml; do
  python3 -c "import yaml,sys; yaml.safe_load(open('$f'))" || echo "BAD: $f"
done
```

Note on check 2: `-o` fails on a cold cache because of the openbis artifact. That is
expected and is the companion plan's responsibility, not this one's.

## 8. Rollback

One `git revert` of the PR restores both POM blocks, the deploy step, the snapshot
workflow and the settings file. No state to unwind. The only irreversible act is deleting
the two repository secrets, and those are trivially recreated from credentials the Nexus
administrator holds.

## 9. Inventory Conflict With the Companion Plan

Both plans touch the repository inventory in `pom.xml`. **Exactly one PR must own any
given block**, or the two branches will conflict on every rebase:

| Block | `pom.xml` lines | Owner |
|---|---|---|
| `<repositories>` (nexus entries) | 97–122 | Companion inbound plan |
| `<distributionManagement>` | 141–154 | **This plan** |
| `.github/**` (all workflows, settings file) | — | **This plan** |
| `datamanager-bom/pom.xml` `<distributionManagement>` | 267–280 | **This plan** |

Land this plan first: it unblocks releases immediately and touches the fewer
load-bearing files. The inbound plan then re-points `<repositories>` in a clean tree.

## 10. Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| An out-of-org consumer of `life.qbic.datamanager:*` appears later | Low | Settled as non-existent; if one ever appears, adopt GitHub Packages (per-repo Maven registry, no external host) rather than reviving Nexus |
| A contributor needs snapshots before a tagged release | Low–Medium | Retired deliberately; the workflow is one `git revert` away, and GitHub Packages is the intended target if it returns |
| Both plans edit `pom.xml` inventory in parallel | **Medium** | §9 single-owner table |
| Removing Nexus as a mirror exposes a Central outage | Very low | `central` was already declared with highest priority; Nexus was never a true mirror |
| A future agent reintroduces `updatePolicy=always` | Medium | Explicit warning in Task 3 and in the follow-ups below |

## 11. Out of Scope / Follow-ups

- Inbound dependency hosting → `docs/plans/PLAN-openbis-artifact-hosting.md`.
- Deciding whether to re-establish a registry if university access returns (Task 3 makes
  this cheap).
- Whether the 17 internal module jars belong in the release at all, given
  `datamanager-app-*.jar` (149 MB) is the only runnable artifact and the rest are ~2 MB
  each with no external consumers.
- Re-evaluating signing/provenance coverage once the published-to-Nexus path is gone:
  confirm Scorecard's Signed-Releases check still passes, since it looks for the
  `.sigstore.json` bundles among release assets.
