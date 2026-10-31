package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HairlineColor
import com.example.ui.theme.ShadowAmbient
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2

/**
 * Clean architectural card styling: clean background, soft elevation, and crisp slate border.
 */
fun Modifier.softDimensionalDepth(
    cornerRadius: Dp = 20.dp,
    backgroundColor: Color = Surface1,
    faintTintGlow: Color? = null,
    insetHighlight: Boolean = true
): Modifier = this.drawBehind {
    val r = cornerRadius.toPx()
    val w = size.width
    val h = size.height

    // Soft modern drop shadow
    drawRoundRect(
        color = ShadowAmbient,
        topLeft = Offset(0f, 3.dp.toPx()),
        size = Size(w, h),
        cornerRadius = CornerRadius(r, r)
    )

    // Base background surface
    drawRoundRect(
        color = backgroundColor,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(r, r)
    )

    // Crisp high-contrast 1.dp slate border
    drawRoundRect(
        color = HairlineColor,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(r, r),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )
}

/**
 * Clean sunken container with crisp border
 */
fun Modifier.sculpted3DSunkenWell(
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = Surface2
): Modifier = this.drawBehind {
    val r = cornerRadius.toPx()

    // Base fill
    drawRoundRect(
        color = backgroundColor,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(r, r)
    )

    // Subtle crisp border
    drawRoundRect(
        color = HairlineColor,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(r, r),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )
}

/**
 * Clean top-rounded sheet shape
 */
class NotchedSheetWithCradleShape(
    val topRadius: Dp = 24.dp,
    val buttonCenterOffsetRight: Dp = 56.dp,
    val cradleRadius: Dp = 38.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { topRadius.toPx() }
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    topLeft = CornerRadius(r, r),
                    topRight = CornerRadius(r, r),
                    bottomLeft = CornerRadius.Zero,
                    bottomRight = CornerRadius.Zero
                )
            )
        }
        return Outline.Generic(path)
    }
}

/**
 * Clean unified card shape
 */
class NotchedTabMergeShape(
    val tabWidth: Dp = 100.dp,
    val tabHeight: Dp = 44.dp,
    val cardRadius: Dp = 20.dp,
    val tabRadius: Dp = 20.dp,
    val inverseRadius: Dp = 16.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { cardRadius.toPx() }
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            )
        }
        return Outline.Generic(path)
    }
}

/**
 * Clean surface without noisy film grain
 */
fun Modifier.subtleFilmGrainOverlay(): Modifier = this
