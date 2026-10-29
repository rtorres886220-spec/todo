package com.example.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.components.AnchorCard
import com.example.ui.theme.AnchorAmber
import com.example.ui.theme.AnchorCyan
import com.example.ui.theme.AnchorCyanGlow
import com.example.ui.theme.AnchorEmerald
import com.example.ui.theme.AnchorIndigo
import com.example.ui.theme.AnchorOutline
import com.example.ui.theme.AnchorSurface
import com.example.ui.theme.AnchorSurfaceHighlight
import com.example.ui.theme.AnchorSurfaceVariant
import com.example.ui.theme.AnchorTextPrimary
import com.example.ui.theme.AnchorTextSecondary
import com.example.ui.theme.AnchorTextTertiary

@Composable
fun TodayView(
    todayTasks: List<AnchorItem>,
    overdueTasks: List<AnchorItem>,
    onMoveToToday: (AnchorItem) -> Unit,
    onSnooze: (AnchorItem) -> Unit,
    onComplete: (AnchorItem) -> Unit,
    onDelete: (AnchorItem) -> Unit,
    onEdit: (AnchorItem) -> Unit,
    onSnoozeCoach: (AnchorItem) -> Unit,
    onOpenMorningBrief: () -> Unit,
    onOpenEveningReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val max5Today = todayTasks.take(5)
    val doThisNextTask = max5Today.firstOrNull()
    val remainingToday = if (max5Today.size > 1) max5Today.drop(1) else emptyList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Action Row: Morning Brief & Evening Review buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Morning Brief Button
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AnchorSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenMorningBrief() }
                        .testTag("morning_brief_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Morning Brief",
                            tint = AnchorAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Morning Brief", color = AnchorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Gemini Top 3", color = AnchorCyan, fontSize = 11.sp)
                        }
                    }
                }

                // Evening Review Button
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AnchorSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenEveningReview() }
                        .testTag("evening_review_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Evening Review",
                            tint = AnchorIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Evening Review", color = AnchorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Daily Closeout", color = AnchorIndigo, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Overdue Tasks pinned to top
        if (overdueTasks.isNotEmpty()) {
            item {
                Text(
                    text = "🚨 OVERDUE ANCHORS (PINNED)",
                    color = AnchorAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(overdueTasks, key = { "overdue_${it.id}" }) { item ->
                AnchorCard(
                    item = item,
                    isOverdue = true,
                    onMoveToToday = { onMoveToToday(item) },
                    onSnooze = { onSnooze(item) },
                    onComplete = { onComplete(item) },
                    onDelete = { onDelete(item) },
                    onEdit = { onEdit(item) },
                    onSnoozeCoach = { onSnoozeCoach(item) }
                )
            }
        }

        // "DO THIS NEXT" Hero Card
        if (doThisNextTask != null) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AnchorSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(listOf(AnchorCyan, AnchorIndigo)),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .testTag("do_this_next_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(AnchorCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DO THIS NEXT",
                                    color = AnchorCyanGlow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (doThisNextTask.estimatedMinutes != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AnchorSurfaceHighlight)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "⏱️ ${doThisNextTask.estimatedMinutes} min sprint",
                                        color = AnchorCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = doThisNextTask.title,
                            color = AnchorTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Finish Anchor Button
                        Button(
                            onClick = { onComplete(doThisNextTask) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("do_this_next_complete_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AnchorCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AnchorSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Complete Focus Anchor",
                                color = AnchorSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Remaining Today items (max 5)
        if (remainingToday.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S REMAINING FOCUS (${remainingToday.size}/4)",
                        color = AnchorTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Max 5 items rule",
                        color = AnchorTextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
            items(remainingToday, key = { it.id }) { item ->
                AnchorCard(
                    item = item,
                    onMoveToToday = { onMoveToToday(item) },
                    onSnooze = { onSnooze(item) },
                    onComplete = { onComplete(item) },
                    onDelete = { onDelete(item) },
                    onEdit = { onEdit(item) },
                    onSnoozeCoach = { onSnoozeCoach(item) }
                )
            }
        } else if (doThisNextTask == null && overdueTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⚓",
                            fontSize = 42.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No anchors on your Today deck",
                            color = AnchorTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Capture a task below or open Morning Brief to plan.",
                            color = AnchorTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Extra padding at bottom for the capture bar
        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
