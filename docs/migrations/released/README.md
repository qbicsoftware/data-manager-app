# Released migration guides

This directory will hold one migration guide per released Data Manager version,
named `v<major>.<minor>.<patch>.md` to match the release's Git tag.

Each file is the **immutable, operator-facing** migration guide for that
release — once written, never modified. Corrections go into the next release's
guide.

## Current state

| Release | Guide |
|---|---|
| **1.14.0** | [`v1.14.0.md`](v1.14.0.md) |

For migrations targeting the upcoming (unreleased) version, see
[`../NEXT.md`](../NEXT.md).

Pre-structure (date-based) migration guides from before the three-tier layout
are still held in the parent [`..`](..) directory; consult those for
migrations they cover.

## File naming convention

- `v1.13.0.md` — full migration guide for 1.13.0
- `v1.13.1.md` — full migration guide for 1.13.1 (patch release)
- `v2.0.0.md` — full migration guide for a major release

Each file references the incremental scripts in [`../../sql/migrations/`](../../sql/migrations/)
that operators need to apply.
