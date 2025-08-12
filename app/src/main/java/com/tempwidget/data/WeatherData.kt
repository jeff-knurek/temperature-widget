package com.tempwidget.data

/**
 * Data class representing weather information
 */
data class WeatherData(
    val currentTemperature: Double,  // Temperature in Fahrenheit (from API)
    val humidity: Double,            // Relative humidity percentage
    val dewPoint: Double,            // Dew point in Fahrenheit
    val rainChanceNext3h: Int = -1,  // % chance of rain in next 3 hours
    val locationName: String = "Unknown Location",
    // Next two days (skip today) forecast data
    val day1HighF: Double = Double.NaN,
    val day1LowF: Double = Double.NaN,
    val day1PrecipPct: Int = -1,
    val day1Icon: String = "",
    val day2HighF: Double = Double.NaN,
    val day2LowF: Double = Double.NaN,
    val day2PrecipPct: Int = -1,
    val day2Icon: String = ""
)

/**
 * Data class representing geographical coordinates
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String = "Unknown"
)

/**
 * Result wrapper for API calls
 */
sealed class WeatherResult {
    data class Success(val data: WeatherData) : WeatherResult()
    data class Error(val message: String) : WeatherResult()
}

/**
 * Result wrapper for location services
 */
sealed class LocationResult {
    data class Success(val location: Location) : LocationResult()
    data class Error(val message: String) : LocationResult()
}
