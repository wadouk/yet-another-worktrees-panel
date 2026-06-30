package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color
import javax.swing.JTable

/**
 * Branch column renderer: the current worktree's branch is prefixed with a yellow
 * "HEAD" pill, so the worktree open in this IDE window stands out at a glance.
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
