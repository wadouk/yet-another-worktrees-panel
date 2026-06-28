package com.comet.worktreemanager.service

import com.comet.worktreemanager.model.WorktreeRow

/** Cleanup suggestion for a row, surfaced in the Cleanup column. */
enum class PruneCategory { PRUNABLE, ALMOST_PRUNABLE, NONE }

/**
 * Derives a cleanup suggestion from a row, pure and unit-testable:
 *  - PRUNABLE         = branch merged into the default branch AND clean
 *  - ALMOST_PRUNABLE  = upstream gone AND clean
 * "Clean" means no worktree (nothing to lose) or a worktree with no changes.
 * The current worktree, the default branch and the bare entry are never flagged.
 */
object PruneCandidate {

    fun of(row: WorktreeRow): PruneCategory {
        if (row.isBare || row.isCurrent) return PruneCategory.NONE
        if (row.branch != null && row.branch == row.defaultBranch) return PruneCategory.NONE

        val clean = if (row.hasWorktree) row.workingTree?.isClean == true else true
        if (!clean) return PruneCategory.NONE

        return when {
            row.isMerged == true -> PruneCategory.PRUNABLE
            row.isGone -> PruneCategory.ALMOST_PRUNABLE
            else -> PruneCategory.NONE
        }
    }
}
