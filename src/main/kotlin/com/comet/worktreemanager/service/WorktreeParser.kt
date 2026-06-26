package com.comet.worktreemanager.service

import com.comet.worktreemanager.model.WorktreeInfo

/**
 * Pure parser for `git worktree list --porcelain` output. Kept free of any
 * IntelliJ API so it can be unit-tested directly.
 *
 * Porcelain format: records separated by blank lines, each a set of
 * `key value` (or bare flag) lines, e.g.
 *
 * ```
 * worktree /repo/main
 * HEAD 1a2b3c...
 * branch refs/heads/main
 *
 * worktree /repo/wt-feature
 * HEAD 4d5e6f...
 * detached
 * ```
 */
object WorktreeParser {

    /**
     * @param lines raw porcelain output lines
     * @param currentRootPath absolute path of the worktree open in the IDE,
     *        used to flag the matching entry as current (already normalized)
     */
    fun parse(lines: List<String>, currentRootPath: String?): List<WorktreeInfo> {
        val result = mutableListOf<WorktreeInfo>()

        var path: String? = null
        var head: String? = null
        var branch: String? = null
        var detached = false
        var bare = false
        var locked = false
        var prunable = false

        fun flush() {
            val p = path ?: return
            result += WorktreeInfo(
                path = p,
                head = head,
                branch = branch,
                isDetached = detached,
                isBare = bare,
                isLocked = locked,
                isPrunable = prunable,
                isCurrent = currentRootPath != null && samePath(p, currentRootPath),
            )
            path = null; head = null; branch = null
            detached = false; bare = false; locked = false; prunable = false
        }

        for (line in lines) {
            when {
                line.isBlank() -> flush()
                line.startsWith("worktree ") -> {
                    // A new record may start without a blank separator on the first one.
                    if (path != null) flush()
                    path = line.removePrefix("worktree ").trim()
                }
                line.startsWith("HEAD ") -> head = line.removePrefix("HEAD ").trim()
                line.startsWith("branch ") ->
                    branch = line.removePrefix("branch ").trim().removePrefix("refs/heads/")
                line == "detached" -> detached = true
                line == "bare" -> bare = true
                line.startsWith("locked") -> locked = true
                line.startsWith("prunable") -> prunable = true
            }
        }
        flush()
        return result
    }

    private fun samePath(a: String, b: String): Boolean =
        a.trimEnd('/') == b.trimEnd('/')
}
