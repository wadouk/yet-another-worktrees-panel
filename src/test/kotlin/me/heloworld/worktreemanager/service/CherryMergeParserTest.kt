package me.heloworld.worktreemanager.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CherryMergeParserTest {

    /** A `-` line means the patch is already upstream → squash/rebase merged. */
    @Test
    fun equivalentPatchPresentIsMerged() {
        assertTrue(CherryMergeParser.isPatchPresentUpstream(listOf("- 396aed25659287cc259dbbcafab2bd8986ad082")))
    }

    /** A `+` line means no equivalent upstream → not merged. */
    @Test
    fun ownPatchIsNotMerged() {
        assertFalse(CherryMergeParser.isPatchPresentUpstream(listOf("+ 9aeaf722262da68c1793cb73e5050d6c8c1b21f")))
    }

    /** No output is treated as not merged (conservative, safe default). */
    @Test
    fun emptyIsNotMerged() {
        assertFalse(CherryMergeParser.isPatchPresentUpstream(emptyList()))
    }
}
