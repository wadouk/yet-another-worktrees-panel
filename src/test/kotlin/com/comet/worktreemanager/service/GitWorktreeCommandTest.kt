package com.comet.worktreemanager.service

import org.junit.Assert.assertEquals
import org.junit.Test

class GitWorktreeCommandTest {

    /**
     * Smoke test for the reflective construction of the `worktree` GitCommand.
     * Runs against the real platform classes, so it fails fast if git4idea ever
     * changes the constructor/locking-policy shape this relies on.
     */
    @Test
    fun buildsWorktreeCommand() {
        assertEquals("worktree", GitWorktreeCommand.INSTANCE.name())
    }
}
