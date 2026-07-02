package me.heloworld.worktreemanager.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorktreeBranchIndexTest {

    /**
     * Returns the first ref name that has a worktree, following the commit's ref
     * order. Names without a worktree (e.g. a remote branch not in the set) are
     * skipped rather than matched.
     */
    @Test
    fun picksFirstBranchWithWorktree() {
        val picked = WorktreeBranchIndex.pickBranch(
            branchNames = listOf("origin/feature", "feature", "main"),
            worktreeBranches = setOf("feature", "main"),
        )

        assertEquals("feature", picked)
    }

    /** When no ref at the commit has a worktree, nothing is picked. */
    @Test
    fun returnsNullWhenNoWorktreeBranch() {
        val picked = WorktreeBranchIndex.pickBranch(
            branchNames = listOf("origin/main", "wip"),
            worktreeBranches = setOf("feature"),
        )

        assertNull(picked)
    }
}
