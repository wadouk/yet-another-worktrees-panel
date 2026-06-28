package me.heloworld.worktreemanager.model

/**
 * A unified table row: either a worktree (with or without a branch) or a local
 * branch that has no worktree. Carries the branch's tracking status so the same
 * row shows ahead/behind even when no worktree is checked out for it.
 *
 * Kept free of IntelliJ types so the merge logic stays unit-testable;
 * [repositoryRoot] is a plain path used to resolve the owning repository.
 */
data class WorktreeRow(
    val branch: String?,
    val worktreePath: String?,
    val head: String?,
    val upstream: String?,
    val ahead: Int,
    val behind: Int,
    val isGone: Boolean,
    val isDetached: Boolean,
    val isBare: Boolean,
    val isLocked: Boolean,
    val isPrunable: Boolean,
    val isCurrent: Boolean,
    val repositoryRoot: String,
    /** Working-tree dirtiness; null when there is no worktree or it's unknown. */
    val workingTree: WorkingTreeStatus? = null,
    /** Whether this branch is merged into the repo's default branch; null = N/A. */
    val isMerged: Boolean? = null,
    /** The repo's default branch name, used to label/compare; null if unknown. */
    val defaultBranch: String? = null,
    /** Last activity timestamp (millis): newest uncommitted change or last commit. */
    val lastActivityMillis: Long? = null,
    /** True when [lastActivityMillis] came from an uncommitted file, not a commit. */
    val lastActivityIsFile: Boolean = false,
    /** Path of the repo's main worktree, used to show paths relatively; null if unknown. */
    val mainWorktreePath: String? = null,
) {
    // Pure data only — display strings are built in the presentation layer
    // (me.heloworld.worktreemanager.toolwindow.WorktreeRowPresenter) so the model
    // stays free of i18n and the current clock.
    val hasWorktree: Boolean get() = worktreePath != null
    val hasBranch: Boolean get() = branch != null
}
