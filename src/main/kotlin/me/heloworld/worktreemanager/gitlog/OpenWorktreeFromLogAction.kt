package me.heloworld.worktreemanager.gitlog

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import java.nio.file.Path

/**
 * Git Log context-menu action: open the worktree of the branch at the selected
 * commit in an IDE window — or just focus that window when it is already open.
 * Enabled only when the commit's branch actually has a worktree.
 */
class OpenWorktreeFromLogAction : AnAction(
    WorktreeBundle.messagePointer("action.log.open"),
    WorktreeBundle.messagePointer("action.log.open.desc"),
    AllIcons.Actions.MoveToWindow,
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = worktreeMatch(e) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val path = worktreeMatch(e)?.worktreePath ?: return
        // openOrImport / focus are slow ops; run off the EDT.
        object : Task.Backgroundable(project, WorktreeBundle.message("message.open.title"), false) {
            override fun run(indicator: ProgressIndicator) {
                val target = Path.of(path)
                // findAndFocusExistingProjectForPath focuses and returns the window
                // if this worktree is already open; otherwise we open it fresh.
                if (ProjectUtil.findAndFocusExistingProjectForPath(target) == null) {
                    ProjectUtil.openOrImport(target, project, /* forceOpenInNewFrame = */ true)
                }
            }
        }.queue()
    }
}
