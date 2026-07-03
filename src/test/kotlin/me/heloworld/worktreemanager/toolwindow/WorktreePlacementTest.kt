package me.heloworld.worktreemanager.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Test

class WorktreePlacementTest {

    private val repo = "/home/me/proj"

    /** An empty setting defaults to `.claude/worktrees` under the repo root. */
    @Test
    fun baseDirDefaultsToClaudeWorktrees() {
        assertEquals("/home/me/proj/.claude/worktrees", WorktreePlacement.baseDir("", repo))
    }

    /** A relative setting is resolved under the repo root. */
    @Test
    fun baseDirResolvesRelativeAgainstRepo() {
        assertEquals("/home/me/proj/wt", WorktreePlacement.baseDir("wt", repo))
    }

    /** An absolute setting is kept as-is. */
    @Test
    fun baseDirKeepsAbsolute() {
        assertEquals("/elsewhere/wt", WorktreePlacement.baseDir("/elsewhere/wt", repo))
    }

    /** The branch becomes a flat folder name, `/` sanitized to `-`. */
    @Test
    fun defaultPathSanitizesBranchSlashes() {
        assertEquals("/base/feature-x", WorktreePlacement.defaultWorktreePath("/base", "feature/x"))
    }

    /**
     * A move re-parents the worktree under the new base, keeping its own folder
     * name appended exactly once (a trailing separator on the base is absorbed,
     * not doubled).
     */
    @Test
    fun movedPathKeepsFolderNameOnceUnderNewBase() {
        assertEquals("/new/feature-x", WorktreePlacement.movedWorktreePath("/new", "/old/feature-x"))
        assertEquals("/new/feature-x", WorktreePlacement.movedWorktreePath("/new/", "/old/feature-x"))
    }
}
