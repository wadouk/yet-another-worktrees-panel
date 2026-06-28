package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.i18n.WorktreeBundle
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color
import javax.swing.JTable

/**
 * Branch column renderer: the current worktree's branch is shown in bold with a
 * yellow "HEAD" tag, mirroring the bundled Git branches UI.
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
        val text = value?.toString().orEmpty()
        val modelRow = row.takeIf { it >= 0 }?.let { model.rowAt(table.convertRowIndexToModel(it)) }
        if (modelRow == null) {
            append(text)
            return
        }
        if (modelRow.isCurrent) {
            append(text, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            append("  ")
            append(
                " ${WorktreeBundle.message("branch.headTag")} ",
                SimpleTextAttributes(headBg, headFg, null, SimpleTextAttributes.STYLE_SMALLER),
            )
        } else {
            append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
    }
}
