package com.comet.worktreemanager.service

import com.comet.worktreemanager.model.WorkingTreeStatus
import com.comet.worktreemanager.model.WorktreeRow
import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupCandidateTest {

    private fun row(
        branch: String? = "feature",
        worktreePath: String? = "/repo/wt",
        isMerged: Boolean? = null,
        isGone: Boolean = false,
        isCurrent: Boolean = false,
        defaultBranch: String? = "main",
        workingTree: WorkingTreeStatus? = WorkingTreeStatus(0, 0, 0, 0),
    ) = WorktreeRow(
        branch = branch, worktreePath = worktreePath, head = "h", upstream = null,
        ahead = 0, behind = 0, isGone = isGone, isDetached = false, isBare = false,
        isLocked = false, isPrunable = false, isCurrent = isCurrent, repositoryRoot = "/repo",
        workingTree = workingTree, isMerged = isMerged, defaultBranch = defaultBranch,
    )

    /** Merged + clean → obsolete. */
    @Test
    fun mergedAndCleanIsObsolete() {
        assertEquals(CleanupCategory.OBSOLETE, CleanupCandidate.of(row(isMerged = true)))
    }

    /** Upstream gone + clean → likely obsolete. */
    @Test
    fun goneAndCleanIsLikelyObsolete() {
        assertEquals(CleanupCategory.LIKELY_OBSOLETE, CleanupCandidate.of(row(isMerged = false, isGone = true)))
    }

    /** Dirty worktree is never a candidate, even when merged. */
    @Test
    fun dirtyIsNeverCandidate() {
        val dirty = row(isMerged = true, workingTree = WorkingTreeStatus(0, 2, 0, 0))
        assertEquals(CleanupCategory.NONE, CleanupCandidate.of(dirty))
    }

    /** The default branch and the current worktree are never flagged. */
    @Test
    fun defaultAndCurrentAreExcluded() {
        assertEquals(CleanupCategory.NONE, CleanupCandidate.of(row(branch = "main", isMerged = true)))
        assertEquals(CleanupCategory.NONE, CleanupCandidate.of(row(isMerged = true, isCurrent = true)))
    }
}
