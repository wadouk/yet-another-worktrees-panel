package me.heloworld.worktreemanager.service

/**
 * Picks the "last activity" timestamp for a row: the most recent uncommitted
 * file change when it is newer than the tip commit, otherwise the last commit.
 * Pure, so the choice rule is unit-tested.
 */
object ActivityResolver {

    /** @return (timestamp in millis or null, true when it came from a file change). */
    fun resolve(commitMillis: Long?, latestFileMillis: Long?): Pair<Long?, Boolean> = when {
        latestFileMillis != null && (commitMillis == null || latestFileMillis > commitMillis) ->
            latestFileMillis to true
        commitMillis != null -> commitMillis to false
        else -> null to false
    }
}
