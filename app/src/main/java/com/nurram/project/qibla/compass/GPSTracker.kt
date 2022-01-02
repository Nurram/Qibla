package com.nurram.project.qibla.compass

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.nurram.project.qibla.MainActivity
import com.nurram.project.qibla.R


class GPSTracker(private val context: Context) : Service() {
    private lateinit var locationManager: LocationManager

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var isGPSEnabled = false
    private var isNetworkEnabled = false

    var isLoading = MutableLiveData<Boolean>()
    private val location = MutableLiveData<Location>()


    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            location.postValue(locationResult.locations[0])
        }
    }

    @Suppress("SENSELESS_COMPARISON")
    @SuppressLint("MissingPermission")
    fun getLocation(): LiveData<Location> {
        isLoading.postValue(true)

        locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGPSEnabled) {
            showSettingsAlert()
        } else if (!isNetworkEnabled) {
            Toast.makeText(context, "No Internet", Toast.LENGTH_SHORT).show()
        } else {
            val locationRequest = LocationRequest.create().apply {
                interval = 60000
                fastestInterval = 10000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            if (fusedLocationProviderClient == null) {
                fusedLocationProviderClient =
                    LocationServices.getFusedLocationProviderClient(context)
                fusedLocationProviderClient!!.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }

            isLoading.postValue(false)
        }

        return location
    }

    fun stopUsingGPS() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
    }

    private fun showSettingsAlert() {
        val ctx = context as MainActivity

        AlertDialog.Builder(context).apply {
            setTitle(context.resources.getString(R.string.gps_settings_title))
            setMessage(context.resources.getString(R.string.gps_settings_text))
            setPositiveButton(context.resources.getString(R.string.settings_button_ok)) { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                ctx.onActivityResult.launch(intent)
            }
            setCancelable(false)
            setNegativeButton(context.resources.getString(R.string.settings_button_cancel)) { dialog, _ ->
                dialog.cancel()
                isLoading.postValue(false)
            }
            show()
        }
    }

    override fun onBind(intent: Intent): IBinder? = null
}