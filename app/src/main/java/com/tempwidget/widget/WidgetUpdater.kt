package com.tempwidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.tempwidget.R
import com.tempwidget.api.OpenMeteoApi
import com.tempwidget.data.LocationResult
import com.tempwidget.data.WeatherResult
import com.tempwidget.location.LocationManager
import com.tempwidget.utils.TemperatureUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import kotlin.math.roundToInt

/**
 * Shared logic to update all main widget instances with latest weather/location
 */
suspend fun updateAllMainWidgets(context: Context) = withContext(Dispatchers.IO) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val widgetIds = appWidgetManager.getAppWidgetIds(
        ComponentName(context, com.tempwidget.widget.TempWidgetProvider::class.java)
    )
    val locationManager = LocationManager(context)
    val weatherApi = OpenMeteoApi(context)

    widgetIds.forEach { appWidgetId ->
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        // Show loading state
        views.setTextViewText(R.id.location_name, "Getting location...")
        appWidgetManager.updateAppWidget(appWidgetId, views)

        if (!locationManager.hasLocationPermission()) {
            views.setTextViewText(R.id.temp_celsius, "No")
            views.setTextViewText(R.id.temp_fahrenheit, "Permission")
            views.setTextViewText(R.id.location_name, "Location permission required")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return@forEach
        }
        Log.d("WidgetUpdater", "Updating location: " + locationManager.hasLocationPermission())
        when (val locationResult = locationManager.getCurrentLocation()) {
            is LocationResult.Success -> {
                val location = locationResult.location
                Log.d("WidgetUpdater", "Location name: " + location.name.toString())
                views.setTextViewText(R.id.location_name, location.name)
                when (val weatherResult = weatherApi.getWeatherData(location.latitude, location.longitude)) {
                    is WeatherResult.Success -> {
                        Log.d("WidgetUpdater", "Weather success")
                        val weatherData = weatherResult.data
                        val tempF = weatherData.currentTemperature
                        val tempC = TemperatureUtils.fahrenheitToCelsius(tempF)
                        Log.d("WidgetUpdater", "Temperature C: " + tempC.toString())
                        views.setTextViewText(
                            R.id.temp_celsius,
                            TemperatureUtils.formatTemperature(tempC, "C")
                        )
                        views.setTextViewText(
                            R.id.temp_fahrenheit,
                            TemperatureUtils.formatTemperature(tempF, "F")
                        )
                        if (weatherData.locationName != "Unknown Location") {
                            views.setTextViewText(R.id.location_name, weatherData.locationName)
                        }
                        // Set humidity and dew point
                        views.setTextViewText(
                            R.id.humidity,
                            "Humidity: ${weatherData.humidity.roundToInt()}%"
                        )
                        views.setTextViewText(
                            R.id.dew_point,
                            "Dew: ${TemperatureUtils.formatTemperature(weatherData.dewPoint, "F")}"
                        )
                        // Set rain chance and tomorrow forecast
                        views.setTextViewText(
                            R.id.rain_chance,
                            "Rain (3h): ${if (weatherData.rainChanceNext3h >= 0) weatherData.rainChanceNext3h else "--"}%"
                        )
                        views.setTextViewText(
                            R.id.tomorrow_forecast,
                            "Tomorrow: ${if (!weatherData.tomorrowHighTemp.isNaN()) TemperatureUtils.formatTemperature(weatherData.tomorrowHighTemp, "F") else "--°F"} / ${if (weatherData.tomorrowRainChance >= 0) weatherData.tomorrowRainChance else "--"}%"
                        )
                        // Set local time in bottom right
                        val localTime = try {
                            java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (e: Throwable) {
                            // Fallback for pre-API 26
                            val cal = java.util.Calendar.getInstance()
                            String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
                        }
                        views.setTextViewText(R.id.widget_time, "Updated: $localTime")
                    }
                    is WeatherResult.Error -> {
                        Log.d("WidgetUpdater", "Weather error: " + weatherResult.message)
                        views.setTextViewText(R.id.temp_celsius, "Weather")
                        views.setTextViewText(R.id.temp_fahrenheit, "Error")
                        views.setTextViewText(R.id.location_name, weatherResult.message)
                    }
                }
            }
            is LocationResult.Error -> {
                Log.d("WidgetUpdater", "Location error: " + locationResult.message)
                views.setTextViewText(R.id.temp_celsius, "Location")
                views.setTextViewText(R.id.temp_fahrenheit, "Error")
                views.setTextViewText(R.id.location_name, locationResult.message)
            }
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
