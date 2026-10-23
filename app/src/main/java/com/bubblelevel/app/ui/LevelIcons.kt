package com.bubblelevel.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The mockup's 24-unit line icons; Icon's tint recolours them. */
object LevelIcons {
    private const val BELL = "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"
    private const val CLAPPER = "M10.3 21a1.94 1.94 0 0 0 3.4 0"

    val Bell = stroked(BELL, CLAPPER)
    val BellOff = stroked(BELL, CLAPPER, "M3 3l18 18")
    val Check = stroked("M20 6 9 17l-5-5", width = 3f)
    val Warning = stroked(
        "M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z",
        "M12 9v4",
        "M12 17h.01",
        width = 2.5f,
    )
    val Target = stroked("M19 12a7 7 0 1 1-14 0a7 7 0 1 1 14 0", "M12 2v4M12 18v4M2 12h4M18 12h4")
    val Pause = filled("M6 5h4v14H6zM14 5h4v14h-4z")
    val Play = filled("M7 5v14l12-7z")

    private fun builder() = ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )

    private fun stroked(vararg paths: String, width: Float = 2f) = builder().apply {
        paths.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

    private fun filled(path: String) =
        builder().addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build()
}
