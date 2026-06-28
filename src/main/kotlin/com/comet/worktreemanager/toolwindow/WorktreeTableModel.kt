package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.i18n.WorktreeBundle
import com.comet.worktreemanager.model.WorktreeRow
import javax.swing.table.AbstractTableModel

/** Backs the table: Branch / Worktree / Tracking / Merged / Changes / Activity / Status. */
class WorktreeTableModel : AbstractTableModel() {

    private var rows: List<WorktreeRow> = emptyList()

    fun setRows(newRows: List<WorktreeRow>) {
        rows = newRows
        fireTableDataChanged()
    }

    fun rowAt(index: Int): WorktreeRow? = rows.getOrNull(index)

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = 7

    override fun getColumnName(column: Int): String = when (column) {
        0 -> WorktreeBundle.message("column.branch")
        1 -> WorktreeBundle.message("column.worktree")
        2 -> WorktreeBundle.message("column.tracking")
        3 -> WorktreeBundle.message("column.merged")
        4 -> WorktreeBundle.message("column.changes")
        5 -> WorktreeBundle.message("column.activity")
        else -> WorktreeBundle.message("column.status")
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val row = rows[rowIndex]
        return when (columnIndex) {
            0 -> WorktreeRowPresenter.branch(row)
            1 -> WorktreeRowPresenter.worktree(row)
            2 -> WorktreeRowPresenter.tracking(row)
            3 -> WorktreeRowPresenter.merged(row)
            4 -> WorktreeRowPresenter.changes(row)
            5 -> RelativeTimeCell(row.lastActivityMillis)
            else -> WorktreeRowPresenter.status(row)
        }
    }

    override fun getColumnClass(columnIndex: Int): Class<*> =
        if (columnIndex == 5) RelativeTimeCell::class.java else String::class.java

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
}
