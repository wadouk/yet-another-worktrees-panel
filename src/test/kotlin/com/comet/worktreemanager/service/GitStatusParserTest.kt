package com.comet.worktreemanager.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitStatusParserTest {

    /** Empty output means a clean working tree. */
    @Test
    fun parsesCleanTree() {
        val status = GitStatusParser.parse(emptyList())

        assertTrue(status.isClean)
        assertEquals("clean", status.shortLabel)
    }

    /** Staged, unstaged, and untracked changes are counted from the XY codes. */
    @Test
    fun countsStagedModifiedAndUntracked() {
        val status = GitStatusParser.parse(
            listOf(
                "M  staged.kt",   // staged modification
                " M dirty.kt",    // unstaged modification
                "MM both.kt",     // staged + unstaged
                "?? new.kt",      // untracked
            ),
        )

        assertEquals(2, status.staged)
        assertEquals(2, status.modified)
        assertEquals(1, status.untracked)
        assertEquals("+2 ~2 ?1", status.shortLabel)
    }

    /** Merge-conflict codes are counted as conflicts, not staged/modified. */
    @Test
    fun countsConflicts() {
        val status = GitStatusParser.parse(listOf("UU conflict.kt", "AA added-both.kt"))

        assertEquals(2, status.conflicted)
        assertEquals(0, status.staged)
        assertEquals(0, status.modified)
    }
}
