@file:Suppress("SameParameterValue", "PrivatePropertyName")

package com.nurram.project.qibla.compass

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class Compass(context: Context) : SensorEventListener {
    interface CompassListener {
        fun onNewAzimuth(azimuth: Float)
    }

    private lateinit var listener: CompassListener

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val asensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val msensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val aData = FloatArray(3)
    private val mData = FloatArray(3)
    private val R = FloatArray(9)
    private val I = FloatArray(9)
    private var azimuthFix = 0f

    fun start(context: Context) {
        sensorManager.registerListener(this, asensor,
                SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, msensor,
                SensorManager.SENSOR_DELAY_GAME)

        val manager = context.packageManager
        val haveAS = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
        val haveCS = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)

        if (!haveAS || !haveCS) {
            sensorManager.unregisterListener(this, asensor)
            sensorManager.unregisterListener(this, msensor)
            dialogError(context)
        }
    }

    private fun dialogError(context: Context) {
        AlertDialog.Builder(context).apply {
            setTitle("Error")
            setCancelable(false)
            setIcon(android.R.drawable.ic_dialog_alert)
            setMessage("Sensor Not Exist")
            setNegativeButton("OK") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                if (context is Activity) context.finish()
            }

            create().show()
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    private fun setAzimuthFix(fix: Float) {
        azimuthFix = fix
    }

    fun resetAzimuthFix() {
        setAzimuthFix(0f)
    }

    fun setListener(l: CompassListener?) {
        listener = l!!
    }

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f
        synchronized(this) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                aData[0] = alpha * aData[0] + (1 - alpha) * event.values[0]
                aData[1] = alpha * aData[1] + (1 - alpha) * event.values[1]
                aData[2] = alpha * aData[2] + (1 - alpha) * event.values[2]
            }
            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                mData[0] = alpha * mData[0] + (1 - alpha) * event.values[0]
                mData[1] = alpha * mData[1] + (1 - alpha) * event.values[1]
                mData[2] = alpha * mData[2] + (1 - alpha) * event.values[2]
            }

            val success = SensorManager.getRotationMatrix(R, I, aData, mData)

            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuth = (azimuth + azimuthFix + 360) % 360
                listener.onNewAzimuth(azimuth)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}