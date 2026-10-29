package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.QuietAccent
import com.example.ui.theme.QuietGlassFill
import com.example.ui.theme.QuietHighlight
import com.example.ui.theme.QuietTextSecondary
import com.example.ui.theme.QuietTextTertiary
import com.example.ui.theme.QuietUrgency
import com.example.ui.util.HapticHelper
import com.example.viewmodel.AnchorTab

@Composable
fun QuietNavigationBar(
    activeTab: AnchorTab,
    onTabSelected: (AnchorTab) -> Unit,
    inboxCount: Int,
    todayCount: Int,
    hasOverdue: Boolean,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 100.dp,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "nav_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, navOffset.roundToPx()) }
            // Lifted glass surface floating 12px above bottom edge:
            // rgba(255,255,255,0.04) fill, 1px top highlight rgba(255,255,255,0.10), long soft shadow
            .drawBehind {
                // Long soft shadow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33000000),
                            Color(0x66000000)
                        ),
                        startY = -20.dp.toPx(),
                        endY = 0f
                    ),
                    topLeft = Offset(0f, -20.dp.toPx()),
                    size = Size(size.width, 20.dp.toPx())
                )

                // Glass base
                drawRect(color = Color(0xF207080B))
                // Glass fill
                drawRect(color = QuietGlassFill)

                // 1px top highlight (rgba(255,255,255,0.10))
                drawLine(
                    color = QuietHighlight,
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .navigationBarsPadding()
            .padding(bottom = 12.dp) // floating 12px above bottom edge
            .height(58.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                AnchorTab.INBOX to "INBOX",
                AnchorTab.TODAY to "TODAY",
                AnchorTab.UPCOMING to "UPCOMING",
                AnchorTab.NOTES to "NOTES",
                AnchorTab.DONE to "DONE"
            )

            tabs.forEach { (tab, label) ->
                val isActive = activeTab == tab
                val testTagStr = when (tab) {
                    AnchorTab.INBOX -> "tab_inbox"
                    AnchorTab.TODAY -> "tab_today"
                    AnchorTab.UPCOMING -> "tab_upcoming"
                    AnchorTab.NOTES -> "tab_notes"
                    AnchorTab.DONE -> "tab_done"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            HapticHelper.vibrate8ms(context)
                            onTabSelected(tab)
                        }
                        .testTag(testTagStr),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 24px icon: Inactive at 55% opacity, active in QuietAccent
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val stroke = 1.25.dp.toPx()
                                val iconColor = if (isActive) {
                                    QuietAccent
                                } else {
                                    QuietTextSecondary.copy(alpha = 0.55f)
                                }
                                drawQuietTabIcon(tab, iconColor, stroke)
                            }

                            // Notification dot if needed
                            if (tab == AnchorTab.INBOX && inboxCount > 0 && !isActive) {
                                Canvas(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    drawCircle(color = QuietAccent)
                                }
                            } else if (tab == AnchorTab.TODAY && hasOverdue && !isActive) {
                                Canvas(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    drawCircle(color = QuietUrgency)
                                }
                            }
                        }

                        // Only active tab shows label (11px) and small glowing dot
                        if (isActive) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Canvas(modifier = Modifier.size(3.dp)) {
                                    drawCircle(color = QuietAccent)
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = label,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = QuietAccent,
                                    letterSpacing = 0.14.em
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawQuietTabIcon(tab: AnchorTab, color: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height

    when (tab) {
        AnchorTab.INBOX -> {
            // Thin-stroke inbox tray
            val path = Path().apply {
                moveTo(w * 0.15f, h * 0.25f)
                lineTo(w * 0.85f, h * 0.25f)
                lineTo(w * 0.85f, h * 0.75f)
                lineTo(w * 0.65f, h * 0.75f)
                lineTo(w * 0.58f, h * 0.88f)
                lineTo(w * 0.42f, h * 0.88f)
                lineTo(w * 0.35f, h * 0.75f)
                lineTo(w * 0.15f, h * 0.75f)
                close()
            }
            drawPath(path, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
        }
        AnchorTab.TODAY -> {
            // Thin-stroke clock dial with hands
            val center = Offset(w / 2f, h / 2f)
            val radius = size.minDimension / 2f - 2f
            drawCircle(color = color, radius = radius, center = center, style = Stroke(width = strokeWidth))
            drawLine(
                color = color,
                start = center,
                end = Offset(center.x, center.y - radius * 0.55f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = center,
                end = Offset(center.x + radius * 0.45f, center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        AnchorTab.UPCOMING -> {
            // Thin-stroke calendar
            val left = w * 0.18f
            val top = h * 0.22f
            val right = w * 0.82f
            val bottom = h * 0.82f
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = strokeWidth)
            )
            // Header bar line
            drawLine(
                color = color,
                start = Offset(left, top + (bottom - top) * 0.3f),
                end = Offset(right, top + (bottom - top) * 0.3f),
                strokeWidth = strokeWidth
            )
            // Binder pins
            drawLine(
                color = color,
                start = Offset(left + 3.dp.toPx(), top - 2.dp.toPx()),
                end = Offset(left + 3.dp.toPx(), top + 2.dp.toPx()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(right - 3.dp.toPx(), top - 2.dp.toPx()),
                end = Offset(right - 3.dp.toPx(), top + 2.dp.toPx()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        AnchorTab.NOTES -> {
            // Thin-stroke memo lines
            val left = w * 0.22f
            val right = w * 0.78f
            drawLine(
                color = color,
                start = Offset(left, h * 0.3f),
                end = Offset(right, h * 0.3f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(left, h * 0.5f),
                end = Offset(right * 0.85f, h * 0.5f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(left, h * 0.7f),
                end = Offset(right * 0.65f, h * 0.7f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        AnchorTab.DONE -> {
            // Thin-stroke check circle
            val center = Offset(w / 2f, h / 2f)
            val radius = size.minDimension / 2f - 2f
            drawCircle(color = color, radius = radius, center = center, style = Stroke(width = strokeWidth))
            drawLine(
                color = color,
                start = Offset(center.x - 4.dp.toPx(), center.y),
                end = Offset(center.x - 1.dp.toPx(), center.y + 2.5f.dp.toPx()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(center.x - 1.dp.toPx(), center.y + 2.5f.dp.toPx()),
                end = Offset(center.x + 4.dp.toPx(), center.y - 2.5f.dp.toPx()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
