package com.nurram.project.qibla

import android.Manifest
import android.location.Geocoder
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.nurram.project.qibla.compass.Compass
import com.nurram.project.qibla.compass.Compass.CompassListener
import com.nurram.project.qibla.compass.GPSTracker
import com.nurram.project.qibla.databinding.ActivityMainBinding
import com.nurram.project.qibla.utils.GONE
import com.nurram.project.qibla.utils.PrefUtils
import com.nurram.project.qibla.utils.VISIBLE
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var compass: Compass
    private lateinit var pref: PrefUtils

    private var gps: GPSTracker? = null

    private var currentAzimuth = 0f

    val onActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            setupCompass()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        pref = PrefUtils(this)
        setupCompass()

        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        setupCompass()
        return true
    }

    private fun setupCompass() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    fetchGPS()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }
            }).check()

        compass = Compass(this)
        val cl = object : CompassListener {
            override fun onNewAzimuth(azimuth: Float) {
                adjustDial(azimuth)
                adjustArrowQibla(azimuth)
            }
        }
        compass.setListener(cl)
    }

    private fun adjustDial(azimuth: Float) {
        val an: Animation = RotateAnimation(
            -currentAzimuth, -azimuth,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f
        )
        currentAzimuth = azimuth
        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true
        binding.ivCompass.startAnimation(an)
    }

    private fun adjustArrowQibla(azimuth: Float) {
        val qiblaDegree = pref.getFloat("kiblat_derajat")
        val an: Animation = RotateAnimation(
            -currentAzimuth + qiblaDegree, -azimuth,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f
        )

        currentAzimuth = azimuth
        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true
        binding.ivMeccaLogo.startAnimation(an)
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

    private fun fetchGPS() {
        gps = GPSTracker(this)
        gps!!.isLoading.observe(this, {
            if (it) {
                binding.apply {
                    tvLocation.GONE()
                    tvDegree.GONE()
                    ivMeccaLogo.GONE()
                    ivCompass.GONE()
                    qiblaProgress.VISIBLE()
                }
            } else {
                binding.apply {
                    tvLocation.VISIBLE()
                    tvDegree.VISIBLE()
                    ivMeccaLogo.VISIBLE()
                    ivCompass.VISIBLE()
                }
            }
        })
        gps!!.getLocation().observe(this, {
            if (it != null) {
                if (abs(it.latitude) > 0 || abs(it.longitude) > 0) {
                    gps!!.isLoading.postValue(false)

                    val lat = it.latitude
                    val lng = it.longitude

                    val geocoder = Geocoder(this, Locale.getDefault())
                    val address = geocoder.getFromLocation(lat, lng, 1)
                    val strYourLocation = address[0].locality
                    binding.tvLocation.text = strYourLocation

                    if (lat < 0.001 && lng < 0.001) {
                        binding.ivMeccaLogo.GONE()
                        binding.tvDegree.text = resources.getString(R.string.location_not_ready)
                        binding.tvLocation.text =
                            resources.getString(R.string.location_not_ready)
                    } else {
                        val kaabaLng = 39.826206
                        val kaabaLat = Math.toRadians(21.422487)
                        val myLatRad = Math.toRadians(lat)
                        val longDiff = Math.toRadians(kaabaLng - lng)
                        val y = sin(longDiff) * cos(kaabaLat)
                        val x = cos(myLatRad) * sin(kaabaLat) - sin(myLatRad) * cos(kaabaLat) * cos(
                            longDiff
                        )
                        val result = (Math.toDegrees(atan2(y, x)) + 360) % 360
                        val strKaabaDirection: String =
                            String.format(Locale.ENGLISH, "%.0f", result.toFloat()) +
                                    " ${resources.getString(R.string.degree)} " +
                                    getDirectionString(result.toFloat())


                        pref.save("kiblat_derajat", result.toFloat())
                        binding.tvDegree.text = strKaabaDirection
                        binding.ivMeccaLogo.GONE()
                    }
                } else {
                    binding.ivMeccaLogo.GONE()
                    binding.ivCompass.GONE()
                }
            }
        })
    }
}