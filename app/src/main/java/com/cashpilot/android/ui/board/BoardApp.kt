package com.cashpilot.android.ui.board

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.cashpilot.android.BuildConfig
import com.cashpilot.android.ui.MainViewModel

/** The device name the server lists this phone under, minus the short id it appends. */
val deviceName: String get() = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

/**
 * The app: onboarding until setup is done, then the board and the screens it
 * leads to. Navigation is a small saved stack of route strings; four screens
 * and one level of depth need nothing more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardApp(vm: MainViewModel) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val apps by vm.apps.collectAsState()
    val notif by vm.hasNotificationAccess.collectAsState()
    val usage by vm.hasUsageAccess.collectAsState()
    val battery by vm.hasBatteryOptOut.collectAsState()
    val lastHeartbeat by vm.lastHeartbeat.collectAsState()
    val failed by vm.lastHeartbeatFailed.collectAsState()
    val earnings by vm.earnings.collectAsState()
    val earningsAsOf by vm.earningsAsOf.collectAsState()
    val pair by vm.pairResult.collectAsState()
    val refreshing by vm.isRefreshing.collectAsState()

    val stack = rememberSaveable(saver = listSaver<SnapshotStateList<String>, String>({ it.toList() }, { it.toMutableStateList() })) {
        mutableStateListOf(HOME)
    }
    var demo by rememberSaveable { mutableStateOf(false) }
    var step by rememberSaveable { mutableStateOf(0) }

    // The server fields as typed. Written through the ViewModel's debounced
    // setters; filled from storage once, so a slow DataStore read never
    // overwrites what the user is typing.
    var url by rememberSaveable { mutableStateOf("") }
    var key by rememberSaveable { mutableStateOf("") }
    var urlSynced by rememberSaveable { mutableStateOf(false) }
    var keySynced by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.serverUrl) {
        if (!urlSynced && settings.serverUrl.isNotEmpty()) { url = settings.serverUrl; urlSynced = true }
    }
    LaunchedEffect(settings.apiKey) {
        if (!keySynced && settings.apiKey.isNotEmpty()) { key = settings.apiKey; keySynced = true }
    }

    val now = System.currentTimeMillis()
    val real = Board.from(apps, settings, lastHeartbeat, failed, earnings, earningsAsOf, notif, usage, battery, now)
    val board = if (demo) Demo.board(now) else real

    fun push(route: String) { stack.add(route) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    val actions = BoardActions(
        openApp = { if (!demo) openApp(context, it) },
        showApp = { push("$APP$it") },
        openSettings = { if (!demo) push(SETTINGS) },
        showSupported = { push(SUPPORTED) },
        install = { if (!demo) install(context, it) },
        allowNotifications = { push(DISCLOSE + Access.NOTIFICATIONS.name) },
        allowUsage = { push(DISCLOSE + Access.USAGE.name) },
        openBattery = { openSystem(context, AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) },
        checkServer = { push(SETTINGS); vm.testConnection(settings.serverUrl, settings.apiKey) },
        startDemo = { demo = true; stack.clear(); stack.add(HOME) },
        toggleApp = { if (!demo) vm.toggleApp(it) },
    )
    val settingsActions = SettingsActions(
        onUrl = { url = it; vm.clearPairResult(); vm.updateServerUrl(it) },
        onKey = { key = it; vm.clearPairResult(); vm.updateApiKey(it) },
        testConnection = { vm.testConnection(url, key) },
        openUrl = { openUri(context, it) },
    )

    val route = stack.last()
    BackHandler(enabled = stack.size > 1 || demo || (!settings.setupCompleted && step > 0)) {
        when {
            stack.size > 1 -> pop()
            demo -> demo = false
            else -> step -= 1
        }
    }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (route.startsWith(DISCLOSE)) {
            val kind = Access.valueOf(route.removePrefix(DISCLOSE))
            DisclosureScreen(
                kind,
                onContinue = {
                    pop()
                    openSystem(
                        context,
                        if (kind == Access.NOTIFICATIONS) AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS else AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS,
                    )
                },
                onNotNow = { pop() },
            )
            return@Box
        }
        if (!settings.setupCompleted && !demo) {
            when (step) {
                0 -> OnboardWelcome { step = 1 }
                1 -> OnboardPermissions(
                    notif, usage, battery,
                    actions.allowNotifications, actions.allowUsage, actions.openBattery,
                ) { step = 2 }
                else -> OnboardServer(
                    url, key, settingsActions.onUrl, settingsActions.onKey, pair, deviceName,
                    onTest = settingsActions.testConnection,
                    onFinish = { vm.finishSetup(keepServer = true) },
                    onSkip = { url = ""; key = ""; vm.finishSetup(keepServer = false) },
                )
            }
            return@Box
        }
        val screen: @Composable () -> Unit = {
            when {
                route == SETTINGS -> SettingsScreen(
                    board, ServerForm(url, key, settings.apiKey, pair), BuildConfig.VERSION_NAME, deviceName,
                    actions, settingsActions, ::pop,
                )
                route == SUPPORTED -> SupportedAppsScreen(board, actions, ::pop)
                route.startsWith(APP) -> {
                    val row = board.apps.firstOrNull { it.slug == route.removePrefix(APP) }
                    if (row == null) LaunchedEffect(route) { pop() } else AppDetailScreen(row, board, actions, ::pop)
                }
                // Pull down to check every app again; it also checks on every return to the app.
                demo -> HomeScreen(board, actions)
                else -> PullToRefreshBox(isRefreshing = refreshing, onRefresh = vm::refreshStatuses) { HomeScreen(board, actions) }
            }
        }
        if (demo) DemoFrame(onExit = { demo = false; stack.clear(); stack.add(HOME) }) { screen() } else screen()
    }
}

private const val HOME = "home"
private const val SETTINGS = "settings"
private const val SUPPORTED = "supported"
private const val APP = "app/"
private const val DISCLOSE = "disclose/"

private fun openSystem(context: Context, action: String) {
    runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun openUri(context: Context, uri: String): Boolean = runCatching {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}.isSuccess

/** Start the app itself. If it has no launcher entry, its system settings page, where Force stop and Open live. */
private fun openApp(context: Context, a: AppRow) {
    val launch = context.packageManager.getLaunchIntentForPackage(a.packageName)
    if (launch != null && runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
    runCatching {
        context.startActivity(
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${a.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** The provider's sign-up page when there is a referral link, otherwise the store page. */
private fun install(context: Context, a: AppRow) {
    if (a.referralUrl != null && openUri(context, a.referralUrl)) return
    if (openUri(context, "market://details?id=${a.packageName}")) return
    openUri(context, "https://play.google.com/store/apps/details?id=${a.packageName}")
}
