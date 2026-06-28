package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.model.WorktreeRow
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
        title = "Delete"
        setOKButtonText("Delete")
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        if (canRemoveWorktree) {
            row { label("Remove worktree:") }
            row { label(row.worktreePath ?: "").bold() }
        }

        when {
            // Worktree + branch: branch removal is optional.
            canDeleteBranch && canRemoveWorktree -> row {
                checkBox("Also delete branch '${row.branch}'")
                    .applyToComponent { addActionListener { deleteBranch = isSelected } }
            }
            // Branch only: branch removal is the action itself.
            canDeleteBranch -> {
                row { label("Delete branch:") }
                row { label(row.branch ?: "").bold() }
            }
        }

        row {
            val text = if (canRemoveWorktree) {
                "Force (discard uncommitted changes / untracked files)"
            } else {
                "Force (-D, delete even if the branch is not fully merged)"
            }
            checkBox(text).applyToComponent { addActionListener { force = isSelected } }
        }
    }

    fun options(): DeleteOptions = DeleteOptions(
        removeWorktree = removeWorktree,
        deleteBranch = deleteBranch,
        force = force,
    )
}
