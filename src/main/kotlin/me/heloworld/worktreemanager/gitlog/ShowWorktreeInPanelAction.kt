package me.heloworld.worktreemanager.gitlog

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import me.heloworld.worktreemanager.toolwindow.WorktreePanelHolder
import me.heloworld.worktreemanager.toolwindow.WorktreePruningContentProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager

/**
 * Git Log context-menu action: reveal the branch at the selected commit in the
 * "Worktrees" tab — activating the tab and selecting its row. Enabled only when
 * the commit's branch has a worktree.
 */
class ShowWorktreeInPanelAction : AnAction(
    WorktreeBundle.messagePointer("action.log.showInTab"),
    WorktreeBundle.messagePointer("action.log.showInTab.desc"),
    AllIcons.Actions.MoveTo2,
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = worktreeMatch(e) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val match = worktreeMatch(e) ?: return
        // Selecting the content lazily creates the panel (registering it in the
        // holder). Defer row selection so that has happened; the panel also
        // remembers a pending selection until its background row load finishes.
        ChangesViewContentManager.getInstance(project)
            .selectContent(WorktreePruningContentProvider.TAB_NAME)
        ApplicationManager.getApplication().invokeLater {
            project.getService(WorktreePanelHolder::class.java).panel
                ?.selectRow(match.repositoryRoot, match.branch)
        }
    }
}
