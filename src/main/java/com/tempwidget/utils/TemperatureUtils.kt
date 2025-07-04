package com.tempwidget.utils

import kotlin.math.roundToInt

/**
 * Utility class for temperature conversions
 */
object TemperatureUtils {

    /**
     * Converts Fahrenheit to Celsius
     * @param fahrenheit Temperature in Fahrenheit
     * @return Temperature in Celsius
     */
    fun fahrenheitToCelsius(fahrenheit: Double): Double {
        return (fahrenheit - 32) * 5.0 / 9.0
    }

    /**
     * Converts Celsius to Fahrenheit
     * @param celsius Temperature in Celsius
     * @return Temperature in Fahrenheit
     */
    fun celsiusToFahrenheit(celsius: Double): Double {
        return celsius * 9.0 / 5.0 + 32
    }

    /**
     * Formats temperature for display
     * @param temperature Temperature value
     * @param unit Temperature unit (C or F)
     * @return Formatted temperature string
     */
    fun formatTemperature(temperature: Double, unit: String): String {
        return "${temperature.roundToInt()}°$unit"
    }

    /**
     * Checks if temperature is valid
     * @param temperature Temperature value
     * @return true if temperature is within reasonable bounds
     */
    fun isValidTemperature(temperature: Double): Boolean {
        return temperature >= -100 && temperature <= 100
    }
}
