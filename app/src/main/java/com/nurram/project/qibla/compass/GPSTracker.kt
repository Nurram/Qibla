package com.nurram.project.qibla.compass

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import com.nurram.project.qibla.R

class GPSTracker(private val context: Context) : Service(), LocationListener {
    private lateinit var locationManager: LocationManager

    private var isGPSEnabled = false
    private var isNetworkEnabled = false
    private var canGetLocation = false

    var loc: Location? = null
    var lat = 0.0
    var lng = 0.0

    @SuppressLint("MissingPermission")
    fun getLocation(): Location? {
        try {
            locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                canGetLocation = true

                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this)

                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                    if (loc != null) {
                        lat = loc!!.latitude
                        lng = loc!!.longitude
                    }
                }

                if (isGPSEnabled) {
                    if (loc == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this)

                        loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (loc != null) {
                            lat = loc!!.latitude
                            lng = loc!!.longitude
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return loc
    }

    fun stopUsingGPS() {
        locationManager.removeUpdates(this@GPSTracker)
    }

    fun getLatitude(): Double {
        if (loc != null) {
            lat = loc!!.latitude
        }

        return lat
    }

    fun getLongitude(): Double {
        if (loc != null) {
            lng = loc!!.longitude
        }

        return lng
    }

    fun canGetLocation(): Boolean {
        return canGetLocation
    }

    fun showSettingsAlert() {
        AlertDialog.Builder(context).apply {
            setTitle(context.resources.getString(R.string.gps_settings_title))
            setMessage(context.resources.getString(R.string.gps_settings_text))
            setPositiveButton(context.resources.getString(R.string.settings_button_ok)) { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            }
            setNegativeButton(context.resources.getString(R.string.settings_button_cancel)) { dialog, _ -> dialog.cancel() }
            show()
        }
    }

    override fun onLocationChanged(location: Location) {
// TODO Auto-generated method stub
    }

    override fun onProviderDisabled(provider: String) {
// TODO Auto-generated method stub
    }

    override fun onProviderEnabled(provider: String) {
// TODO Auto-generated method stub
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
// TODO Auto-generated method stub
    }

    override fun onBind(intent: Intent): IBinder? {
// TODO Auto-generated method stub
        return null
    }

    companion object {
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 100 // 10 meters
        private const val MIN_TIME_BW_UPDATES = (1000 * 60).toLong()
    }

    init {
        loc = getLocation()
    }
}