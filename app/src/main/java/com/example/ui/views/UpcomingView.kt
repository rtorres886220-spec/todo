package com.example.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.components.AnchorCard
import com.example.ui.components.QuietEmptyState
import com.example.ui.components.QuietHairline
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.QuietAccent
import com.example.ui.theme.QuietTextSecondary
import com.example.ui.theme.QuietTextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun UpcomingView(
    upcomingTasks: List<AnchorItem>,
    onMoveToToday: (AnchorItem) -> Unit,
    onSnooze: (AnchorItem) -> Unit,
    onComplete: (AnchorItem) -> Unit,
    onDelete: (AnchorItem) -> Unit,
    onEdit: (AnchorItem) -> Unit,
    onSnoozeCoach: (AnchorItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = dateFormat.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, 1)
    val tomorrowStr = dateFormat.format(cal.time)

    val tomorrowTasks = upcomingTasks.filter { it.dueDate == tomorrowStr }
    val laterTasks = upcomingTasks.filter { it.dueDate != null && it.dueDate > tomorrowStr }
    val otherTasks = upcomingTasks.filter { it.dueDate == null || it.dueDate <= todayStr }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (upcomingTasks.isEmpty()) {
            item {
                QuietEmptyState(message = "Nothing upcoming")
            }
        } else {
            // Tomorrow Section
            if (tomorrowTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOMORROW",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = QuietAccent,
                            letterSpacing = 0.14.em
                        )
                        Text(
                            text = "${tomorrowTasks.size}",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = QuietTextTertiary,
                            letterSpacing = 0.14.em
                        )
                    }
                    QuietHairline(modifier = Modifier.padding(horizontal = 24.dp))
                }
                items(tomorrowTasks, key = { "tomorrow_${it.id}" }) { item ->
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
            }

            // Later Section
            if (laterTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LATER TIMELINES",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = QuietTextSecondary,
                            letterSpacing = 0.14.em
                        )
                        Text(
                            text = "${laterTasks.size}",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = QuietTextTertiary,
                            letterSpacing = 0.14.em
                        )
                    }
                    QuietHairline(modifier = Modifier.padding(horizontal = 24.dp))
                }
                items(laterTasks, key = { "later_${it.id}" }) { item ->
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
            }

            // Other Scheduled
            if (otherTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OTHER SCHEDULED",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = QuietTextSecondary,
                            letterSpacing = 0.14.em
                        )
                        Text(
                            text = "${otherTasks.size}",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = QuietTextTertiary,
                            letterSpacing = 0.14.em
                        )
                    }
                    QuietHairline(modifier = Modifier.padding(horizontal = 24.dp))
                }
                items(otherTasks, key = { "other_${it.id}" }) { item ->
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
            }
        }

        item {
            Spacer(modifier = Modifier.height(170.dp))
        }
    }
}
