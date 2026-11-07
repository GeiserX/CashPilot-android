package com.cashpilot.android

import android.app.Application
import androidx.work.Configuration
import com.cashpilot.android.service.Heartbeat
import com.cashpilot.android.service.HeartbeatWorker

/**
 * Implements [Configuration.Provider], and the manifest removes WorkManager's
 * startup initializer, so WorkManager initialises on first use: the
 * [HeartbeatWorker.schedule] call below. Same behaviour on a device and under
 * Robolectric, which does not run androidx.startup's content provider.
 */
class CashPilotApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        // Every process start, including the system binding the notification
        // listener after a reboot: restore what the last heartbeat said, and make
        // sure the periodic schedule exists (KEEP, so this never resets it).
        Heartbeat.restore(this)
        HeartbeatWorker.schedule(this)
    }
}
