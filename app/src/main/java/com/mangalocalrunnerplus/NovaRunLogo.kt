
package com.mangalocalrunnerplus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NovaRunMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height

        fun x(value: Float) = w * value / 100f
        fun y(value: Float) = h * value / 100f

        // Main cyan part of the N
        val cyanPath = Path().apply {
            moveTo(x(12f), y(12f))
            lineTo(x(48f), y(12f))
            lineTo(x(87f), y(40f))
            lineTo(x(87f), y(59f))
            lineTo(x(49f), y(33f))
            lineTo(x(49f), y(68f))
            lineTo(x(12f), y(43f))
            close()
        }

        // Lime lower-right part
        val limePath = Path().apply {
            moveTo(x(12f), y(50f))
            lineTo(x(35f), y(66f))
            lineTo(x(53f), y(79f))
            lineTo(x(70f), y(68f))
            lineTo(x(87f), y(57f))
            lineTo(x(87f), y(81f))
            lineTo(x(70f), y(94f))
            lineTo(x(12f), y(62f))
            close()
        }

        drawPath(
            path = cyanPath,
            color = NovaRunPalette.Cyan
        )

        drawPath(
            path = limePath,
            color = NovaRunPalette.Lime
        )

        // Terminal prompt symbol
        val terminalPath = Path().apply {
            moveTo(x(39f), y(43f))
            lineTo(x(49f), y(52f))
            lineTo(x(39f), y(61f))
        }

        drawPath(
            path = terminalPath,
            color = NovaRunPalette.Cyan,
            style = Stroke(width = w * 0.075f)
        )

        drawLine(
            color = NovaRunPalette.Cyan,
            start = Offset(x(53f), y(63f)),
            end = Offset(x(65f), y(63f)),
            strokeWidth = w * 0.075f
        )

        // Speed lines above the mark
        drawLine(
            color = NovaRunPalette.Cyan,
            start = Offset(x(67f), y(10f)),
            end = Offset(x(88f), y(1f)),
            strokeWidth = w * 0.065f
        )

        drawLine(
            color = NovaRunPalette.Cyan,
            start = Offset(x(67f), y(19f)),
            end = Offset(x(88f), y(10f)),
            strokeWidth = w * 0.065f
        )
    }
}

@Composable
fun NovaRunBrandLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 44.dp,
    showTagline: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NovaRunMark(size = markSize)

        Spacer(Modifier.width(10.dp))

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nova",
                    color = NovaRunPalette.Cyan,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Run",
                    color = NovaRunPalette.Lime,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (showTagline) {
                Text(
                    text = "LOCAL PROJECT RUNNER",
                    color = NovaRunPalette.TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}
