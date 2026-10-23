package com.bubblelevel.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bubblelevel.app.level.EdgeTrigger
import com.bubblelevel.app.level.GravitySensor
import com.bubblelevel.app.level.LevelSettings
import com.bubblelevel.app.level.Pose
import com.bubblelevel.app.level.Tilt
import com.bubblelevel.app.level.formatDeg
import com.bubblelevel.app.level.formatMmPerM
import com.bubblelevel.app.level.isLevel
import com.bubblelevel.app.level.measuresX
import com.bubblelevel.app.level.measuresY
import com.bubblelevel.app.level.poseOf
import com.bubblelevel.app.level.tiltOf
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val TOLERANCE = 0.2f

/** How far past tolerance a reading must go before the next return to level buzzes again. */
private const val REARM_MARGIN = 0.1f

private class Reading(val tilt: Tilt, val pose: Pose)

@Composable
fun LevelScreen(sensor: GravitySensor, settings: LevelSettings, onLevel: () -> Unit) {
    var offset by remember { mutableStateOf(settings.offset) }
    var alertOn by remember { mutableStateOf(settings.alertOn) }
    var held by remember { mutableStateOf<Reading?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val trigger = remember { EdgeTrigger() }

    val g = sensor.gravity
    val raw = g?.let { tiltOf(it[0], it[1], it[2]) }
    val live = g?.let { Reading(raw!! - offset, poseOf(it[0], it[1], it[2])) }
    val shown = held ?: live

    val x = shown?.tilt?.x ?: 0f
    val y = shown?.tilt?.y ?: 0f
    val xOn = shown?.pose?.measuresX() == true
    val yOn = shown?.pose?.measuresY() == true
    val xLevel = xOn && isLevel(x, TOLERANCE)
    val yLevel = yOn && isLevel(y, TOLERANCE)
    val level = shown != null && (!xOn || xLevel) && (!yOn || yLevel)
    val farOff = (xOn && abs(x) > TOLERANCE + REARM_MARGIN) || (yOn && abs(y) > TOLERANCE + REARM_MARGIN)

    SideEffect {
        if (shown != null && trigger.update(level, farOff) && alertOn) onLevel()
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2200)
            toast = null
        }
    }

    val calibrate = {
        if (raw != null && live != null) {
            // Only the axes this pose measures; the other one points up and would wreck its offset.
            offset = Tilt(
                if (live.pose.measuresX()) raw.x else offset.x,
                if (live.pose.measuresY()) raw.y else offset.y,
            )
            settings.offset = offset
            held = null
            toast = "현재 자세를 0.0°로 맞췄습니다"
        }
    }
    val resetCalibration = {
        offset = Tilt(0f, 0f)
        settings.offset = offset
        toast = "보정값을 초기화했습니다"
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Palette.Ink)
                .safeDrawingPadding(),
        ) {
            if (!sensor.available) {
                Text(
                    "이 기기에는 기울기 센서가 없습니다",
                    Modifier.align(Alignment.Center),
                    color = Palette.Text,
                    fontSize = 16.sp,
                )
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Header(alertOn) {
                        alertOn = !alertOn
                        settings.alertOn = alertOn
                    }
                    TubeVial(x, xLevel, horizontal = true, modifier = Modifier.fillMaxWidth().height(64.dp))
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val vialWidth = 64.dp
                        val gap = 24.dp
                        val rowGap = 14.dp
                        val circle = maxWidth - vialWidth - gap
                        // Intrinsic height: the vertical vial spans the circle plus the readout, however tall large fonts make it.
                        Row(Modifier.height(IntrinsicSize.Min)) {
                            Column(Modifier.width(circle), verticalArrangement = Arrangement.spacedBy(rowGap)) {
                                Box {
                                    // Upright, the bubble is pinned to the rim, so the bull's-eye never reads level.
                                    CircleVial(x, y, level && shown?.pose == Pose.FLAT, Modifier.size(circle))
                                    if (held != null) HoldBadge(Modifier.align(Alignment.TopCenter).padding(top = 18.dp))
                                }
                                Readout(
                                    x = if (shown != null && xOn) x else null,
                                    y = if (shown != null && yOn) y else null,
                                    xLevel = xLevel,
                                    yLevel = yLevel,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 86.dp),
                                )
                            }
                            Spacer(Modifier.width(gap))
                            TubeVial(
                                y,
                                yLevel,
                                horizontal = false,
                                modifier = Modifier.width(vialWidth).fillMaxHeight(),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 36.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            when {
                                shown == null -> Chip("센서 대기 중", null, Color.Transparent, Palette.Muted)
                                level -> Chip("수평", LevelIcons.Check, Palette.Accent, Palette.Ink)
                                else -> Chip(tiltedLabel(xOn && !xLevel, yOn && !yLevel), LevelIcons.Warning, Color.Transparent, Palette.Warn)
                            }
                            Text("허용 오차 ±${TOLERANCE}°", color = Palette.Muted, fontSize = 13.sp)
                        }
                        if (offset != Tilt(0f, 0f)) CalibrationRow(offset, resetCalibration)
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionButton(
                            "영점 보정",
                            LevelIcons.Target,
                            onClick = calibrate,
                            container = Palette.Surface,
                            content = Palette.Text,
                            border = BorderStroke(1.5.dp, Palette.ButtonLine),
                            modifier = Modifier.weight(1f),
                        )
                        ActionButton(
                            if (held != null) "홀드 해제" else "홀드",
                            if (held != null) LevelIcons.Play else LevelIcons.Pause,
                            onClick = { held = if (held != null) null else live },
                            container = if (held != null) Palette.Accent else Palette.Text,
                            content = Palette.Ink,
                            border = null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                toast?.let {
                    Text(
                        it,
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 20.dp, end = 20.dp, bottom = 108.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Palette.Text)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        color = Palette.Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun tiltedLabel(x: Boolean, y: Boolean) =
    listOfNotNull("수평".takeIf { x }, "수직".takeIf { y }).joinToString("·") + " 기울어짐"

@Composable
private fun Header(alertOn: Boolean, onToggleAlert: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("수평계", color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("수평 · 수직 · 원형", color = Palette.Muted, fontSize = 13.sp)
        }
        Surface(
            onClick = onToggleAlert,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = Palette.Surface,
            border = BorderStroke(1.5.dp, if (alertOn) Palette.Accent else Palette.ButtonLine),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (alertOn) LevelIcons.Bell else LevelIcons.BellOff,
                    contentDescription = if (alertOn) "수평 진동 알림 끄기" else "수평 진동 알림 켜기",
                    modifier = Modifier.size(22.dp),
                    tint = if (alertOn) Palette.Accent else Palette.Muted,
                )
            }
        }
    }
}

@Composable
private fun Readout(x: Float?, y: Float?, xLevel: Boolean, yLevel: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AxisValue("X · 수평", x, xLevel, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 2.dp).background(Palette.Line))
        Spacer(Modifier.width(14.dp))
        AxisValue("Y · 수직", y, yLevel, Modifier.weight(1f))
    }
}

/** [deg] is null when the current pose does not measure this axis. */
@Composable
private fun AxisValue(label: String, deg: Float?, level: Boolean, modifier: Modifier) {
    // Sized in dp, not sp: already large, and at a 1.3 font scale "−12.3°" would overflow its half of the readout.
    val valueSize = with(LocalDensity.current) { 28.dp.toSp() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = Palette.Muted, fontSize = 12.sp)
        Text(
            if (deg != null) formatDeg(deg) else "—",
            color = when {
                deg == null -> Palette.Muted
                level -> Palette.Accent
                else -> Palette.Text
            },
            fontSize = valueSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-1).sp,
            maxLines = 1,
        )
        Text(
            if (deg != null) formatMmPerM(deg) else " ",
            color = Palette.Muted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun Chip(label: String, icon: ImageVector?, fill: Color, content: Color) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(fill)
            .then(if (fill == Color.Transparent) Modifier.border(1.5.dp, content, shape) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = content)
        Text(label, color = content, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalibrationRow(offset: Tilt, onReset: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, shape)
            .padding(start = 14.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "보정 적용 · X ${formatDeg(offset.x)}  Y ${formatDeg(offset.y)}",
            color = Palette.Muted,
            fontSize = 13.sp,
        )
        Surface(onClick = onReset, color = Color.Transparent, shape = RoundedCornerShape(10.dp)) {
            Text(
                "초기화",
                Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                color = Palette.Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HoldBadge(modifier: Modifier) {
    Text(
        "HOLD",
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.Text)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = Palette.Ink,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    border: BorderStroke?,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content,
        border = border,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}
