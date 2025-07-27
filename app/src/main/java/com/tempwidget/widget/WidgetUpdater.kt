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
import android.util.TypedValue

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
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        // Calculate scaling factor based on width (minWidth is in dp)
        val scale = (minWidth / 200f).coerceIn(1f, 2.5f)
        // Temp text size: scale between 48sp and 130sp
        val tempTextSizeF = (48f + (130f - 48f) * ((scale - 1f) / (2.5f - 1f))).coerceIn(48f, 130f)
        val tempTextSizeC = (40f + (110f - 40f) * ((scale - 1f) / (2.5f - 1f))).coerceIn(48f, 130f)
        // Other text fields: default and max sizes
        val humidityDefault = 14f
        val dewPointDefault = 14f
        val rainChanceDefault = 14f
        val tomorrowDefault = 14f
        val locationDefault = 18f
        val widgetTimeDefault = 12f
        // Max sizes
        val humidityMax = humidityDefault * 2.5f
        val dewPointMax = dewPointDefault * 2.5f
        val rainChanceMax = rainChanceDefault * 2.5f
        val tomorrowMax = tomorrowDefault * 2.5f
        val locationMax = locationDefault * 2.5f
        val widgetTimeMax = widgetTimeDefault * 2.5f
        // Scaled sizes
        val humiditySize = (humidityDefault + (humidityMax - humidityDefault) * ((scale - 1f) / (2.5f - 1f))).coerceIn(humidityDefault, humidityMax)
        val dewPointSize = (dewPointDefault + (dewPointMax - dewPointDefault) * ((scale - 1f) / (2.5f - 1f))).coerceIn(dewPointDefault, dewPointMax)
        val rainChanceSize = (rainChanceDefault + (rainChanceMax - rainChanceDefault) * ((scale - 1f) / (2.5f - 1f))).coerceIn(rainChanceDefault, rainChanceMax)
        val tomorrowSize = (tomorrowDefault + (tomorrowMax - tomorrowDefault) * ((scale - 1f) / (2.5f - 1f))).coerceIn(tomorrowDefault, tomorrowMax)
        val locationSize = (locationDefault + (locationMax - locationDefault) * ((scale - 1f) / (2.5f - 1f))).coerceIn(locationDefault, locationMax)
        val widgetTimeSize = (widgetTimeDefault + (widgetTimeMax - widgetTimeDefault) * ((scale - 1f) / (2.5f - 1f))).coerceIn(widgetTimeDefault, widgetTimeMax)
        // Set text sizes
        views.setTextViewTextSize(R.id.temp_celsius, TypedValue.COMPLEX_UNIT_SP, tempTextSizeC)
        views.setTextViewTextSize(R.id.temp_fahrenheit, TypedValue.COMPLEX_UNIT_SP, tempTextSizeF)
        views.setTextViewTextSize(R.id.humidity, TypedValue.COMPLEX_UNIT_SP, humiditySize)
        views.setTextViewTextSize(R.id.dew_point, TypedValue.COMPLEX_UNIT_SP, dewPointSize)
        views.setTextViewTextSize(R.id.rain_chance, TypedValue.COMPLEX_UNIT_SP, rainChanceSize)
        views.setTextViewTextSize(R.id.tomorrow_forecast, TypedValue.COMPLEX_UNIT_SP, tomorrowSize)
        views.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, locationSize)
        views.setTextViewTextSize(R.id.widget_time, TypedValue.COMPLEX_UNIT_SP, widgetTimeSize)
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
