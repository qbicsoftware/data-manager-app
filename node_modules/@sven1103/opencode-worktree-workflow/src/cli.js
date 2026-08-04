#!/usr/bin/env node

import { execFile } from "node:child_process";
import fs from "node:fs";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

import { WorktreeWorkflowPlugin } from "./index.js";

const execFileAsync = promisify(execFile);

function shellEscape(value) {
  return `'${String(value).replaceAll("'", `'"'"'`)}'`;
}

function createShell(cwdBase) {
  const shell = (strings, ...values) => {
    const [firstValue] = values;
    const raw = firstValue?.raw || strings.raw?.join("") || strings.join("");
    let cwd = cwdBase;

    return {
      cwd(nextCwd) {
        cwd = nextCwd;
        return this;
      },
      quiet() {
        return this;
      },
      async nothrow() {
        try {
          const result = await execFileAsync("sh", ["-lc", raw], { cwd });
          return {
            text() {
              return result.stdout;
            },
            stderr: Buffer.from(result.stderr),
            exitCode: 0,
          };
        } catch (error) {
          return {
            text() {
              return error.stdout || "";
            },
            stderr: Buffer.from(error.stderr || ""),
            exitCode: typeof error.code === "number" ? error.code : 1,
          };
        }
      },
    };
  };

  shell.escape = shellEscape;
  return shell;
}

function printUsage() {
  process.stdout.write(
    [
      "Usage:",
      "  opencode-worktree-workflow wt-new <title> [--json]",
      "  opencode-worktree-workflow wt-clean [preview|apply] [selectors...] [--json]",
      "",
      "Examples:",
      "  opencode-worktree-workflow wt-new \"Improve checkout retry logic\"",
      "  opencode-worktree-workflow wt-clean preview",
      "  opencode-worktree-workflow wt-clean apply feature/foo",
    ].join("\n") + "\n",
  );
}

function printSubcommandUsage(command) {
  if (command === "wt-new") {
    process.stdout.write(
      [
        "Usage:",
        "  opencode-worktree-workflow wt-new <title> [--json]",
        "",
        "Create a synced worktree and branch from the configured base branch.",
      ].join("\n") + "\n",
    );
    return;
  }

  if (command === "wt-clean") {
    process.stdout.write(
      [
        "Usage:",
        "  opencode-worktree-workflow wt-clean [preview|apply] [selectors...] [--json]",
        "",
        "Preview connected worktrees or remove safe and explicitly selected review worktrees.",
      ].join("\n") + "\n",
    );
  }
}

export function parseCliArgs(argv) {
  const outputJson = argv.includes("--json");
  const args = argv.filter((arg) => arg !== "--json");
  return { outputJson, args };
}

export async function run(argv = process.argv.slice(2)) {
  const { outputJson, args } = parseCliArgs(argv);
  const [command, ...rest] = args;

  if (!command || command === "--help" || command === "-h" || command === "help") {
    printUsage();
    process.exitCode = command ? 0 : 1;
    return;
  }

  if ((command === "wt-new" || command === "wt-clean") && rest.some((arg) => arg === "--help" || arg === "-h" || arg === "help")) {
    printSubcommandUsage(command);
    return;
  }

  const plugin = await WorktreeWorkflowPlugin({
    $: createShell(process.cwd()),
    directory: process.cwd(),
  });

  let result;

  if (command === "wt-new") {
    const title = rest.join(" ").trim();

    if (!title) {
      throw new Error("wt-new requires a descriptive title.");
    }

    result = await plugin.tool.worktree_prepare.execute(
      { title },
      { metadata() {}, worktree: process.cwd() },
    );
  } else if (command === "wt-clean") {
    const raw = rest.join(" ").trim();
    result = await plugin.tool.worktree_cleanup.execute(
      { raw, selectors: [] },
      { metadata() {}, worktree: process.cwd() },
    );
  } else {
    throw new Error(`Unknown command: ${command}`);
  }

  if (outputJson) {
    process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
    return;
  }

  process.stdout.write(`${result.message || JSON.stringify(result, null, 2)}\n`);
}

export function isInvokedAsScript(argvPath = process.argv[1]) {
  if (!argvPath) {
    return false;
  }

  try {
    return fs.realpathSync(argvPath) === fileURLToPath(import.meta.url);
  } catch {
    return fileURLToPath(import.meta.url) === argvPath;
  }
}

const invokedAsScript = isInvokedAsScript();

if (invokedAsScript) {
  run().catch((error) => {
    process.stderr.write(`${error.message || String(error)}\n`);
    process.exitCode = 1;
  });
}
