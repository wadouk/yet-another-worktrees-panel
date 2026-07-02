package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import me.heloworld.worktreemanager.model.WorktreeRow
import me.heloworld.worktreemanager.service.WorktreeService
import me.heloworld.worktreemanager.settings.WorktreeSettings
import com.intellij.icons.AllIcons
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ide.CopyPasteManager
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
import git4idea.repo.GitRepositoryChangeListener
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.awt.event.HierarchyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
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
        selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
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
    /** Guards against overlapping background scans when refresh triggers pile up. */
    private val refreshing = AtomicBoolean(false)

    init {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction())
            add(OpenAction())
            add(CreateWorktreeAction())
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
            add(CreateWorktreeAction())
            add(ShowInGitLogAction())
            addSeparator()
            add(CopyBranchAction())
            add(CopyPathAction())
            addSeparator()
            add(MoveWorktreeAction())
            add(DeleteAction())
        }
        PopupHandler.installPopupMenu(table, popupGroup, "WorktreeManagerPopup")

        // Keep the list current from two angles:
        //  - GIT_REPO_CHANGE reflects in-IDE git activity (checkout, worktree
        //    create/delete via the plugin) as soon as it happens;
        //  - becoming visible re-scans on tab selection, which catches worktrees
        //    added/removed from an external terminal that git4idea hasn't noticed.
        project.messageBus.connect(this).subscribe(
            GitRepository.GIT_REPO_CHANGE,
            GitRepositoryChangeListener { refresh() },
        )
        addHierarchyListener { e ->
            if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && isShowing) refresh()
        }

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

    private fun selectedRows(): List<WorktreeRow> =
        table.selectedRows.toList().mapNotNull { viewIndex ->
            tableModel.rowAt(table.convertRowIndexToModel(viewIndex))
        }

    private fun maybeSelectForPopup(e: MouseEvent) {
        if (!e.isPopupTrigger) return
        val viewRow = table.rowAtPoint(e.point)
        // Keep an existing multi-selection when right-clicking inside it;
        // otherwise select just the row under the cursor.
        if (viewRow >= 0 && !table.isRowSelected(viewRow)) {
            table.setRowSelectionInterval(viewRow, viewRow)
        }
    }

    private fun copyToClipboard(text: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    /** Opens the Git Log tab and navigates the commit graph to the branch. */
    private fun showInGitLog(row: WorktreeRow) {
        val branch = row.branch ?: return
        VcsLogContentUtil.runInMainLog(project) { ui -> ui.vcsLog.jumpToReference(branch) }
    }

    private fun refresh() {
        // Coalesce bursts (repeated GIT_REPO_CHANGE, docking flaps) so we don't
        // stack redundant background git scans; the flag clears once git returns.
        if (!refreshing.compareAndSet(false, true)) return
        runInBackground(WorktreeBundle.message("task.loading")) {
            try {
                val rows = service.listRows()
                ApplicationManager.getApplication().invokeLater {
                    tableModel.setRows(rows)
                    // After the first load, an empty table means there really is nothing.
                    table.emptyText.text = WorktreeBundle.message("table.empty")
                }
            } finally {
                refreshing.set(false)
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

    /** Creates a worktree for a branch that has none, then opens it in a new window. */
    private fun createWorktree(row: WorktreeRow) {
        val branch = row.branch ?: return
        val repo = service.repositoryByRoot(row.repositoryRoot) ?: run {
            Messages.showErrorDialog(
                project,
                WorktreeBundle.message("message.create.noRepo"),
                WorktreeBundle.message("dialog.create.title"),
            )
            return
        }
        val base = WorktreePlacement.baseDir(
            WorktreeSettings.getInstance(project).worktreeBaseDir,
            repo.root.path,
        )
        val dialog = CreateWorktreeDialog(project, branch, WorktreePlacement.defaultWorktreePath(base, branch))
        if (!dialog.showAndGet()) return
        val path = dialog.options().path
        runInBackground(WorktreeBundle.message("task.creating")) {
            val result = service.addWorktree(repo, path, branch)
            if (!result.success()) {
                notifyError(
                    WorktreeBundle.message("dialog.create.title"),
                    WorktreeBundle.message("message.error.create", result.errorOutputAsJoinedString),
                )
                return@runInBackground
            }
            // openOrImport is a slow op; run it here on the background task (it
            // handles its own threading) rather than pushing it onto the EDT.
            ProjectUtil.openOrImport(Path.of(path), project, /* forceOpenInNewFrame = */ true)
            refresh()
        }
    }

    /** Moves the selected worktree to a new location chosen in a dialog. */
    private fun moveWorktree(row: WorktreeRow) {
        val fromPath = row.worktreePath ?: return
        val repo = service.repositoryByRoot(row.repositoryRoot) ?: run {
            Messages.showErrorDialog(
                project,
                WorktreeBundle.message("message.move.noRepo"),
                WorktreeBundle.message("dialog.move.title"),
            )
            return
        }
        val dialog = MoveWorktreeDialog(project, fromPath)
        if (!dialog.showAndGet()) return
        val toPath = dialog.options().path
        runInBackground(WorktreeBundle.message("task.moving")) {
            val result = service.move(repo, fromPath, toPath)
            if (!result.success()) {
                notifyError(
                    WorktreeBundle.message("dialog.move.title"),
                    WorktreeBundle.message("message.error.move", result.errorOutputAsJoinedString),
                )
                return@runInBackground
            }
            // Moving the worktree this window has open leaves it on a dead path,
            // so reopen at the new location; otherwise just refresh the list.
            if (row.isCurrent) reopenAt(toPath) else refresh()
        }
    }

    /**
     * Reopens the moved current worktree in this window. `openOrImport`'s second
     * argument is the project to close: with `forceOpenInNewFrame = false` the
     * platform reuses this frame and closes the stale project itself, in the right
     * order — a manual `closeAndDispose` raced with background services still
     * touching the just-disposed project.
     */
    private fun reopenAt(path: String) {
        ProjectUtil.openOrImport(Path.of(path), project, /* forceOpenInNewFrame = */ false)
    }

    /** Entry point for the Delete action: one adaptive dialog, or a batch one. */
    private fun deleteSelected() {
        val rows = selectedRows().filter { canDelete(it) }
        when {
            rows.isEmpty() -> return
            rows.size == 1 -> deleteSingle(rows.first())
            else -> deleteBatch(rows)
        }
    }

    private fun deleteSingle(row: WorktreeRow) {
        val repo = service.repositoryByRoot(row.repositoryRoot) ?: run {
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
            val error = performDelete(repo, row, opts.removeWorktree, opts.deleteBranch, opts.force)
            if (error != null) notifyError(WorktreeBundle.message("dialog.delete.title"), error)
            refresh()
        }
    }

    private fun deleteBatch(rows: List<WorktreeRow>) {
        val dialog = BatchDeleteDialog(project, rows)
        if (!dialog.showAndGet()) return
        val opts = dialog.options()
        runInBackground(WorktreeBundle.message("task.deleting")) {
            val errors = rows.mapNotNull { row ->
                val repo = service.repositoryByRoot(row.repositoryRoot) ?: return@mapNotNull null
                performDelete(
                    repo = repo,
                    row = row,
                    removeWorktree = row.hasWorktree && !row.isBare,
                    // Branch-only rows always delete the branch; worktree rows only when opted in.
                    deleteBranch = row.hasBranch && (!row.hasWorktree || opts.deleteBranches),
                    force = opts.force,
                )
            }
            if (errors.isNotEmpty()) {
                notifyError(
                    WorktreeBundle.message("dialog.delete.title"),
                    WorktreeBundle.message("message.error.batch", errors.joinToString("\n")),
                )
            }
            refresh()
        }
    }

    /** Performs the git ops for one row; returns an error line, or null on success. */
    private fun performDelete(
        repo: GitRepository,
        row: WorktreeRow,
        removeWorktree: Boolean,
        deleteBranch: Boolean,
        force: Boolean,
    ): String? {
        val label = row.branch ?: row.worktreePath ?: "?"
        if (removeWorktree && row.worktreePath != null) {
            val result = service.remove(repo, row.worktreePath, force)
            if (!result.success()) return "$label: ${result.errorOutputAsJoinedString}"
        }
        if (deleteBranch && row.branch != null) {
            val result = service.deleteBranch(repo, row.branch, force = force)
            if (!result.success()) return "$label: ${result.errorOutputAsJoinedString}"
        }
        return null
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
            e.presentation.isEnabled =
                table.selectedRowCount == 1 && row != null && row.hasWorktree && !row.isCurrent
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class CreateWorktreeAction : AnAction(
        WorktreeBundle.message("action.create"),
        WorktreeBundle.message("action.create.desc"),
        AllIcons.General.Add,
    ) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.let { createWorktree(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            val row = selected()
            e.presentation.isEnabled = table.selectedRowCount == 1 && row != null &&
                !row.hasWorktree && row.hasBranch && !row.isCurrent && !row.isBare
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
            e.presentation.isEnabled = table.selectedRowCount == 1 && selected()?.hasBranch == true
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class CopyBranchAction : AnAction(
        WorktreeBundle.message("action.copyBranch"),
        WorktreeBundle.message("action.copyBranch.desc"),
        AllIcons.Actions.Copy,
    ) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.branch?.let { copyToClipboard(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = table.selectedRowCount == 1 && selected()?.hasBranch == true
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class CopyPathAction : AnAction(
        WorktreeBundle.message("action.copyPath"),
        WorktreeBundle.message("action.copyPath.desc"),
        AllIcons.Actions.Copy,
    ) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.worktreePath?.let { copyToClipboard(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = table.selectedRowCount == 1 && selected()?.hasWorktree == true
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class MoveWorktreeAction : AnAction(
        WorktreeBundle.message("action.move"),
        WorktreeBundle.message("action.move.desc"),
        AllIcons.Actions.MoveTo2,
    ) {
        override fun actionPerformed(e: AnActionEvent) = selected()?.let { moveWorktree(it) } ?: Unit
        override fun update(e: AnActionEvent) {
            val row = selected()
            e.presentation.isEnabled = table.selectedRowCount == 1 && row != null &&
                row.hasWorktree && !row.isBare && !row.isMain
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class DeleteAction : AnAction(
        WorktreeBundle.message("action.delete"),
        WorktreeBundle.message("action.delete.desc"),
        AllIcons.General.Remove,
    ) {
        override fun actionPerformed(e: AnActionEvent) = deleteSelected()
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = selectedRows().any { canDelete(it) }
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
