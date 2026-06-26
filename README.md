# Worktree Manager

An IntelliJ Platform plugin to manage your Git worktrees from a dedicated tool window.

## Features

- **List** every worktree of the project's repositories (branch, path, status).
- **Open** a worktree in a new IDE window (toolbar action or double-click).
- **Remove / prune** a worktree, optionally deleting its branch — with guard rails
  (you can't remove the worktree currently open, force is opt-in, branch deletion is opt-in).

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
```

Install the built zip via *Settings → Plugins → ⚙ → Install Plugin from Disk…*.

## Project layout

```
src/main/kotlin/com/comet/worktreemanager/
├── model/WorktreeInfo.kt              # data model
├── service/
│   ├── WorktreeParser.kt              # pure parser for `git worktree list --porcelain`
│   └── WorktreeService.kt             # git4idea-backed operations (list/remove/prune)
└── toolwindow/
    ├── WorktreeToolWindowFactory.kt   # registers the tool window
    ├── WorktreePanel.kt               # toolbar + table UI
    ├── WorktreeTableModel.kt          # table model
    └── RemoveWorktreeDialog.kt        # remove confirmation with guard rails
```
