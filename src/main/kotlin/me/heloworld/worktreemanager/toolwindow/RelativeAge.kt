package me.heloworld.worktreemanager.toolwindow

/**
 * Locale-independent relative-age bucket. Pure: `now` is injected, so it has no
 * dependency on the system clock and is unit-testable. Translation of the bucket
 * into text happens in the presentation layer (see [RelativeTimeCell]).
 */
data class RelativeAge(val bucket: Bucket, val count: Long) {

    enum class Bucket { JUST_NOW, MINUTE, HOUR, YESTERDAY, DAY, MONTH, YEAR }

    companion object {
        fun of(epochMillis: Long, nowMillis: Long): RelativeAge {
            val sec = ((nowMillis - epochMillis) / 1000).coerceAtLeast(0)
            return when {
                sec < 45 -> RelativeAge(Bucket.JUST_NOW, 0)
                sec < 90 -> RelativeAge(Bucket.MINUTE, 1)
                sec < 3_600 -> RelativeAge(Bucket.MINUTE, sec / 60)
                sec < 7_200 -> RelativeAge(Bucket.HOUR, 1)
                sec < 86_400 -> RelativeAge(Bucket.HOUR, sec / 3_600)
                sec < 172_800 -> RelativeAge(Bucket.YESTERDAY, 1)
                sec < 2_592_000 -> RelativeAge(Bucket.DAY, sec / 86_400)
                sec < 5_184_000 -> RelativeAge(Bucket.MONTH, 1)
                sec < 31_536_000 -> RelativeAge(Bucket.MONTH, sec / 2_592_000)
                sec < 63_072_000 -> RelativeAge(Bucket.YEAR, 1)
                else -> RelativeAge(Bucket.YEAR, sec / 31_536_000)
            }
        }
    }
}
