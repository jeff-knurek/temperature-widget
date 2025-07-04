package com.tempwidget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tempwidget.location.LocationManager

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = LocationManager(this)

        // Check and request location permissions
        if (!locationManager.hasLocationPermission()) {
            requestLocationPermissions()
        } else {
            showPermissionGrantedMessage()
        }
    }

    /**
     * Requests location permissions from user
     */
    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        ActivityCompat.requestPermissions(
            this,
            permissions,
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Handles permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                     grantResults.getOrNull(1) == PackageManager.PERMISSION_GRANTED)) {

                    showPermissionGrantedMessage()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission is required for the weather widget to work properly.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showPermissionGrantedMessage() {
        Toast.makeText(
            this,
            "Weather widget is ready! You can now add it to your home screen.",
            Toast.LENGTH_LONG
        ).show()
    }
}
