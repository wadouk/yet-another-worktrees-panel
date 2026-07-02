package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/** The new path chosen for the moved worktree. */
data class MoveWorktreeOptions(val path: String)

/**
 * Confirmation dialog for moving a worktree: shows its current path and a path
 * field (prefilled with the current path) with a folder browse button. Only the
 * target path is editable; the branch is unaffected.
 */
class MoveWorktreeDialog(
    project: Project,
    private val currentPath: String,
) : DialogWrapper(project) {

    private val pathField = TextFieldWithBrowseButton().apply {
        text = currentPath
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        )
    }

    init {
        title = WorktreeBundle.message("dialog.move.title")
        setOKButtonText(WorktreeBundle.message("dialog.move.ok"))
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row(WorktreeBundle.message("dialog.move.currentLabel")) { label(currentPath).bold() }
        row(WorktreeBundle.message("dialog.move.pathLabel")) {
            cell(pathField).align(AlignX.FILL)
        }
    }

    override fun doValidate(): ValidationInfo? = when {
        pathField.text.isBlank() ->
            ValidationInfo(WorktreeBundle.message("dialog.move.pathEmpty"), pathField)
        pathField.text.trim() == currentPath ->
            ValidationInfo(WorktreeBundle.message("dialog.move.pathSame"), pathField)
        else -> null
    }

    fun options(): MoveWorktreeOptions = MoveWorktreeOptions(pathField.text.trim())
}
