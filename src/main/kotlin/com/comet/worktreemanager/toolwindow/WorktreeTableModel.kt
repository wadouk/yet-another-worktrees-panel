package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.model.WorktreeRow
import javax.swing.table.AbstractTableModel

/** Backs the table: Branch / Worktree / Tracking / Status columns. */
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
        0 -> "Branch"
        1 -> "Worktree"
        2 -> "Tracking"
        3 -> "Merged"
        4 -> "Changes"
        5 -> "Activity"
        else -> "Status"
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val row = rows[rowIndex]
        return when (columnIndex) {
            0 -> row.refLabel
            1 -> row.worktreeLabel
            2 -> row.trackingLabel
            3 -> row.mergedLabel
            4 -> row.changesLabel
            5 -> RelativeTimeCell(row.lastActivityMillis)
            else -> row.statusLabel
        }
    }

    override fun getColumnClass(columnIndex: Int): Class<*> =
        if (columnIndex == 5) RelativeTimeCell::class.java else String::class.java

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
}
