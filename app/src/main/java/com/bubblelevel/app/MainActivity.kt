package com.bubblelevel.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bubblelevel.app.level.GravitySensor
import com.bubblelevel.app.level.LevelSettings
import com.bubblelevel.app.ui.LevelScreen

class MainActivity : ComponentActivity() {

    private lateinit var sensor: GravitySensor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The screen is always dark, so the bar icons stay light even when the phone is in light mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensor = GravitySensor(this)
        val settings = LevelSettings(this)
        val vibrator = vibrator()
        setContent {
            LevelScreen(
                sensor,
                settings,
                onLevel = { vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)) },
                // Also drops the app from the recents list, so exiting feels final.
                onExit = { finishAndRemoveTask() },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        sensor.start()
    }

    override fun onPause() {
        sensor.stop()
        super.onPause()
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
}
