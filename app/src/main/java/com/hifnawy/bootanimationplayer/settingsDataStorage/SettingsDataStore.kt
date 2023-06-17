package com.hifnawy.bootanimationplayer.settingsDataStorage

import android.content.Context
import android.content.SharedPreferences
import com.hifnawy.bootanimationplayer.R

class SettingsDataStore(context: Context) {
    private val sharedPreferences: SharedPreferences

    var fps
        get() = sharedPreferences.getInt("FPS", 0)
        set(value) {
            with(sharedPreferences.edit()) {
                putInt("FPS", value)
            }
        }

    var resolutionScale
        get() = sharedPreferences.getFloat("scale", 0f)
        set(value) {
            with(sharedPreferences.edit()) {
                putFloat("scale", value)
            }
        }

    init {
        sharedPreferences = context.getSharedPreferences(
            "${context.getString(R.string.app_name)}_Settings",
            Context.MODE_PRIVATE
        )
    }
}