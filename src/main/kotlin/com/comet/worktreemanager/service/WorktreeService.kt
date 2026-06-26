package com.comet.worktreemanager.service

import com.comet.worktreemanager.model.WorktreeInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

/**
 * Drives `git worktree` operations through the bundled git4idea command API.
 * All methods run the underlying git process synchronously, so callers must
 * invoke them off the EDT (e.g. from a background task).
 */
@Service(Service.Level.PROJECT)
class WorktreeService(private val project: Project) {

    /** Git repositories registered in the project (one per VCS root). */
    fun repositories(): List<GitRepository> =
        GitRepositoryManager.getInstance(project).repositories

    /** Lists every worktree across all repositories, de-duplicated by path. */
    fun listAll(): List<WorktreeInfo> =
        repositories()
            .flatMap { list(it) }
            .distinctBy { it.path.trimEnd('/') }

    /** Lists worktrees for a single repository. */
    fun list(repo: GitRepository): List<WorktreeInfo> {
        val handler = GitLineHandler(project, repo.root, GitCommand.WORKTREE)
        handler.addParameters("list", "--porcelain")
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            thisLogger().warn("git worktree list failed: ${result.errorOutputAsJoinedString}")
            return emptyList()
        }
        return WorktreeParser.parse(result.output, repo.root.path)
    }

    /**
     * Removes a worktree. With [force] git also removes worktrees that contain
     * uncommitted changes or untracked files.
     */
    fun remove(repo: GitRepository, worktreePath: String, force: Boolean): GitCommandResult {
        val handler = GitLineHandler(project, repo.root, GitCommand.WORKTREE)
        handler.addParameters("remove")
        if (force) handler.addParameters("--force")
        handler.addParameters(worktreePath)
        return Git.getInstance().runCommand(handler)
    }

    /** Deletes a local branch. Uses `-D` when [force] to drop unmerged branches. */
    fun deleteBranch(repo: GitRepository, branch: String, force: Boolean): GitCommandResult {
        val handler = GitLineHandler(project, repo.root, GitCommand.BRANCH)
        handler.addParameters(if (force) "-D" else "-d", branch)
        return Git.getInstance().runCommand(handler)
    }

    /** Prunes administrative files for worktrees whose directory is gone. */
    fun prune(repo: GitRepository): GitCommandResult {
        val handler = GitLineHandler(project, repo.root, GitCommand.WORKTREE)
        handler.addParameters("prune")
        return Git.getInstance().runCommand(handler)
    }

    /** Finds the repository owning a given worktree path, if any. */
    fun repositoryFor(worktreePath: String): GitRepository? {
        val normalized = worktreePath.trimEnd('/')
        return repositories().firstOrNull { repo ->
            list(repo).any { it.path.trimEnd('/') == normalized }
        }
    }
}
