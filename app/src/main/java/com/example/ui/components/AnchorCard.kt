package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.theme.AnchorAmber
import com.example.ui.theme.AnchorCyan
import com.example.ui.theme.AnchorCyanDim
import com.example.ui.theme.AnchorEmerald
import com.example.ui.theme.AnchorEmeraldDim
import com.example.ui.theme.AnchorIndigo
import com.example.ui.theme.AnchorOutline
import com.example.ui.theme.AnchorRose
import com.example.ui.theme.AnchorSurface
import com.example.ui.theme.AnchorSurfaceHighlight
import com.example.ui.theme.AnchorSurfaceVariant
import com.example.ui.theme.AnchorTextPrimary
import com.example.ui.theme.AnchorTextSecondary
import com.example.ui.theme.AnchorTextTertiary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    // Swipe gestures:
    // Right swipe (StartToEnd) -> Move to Today
    // Left swipe (EndToStart) -> Snooze
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onMoveToToday()
                    false // Return false so item stays rendered or handles via state update
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSnooze()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> AnchorCyanDim.copy(alpha = 0.5f)
                    SwipeToDismissBoxValue.EndToStart -> AnchorIndigo.copy(alpha = 0.5f)
                    else -> Color.Transparent
                },
                label = "swipeColor"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Today, contentDescription = "Move to Today", tint = AnchorCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Move to Today", color = AnchorCyan, fontWeight = FontWeight.Bold)
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Snooze", color = AnchorIndigo, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Snooze, contentDescription = "Snooze", tint = AnchorIndigo)
                    }
                }
            }
        },
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        val isDone = item.status == AnchorItem.STATUS_DONE
        val cardBorderColor = when {
            isOverdue -> AnchorRose.copy(alpha = 0.8f)
            item.priority == AnchorItem.PRIORITY_HIGH -> AnchorAmber.copy(alpha = 0.6f)
            item.status == AnchorItem.STATUS_TODAY -> AnchorCyan.copy(alpha = 0.5f)
            else -> AnchorOutline.copy(alpha = 0.4f)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOverdue) AnchorSurface.copy(alpha = 0.95f) else AnchorSurfaceVariant.copy(alpha = 0.85f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isOverdue || item.status == AnchorItem.STATUS_TODAY) 1.5.dp else 1.dp,
                    color = cardBorderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onEdit() }
                .testTag("anchor_item_${item.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Snoozed 3+ times prompt
                if (item.snoozeCount >= 3 && !isDone) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AnchorAmber.copy(alpha = 0.15f))
                            .clickable { onSnoozeCoach() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Snoozed 3x",
                            tint = AnchorAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Snoozed ${item.snoozeCount}x — Tap for Gemini micro-step breakdown",
                            color = AnchorAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Overdue warning banner
                if (isOverdue) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ OVERDUE",
                            color = AnchorRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Due: ${item.dueDate} ${item.dueTime ?: ""}".trim(),
                            color = AnchorRose.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Main Row: Checkbox, Title, and Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox / Complete Button (Up swipe or tap)
                    IconButton(
                        onClick = onComplete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("complete_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isDone) "Completed" else "Complete task",
                            tint = if (isDone) AnchorEmerald else AnchorTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = if (isDone) AnchorTextTertiary else AnchorTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                        )

                        if (item.type == AnchorItem.TYPE_NOTE) {
                            Text(
                                text = "Note / Thought",
                                color = AnchorIndigo,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Action buttons
                    if (!isDone) {
                        // Move to Today quick button
                        if (item.status != AnchorItem.STATUS_TODAY) {
                            IconButton(
                                onClick = onMoveToToday,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = "Move to Today",
                                    tint = AnchorCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Snooze quick button
                        IconButton(
                            onClick = onSnooze,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = "Snooze",
                                tint = AnchorTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = AnchorTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Metadata Chips: AI-sorted chip, Due Date, Duration, Tags
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // AI-sorted chip
                    if (item.aiSorted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AnchorCyan.copy(alpha = 0.15f))
                                .border(0.5.dp, AnchorCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .clickable { onEdit() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Sorted",
                                    tint = AnchorCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "AI-sorted",
                                    color = AnchorCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Due date chip
                    if (!item.dueDate.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AnchorSurfaceHighlight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Due",
                                    tint = AnchorTextSecondary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${item.dueDate}${if (!item.dueTime.isNullOrBlank()) " ${item.dueTime}" else ""}",
                                    color = AnchorTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Estimated minutes
                    if (item.estimatedMinutes != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AnchorSurfaceHighlight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⏱️ ${item.estimatedMinutes}m",
                                color = AnchorTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Priority tag
                    if (item.priority == AnchorItem.PRIORITY_HIGH) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AnchorAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "High Priority",
                                color = AnchorAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // User tags
                    item.tagList.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AnchorSurfaceHighlight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = AnchorTextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
