package com.kalpande.algopulse

import android.app.Application
import com.kalpande.algopulse.data.SettingsRepository
import com.kalpande.algopulse.work.Monitor
import com.kalpande.algopulse.work.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlgoPulseApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        runCatching { Notifier.createChannels(this) }
        appScope.launch {
            runCatching { Monitor.schedule(this@AlgoPulseApp, SettingsRepository(this@AlgoPulseApp).current()) }
        }
    }
}
