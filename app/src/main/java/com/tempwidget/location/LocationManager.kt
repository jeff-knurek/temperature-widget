package com.tempwidget.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Tasks
import com.tempwidget.data.LocationResult
import com.tempwidget.data.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import android.util.Log

/**
 * Manages location services for the widget
 * Uses FusedLocationProviderClient for Google devices, falls back to default coordinates otherwise
 */
class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

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

    suspend fun getCurrentLocation(): LocationResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasLocationPermission()) {
                    return@withContext LocationResult.Error("Location permission not granted")
                }
                val location = try {
                    Tasks.await(fusedLocationClient.lastLocation)
                } catch (e: Exception) {
                    Log.e("LocationManager", "Error getting last location: ${e.message}")
                    null
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
                    // fallback to default coordinates
                    LocationResult.Success(
                        Location(
                            latitude = 42.0824,
                            longitude = -71.3967,
                            name = "MA (default)"
                        )
                    )
                }
            } catch (e: Exception) {
                LocationResult.Error("Location error: ${e.message}")
            }
        }
    }

    private suspend fun getLocationName(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses?.isNotEmpty() == true) {
                    val address = addresses[0]
                    address.locality
                        ?: address.subAdminArea
                        ?: address.adminArea
                        ?: address.countryName
                        ?: "Unknown Location"
                } else {
                    "Unknown Location"
                }
            } catch (e: Exception) {
                Log.e("LocationManager", "Error getting location name: ${e.message}")
                "Unknown Location"
            }
        }
    }

    fun isLocationEnabled(): Boolean {
        // FusedLocationProviderClient does not expose provider state, so always return true
        return true
    }
}
