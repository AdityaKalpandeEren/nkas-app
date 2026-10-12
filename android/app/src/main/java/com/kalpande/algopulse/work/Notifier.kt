package com.kalpande.algopulse.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kalpande.algopulse.MainActivity
import com.kalpande.algopulse.R

object Notifier {
    const val CH_HEALTH = "bot_health"
    const val CH_SUMMARY = "daily_summary"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_HEALTH, ctx.getString(R.string.channel_health), NotificationManager.IMPORTANCE_HIGH).apply {
                description = ctx.getString(R.string.channel_health_desc)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SUMMARY, ctx.getString(R.string.channel_summary), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = ctx.getString(R.string.channel_summary_desc)
            },
        )
    }

    fun canNotify(ctx: Context): Boolean {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun notify(ctx: Context, id: Int, channel: String, title: String, text: String, tab: String) {
        if (!canNotify(ctx)) return
        val open = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_TAB, tab)
        val pi = PendingIntent.getActivity(ctx, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_pulse)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(if (channel == CH_HEALTH) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
        }
    }
}
