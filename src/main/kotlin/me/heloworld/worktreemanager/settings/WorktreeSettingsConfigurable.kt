package me.heloworld.worktreemanager.settings

import me.heloworld.worktreemanager.i18n.WorktreeBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

/** Project-level settings page (Settings | Tools) for the worktree base dir. */
class WorktreeSettingsConfigurable(private val project: Project) :
    BoundConfigurable(WorktreeBundle.message("settings.title")) {

    override fun createPanel(): DialogPanel {
        val settings = WorktreeSettings.getInstance(project)
        return panel {
            row(WorktreeBundle.message("settings.baseDir.label")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle(WorktreeBundle.message("settings.title")),
                    project,
                ).bindText(settings::worktreeBaseDir)
                    .align(AlignX.FILL)
                    .comment(WorktreeBundle.message("settings.baseDir.comment"))
            }
        }
    }
}
