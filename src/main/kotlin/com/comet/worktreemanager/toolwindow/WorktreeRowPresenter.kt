package com.comet.worktreemanager.toolwindow

import com.comet.worktreemanager.i18n.WorktreeBundle
import com.comet.worktreemanager.model.WorktreeRow

/**
 * Presentation layer: builds the translated, user-facing labels and tooltips for
 * a [WorktreeRow] from its raw data and the message bundle. Neutral symbols
 * (↑ ↓ + ~ ? !, em dash) are kept as-is; only words go through the bundle.
 */
object WorktreeRowPresenter {

    // --- Cell labels -----------------------------------------------------

    fun branch(row: WorktreeRow): String = when {
        row.isBare -> WorktreeBundle.message("ref.bare")
        row.branch != null -> row.branch
        row.isDetached && row.head != null ->
            WorktreeBundle.message("ref.detached.at", row.head.take(7))
        else -> WorktreeBundle.message("ref.detached")
    }

    fun worktree(row: WorktreeRow): String =
        row.worktreePath ?: WorktreeBundle.message("worktree.none")

    fun tracking(row: WorktreeRow): String = when {
        row.upstream == null -> WorktreeBundle.message("tracking.none")
        row.isGone -> WorktreeBundle.message("tracking.gone")
        else -> buildList {
            if (row.ahead > 0) add("↑${row.ahead}")
            if (row.behind > 0) add("↓${row.behind}")
        }.ifEmpty { listOf(WorktreeBundle.message("tracking.upToDate")) }.joinToString(" ")
    }

    fun merged(row: WorktreeRow): String = when {
        row.branch != null && row.branch == row.defaultBranch -> WorktreeBundle.message("merged.default")
        row.isMerged == true -> WorktreeBundle.message("merged.merged")
        row.isMerged == false -> WorktreeBundle.message("merged.unmerged")
        else -> WorktreeBundle.message("merged.none")
    }

    fun changes(row: WorktreeRow): String {
        if (!row.hasWorktree) return WorktreeBundle.message("changes.none")
        val s = row.workingTree ?: return WorktreeBundle.message("changes.none")
        if (s.isClean) return WorktreeBundle.message("changes.clean")
        return buildList {
            if (s.staged > 0) add("+${s.staged}")
            if (s.modified > 0) add("~${s.modified}")
            if (s.untracked > 0) add("?${s.untracked}")
            if (s.conflicted > 0) add("!${s.conflicted}")
        }.joinToString(" ")
    }

    fun status(row: WorktreeRow): String = buildList {
        if (row.isCurrent) add(WorktreeBundle.message("status.current"))
        if (!row.hasWorktree && row.hasBranch) add(WorktreeBundle.message("status.noWorktree"))
        if (row.isLocked) add(WorktreeBundle.message("status.locked"))
        if (row.isPrunable) add(WorktreeBundle.message("status.prunable"))
    }.joinToString(", ")

    // --- Tooltips --------------------------------------------------------

    fun trackingTooltip(row: WorktreeRow): String = when {
        row.upstream == null -> WorktreeBundle.message("tooltip.tracking.noUpstream")
        row.isGone -> WorktreeBundle.message("tooltip.tracking.gone", row.upstream)
        row.ahead == 0 && row.behind == 0 -> WorktreeBundle.message("tooltip.tracking.upToDate", row.upstream)
        else -> {
            val parts = buildList {
                if (row.ahead > 0) add(WorktreeBundle.message("tooltip.tracking.ahead", row.ahead))
                if (row.behind > 0) add(WorktreeBundle.message("tooltip.tracking.behind", row.behind))
            }.joinToString(", ")
            WorktreeBundle.message("tooltip.tracking.relative", row.upstream, parts)
        }
    }

    fun mergedTooltip(row: WorktreeRow): String {
        if (row.branch == null) return WorktreeBundle.message("tooltip.merged.notBranch")
        val default = row.defaultBranch ?: return WorktreeBundle.message("tooltip.merged.unknownDefault")
        return when {
            row.branch == default -> WorktreeBundle.message("tooltip.merged.isDefault", default)
            row.isMerged == true -> WorktreeBundle.message("tooltip.merged.merged", default)
            row.isMerged == false -> WorktreeBundle.message("tooltip.merged.unmerged", default)
            else -> WorktreeBundle.message("tooltip.merged.unavailable")
        }
    }

    fun changesTooltip(row: WorktreeRow): String {
        if (!row.hasWorktree) return WorktreeBundle.message("tooltip.changes.noWorktree")
        val s = row.workingTree ?: return WorktreeBundle.message("tooltip.changes.unavailable")
        if (s.isClean) return WorktreeBundle.message("tooltip.changes.clean")
        return buildList {
            if (s.staged > 0) add(WorktreeBundle.message("tooltip.changes.staged", s.staged))
            if (s.modified > 0) add(WorktreeBundle.message("tooltip.changes.modified", s.modified))
            if (s.untracked > 0) add(WorktreeBundle.message("tooltip.changes.untracked", s.untracked))
            if (s.conflicted > 0) add(WorktreeBundle.message("tooltip.changes.conflicted", s.conflicted))
        }.joinToString(", ")
    }

    fun activityTooltip(row: WorktreeRow): String {
        val millis = row.lastActivityMillis ?: return WorktreeBundle.message("tooltip.activity.none")
        val relative = RelativeTimeCell(millis).toString()
        return if (row.lastActivityIsFile) {
            WorktreeBundle.message("tooltip.activity.file", relative)
        } else {
            WorktreeBundle.message("tooltip.activity.commit", relative)
        }
    }

    fun statusTooltip(row: WorktreeRow): String {
        val parts = buildList {
            if (row.isCurrent) add(WorktreeBundle.message("tooltip.status.current"))
            if (!row.hasWorktree && row.hasBranch) add(WorktreeBundle.message("tooltip.status.noWorktree"))
            if (row.isLocked) add(WorktreeBundle.message("tooltip.status.locked"))
            if (row.isPrunable) add(WorktreeBundle.message("tooltip.status.prunable"))
            if (row.isBare) add(WorktreeBundle.message("tooltip.status.bare"))
        }
        return if (parts.isEmpty()) WorktreeBundle.message("tooltip.status.none") else parts.joinToString("; ")
    }
}
