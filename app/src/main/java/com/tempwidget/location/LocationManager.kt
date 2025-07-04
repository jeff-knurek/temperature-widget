package com.tempwidget.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.tempwidget.data.Location
import com.tempwidget.data.LocationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Manages location services for the widget
 * Avoids Google Play Services dependency for Fire tablet compatibility
 */
class LocationManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val geocoder = Geocoder(context, Locale.getDefault())

    /**
     * Checks if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Gets the current location using the last known location
     * @return LocationResult containing location data or error
     */
    suspend fun getCurrentLocation(): LocationResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasLocationPermission()) {
                    return@withContext LocationResult.Error("Location permission not granted")
                }

                // Try to get last known location from different providers
                val location = try {
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                } catch (e: SecurityException) {
                    return@withContext LocationResult.Error("Security exception: ${e.message}")
                }

                if (location != null) {
                    val locationName = getLocationName(location.latitude, location.longitude)
                    LocationResult.Success(
                        Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            name = locationName
                        )
                    )
                } else {
                    LocationResult.Error("Unable to get location. Please ensure location services are enabled.")
                }
            } catch (e: Exception) {
                LocationResult.Error("Location error: ${e.message}")
            }
        }
    }

    /**
     * Gets location name from coordinates using reverse geocoding
     */
    private suspend fun getLocationName(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses?.isNotEmpty() == true) {
                    val address = addresses[0]
                    // Try to get city name, or locality, or administrative area
                    address.locality
                        ?: address.subAdminArea
                        ?: address.adminArea
                        ?: address.countryName
                        ?: "Unknown Location"
                } else {
                    "Unknown Location"
                }
            } catch (e: Exception) {
                "Unknown Location"
            }
        }
    }

    /**
     * Checks if location services are enabled
     */
    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
