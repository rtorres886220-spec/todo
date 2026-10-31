package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.GeistMono
import com.example.ui.theme.HairlineColor
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.UrgencyCoral
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .drawBehind {
                // Top border stroke
                drawLine(
                    color = HairlineColor,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(Surface1)
            .height(68.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                AnchorTab.INBOX to "Inbox",
                AnchorTab.TODAY to "Today",
                AnchorTab.UPCOMING to "Upcoming",
                AnchorTab.NOTES to "Notes",
                AnchorTab.DONE to "Done"
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
                        .clip(RoundedCornerShape(12.dp))
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
                        // Icon + Badge
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(22.dp)) {
                                val stroke = if (isActive) 2.dp.toPx() else 1.5.dp.toPx()
                                val iconColor = if (isActive) AccentGold else TextSecondary

                                when (tab) {
                                    AnchorTab.INBOX -> drawInboxNavIcon(iconColor, stroke)
                                    AnchorTab.TODAY -> drawTodayNavIcon(iconColor, stroke)
                                    AnchorTab.UPCOMING -> drawUpcomingNavIcon(iconColor, stroke)
                                    AnchorTab.NOTES -> drawNotesNavIcon(iconColor, stroke)
                                    AnchorTab.DONE -> drawDoneNavIcon(iconColor, stroke)
                                }
                            }

                            // Notification dot
                            if (tab == AnchorTab.TODAY && hasOverdue) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(UrgencyCoral)
                                )
                            } else if (tab == AnchorTab.INBOX && inboxCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(AccentGold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = label,
                            fontFamily = PlusJakartaSans,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isActive) AccentGold else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawInboxNavIcon(color: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.15f, h * 0.25f)
        lineTo(w * 0.85f, h * 0.25f)
        lineTo(w * 0.85f, h * 0.78f)
        lineTo(w * 0.65f, h * 0.78f)
        lineTo(w * 0.58f, h * 0.9f)
        lineTo(w * 0.42f, h * 0.9f)
        lineTo(w * 0.35f, h * 0.78f)
        lineTo(w * 0.15f, h * 0.78f)
        close()
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawTodayNavIcon(color: Color, strokeWidth: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f - 2f
    drawCircle(color = color, radius = r, center = center, style = Stroke(width = strokeWidth))
    drawLine(color = color, start = center, end = Offset(center.x, center.y - r * 0.55f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = center, end = Offset(center.x + r * 0.45f, center.y), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawUpcomingNavIcon(color: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height
    // Calendar outline
    val path = Path().apply {
        moveTo(w * 0.15f, h * 0.3f)
        lineTo(w * 0.85f, h * 0.3f)
        lineTo(w * 0.85f, h * 0.85f)
        lineTo(w * 0.15f, h * 0.85f)
        close()
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Calendar hooks
    drawLine(color = color, start = Offset(w * 0.35f, h * 0.18f), end = Offset(w * 0.35f, h * 0.32f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(w * 0.65f, h * 0.18f), end = Offset(w * 0.65f, h * 0.32f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawNotesNavIcon(color: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.2f, h * 0.18f)
        lineTo(w * 0.8f, h * 0.18f)
        lineTo(w * 0.8f, h * 0.85f)
        lineTo(w * 0.2f, h * 0.85f)
        close()
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(color = color, start = Offset(w * 0.35f, h * 0.4f), end = Offset(w * 0.65f, h * 0.4f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(w * 0.35f, h * 0.58f), end = Offset(w * 0.6f, h * 0.58f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawDoneNavIcon(color: Color, strokeWidth: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f - 2f
    drawCircle(color = color, radius = r, center = center, style = Stroke(width = strokeWidth))
    drawLine(color = color, start = Offset(center.x - 4.5.dp.toPx(), center.y), end = Offset(center.x - 1.dp.toPx(), center.y + 3.5.dp.toPx()), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(center.x - 1.dp.toPx(), center.y + 3.5.dp.toPx()), end = Offset(center.x + 4.5.dp.toPx(), center.y - 3.5.dp.toPx()), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}
