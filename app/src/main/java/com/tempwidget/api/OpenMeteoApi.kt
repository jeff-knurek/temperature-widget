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
 * Open-Meteo weather API implementation
 * https://open-meteo.com/en/docs
 */
class OpenMeteoApi(private val context: Context) : WeatherApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherResult {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = context.getString(R.string.weather_api_base_url)
                val params = context.getString(R.string.weather_api_params)

                val url = "$baseUrl?latitude=$latitude&longitude=$longitude&$params"
                Log.d("OpenMeteoApi", "Getting weather data for URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("OpenMeteoApi", "Weather service returned success: $responseBody")
                    if (responseBody != null) {
                        parseWeatherResponse(responseBody)
                    } else {
                        WeatherResult.Error("Empty response from weather service")
                    }
                } else {
                    Log.d("OpenMeteoApi", "Weather service returned error: ${response.code}")
                    WeatherResult.Error("Weather service returned error: ${response.code}")
                }
            } catch (e: Exception) {
                Log.d("OpenMeteoApi", "Network error: ${e.message}")
                WeatherResult.Error("Network error: ${e.message}")
            }
        }
    }

    private fun parseWeatherResponse(jsonString: String): WeatherResult {
        return try {
            val jsonObject = JSONObject(jsonString)
            val current = jsonObject.getJSONObject("current")
            val temperature = current.getDouble("temperature_2m")
            val humidity = current.getDouble("relative_humidity_2m")
            // Get dew point from hourly data (current hour)
            val hourly = jsonObject.getJSONObject("hourly")
            val dewPointArray = hourly.getJSONArray("dew_point_2m")
            val precipProbArray = hourly.getJSONArray("precipitation_probability")
            val dewPoint = if (dewPointArray.length() > 0) {
                dewPointArray.getDouble(0)
            } else {
                0.0
            }
            // Find the index for now (first hour is now)
            val nowIndex = 0
            // Rain chance next 3 hours: max of next 3 precipProbArray values
            var rainChanceNext3h = -1
            for (i in nowIndex until minOf(nowIndex + 3, precipProbArray.length())) {
                rainChanceNext3h = maxOf(rainChanceNext3h, precipProbArray.getInt(i))
            }
            val weatherData = WeatherData(
                currentTemperature = temperature,
                humidity = humidity,
                dewPoint = dewPoint,
                rainChanceNext3h = rainChanceNext3h
            )
            WeatherResult.Success(weatherData)
        } catch (e: Exception) {
            WeatherResult.Error("Failed to parse weather data: ${e.message}")
        }
    }

    override fun getServiceName(): String = "Open-Meteo"
}
