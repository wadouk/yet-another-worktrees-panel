package me.heloworld.worktreemanager.service

/**
 * Pure parser for `git cherry <upstream> <head>` output, used to detect
 * squash-/rebase-merged branches (which the ancestor test `--merged` misses).
 *
 * `git cherry` prints one line per commit in <head>:
 *  - `- <sha>` when an equivalent patch already exists upstream,
 *  - `+ <sha>` when it does not.
 *
 * We feed it a single throwaway commit carrying the branch's whole tree on top
 * of the merge base, so its one cumulative patch is `-` exactly when the
 * branch's changes are already in the upstream (i.e. it was squash/rebase
 * merged). Kept free of any git/IntelliJ API so it can be unit-tested directly.
 */
object CherryMergeParser {

    /**
     * True when every reported commit has an equivalent already upstream (all
     * lines `-`). Empty or unexpected output (e.g. a `+` line) yields false, so
     * ambiguity is treated as "not merged" — the conservative, safe default.
     */
    fun isPatchPresentUpstream(cherryOutput: List<String>): Boolean {
        val lines = cherryOutput.map { it.trim() }.filter { it.isNotEmpty() }
        return lines.isNotEmpty() && lines.all { it.startsWith("-") }
    }
}
