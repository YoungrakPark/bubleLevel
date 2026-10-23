package com.bubblelevel.app.level

import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tan

/** How the phone is lying, from the axis gravity mostly runs along. */
enum class Pose { FLAT, PORTRAIT, LANDSCAPE }

/** The x vial means nothing when the phone stands on its long edge, the y vial when it stands upright. */
fun Pose.measuresX() = this != Pose.LANDSCAPE
fun Pose.measuresY() = this != Pose.PORTRAIT

/** Elevation of the device x and y axes in degrees; + means the right / top end is raised. */
data class Tilt(val x: Float, val y: Float) {
    operator fun minus(other: Tilt) = Tilt(x - other.x, y - other.y)
}

/**
 * A gravity reading points up the axis that is raised, so each axis's elevation is
 * asin(component / |g|). Like a real tube vial this holds in any pose, flat or upright.
 */
fun tiltOf(gx: Float, gy: Float, gz: Float): Tilt {
    val g = sqrt(gx * gx + gy * gy + gz * gz)
    if (g == 0f) return Tilt(0f, 0f)
    return Tilt(elevation(gx / g), elevation(gy / g))
}

private fun elevation(ratio: Float) =
    Math.toDegrees(asin(ratio.coerceIn(-1f, 1f).toDouble())).toFloat()

fun poseOf(gx: Float, gy: Float, gz: Float): Pose {
    val ax = abs(gx)
    val ay = abs(gy)
    val az = abs(gz)
    return when {
        az >= ax && az >= ay -> Pose.FLAT
        ay >= ax -> Pose.PORTRAIT
        else -> Pose.LANDSCAPE
    }
}

/** Judged on the value rounded to the 0.1° the screen shows, so "+0.2°" and the verdict always agree. */
fun isLevel(deg: Float, tolerance: Float) = abs((deg * 10).roundToInt()) <= (tolerance * 10).roundToInt()

fun formatDeg(deg: Float): String {
    val tenths = (deg * 10).roundToInt()
    if (tenths == 0) return "0.0°"
    val sign = if (tenths > 0) "+" else "−"
    return sign + String.format(Locale.ROOT, "%.1f", abs(tenths) / 10f) + "°"
}

/** Rise over a one-metre run, the unit builders read off a level. */
fun formatMmPerM(deg: Float): String =
    String.format(Locale.ROOT, "%.1f mm/m", tan(Math.toRadians(abs(deg).toDouble())) * 1000)

/** Fires once each time [update] sees `on`, re-arming only after `off`; the gap stops buzzing at the edge of tolerance. */
class EdgeTrigger {
    private var armed = true

    fun update(on: Boolean, off: Boolean): Boolean {
        if (off) armed = true
        if (on && armed) {
            armed = false
            return true
        }
        return false
    }
}
