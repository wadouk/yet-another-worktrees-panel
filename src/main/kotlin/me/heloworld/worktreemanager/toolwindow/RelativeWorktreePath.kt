package me.heloworld.worktreemanager.toolwindow

import java.nio.file.Path

/**
 * Pure worktree-path relativizer, used to label the Worktree column:
 *  - the main worktree itself ([path] == [mainPath]) shows as `.`;
 *  - a sibling under the main worktree's parent dir shows just its own folder
 *    name, so the list stays short instead of repeating long absolute paths;
 *  - anything else (different root, a path escaping upward via `..`, or the base
 *    dir) keeps its absolute [path].
 *
 * Pure and unit-testable; the null/absent cases and translation are handled by
 * the presentation layer (see [WorktreeRowPresenter]).
 */
object RelativeWorktreePath {

    fun of(mainPath: String, path: String): String {
        return try {
            val main = Path.of(mainPath)
            val target = Path.of(path)
            if (target == main) return "."
            val base = main.parent ?: return path
            val rel = base.relativize(target)
            if (rel.toString().isEmpty() || rel.startsWith("..")) path else rel.fileName.toString()
        } catch (e: Exception) {
            path
        }
    }
}
