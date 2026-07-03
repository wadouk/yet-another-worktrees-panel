package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Dimension
import java.io.File
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

/** The new (full) path chosen for the moved worktree. */
data class MoveWorktreeOptions(val path: String)

/**
 * Confirmation dialog for moving a worktree. It asks only for the destination
 * *base directory* and keeps the worktree's own folder name, so moving is a
 * matter of picking a new parent — the folder name is appended exactly once (see
 * [WorktreePlacement.movedWorktreePath]). Prefilled with the current base dir;
 * the branch is unaffected.
 *
 * Asking for the base dir (rather than the full destination) avoids a subtle
 * `git worktree move` trap: handed a destination path that already exists as a
 * directory, git nests the worktree *inside* it, doubling the folder name. Since
 * this differs from the raw `git worktree move` behavior that seasoned users
 * expect, a live preview spells out the resolved path and whether the base will
 * be created (new tree) or already exists (moved inside it).
 */
class MoveWorktreeDialog(
    project: Project,
    private val currentPath: String,
) : DialogWrapper(project) {

    private val folderName: String = Path.of(currentPath).fileName?.toString() ?: currentPath
    private val currentBase: String = Path.of(currentPath).parent?.toString() ?: ""

    private val baseField = TextFieldWithBrowseButton().apply {
        text = currentBase
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        )
    }

    private val previewLabel = JBLabel()

    init {
        title = WorktreeBundle.message("dialog.move.title")
        setOKButtonText(WorktreeBundle.message("dialog.move.ok"))
        baseField.textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = updatePreview()
        })
        init()
        updatePreview()
    }

    override fun createCenterPanel(): JComponent = panel {
        // Vertical layout: captions sit ABOVE their value via LabelPosition.TOP, so
        // there is no left label column stealing horizontal space — the picker (the
        // one thing you edit here) spans the full dialog width, and its grey hint
        // wraps underneath instead of squeezing the field.
        row {
            label(wrapped(currentPath)).bold()
                .label(WorktreeBundle.message("dialog.move.currentLabel"), LabelPosition.TOP)
        }
        row {
            cell(baseField)
                .align(AlignX.FILL)
                .resizableColumn()
                .label(WorktreeBundle.message("dialog.move.baseLabel"), LabelPosition.TOP)
                .comment(WorktreeBundle.message("dialog.move.baseComment", folderName))
        }
        row { cell(previewLabel).align(AlignX.FILL) }
    }.apply {
        // Keep a comfortable minimum width even when every text is short; the
        // wrapped labels bound the maximum, and DialogWrapper stays resizable.
        preferredSize = Dimension(JBUI.scale(560), preferredSize.height)
    }

    /**
     * Wraps text in a fixed-width HTML body so a long absolute path breaks over
     * several lines instead of stretching the dialog to fit a single line.
     */
    private fun wrapped(text: String): String =
        "<html><body style='width:${WRAP_WIDTH}px'>${StringUtil.escapeXmlEntities(text)}</body></html>"

    /** Destination base dir with the worktree's folder name appended once. */
    private fun targetPath(): String =
        WorktreePlacement.movedWorktreePath(baseField.text.trim(), currentPath)

    /**
     * Live hint mirroring the three outcomes: an empty base clears it; a target
     * that already exists is flagged red (git would nest the folder); otherwise
     * the base either exists (worktree moved inside it) or will be created —
     * distinguished by icon and color so the resolved path is never a surprise.
     */
    private fun updatePreview() {
        val base = baseField.text.trim()
        if (base.isEmpty()) {
            previewLabel.icon = null
            previewLabel.text = ""
            return
        }
        val target = targetPath()
        when {
            File(target).exists() -> {
                previewLabel.icon = AllIcons.General.Error
                previewLabel.foreground = RED
                previewLabel.text = wrapped(WorktreeBundle.message("dialog.move.preview.exists", target))
            }
            File(base).isDirectory -> {
                previewLabel.icon = AllIcons.General.InspectionsOK
                previewLabel.foreground = GREEN
                previewLabel.text = wrapped(WorktreeBundle.message("dialog.move.preview.intoExisting", target))
            }
            else -> {
                previewLabel.icon = AllIcons.General.Information
                previewLabel.foreground = BLUE
                previewLabel.text = wrapped(WorktreeBundle.message("dialog.move.preview.create", target))
            }
        }
    }

    override fun doValidate(): ValidationInfo? = when {
        baseField.text.isBlank() ->
            ValidationInfo(WorktreeBundle.message("dialog.move.baseEmpty"), baseField)
        targetPath() == currentPath ->
            ValidationInfo(WorktreeBundle.message("dialog.move.pathSame"), baseField)
        File(targetPath()).exists() ->
            ValidationInfo(WorktreeBundle.message("dialog.move.pathExists", targetPath()), baseField)
        else -> null
    }

    fun options(): MoveWorktreeOptions = MoveWorktreeOptions(targetPath())

    private companion object {
        // Light/dark pairs so the hint stays legible in both themes.
        val GREEN = JBColor(Color(0x1A7F37), Color(0x3FB950))
        val BLUE = JBColor(Color(0x0A66C2), Color(0x4C9AFF))
        val RED = JBColor(Color(0xC7222B), Color(0xF16B6B))

        // Wrap long paths at a fixed width so the dialog stays a readable size
        // instead of stretching to fit a single-line absolute path.
        val WRAP_WIDTH get() = JBUI.scale(460)
    }
}
