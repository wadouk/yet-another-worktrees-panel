package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.model.WorktreeRow

/** The git flags a single "Force" tick would carry, for the row at hand. */
enum class ForceScope {
    /** `worktree remove --force` only — a detached worktree. */
    WORKTREE,

    /** `branch -D` only — a branch with no worktree. */
    BRANCH,

    /** Both, the usual case: a worktree and the branch it checks out. */
    BOTH,
}

/**
 * The rules behind the delete dialog, kept out of the Swing code so they stay
 * unit-testable and live in a single place.
 *
 * The dialog offers one "Force" checkbox, but it drives two independent git
 * flags at once: `worktree remove --force` (discard uncommitted work) and
 * `branch -D` (delete a branch that is not fully merged). Reasoning about that
 * checkbox as if it only affected the worktree is what once let a clean worktree
 * grey out the only path to `-D`.
 */
object DeleteRules {

    /** The row has a worktree we are allowed to remove. */
    fun canRemoveWorktree(row: WorktreeRow): Boolean =
        row.hasWorktree && !row.isBare && !row.isCurrent

    /** The row has a branch we are allowed to delete. */
    fun canDeleteBranch(row: WorktreeRow): Boolean =
        row.hasBranch && !row.isCurrent

    /**
     * Whether ticking "Force" can still change the outcome. The dialog greys the
     * checkbox out when it cannot, rather than offering an inert toggle.
     *
     * A clean worktree has nothing to discard — but its branch may well need
     * `-D`, so [deleteBranch] (the "Also delete branch" toggle) is part of the
     * answer and the checkbox comes back to life as soon as it is ticked.
     *
     * An unknown working-tree state counts as dirty: better an ineffective
     * toggle than a missing one. Mergedness is deliberately not consulted —
     * squash merges make it unreliable, so `-d` can fail on a branch we believe
     * is merged.
     */
    fun forceIsUseful(row: WorktreeRow, deleteBranch: Boolean): Boolean =
        isWorktreeDirty(row) || deleteBranch

    /**
     * Which flags the "Force" checkbox stands for, so the dialog can label it
     * accurately. The label itself is picked there: bundle keys have to stay
     * literals for `@PropertyKey` to resolve them.
     */
    fun forceScope(row: WorktreeRow): ForceScope = when {
        canRemoveWorktree(row) && canDeleteBranch(row) -> ForceScope.BOTH
        canRemoveWorktree(row) -> ForceScope.WORKTREE
        else -> ForceScope.BRANCH
    }

    private fun isWorktreeDirty(row: WorktreeRow): Boolean =
        canRemoveWorktree(row) && row.workingTree?.isClean != true
}
