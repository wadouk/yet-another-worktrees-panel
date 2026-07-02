package me.heloworld.worktreemanager.gitlog

import me.heloworld.worktreemanager.service.WorktreeBranchIndex
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.vcs.log.VcsLogDataKeys

/**
 * The worktree (if any) checked out at the branch of the commit selected in the
 * Git Log — the shared enablement/target lookup for the log actions. Returns
 * null when there is no log selection or no branch there has a worktree.
 */
internal fun worktreeMatch(e: AnActionEvent): WorktreeBranchIndex.Match? {
    val project = e.project ?: return null
    val refs = e.getData(VcsLogDataKeys.VCS_LOG_REFS) ?: return null
    return project.getService(WorktreeBranchIndex::class.java).match(refs)
}
