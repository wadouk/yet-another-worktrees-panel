package me.heloworld.worktreemanager.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeWorktreePathTest {

    /** Main worktree; siblings live next to it under its parent dir. */
    private val main = "/home/me/proj"

    /** A sibling worktree shows relative to the dir containing the main worktree. */
    @Test
    fun relativizesSiblingWorktree() {
        assertEquals("feature", RelativeWorktreePath.of(main, "/home/me/proj-wt/feature"))
    }

    /** The main worktree itself shows as the current dir (`.`). */
    @Test
    fun summarizeProjectPath() {
        assertEquals(".", RelativeWorktreePath.of(main, "/home/me/proj"))
    }

    /** A path outside that base (escaping upward) keeps its absolute form. */
    @Test
    fun keepsAbsoluteWhenOutsideBase() {
        assertEquals("/var/tmp/wt", RelativeWorktreePath.of(main, "/var/tmp/wt"))
    }

    /** The base dir itself (empty relative path) keeps its absolute form. */
    @Test
    fun keepsAbsoluteForBaseDir() {
        assertEquals("/home/me", RelativeWorktreePath.of(main, "/home/me"))
    }
}
