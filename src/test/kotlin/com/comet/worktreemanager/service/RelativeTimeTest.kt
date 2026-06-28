package com.comet.worktreemanager.service

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_000_000_000_000L // fixed reference

    /** Buckets render git-style relative strings. */
    @Test
    fun formatsRelativeBuckets() {
        assertEquals("just now", RelativeTime.format(now - 10_000, now))
        assertEquals("5 minutes ago", RelativeTime.format(now - 5 * 60_000, now))
        assertEquals("3 hours ago", RelativeTime.format(now - 3 * 3_600_000L, now))
        assertEquals("2 days ago", RelativeTime.format(now - 2 * 86_400_000L, now))
    }

    /** Future or equal timestamps clamp to "just now" rather than going negative. */
    @Test
    fun clampsFutureToJustNow() {
        assertEquals("just now", RelativeTime.format(now + 60_000, now))
    }
}
