# Migration documentation

This directory holds migration documentation for the Data Manager application.
It is organized in **three tiers**, each with a distinct audience and purpose:

| Tier | Location | Audience | Purpose |
|---|---|---|---|
| **1. Runnable DDL** | [`sql/migrations/<name>.sql`](../sql/migrations/) | Developers / SREs | Individual, idempotent DDL scripts, one per schema change |
| **2. Operator guide per release** | `released/v<version>.md` or [`NEXT.md`](NEXT.md) | Operators | One file per release listing every migration to apply when upgrading to that version |
| **3. Index** | this file + [`sql/migrations/README.md`](../sql/migrations/README.md) | Everyone | Points to the right place depending on what you're trying to do |

All three tiers together answer every question an operator or developer might
have during a schema change: *what's the change*, *where's the script*, *what
do I apply during upgrade*, *what if I need to roll back*.

---

## How the tiers relate

```
sql/migrations/
├── README.md                         ← Tier 3: incremental script index
└── <name>.sql                        ← Tier 1: one file per change

docs/migrations/
├── README.md                         ← this file
├── NEXT.md                           ← Tier 2: working doc for the next release
└── released/
    ├── v1.14.0.md                    ← Tier 2: released operator guides
    └── v1.13.0.md
```

Each Tier-1 script is referenced by exactly one Tier-2 entry. Tier-2 entries
point at their Tier-1 scripts and add pre-flight, post-migration verification,
and rollback detail. Tier-3 files explain the structure.

---

## I'm an operator deploying a release

Open the migration guide that corresponds to the version you're deploying to:

- **Unreleased / on a branch:** [`NEXT.md`](NEXT.md) — the active doc listing
  all migrations that will ship in the next release
- **Released:** `released/v<version>.md` — immutable once published

Each guide has a summary table of all migrations plus a per-migration section
covering:

- **What it does** — explanation + link to story/feature/ADR
- **Pre-flight** — SQL checks to run before applying
- **Apply** — command to run the incremental script
- **Verify** — SQL checks to confirm success
- **Rollback** — reversal steps (often destructive — always read carefully)
- **Operator notes** — caveats (downtime, data migration, cascade effects)

### Upgrading across multiple releases

If you are upgrading from version `vA` to version `vB` and several releases
exist in between, apply each release's migrations in version order. The
migrations are designed to be **idempotent** (e.g. `CREATE TABLE IF NOT
EXISTS`), so re-running a migration is safe.

---

## I'm a developer introducing a schema change

When you add a schema change (new table, altered column, new index, new view, …):

1. **Create an incremental script** at `sql/migrations/<kebab-name>.sql`.
   - Keep it idempotent (`CREATE TABLE IF NOT EXISTS`, `ALTER TABLE … ADD COLUMN
     IF NOT EXISTS`, etc.).
   - Include a header comment with the story/feature number, ADRs, and date.

2. **Append an entry to [`NEXT.md`](NEXT.md).** Use the template section at the
   bottom of that file. The entry links to your script and repeats the pre-flight
   / apply / verify / rollback instructions so operators can review in a single
   location.

3. **Do not touch `released/`** — that directory is only edited at release cut.

See [`sql/migrations/README.md`](../sql/migrations/README.md) for the incremental
script naming convention and header format.

---

## I'm the release manager cutting a release

At release cut:

1. Confirm the target version number in root `pom.xml`.
2. Freeze [`NEXT.md`](NEXT.md) — no further entries after this point.
3. Copy it to `released/v<version>.md`.
4. If needed, add a release-wide pre-flight section to the new file (e.g. backup
   instructions, cross-datasource sanity checks).
5. Reset `NEXT.md` to a fresh empty template for the *subsequent* release.
6. Commit the release-cut commit.

`NEXT.md` is a rolling document: it represents the "draft" migration guide for
whatever is currently in the development branch, and is only frozen once the
release is cut.

---

## Naming conventions

| Artifact | Convention | Example |
|---|---|---|
| Incremental script | `sql/migrations/<kebab-case>.sql` | `create-associated-dataset.sql` |
| Released operator guide | `docs/migrations/released/v<major>.<minor>.<patch>.md` | `released/v1.13.0.md` |
| Next-release draft | `docs/migrations/NEXT.md` | (always this name) |
| Migration header comment | `-- Story:`, `-- Feature:`, `-- ADR:`, `-- Date:` | see [`create-associated-dataset.sql`](../sql/migrations/create-associated-dataset.sql) |

---

## Historical note — pre-2026-07 migrations

Before the three-tier structure was established, migration guides followed a
date-based naming convention (e.g., `2026-06-11-remove-materialized-ip-table-*.md`).
Those documents remain in this directory as historical records and are still
authoritative for the migrations they describe. Going forward, all new
migrations use the three-tier structure documented above.

**List of pre-structure guides:**

- [`2026-06-11-remove-materialized-ip-table-and-optimize-indexes.md`](./2026-06-11-remove-materialized-ip-table-and-optimize-indexes.md)
