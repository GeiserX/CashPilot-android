package com.cashpilot.android

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.cashpilot.android.model.Earnings
import com.cashpilot.android.model.WorkerHeartbeatResponse
import com.cashpilot.android.service.Heartbeat
import com.cashpilot.android.service.HeartbeatWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * What the app may run in the background, checked against the MERGED manifest
 * and the real WorkManager schedule rather than against a copy of either.
 *
 * Google Play allows REQUEST_IGNORE_BATTERY_OPTIMIZATIONS only for chat, VoIP,
 * safety, automation and peripheral apps, and asks every special-use foreground
 * service for a justification. CashPilot is none of those, so it reports on a
 * WorkManager schedule and opens the battery settings LIST instead of the direct
 * exemption dialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackgroundPolicyTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun initWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    private fun requested(): Set<String> =
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toSet()

    @Test
    fun `the manifest no longer asks for the battery exemption or a special-use service`() {
        val perms = requested()
        // Positive control: the check reads the merged manifest. If it saw
        // nothing, every "absent" assertion below would pass for free.
        assertTrue("INTERNET should be requested; is the manifest being read?", "android.permission.INTERNET" in perms)
        assertFalse("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" in perms)
        assertFalse("android.permission.FOREGROUND_SERVICE_SPECIAL_USE" in perms)
    }

    @Test
    fun `no service declares the special-use foreground type`() {
        val services = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_SERVICES).services.orEmpty()
        // Positive control: the notification listener is declared, so the list is real.
        assertTrue(services.any { it.name.endsWith("AppNotificationListener") })
        if (Build.VERSION.SDK_INT >= 34) {
            val special = services.filter {
                it.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE != 0
            }
            assertTrue("special-use services: ${special.map { it.name }}", special.isEmpty())
        }
    }

    @Test
    fun `no code opens the direct battery exemption dialog`() {
        val main = File("src/main/java")
        assertTrue("expected sources at ${main.absolutePath}", main.isDirectory)
        val offenders = main.walkTopDown().filter { it.extension == "kt" }
            .filter { f ->
                // Only code, not the comment that explains why it is not used.
                f.readLines().any { line ->
                    "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" in line && !line.trimStart().startsWith("//")
                }
            }.map { it.name }.toList()
        assertTrue("direct exemption dialog used in: $offenders", offenders.isEmpty())
    }

    @Test
    fun `the heartbeat is one periodic job every 15 minutes that needs a network`() {
        HeartbeatWorker.schedule(context)
        HeartbeatWorker.schedule(context) // a second start must not add or reset anything
        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(HeartbeatWorker.PERIODIC).get()
        assertEquals(1, infos.size)
        val info = infos.single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(TimeUnit.MINUTES.toMillis(15), info.periodicityInfo?.repeatIntervalMillis)
        assertEquals(androidx.work.NetworkType.CONNECTED, info.constraints.requiredNetworkType)
    }

    @Test
    fun `asking for a report now twice leaves one pending run`() {
        HeartbeatWorker.runNow(context)
        HeartbeatWorker.runNow(context)
        val pending = WorkManager.getInstance(context).getWorkInfosForUniqueWork(HeartbeatWorker.NOW).get()
            .filter { !it.state.isFinished }
        assertEquals(1, pending.size)
    }

    @Test
    fun `with no server the worker succeeds and sends nothing`() = runBlocking {
        val before = Heartbeat.lastHeartbeat.value
        val result = TestListenableWorkerBuilder<HeartbeatWorker>(context).build().doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(before, Heartbeat.lastHeartbeat.value)
        assertFalse(Heartbeat.lastHeartbeatFailed.value)
    }

    @Test
    fun `what the last report said survives a process restart`() {
        val kept = Earnings(windowDays = 30, totalUsd = 12.5)
        Heartbeat.recordEarnings(WorkerHeartbeatResponse(status = "ok", earnings = kept), now = 1_000L)
        Heartbeat.persist(context)

        // Change the in-memory state, as a new process would start empty...
        Heartbeat.recordEarnings(WorkerHeartbeatResponse(status = "ok", earnings = Earnings(totalUsd = 99.0)), now = 2_000L)
        // ...then restore must bring back what was on disk, not what is in memory.
        Heartbeat.restore(context)
        assertEquals(kept, Heartbeat.earnings.value)
        assertEquals(1_000L, Heartbeat.earningsAsOf.value)
    }

    @Test
    fun `an unreadable stored figure restores as unknown, not a crash`() {
        context.getSharedPreferences("heartbeat_state", Context.MODE_PRIVATE).edit()
            .putString("earnings_json", "{not json").commit()
        Heartbeat.restore(context)
        assertNull(Heartbeat.earnings.value)
    }
}
