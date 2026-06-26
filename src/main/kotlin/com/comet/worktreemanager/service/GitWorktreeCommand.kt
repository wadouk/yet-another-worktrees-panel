package com.comet.worktreemanager.service

import git4idea.commands.GitCommand

/**
 * `git worktree` is not exposed as a predefined [GitCommand] constant in the
 * targeted IDE build, and [GitCommand]'s constructor (plus its `LockingPolicy`
 * enum) is package-private. We build the command reflectively so it still flows
 * through git4idea's standard execution pipeline (configured executable, repo
 * locking, environment) rather than shelling out ourselves.
 *
 * WRITE locking is used so removal/prune serialize against other git writes;
 * the read-only `list` simply waits a touch longer, which is harmless.
 */
internal object GitWorktreeCommand {

    val INSTANCE: GitCommand by lazy { build() }

    private fun build(): GitCommand {
        val lockingPolicyClass = Class.forName("git4idea.commands.GitCommand\$LockingPolicy")
        val write = lockingPolicyClass.getDeclaredField("WRITE").apply { isAccessible = true }.get(null)
        val ctor = GitCommand::class.java
            .getDeclaredConstructor(String::class.java, lockingPolicyClass)
            .apply { isAccessible = true }
        return ctor.newInstance("worktree", write) as GitCommand
    }
}
