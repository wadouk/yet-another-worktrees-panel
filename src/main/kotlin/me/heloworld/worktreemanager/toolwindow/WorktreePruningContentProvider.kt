package me.heloworld.worktreemanager.toolwindow

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import me.heloworld.worktreemanager.service.WorktreeService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider
import git4idea.repo.GitRepositoryManager
import java.util.function.Predicate
import java.util.function.Supplier
import javax.swing.JComponent

/**
 * Supplies the "Pruning" tab content hosted inside the Version Control tool
 * window (via the `changesViewContent` extension point) instead of a standalone
 * tool window. The panel is created lazily and disposed with the tab.
 */
class WorktreePruningContentProvider(private val project: Project) : ChangesViewContentProvider {

    private var panel: WorktreePanel? = null

    override fun initContent(): JComponent =
        WorktreePanel(project).also { panel = it }

    override fun disposeContent() {
        panel?.dispose()
        panel = null
    }
}

/** Localized display name for the tab (`displayNameSupplierClassName`). */
class WorktreeTabName : Supplier<String> {
    override fun get(): String = WorktreeBundle.message("tab.title")
}

/** Shows the tab only for projects that have at least one Git repository. */
class WorktreeTabVisibility : Predicate<Project> {
    override fun test(project: Project): Boolean =
        GitRepositoryManager.getInstance(project).repositories.isNotEmpty()
}
