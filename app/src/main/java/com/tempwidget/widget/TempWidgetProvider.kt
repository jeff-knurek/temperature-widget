package com.tempwidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
        // Update all widgets
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget is added
        scheduleWidgetUpdates(context)
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget is removed
        cancelWidgetUpdates(context)
    }

    /**
     * Updates a single widget instance
     */
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Show loading state
        views.setTextViewText(R.id.temp_celsius, "Loading...")
        views.setTextViewText(R.id.temp_fahrenheit, "Loading...")
        views.setTextViewText(R.id.location_name, "Getting location...")

        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Get location and weather data
        coroutineScope.launch {
            updateWeatherData(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Updates widget with weather data
     */
    private suspend fun updateWeatherData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val locationManager = LocationManager(context)
        val weatherApi = OpenMeteoApi(context)
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        if (!locationManager.hasLocationPermission()) {
            // Show permission error
            views.setTextViewText(R.id.temp_celsius, "No")
            views.setTextViewText(R.id.temp_fahrenheit, "Permission")
            views.setTextViewText(R.id.location_name, "Location permission required")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }
        Log.d("TempHiWidgetProvider", "Updating location: " + locationManager.hasLocationPermission())

        when (val locationResult = locationManager.getCurrentLocation()) {
            is LocationResult.Success -> {
                val location = locationResult.location
                Log.d("TempHiWidgetProvider", "Location name: " + location.name.toString())
                views.setTextViewText(R.id.location_name, location.name)

                when (val weatherResult = weatherApi.getWeatherData(location.latitude, location.longitude)) {
                    is WeatherResult.Success -> {
                        Log.d("TempHiWidgetProvider", "Weather success")
                        val weatherData = weatherResult.data

                        // Temperature from API is in Fahrenheit
                        val tempF = weatherData.currentTemperature
                        val tempC = TemperatureUtils.fahrenheitToCelsius(tempF)
                        Log.d("TempHiWidgetProvider", "Temperature: " + tempC.toString())
                        // Update temperature displays
                        views.setTextViewText(
                            R.id.temp_celsius,
                            TemperatureUtils.formatTemperature(tempC, "C")
                        )
                        views.setTextViewText(
                            R.id.temp_fahrenheit,
                            TemperatureUtils.formatTemperature(tempF, "F")
                        )

                        // Update location name if available from weather data
                        if (weatherData.locationName != "Unknown Location") {
                            views.setTextViewText(R.id.location_name, weatherData.locationName)
                        }

                        // Future: Update humidity and dew point if needed
                        // views.setTextViewText(R.id.humidity, "Humidity: ${weatherData.humidity.roundToInt()}%")
                        // views.setTextViewText(R.id.dew_point, "Dew: ${TemperatureUtils.formatTemperature(weatherData.dewPoint, "F")}")
                    }
                    is WeatherResult.Error -> {
                        Log.d("TempHiWidgetProvider", "Weather error: " + weatherResult.message)
                        views.setTextViewText(R.id.temp_celsius, "Weather")
                        views.setTextViewText(R.id.temp_fahrenheit, "Error")
                        views.setTextViewText(R.id.location_name, weatherResult.message)
                    }
                }
            }
            is LocationResult.Error -> {
                Log.d("TempHiWidgetProvider", "Location error: " + locationResult.message)
                views.setTextViewText(R.id.temp_celsius, "Location")
                views.setTextViewText(R.id.temp_fahrenheit, "Error")
                views.setTextViewText(R.id.location_name, locationResult.message)
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /**
     * Schedules periodic widget updates using WorkManager
     */
    private fun scheduleWidgetUpdates(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "widget_update_work",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancels scheduled widget updates
     */
    private fun cancelWidgetUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("widget_update_work")
    }
}
