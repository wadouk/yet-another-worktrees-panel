package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.i18n.WorktreeBundle
import com.comet.worktreemanager.model.WorktreeRow
import com.comet.worktreemanager.service.WorktreeService
import com.intellij.icons.AllIcons
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
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
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.table.JBTable
import com.intellij.vcs.log.impl.VcsLogContentUtil
import git4idea.repo.GitRepository
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import java.util.regex.Pattern
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.RowFilter
import javax.swing.ToolTipManager
import javax.swing.event.DocumentEvent
import javax.swing.table.TableRowSorter

/**
 * The "Pruning" tab content: a toolbar (Refresh / Open / Delete / Prune) and a
 * filter field above a sortable, filterable table of worktrees and branches.
 * Double-clicking a worktree row opens it in a new window. User-facing strings
 * come from [WorktreeBundle]; cell/tooltip text is built by [WorktreeRowPresenter].
 */
class WorktreePanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

    private val service = project.getService(WorktreeService::class.java)
    private val tableModel = WorktreeTableModel()
    private val sorter = TableRowSorter(tableModel)
    private val table = object : JBTable(tableModel) {
        override fun getToolTipText(e: MouseEvent): String? {
            val viewRow = rowAtPoint(e.point)
            val viewCol = columnAtPoint(e.point)
            if (viewRow < 0 || viewCol < 0) return null
            val row = tableModel.rowAt(convertRowIndexToModel(viewRow)) ?: return null
            return when (convertColumnIndexToModel(viewCol)) {
                1 -> WorktreeRowPresenter.worktreeTooltip(row)
                2 -> WorktreeRowPresenter.trackingTooltip(row)
                3 -> WorktreeRowPresenter.mergedTooltip(row)
                4 -> WorktreeRowPresenter.changesTooltip(row)
                5 -> WorktreeRowPresenter.activityTooltip(row)
                6 -> WorktreeRowPresenter.cleanupTooltip(row)
                else -> null
            }
        }
    }.apply {
        setShowGrid(false)
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        rowSorter = sorter
        // Show "loading" until the first load completes (avoids a misleading
        // "no worktrees" flash while git runs in the background).
        emptyText.text = WorktreeBundle.message("table.loading")
        ToolTipManager.sharedInstance().registerComponent(this)
        getColumnModel().getColumn(0).cellRenderer = WorktreeBranchRenderer(tableModel)
    }
    private val filterField = SearchTextField().apply {
        textEditor.emptyText.text = WorktreeBundle.message("filter.placeholder")
    }

    init {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction())
            add(OpenAction())
            add(DeleteAction())
            addSeparator()
            add(PruneAction())
        }
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("WorktreeManagerToolbar", actionGroup, true)
        toolbar.targetComponent = this

        val top = JPanel(BorderLayout())
        top.add(toolbar.component, BorderLayout.WEST)
        top.add(filterField, BorderLayout.CENTER)
        add(top, BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)

        filterField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = applyFilter()
        })

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) selected()?.let { openWorktree(it) }
            }

            // Select the row under the cursor before the context menu opens.
            override fun mousePressed(e: MouseEvent) = maybeSelectForPopup(e)
            override fun mouseReleased(e: MouseEvent) = maybeSelectForPopup(e)
        })

        val popupGroup = DefaultActionGroup().apply {
            add(OpenAction())
            add(ShowInGitLogAction())
            addSeparator()
            add(DeleteAction())
        }
        PopupHandler.installPopupMenu(table, popupGroup, "WorktreeManagerPopup")

        refresh()
    }

    /**
     * Called when the tab content is disposed (incl. dynamic plugin unload).
     * Unregisters the table from the shared ToolTipManager so no reference to
     * plugin classes lingers; the other listeners and the PopupHandler are tied
     * to the (garbage-collected) table itself.
     */
    override fun dispose() {
        ToolTipManager.sharedInstance().unregisterComponent(table)
    }

    private fun applyFilter() {
        val text = filterField.text.trim()
        sorter.rowFilter =
            if (text.isEmpty()) null
            else RowFilter.regexFilter("(?i)" + Pattern.quote(text))
    }

    private fun selected(): WorktreeRow? =
        table.selectedRow.takeIf { it >= 0 }
            ?.let { tableModel.rowAt(table.convertRowIndexToModel(it)) }

    private fun maybeSelectForPopup(e: MouseEvent) {
        if (!e.isPopupTrigger) return
        val viewRow = table.rowAtPoint(e.point)
        if (viewRow >= 0) table.setRowSelectionInterval(viewRow, viewRow)
    }

    /** Opens the Git Log tab and navigates the commit graph to the branch. */
    private fun showInGitLog(row: WorktreeRow) {
        val branch = row.branch ?: return
        VcsLogContentUtil.runInMainLog(project) { ui -> ui.vcsLog.jumpToReference(branch) }
    }

    private fun refresh() {
        runInBackground(WorktreeBundle.message("task.loading")) {
            val rows = service.listRows()
            ApplicationManager.getApplication().invokeLater {
                tableModel.setRows(rows)
                // After the first load, an empty table means there really is nothing.
                table.emptyText.text = WorktreeBundle.message("table.empty")
            }
        }
    }

    private fun openWorktree(row: WorktreeRow) {
        val path = row.worktreePath ?: return
        if (row.isCurrent) {
            Messages.showInfoMessage(
                project,
                WorktreeBundle.message("message.open.alreadyOpen"),
                WorktreeBundle.message("message.open.title"),
            )
            return
        }
        ProjectUtil.openOrImport(Path.of(path), project, /* forceOpenInNewFrame = */ true)
    }

    private fun deleteRow(row: WorktreeRow) {
        if (row.isCurrent) {
            Messages.showWarningDialog(
                project,
                WorktreeBundle.message("message.delete.currentWorktree"),
                WorktreeBundle.message("dialog.delete.title"),
            )
            return
        }
        val repo = service.repositoryByRoot(row.repositoryRoot)
        if (repo == null) {
            Messages.showErrorDialog(
                project,
                WorktreeBundle.message("message.delete.noRepo"),
                WorktreeBundle.message("dialog.delete.title"),
            )
            return
        }
        val dialog = DeleteDialog(project, row)
        if (!dialog.showAndGet()) return
        val opts = dialog.options()

        runInBackground(WorktreeBundle.message("task.deleting")) {
            if (opts.removeWorktree && row.worktreePath != null) {
                val result = service.remove(repo, row.worktreePath, opts.force)
                if (!result.success()) {
                    notifyError(WorktreeBundle.message("message.error.removeWorktree"), result.errorOutputAsJoinedString)
                    return@runInBackground
                }
            }
            if (opts.deleteBranch && row.branch != null) {
                val result = service.deleteBranch(repo, row.branch, force = opts.force)
                if (!result.success()) {
                    notifyError(WorktreeBundle.message("message.error.deleteBranch"), result.errorOutputAsJoinedString)
                }
            }
            refresh()
        }
    }

    private fun pruneAll() {
        val repos = service.repositories()
        val confirmed = Messages.showYesNoDialog(
            project,
            WorktreeBundle.message("dialog.prune.message", repos.size),
            WorktreeBundle.message("dialog.prune.title"),
            Messages.getQuestionIcon(),
        )
        if (confirmed != Messages.YES) return
        runInBackground(WorktreeBundle.message("task.pruning")) {
            repos.forEach { repo: GitRepository -> service.prune(repo) }
            refresh()
        }
    }

    private fun notifyError(title: String, detail: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(
                project,
                detail.ifBlank { WorktreeBundle.message("message.error.unknown") },
                title,
            )
        }
    }

    private fun runInBackground(title: String, action: () -> Unit) {
        object : Task.Backgroundable(project, title, false) {
            override fun run(indicator: ProgressIndicator) = action()
        }.queue()
    }

    private fun canDelete(row: WorktreeRow?): Boolean =
        row != null && !row.isCurrent && !row.isBare && (row.hasWorktree || row.hasBranch)

    // --- Toolbar actions -------------------------------------------------

    private inner class RefreshAction : AnAction(
        WorktreeBundle.message("action.refresh"),
        WorktreeBundle.message("action.refresh.desc"),
        AllIcons.Actions.Refresh,
    ) {
        override fun actionPerformed(e: AnActionEvent) = refresh()
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class OpenAction : AnAction(
        WorktreeBundle.message("action.open"),
        WorktreeBundle.message("action.open.desc"),
        AllIcons.Actions.MoveToWindow,
    ) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.let { openWorktree(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            val row = selected()
            e.presentation.isEnabled = row != null && row.hasWorktree && !row.isCurrent
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class ShowInGitLogAction : AnAction(
        WorktreeBundle.message("action.showInLog"),
        WorktreeBundle.message("action.showInLog.desc"),
        AllIcons.Vcs.Branch,
    ) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.let { showInGitLog(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = selected()?.hasBranch == true
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class DeleteAction : AnAction(
        WorktreeBundle.message("action.delete"),
        WorktreeBundle.message("action.delete.desc"),
        AllIcons.General.Remove,
    ) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.let { deleteRow(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = canDelete(selected())
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class PruneAction : AnAction(
        WorktreeBundle.message("action.prune"),
        WorktreeBundle.message("action.prune.desc"),
        AllIcons.Actions.GC,
    ) {
        override fun actionPerformed(e: AnActionEvent) = pruneAll()
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
}
