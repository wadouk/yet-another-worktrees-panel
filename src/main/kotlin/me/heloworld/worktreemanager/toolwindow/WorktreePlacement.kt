package me.heloworld.worktreemanager.toolwindow

import java.nio.file.Path

/**
 * Pure placement logic for a new worktree, used to prefill the create dialog:
 *  - [baseDir] resolves the configured setting against a repository root — an
 *    empty setting defaults to `<repoRoot>/.claude/worktrees`, a relative one is
 *    resolved under the repo root, and an absolute one is kept as-is;
 *  - [defaultWorktreePath] appends the branch as a flat folder name under the
 *    base dir, sanitizing `/` to `-` so `feature/x` lands in `<base>/feature-x`.
 *
 * Pure (String -> String); the settings lookup and git call live elsewhere.
 */
object WorktreePlacement {

    fun baseDir(setting: String, repoRoot: String): String {
        val s = setting.trim()
        return when {
            s.isEmpty() -> Path.of(repoRoot, ".claude", "worktrees").toString()
            Path.of(s).isAbsolute -> s
            else -> Path.of(repoRoot).resolve(s).toString()
        }
    }

    fun defaultWorktreePath(baseDir: String, branch: String): String =
        Path.of(baseDir).resolve(branch.replace('/', '-')).toString()
}
