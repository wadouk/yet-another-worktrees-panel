package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.toolwindow.RelativeAge.Bucket
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeAgeTest {

    private val now = 1_000_000_000_000L // fixed reference; `now` is injected

    /** Buckets are selected from the delta, independent of locale. */
    @Test
    fun selectsBuckets() {
        assertEquals(RelativeAge(Bucket.JUST_NOW, 0), RelativeAge.of(now - 10_000, now))
        assertEquals(RelativeAge(Bucket.MINUTE, 5), RelativeAge.of(now - 5 * 60_000, now))
        assertEquals(RelativeAge(Bucket.HOUR, 3), RelativeAge.of(now - 3 * 3_600_000L, now))
        assertEquals(RelativeAge(Bucket.YESTERDAY, 1), RelativeAge.of(now - 30 * 3_600_000L, now))
        assertEquals(RelativeAge(Bucket.DAY, 3), RelativeAge.of(now - 3 * 86_400_000L, now))
    }

    /** Future or equal timestamps clamp to JUST_NOW rather than going negative. */
    @Test
    fun clampsFutureToJustNow() {
        assertEquals(RelativeAge(Bucket.JUST_NOW, 0), RelativeAge.of(now + 60_000, now))
    }
}
