package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import me.heloworld.worktreemanager.model.WorktreeRow
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/** Options for deleting several rows at once. */
data class BatchDeleteOptions(val deleteBranches: Boolean, val force: Boolean)

/**
 * Confirmation dialog for deleting multiple rows at once. Lists the affected
 * items; offers "also delete branches" (only relevant for worktree rows that
 * have a branch — branch-only rows always delete their branch) and "force".
 */
class BatchDeleteDialog(
    project: Project,
    private val rows: List<WorktreeRow>,
) : DialogWrapper(project) {

    private val hasOptionalBranch = rows.any { it.hasWorktree && it.hasBranch }

    private var deleteBranches = false
    private var force = false

    init {
        title = WorktreeBundle.message("dialog.delete.title")
        setOKButtonText(WorktreeBundle.message("dialog.delete.ok"))
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row { label(WorktreeBundle.message("dialog.delete.batch.header", rows.size)) }
        rows.take(LIST_LIMIT).forEach { r ->
            row { label("• ${itemLabel(r)}") }
        }
        if (rows.size > LIST_LIMIT) {
            row { label("…") }
        }
        if (hasOptionalBranch) {
            row {
                checkBox(WorktreeBundle.message("dialog.delete.batch.deleteBranches"))
                    .applyToComponent { addActionListener { deleteBranches = isSelected } }
            }
        }
        row {
            checkBox(WorktreeBundle.message("dialog.delete.force.batch"))
                .applyToComponent { addActionListener { force = isSelected } }
        }
    }

    private fun itemLabel(row: WorktreeRow): String {
        val ref = WorktreeRowPresenter.branch(row)
        return if (row.hasWorktree) "$ref — ${WorktreeRowPresenter.worktree(row)}" else ref
    }

    fun options(): BatchDeleteOptions = BatchDeleteOptions(deleteBranches = deleteBranches, force = force)

    private companion object {
        const val LIST_LIMIT = 15
    }
}
