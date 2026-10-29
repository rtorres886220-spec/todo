package com.example.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.components.AnchorCard
import com.example.ui.theme.AnchorCyan
import com.example.ui.theme.AnchorTextPrimary
import com.example.ui.theme.AnchorTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (upcomingTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🗓️", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No upcoming scheduled tasks",
                            color = AnchorTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add dates to tasks like 'Dentist next Friday at 2pm'",
                            color = AnchorTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            // Tomorrow Section
            if (tomorrowTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "TOMORROW (${tomorrowTasks.size})",
                        color = AnchorCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 10.dp)
                    )
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

            // Later This Week / Month
            if (laterTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "LATER & UPCOMING (${laterTasks.size})",
                        color = AnchorTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 12.dp)
                    )
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

            // Other scheduled
            if (otherTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "OTHER SCHEDULED (${otherTasks.size})",
                        color = AnchorTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 12.dp)
                    )
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
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
