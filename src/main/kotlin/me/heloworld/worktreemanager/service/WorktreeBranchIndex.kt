package me.heloworld.worktreemanager.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.vcs.log.VcsRef
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A fast, EDT-safe lookup of "which local branches currently have a worktree",
 * keyed by repository root. The Git Log column and the log actions read this
 * cache synchronously; the actual `git worktree list` scan runs off the EDT and
 * is refreshed whenever git state changes (GIT_REPO_CHANGE).
 *
 * The snapshot is a `rootPath -> (branchName -> worktreePath)` map. Branch names
 * are short (no `refs/heads/`), matching [VcsRef.getName] for local branches.
 */
@Service(Service.Level.PROJECT)
class WorktreeBranchIndex(private val project: Project) : Disposable {

    private val service = project.getService(WorktreeService::class.java)

    @Volatile
    private var snapshot: Map<String, Map<String, String>> = emptyMap()

    /** Guards against overlapping background scans. */
    private val scanning = AtomicBoolean(false)

    init {
        project.messageBus.connect(this).subscribe(
            GitRepository.GIT_REPO_CHANGE,
            GitRepositoryChangeListener { scheduleRefresh() },
        )
        scheduleRefresh()
    }

    /**
     * Worktree path for [branch] in the repository rooted at [rootPath], or null
     * when that branch has no worktree. [rootPath] is compared trimmed of a
     * trailing slash to match [WorktreeService] path normalization.
     */
    fun worktreePathFor(rootPath: String, branch: String): String? =
        snapshot[rootPath.trimEnd('/')]?.get(branch)

    /**
     * The first of [branches] (as reported at a commit) that is a local branch
     * with a worktree, or null when none is. Each ref is matched within its own
     * repository root, so this works across multi-root selections. Remote
     * branches and tags never match (their names carry a remote prefix and won't
     * equal a local branch key).
     */
    fun match(branches: List<VcsRef>): Match? {
        for ((root, refs) in branches.groupBy { it.root.path.trimEnd('/') }) {
            val byBranch = snapshot[root] ?: continue
            val branch = pickBranch(refs.map { it.name }, byBranch.keys) ?: continue
            return Match(root, branch, byBranch.getValue(branch))
        }
        return null
    }

    /** A branch checked out in a worktree, resolved from a log commit's refs. */
    data class Match(val repositoryRoot: String, val branch: String, val worktreePath: String)

    private fun scheduleRefresh() {
        if (!scanning.compareAndSet(false, true)) return
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                refresh()
            } finally {
                scanning.set(false)
            }
        }
    }

    /** Rebuilds the snapshot by scanning every repository's worktrees (off EDT). */
    private fun refresh() {
        val next = service.repositories().associate { repo ->
            repo.root.path.trimEnd('/') to worktreeBranchesFor(repo)
        }
        val changed = next != snapshot
        snapshot = next
        if (changed) repaintLog()
    }

    private fun worktreeBranchesFor(repo: GitRepository): Map<String, String> =
        service.listWorktrees(repo)
            .filter { !it.isBare && it.branch != null }
            .associate { it.branch!! to it.path }

    /**
     * Best-effort repaint of the main Git Log table so freshly-scanned worktree
     * icons show without waiting for the next natural repaint. Guarded because
     * the log may not be initialized yet; failure here is harmless.
     */
    private fun repaintLog() {
        ApplicationManager.getApplication().invokeLater {
            try {
                com.intellij.vcs.log.impl.VcsProjectLog.getInstance(project).mainLogUi?.table?.repaint()
            } catch (_: Throwable) {
                // Log not ready or API shape changed — icons still appear on the next repaint.
            }
        }
    }

    override fun dispose() = Unit

    companion object {
        /**
         * The first of [branchNames] present in [worktreeBranches], or null when
         * none is. This is the matching rule shared by [match]: order follows the
         * commit's refs, and a name absent from the set (e.g. a remote branch) is
         * skipped. Pure so it can be unit-tested without the platform.
         */
        fun pickBranch(branchNames: List<String>, worktreeBranches: Set<String>): String? =
            branchNames.firstOrNull { it in worktreeBranches }
    }
}
