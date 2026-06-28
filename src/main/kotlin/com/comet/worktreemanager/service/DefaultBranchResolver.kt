package com.comet.worktreemanager.service

/**
 * Resolves the repository's default branch, used as the target for "merged?".
 * Prefers the remote's default (`origin/HEAD`); otherwise falls back to a local
 * `main` or `master`. Pure, so the resolution priority can be unit-tested.
 */
object DefaultBranchResolver {

    /** @param name short branch name; @param ref commit-ish to compare against. */
    data class Result(val name: String, val ref: String)

    /**
     * @param originHead trimmed output of `git rev-parse --abbrev-ref origin/HEAD`
     *        (e.g. `origin/main`), or null/blank when there is no remote default
     * @param localBranches names of existing local branches, for the fallback
     */
    fun resolve(originHead: String?, localBranches: Set<String>): Result? {
        val head = originHead?.trim().orEmpty()
        if (head.startsWith("origin/") && head != "origin/HEAD") {
            val name = head.removePrefix("origin/")
            if (name.isNotEmpty()) return Result(name, head)
        }
        for (candidate in listOf("main", "master")) {
            if (candidate in localBranches) return Result(candidate, candidate)
        }
        return null
    }
}
