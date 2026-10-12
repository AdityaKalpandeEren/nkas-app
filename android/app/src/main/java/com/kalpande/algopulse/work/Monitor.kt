package com.kalpande.algopulse.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kalpande.algopulse.data.Settings
import java.util.concurrent.TimeUnit

/** Schedules the background health check (WorkManager, survives reboots). */
object Monitor {
    private const val PERIODIC = "algopulse-monitor"
    private const val NOW = "algopulse-monitor-now"

    private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedule(ctx: Context, s: Settings) {
        val wm = WorkManager.getInstance(ctx)
        if (!s.monitorEnabled) {
            wm.cancelUniqueWork(PERIODIC)
            return
        }
        val req = PeriodicWorkRequestBuilder<MonitorWorker>(s.monitorIntervalMin.coerceAtLeast(15).toLong(), TimeUnit.MINUTES)
            .setConstraints(net)
            .build()
        wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun runNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            NOW, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MonitorWorker>().setConstraints(net).addTag("algopulse-now").build(),
        )
    }
}
