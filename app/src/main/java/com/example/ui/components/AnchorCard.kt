package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.theme.AccentGold
import com.example.ui.theme.GeistMono
import com.example.ui.theme.HairlineColor
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.ShadowAmbient
import com.example.ui.theme.Surface1
import com.example.ui.theme.SwipeWashDone
import com.example.ui.theme.SwipeWashSnooze
import com.example.ui.theme.SwipeWashToday
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.UrgencyCoral
import com.example.ui.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorCard(
    item: AnchorItem,
    isOverdue: Boolean = false,
    onMoveToToday: () -> Unit,
    onSnooze: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSnoozeCoach: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCollapsing by remember { mutableStateOf(false) }
    val isDone = item.status == AnchorItem.STATUS_DONE

    // Completion ring animated progress (0f -> 1f)
    val completeAnim = remember { Animatable(if (isDone) 1f else 0f) }

    fun triggerComplete() {
        if (isDone) {
            onComplete()
            return
        }
        scope.launch {
            HapticHelper.vibrate8ms(context)
            completeAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            delay(100)
            isCollapsing = true
            delay(200)
            onComplete()
        }
    }

    val isTodayTask = item.status == AnchorItem.STATUS_TODAY

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    HapticHelper.vibrate8ms(context)
                    if (isTodayTask) {
                        triggerComplete()
                    } else {
                        onMoveToToday()
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    HapticHelper.vibrate8ms(context)
                    onSnooze()
                    false
                }
                else -> false
            }
        }
    )

    var hasHapticFiredForSwipe by remember { mutableStateOf(false) }
    LaunchedEffect(dismissState.progress) {
        if (dismissState.progress > 0.35f && !hasHapticFiredForSwipe) {
            HapticHelper.vibrate8ms(context)
            hasHapticFiredForSwipe = true
        } else if (dismissState.progress <= 0.35f) {
            hasHapticFiredForSwipe = false
        }
    }

    AnimatedVisibility(
        visible = !isCollapsing,
        exit = fadeOut(tween(180)) + shrinkVertically(tween(200)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val progress = dismissState.progress.coerceIn(0f, 1f)

                val washColor = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> if (isTodayTask) SwipeWashDone else SwipeWashToday
                    SwipeToDismissBoxValue.EndToStart -> SwipeWashSnooze
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(washColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    val iconScale = (progress * 1.2f).coerceIn(0.5f, 1f)
                    Canvas(
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer(scaleX = iconScale, scaleY = iconScale, alpha = progress)
                    ) {
                        val stroke = 1.75.dp.toPx()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                            val tint = if (isTodayTask) Color(0xFF15803D) else AccentGold
                            drawCircle(color = tint, radius = size.minDimension / 2f - 2f, style = Stroke(width = stroke))
                            drawLine(color = tint, start = Offset(center.x - 4.dp.toPx(), center.y), end = Offset(center.x - 1.dp.toPx(), center.y + 3.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
                            drawLine(color = tint, start = Offset(center.x - 1.dp.toPx(), center.y + 3.dp.toPx()), end = Offset(center.x + 4.dp.toPx(), center.y - 3.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
                        } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                            val tint = TextSecondary
                            drawLine(color = tint, start = Offset(size.width * 0.4f, size.height * 0.3f), end = Offset(size.width * 0.65f, size.height * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
                            drawLine(color = tint, start = Offset(size.width * 0.65f, size.height * 0.5f), end = Offset(size.width * 0.4f, size.height * 0.7f), strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            // High-contrast clean card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (isDone) alpha = 0.55f
                    }
                    .drawBehind {
                        // Soft modern shadow
                        drawRoundRect(
                            color = ShadowAmbient,
                            topLeft = Offset(0f, 2.dp.toPx()),
                            size = size,
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface1)
                    .border(
                        width = 1.dp,
                        color = if (isOverdue) UrgencyCoral.copy(alpha = 0.5f) else HairlineColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onEdit() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("anchor_item_${item.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox: 48dp minimum touch target
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { triggerComplete() }
                        .testTag("complete_button_${item.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        val strokeWidth = 1.75.dp.toPx()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f - 2f

                        val activeColor = if (isOverdue) UrgencyCoral else AccentGold

                        if (completeAnim.value > 0f) {
                            drawCircle(
                                color = activeColor,
                                radius = radius * completeAnim.value,
                                center = center
                            )
                            if (completeAnim.value > 0.6f) {
                                val checkAlpha = ((completeAnim.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                                drawLine(
                                    color = Color.White.copy(alpha = checkAlpha),
                                    start = Offset(center.x - 4.dp.toPx(), center.y),
                                    end = Offset(center.x - 1.dp.toPx(), center.y + 3.5.dp.toPx()),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = checkAlpha),
                                    start = Offset(center.x - 1.dp.toPx(), center.y + 3.5.dp.toPx()),
                                    end = Offset(center.x + 4.5.dp.toPx(), center.y - 3.5.dp.toPx()),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        } else {
                            drawCircle(
                                color = if (isOverdue) UrgencyCoral else HairlineColor.copy(alpha = 0.9f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title & meta details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = when {
                            isDone -> TextTertiary
                            isOverdue -> UrgencyCoral
                            else -> TextPrimary
                        },
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.snoozeCount >= 3 && !isDone) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Postponed ${item.snoozeCount}x • Tap for focus breakdown",
                            fontFamily = GeistMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = UrgencyCoral,
                            modifier = Modifier.clickable { onSnoozeCoach() }
                        )
                    } else if (item.tagList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row {
                            item.tagList.take(3).forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        fontFamily = GeistMono,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side: Due time / duration badge
                val timeText = buildString {
                    if (isOverdue) append("OVERDUE ")
                    if (!item.dueTime.isNullOrBlank()) append(item.dueTime)
                    else if (!item.dueDate.isNullOrBlank()) append(item.dueDate)
                    if (item.estimatedMinutes != null) {
                        if (isNotEmpty()) append(" • ")
                        append("${item.estimatedMinutes}m")
                    }
                }.trim()

                if (timeText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isOverdue) UrgencyCoral.copy(alpha = 0.1f) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isOverdue) UrgencyCoral.copy(alpha = 0.3f) else HairlineColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = timeText,
                            fontFamily = GeistMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isOverdue) UrgencyCoral else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
