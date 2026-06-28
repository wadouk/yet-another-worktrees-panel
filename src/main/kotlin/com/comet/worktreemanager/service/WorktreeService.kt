package com.comet.worktreemanager.service

import com.comet.worktreemanager.model.WorkingTreeStatus
import com.comet.worktreemanager.model.WorktreeInfo
import com.comet.worktreemanager.model.WorktreeRow
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.io.File

/**
 * Drives `git worktree` / branch operations through the bundled git4idea command
 * API. All methods run the underlying git process synchronously, so callers must
 * invoke them off the EDT (e.g. from a background task).
 */
@Service(Service.Level.PROJECT)
class WorktreeService(private val project: Project) {

    /** Git repositories registered in the project (one per VCS root). */
    fun repositories(): List<GitRepository> =
        GitRepositoryManager.getInstance(project).repositories

    fun repositoryByRoot(root: String): GitRepository? {
        val normalized = root.trimEnd('/')
        return repositories().firstOrNull { it.root.path.trimEnd('/') == normalized }
    }

    /**
     * The unified rows for every repository: worktrees plus local branches that
     * have no worktree, each carrying upstream tracking status.
     */
    fun listRows(): List<WorktreeRow> =
        repositories()
            .flatMap { repo -> rowsFor(repo) }
            .distinctBy { (it.worktreePath ?: it.branch).orEmpty() + "@" + it.repositoryRoot }

    private fun rowsFor(repo: GitRepository): List<WorktreeRow> {
        val branches = listBranches(repo)
        val rows = WorktreeRowBuilder.build(listWorktrees(repo), branches, repo.root.path)

        val default = detectDefaultBranch(repo, branches.mapTo(mutableSetOf()) { it.name })
        val merged = default?.let { mergedBranches(repo, it.ref) } ?: emptySet()

        return rows.map { row ->
            row.copy(
                workingTree = if (row.hasWorktree && !row.isBare) workingTreeStatus(row.worktreePath!!) else null,
                isMerged = when {
                    row.branch == null || default == null -> null
                    row.branch == default.name -> null
                    else -> row.branch in merged
                },
                defaultBranch = default?.name,
            )
        }
    }

    /** Resolves the repo's default branch via `origin/HEAD`, else local main/master. */
    private fun detectDefaultBranch(repo: GitRepository, localBranches: Set<String>): DefaultBranchResolver.Result? {
        val handler = GitLineHandler(project, repo.root, GitCommand.REV_PARSE)
        handler.addParameters("--abbrev-ref", "origin/HEAD")
        val result = Git.getInstance().runCommand(handler)
        val originHead = if (result.success()) result.output.firstOrNull() else null
        return DefaultBranchResolver.resolve(originHead, localBranches)
    }

    /** Local branches whose tip is already an ancestor of [targetRef]. */
    private fun mergedBranches(repo: GitRepository, targetRef: String): Set<String> {
        val handler = GitLineHandler(project, repo.root, GitCommand.FOR_EACH_REF)
        handler.addParameters("--format=%(refname:short)", "--merged=$targetRef", "refs/heads")
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            thisLogger().warn("git for-each-ref --merged failed: ${result.errorOutputAsJoinedString}")
            return emptySet()
        }
        return result.output.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /** Working-tree dirtiness of a worktree, via `git status --porcelain`. */
    fun workingTreeStatus(worktreePath: String): WorkingTreeStatus? {
        val handler = GitLineHandler(project, File(worktreePath), GitCommand.STATUS)
        handler.addParameters("--porcelain")
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            thisLogger().warn("git status failed for $worktreePath: ${result.errorOutputAsJoinedString}")
            return null
        }
        return GitStatusParser.parse(result.output)
    }

    /** Worktrees of a single repository, via `git worktree list --porcelain`. */
    fun listWorktrees(repo: GitRepository): List<WorktreeInfo> {
        val handler = GitLineHandler(project, repo.root, GitWorktreeCommand.INSTANCE)
        handler.addParameters("list", "--porcelain")
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            thisLogger().warn("git worktree list failed: ${result.errorOutputAsJoinedString}")
            return emptyList()
        }
        return WorktreeParser.parse(result.output, repo.root.path)
    }

    /** Local branches with tracking info, via `git for-each-ref refs/heads`. */
    fun listBranches(repo: GitRepository): List<BranchRef> {
        val sep = BranchRefParser.SEP
        val format = "%(refname:short)$sep%(objectname)$sep%(upstream:short)$sep%(upstream:track)"
        val handler = GitLineHandler(project, repo.root, GitCommand.FOR_EACH_REF)
        handler.addParameters("--format=$format", "refs/heads")
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            thisLogger().warn("git for-each-ref failed: ${result.errorOutputAsJoinedString}")
            return emptyList()
        }
        return BranchRefParser.parse(result.output)
    }

    /**
     * Removes a worktree. With [force] git also removes worktrees that contain
     * uncommitted changes or untracked files.
     */
    fun remove(repo: GitRepository, worktreePath: String, force: Boolean): GitCommandResult {
        val handler = GitLineHandler(project, repo.root, GitWorktreeCommand.INSTANCE)
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
        val handler = GitLineHandler(project, repo.root, GitWorktreeCommand.INSTANCE)
        handler.addParameters("prune")
        return Git.getInstance().runCommand(handler)
    }
}
