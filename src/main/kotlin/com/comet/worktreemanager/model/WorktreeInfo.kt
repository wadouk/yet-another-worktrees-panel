package com.comet.worktreemanager.model

/**
 * A single Git worktree as reported by `git worktree list --porcelain`.
 *
 * @param path absolute path of the worktree on disk
 * @param head the checked-out commit (full SHA), or null for a bare worktree
 * @param branch short branch name (e.g. `feature/x`), or null when detached/bare
 * @param isDetached true when HEAD is detached (no branch)
 * @param isBare true for the bare repository entry
 * @param isLocked true when the worktree is locked (`git worktree lock`)
 * @param isPrunable true when Git reports the worktree as prunable (gone)
 * @param isCurrent true when this is the worktree currently open in the IDE
 */
data class WorktreeInfo(
    val path: String,
    val head: String?,
    val branch: String?,
    val isDetached: Boolean,
    val isBare: Boolean,
    val isLocked: Boolean,
    val isPrunable: Boolean,
    val isCurrent: Boolean,
) {
    /** Human-readable ref label for the branch column. */
    val refLabel: String
        get() = when {
            isBare -> "(bare)"
            branch != null -> branch
            isDetached && head != null -> "(detached ${head.take(7)})"
            else -> "(detached)"
        }

    /** Short comma-separated status flags for the status column. */
    val statusLabel: String
        get() = buildList {
            if (isCurrent) add("current")
            if (isLocked) add("locked")
            if (isPrunable) add("prunable")
        }.joinToString(", ")
}
