package com.tempwidget.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Service for handling widget update requests
 * This service acts as a bridge between the widget provider and WorkManager
 */
class WidgetUpdateService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Delegate the work to WorkManager
        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "widget_update_service",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        // Stop the service after starting the work
        stopSelf()

        return START_NOT_STICKY
    }
}
