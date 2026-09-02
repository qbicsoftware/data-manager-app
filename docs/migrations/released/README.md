# Released migration guides

This directory will hold one migration guide per released Data Manager version,
named `v<major>.<minor>.<patch>.md` to match the release's Git tag.

Each file is the **immutable, operator-facing** migration guide for that
release — once written, never modified. Corrections go into the next release's
guide.

## Current state

| Release | Guide | Scripts |
|---|---|---|
| **1.16.0** | [`v1.16.0.md`](v1.16.0.md) | [`v1.16.0/`](v1.16.0/) |
| **1.14.0** | [`v1.14.0.md`](v1.14.0.md) | [`v1.14.0/`](v1.14.0/) |

For migrations targeting the upcoming (unreleased) version, see
[`../NEXT.md`](../NEXT.md).

Pre-structure (date-based) migration guides from before the three-tier layout
are still held in the parent [`..`](..) directory; consult those for
migrations they cover.

## File naming convention

- `v1.13.0.md` — full migration guide for 1.13.0
- `v1.13.1.md` — full migration guide for 1.13.1 (patch release)
- `v2.0.0.md` — full migration guide for a major release

Each file references the incremental scripts that operators need to apply.

## Co-located SQL scripts

Each released guide is shipped alongside **its own copy of the SQL scripts** it
references, in a per-release subfolder named after the release:

```
released/
├── v1.14.0.md
├── v1.14.0/
│   ├── create-associated-dataset.sql
│   └── extend-dataset-visibility-in-project-overview.sql
├── v1.16.0.md
└── v1.16.0/
    └── create-user-external-credential.sql
```

**Rule:** a released guide must only reference a script that exists in its own
subfolder (`released/v<version>/<name>.sql`). When a new migration lands in the
development branch, copy the incremental script from
[`../../sql/migrations/`](../../sql/migrations/) into the release's subfolder and
point the guide at that copy. This keeps every guide self-contained and ensures
the migration guide always references an existing SQL script.
