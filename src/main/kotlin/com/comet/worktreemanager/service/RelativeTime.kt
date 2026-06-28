package com.comet.worktreemanager.service

/** Formats a timestamp as a git-style relative string ("3 days ago"). Pure. */
object RelativeTime {

    fun format(epochMillis: Long, nowMillis: Long): String {
        val sec = ((nowMillis - epochMillis) / 1000).coerceAtLeast(0)
        return when {
            sec < 45 -> "just now"
            sec < 90 -> "1 minute ago"
            sec < 3_600 -> "${sec / 60} minutes ago"
            sec < 7_200 -> "1 hour ago"
            sec < 86_400 -> "${sec / 3_600} hours ago"
            sec < 172_800 -> "yesterday"
            sec < 2_592_000 -> "${sec / 86_400} days ago"
            sec < 5_184_000 -> "1 month ago"
            sec < 31_536_000 -> "${sec / 2_592_000} months ago"
            sec < 63_072_000 -> "1 year ago"
            else -> "${sec / 31_536_000} years ago"
        }
    }
}
