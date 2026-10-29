package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Manrope
import com.example.ui.theme.QuietAccent
import com.example.ui.theme.QuietAccentDim
import com.example.ui.theme.QuietAccentGlow
import com.example.ui.theme.QuietBackgroundLift
import com.example.ui.theme.QuietHairline
import com.example.ui.theme.QuietHighlight
import com.example.ui.theme.QuietTextPrimary
import com.example.ui.theme.QuietTextSecondary
import com.example.ui.theme.QuietTextTertiary
import com.example.ui.util.HapticHelper
import com.example.ui.util.MotionUtil
import kotlinx.coroutines.launch

/**
 * 0.5px subtle hairline divider (rgba(255,255,255,0.06))
 */
@Composable
fun QuietHairline(
    modifier: Modifier = Modifier,
    color: Color = QuietHairline,
    thickness: Dp = 0.5.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .drawBehind {
                drawRect(color = color)
            }
    )
}

/**
 * Empty state:
 * - Ring increased to 96px with a brighter 1px stroke
 * - Identical vertical position on every tab
 * - Consistent, calm copy:
 *   "Nothing in your inbox," "Nothing planned for today," "Nothing upcoming," "No notes yet," "Nothing completed yet"
 */
@Composable
fun QuietEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_rotate")
    val angle = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 110.dp, bottom = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.size(96.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 6f
                val strokeWidth = 1.dp.toPx()

                // Brighter 1px static base ring
                drawCircle(
                    color = Color(0x33FFFFFF),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Slowly rotating brighter arc of light
                val sweepGradient = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        QuietAccent.copy(alpha = 0.25f),
                        QuietAccent.copy(alpha = 0.95f),
                        Color.Transparent
                    ),
                    center = center
                )

                drawArc(
                    brush = sweepGradient,
                    startAngle = angle.value,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth + 0.25f, cap = StrokeCap.Round)
                )

                // Precise center instrument indicator
                drawCircle(
                    color = QuietTextSecondary.copy(alpha = 0.6f),
                    radius = 2.dp.toPx(),
                    center = center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = message,
                color = QuietTextSecondary,
                fontFamily = Manrope,
                fontSize = 15.sp,
                letterSpacing = 0.sp
            )
        }
    }
}

/**
 * Modifier for elevated surfaces:
 * Stacked shadows, 1px inset top highlight (rgba(255,255,255,0.10)), light source top-left.
 */
fun Modifier.quietDepth(
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    backgroundColor: Color = QuietBackgroundLift,
    faintAccentGlow: Boolean = false
): Modifier = this.drawBehind {
    val highlightColor = QuietHighlight

    // Base background
    drawRect(color = backgroundColor)

    if (faintAccentGlow) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(QuietAccentGlow, Color.Transparent),
                center = Offset(size.width * 0.2f, 0f),
                radius = size.width * 0.6f
            )
        )
    }

    // 1px top highlight (rgba(255,255,255,0.10))
    drawLine(
        color = highlightColor,
        start = Offset(0f, 0.5f),
        end = Offset(size.width, 0.5f),
        strokeWidth = 1.dp.toPx()
    )

    // Left subtle highlight continuation
    drawLine(
        color = highlightColor.copy(alpha = 0.05f),
        start = Offset(0.5f, 0f),
        end = Offset(0.5f, size.height * 0.4f),
        strokeWidth = 1.dp.toPx()
    )
}

/**
 * The Today "Do this next" glass slab:
 * - 3D CSS perspective that tilts up to 6° following touch position
 * - Glow shifting opposite the tilt
 * - 1px inset top highlight rgba(255,255,255,0.10)
 * - Single accent #7FE3FF
 * - No font weight above 400
 * - No emojis
 */
@Composable
fun QuietDoThisNextCard(
    item: AnchorItem,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isReducedMotion = remember { MotionUtil.isReducedMotion(context) }

    // Tilt angles in degrees
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }

    val springSpec = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 300f
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (!isReducedMotion) {
                    rotationX = tiltX.value
                    rotationY = tiltY.value
                    cameraDistance = 16f * density.density
                }
            }
            .pointerInput(isReducedMotion) {
                if (isReducedMotion) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        val normX = ((offset.x / size.width) - 0.5f) * 2f
                        val normY = ((offset.y / size.height) - 0.5f) * 2f
                        scope.launch { tiltY.animateTo(normX * 6f, springSpec) }
                        scope.launch { tiltX.animateTo(-normY * 6f, springSpec) }
                    },
                    onDrag = { change, _ ->
                        val normX = ((change.position.x / size.width) - 0.5f).coerceIn(-1f, 1f) * 2f
                        val normY = ((change.position.y / size.height) - 0.5f).coerceIn(-1f, 1f) * 2f
                        scope.launch { tiltY.animateTo(normX * 6f, springSpec) }
                        scope.launch { tiltX.animateTo(-normY * 6f, springSpec) }
                    },
                    onDragEnd = {
                        scope.launch { tiltX.animateTo(0f, springSpec) }
                        scope.launch { tiltY.animateTo(0f, springSpec) }
                    },
                    onDragCancel = {
                        scope.launch { tiltX.animateTo(0f, springSpec) }
                        scope.launch { tiltY.animateTo(0f, springSpec) }
                    }
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                // Background glass slab
                drawRect(color = Color(0xFF0F121A))

                // Glow shifting opposite the tilt
                val glowCenterX = size.width * (0.5f - (tiltY.value / 12f))
                val glowCenterY = size.height * (0.4f + (tiltX.value / 12f))

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            QuietAccent.copy(alpha = 0.18f),
                            QuietAccent.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(glowCenterX, glowCenterY),
                        radius = size.width * 0.75f
                    )
                )

                // 1px top highlight (rgba(255,255,255,0.10))
                drawLine(
                    color = QuietHighlight,
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    strokeWidth = 1.dp.toPx()
                )

                // Left faint hairline
                drawLine(
                    color = QuietHairline,
                    start = Offset(0.5f, 0f),
                    end = Offset(0.5f, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .clickable { onEdit() }
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("do_this_next_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active cyan pip
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = QuietAccent)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DO THIS NEXT",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = QuietAccent,
                    letterSpacing = 0.14.em
                )

                Spacer(modifier = Modifier.weight(1f))

                val metadataStr = buildString {
                    if (!item.dueTime.isNullOrBlank()) {
                        append(item.dueTime)
                    }
                    if (item.estimatedMinutes != null) {
                        if (isNotEmpty()) append("  ")
                        append("${item.estimatedMinutes}M SPRINT")
                    }
                }.trim()

                if (metadataStr.isNotEmpty()) {
                    Text(
                        text = metadataStr.uppercase(),
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = item.title,
                fontFamily = Manrope,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                color = QuietTextPrimary,
                letterSpacing = (-0.01).sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action row: thin-stroke completion trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticHelper.vibrate8ms(context)
                        onComplete()
                    }
                    .testTag("do_this_next_complete_button")
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thin ring checkbox indicator
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawCircle(
                        color = QuietAccent,
                        radius = size.minDimension / 2f - 2f,
                        style = Stroke(width = 1.25.dp.toPx())
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "COMPLETE ANCHOR",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = QuietAccent,
                    letterSpacing = 0.14.em
                )
            }
        }
    }
}
