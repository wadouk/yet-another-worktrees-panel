package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import me.heloworld.worktreemanager.model.WorktreeRow
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/** What the delete action should do, decided from the row + user toggles. */
data class DeleteOptions(
    val removeWorktree: Boolean,
    val deleteBranch: Boolean,
    val force: Boolean,
)

/**
 * Adaptive confirmation dialog. It only offers the operations that apply to the
 * selected row:
 *  - worktree + branch  → remove worktree (always), delete branch (opt-in)
 *  - branch only        → delete branch
 *  - detached worktree  → remove worktree
 *
 * The current worktree and the bare entry are never deletable (guarded upstream).
 */
class DeleteDialog(
    project: Project,
    private val row: WorktreeRow,
) : DialogWrapper(project) {

    private val canRemoveWorktree = row.hasWorktree && !row.isBare && !row.isCurrent
    private val canDeleteBranch = row.hasBranch && !row.isCurrent

    private var removeWorktree = canRemoveWorktree
    // Branch-only rows delete the branch by default; when a worktree exists the
    // branch deletion is opt-in (you usually just want to drop the worktree).
    private var deleteBranch = canDeleteBranch && !canRemoveWorktree
    private var force = false

    init {
        title = WorktreeBundle.message("dialog.delete.title")
        setOKButtonText(WorktreeBundle.message("dialog.delete.ok"))
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        if (canRemoveWorktree) {
            row { label(WorktreeBundle.message("dialog.delete.removeWorktree")) }
            row { label(row.worktreePath ?: "").bold() }
        }

        when {
            // Worktree + branch: branch removal is optional.
            canDeleteBranch && canRemoveWorktree -> row {
                checkBox(WorktreeBundle.message("dialog.delete.deleteBranch", row.branch ?: ""))
                    .applyToComponent { addActionListener { deleteBranch = isSelected } }
            }
            // Branch only: branch removal is the action itself.
            canDeleteBranch -> {
                row { label(WorktreeBundle.message("dialog.delete.branchLabel")) }
                row { label(row.branch ?: "").bold() }
            }
        }

        row {
            val text = if (canRemoveWorktree) {
                WorktreeBundle.message("dialog.delete.force.worktree")
            } else {
                WorktreeBundle.message("dialog.delete.force.branch")
            }
            // A clean worktree has nothing to discard, so forcing its removal is
            // pointless — keep the checkbox visible but disabled.
            val cleanWorktree = canRemoveWorktree && row.workingTree?.isClean == true
            checkBox(text)
                .enabled(!cleanWorktree)
                .applyToComponent { addActionListener { force = isSelected } }
        }
    }

    fun options(): DeleteOptions = DeleteOptions(
        removeWorktree = removeWorktree,
        deleteBranch = deleteBranch,
        force = force,
    )
}
