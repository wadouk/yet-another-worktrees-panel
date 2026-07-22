package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color
import javax.swing.JTable

/**
 * Branch column renderer. Two decorations flag the row at a glance:
 *  - a padlock icon on any worktree — closed when the worktree is locked
 *    (`git worktree lock`), open otherwise; branch-only rows get none;
 *  - a yellow "HEAD" pill on the worktree open in this IDE window.
 */
class WorktreeBranchRenderer(private val model: WorktreeTableModel) : ColoredTableCellRenderer() {

    private val headBg = JBColor(Color(0xFFF1B8), Color(0x6E5C16))
    private val headFg = JBColor(Color(0x6E5916), Color(0xF2D784))

    override fun customizeCellRenderer(
        table: JTable,
        value: Any?,
        selected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ) {
        val modelRow = row.takeIf { it >= 0 }?.let { model.rowAt(table.convertRowIndexToModel(it)) }
        if (modelRow?.hasWorktree == true) {
            icon = if (modelRow.isLocked) AllIcons.Ide.Readonly else AllIcons.Ide.Readwrite
        }
        if (modelRow?.isCurrent == true) {
            append(
                " ${WorktreeBundle.message("branch.headTag")} ",
                SimpleTextAttributes(headBg, headFg, null, SimpleTextAttributes.STYLE_SMALLER),
            )
            append("  ")
        }
        append(value?.toString().orEmpty(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
}
