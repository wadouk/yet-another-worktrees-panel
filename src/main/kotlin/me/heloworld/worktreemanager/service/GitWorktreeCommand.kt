package me.heloworld.worktreemanager.service

import git4idea.commands.GitCommand

/**
 * `git worktree` is not exposed as a predefined [GitCommand] constant in the
 * targeted IDE build, so it is built reflectively (see [GitReflectiveCommand]).
 *
 * WRITE locking is used so removal/prune serialize against other git writes;
 * the read-only `list` simply waits a touch longer, which is harmless.
 */
internal object GitWorktreeCommand {
    val INSTANCE: GitCommand by lazy { GitReflectiveCommand.build("worktree", "WRITE") }
}
