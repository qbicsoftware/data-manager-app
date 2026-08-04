# OpenCode Worktree Workflow

`@sven1103/opencode-worktree-workflow` is an npm package that provides OpenCode git worktree helpers for creating synced feature worktrees and cleaning up merged ones.

## Quick start

To get the workflow running in a project:

1. Install the package once by following [Recommended setup](#recommended-setup).
2. Enable the plugin in your OpenCode config as shown in [Recommended setup](#recommended-setup).
3. If you want manual `/wt-new` and `/wt-clean` triggers, install the markdown files from [Install slash commands](#install-slash-commands).
4. If you want policy guidance for when to isolate work, install the skill from [Co-shipped skill](#co-shipped-skill).
5. If you need to understand how the local fallback works, see [CLI fallback](#cli-fallback).

## Recommended setup

Install the package once:

```sh
npm install -D @sven1103/opencode-worktree-workflow
```

Enable the native OpenCode plugin in `opencode.json`:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "plugin": ["@sven1103/opencode-worktree-workflow"]
}
```

This single package provides two access modes:

- native plugin tools inside OpenCode: `worktree_prepare`, `worktree_cleanup`
- local CLI fallback from the same installed package:
  - `npx opencode-worktree-workflow wt-new "<title>" --json`
  - `npx opencode-worktree-workflow wt-clean <args> --json`

In practice:

- if the plugin is loaded, use the native tools first
- if the native tools are unavailable, use the local CLI fallback from the same installed package
- if the package is not installed, no CLI fallback is available

## Install in an OpenCode project

Add the package as a project dependency, following the official docs style:

```json
{
  "dependencies": {
    "@sven1103/opencode-worktree-workflow": "^0.2.0"
  }
}
```

Then reference the installed package from your OpenCode config:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "plugin": ["@sven1103/opencode-worktree-workflow"]
}
```

Keeping the npm dependency in `package.json` makes the installation more durable even if shared `opencode.json` bundles overwrite plugin entries.

If you do not already install dependencies in your project, you can add the package directly with npm:

```sh
npm install -D @sven1103/opencode-worktree-workflow
```

## Install slash commands

OpenCode loads custom commands from either `.opencode/commands/` (project) or `~/.config/opencode/commands/` (global).

This repo publishes `wt-new.md` and `wt-clean.md` as GitHub Release assets so you can install them without browsing the repository.
For a plain-language explanation of what each release contains, how it is produced, and how to verify it before installing, see `docs/releases.md`.

Project install (latest release):

```sh
mkdir -p .opencode/commands
curl -fsSL "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/latest/download/wt-new.md"  -o ".opencode/commands/wt-new.md"
curl -fsSL "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/latest/download/wt-clean.md" -o ".opencode/commands/wt-clean.md"
```

```sh
mkdir -p .opencode/commands
wget -qO ".opencode/commands/wt-new.md"  "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/latest/download/wt-new.md"
wget -qO ".opencode/commands/wt-clean.md" "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/latest/download/wt-clean.md"
```

Global install (latest release):

```sh
mkdir -p ~/.config/opencode/commands
curl -fsSL "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/latest/download/wt-new.md"  -o "$HOME/.config/opencode/commands/wt-new.md"
curl -fsSL "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/latest/download/wt-clean.md" -o "$HOME/.config/opencode/commands/wt-clean.md"
```

```sh
mkdir -p ~/.config/opencode/commands
wget -qO "$HOME/.config/opencode/commands/wt-new.md"  "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/latest/download/wt-new.md"
wget -qO "$HOME/.config/opencode/commands/wt-clean.md" "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/latest/download/wt-clean.md"
```

Pinned to a specific release tag:

```sh
VERSION=v0.1.0
mkdir -p .opencode/commands
curl -fsSL "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/download/${VERSION}/wt-new.md"  -o ".opencode/commands/wt-new.md"
curl -fsSL "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/download/${VERSION}/wt-clean.md" -o ".opencode/commands/wt-clean.md"
```

## Co-shipped skill

This repo also co-ships a `worktree-workflow` skill as a policy layer over the package capability.

- checked-in skill: `skills/worktree-workflow/SKILL.md`
- release asset: `SKILL.md`

The skill teaches when to use task-scoped worktrees, when repo root is still acceptable, and how to prefer the native tool path before falling back to the packaged CLI.

Project-local install (latest release):

```sh
mkdir -p .opencode/skills/worktree-workflow
curl -fsSL "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/latest/download/SKILL.md" -o ".opencode/skills/worktree-workflow/SKILL.md"
```

```sh
mkdir -p .opencode/skills/worktree-workflow
wget -qO ".opencode/skills/worktree-workflow/SKILL.md" "https://github.com/sven1103-agent/opencode-worktree-plugin/releases/latest/download/SKILL.md"
```

If your setup uses installed skill files, copy the released `SKILL.md` into a `worktree-workflow/` skill folder in the appropriate location for that environment, or consume the checked-in file from this repo directly.

## What the plugin provides

- `worktree_prepare`: create a worktree and matching branch from the latest configured base-branch commit, or the default branch when no base branch is configured
- `worktree_cleanup`: preview all connected worktrees against the configured base branch, auto-clean safe ones, and optionally remove selected review items

This package now ships the plugin capability, a CLI fallback surface, thin slash commands, and a co-shipped policy skill.

## Structured contract

The native tool results and CLI `--json` output now use a versioned structured contract with a `schema_version` field.

- current `schema_version`: `1.0.0`
- contract overview: `docs/contract.md`
- compatibility model: `docs/compatibility.md`
- checked-in schemas for transparency:
  - `schemas/worktree-prepare.result.schema.json`
  - `schemas/worktree-cleanup-preview.result.schema.json`
  - `schemas/worktree-cleanup-apply.result.schema.json`

Human-readable output remains available through the result `message`, but callers should depend on the structured fields rather than parsing prose.

## CLI fallback

The npm package also exposes a local CLI so agents can fall back to the same installed package when the native plugin tools are unavailable.

Examples:

```sh
npx opencode-worktree-workflow wt-new "Improve checkout retry logic"
npx opencode-worktree-workflow wt-new "Improve checkout retry logic" --json
npx opencode-worktree-workflow wt-clean preview
npx opencode-worktree-workflow wt-clean apply feature/foo --json
```

Defaults:

- human-readable output by default
- structured output with `--json`
- the CLI shares the same underlying implementation and result contract as the native tools
- the CLI fallback depends on the package already being installed in the project

## Compatibility model

The repo keeps config loading, argument normalization, and execution semantics centralized in the package implementation so existing installations continue to work across native tools, CLI fallback, and slash commands.

- compatibility overview: `docs/compatibility.md`
- existing `.opencode/worktree-workflow.json` setups remain the supported configuration path

## Optional project configuration

OpenCode's native `opencode.json` and `opencode.jsonc` files are schema-validated, so they can load this plugin through the standard `plugin` key but they cannot store a custom `worktreeWorkflow` block.

To override this plugin's defaults, put a sidecar config file at `.opencode/worktree-workflow.json`:

```json
{
  "branchPrefix": "wt/",
  "remote": "origin",
  "baseBranch": "release/v0.4.0",
  "worktreeRoot": ".worktrees/$REPO",
  "cleanupMode": "preview",
  "protectedBranches": ["release"]
}
```

Use `opencode.json` only to load the npm plugin itself:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "plugin": ["@sven1103/opencode-worktree-workflow"]
}
```

Supported settings:

- `branchPrefix`: prefix for generated worktree branches
- `remote`: remote used to detect the default branch and fetch updates
- `baseBranch`: optional branch name used as the creation and cleanup base; defaults to the remote's default branch
- `worktreeRoot`: destination root for new worktrees; supports `$REPO`, `$ROOT`, and `$ROOT_PARENT`
- `cleanupMode`: default cleanup behavior, either `preview` or `apply`
- `protectedBranches`: branches that should never be auto-cleaned

## Publish workflow

This repo is prepared for npm publishing from GitHub Actions using npm trusted publishing.

If you consume releases instead of contributing to the repo, `docs/releases.md` is the end-user guide for understanding what the published npm package and GitHub Release assets include.

Typical release flow:

1. Publish the package once manually to create it on npm.
2. Configure the package's trusted publisher on npm for `.github/workflows/publish.yml`.
3. Run the `Prepare Release` workflow from `main` with a version like `0.2.0`.

The release workflow creates a `release/v<version>` branch from `main`, updates `package.json` and `package-lock.json`, commits the version bump there, creates a matching `v<version>` tag, and pushes the branch and tag.

The release workflow then explicitly starts the publish workflow for that tag, which verifies the tag matches `package.json`, runs `npm publish` using OIDC without storing an `NPM_TOKEN` secret, and creates or updates the GitHub Release with generated release notes plus the command assets. Merge the release branch back to `main` afterward if you want the version bump recorded on the default branch.

## Local development

The repo still contains a project-local `.opencode/` setup for development and testing:

- `.opencode/plugins/worktree.js` re-exports the plugin from `src/index.js`
- `.opencode/commands/` contains local slash command wrappers for manual testing
- `.opencode/worktree-workflow.md` documents the local workflow

The publishable npm artifact is limited to `src/` via the root `package.json` `files` field. Install dependencies at the repo root with `npm install` for local development.
