package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.service.RelativeTime

/**
 * Table cell that renders a timestamp as a relative string but sorts by the
 * underlying millis, so the Activity column orders chronologically rather than
 * lexically. Null timestamps render as "—" and sort oldest.
 */
class RelativeTimeCell(private val millis: Long?) : Comparable<RelativeTimeCell> {

    override fun compareTo(other: RelativeTimeCell): Int {
        val a = millis
        val b = other.millis
        return when {
            a == null && b == null -> 0
            a == null -> -1
            b == null -> 1
            else -> a.compareTo(b)
        }
    }

    override fun toString(): String =
        millis?.let { RelativeTime.format(it, System.currentTimeMillis()) } ?: "—"
}
