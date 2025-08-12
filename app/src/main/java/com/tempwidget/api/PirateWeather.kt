package com.tempwidget.api

import android.content.Context
import com.tempwidget.R
import com.tempwidget.data.WeatherData
import com.tempwidget.data.WeatherResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import android.util.Log

/**
 * Pirate-Weather weather API implementation
 * https://pirateweather.net/en/latest/API/
 */
class PirateWeatherApi(private val context: Context) : WeatherApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherResult {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = context.getString(R.string.weather_api_base_url)
                val params = context.getString(R.string.weather_api_params)
                val apiKey = localProperties.getProperty("pirate_weather_api_key")

                val url = "$baseUrl/$apiKey/$latitude,$longitude?$params"
                Log.d("PirateWeatherApi", "Getting weather data for URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("PirateWeatherApi", "Weather service returned success: $responseBody")
                    if (responseBody != null) {
                        parseWeatherResponse(responseBody)
                    } else {
                        WeatherResult.Error("Empty response from weather service")
                    }
                } else {
                    Log.d("PirateWeatherApi", "Weather service returned error: ${response.code}")
                    WeatherResult.Error("Weather service returned error: ${response.code}")
                }
            } catch (e: Exception) {
                Log.d("PirateWeatherApi", "Network error: ${e.message}")
                WeatherResult.Error("Network error: ${e.message}")
            }
        }
    }

    private fun parseWeatherResponse(jsonString: String): WeatherResult {
        return try {
            val jsonObject = JSONObject(jsonString)
            val current = jsonObject.getJSONObject("currently")
            val temperature = current.getDouble("temperature")
            val humidity = current.getDouble("humidity") * 100
            val dewPoint = current.getDouble("dewPoint")
            // TODO: get the weather icon set that aligns with the icon in the response
            // possible icons:
            //      https://github.com/b-reich/MMM-PirateSkyForecast/blob/main/icons/iconsets.png
            //      https://github.com/bramkragten/weather-card/tree/master/icons
            // but maybe just some generic icons?
            // val icon = current.getString("icon")

            val hourly = jsonObject.getJSONObject("hourly").getJSONArray("data")
            var maxRainChanceNext3Hours = 0.0
            var maxRainChanceNextDay = 0.0
            var tomorrowHighTemp = 0.0
            val totalHours = hourly.length().coerceAtMost(20)
            for (i in 0 until totalHours) {
                val hourData = hourly.getJSONObject(i)
                val precipProbability = hourData.optDouble("precipProbability", 0.0)
                // get Precipitation chance for the first 3 hours, and next day after that
                if (i < 3) {
                    maxRainChanceNext3Hours = maxOf(maxRainChanceNext3Hours, precipProbability)
                } else {
                    maxRainChanceNextDay = maxOf(maxRainChanceNextDay, precipProbability)
                    tomorrowHighTemp = maxOf(tomorrowHighTemp, hourData.optDouble("temperature"))
                }
            }

            val weatherData = WeatherData(
                currentTemperature = temperature,
                humidity = humidity,
                dewPoint = dewPoint,
                rainChanceNext3h = (maxRainChanceNext3Hours*100).toInt(),
                tomorrowHighTemp = tomorrowHighTemp,
                tomorrowRainChance = (maxRainChanceNextDay*100).toInt()
            )
            WeatherResult.Success(weatherData)
        } catch (e: Exception) {
            WeatherResult.Error("Failed to parse weather data: ${e.message}")
        }
    }

    override fun getServiceName(): String = "Pirate-Weather"
}
