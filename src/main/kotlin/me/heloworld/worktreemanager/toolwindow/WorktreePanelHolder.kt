package me.heloworld.worktreemanager.toolwindow

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * Bridges the Git Log actions to the live [WorktreePanel] instance. The panel is
 * created lazily by [WorktreePruningContentProvider]; it registers itself here on
 * [initContent][WorktreePruningContentProvider.initContent] and clears it on
 * dispose, so an action can reach it (or find it absent) without holding a
 * direct reference to a component that may not exist yet.
 */
@Service(Service.Level.PROJECT)
class WorktreePanelHolder(@Suppress("unused") private val project: Project) {

    @Volatile
    var panel: WorktreePanel? = null
}
