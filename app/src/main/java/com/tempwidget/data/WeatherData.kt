package com.tempwidget.data

/**
 * Data class representing weather information
 */
data class WeatherData(
    val currentTemperature: Double,  // Temperature in Fahrenheit (from API)
    val humidity: Double,            // Relative humidity percentage
    val dewPoint: Double,            // Dew point in Fahrenheit
    val locationName: String = "Unknown Location"
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
