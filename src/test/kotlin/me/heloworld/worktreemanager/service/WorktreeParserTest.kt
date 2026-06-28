package me.heloworld.worktreemanager.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorktreeParserTest {

    /** A normal branch worktree, flagged as current when its path matches. */
    @Test
    fun parsesBranchWorktreeAndMarksCurrent() {
        val lines = """
            worktree /repo/main
            HEAD 1a2b3c4d
            branch refs/heads/main
        """.trimIndent().lines()

        val result = WorktreeParser.parse(lines, currentRootPath = "/repo/main")

        assertEquals(1, result.size)
        val wt = result[0]
        assertEquals("/repo/main", wt.path)
        assertEquals("main", wt.branch)
        assertEquals("1a2b3c4d", wt.head)
        assertFalse(wt.isDetached)
        assertTrue(wt.isCurrent)
    }

    /** A detached-HEAD worktree has no branch and is flagged detached. */
    @Test
    fun parsesDetachedWorktree() {
        val lines = """
            worktree /repo/wt-detached
            HEAD 4d5e6f70
            detached
        """.trimIndent().lines()

        val wt = WorktreeParser.parse(lines, currentRootPath = "/repo/main").single()

        assertNull(wt.branch)
        assertTrue(wt.isDetached)
        assertFalse(wt.isCurrent)
        assertEquals("(detached 4d5e6f7)", wt.refLabel)
    }

    /** Multiple records, including the bare entry, are split on blank lines. */
    @Test
    fun parsesMultipleRecordsIncludingBare() {
        val lines = """
            worktree /repo/.bare
            bare

            worktree /repo/main
            HEAD 1a2b3c4d
            branch refs/heads/main

            worktree /repo/wt-feature
            HEAD aabbccdd
            branch refs/heads/feature/x
            locked
        """.trimIndent().lines()

        val result = WorktreeParser.parse(lines, currentRootPath = null)

        assertEquals(3, result.size)
        assertTrue(result[0].isBare)
        assertEquals("main", result[1].branch)
        assertEquals("feature/x", result[2].branch)
        assertTrue(result[2].isLocked)
    }
}
