package me.heloworld.worktreemanager.gitlog

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import me.heloworld.worktreemanager.service.WorktreeBranchIndex
import com.intellij.icons.AllIcons
import com.intellij.ui.SimpleColoredComponent
import com.intellij.vcs.log.ui.table.GraphTableModel
import com.intellij.vcs.log.ui.table.VcsLogCellRenderer
import com.intellij.vcs.log.ui.table.VcsLogGraphTable
import com.intellij.vcs.log.ui.table.column.VcsLogCustomColumn
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

/**
 * A narrow Git Log column that shows a folder icon on commits which are the tip
 * of a local branch checked out in a worktree. Values come from the EDT-safe
 * [WorktreeBranchIndex] cache, so painting never blocks on git.
 *
 * Enabled by default and toggleable from the log's column gear menu. The value
 * is the worktree path (used as the tooltip), or an empty string when the row's
 * branch has no worktree.
 */
class WorktreeLogColumn : VcsLogCustomColumn<String> {

    override val id: String = "WorktreeManager.WorktreeColumn"

    override val localizedName: String get() = WorktreeBundle.message("column.log.worktree")

    override val isDynamic: Boolean = true

    override fun isEnabledByDefault(): Boolean = true

    override fun getStubValue(model: GraphTableModel): String = ""

    override fun getValue(model: GraphTableModel, row: Int): String {
        // `getRefsAtRow` + branch filter rather than `getBranchesAtRow`: the latter
        // was dropped from GraphTableModel after 2024.3, so calling it throws
        // NoSuchMethodError on newer IDEs (the plugin leaves untilBuild open).
        val branches = model.getRefsAtRow(row).filter { it.type.isBranch }
        if (branches.isEmpty()) return ""
        val index = model.logData.project.getService(WorktreeBranchIndex::class.java)
        return index.match(branches)?.worktreePath ?: ""
    }

    override fun createTableCellRenderer(table: VcsLogGraphTable): TableCellRenderer = WorktreeCellRenderer()

    private class WorktreeCellRenderer : SimpleColoredComponent(), TableCellRenderer, VcsLogCellRenderer {

        init {
            isOpaque = false
        }

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): java.awt.Component {
            clear()
            val path = value as? String
            if (!path.isNullOrEmpty()) {
                icon = AllIcons.Nodes.Folder
                toolTipText = WorktreeBundle.message("column.log.worktree.tooltip", path)
            } else {
                toolTipText = null
            }
            return this
        }

        // Fixed, icon-width column so it stays out of the way.
        override fun getPreferredWidth(): VcsLogCellRenderer.PreferredWidth =
            VcsLogCellRenderer.PreferredWidth.Fixed { AllIcons.Nodes.Folder.iconWidth + JBUI_GAP }
    }

    private companion object {
        const val JBUI_GAP = 8
    }
}
