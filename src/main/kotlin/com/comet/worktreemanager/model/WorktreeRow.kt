package com.comet.worktreemanager.model

/**
 * A unified table row: either a worktree (with or without a branch) or a local
 * branch that has no worktree. Carries the branch's tracking status so the same
 * row shows ahead/behind even when no worktree is checked out for it.
 *
 * Kept free of IntelliJ types so the merge logic stays unit-testable;
 * [repositoryRoot] is a plain path used to resolve the owning repository.
 */
data class WorktreeRow(
    val branch: String?,
    val worktreePath: String?,
    val head: String?,
    val upstream: String?,
    val ahead: Int,
    val behind: Int,
    val isGone: Boolean,
    val isDetached: Boolean,
    val isBare: Boolean,
    val isLocked: Boolean,
    val isPrunable: Boolean,
    val isCurrent: Boolean,
    val repositoryRoot: String,
    /** Working-tree dirtiness; null when there is no worktree or it's unknown. */
    val workingTree: WorkingTreeStatus? = null,
) {
    val hasWorktree: Boolean get() = worktreePath != null
    val hasBranch: Boolean get() = branch != null

    /** Changes column: working-tree dirtiness, or em dash when not applicable. */
    val changesLabel: String
        get() = when {
            !hasWorktree -> "—"
            workingTree == null -> "—"
            else -> workingTree.shortLabel
        }

    /** Branch column. */
    val refLabel: String
        get() = when {
            isBare -> "(bare)"
            branch != null -> branch
            isDetached && head != null -> "(detached ${head.take(7)})"
            else -> "(detached)"
        }

    /** Worktree column: the path, or an em dash when the branch has none. */
    val worktreeLabel: String get() = worktreePath ?: "—"

    /** Tracking column: ↑ahead / ↓behind vs upstream, "gone", "✓", or em dash. */
    val trackingLabel: String
        get() = when {
            upstream == null -> "—"
            isGone -> "gone"
            else -> buildList {
                if (ahead > 0) add("↑$ahead")
                if (behind > 0) add("↓$behind")
            }.ifEmpty { listOf("✓") }.joinToString(" ")
        }

    /** Status column: state flags. */
    val statusLabel: String
        get() = buildList {
            if (isCurrent) add("current")
            if (!hasWorktree && hasBranch) add("no worktree")
            if (isLocked) add("locked")
            if (isPrunable) add("prunable")
        }.joinToString(", ")
}
