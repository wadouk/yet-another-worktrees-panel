package com.comet.worktreemanager.service

import com.comet.worktreemanager.model.WorktreeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorktreeRowBuilderTest {

    private fun worktree(path: String, branch: String?, current: Boolean = false) = WorktreeInfo(
        path = path, head = "h_$path", branch = branch,
        isDetached = branch == null, isBare = false, isLocked = false,
        isPrunable = false, isCurrent = current,
    )

    /**
     * Worktrees pick up their branch's tracking status, and a local branch with
     * no worktree shows up as its own row (raw data; labels live in the presenter).
     */
    @Test
    fun mergesWorktreesWithBranchesAndKeepsOrphanBranches() {
        val worktrees = listOf(
            worktree("/repo/main", "main", current = true),
            worktree("/repo/wt-feature", "feature"),
        )
        val branches = listOf(
            BranchRef("main", "h1", "origin/main", ahead = 0, behind = 0, isGone = false),
            BranchRef("feature", "h2", "origin/feature", ahead = 2, behind = 1, isGone = false),
            BranchRef("orphan", "h3", "origin/orphan", ahead = 0, behind = 3, isGone = false),
        )

        val rows = WorktreeRowBuilder.build(worktrees, branches, repositoryRoot = "/repo/main")

        assertEquals(3, rows.size)

        val feature = rows.first { it.branch == "feature" }
        assertTrue(feature.hasWorktree)
        assertEquals(2, feature.ahead)
        assertEquals(1, feature.behind)

        val orphan = rows.first { it.branch == "orphan" }
        assertFalse(orphan.hasWorktree)
        assertEquals(3, orphan.behind)
        assertEquals("origin/orphan", orphan.upstream)
    }
}
