package com.bubblelevel.app.level

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class LevelMathTest {

    private val g = 9.81f

    private fun rad(deg: Double) = Math.toRadians(deg)

    @Test
    fun flatPhoneHasNoTilt() {
        assertEquals(Tilt(0f, 0f), tiltOf(0f, 0f, g))
        assertEquals(Pose.FLAT, poseOf(0f, 0f, g))
    }

    @Test
    fun raisingTheRightEdgeGivesPositiveX() {
        val t = tiltOf((g * sin(rad(10.0))).toFloat(), 0f, (g * cos(rad(10.0))).toFloat())
        assertEquals(10f, t.x, 0.01f)
        assertEquals(0f, t.y, 0.01f)
    }

    @Test
    fun loweringTheTopEdgeGivesNegativeY() {
        val t = tiltOf(0f, (-g * sin(rad(3.0))).toFloat(), (g * cos(rad(3.0))).toFloat())
        assertEquals(-3f, t.y, 0.01f)
    }

    @Test
    fun uprightPhoneReadsItsRollOnX() {
        val t = tiltOf((g * sin(rad(1.5))).toFloat(), (g * cos(rad(1.5))).toFloat(), 0f)
        assertEquals(1.5f, t.x, 0.01f)
        assertEquals(Pose.PORTRAIT, poseOf(0.2f, g, 0.1f))
        assertEquals(Pose.LANDSCAPE, poseOf(-g, 0.2f, 0.1f))
    }

    @Test
    fun poseDecidesWhichAxesCount() {
        assertTrue(Pose.FLAT.measuresX() && Pose.FLAT.measuresY())
        assertTrue(Pose.PORTRAIT.measuresX() && !Pose.PORTRAIT.measuresY())
        assertTrue(!Pose.LANDSCAPE.measuresX() && Pose.LANDSCAPE.measuresY())
    }

    @Test
    fun formatsDegreesWithSignAndTenths() {
        assertEquals("0.0°", formatDeg(0.04f))
        assertEquals("0.0°", formatDeg(-0.04f))
        assertEquals("+1.8°", formatDeg(1.8f))
        assertEquals("−0.6°", formatDeg(-0.6f))
        assertEquals("+12.3°", formatDeg(12.3f))
    }

    @Test
    fun formatsMillimetresPerMetre() {
        assertEquals("17.5 mm/m", formatMmPerM(1f))
        assertEquals("17.5 mm/m", formatMmPerM(-1f))
        assertEquals("0.0 mm/m", formatMmPerM(0f))
    }

    @Test
    fun levelVerdictMatchesTheShownValue() {
        assertTrue(isLevel(0.24f, 0.2f))
        assertTrue(isLevel(-0.2f, 0.2f))
        assertFalse(isLevel(0.26f, 0.2f))
    }

    @Test
    fun edgeTriggerFiresOncePerEntry() {
        val trigger = EdgeTrigger()
        assertTrue(trigger.update(on = true, off = false))
        assertFalse(trigger.update(on = true, off = false))
        // Wobbling just outside tolerance, but not far enough to re-arm.
        assertFalse(trigger.update(on = false, off = false))
        assertFalse(trigger.update(on = true, off = false))
        // Clearly off level, then back.
        assertFalse(trigger.update(on = false, off = true))
        assertTrue(trigger.update(on = true, off = false))
    }
}
