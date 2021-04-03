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
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nurram.project.qibla.MainActivity
import com.nurram.project.qibla.R

class GPSTracker(private val context: Context) : Service(), LocationListener {
    private lateinit var locationManager: LocationManager

    private var isGPSEnabled = false
    private var isNetworkEnabled = false

    var isLoading = MutableLiveData<Boolean>()
    private val location = MutableLiveData<Location>()

    @SuppressLint("MissingPermission")
    fun getLocation(): MutableLiveData<Location> {
        isLoading.postValue(true)

        locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if(!isGPSEnabled) {
            showSettingsAlert()
        } else if(!isNetworkEnabled) {
            Toast.makeText(context, "No Internet", Toast.LENGTH_SHORT).show()
        } else {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this)

            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            this.location.postValue(location)
        }

        return location
    }

    fun stopUsingGPS() {
        locationManager.removeUpdates(this@GPSTracker)
    }

    private fun showSettingsAlert() {
        val ctx = context as MainActivity

        AlertDialog.Builder(context).apply {
            setTitle(context.resources.getString(R.string.gps_settings_title))
            setMessage(context.resources.getString(R.string.gps_settings_text))
            setPositiveButton(context.resources.getString(R.string.settings_button_ok)) { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                ctx.startActivityForResult(intent, 1)
            }

            setNegativeButton(context.resources.getString(R.string.settings_button_cancel)) { dialog, _ -> dialog.cancel() }
            show()
        }
    }

    override fun onLocationChanged(location: Location) {
        this.location.postValue(location)
    }

    override fun onProviderDisabled(provider: String) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 100 // 10 meters
        private const val MIN_TIME_BW_UPDATES = (1000 * 60).toLong()
    }
}