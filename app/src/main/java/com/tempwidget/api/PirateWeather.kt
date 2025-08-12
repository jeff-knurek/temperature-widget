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
import com.tempwidget.BuildConfig
import kotlin.math.roundToInt

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
                val apiKey = BuildConfig.PIRATE_WEATHER_API_KEY

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

            val hourly = jsonObject.getJSONObject("hourly").getJSONArray("data")
            var maxRainChanceNext3Hours = 0.0
            val totalHours = hourly.length().coerceAtMost(5)
            for (i in 0 until totalHours) {
                val hourData = hourly.getJSONObject(i)
                val precipProbability = hourData.optDouble("precipProbability", 0.0)
                // get Precipitation chance for the first 3 hours, and next day after that
                if (i < 3) {
                    maxRainChanceNext3Hours = maxOf(maxRainChanceNext3Hours, precipProbability)
                }
            }

            // Parse daily data: skip today (index 0), take next two days (index 1 and 2)
            var day1HighF = Double.NaN
            var day1LowF = Double.NaN
            var day1PrecipPct = -1
            var day1Icon = ""
            var day2HighF = Double.NaN
            var day2LowF = Double.NaN
            var day2PrecipPct = -1
            var day2Icon = ""
            try {
                val dailyArray = jsonObject.optJSONObject("daily")?.optJSONArray("data")
                if (dailyArray != null) {
                    if (dailyArray.length() > 1) {
                        val d1 = dailyArray.optJSONObject(1)
                        if (d1 != null) {
                            day1HighF = d1.optDouble("temperatureHigh", Double.NaN)
                            day1LowF = d1.optDouble("temperatureLow", Double.NaN)
                            val p1 = d1.optDouble("precipProbability", Double.NaN)
                            day1PrecipPct = if (p1.isNaN()) -1 else (p1 * 100).roundToInt()
                            day1Icon = d1.optString("icon", "")
                        }
                    }
                    if (dailyArray.length() > 2) {
                        val d2 = dailyArray.optJSONObject(2)
                        if (d2 != null) {
                            day2HighF = d2.optDouble("temperatureHigh", Double.NaN)
                            day2LowF = d2.optDouble("temperatureLow", Double.NaN)
                            val p2 = d2.optDouble("precipProbability", Double.NaN)
                            day2PrecipPct = if (p2.isNaN()) -1 else (p2 * 100).roundToInt()
                            day2Icon = d2.optString("icon", "")
                        }
                    }
                }
            } catch (_: Throwable) {
                // keep defaults
            }

            val weatherData = WeatherData(
                currentTemperature = temperature,
                humidity = humidity,
                dewPoint = dewPoint,
                rainChanceNext3h = (maxRainChanceNext3Hours*100).toInt(),
                day1HighF = day1HighF,
                day1LowF = day1LowF,
                day1PrecipPct = day1PrecipPct,
                day1Icon = day1Icon,
                day2HighF = day2HighF,
                day2LowF = day2LowF,
                day2PrecipPct = day2PrecipPct,
                day2Icon = day2Icon
            )
            WeatherResult.Success(weatherData)
        } catch (e: Exception) {
            WeatherResult.Error("Failed to parse weather data: ${e.message}")
        }
    }

    override fun getServiceName(): String = "Pirate-Weather"
}
