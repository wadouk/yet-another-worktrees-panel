package me.heloworld.worktreemanager.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Per-project settings. [worktreeBaseDir] is the base directory proposed when
 * creating a worktree; empty means the default `.claude/worktrees` under the
 * repo root (see WorktreePlacement.baseDir for how it is resolved).
 */
@Service(Service.Level.PROJECT)
@State(name = "YawpWorktreeSettings", storages = [Storage("yawp.xml")])
class WorktreeSettings : PersistentStateComponent<WorktreeSettings.State> {

    data class State(var worktreeBaseDir: String = "")

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var worktreeBaseDir: String
        get() = state.worktreeBaseDir
        set(value) {
            state.worktreeBaseDir = value
        }

    companion object {
        fun getInstance(project: Project): WorktreeSettings = project.service()
    }
}
