package com.comet.worktreemanager.service

/** A local branch with its upstream tracking status. */
data class BranchRef(
    val name: String,
    val head: String,
    val upstream: String?,
    val ahead: Int,
    val behind: Int,
    val isGone: Boolean,
)

/**
 * Pure parser for `git for-each-ref refs/heads` output produced with the format
 * `%(refname:short)<SEP>%(objectname)<SEP>%(upstream:short)<SEP>%(upstream:track)`.
 *
 * The track field looks like `[ahead 2]`, `[behind 1]`, `[ahead 2, behind 1]`,
 * `[gone]`, or is empty when the branch is in sync / has no upstream.
 * A U+0001 (SOH) separator is used because it cannot appear in a ref name.
 */
object BranchRefParser {

    /** SOH field separator, shared with the for-each-ref format string. */
    val SEP: String = Char(1).toString()

    private val AHEAD = Regex("""ahead (\d+)""")
    private val BEHIND = Regex("""behind (\d+)""")

    fun parse(lines: List<String>): List<BranchRef> =
        lines.filter { it.isNotBlank() }.mapNotNull { parseLine(it) }

    private fun parseLine(line: String): BranchRef? {
        val parts = line.split(SEP)
        if (parts.size < 4) return null
        val name = parts[0].trim()
        if (name.isEmpty()) return null
        val head = parts[1].trim()
        val upstream = parts[2].trim().ifEmpty { null }
        val track = parts[3]
        return BranchRef(
            name = name,
            head = head,
            upstream = upstream,
            ahead = AHEAD.find(track)?.groupValues?.get(1)?.toInt() ?: 0,
            behind = BEHIND.find(track)?.groupValues?.get(1)?.toInt() ?: 0,
            isGone = track.contains("gone"),
        )
    }
}
