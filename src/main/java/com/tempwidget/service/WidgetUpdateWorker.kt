package com.tempwidget.service

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tempwidget.R
import com.tempwidget.widget.TempWidgetProvider
import java.util.concurrent.TimeUnit

/**
 * Background worker for updating widgets periodically
 * Uses WorkManager instead of AlarmManager for better compatibility
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get widget manager
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

            // Get all widget instances
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(applicationContext, TempWidgetProvider::class.java)
            )

            if (widgetIds.isNotEmpty()) {
                // Trigger widget update
                val intent = Intent(applicationContext, TempWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                applicationContext.sendBroadcast(intent)
            }

            // Schedule next update
            scheduleNextUpdate()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Schedules the next periodic update
     */
    private fun scheduleNextUpdate() {
        val updateIntervalMs = applicationContext.resources.getInteger(R.integer.widget_update_interval_ms)
        val updateIntervalMinutes = updateIntervalMs / (60 * 1000)

        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            updateIntervalMinutes.toLong(),
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "widget_periodic_update",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
