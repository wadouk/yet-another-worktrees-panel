package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.model.WorktreeInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/** Result of the remove dialog. */
data class RemoveOptions(val force: Boolean, val deleteBranch: Boolean)

/**
 * Confirmation dialog for removing a worktree. Offers a force toggle (for dirty
 * worktrees) and an optional "also delete the branch" toggle. The branch option
 * is disabled when there is no branch to delete or it is the current worktree.
 */
class RemoveWorktreeDialog(
    project: Project,
    private val worktree: WorktreeInfo,
) : DialogWrapper(project) {

    private var force = false
    private var deleteBranch = false

    private val branchDeletable = worktree.branch != null && !worktree.isCurrent

    init {
        title = "Remove Worktree"
        setOKButtonText("Remove")
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            label("Remove worktree at:")
        }
        row {
            label(worktree.path).bold()
        }
        row {
            checkBox("Force (discard uncommitted changes / untracked files)")
                .applyToComponent { addActionListener { force = isSelected } }
        }
        if (branchDeletable) {
            row {
                checkBox("Also delete branch '${worktree.branch}'")
                    .applyToComponent { addActionListener { deleteBranch = isSelected } }
            }
        }
    }

    fun options(): RemoveOptions = RemoveOptions(force = force, deleteBranch = deleteBranch)
}
