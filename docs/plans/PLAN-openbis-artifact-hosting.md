# Task Plan: Inbound Dependency Lane — Host `zzzz-openbis-api` Off Nexus

> **Scope:** Dependency *resolution* only. Publishing and the GitHub release are handled
> separately in `docs/plans/PLAN-build-publish-lane.md`.
> **Intended executor:** a **separate agent session**. This work needs a
> repository-creation decision (a human may have to create the repo) and a network
> harvest window (§5 Task 3) that must not be assumed.
> **Status:** Proposed — not started.
> **Written:** 2026-11-25, agent session, following an audit of the failing workflows.

---

## 1. Problem

Every Nexus-hosted coordinate in the reactor builds in-reactor **except one**:

```
life.qbic:zzzz-openbis-api:20.10.7.3:jar:r1700646105
```

Declared in `datamanager-bom/pom.xml` (dependencyManagement, around line 253, with a
`<classifier>` of `r1700646105`) and consumed in `datamanager-app/pom.xml` (around
line 272).

Facts established by inspection, to be re-verified in §5 Task 3:

| Property | Value |
|---|---|
| jar size | 13,026,477 bytes |
| jar SHA-1 | `5c7350674e178b6c19a0c4946cc7d97470693001` |
| POM SHA-1 | `26d3f5a64c9830733afdbb326612ec10bb517cef` |
| POM content | **empty** — no parent, no dependencies, no plugins |
| Transitive dependencies | **none** |
| Signature | none published (`*.jar.asc` returns 404) |
| Signature policy | already waived — `pgp-keys-override.list` line 6 is `life.qbic:*:* = noSig` |
| Public alternative | **none** — `repo1.maven.org/maven2/life/qbic/zzzz-openbis-api/` → 404, upstream `ch/ethz/sis/` → 404 |
| Licence | openBIS is Apache 2.0, so mirroring the binary publicly is acceptable |

Because the POM is empty and nothing is transitive, hosting this is **two files and two
checksums**. There is no dependency graph to reproduce.

### 1.1 Failure modes observed

- **Cold cache (CI):** the build fails outright.
- **Warm cache, unreachable host:** the entries carry `<updatePolicy>always</updatePolicy>`
  (`pom.xml` lines 106 and 116), so every build revalidates against an unreachable host and
  stalls. Failures are cached in `~/.m2` as `.lastUpdated` markers, which then poison
  later builds until forced with `-U` or deleted.
- **Reachable host, changed repository IDs:** Maven prints
  `cached from a remote repository ID that is unavailable in current build context,
  verifying that is downloadable from [... nexus-releases ...]` and re-checks remotely for
  every artifact — including ones from Central and Vaadin. This is the wall of
  `[INFO] Artifact ...` lines visible in recent CI logs.

Nexus is currently reachable **from a developer machine**: `HTTP 200`,
`Server: Nexus/3.98.3-01 (COMMUNITY)`. It is GitHub Actions egress that is blocked. That is
precisely why §5 Task 3 treats the harvest as a time-boxed operation rather than something
that can be re-fetched whenever convenient.

### 1.2 Shared need, not a Data Manager quirk

`qbicsoftware/data-download-server` and `qbicsoftware/openbis20-java-dropboxes` also
consume openBIS. A dedicated hosting repository serves all three; this plan should not
scope the solution to Data Manager alone, even though Data Manager is the project driving
it.

---

## 2. Goals

1. A clean checkout on a cold cache resolves every dependency from hosts reachable from
   GitHub Actions, with **no credentials required for reads**.
2. `zzzz-openbis-api` is hosted durably and versioned, with checksums, so the existing
   `checksumPolicy=fail` continues to mean something.
3. The result is consumable by a fork, by Dependabot, and by a new developer with no
   `settings.xml` edit and no account.

## 3. Non-Goals

- Publishing Data Manager artifacts (outbound) — see the companion plan.
- Mirroring Central or Vaadin. `central` is already declared with highest priority and
  Nexus was never a true mirror.
- Upgrading openBIS. Keep the pinned version and classifier.
- Vendoring into `data-manager-app` itself, except as the documented fallback in §4.

---

## 4. Design Decision (settle this first, in the executor session)

| Option | Anonymous read | Setup | Verdict |
|---|---|---|---|
| **A. Dedicated `qbicsoftware/maven-repository` + GitHub Pages** | yes, plain HTTPS | ~2 h | **Recommended** |
| B. In-repo `maven-repo/` served over `file://` | yes (it is just git) | ~1 h | Fallback if repo creation is blocked |
| C. GitHub Packages (Maven registry) | **no** — a token is required even for public reads | half day | Rejected for reads; viable for a future snapshot channel |
| D. Gist | impossible | — | Wrong content type, flat layout, 5 files per gist, 10 MB cap, nowhere for checksums |
| E. Maven Central | impossible | — | Namespace and provenance are not ours to publish |

**Decision (2026-11-25):** option **A**, and the project owner is creating
`qbicsoftware/maven-repository`. Option B stays documented as the fallback if Pages is
unavailable; C stays the answer for *publishing* if that is ever reintroduced. The
reasons D and E fail are settled; do not re-litigate them in review.

Recommended layout for option A — a `gh-pages` branch of a new public repository
`qbicsoftware/maven-repository`:

```
releases/
  life/qbic/zzzz-openbis-api/20.10.7.3/
    zzzz-openbis-api-20.10.7.3.pom
    zzzz-openbis-api-20.10.7.3.pom.sha1
    zzzz-openbis-api-20.10.7.3-r1700646105.jar
    zzzz-openbis-api-20.10.7.3-r1700646105.jar.sha1
```

Served as `https://qbicsoftware.github.io/maven-repository/releases` — stable,
credential-free, cacheable, with correct content types.

Omit `maven-metadata.xml` unless a `*-SNAPSHOT` or version-range dependency appears later.
Nothing here uses either, and keeping this repository static is what makes it cheap to
trust (§8).

---

## 5. Task Breakdown

### Task 1 — Record the decision

- The §4 decision is made (option A). Still put the outcome and the D/E rejection reasons
  into the PR description so the choice is not reopened.
- If the team chooses B, state the tradeoff explicitly: 13 MB enters `data-manager-app`
  history permanently. Note that `.gitignore` does not block it — the comment block around
  line 19 only guards `.mvn/wrapper/maven-wrapper.jar` — but the stated intent there
  ("never commit a binary") means the comment must be amended if B is chosen.

### Task 2 — Prepare the hosting location (repo creation done by the owner)

**Option A** — the repository **already exists** (created by the project owner); this task
starts from an empty repo.

- Add a `gh-pages` branch carrying the layout above. If the repository is brand new, the
  simplest sequence is: create the branch orphan (`git checkout --orphan gh-pages`), add
  the files, commit, push, then set it as the Pages source.
- Enable Pages: source = `gh-pages` branch, root.
- Add a `../README.md` stating what is hosted and why, the Apache 2.0 licence of openBIS, the
  pinned version and classifier, and the short `<repository>` snippet for consumers.
- Confirm serving before touching any POM:
  ```bash
  curl -sI https://qbicsoftware.github.io/maven-repository/releases/life/qbic/zzzz-openbis-api/20.10.7.3/zzzz-openbis-api-20.10.7.3-r1700646105.jar
  ```
  Expect `200` and a binary content type, **not** `text/plain`.

**Option B**

- Create `maven-repo/` at the root of `data-manager-app` with the same layout.

> **Order matters:** the new location must accept the artifact *before* any POM points at
> it. Otherwise the build is broken in the interval between the two changes.

### Task 3 — Harvest the artifact while it is still reachable

> **Status: DONE 2026-11-25.** Both files were fetched from Nexus and their SHA-1s verified
> against §1 exactly (pom `26d3f5a6…`, jar `5c735067…`). The staged layout, including
> `.sha1` files, is at `/tmp/qbic-maven-repository-staging/releases/`. Push that tree to
> `gh-pages`, then delete the temp directory. The steps below are retained so the harvest
> can be reproduced or re-verified.

Confirm reachability first, and do not assume it:

```bash
curl -sS -o /dev/null -w '%{http_code}\n' \
  https://qbic-repo.qbic.uni-tuebingen.de/repository/maven-releases/life/qbic/zzzz-openbis-api/20.10.7.3/zzzz-openbis-api-20.10.7.3.pom
```

On `200`, harvest and **verify the SHA-1s against §1 before committing anything**:

```bash
BASE=https://qbic-repo.qbic.uni-tuebingen.de/repository/maven-releases/life/qbic/zzzz-openbis-api/20.10.7.3
mkdir -p staging && cd staging
curl -sSLO "$BASE/zzzz-openbis-api-20.10.7.3.pom"
curl -sSLO "$BASE/zzzz-openbis-api-20.10.7.3-r1700646105.jar"
shasum -a 1 *   # must print 26d3f5a6... (pom) and 5c735067... (jar)
```

On a timeout, **escalate rather than improvise**. The jar may still exist in a teammate's
cache at `~/.m2/repository/life/qbic/zzzz-openbis-api/20.10.7.3/`, and a copy that matches
the SHA-1 in §1 is acceptable. An unverified copy is not.

Generate the checksum files in Maven's format (40 hex characters, nothing else on the
line):

```bash
shasum -a 1 zzzz-openbis-api-20.10.7.3.pom | cut -d' ' -f1 > zzzz-openbis-api-20.10.7.3.pom.sha1
shasum -a 1 zzzz-openbis-api-20.10.7.3-r1700646105.jar | cut -d' ' -f1 > zzzz-openbis-api-20.10.7.3-r1700646105.jar.sha1
```

Equivalent one-step alternative that writes the layout and checksums together (handy for
option B):

```bash
mvn install:install-file \
  -Dfile=zzzz-openbis-api-20.10.7.3-r1700646105.jar \
  -DgroupId=life.qbic -DartifactId=zzzz-openbis-api -Dversion=20.10.7.3 \
  -Dclassifier=r1700646105 -Dpackaging=jar \
  -DlocalRepositoryPath=maven-repo -DcreateChecksum=true
```

Publish the harvested files, then re-verify the SHA-1s **from the new host**. A transfer
that mangles bytes is the one failure `checksumPolicy=fail` will not forgive.

### Task 4 — Re-point the repository declarations

**File:** `pom.xml` — the `<repositories>` block, **lines 73–123**; the Nexus entries to
replace are `nexus-snapshots` (lines 97–109) and `nexus-releases` (lines 110–122).

Replace both with a single release-only repository:

```xml
<repository>
  <id>qbic-maven-repository</id>
  <name>QBiC Maven Repository</name>
  <url>https://qbicsoftware.github.io/maven-repository/releases</url>
  <releases>
    <checksumPolicy>fail</checksumPolicy>
    <enabled>true</enabled>
  </releases>
  <snapshots>
    <enabled>false</enabled>
  </snapshots>
  <!-- Deliberately no <updatePolicy>always</updatePolicy>: an unreachable
       third-party host must cost one failed lookup, not a per-build stall. -->
</repository>
```

For option B, use `<url>file://${maven.multiModuleProjectDirectory}/maven-repo</url>`.
**Use `maven.multiModuleProjectDirectory`, not `project.basedir`** — the latter resolves
per module, so the path breaks for 17 of the 18 modules.

Leave `<pluginRepositories>` (lines 125–140) untouched; no plugin needs the new host.

Add a comment above the block explaining *why* the QBiC host is gone, with a link to this
plan. The next reader otherwise assumes the removal was accidental.

### Task 5 — De-poison developer caches (documented, one-off)

Not a code change, but it must appear in the PR body so the first local build after the
merge does not confuse anyone:

```bash
# Drop stale "cached from an unavailable remote repository ID" markers
find ~/.m2/repository -name '_remote.repositories' -delete
# Drop failure markers left by the outage
find ~/.m2/repository -name '*.lastUpdated' -delete
```

Without the first line, Maven re-validates every cached artifact remotely and emits the
long `Artifact ... is present in the local repository, but cached from a remote repository
ID that is unavailable` wall seen in CI logs. Without the second, failed openbis lookups
keep short-circuiting resolution even once a working host exists.

Put both in the PR body, and in `docs/operations/` if a fitting page exists.

### Task 6 — Verify from cold

The entire point is a cold-cache build, so proving it requires discarding the cache:

```bash
# 1. cold-cache resolution of the reactor
mv ~/.m2/repository/life/qbic ~/.m2/repository/life.qbic.bak
./mvnw -B -U clean verify -DskipTests

# 2. prove no request reaches Tübingen
./mvnw -B -X clean verify -DskipTests 2>&1 | grep -c 'qbic-repo.qbic.uni-tuebingen.de'   # expect 0

# 3. restore
rm -rf ~/.m2/repository/life/qbic && mv ~/.m2/repository/life.qbic.bak ~/.m2/repository/life/qbic
```

### Task 7 — Prove it in CI

- Push a branch and confirm `Build Maven Package` and `Run Maven Tests` pass **with the
  cache step neutralised** — bump the cache key, e.g.
  `key: cold-${{ github.run_id }}`, or the run proves nothing about a cold cache.
- Revert the temporary cache-key change before merging.

---

## 6. Acceptance Criteria

| # | Criterion | How to verify |
|---|---|---|
| 1 | A cold-`~/.m2` build resolves everything from hosts reachable by GitHub Actions | §5 Task 6 step 1, plus a CI run with a fresh cache key |
| 2 | No build request reaches `qbic-repo.qbic.uni-tuebingen.de` | §5 Task 6 step 2 returns 0 |
| 3 | Hosted jar and POM match the recorded SHA-1s | `shasum -a 1` on the downloaded files |
| 4 | Consuming requires no credentials | `curl` the repository URL with no auth; must be `200` |
| 5 | No dependency source named `nexus-releases` / `nexus-snapshots` remains | `grep -n 'nexus-' pom.xml datamanager-bom/pom.xml` |
| 6 | Coordinates are unchanged — only the URL moved | diff shows no change to groupId/artifactId/version/classifier |
| 7 | A fresh clone with a different `~/.m2` can build | Optional but decisive; a second `-Dmaven.repo.local` path is enough |

## 7. Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Nexus becomes unreachable before the harvest runs | ~~Medium~~ **Resolved** | Harvest done and SHA-1-verified on 2026-11-25; the artifact no longer depends on Nexus availability |
| The executor session has no network path to Tübingen | ~~Medium~~ **Resolved** | Harvest already complete; the staged copy is hash-verified |
| GitHub Pages serves an unexpected content type | Low | The `curl -I` check in §5 Task 2 |
| Pages disabled or repo renamed, silently breaking builds | Low | The URL appears in exactly one POM file; document it in the hosting repo's README |
| Both plans edit the `pom.xml` inventory | **Medium** | Single-owner table below |
| 13 MB binary in git history (option B only) | Certain for B | Accepted tradeoff if A is blocked; do not switch to B silently |
| Licence objection to public mirroring | Low | Apache 2.0; state it in the hosting repo's README |

## 8. Inventory Boundary With the Companion Plan

| Block | `pom.xml` lines | Owner |
|---|---|---|
| `<repositories>` — the two Nexus entries | 97–122 | **This plan** |
| `<pluginRepositories>` | 125–140 | neither; leave alone |
| `<distributionManagement>` | 141–154 | Companion build/publish plan |
| `datamanager-bom/pom.xml` `<distributionManagement>` | 267–280 | Companion build/publish plan |
| `.github/**` | — | Companion build/publish plan |

Sequence: land the build/publish plan first (it unblocks releases and touches fewer
load-bearing files), then this one on a clean tree.

## 9. Out of Scope / Follow-ups

- Outbound publishing and the release workflow → `docs/plans/PLAN-build-publish-lane.md`.
- Upstreaming `zzzz-openbis-api` to a public mirror such as Maven Central — blocked by
  namespace and provenance; do not attempt.
- Mirroring Central or Vaadin. If build times matter, a hosted pull-through cache is a
  separate decision, and `updatePolicy=always` must not be reintroduced for it.
- Re-pointing `data-download-server` and `openbis20-java-dropboxes` at the new hosting
  repo. Recommended, but each project does it in its own PR.
- `datamanager-app/pom.xml` declares its own `<repositories>` block (central, vaadin-prereleases,
  Vaadin Directory — no Nexus entries). It inherits nothing from the root POM's repository
  list, so it does **not** need the new host; only the root POM does. Verified as part of the
  build/publish work.
