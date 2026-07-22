package me.heloworld.worktreemanager.service

import me.heloworld.worktreemanager.model.WorktreeRow

/**
 * Cleanup suggestion for a row, surfaced in the Cleanup column. This is about
 * whether a branch/worktree is likely safe to **delete**, and is unrelated to
 * `git worktree prune` (which only removes metadata for already-missing dirs).
 */
enum class CleanupCategory { OBSOLETE, LIKELY_OBSOLETE, NONE }

/**
 * Derives a cleanup suggestion from a row, pure and unit-testable:
 *  - OBSOLETE         = branch merged into the default branch AND clean
 *  - LIKELY_OBSOLETE  = upstream gone AND clean
 * "Clean" means no worktree (nothing to lose) or a worktree with no changes.
 * The current worktree, the default branch, the bare entry and any locked
 * worktree are never flagged (git refuses to remove a locked worktree).
 */
object CleanupCandidate {

    fun of(row: WorktreeRow): CleanupCategory {
        if (row.isBare || row.isCurrent || row.isLocked) return CleanupCategory.NONE
        if (row.branch != null && row.branch == row.defaultBranch) return CleanupCategory.NONE

        val clean = if (row.hasWorktree) row.workingTree?.isClean == true else true
        if (!clean) return CleanupCategory.NONE

        return when {
            row.isMerged == true -> CleanupCategory.OBSOLETE
            row.isGone -> CleanupCategory.LIKELY_OBSOLETE
            else -> CleanupCategory.NONE
        }
    }
}
