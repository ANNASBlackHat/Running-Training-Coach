package com.runningcompanion.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.runningcompanion.app.domain.model.GpsPoint

interface LocationProvider {
    fun start(onLocation: (GpsPoint) -> Unit)
    fun stop()
}

class AndroidLocationManager(
    private val context: Context
) : LocationProvider {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var locationCallback: ((GpsPoint) -> Unit)? = null

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val point = GpsPoint(
                timestamp = location.time,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                altitude = if (location.hasAltitude()) location.altitude else null,
                speed = if (location.hasSpeed()) location.speed else null
            )
            locationCallback?.invoke(point)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    override fun start(onLocation: (GpsPoint) -> Unit) {
        this.locationCallback = onLocation
        val lm = locationManager ?: return

        try {
            // Register for GPS provider: 1000ms interval, 0m distance interval (time-based)
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    override fun stop() {
        try {
            locationManager?.removeUpdates(listener)
        } catch (_: Exception) {}
        locationCallback = null
    }
}
