package me.heloworld.worktreemanager.service

import org.junit.Assert.assertEquals
import org.junit.Test

class GitCommitTreeCommandTest {

    /**
     * Smoke test for the reflective construction of the `commit-tree` GitCommand.
     * Runs against the real platform classes, so it fails fast if git4idea ever
     * changes the constructor/locking-policy shape this relies on.
     */
    @Test
    fun buildsCommitTreeCommand() {
        assertEquals("commit-tree", GitCommitTreeCommand.INSTANCE.name())
    }
}
