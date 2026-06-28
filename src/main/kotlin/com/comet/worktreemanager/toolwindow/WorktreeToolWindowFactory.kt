package com.comet.worktreemanager.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Registers the Worktrees tool window content. */
class WorktreeToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = WorktreePanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        // Dispose the panel with the content so its shared-manager registrations
        // are released on tool window close / dynamic plugin unload.
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }
}
