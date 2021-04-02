package com.nurram.project.qibla

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.nurram.project.qibla.compass.Compass
import com.nurram.project.qibla.compass.Compass.CompassListener
import com.nurram.project.qibla.compass.GPSTracker
import com.nurram.project.qibla.databinding.ActivityMainBinding
import com.nurram.project.qibla.utils.GONE
import com.nurram.project.qibla.utils.PrefUtils
import com.nurram.project.qibla.utils.VISIBLE
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var compass: Compass
    private lateinit var pref: PrefUtils

    private var gps: GPSTracker? = null

    private var currentAzimuth = 0f
    private val permissionCode = 1221

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pref = PrefUtils(this)
        gps = GPSTracker(this)
        setupCompass()
    }

    override fun onResume() {
        super.onResume()
        compass.start(this)
    }

    override fun onPause() {
        super.onPause()
        compass.stop()
    }

    override fun onStop() {
        super.onStop()

        if (gps != null) {
            gps!!.stopUsingGPS()
            gps = null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupCompass() {
        val permissionGranted = pref.getBoolean("permission_granted")
        if (permissionGranted) {
            getBearing()
        } else {
            binding.qiblaDegree.text = resources.getString(R.string.msg_permission_not_granted_yet)
            binding.qiblaLocation.text = resources.getString(R.string.msg_permission_not_granted_yet)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION),
                        permissionCode)
            } else {
                fetchGPS()
            }
        }
        compass = Compass(this)
        val cl = CompassListener { azimuth ->
            adjustDial(azimuth)
            adjustArrowQibla(azimuth)
        }
        compass.setListener(cl)
    }


    private fun adjustDial(azimuth: Float) {
        val an: Animation = RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f)
        currentAzimuth = azimuth
        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true
        binding.qiblaDirection.startAnimation(an)
    }

    private fun adjustArrowQibla(azimuth: Float) {
        val qiblaDegree = pref.getFloat("kiblat_derajat")
        val an: Animation = RotateAnimation(-currentAzimuth + qiblaDegree, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f)

        currentAzimuth = azimuth
        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true
        binding.qiblaMecca.startAnimation(an)
        if (qiblaDegree > 0) {
            binding.qiblaMecca.VISIBLE()
        } else {
            binding.qiblaMecca.GONE()
        }
    }

    @SuppressLint("MissingPermission")
    fun getBearing() {
        val qiblaDeg = pref.getFloat("kiblat_derajat")
        if (qiblaDeg > 0.0001) {
            val strYourLocation: String = if (gps?.location != null) resources.getString(R.string.your_location) + " " + gps!!.location.latitude + ", " + gps!!.location.longitude else resources.getString(R.string.unable_to_get_your_location)
            binding.qiblaLocation.text = strYourLocation
            val strKaabaDirection: String = java.lang.String.format(Locale.ENGLISH, "%.0f", qiblaDeg)
                    .toString() + " " + resources.getString(R.string.degree) + " " + getDirectionString(qiblaDeg)
            binding.qiblaDegree.text = strKaabaDirection


            binding.qiblaMecca.GONE()
        } else {
            fetchGPS()
        }
    }

    private fun getDirectionString(azimuthDegrees: Float): String {
        var where = "NW"
        if (azimuthDegrees >= 350 || azimuthDegrees <= 10) where = "N"
        if (azimuthDegrees < 350 && azimuthDegrees > 280) where = "NW"
        if (azimuthDegrees <= 280 && azimuthDegrees > 260) where = "W"
        if (azimuthDegrees <= 260 && azimuthDegrees > 190) where = "SW"
        if (azimuthDegrees <= 190 && azimuthDegrees > 170) where = "S"
        if (azimuthDegrees <= 170 && azimuthDegrees > 100) where = "SE"
        if (azimuthDegrees <= 100 && azimuthDegrees > 80) where = "E"
        if (azimuthDegrees <= 80 && azimuthDegrees > 10) where = "NE"
        return where
    }


    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String?>,
                                            grantResults: IntArray) {
        if (requestCode == permissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pref.save("permission_granted", true)
                binding.qiblaDegree.text = resources.getString(R.string.msg_permission_granted)
                binding.qiblaLocation.text = resources.getString(R.string.msg_permission_granted)
                binding.qiblaMecca.GONE()
                fetchGPS()
            } else {
                Toast.makeText(applicationContext, resources.getString(R.string.toast_permission_required), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun fetchGPS() {
        val result: Double
        gps = GPSTracker(this)
        if (gps!!.canGetLocation()) {
            val myLat = gps!!.latitude
            val myLng = gps!!.longitude
            val strYourLocation = resources.getString(R.string.your_location) + " " + myLat + ", " + myLng
            binding.qiblaLocation.text = strYourLocation

            if (myLat < 0.001 && myLng < 0.001) {
                binding.qiblaMecca.GONE()
                binding.qiblaDegree.text = resources.getString(R.string.location_not_ready)
                binding.qiblaLocation.text = resources.getString(R.string.location_not_ready)
               } else {
                val kaabaLng = 39.826206
                val kaabaLat = Math.toRadians(21.422487)
                val myLatRad = Math.toRadians(myLat)
                val longDiff = Math.toRadians(kaabaLng - myLng)
                val y = sin(longDiff) * cos(kaabaLat)
                val x = cos(myLatRad) * sin(kaabaLat) - sin(myLatRad) * cos(kaabaLat) * cos(longDiff)
                result = (Math.toDegrees(atan2(y, x)) + 360) % 360
                pref.save("kiblat_derajat", result.toFloat())
                val strKaabaDirection: String = java.lang.String.format(Locale.ENGLISH, "%.0f", result.toFloat())
                        .toString() + " " + resources.getString(R.string.degree) + " " + getDirectionString(result.toFloat())
                binding.qiblaDegree.text = strKaabaDirection
                binding.qiblaMecca.GONE()
            }
        } else {
            gps!!.showSettingsAlert()

            binding.qiblaMecca.GONE()
            binding.qiblaDegree.text = resources.getString(R.string.pls_enable_location)
            binding.qiblaLocation.text = resources.getString(R.string.pls_enable_location)
        }
    }
}