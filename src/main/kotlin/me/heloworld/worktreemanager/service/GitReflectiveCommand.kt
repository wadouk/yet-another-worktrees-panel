package me.heloworld.worktreemanager.service

import git4idea.commands.GitCommand

/**
 * Builds [GitCommand] instances for git subcommands the targeted IDE build does
 * not expose as predefined constants. [GitCommand]'s constructor and its
 * `LockingPolicy` enum are package-private, so we reach them reflectively while
 * still flowing through git4idea's standard execution pipeline (configured
 * executable, repo locking, environment) rather than shelling out ourselves.
 */
internal object GitReflectiveCommand {

    /**
     * @param name the git subcommand (e.g. `worktree`, `commit-tree`)
     * @param lockingPolicy the `GitCommand.LockingPolicy` enum constant name,
     *        e.g. `WRITE` for ref/index mutations, `READ` for read-only calls.
     */
    fun build(name: String, lockingPolicy: String): GitCommand {
        val lockingPolicyClass = Class.forName("git4idea.commands.GitCommand\$LockingPolicy")
        val policy = lockingPolicyClass.getDeclaredField(lockingPolicy)
            .apply { isAccessible = true }.get(null)
        val ctor = GitCommand::class.java
            .getDeclaredConstructor(String::class.java, lockingPolicyClass)
            .apply { isAccessible = true }
        return ctor.newInstance(name, policy) as GitCommand
    }
}
