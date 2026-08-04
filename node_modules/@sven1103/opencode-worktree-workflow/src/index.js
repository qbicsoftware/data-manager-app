import fs from "node:fs/promises";
import path from "node:path";

import { parse } from "jsonc-parser";
import { tool } from "@opencode-ai/plugin";

const DEFAULTS = {
  branchPrefix: "wt/",
  remote: "origin",
  baseBranch: null,
  worktreeRoot: ".worktrees/$REPO",
  cleanupMode: "preview",
  protectedBranches: [],
};

const RESULT_SCHEMA_VERSION = "1.0.0";

async function pathExists(targetPath) {
  try {
    await fs.access(targetPath);
    return true;
  } catch {
    return false;
  }
}

function isMissingGitRepositoryError(message) {
  return /not a git repository/i.test(message);
}

function isMissingRemoteError(message, remote) {
  return new RegExp(`No such remote:?\s+${remote}|does not appear to be a git repository|Could not read from remote repository`, "i").test(message);
}

async function readJsonFile(filePath) {
  if (!(await pathExists(filePath))) {
    return null;
  }

  const source = await fs.readFile(filePath, "utf8");
  const data = parse(source);
  return data && typeof data === "object" ? data : null;
}

function normalizeBranchPrefix(prefix) {
  if (!prefix) {
    return "";
  }

  return prefix.endsWith("/") ? prefix : `${prefix}/`;
}

function slugifyTitle(title) {
  return title
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .replace(/-{2,}/g, "-");
}

function formatRootTemplate(template, repoRoot) {
  const repoName = path.basename(repoRoot);
  return template
    .replaceAll("$REPO", repoName)
    .replaceAll("$ROOT", repoRoot)
    .replaceAll("$ROOT_PARENT", path.dirname(repoRoot));
}

function parseShortBranch(branchRef) {
  const prefix = "refs/heads/";
  return branchRef.startsWith(prefix) ? branchRef.slice(prefix.length) : branchRef;
}

function parseWorktreeList(output) {
  const entries = [];
  let current = null;

  for (const rawLine of output.split("\n")) {
    const line = rawLine.trim();
    if (!line) {
      if (current) {
        entries.push(current);
        current = null;
      }
      continue;
    }

    if (line.startsWith("worktree ")) {
      if (current) {
        entries.push(current);
      }
      current = { path: line.slice("worktree ".length) };
      continue;
    }

    if (!current) {
      continue;
    }

    if (line.startsWith("HEAD ")) {
      current.head = line.slice("HEAD ".length);
      continue;
    }

    if (line.startsWith("branch ")) {
      current.branchRef = line.slice("branch ".length);
      current.branch = parseShortBranch(current.branchRef);
      continue;
    }

    if (line === "detached") {
      current.detached = true;
    }
  }

  if (current) {
    entries.push(current);
  }

  return entries;
}

function shellQuote(value) {
  return `'${String(value).replaceAll("'", `'"'"'`)}'`;
}

function formatWorktreeSummary(item) {
  return `${item.branch || "(detached)"} -> ${item.path}${item.head ? ` (${item.head.slice(0, 12)})` : ""}`;
}

function formatCopyPasteCommands(item) {
  const selector = item.branch || item.path;
  const branchFlag = item.status === "safe" ? "-d" : "-D";

  return [
    `  copy: /wt-clean apply ${selector}`,
    `  git:  git worktree remove ${shellQuote(item.path)} && git branch ${branchFlag} ${shellQuote(item.branch)}`,
  ];
}

function formatPreviewSection(title, items, { includeCommands = false } = {}) {
  if (items.length === 0) {
    return [title, "- none"];
  }

  const lines = [title];

  for (const item of items) {
    lines.push(`- ${formatWorktreeSummary(item)}: ${item.reason}`);

    if (includeCommands && item.branch) {
      lines.push(...formatCopyPasteCommands(item));
    }
  }

  return lines;
}

function formatPreview(grouped, defaultBranch) {
  return [
    `Worktrees connected to this repository against ${defaultBranch}:`,
    "",
    ...formatPreviewSection("Safe to clean automatically:", grouped.safe, { includeCommands: true }),
    "",
    ...formatPreviewSection("Needs review before cleanup:", grouped.review, { includeCommands: true }),
    "",
    ...formatPreviewSection("Not cleanable here:", grouped.blocked),
    "",
    "Run `/wt-clean apply` to remove only the safe group.",
    "Run `/wt-clean apply <branch-or-path>` to also remove selected review items.",
  ].join("\n");
}

function formatPrepareSummary(result) {
  return [
    `Created worktree for "${result.title}".`,
    `- branch: ${result.branch}`,
    `- worktree: ${result.worktree_path}`,
    `- default branch: ${result.default_branch}`,
    `- base branch: ${result.base_branch}`,
    `- base ref: ${result.base_ref}`,
    `- base commit: ${result.base_commit}`,
  ].join("\n");
}

function formatCleanupSummary(defaultBranch, removed, failed, requestedSelectors) {
  const lines = [`Cleaned worktrees relative to ${defaultBranch}:`];

  if (removed.length === 0) {
    lines.push("- none removed");
  } else {
    for (const item of removed) {
      const modeLabel = item.selected ? "selected" : "auto";
      lines.push(`- removed (${modeLabel}) ${item.branch} -> ${item.path}`);
    }
  }

  if (requestedSelectors.length > 0) {
    lines.push("");
    lines.push("Requested selectors:");
    for (const selector of requestedSelectors) {
      lines.push(`- ${selector}`);
    }
  }

  if (failed.length > 0) {
    lines.push("");
    lines.push("Cleanup skipped for:");
    for (const item of failed) {
      lines.push(`- ${item.branch || item.selector} -> ${item.path || "(no path)"}: ${item.reason}`);
    }
  }

  return lines.join("\n");
}

function toStructuredCleanupItem(item) {
  return {
    branch: item.branch ?? null,
    worktree_path: item.path ?? item.worktree_path ?? null,
    head: item.head ?? null,
    status: item.status ?? null,
    reason: item.reason ?? null,
    detached: Boolean(item.detached),
    selectable: typeof item.selectable === "boolean" ? item.selectable : null,
  };
}

function toStructuredCleanupFailure(item) {
  return {
    selector: item.selector ?? null,
    branch: item.branch ?? null,
    worktree_path: item.path ?? item.worktree_path ?? null,
    head: item.head ?? null,
    status: item.status ?? null,
    reason: item.reason ?? null,
    detached: Boolean(item.detached),
    selectable: typeof item.selectable === "boolean" ? item.selectable : null,
  };
}

function buildPrepareResult({ title, branch, worktreePath, defaultBranch, baseBranch, baseRef, baseCommit }) {
  const result = {
    schema_version: RESULT_SCHEMA_VERSION,
    ok: true,
    title,
    branch,
    worktree_path: worktreePath,
    default_branch: defaultBranch,
    base_branch: baseBranch,
    base_ref: baseRef,
    base_commit: baseCommit,
    created: true,
  };

  return {
    ...result,
    message: formatPrepareSummary(result),
  };
}

function buildCleanupPreviewResult({ defaultBranch, baseBranch, baseRef, grouped }) {
  const structuredGroups = {
    safe: grouped.safe.map(toStructuredCleanupItem),
    review: grouped.review.map(toStructuredCleanupItem),
    blocked: grouped.blocked.map(toStructuredCleanupItem),
  };

  return {
    schema_version: RESULT_SCHEMA_VERSION,
    ok: true,
    mode: "preview",
    default_branch: defaultBranch,
    base_branch: baseBranch,
    base_ref: baseRef,
    groups: structuredGroups,
    message: formatPreview(grouped, baseBranch),
  };
}

function buildCleanupApplyResult({ defaultBranch, baseBranch, baseRef, removed, failed, requestedSelectors }) {
  return {
    schema_version: RESULT_SCHEMA_VERSION,
    ok: true,
    mode: "apply",
    default_branch: defaultBranch,
    base_branch: baseBranch,
    base_ref: baseRef,
    requested_selectors: requestedSelectors,
    removed: removed.map((item) => ({
      ...toStructuredCleanupItem(item),
      selected: Boolean(item.selected),
    })),
    failed: failed.map(toStructuredCleanupFailure),
    message: formatCleanupSummary(baseBranch, removed, failed, requestedSelectors),
  };
}

function splitCleanupToken(value) {
  if (typeof value !== "string") {
    return [];
  }

  return value
    .trim()
    .split(/\s+/)
    .filter(Boolean);
}

function parseCleanupRawArguments(raw) {
  const tokens = splitCleanupToken(raw);

  if (tokens[0] === "apply") {
    return {
      mode: "apply",
      selectors: tokens.slice(1),
    };
  }

  if (tokens[0] === "preview") {
    return {
      mode: "preview",
      selectors: tokens.slice(1),
    };
  }

  return {
    mode: null,
    selectors: tokens,
  };
}

function normalizeCleanupArgs(args, config) {
  const selectors = Array.isArray(args.selectors) ? [...args.selectors] : [];
  const normalizedSelectors = [];
  const rawArgs = parseCleanupRawArguments(args.raw);
  let explicitMode = rawArgs.mode;

  if (rawArgs.selectors.length > 0) {
    selectors.unshift(...rawArgs.selectors);
  }

  if (typeof args.mode === "string" && args.mode.trim()) {
    const modeValue = args.mode.trim();
    if (modeValue === "apply" || modeValue === "preview") {
      explicitMode = modeValue;
    } else {
      selectors.unshift(...splitCleanupToken(modeValue));
    }
  }

  for (const selector of selectors) {
    if (typeof selector !== "string") {
      continue;
    }

    if (selector.includes(" ")) {
      normalizedSelectors.push(...splitCleanupToken(selector));
      continue;
    }

    normalizedSelectors.push(selector);
  }

  const inlineApply = normalizedSelectors[0] === "apply";

  if (inlineApply) {
    normalizedSelectors.shift();
  }

  const mode = explicitMode === "apply" || inlineApply ? "apply" : explicitMode || config.cleanupMode;

  return {
    mode,
    selectors: normalizedSelectors,
  };
}

export const __internal = {
  RESULT_SCHEMA_VERSION,
  buildCleanupApplyResult,
  buildCleanupPreviewResult,
  buildPrepareResult,
  classifyEntry,
  isMissingGitRepositoryError,
  isMissingRemoteError,
  parseCleanupRawArguments,
  normalizeCleanupArgs,
  toStructuredCleanupFailure,
  toStructuredCleanupItem,
};

function selectorMatches(item, selector) {
  const normalized = path.resolve(selector);
  return item.branch === selector || item.path === normalized;
}

function classifyEntry(entry, repoRoot, activeWorktree, protectedBranches, mergedIntoBase) {
  const entryPath = path.resolve(entry.path);
  const branchName = entry.branch;
  const item = {
    branch: branchName,
    path: entryPath,
    head: entry.head,
    detached: Boolean(entry.detached),
  };

  if (!branchName || entry.detached) {
    return {
      ...item,
      status: "blocked",
      reason: !branchName ? "no branch" : "detached HEAD",
      selectable: false,
    };
  }

  if (entryPath === path.resolve(repoRoot)) {
    return {
      ...item,
      status: "blocked",
      reason: entryPath === activeWorktree ? "repository root, current worktree, protected branch" : "repository root",
      selectable: false,
    };
  }

  if (entryPath === activeWorktree) {
    return {
      ...item,
      status: "blocked",
      reason: "current worktree",
      selectable: false,
    };
  }

  if (protectedBranches.has(branchName)) {
    return {
      ...item,
      status: "blocked",
      reason: "protected branch",
      selectable: false,
    };
  }

  if (mergedIntoBase) {
    return {
      ...item,
      status: "safe",
      reason: "merged into base branch by git ancestry",
      selectable: true,
    };
  }

  return {
    ...item,
    status: "review",
    reason: "not merged into base branch by git ancestry",
    selectable: true,
  };
}

export const WorktreeWorkflowPlugin = async ({ $, directory }) => {
  async function git(args, options = {}) {
    const cwd = options.cwd ?? directory;
    const command = `git ${args.map((arg) => $.escape(String(arg))).join(" ")}`;
    const result = await $`${{ raw: command }}`.cwd(cwd).quiet().nothrow();
    const stdout = result.text().trim();
    const stderr = result.stderr.toString("utf8").trim();

    if (!options.allowFailure && result.exitCode !== 0) {
      throw new Error(stderr || stdout || `Git command failed: ${command}`);
    }

    return {
      stdout,
      stderr,
      exitCode: result.exitCode,
    };
  }

  async function getRepoRoot() {
    try {
      const result = await git(["rev-parse", "--show-toplevel"]);
      return result.stdout;
    } catch (error) {
      if (isMissingGitRepositoryError(error.message || "")) {
        throw new Error(
          "This command must run inside a git repository. Initialize a repository first or run it from an existing repo root.",
        );
      }

      throw error;
    }
  }

  async function loadWorkflowConfig(repoRoot) {
    const [projectConfig, projectConfigC, sidecarConfig] = await Promise.all([
      readJsonFile(path.join(repoRoot, "opencode.json")),
      readJsonFile(path.join(repoRoot, "opencode.jsonc")),
      readJsonFile(path.join(repoRoot, ".opencode", "worktree-workflow.json")),
    ]);

    const merged = {
      ...DEFAULTS,
      ...(projectConfig?.worktreeWorkflow ?? {}),
      ...(projectConfigC?.worktreeWorkflow ?? {}),
      ...(sidecarConfig ?? {}),
    };

    return {
      branchPrefix: normalizeBranchPrefix(merged.branchPrefix ?? DEFAULTS.branchPrefix),
      remote: merged.remote || DEFAULTS.remote,
      baseBranch: typeof merged.baseBranch === "string" && merged.baseBranch.trim() ? merged.baseBranch.trim() : null,
      cleanupMode: merged.cleanupMode === "apply" ? "apply" : DEFAULTS.cleanupMode,
      protectedBranches: Array.isArray(merged.protectedBranches)
        ? merged.protectedBranches.filter((value) => typeof value === "string")
        : [],
      worktreeRoot: path.resolve(repoRoot, formatRootTemplate(merged.worktreeRoot || DEFAULTS.worktreeRoot, repoRoot)),
    };
  }

  async function getDefaultBranch(repoRoot, remote) {
    const remoteHead = await git(
      ["symbolic-ref", "--quiet", "--short", `refs/remotes/${remote}/HEAD`],
      { cwd: repoRoot, allowFailure: true },
    );

    if (remoteHead.exitCode === 0 && remoteHead.stdout.startsWith(`${remote}/`)) {
      return remoteHead.stdout.slice(remote.length + 1);
    }

    const remoteShow = await git(["remote", "show", remote], {
      cwd: repoRoot,
      allowFailure: true,
    });

    if (remoteShow.exitCode === 0) {
      const match = remoteShow.stdout.match(/HEAD branch: (.+)/);
      if (match?.[1]) {
        return match[1].trim();
      }
    }

    for (const candidate of ["main", "master", "trunk", "develop"]) {
      const hasLocal = await git(["show-ref", "--verify", "--quiet", `refs/heads/${candidate}`], {
        cwd: repoRoot,
        allowFailure: true,
      });
      if (hasLocal.exitCode === 0) {
        return candidate;
      }

      const hasRemote = await git(["show-ref", "--verify", "--quiet", `refs/remotes/${remote}/${candidate}`], {
        cwd: repoRoot,
        allowFailure: true,
      });
      if (hasRemote.exitCode === 0) {
        return candidate;
      }
    }

    const currentBranch = await git(["branch", "--show-current"], {
      cwd: repoRoot,
      allowFailure: true,
    });

    if (currentBranch.stdout) {
      return currentBranch.stdout;
    }

    throw new Error("Could not determine the default branch for this repository.");
  }

  async function resolveBaseBranch(repoRoot, remote, configuredBaseBranch) {
    return configuredBaseBranch || getDefaultBranch(repoRoot, remote);
  }

  async function getBaseRef(repoRoot, remote, baseBranch) {
    try {
      await git(["fetch", "--prune", remote, baseBranch], { cwd: repoRoot });
    } catch (error) {
      if (isMissingRemoteError(error.message || "", remote)) {
        throw new Error(
          `Could not fetch base branch information from remote \"${remote}\". Configure the expected remote in .opencode/worktree-workflow.json or add that remote to this repository.`,
        );
      }

      throw error;
    }

    const remoteRef = `refs/remotes/${remote}/${baseBranch}`;
    const remoteExists = await git(["show-ref", "--verify", "--quiet", remoteRef], {
      cwd: repoRoot,
      allowFailure: true,
    });

    return remoteExists.exitCode === 0 ? `${remote}/${baseBranch}` : baseBranch;
  }

  async function resolveBaseTarget(repoRoot, config) {
    const defaultBranch = await getDefaultBranch(repoRoot, config.remote);
    const baseBranch = await resolveBaseBranch(repoRoot, config.remote, config.baseBranch);
    const baseRef = await getBaseRef(repoRoot, config.remote, baseBranch);

    return {
      defaultBranch,
      baseBranch,
      baseRef,
    };
  }

  async function ensureBranchDoesNotExist(repoRoot, branchName) {
    const exists = await git(["show-ref", "--verify", "--quiet", `refs/heads/${branchName}`], {
      cwd: repoRoot,
      allowFailure: true,
    });

    if (exists.exitCode === 0) {
      throw new Error(`Local branch already exists: ${branchName}`);
    }
  }

  return {
    tool: {
      worktree_prepare: tool({
        description: "Create a synced git worktree from a descriptive title",
        args: {
          title: tool.schema.string().min(3).describe("Descriptive working title for the new worktree"),
        },
        async execute(args, context) {
          context.metadata({ title: `Create worktree: ${args.title}` });

          const repoRoot = await getRepoRoot();
          const config = await loadWorkflowConfig(repoRoot);
          const { defaultBranch, baseBranch, baseRef } = await resolveBaseTarget(repoRoot, config);
          const baseCommit = (await git(["rev-parse", baseRef], { cwd: repoRoot })).stdout;
          const slug = slugifyTitle(args.title);

          if (!slug) {
            throw new Error("Could not derive a branch name from the provided title.");
          }

          const branchName = `${config.branchPrefix}${slug}`;
          const worktreePath = path.join(config.worktreeRoot, slug);

          await ensureBranchDoesNotExist(repoRoot, branchName);

          if (await pathExists(worktreePath)) {
            throw new Error(`Worktree path already exists: ${worktreePath}`);
          }

          await fs.mkdir(config.worktreeRoot, { recursive: true });
          await git(["worktree", "add", "-b", branchName, worktreePath, baseRef], {
            cwd: repoRoot,
          });

          const branchCommit = (await git(["rev-parse", branchName], { cwd: repoRoot })).stdout;
          if (branchCommit !== baseCommit) {
            throw new Error(
              `New branch ${branchName} does not match ${baseBranch} at ${baseCommit}. Found ${branchCommit} instead.`,
            );
          }

          return buildPrepareResult({
            title: args.title,
            branch: branchName,
            worktreePath,
            defaultBranch,
            baseBranch,
            baseRef,
            baseCommit,
          });
        },
      }),
      worktree_cleanup: tool({
        description: "Preview or clean git worktrees",
        args: {
          mode: tool.schema.string().optional().describe("Preview cleanup candidates or remove them"),
          raw: tool.schema.string().optional().describe("Raw cleanup arguments from slash commands"),
          selectors: tool.schema
            .array(tool.schema.string())
            .default([])
            .describe("Optional branch names or worktree paths to remove explicitly"),
        },
        async execute(args, context) {
          const repoRoot = await getRepoRoot();
          const config = await loadWorkflowConfig(repoRoot);
          const normalizedArgs = normalizeCleanupArgs(args, config);

          context.metadata({ title: `Clean worktrees (${normalizedArgs.mode})` });

          const { defaultBranch, baseBranch, baseRef } = await resolveBaseTarget(repoRoot, config);
          const activeWorktree = path.resolve(context.worktree || repoRoot);
          const worktreeList = await git(["worktree", "list", "--porcelain"], { cwd: repoRoot });
          const entries = parseWorktreeList(worktreeList.stdout);
          const protectedBranches = new Set([defaultBranch, baseBranch, ...config.protectedBranches]);
          const grouped = {
            safe: [],
            review: [],
            blocked: [],
          };

          for (const entry of entries) {
            const branchName = entry.branch;
            let mergedIntoBase = false;

            if (branchName && !entry.detached) {
              const merged = await git(["merge-base", "--is-ancestor", branchName, baseRef], {
                cwd: repoRoot,
                allowFailure: true,
              });

              mergedIntoBase = merged.exitCode === 0;
            }

            const classified = classifyEntry(
              entry,
              repoRoot,
              activeWorktree,
              protectedBranches,
              mergedIntoBase,
            );

            grouped[classified.status].push(classified);
          }

          if (normalizedArgs.mode !== "apply") {
            return buildCleanupPreviewResult({
              defaultBranch,
              baseBranch,
              baseRef,
              grouped,
            });
          }

          const requestedSelectors = [...new Set(normalizedArgs.selectors || [])];
          const selected = [];
          const failed = [];

          for (const selector of requestedSelectors) {
            const match = [...grouped.safe, ...grouped.review, ...grouped.blocked].find((item) =>
              selectorMatches(item, selector),
            );

            if (!match) {
              failed.push({
                selector,
                reason: "selector did not match any connected worktree",
              });
              continue;
            }

            if (!match.selectable) {
              failed.push({
                ...match,
                selector,
                reason: `cannot remove via selector: ${match.reason}`,
              });
              continue;
            }

            selected.push({
              ...match,
              selector,
            });
          }

          const targets = [...grouped.safe];

          for (const item of selected) {
            if (!targets.some((target) => target.path === item.path)) {
              targets.push({
                ...item,
                selector: item.selector ?? null,
                selected: true,
              });
            }
          }

          const removed = [];

          for (const candidate of targets) {
            const removeWorktree = await git(["worktree", "remove", candidate.path], {
              cwd: repoRoot,
              allowFailure: true,
            });

            if (removeWorktree.exitCode !== 0) {
              failed.push({
                ...candidate,
                reason: removeWorktree.stderr || removeWorktree.stdout || "worktree remove failed",
              });
              continue;
            }

            const deleteBranch = await git(
              ["branch", candidate.status === "safe" ? "-d" : "-D", candidate.branch],
              {
                cwd: repoRoot,
                allowFailure: true,
              },
            );

            if (deleteBranch.exitCode !== 0) {
              failed.push({
                ...candidate,
                reason: deleteBranch.stderr || deleteBranch.stdout || "branch delete failed",
              });
              continue;
            }

            removed.push(candidate);
          }

          await git(["worktree", "prune"], {
            cwd: repoRoot,
            allowFailure: true,
          });

          return buildCleanupApplyResult({
            defaultBranch,
            baseBranch,
            baseRef,
            removed,
            failed,
            requestedSelectors,
          });
        },
      }),
    },
  };
};

export default WorktreeWorkflowPlugin;
