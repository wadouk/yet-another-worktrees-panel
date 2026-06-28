# Worktree Manager

An IntelliJ Platform plugin to manage your Git worktrees from a dedicated tool window.

## Features

- **List** every worktree of the project's repositories **and** every local branch
  that has no worktree of its own — each row shows its upstream tracking status
  (↑ahead / ↓behind, `gone`, `✓`).
- **Merged** column: whether each branch is merged into the repo's default branch
  (`merged` / `unmerged` / `default`), resolved via `origin/HEAD` (fallback main/master).
- **Working-tree status** for each existing worktree (`clean`, or `+staged ~modified
  ?untracked !conflicts`) from `git status --porcelain`.
- **Activity** column (relative, e.g. `3 days ago`): the most recent uncommitted file
  change for a dirty worktree, otherwise the last commit. Sorts chronologically.
- **Show in Git Log** (right-click) jumps to the branch in the commit graph.
- **Sort** by any column and **filter** the table live with the search field
  (matches branch / path / status, case-insensitive).
- **Open** a worktree in a new IDE window (toolbar action or double-click).
- **Delete** adaptively, depending on what exists for the selected row:
  - worktree + branch → remove the worktree, optionally also delete the branch;
  - branch only → delete the branch;
  - detached worktree → remove the worktree.
- **Prune** worktree metadata for directories that no longer exist.

Guard rails: the worktree currently open can't be deleted, the bare entry is
protected, and the destructive `--force` / `-D` is always opt-in.

The plugin drives the bundled Git integration (`git4idea`), so it uses the same
Git executable IntelliJ is configured with.

## Requirements

- IntelliJ IDEA 2024.3+ (build 243+), Community or Ultimate.
- The bundled **Git** plugin enabled.

## Build & run

This project ships a Gradle wrapper, so no local Gradle install is needed.

```bash
./gradlew build          # compile + run tests
./gradlew runIde         # launch a sandbox IDE with the plugin installed
./gradlew buildPlugin    # produce build/distributions/worktree-manager-<version>.zip
./gradlew verifyPlugin   # run the JetBrains Plugin Verifier (compat + dynamic check)
```

Install the built zip via *Settings → Plugins → ⚙ → Install Plugin from Disk…*.

## No IDE restart needed

This is a **dynamic plugin**: it only uses dynamic extension points (a tool window) and
disposes its resources cleanly, so it loads, updates, and unloads **without restarting the
IDE**.

- **Installing / updating**: *Install Plugin from Disk* loads it immediately — no mandatory
  restart.
- **Dev hot-reload**: keep `./gradlew runIde` running and, in another terminal, run
  `./gradlew buildPlugin` (or `prepareSandbox`). The sandbox auto-reloads the plugin live
  (`idea.auto.reload.plugins=true` is set by `runIde`) — no restart.
- Note: the `Restart not supported; exiting` line you may see from `runIde` is just the **dev
  sandbox process exiting** (its IntelliJ home isn't a real app bundle); it is **not** an
  install-time restart requirement.

## Project layout

```
src/main/kotlin/com/comet/worktreemanager/
├── model/
│   ├── WorktreeInfo.kt                # raw `git worktree list` entry
│   └── WorktreeRow.kt                 # unified worktree-or-branch table row
├── service/
│   ├── WorktreeParser.kt             # pure parser for `git worktree list --porcelain`
│   ├── BranchRefParser.kt            # pure parser for `git for-each-ref` (tracking)
│   ├── WorktreeRowBuilder.kt         # pure merge of worktrees + branches into rows
│   ├── GitWorktreeCommand.kt         # reflective `worktree` GitCommand
│   └── WorktreeService.kt            # git4idea-backed operations
└── toolwindow/
    ├── WorktreeToolWindowFactory.kt   # registers the tool window
    ├── WorktreePanel.kt               # toolbar + filter + sortable table
    ├── WorktreeTableModel.kt          # 4-column table model
    └── DeleteDialog.kt                # adaptive delete confirmation (worktree/branch)
```
