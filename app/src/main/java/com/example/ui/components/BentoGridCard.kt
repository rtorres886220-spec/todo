package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.theme.HairlineColor
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.ShadowAmbient
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.HapticHelper
import kotlinx.coroutines.delay

@Composable
fun BentoGridCard(
    item: AnchorItem,
    tint: CategoryTint,
    minHeight: Dp = 150.dp,
    staggerIndex: Int = 0,
    onCardClick: () -> Unit,
    onReminderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val alphaAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(20f) }

    LaunchedEffect(item.id) {
        delay((staggerIndex * 30L).coerceAtMost(250L))
        val springSpec = spring<Float>(stiffness = 350f, dampingRatio = 0.85f)
        alphaAnim.animateTo(1f, springSpec)
    }

    LaunchedEffect(item.id) {
        delay((staggerIndex * 30L).coerceAtMost(250L))
        val springSpec = spring<Float>(stiffness = 350f, dampingRatio = 0.85f)
        offsetYAnim.animateTo(0f, springSpec)
    }

    val bigDisplayStr = remember(item.dueDate, item.dueTime, item.createdAt) {
        when {
            !item.dueTime.isNullOrBlank() -> item.dueTime
            !item.dueDate.isNullOrBlank() -> {
                try {
                    val sdfIn = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val d = sdfIn.parse(item.dueDate)
                    if (d != null) {
                        java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(d)
                    } else item.dueDate
                } catch (_: Exception) {
                    item.dueDate
                }
            }
            else -> {
                val d = java.util.Date(item.createdAt)
                java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(d)
            }
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(0, offsetYAnim.value.dp.roundToPx()) }
            .graphicsLayer { alpha = alphaAnim.value }
            .fillMaxWidth()
            .height(minHeight)
            .drawBehind {
                drawRoundRect(
                    color = ShadowAmbient,
                    topLeft = Offset(0f, 2.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(18.dp))
            .drawBehind {
                drawRect(brush = tint.gradientBrush)
            }
            .border(
                width = 1.dp,
                color = HairlineColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable {
                HapticHelper.vibrate8ms(context)
                onCardClick()
            }
            .padding(16.dp)
            .testTag("bento_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.matchParentSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawCategoryIcon(tint.iconType, color = tint.foreground, strokeWidth = 1.75.dp.toPx())
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            HapticHelper.vibrate8ms(context)
                            onReminderClick()
                        }
                        .drawBehind {
                            drawCircle(color = tint.foreground.copy(alpha = 0.12f))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        val stroke = 1.5.dp.toPx()
                        val w = size.width
                        val h = size.height
                        val bellPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.5f, h * 0.2f)
                            cubicTo(w * 0.35f, h * 0.2f, w * 0.28f, h * 0.45f, w * 0.22f, h * 0.65f)
                            lineTo(w * 0.78f, h * 0.65f)
                            cubicTo(w * 0.72f, h * 0.45f, w * 0.65f, h * 0.2f, w * 0.5f, h * 0.2f)
                            close()
                        }
                        drawPath(bellPath, color = tint.foreground, style = Stroke(width = stroke, cap = StrokeCap.Round))
                        drawArc(
                            color = tint.foreground,
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(w * 0.42f, h * 0.65f),
                            size = Size(w * 0.16f, h * 0.15f),
                            style = Stroke(width = stroke)
                        )
                    }
                }
            }

            // Bottom section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = bigDisplayStr,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    color = tint.foreground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
