package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.model.WorktreeRow
import com.comet.worktreemanager.service.RelativeTime
import com.comet.worktreemanager.service.WorktreeService
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
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
 * The Worktrees tool window content: a toolbar (Refresh / Open / Delete / Prune)
 * and a filter field above a sortable, filterable table of worktrees and
 * branches. Double-clicking a worktree row opens it in a new window.
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
                2 -> trackingTooltip(row)
                3 -> mergedTooltip(row)
                4 -> changesTooltip(row)
                5 -> activityTooltip(row)
                6 -> statusTooltip(row)
                else -> null
            }
        }
    }.apply {
        setShowGrid(false)
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        rowSorter = sorter
        emptyText.text = "No worktrees or branches found"
        ToolTipManager.sharedInstance().registerComponent(this)
    }
    private val filterField = SearchTextField().apply {
        textEditor.emptyText.text = "Filter branch / path / status"
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
     * Called when the tool window content is disposed (incl. dynamic plugin
     * unload). Unregisters the table from the shared ToolTipManager so no
     * reference to plugin classes lingers; the other listeners and the
     * PopupHandler are tied to the (garbage-collected) table itself.
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

    private fun trackingTooltip(row: WorktreeRow): String = when {
        row.upstream == null -> "No upstream branch configured"
        row.isGone -> "Upstream '${row.upstream}' is gone (deleted on the remote)"
        row.ahead == 0 && row.behind == 0 -> "Up to date with '${row.upstream}'"
        else -> buildString {
            append("Relative to '${row.upstream}': ")
            append(
                buildList {
                    if (row.ahead > 0) add("${row.ahead} ahead")
                    if (row.behind > 0) add("${row.behind} behind")
                }.joinToString(", "),
            )
        }
    }

    private fun mergedTooltip(row: WorktreeRow): String {
        if (row.branch == null) return "Not a branch"
        val default = row.defaultBranch ?: return "Default branch is unknown"
        return when {
            row.branch == default -> "This is the default branch ('$default')"
            row.isMerged == true -> "Merged into the default branch ('$default')"
            row.isMerged == false -> "Not yet merged into the default branch ('$default')"
            else -> "Merge status unavailable"
        }
    }

    private fun activityTooltip(row: WorktreeRow): String {
        val millis = row.lastActivityMillis ?: return "No activity information"
        val relative = RelativeTime.format(millis, System.currentTimeMillis())
        return if (row.lastActivityIsFile) {
            "Most recent uncommitted change ($relative)"
        } else {
            "Last commit ($relative)"
        }
    }

    private fun changesTooltip(row: WorktreeRow): String {
        if (!row.hasWorktree) return "No worktree (branch only)"
        val s = row.workingTree ?: return "Working-tree status unavailable"
        if (s.isClean) return "Working tree is clean"
        return buildList {
            if (s.staged > 0) add("${s.staged} staged")
            if (s.modified > 0) add("${s.modified} modified")
            if (s.untracked > 0) add("${s.untracked} untracked")
            if (s.conflicted > 0) add("${s.conflicted} conflicted")
        }.joinToString(", ")
    }

    private fun statusTooltip(row: WorktreeRow): String {
        val parts = buildList {
            if (row.isCurrent) add("Currently open in this window")
            if (!row.hasWorktree && row.hasBranch) add("Local branch with no worktree checked out")
            if (row.isLocked) add("Worktree is locked")
            if (row.isPrunable) add("Worktree directory is missing — prunable")
            if (row.isBare) add("Bare repository entry")
        }
        return if (parts.isEmpty()) "No special status" else parts.joinToString("; ")
    }

    private fun refresh() {
        runInBackground("Loading worktrees") {
            val rows = service.listRows()
            ApplicationManager.getApplication().invokeLater {
                tableModel.setRows(rows)
            }
        }
    }

    private fun openWorktree(row: WorktreeRow) {
        val path = row.worktreePath ?: return
        if (row.isCurrent) {
            Messages.showInfoMessage(project, "This worktree is already open.", "Open Worktree")
            return
        }
        ProjectUtil.openOrImport(Path.of(path), project, /* forceOpenInNewFrame = */ true)
    }

    private fun deleteRow(row: WorktreeRow) {
        if (row.isCurrent) {
            Messages.showWarningDialog(
                project,
                "You cannot delete the worktree currently open in this window.",
                "Delete",
            )
            return
        }
        val repo = service.repositoryByRoot(row.repositoryRoot)
        if (repo == null) {
            Messages.showErrorDialog(project, "Could not find the owning repository.", "Delete")
            return
        }
        val dialog = DeleteDialog(project, row)
        if (!dialog.showAndGet()) return
        val opts = dialog.options()

        runInBackground("Deleting") {
            if (opts.removeWorktree && row.worktreePath != null) {
                val result = service.remove(repo, row.worktreePath, opts.force)
                if (!result.success()) {
                    notifyError("Failed to remove worktree", result.errorOutputAsJoinedString)
                    return@runInBackground
                }
            }
            if (opts.deleteBranch && row.branch != null) {
                val result = service.deleteBranch(repo, row.branch, force = opts.force)
                if (!result.success()) {
                    notifyError("Branch deletion failed", result.errorOutputAsJoinedString)
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

    private fun canDelete(row: WorktreeRow?): Boolean =
        row != null && !row.isCurrent && !row.isBare &&
            ((row.hasWorktree) || row.hasBranch)

    // --- Toolbar actions -------------------------------------------------

    private inner class RefreshAction :
        AnAction("Refresh", "Reload worktrees and branches", AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) = refresh()
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class OpenAction :
        AnAction("Open in New Window", "Open the selected worktree in a new IDE window", AllIcons.Actions.MoveToWindow) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.let { openWorktree(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            val row = selected()
            e.presentation.isEnabled = row != null && row.hasWorktree && !row.isCurrent
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class ShowInGitLogAction :
        AnAction("Show in Git Log", "Show the selected branch in the Git Log graph", AllIcons.Vcs.Branch) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.let { showInGitLog(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = selected()?.hasBranch == true
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class DeleteAction :
        AnAction("Delete", "Remove the selected worktree and/or branch", AllIcons.General.Remove) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.let { deleteRow(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = canDelete(selected())
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class PruneAction :
        AnAction("Prune", "Prune worktree metadata for directories that no longer exist", AllIcons.Actions.GC) {
        override fun actionPerformed(e: AnActionEvent) = pruneAll()
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
}
