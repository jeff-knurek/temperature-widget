package com.tempwidget.api

import com.tempwidget.data.WeatherData
import com.tempwidget.data.WeatherResult

/**
 * Interface for weather API implementations
 * This allows for easy switching between different weather services
 */
interface WeatherApi {

    /**
     * Fetches weather data for given coordinates
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return WeatherResult containing weather data or error
     */
    suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherResult

    /**
     * Gets the name of the weather service
     * @return String name of the weather service
     */
    fun getServiceName(): String
}
