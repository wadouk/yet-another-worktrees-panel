package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.model.WorktreeInfo
import com.comet.worktreemanager.service.WorktreeService
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import git4idea.repo.GitRepository
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import com.intellij.icons.AllIcons

/**
 * The Worktrees tool window content: a toolbar (Refresh / Open / Remove / Prune)
 * above a table of worktrees. Double-clicking a row opens it in a new window.
 */
class WorktreePanel(private val project: Project) : JPanel(BorderLayout()) {

    private val service = project.getService(WorktreeService::class.java)
    private val tableModel = WorktreeTableModel()
    private val table = JBTable(tableModel).apply {
        setShowGrid(false)
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        emptyText.text = "No worktrees found"
    }
    private val emptyLabel = JBLabel("This project has no Git repository.", JBLabel.CENTER)

    init {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction())
            add(OpenAction())
            add(RemoveAction())
            addSeparator()
            add(PruneAction())
        }
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("WorktreeManagerToolbar", actionGroup, true)
        toolbar.targetComponent = this
        add(toolbar.component, BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
        emptyLabel.border = JBUI.Borders.empty(8)

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) selected()?.let { openWorktree(it) }
            }
        })

        refresh()
    }

    private fun selected(): WorktreeInfo? =
        table.selectedRow.takeIf { it >= 0 }?.let { tableModel.rowAt(table.convertRowIndexToModel(it)) }

    private fun refresh() {
        runInBackground("Loading worktrees") {
            val worktrees = service.listAll()
            ApplicationManager.getApplication().invokeLater {
                tableModel.setRows(worktrees)
            }
        }
    }

    private fun openWorktree(wt: WorktreeInfo) {
        if (wt.isCurrent) {
            Messages.showInfoMessage(project, "This worktree is already open.", "Open Worktree")
            return
        }
        ProjectUtil.openOrImport(Path.of(wt.path), project, /* forceOpenInNewFrame = */ true)
    }

    private fun removeWorktree(wt: WorktreeInfo) {
        if (wt.isCurrent) {
            Messages.showWarningDialog(
                project,
                "You cannot remove the worktree currently open in this window.",
                "Remove Worktree",
            )
            return
        }
        val repo = service.repositoryFor(wt.path)
        if (repo == null) {
            Messages.showErrorDialog(project, "Could not find the repository owning this worktree.", "Remove Worktree")
            return
        }
        val dialog = RemoveWorktreeDialog(project, wt)
        if (!dialog.showAndGet()) return
        val opts = dialog.options()

        runInBackground("Removing worktree") {
            val removeResult = service.remove(repo, wt.path, opts.force)
            if (!removeResult.success()) {
                notifyError("Failed to remove worktree", removeResult.errorOutputAsJoinedString)
                return@runInBackground
            }
            if (opts.deleteBranch && wt.branch != null) {
                val branchResult = service.deleteBranch(repo, wt.branch, force = opts.force)
                if (!branchResult.success()) {
                    notifyError("Worktree removed, but branch deletion failed", branchResult.errorOutputAsJoinedString)
                }
            }
            refresh()
        }
    }

    private fun pruneAll() {
        runInBackground("Pruning worktrees") {
            service.repositories().forEach { repo: GitRepository -> service.prune(repo) }
            refresh()
        }
    }

    private fun notifyError(title: String, detail: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(project, detail.ifBlank { "Unknown git error." }, title)
        }
    }

    private fun runInBackground(title: String, action: () -> Unit) {
        object : Task.Backgroundable(project, title, false) {
            override fun run(indicator: ProgressIndicator) = action()
        }.queue()
    }

    // --- Toolbar actions -------------------------------------------------

    private inner class RefreshAction :
        AnAction("Refresh", "Reload the worktree list", AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) = refresh()
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class OpenAction :
        AnAction("Open in New Window", "Open the selected worktree in a new IDE window", AllIcons.Actions.MoveToWindow) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.let { openWorktree(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = selected()?.isCurrent == false
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class RemoveAction :
        AnAction("Remove", "Remove the selected worktree", AllIcons.General.Remove) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.let { removeWorktree(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            val wt = selected()
            e.presentation.isEnabled = wt != null && !wt.isCurrent && !wt.isBare
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class PruneAction :
        AnAction("Prune", "Prune worktree metadata for directories that no longer exist", AllIcons.Actions.GC) {
        override fun actionPerformed(e: AnActionEvent) = pruneAll()
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
}
