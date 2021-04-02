package com.nurram.project.qibla.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity

class PrefUtils(context: Context) {
    private var pref = context.getSharedPreferences("qibla", AppCompatActivity.MODE_PRIVATE)

    fun save(key: String, value: Boolean) {
        val edit: SharedPreferences.Editor = pref.edit()
        edit.putBoolean(key, value)
        edit.apply()
    }

    fun getBoolean(key: String): Boolean {
        return pref.getBoolean(key, false)
    }

    fun save(key: String, value: Float) {
        val edit: SharedPreferences.Editor = pref.edit()
        edit.putFloat(key, value)
        edit.apply()
    }

    fun getFloat(key: String): Float {
        return pref.getFloat(key, 0f)
    }

}