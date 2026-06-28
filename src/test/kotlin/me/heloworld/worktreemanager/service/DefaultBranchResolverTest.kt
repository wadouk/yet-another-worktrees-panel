package me.heloworld.worktreemanager.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultBranchResolverTest {

    /** The remote default (origin/HEAD) wins and is compared against origin/<name>. */
    @Test
    fun prefersOriginHead() {
        val result = DefaultBranchResolver.resolve("origin/main", setOf("main", "feature"))

        assertEquals("main", result?.name)
        assertEquals("origin/main", result?.ref)
    }

    /** Without a remote default, falls back to a local main/master. */
    @Test
    fun fallsBackToLocalMaster() {
        val result = DefaultBranchResolver.resolve(originHead = null, localBranches = setOf("master", "wip"))

        assertEquals("master", result?.name)
        assertEquals("master", result?.ref)
    }

    /** Nothing resolvable returns null. */
    @Test
    fun returnsNullWhenUnknown() {
        assertNull(DefaultBranchResolver.resolve(originHead = "origin/HEAD", localBranches = setOf("dev")))
    }
}
