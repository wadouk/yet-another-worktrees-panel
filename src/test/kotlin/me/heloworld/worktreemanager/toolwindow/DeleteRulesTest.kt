package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.model.WorkingTreeStatus
import me.heloworld.worktreemanager.model.WorktreeRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteRulesTest {

    private val clean = WorkingTreeStatus(0, 0, 0, 0)
    private val dirty = WorkingTreeStatus(0, 1, 0, 0)

    private fun row(
        branch: String? = "feature",
        worktreePath: String? = "/repo/wt",
        workingTree: WorkingTreeStatus? = clean,
    ) = WorktreeRow(
        branch = branch, worktreePath = worktreePath, head = "h", upstream = null,
        ahead = 0, behind = 0, isGone = false, isDetached = false, isBare = false,
        isLocked = false, isPrunable = false, isCurrent = false, repositoryRoot = "/repo",
        workingTree = workingTree,
    )

    /** A dirty worktree always has something to discard, branch toggle or not. */
    @Test
    fun dirtyWorktreeMakesForceUseful() {
        val dirtyRow = row(workingTree = dirty)
        assertTrue(DeleteRules.forceIsUseful(dirtyRow, deleteBranch = false))
        assertTrue(DeleteRules.forceIsUseful(dirtyRow, deleteBranch = true))
    }

    /** Regression: a clean worktree must not lock `-D` out of reach. */
    @Test
    fun cleanWorktreeWithBranchDeletionMakesForceUseful() {
        assertTrue(DeleteRules.forceIsUseful(row(), deleteBranch = true))
    }

    /** The original intent of the grey-out: nothing to force, so offer nothing. */
    @Test
    fun cleanWorktreeWithoutBranchDeletionMakesForceInert() {
        assertFalse(DeleteRules.forceIsUseful(row(), deleteBranch = false))
    }

    /** A branch with no worktree can always need `-D`. */
    @Test
    fun branchOnlyMakesForceUseful() {
        assertTrue(DeleteRules.forceIsUseful(row(worktreePath = null), deleteBranch = true))
    }

    /** One scope — hence one label — per shape the dialog takes. */
    @Test
    fun scopeFollowsTheDialogShape() {
        assertEquals(ForceScope.BOTH, DeleteRules.forceScope(row()))
        assertEquals(ForceScope.WORKTREE, DeleteRules.forceScope(row(branch = null)))
        assertEquals(ForceScope.BRANCH, DeleteRules.forceScope(row(worktreePath = null)))
    }
}
