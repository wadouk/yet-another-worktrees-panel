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

/** The path chosen for the new worktree. */
data class CreateWorktreeOptions(val path: String)

/**
 * Confirmation dialog for creating a worktree for an existing branch: shows the
 * branch and a path field (prefilled with the configured base dir) with a folder
 * browse button. The branch is fixed; only the target path is editable.
 */
class CreateWorktreeDialog(
    project: Project,
    private val branch: String,
    defaultPath: String,
) : DialogWrapper(project) {

    private val pathField = TextFieldWithBrowseButton().apply {
        text = defaultPath
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        )
    }

    init {
        title = WorktreeBundle.message("dialog.create.title")
        setOKButtonText(WorktreeBundle.message("dialog.create.ok"))
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row(WorktreeBundle.message("dialog.create.branchLabel")) { label(branch).bold() }
        row(WorktreeBundle.message("dialog.create.pathLabel")) {
            cell(pathField).align(AlignX.FILL)
        }
    }

    override fun doValidate(): ValidationInfo? =
        if (pathField.text.isBlank()) {
            ValidationInfo(WorktreeBundle.message("dialog.create.pathEmpty"), pathField)
        } else {
            null
        }

    fun options(): CreateWorktreeOptions = CreateWorktreeOptions(pathField.text.trim())
}
