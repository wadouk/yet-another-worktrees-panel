package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.model.WorktreeInfo
import javax.swing.table.AbstractTableModel

/** Backs the worktree table: Branch / Path / Status columns. */
class WorktreeTableModel : AbstractTableModel() {

    private var rows: List<WorktreeInfo> = emptyList()

    fun setRows(newRows: List<WorktreeInfo>) {
        rows = newRows
        fireTableDataChanged()
    }

    fun rowAt(index: Int): WorktreeInfo? = rows.getOrNull(index)

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = 3

    override fun getColumnName(column: Int): String = when (column) {
        0 -> "Branch"
        1 -> "Path"
        else -> "Status"
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val wt = rows[rowIndex]
        return when (columnIndex) {
            0 -> wt.refLabel
            1 -> wt.path
            else -> wt.statusLabel
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
}
