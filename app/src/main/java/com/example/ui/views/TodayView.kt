package com.example.ui.views

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.components.AnchorCard
import com.example.ui.components.QuietDoThisNextCard
import com.example.ui.components.QuietEmptyState
import com.example.ui.components.QuietHairline
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.QuietAccent
import com.example.ui.theme.QuietHairline
import com.example.ui.theme.QuietTextPrimary
import com.example.ui.theme.QuietTextSecondary
import com.example.ui.theme.QuietTextTertiary
import com.example.ui.theme.QuietUrgency

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
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Quick Actions: Morning Brief & Evening Review (Quiet text triggers, no pills, no emojis)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Morning brief trigger
                Row(
                    modifier = Modifier
                        .clickable { onOpenMorningBrief() }
                        .testTag("morning_brief_button")
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BRIEF",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TOP 3",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietAccent,
                        letterSpacing = 0.14.em
                    )
                }

                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = QuietHairline
                )

                // Evening review trigger
                Row(
                    modifier = Modifier
                        .clickable { onOpenEveningReview() }
                        .testTag("evening_review_button")
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REVIEW",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CLOSEOUT",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextTertiary,
                        letterSpacing = 0.14.em
                    )
                }
            }
            QuietHairline(modifier = Modifier.padding(horizontal = 24.dp))
        }

        // Pinned Overdue Tasks
        if (overdueTasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OVERDUE",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietUrgency,
                        letterSpacing = 0.14.em
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${overdueTasks.size}",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietUrgency,
                        letterSpacing = 0.14.em
                    )
                }
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

        // "DO THIS NEXT" Card: 3D perspective glass slab tilting up to 6° following touch position
        if (doThisNextTask != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    QuietDoThisNextCard(
                        item = doThisNextTask,
                        onComplete = { onComplete(doThisNextTask) },
                        onEdit = { onEdit(doThisNextTask) }
                    )
                }
            }
        }

        // Remaining Today items
        if (remainingToday.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REMAINING FOCUS",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em
                    )
                    Text(
                        text = "${remainingToday.size} / 4",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextTertiary,
                        letterSpacing = 0.14.em
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
                QuietEmptyState(message = "Nothing planned for today")
            }
        }

        item {
            Spacer(modifier = Modifier.height(170.dp))
        }
    }
}
