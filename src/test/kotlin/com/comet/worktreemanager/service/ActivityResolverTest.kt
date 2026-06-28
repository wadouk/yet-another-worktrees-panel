package com.comet.worktreemanager.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityResolverTest {

    /** A newer uncommitted file change wins over the last commit. */
    @Test
    fun fileChangeWinsWhenNewer() {
        val (millis, fromFile) = ActivityResolver.resolve(commitMillis = 1_000, latestFileMillis = 5_000)

        assertEquals(5_000L, millis)
        assertTrue(fromFile)
    }

    /** Falls back to the commit when there is no (newer) file change. */
    @Test
    fun commitUsedWhenNoNewerFile() {
        val (millis, fromFile) = ActivityResolver.resolve(commitMillis = 9_000, latestFileMillis = 2_000)

        assertEquals(9_000L, millis)
        assertFalse(fromFile)
    }

    /** Nothing known yields null. */
    @Test
    fun nullWhenNothingKnown() {
        val (millis, fromFile) = ActivityResolver.resolve(commitMillis = null, latestFileMillis = null)

        assertNull(millis)
        assertFalse(fromFile)
    }
}
