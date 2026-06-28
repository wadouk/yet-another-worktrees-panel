package com.comet.worktreemanager.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BranchRefParserTest {

    private val s = BranchRefParser.SEP

    /** Ahead/behind counts are extracted from the upstream track field. */
    @Test
    fun parsesAheadBehind() {
        val br = BranchRefParser.parse(listOf("feature${s}def456${s}origin/feature${s}[ahead 2, behind 1]")).single()

        assertEquals("feature", br.name)
        assertEquals("origin/feature", br.upstream)
        assertEquals(2, br.ahead)
        assertEquals(1, br.behind)
        assertFalse(br.isGone)
    }

    /** A branch with no upstream has a null upstream and zero counts. */
    @Test
    fun parsesBranchWithoutUpstream() {
        val br = BranchRefParser.parse(listOf("local${s}abc123${s}${s}")).single()

        assertNull(br.upstream)
        assertEquals(0, br.ahead)
        assertEquals(0, br.behind)
    }

    /** A gone upstream is flagged. */
    @Test
    fun parsesGoneUpstream() {
        val br = BranchRefParser.parse(listOf("stale${s}aaa${s}origin/stale${s}[gone]")).single()

        assertTrue(br.isGone)
    }
}
