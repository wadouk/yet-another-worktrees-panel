package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.i18n.WorktreeBundle

/**
 * Table cell that renders a timestamp as a localized relative string but sorts
 * by the underlying millis, so the Activity column orders chronologically rather
 * than lexically. The system clock and the translation live only here (the
 * presentation layer); null timestamps render as a dash and sort oldest.
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

    override fun toString(): String {
        val ts = millis ?: return WorktreeBundle.message("activity.none")
        val age = RelativeAge.of(ts, System.currentTimeMillis())
        return when (age.bucket) {
            RelativeAge.Bucket.JUST_NOW -> WorktreeBundle.message("activity.justNow")
            RelativeAge.Bucket.YESTERDAY -> WorktreeBundle.message("activity.yesterday")
            RelativeAge.Bucket.MINUTE ->
                if (age.count == 1L) WorktreeBundle.message("activity.minute.one")
                else WorktreeBundle.message("activity.minutes", age.count)
            RelativeAge.Bucket.HOUR ->
                if (age.count == 1L) WorktreeBundle.message("activity.hour.one")
                else WorktreeBundle.message("activity.hours", age.count)
            RelativeAge.Bucket.DAY -> WorktreeBundle.message("activity.days", age.count)
            RelativeAge.Bucket.MONTH ->
                if (age.count == 1L) WorktreeBundle.message("activity.month.one")
                else WorktreeBundle.message("activity.months", age.count)
            RelativeAge.Bucket.YEAR ->
                if (age.count == 1L) WorktreeBundle.message("activity.year.one")
                else WorktreeBundle.message("activity.years", age.count)
        }
    }
}
