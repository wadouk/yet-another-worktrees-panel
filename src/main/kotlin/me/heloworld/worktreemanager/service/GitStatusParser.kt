package me.heloworld.worktreemanager.service

import me.heloworld.worktreemanager.model.WorkingTreeStatus

/**
 * Pure parser for `git status --porcelain` (v1) output. Each line begins with a
 * two-character `XY` code (X = index/staged state, Y = working-tree state):
 * `??` is untracked; `U`/`AA`/`DD` are merge conflicts; otherwise a non-blank X
 * counts as staged and a non-blank Y as modified. Kept IntelliJ-free for tests.
 */
object GitStatusParser {

    fun parse(lines: List<String>): WorkingTreeStatus {
        var staged = 0
        var modified = 0
        var untracked = 0
        var conflicted = 0

        for (line in lines) {
            if (line.length < 2) continue
            val x = line[0]
            val y = line[1]
            when {
                x == '?' && y == '?' -> untracked++
                x == 'U' || y == 'U' || (x == 'A' && y == 'A') || (x == 'D' && y == 'D') -> conflicted++
                else -> {
                    if (x != ' ') staged++
                    if (y != ' ') modified++
                }
            }
        }
        return WorkingTreeStatus(staged, modified, untracked, conflicted)
    }

    /**
     * Extracts the worktree-relative paths of changed files from porcelain lines.
     * For renames/copies (`R  old -> new`) the destination path is returned.
     * Surrounding quotes (git quotes paths with special chars) are stripped.
     */
    fun changedPaths(lines: List<String>): List<String> =
        lines.filter { it.length > 3 }.map { line ->
            val raw = line.substring(3)
            val path = raw.substringAfter(" -> ", raw)
            path.removeSurrounding("\"")
        }
}
