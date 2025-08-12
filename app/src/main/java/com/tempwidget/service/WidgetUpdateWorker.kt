package com.tempwidget.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import com.tempwidget.R
import com.tempwidget.api.PirateWeatherApi
import com.tempwidget.data.LocationResult
import com.tempwidget.data.WeatherResult
import com.tempwidget.location.LocationManager
import com.tempwidget.utils.TemperatureUtils
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

/**
 * Background worker for updating widgets periodically
 * Uses WorkManager instead of AlarmManager for better compatibility
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "widget_update_channel"
    }

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        try {
            Log.d("WidgetUpdateWorker", "doWork() started")
            val context = applicationContext
            com.tempwidget.widget.updateAllMainWidgets(context)
            return Result.success()
        } catch (e: CancellationException) {
            Log.d("WidgetUpdateWorker", "Worker was cancelled before completion")
            throw e // Always rethrow cancellation!
        } catch (e: Exception) {
            Log.e("WidgetUpdateWorker", "Worker failed: ${e.message}", e)
            return Result.failure()
        } finally {
            Log.d("WidgetUpdateWorker", "doWork() finally block reached")
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val context = applicationContext
        val channelId = CHANNEL_ID
        val channelName = "Widget Update"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Updating Weather Widget")
            .setContentText("Fetching latest weather and location data...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
