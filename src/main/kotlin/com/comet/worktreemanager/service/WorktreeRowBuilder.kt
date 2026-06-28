package com.comet.worktreemanager.service

import com.comet.worktreemanager.model.WorktreeInfo
import com.comet.worktreemanager.model.WorktreeRow

/**
 * Merges the worktree list with the local-branch list into the unified rows the
 * table shows: every worktree (branch-backed, detached, or bare) plus every
 * local branch that has no worktree of its own. Pure, so it can be unit-tested.
 */
object WorktreeRowBuilder {

    fun build(
        worktrees: List<WorktreeInfo>,
        branches: List<BranchRef>,
        repositoryRoot: String,
    ): List<WorktreeRow> {
        val byName = branches.associateBy { it.name }
        val consumed = mutableSetOf<String>()
        val rows = mutableListOf<WorktreeRow>()

        for (wt in worktrees) {
            val branch = wt.branch
            val track = branch?.let { byName[it] }
            if (branch != null) consumed += branch
            rows += WorktreeRow(
                branch = branch,
                worktreePath = wt.path,
                head = wt.head,
                upstream = track?.upstream,
                ahead = track?.ahead ?: 0,
                behind = track?.behind ?: 0,
                isGone = track?.isGone ?: false,
                isDetached = wt.isDetached,
                isBare = wt.isBare,
                isLocked = wt.isLocked,
                isPrunable = wt.isPrunable,
                isCurrent = wt.isCurrent,
                repositoryRoot = repositoryRoot,
            )
        }

        for (br in branches) {
            if (br.name in consumed) continue
            rows += WorktreeRow(
                branch = br.name,
                worktreePath = null,
                head = br.head,
                upstream = br.upstream,
                ahead = br.ahead,
                behind = br.behind,
                isGone = br.isGone,
                isDetached = false,
                isBare = false,
                isLocked = false,
                isPrunable = false,
                isCurrent = false,
                repositoryRoot = repositoryRoot,
            )
        }

        return rows
    }
}
