package com.cashpilot.android.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Runs one [Heartbeat]. Scheduled every [PERIOD_MINUTES] minutes, the shortest
 * period WorkManager allows, and once more whenever [runNow] is called.
 *
 * Always returns success. A failed heartbeat is recorded for the UI, and the next
 * period is the retry; asking WorkManager to retry as well would stack a second
 * schedule on top of the periodic one.
 */
class HeartbeatWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Heartbeat.sendOnce(applicationContext)
        return Result.success()
    }

    companion object {
        const val PERIOD_MINUTES = 15L
        const val PERIODIC = "heartbeat"
        const val NOW = "heartbeat-now"

        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Idempotent: an existing schedule is kept, so calling this on every start does not reset it. */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<HeartbeatWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(online)
                    .build(),
            )
        }

        /**
         * One heartbeat as soon as there is a network: when the app is opened and
         * when the server details change, so a new URL or key is tried at once
         * instead of on the next period. A pending one is replaced, not queued twice.
         */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<HeartbeatWorker>().setConstraints(online).build(),
            )
        }
    }
}
