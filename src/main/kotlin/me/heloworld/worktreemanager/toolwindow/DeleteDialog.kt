package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import me.heloworld.worktreemanager.model.WorktreeRow
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
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
 *  - worktree + branch  -> remove worktree (always), delete branch (opt-in)
 *  - branch only        -> delete branch
 *  - detached worktree  -> remove worktree
 *
 * A single "Force" checkbox covers both git flags at once (`worktree remove
 * --force` and `branch -D`): the panel treats a worktree and its branch as two
 * views of one thing, so the dialog does too. Its enabled state and its label
 * both follow from [DeleteRules].
 *
 * The current worktree and the bare entry are never deletable (guarded upstream).
 */
class DeleteDialog(
    project: Project,
    private val row: WorktreeRow,
) : DialogWrapper(project) {

    private val canRemoveWorktree = DeleteRules.canRemoveWorktree(row)
    private val canDeleteBranch = DeleteRules.canDeleteBranch(row)

    private var removeWorktree = canRemoveWorktree
    // Branch-only rows delete the branch by default; when a worktree exists the
    // branch deletion is opt-in (you usually just want to drop the worktree).
    private var deleteBranch = canDeleteBranch && !canRemoveWorktree
    private var force = false

    /** Held so toggling the branch checkbox can re-evaluate the force checkbox. */
    private var forceCheckBox: JBCheckBox? = null

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
            // Worktree + branch: branch removal is optional, and it is also what
            // decides whether forcing can do anything on a clean worktree.
            canDeleteBranch && canRemoveWorktree -> row {
                checkBox(WorktreeBundle.message("dialog.delete.deleteBranch", row.branch ?: ""))
                    .applyToComponent {
                        addActionListener {
                            deleteBranch = isSelected
                            syncForceCheckBox()
                        }
                    }
            }
            // Branch only: branch removal is the action itself.
            canDeleteBranch -> {
                row { label(WorktreeBundle.message("dialog.delete.branchLabel")) }
                row { label(row.branch ?: "").bold() }
            }
        }

        row {
            // Disabled while it would be inert — a clean worktree with no branch
            // deletion queued has nothing to force.
            checkBox(forceLabel())
                .enabled(DeleteRules.forceIsUseful(row, deleteBranch))
                .applyToComponent {
                    forceCheckBox = this
                    addActionListener { force = isSelected }
                }
        }
    }

    /** Names what a "Force" tick would actually do for this row. */
    private fun forceLabel(): String = when (DeleteRules.forceScope(row)) {
        ForceScope.BOTH -> WorktreeBundle.message("dialog.delete.force.both")
        ForceScope.WORKTREE -> WorktreeBundle.message("dialog.delete.force.worktree")
        ForceScope.BRANCH -> WorktreeBundle.message("dialog.delete.force.branch")
    }

    /**
     * Re-enables or greys out the force checkbox after the branch toggle moved.
     * Clearing it when it goes inert avoids a ticked-but-disabled box on screen;
     * `setSelected` fires no ActionEvent, so [force] is reset by hand.
     */
    private fun syncForceCheckBox() {
        val box = forceCheckBox ?: return
        val useful = DeleteRules.forceIsUseful(row, deleteBranch)
        box.isEnabled = useful
        if (!useful) {
            box.isSelected = false
            force = false
        }
    }

    fun options(): DeleteOptions = DeleteOptions(
        removeWorktree = removeWorktree,
        deleteBranch = deleteBranch,
        force = force && DeleteRules.forceIsUseful(row, deleteBranch),
    )
}
