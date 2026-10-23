package com.bubblelevel.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** At this tilt the bubble reaches the end of every vial. */
private const val FULL_SCALE_DEG = 5f

private val shade = arrayOf(
    0f to Color.Black.copy(alpha = 0.45f),
    0.38f to Color.Transparent,
    0.58f to Color.White.copy(alpha = 0.28f),
    1f to Color.Black.copy(alpha = 0.35f),
)

private fun markColor(level: Boolean) = if (level) Palette.Ink else Palette.Ink.copy(alpha = 0.5f)

@Composable
private fun Bezel(level: Boolean, shape: Shape, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .clip(shape)
            .background(Palette.Bezel)
            .border(1.5.dp, if (level) Palette.Accent else Palette.FrameIdle, shape)
            .padding(5.dp),
        content = content,
    )
}

/** A straight vial; the bubble rises to the raised end (right for +x, up for +y). */
@Composable
fun TubeVial(deg: Float, level: Boolean, horizontal: Boolean, modifier: Modifier = Modifier) {
    Bezel(level, RoundedCornerShape(16.dp), modifier) {
        Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp))) {
            drawRect(Palette.Accent)
            drawRect(if (horizontal) Brush.verticalGradient(*shade) else Brush.horizontalGradient(*shade))

            val length = if (horizontal) size.width else size.height
            val bubbleLong = 76.dp.toPx()
            val bubbleShort = 36.dp.toPx()
            val travel = length / 2 - bubbleLong / 2 - 8.dp.toPx()
            val shift = (deg / FULL_SCALE_DEG).coerceIn(-1f, 1f) * travel * (if (horizontal) 1f else -1f)

            val markGap = bubbleLong / 2 + 2.dp.toPx()
            val markWidth = 2.dp.toPx()
            for (side in listOf(-1f, 1f)) {
                val at = side * markGap - markWidth / 2
                if (horizontal) {
                    drawRect(markColor(level), Offset(center.x + at, 0f), Size(markWidth, size.height))
                } else {
                    drawRect(markColor(level), Offset(0f, center.y + at), Size(size.width, markWidth))
                }
            }

            val bubble = if (horizontal) Size(bubbleLong, bubbleShort) else Size(bubbleShort, bubbleLong)
            val middle = if (horizontal) Offset(center.x + shift, center.y) else Offset(center.x, center.y + shift)
            drawBubble(Rect(middle - Offset(bubble.width / 2, bubble.height / 2), bubble))
        }
    }
}

/** The bull's-eye: both axes at once, rings every degree up to full scale. */
@Composable
fun CircleVial(x: Float, y: Float, level: Boolean, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Palette.Ink.copy(alpha = 0.75f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    Bezel(level, CircleShape, modifier) {
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            val r = size.minDimension / 2
            drawCircle(Palette.Accent)
            drawCircle(
                Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.35f),
                    0.55f to Color.Transparent,
                    center = Offset(center.x, size.height * 0.72f),
                    radius = r * 1.1f,
                ),
            )
            drawCircle(Brush.radialGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f), center = center, radius = r))

            val bubbleR = r * 0.21f
            val travel = r - bubbleR - 5.dp.toPx()
            val grid = Palette.Ink.copy(alpha = 0.45f)
            val line = 1.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx()))
            for (deg in 2..5) {
                drawCircle(grid, travel * deg / FULL_SCALE_DEG, style = Stroke(line, pathEffect = if (deg % 2 == 0) dash else null))
            }
            drawLine(grid, Offset(center.x, 0f), Offset(center.x, size.height), line)
            drawLine(grid, Offset(0f, center.y), Offset(size.width, center.y), line)
            for (deg in listOf(3, 5)) {
                val top = center.y - travel * deg / FULL_SCALE_DEG
                drawText(measurer, "$deg°", Offset(center.x + 4.dp.toPx(), top + 2.dp.toPx()), labelStyle)
            }
            drawCircle(markColor(level), bubbleR + 3.dp.toPx(), style = Stroke(2.dp.toPx()))

            var shift = Offset(x, -y) * (travel / FULL_SCALE_DEG)
            if (shift.getDistance() > travel) shift *= travel / shift.getDistance()
            val middle = center + shift
            drawBubble(Rect(middle - Offset(bubbleR, bubbleR), Size(bubbleR * 2, bubbleR * 2)))
        }
    }
}

private fun DrawScope.drawBubble(area: Rect) {
    val corner = CornerRadius(minOf(area.width, area.height) / 2)
    drawRoundRect(
        Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.95f),
            0.45f to Color.White.copy(alpha = 0.45f),
            1f to Color.White.copy(alpha = 0.18f),
            center = Offset(area.left + area.width * 0.35f, area.top + area.height * 0.3f),
            radius = maxOf(area.width, area.height) * 0.8f,
        ),
        area.topLeft,
        area.size,
        corner,
    )
    drawRoundRect(Color.Black.copy(alpha = 0.25f), area.topLeft, area.size, corner, style = Stroke(1.5.dp.toPx()))
}
