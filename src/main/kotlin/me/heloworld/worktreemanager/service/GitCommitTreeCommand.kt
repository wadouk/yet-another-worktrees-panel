package me.heloworld.worktreemanager.service

import git4idea.commands.GitCommand

/**
 * `git commit-tree` is not exposed as a predefined [GitCommand] constant in the
 * targeted IDE build, so it is built reflectively (see [GitReflectiveCommand]).
 *
 * Used to detect squash-/rebase-merged branches: it only writes a throwaway
 * (dangling) commit object — no ref or index mutation — so READ locking is
 * enough and avoids serializing against the surrounding read-only scan.
 */
internal object GitCommitTreeCommand {
    val INSTANCE: GitCommand by lazy { GitReflectiveCommand.build("commit-tree", "READ") }
}
