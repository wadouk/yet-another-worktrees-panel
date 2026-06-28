package com.comet.worktreemanager.model

/**
 * Working-tree dirtiness of a worktree, derived from `git status --porcelain`.
 *
 * @param staged files with staged (index) changes
 * @param modified files with unstaged modifications
 * @param untracked untracked files
 * @param conflicted files with merge conflicts
 */
data class WorkingTreeStatus(
    val staged: Int,
    val modified: Int,
    val untracked: Int,
    val conflicted: Int,
) {
    val isClean: Boolean get() = staged == 0 && modified == 0 && untracked == 0 && conflicted == 0
}
