package com.example.ui.views

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.components.AnchorCard
import com.example.ui.components.BentoGridCard
import com.example.ui.components.QuietEmptyState
import com.example.ui.components.getCategoryTint
import com.example.ui.theme.AccentGold
import com.example.ui.theme.GeistMono
import com.example.ui.theme.HairlineColor
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.ShadowAmbient
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.UrgencyCoral
import com.example.ui.util.HapticHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TodayView(
    todayTasks: List<AnchorItem>,
    overdueTasks: List<AnchorItem>,
    upcomingTasks: List<AnchorItem> = emptyList(),
    onMoveToToday: (AnchorItem) -> Unit,
    onSnooze: (AnchorItem) -> Unit,
    onComplete: (AnchorItem) -> Unit,
    onDelete: (AnchorItem) -> Unit,
    onEdit: (AnchorItem) -> Unit,
    onTaskClick: (AnchorItem) -> Unit,
    onSnoozeCoach: (AnchorItem) -> Unit,
    onOpenMorningBrief: () -> Unit,
    onOpenEveningReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Selected Day Offset (null = Today, 1 = Tomorrow, 2 = Day 2, etc.)
    var selectedDayOffset by remember { mutableStateOf<Int?>(null) }
    // Filter pill selection: Day / Week / Month
    var selectedFilter by remember { mutableStateOf("Day") }

    val sdfIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayIso = remember { sdfIso.format(Date()) }

    // Date formatting for Header
    val headerDateStr = remember(selectedDayOffset) {
        val cal = Calendar.getInstance()
        if (selectedDayOffset != null && selectedDayOffset != 0) {
            cal.add(Calendar.DAY_OF_YEAR, selectedDayOffset!!)
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(cal.time)
        } else {
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(cal.time)
        }
    }

    // Filter displayed tasks based on selectedFilter (Day, Week, Month) and selectedDayOffset
    val displayedTasks = remember(todayTasks, upcomingTasks, selectedFilter, selectedDayOffset) {
        when (selectedFilter) {
            "Day" -> {
                if (selectedDayOffset == null || selectedDayOffset == 0) {
                    todayTasks
                } else {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, selectedDayOffset!!)
                    val targetIso = sdfIso.format(cal.time)
                    (todayTasks + upcomingTasks).filter { it.dueDate == targetIso }.distinctBy { it.id }
                }
            }
            "Week" -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 7)
                val weekEndIso = sdfIso.format(cal.time)
                (todayTasks + upcomingTasks).filter { item ->
                    val d = item.dueDate ?: todayIso
                    d >= todayIso && d <= weekEndIso
                }.distinctBy { it.id }.sortedWith(compareBy({ it.dueDate ?: todayIso }, { it.dueTime ?: "23:59" }))
            }
            "Month" -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 30)
                val monthEndIso = sdfIso.format(cal.time)
                (todayTasks + upcomingTasks).filter { item ->
                    val d = item.dueDate ?: todayIso
                    d >= todayIso && d <= monthEndIso
                }.distinctBy { it.id }.sortedWith(compareBy({ it.dueDate ?: todayIso }, { it.dueTime ?: "23:59" }))
            }
            else -> todayTasks
        }
    }

    // Hero "Do this next" task (first priority task or next scheduled task from displayed list)
    val heroTask = displayedTasks.firstOrNull { it.dueTime != null } ?: displayedTasks.firstOrNull()
    val bentoTasks = if (heroTask != null) displayedTasks.filter { it.id != heroTask.id } else displayedTasks

    val totalPlannedMinutes = remember(displayedTasks) {
        displayedTasks.sumOf { it.estimatedMinutes ?: 25 }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 1. Overdue alerts if any
        if (overdueTasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(UrgencyCoral)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OVERDUE",
                            fontFamily = GeistMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = UrgencyCoral
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(UrgencyCoral.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${overdueTasks.size}",
                                fontFamily = GeistMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = UrgencyCoral
                            )
                        }
                    }
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
                    onEdit = { onTaskClick(item) },
                    onSnoozeCoach = { onSnoozeCoach(item) }
                )
            }
        }

        // 2. Clean Header & Day Selector Strip
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Header Date and Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = headerDateStr,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${displayedTasks.size} tasks • $totalPlannedMinutes min planned",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    // Brief & Review quick access buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface2)
                                .clickable { onOpenMorningBrief() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("morning_brief_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BRIEF",
                                fontFamily = GeistMono,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = AccentGold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface2)
                                .clickable { onOpenEveningReview() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("evening_review_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "REVIEW",
                                fontFamily = GeistMono,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Day Selection Strip
                val dayChips = remember {
                    val cal = Calendar.getInstance()
                    val list = mutableListOf<Triple<Int?, String, String>>()
                    // Today
                    list.add(Triple(null, "Today", SimpleDateFormat("d", Locale.getDefault()).format(cal.time)))
                    for (i in 1..4) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
                        val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)
                        list.add(Triple(i, dayName, dayNum))
                    }
                    list
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dayChips.forEach { (offset, label, num) ->
                        val isSelected = selectedDayOffset == offset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentGold else Surface1)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AccentGold else HairlineColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    HapticHelper.vibrate8ms(context)
                                    selectedFilter = "Day"
                                    selectedDayOffset = if (selectedDayOffset == offset && offset != null) null else offset
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = num,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Hero "Current Focus" Card (Clean, modern, no 3D tilts, no scroll hijacking)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                if (heroTask != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRoundRect(
                                    color = ShadowAmbient,
                                    topLeft = Offset(0f, 3.dp.toPx()),
                                    size = size,
                                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                                )
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .background(Surface1)
                            .border(width = 1.dp, color = HairlineColor, shape = RoundedCornerShape(20.dp))
                            .clickable {
                                HapticHelper.vibrate8ms(context)
                                onTaskClick(heroTask)
                            }
                            .padding(20.dp)
                            .testTag("today_hero_card")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Badge & due time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(AccentGold.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "CURRENT FOCUS",
                                        fontFamily = GeistMono,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = AccentGold
                                    )
                                }

                                val dueBadge = heroTask.dueTime ?: heroTask.dueDate ?: "Scheduled"
                                Text(
                                    text = dueBadge,
                                    fontFamily = GeistMono,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Hero task title
                            Text(
                                text = heroTask.title,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                                color = TextPrimary
                            )

                            if (heroTask.tagList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    heroTask.tagList.take(3).forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 6.dp)
                                                .background(Surface2, RoundedCornerShape(4.dp))
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

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons: Complete button & Snooze
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AccentGold)
                                        .clickable {
                                            HapticHelper.vibrate8ms(context)
                                            onComplete(heroTask)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "✓ Mark Done",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Surface2)
                                            .clickable {
                                                HapticHelper.vibrate8ms(context)
                                                onSnooze(heroTask)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Later",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Up Next Section with Scope Filter (Day / Week / Month)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Up next",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )

                    // Filter tabs (Day / Week / Month)
                    Row(
                        modifier = Modifier
                            .background(Surface2, RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("Day", "Week", "Month").forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Surface1 else Color.Transparent)
                                    .clickable {
                                        HapticHelper.vibrate8ms(context)
                                        selectedFilter = filter
                                        if (filter != "Day") {
                                            selectedDayOffset = null
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isSelected) AccentGold else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Tasks Grid or Empty State
        if (bentoTasks.isEmpty()) {
            item {
                QuietEmptyState(message = "All clear! No remaining tasks.")
            }
        } else {
            val pairs = bentoTasks.chunked(2)
            pairs.forEachIndexed { rowIndex, pair ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val leftItem = pair[0]
                        val leftTint = getCategoryTint(leftItem)
                        val leftHeight = if (rowIndex % 2 == 0) 155.dp else 135.dp

                        Box(modifier = Modifier.weight(1f)) {
                            BentoGridCard(
                                item = leftItem,
                                tint = leftTint,
                                minHeight = leftHeight,
                                staggerIndex = rowIndex * 2,
                                onCardClick = { onTaskClick(leftItem) },
                                onReminderClick = { onSnooze(leftItem) }
                            )
                        }

                        if (pair.size > 1) {
                            val rightItem = pair[1]
                            val rightTint = getCategoryTint(rightItem)
                            val rightHeight = if (rowIndex % 2 == 0) 135.dp else 155.dp

                            Box(modifier = Modifier.weight(1f)) {
                                BentoGridCard(
                                    item = rightItem,
                                    tint = rightTint,
                                    minHeight = rightHeight,
                                    staggerIndex = rowIndex * 2 + 1,
                                    onCardClick = { onTaskClick(rightItem) },
                                    onReminderClick = { onSnooze(rightItem) }
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}
