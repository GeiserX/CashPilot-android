package com.cashpilot.android.ui

/**
 * The decisions the dashboard makes about an app, as pure functions.
 *
 * These live outside [MainViewModel] for two reasons.
 *
 * The first is testability without Android. [MainViewModel] is an
 * `AndroidViewModel` and needs an `Application`, so its logic could only be
 * covered by copying it into the test — which `AppStateResolutionTest` did,
 * commenting "this is the exact same when-expression used in production". A
 * copy cannot fail when production changes, so it verified the copy, not the
 * app. These functions are called by both now.
 *
 * The second is that ordering is a product decision worth stating explicitly
 * rather than leaving to an enum's declaration order — see [attentionRank].
 */
object AppPresentation {

    /**
     * Which state an app is in, given what the detectors could establish.
     *
     * Order matters: a package that is not installed cannot be running, and an
     * app the user switched off should not be reported as stopped — "stopped"
     * is a problem, "disabled" is a choice.
     *
     * `running == null` means the detectors could not tell, and it resolves to
     * [AppState.STOPPED] deliberately: the app is installed and enabled, so
     * something SHOULD be reporting it, and staying silent about an app that
     * may have died is the failure this whole product exists to prevent. The
     * card says how it knows; see [AppState].
     */
    fun resolveState(
        installed: Boolean,
        enabled: Boolean,
        running: Boolean?,
    ): AppState = when {
        !installed -> AppState.NOT_INSTALLED
        !enabled -> AppState.DISABLED
        running == true -> AppState.RUNNING
        else -> AppState.STOPPED
    }

    /**
     * Sort key for the dashboard: lower sorts first. **Problems lead.**
     *
     * The dashboard previously sorted by `AppState.ordinal`, which is the order
     * the enum happens to be declared in — RUNNING first. So the apps that were
     * fine occupied the top of the screen and the ones that had STOPPED were
     * pushed underneath them. On a two-column grid with eleven apps, a stopped
     * earner could sit below the fold on a phone, which is the one thing the
     * user opened the app to find out.
     *
     * Ranked by what the user can act on:
     *
     * | | | |
     * |---|---|---|
     * | 0 | [AppState.STOPPED]       | should be earning and is not — act now |
     * | 1 | [AppState.RUNNING]       | working; confirmation is worth seeing  |
     * | 2 | [AppState.DISABLED]      | switched off deliberately              |
     * | 3 | [AppState.NOT_INSTALLED] | not on this device at all              |
     *
     * Deliberately NOT derived from the enum, so that reordering or inserting a
     * state cannot silently rearrange the screen. A new state must be given a
     * rank here, and [rankIsTotal] fails if one is forgotten.
     */
    fun attentionRank(state: AppState): Int = when (state) {
        AppState.STOPPED -> 0
        AppState.RUNNING -> 1
        AppState.DISABLED -> 2
        AppState.NOT_INSTALLED -> 3
    }

    /**
     * Every state has a distinct rank. Guards against a new [AppState] being
     * given a duplicate rank, which would make the ordering depend on the
     * sort's stability rather than on this table.
     */
    fun rankIsTotal(): Boolean =
        AppState.entries.map(::attentionRank).toSet().size == AppState.entries.size

    /**
     * The dashboard order: problems first, then alphabetically within a rank.
     *
     * The secondary key matters more than it looks — without it, apps in the
     * same state come back in whatever order the detectors happened to return,
     * so the grid reshuffles under the user's thumb on every 30-second refresh.
     */
    fun sortForDashboard(apps: List<AppDisplayInfo>): List<AppDisplayInfo> =
        apps.sortedWith(compareBy({ attentionRank(it.state) }, { it.app.displayName.lowercase() }))
}
