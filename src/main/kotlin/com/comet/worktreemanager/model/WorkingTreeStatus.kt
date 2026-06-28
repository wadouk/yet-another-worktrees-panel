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

    /** Compact column label, e.g. `clean` or `+1 ~2 ?3 !1`. */
    val shortLabel: String
        get() = if (isClean) "clean" else buildList {
            if (staged > 0) add("+$staged")
            if (modified > 0) add("~$modified")
            if (untracked > 0) add("?$untracked")
            if (conflicted > 0) add("!$conflicted")
        }.joinToString(" ")
}
