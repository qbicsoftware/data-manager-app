---
name: worktree-workflow
description: Use this skill when you need to decide whether a task should move into a git worktree, when repo root is still safe, or when you need to choose between native worktree tools and the standard CLI fallback.
---

## When to use me

- Use this skill when work should be isolated from the current checkout.
- Use this skill when the task is substantial, risky, or likely to involve multiple collaborating agents.
- Use this skill when you need to decide whether repo root is still safe for a tiny edit.
- Use this skill when you need to choose the native worktree tools first and the packaged CLI fallback second.

## Goals

- Keep user work isolated from repo root when appropriate.
- Prefer task-scoped worktrees for non-trivial editable work.
- Support both native worktree tools and CLI fallback environments.
- Keep policy in the skill and execution semantics in the package.

## Root policy

- Treat repo root as shared space.
- Use repo root only for tiny, root-safe tasks.
- Treat a task as root-safe only when it is one focused change, touches at most one or two closely related files, does not need parallel delegation, does not imply a likely edit-test-fix loop, does not involve risky refactoring or migration work, and does not risk interfering with unrelated dirty root state.

## Task worktree policy

- Prefer one task-scoped worktree for non-trivial editable work.
- Treat a task worktree as belonging to a task or workstream, not to a single agent.
- Keep planning, implementation, and review for one linear task in the same task worktree unless the work splits.
- Create a separate divergent worktree only when concurrent branches of work may conflict or need independent experimentation.

## Capability ladder

- Use the native worktree tools as the primary path when the native worktree tools are available.
- Use the packaged CLI fallback path when the native tools are unavailable.
- Continue in repo root only for tiny, root-safe tasks when no worktree capability is available; otherwise stop and explain that isolation capability is unavailable.

## Creation behavior

- Use a short descriptive task title when creating a worktree.
- Treat the returned `worktree_path` as the active execution target for follow-up work.
- Use that worktree path as the working directory for later shell commands.
- Use paths inside that worktree for later file reads or edits.

## Cleanup behavior

- Keep cleanup preview-first by default because cleanup is preview-first unless deletion is clearly intended.
- Use cleanup apply only when deletion is clearly intended and controlled by the orchestrating runtime.
- Treat slash commands as manual human entry points, not as the canonical agent interface.

## Boundaries

- Do not encode runtime storage, session artifact, or orchestration file-layout details here.
- Do not duplicate package argument normalization or config parsing here.
- Rely on the shared package implementation for config loading, base-branch resolution, cleanup normalization, and structured result semantics.

## Examples

- Move a risky refactor into a task-scoped worktree before editing multiple files and running several test-fix loops.
- Stay in repo root for a tiny, root-safe doc fix that touches one related file and does not need delegation.
- Prefer the native worktree tools first, then switch to the packaged CLI fallback path if the native tools are unavailable.
