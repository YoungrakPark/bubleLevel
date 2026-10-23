package com.bubblelevel.app.level

import android.content.Context
import androidx.core.content.edit

/** Calibration and the alert switch, kept across launches. */
class LevelSettings(context: Context) {

    private val prefs = context.getSharedPreferences("level", Context.MODE_PRIVATE)

    var offset: Tilt
        get() = Tilt(prefs.getFloat("offsetX", 0f), prefs.getFloat("offsetY", 0f))
        set(value) = prefs.edit { putFloat("offsetX", value.x).putFloat("offsetY", value.y) }

    var alertOn: Boolean
        get() = prefs.getBoolean("alertOn", true)
        set(value) = prefs.edit { putBoolean("alertOn", value) }
}
