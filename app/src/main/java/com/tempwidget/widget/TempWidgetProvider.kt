package com.tempwidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import java.util.concurrent.TimeUnit
import com.tempwidget.R
import com.tempwidget.api.OpenMeteoApi
import com.tempwidget.data.LocationResult
import com.tempwidget.data.WeatherResult
import com.tempwidget.location.LocationManager
import com.tempwidget.service.WidgetUpdateWorker
import com.tempwidget.utils.TemperatureUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Temperature widget provider
 * Handles widget updates and displays weather information
 */
class TempWidgetProvider : AppWidgetProvider() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("TempWidgetProvider", "onUpdate() called")
        // Update all widgets using shared updater
        CoroutineScope(Dispatchers.Main).launch {
            updateAllMainWidgets(context)
        }
    }

    override fun onEnabled(context: Context?) {
        context?.let {
            val intervalMinutes = it.resources.getInteger(R.integer.widget_update_interval_minutes).toLong()
            val interval = maxOf(intervalMinutes, 15L)
            val workRequest = PeriodicWorkRequestBuilder<com.tempwidget.service.WidgetUpdateWorker>(
                interval, TimeUnit.MINUTES
            ).addTag("widget_update_periodic").build()
            androidx.work.WorkManager.getInstance(it).enqueueUniquePeriodicWork(
                "widget_update_periodic_work",
                androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    override fun onDisabled(context: Context?) {
        context?.let {
            androidx.work.WorkManager.getInstance(it).cancelUniqueWork("widget_update_periodic_work")
        }
    }
}
