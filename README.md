# Yet Another Worktrees Panel

An IntelliJ Platform plugin to manage your Git worktrees from a **"Worktrees" tab** inside the
Version Control tool window (the existing Git tool window). The UI is localized in **English and
French**.

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
- **Cleanup** column: a derived hint — `obsolete` (merged into the default branch **and**
  clean) or `likely obsolete` (upstream gone **and** clean) — to spot what's safe to delete.
  (This is unrelated to `git worktree prune`, which only removes metadata for missing dirs.)
- Worktree paths are shown **relative to the main worktree** (full path on hover).
- The **current** worktree's branch is shown in **bold with a yellow `HEAD` tag**.
- **Show in Git Log** (right-click) jumps to the branch in the commit graph.
- **Sort** by any column and **filter** the table live with the search field.
- **Open** a worktree in a new IDE window (toolbar action or double-click).
- **Delete** adaptively, depending on what exists for the selected row:
  - worktree + branch → remove the worktree, optionally also delete the branch;
  - branch only → delete the branch;
  - detached worktree → remove the worktree.
- **Prune** worktree metadata for directories that no longer exist (with a confirmation dialog).

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

This is a **dynamic plugin**: it only uses dynamic extension points (the `changesViewContent`
tab) and disposes its resources cleanly, so it loads, updates, and unloads **without restarting
the IDE**.

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
├── i18n/
│   └── WorktreeBundle.kt              # DynamicBundle (messages.WorktreeBundle)
├── model/                            # pure data only — no i18n, no clock
│   ├── WorktreeInfo.kt               # raw `git worktree list` entry
│   ├── WorktreeRow.kt                # unified worktree-or-branch row (raw fields)
│   └── WorkingTreeStatus.kt          # staged/modified/untracked/conflicted counts
├── service/
│   ├── WorktreeParser.kt             # pure parser for `git worktree list --porcelain`
│   ├── BranchRefParser.kt            # pure parser for `git for-each-ref` (tracking + date)
│   ├── GitStatusParser.kt            # pure parser for `git status --porcelain`
│   ├── WorktreeRowBuilder.kt         # pure merge of worktrees + branches into rows
│   ├── ActivityResolver.kt           # pure: pick file-change vs commit timestamp
│   ├── DefaultBranchResolver.kt      # pure: resolve the default branch
│   ├── GitWorktreeCommand.kt         # reflective `worktree` GitCommand
│   └── WorktreeService.kt            # git4idea-backed operations
└── toolwindow/                       # presentation: i18n + current clock live here
    ├── WorktreePruningContentProvider.kt  # VCS-tab provider (+ tab name & visibility)
    ├── WorktreePanel.kt              # toolbar + filter + sortable table
    ├── WorktreeRowPresenter.kt       # builds translated cell labels + tooltips
    ├── WorktreeTableModel.kt         # 7-column table model
    ├── RelativeAge.kt                # pure relative-age bucket (now injected)
    ├── RelativeTimeCell.kt           # localizes the bucket; sorts by timestamp
    └── DeleteDialog.kt               # adaptive delete confirmation (worktree/branch)

src/main/resources/messages/
├── WorktreeBundle.properties         # English (base)
└── WorktreeBundle_fr.properties      # French (« Élagage »)
```
