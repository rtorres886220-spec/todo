package com.example.ui.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.components.AnchorCard
import com.example.ui.components.BentoGridCard
import com.example.ui.components.NotchedTabMergeShape
import com.example.ui.components.QuietEmptyState
import com.example.ui.components.QuietHairline
import com.example.ui.components.getCategoryTint
import com.example.ui.components.softDimensionalDepth
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGoldGlow
import com.example.ui.theme.GeistMono
import com.example.ui.theme.HairlineColor
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.UrgencyCoral
import com.example.ui.util.HapticHelper
import com.example.ui.util.MotionUtil
import kotlinx.coroutines.launch
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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isReducedMotion = remember { MotionUtil.isReducedMotion(context) }

    // Selected Day Offset (null = Today, 1 = Tomorrow, 2 = Day 2, etc.)
    var selectedDayOffset by remember { mutableStateOf<Int?>(null) }
    // Filter pill selection: Day / Week / Month
    var selectedFilter by remember { mutableStateOf("Day") }

    val sdfIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayIso = remember { sdfIso.format(Date()) }

    // Date formatting for Hero card: Left: weekday & date in big light type
    val headerInfo = remember(selectedDayOffset) {
        val cal = Calendar.getInstance()
        if (selectedDayOffset != null && selectedDayOffset != 0) {
            cal.add(Calendar.DAY_OF_YEAR, selectedDayOffset!!)
            val d = cal.time
            val m = SimpleDateFormat("MMMM", Locale.getDefault()).format(d)
            val num = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(d)
            val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(d)
            DateHeaderInfo(d, m, num, dayLabel)
        } else {
            val d = cal.time
            val m = SimpleDateFormat("MMMM", Locale.getDefault()).format(d)
            val num = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(d)
            DateHeaderInfo(d, m, num, "Today")
        }
    }
    val activeMonthName = headerInfo.monthName
    val activeDayNumberStr = headerInfo.dayNumberStr
    val activeTabLabel = headerInfo.tabLabel


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

    // Hero due time formatting: e.g. "2:22" + "PM" or "11:30" + "AM"
    val (dueTimeNumeral, amPm) = remember(heroTask?.dueTime) {
        val dt = heroTask?.dueTime
        if (dt.isNullOrBlank()) {
            val cal = Calendar.getInstance()
            val h = cal.get(Calendar.HOUR)
            val hour = if (h == 0) 12 else h
            val min = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MINUTE))
            val ampm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            "$hour:$min" to ampm
        } else {
            try {
                val parts = dt.split(":")
                var hour = parts[0].toInt()
                val min = parts.getOrNull(1) ?: "00"
                val ampm = if (hour >= 12) "PM" else "AM"
                if (hour > 12) hour -= 12
                if (hour == 0) hour = 12
                "$hour:$min" to ampm
            } catch (_: Exception) {
                dt to ""
            }
        }
    }

    // Hero card tilt angles (up to 4° with touch or motion)
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    val springSpec = spring<Float>(dampingRatio = 0.85f, stiffness = 300f)

    // Total planned minutes and remaining count
    val totalPlannedMinutes = remember(displayedTasks) {
        displayedTasks.sumOf { it.estimatedMinutes ?: 25 }
    }


    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Overdue alerts if any
        if (overdueTasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(color = UrgencyCoral)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OVERDUE",
                            fontFamily = GeistMono,
                            fontWeight = FontWeight.Light,
                            fontSize = 11.sp,
                            color = UrgencyCoral,
                            letterSpacing = 0.14.em
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${overdueTasks.size}",
                            fontFamily = GeistMono,
                            fontWeight = FontWeight.Light,
                            fontSize = 11.sp,
                            color = UrgencyCoral,
                            letterSpacing = 0.14.em
                        )
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

        // Horizontal strip of circular day chips + NOTCHED TAB MERGE with Hero Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                // Circular day chips row to the right of the Today tab
                val calendar = remember { Calendar.getInstance() }
                // Circular day chips row to the right of the Today tab
                val dayChips = remember {
                    val cal = Calendar.getInstance()
                    val list = mutableListOf<Pair<Int, String>>()
                    for (i in 1..4) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        list.add(i to "${cal.get(Calendar.DAY_OF_MONTH)}")
                    }
                    list
                }

                // The unified Notched Tab & Hero Card container:
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Day chips row aligned horizontally at top right of the Today tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dayChips.forEach { (offset, dayNum) ->
                            val isChipSelected = selectedDayOffset == offset
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 5.dp)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isChipSelected) Surface2 else Surface2.copy(alpha = 0.6f))
                                    .clickable {
                                        HapticHelper.vibrate8ms(context)
                                        selectedFilter = "Day"
                                        selectedDayOffset = if (selectedDayOffset == offset) null else offset
                                    }
                                    .drawBehind {
                                        if (isChipSelected) {
                                            drawCircle(
                                                color = AccentGold,
                                                radius = size.minDimension / 2f - 0.75.dp.toPx(),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                            )
                                        } else {
                                            drawLine(
                                                color = SurfaceHighlight,
                                                start = Offset(0f, 0.5f),
                                                end = Offset(size.width, 0.5f),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = if (isChipSelected) FontWeight.Normal else FontWeight.Light,
                                    fontSize = 14.sp,
                                    color = if (isChipSelected) AccentGold else TextSecondary
                                )
                            }
                        }
                    }


                    // Continuous Notched Tab + Hero Card Surface
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                if (!isReducedMotion) {
                                    rotationX = tiltX.value
                                    rotationY = tiltY.value
                                    cameraDistance = 16f * density.density
                                }
                            }
                            .pointerInput(isReducedMotion) {
                                if (isReducedMotion) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val normX = ((offset.x / size.width) - 0.5f) * 2f
                                        val normY = ((offset.y / size.height) - 0.5f) * 2f
                                        scope.launch { tiltY.animateTo(normX * 4f, springSpec) }
                                        scope.launch { tiltX.animateTo(-normY * 4f, springSpec) }
                                    },
                                    onDrag = { change, _ ->
                                        val normX = ((change.position.x / size.width) - 0.5f).coerceIn(-1f, 1f) * 2f
                                        val normY = ((change.position.y / size.height) - 0.5f).coerceIn(-1f, 1f) * 2f
                                        scope.launch { tiltY.animateTo(normX * 4f, springSpec) }
                                        scope.launch { tiltX.animateTo(-normY * 4f, springSpec) }
                                    },
                                    onDragEnd = {
                                        scope.launch { tiltX.animateTo(0f, springSpec) }
                                        scope.launch { tiltY.animateTo(0f, springSpec) }
                                    },
                                    onDragCancel = {
                                        scope.launch { tiltX.animateTo(0f, springSpec) }
                                        scope.launch { tiltY.animateTo(0f, springSpec) }
                                    }
                                )
                            }
                            // Faint gold glow bleeds from behind the hero card
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            AccentGoldGlow,
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.45f, size.height * 0.5f),
                                        radius = size.width * 0.75f
                                    )
                                )
                            }
                            .clip(NotchedTabMergeShape(tabWidth = 100.dp, tabHeight = 44.dp, cardRadius = 28.dp, tabRadius = 22.dp, inverseRadius = 20.dp))
                            .background(Surface1)
                            .drawBehind {
                                // 1px inset top highlight (rgba(255,255,255,0.07))
                                drawLine(
                                    color = SurfaceHighlight,
                                    start = Offset(22.dp.toPx(), 0.5f),
                                    end = Offset(100.dp.toPx() - 22.dp.toPx(), 0.5f),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = SurfaceHighlight,
                                    start = Offset(120.dp.toPx(), 44.dp.toPx() + 0.5f),
                                    end = Offset(size.width - 28.dp.toPx(), 44.dp.toPx() + 0.5f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable {
                                if (heroTask != null) {
                                    HapticHelper.vibrate8ms(context)
                                    onTaskClick(heroTask)
                                }
                            }
                            .testTag("today_hero_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            // Tab label "Today" inside the integrated top tab
                            Box(
                                modifier = Modifier
                                    .width(76.dp)
                                    .height(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeTabLabel,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = if (selectedDayOffset != null) AccentGold else TextPrimary,
                                    modifier = Modifier.clickable {
                                        if (selectedDayOffset != null) {
                                            HapticHelper.vibrate8ms(context)
                                            selectedDayOffset = null
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Hero Content: Left weekday & date | hairline divider | Right due time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Month & Date
                                Column(modifier = Modifier.weight(1.1f)) {
                                    Text(
                                        text = activeMonthName,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 24.sp,
                                        lineHeight = 28.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = activeDayNumberStr,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 18.sp,
                                        color = TextSecondary
                                    )
                                }


                                // Thin vertical hairline divider
                                Box(
                                    modifier = Modifier
                                        .height(56.dp)
                                        .width(1.dp)
                                        .background(HairlineColor)
                                )

                                // Right: next task's due time in 56px light numerals with small mono AM/PM
                                Row(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .padding(start = 16.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = dueTimeNumeral,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 54.sp,
                                        lineHeight = 56.sp,
                                        color = TextPrimary,
                                        letterSpacing = (-0.02).em
                                    )
                                    if (amPm.isNotEmpty()) {
                                        Text(
                                            text = " $amPm",
                                            fontFamily = GeistMono,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            letterSpacing = 0.14.em,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Bottom row: secondary info in muted text
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val remainingCount = displayedTasks.size
                                val subText = if (heroTask != null) {
                                    "${heroTask.title.take(30)} • $totalPlannedMinutes min planned"
                                } else {
                                    "$remainingCount tasks • $totalPlannedMinutes min planned"
                                }
                                Text(
                                    text = subText,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )

                                Text(
                                    text = "DO THIS NEXT",
                                    fontFamily = GeistMono,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 11.sp,
                                    color = AccentGold,
                                    letterSpacing = 0.14.em
                                )
                            }
                        }
                    }
                }
            }
        }

        // Up next header & Pill Filter Row (Day / Week / Month)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 18.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "Up next",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pill filter row (Day / Week / Month) with selected pill in Surface 2 plus gold text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Day", "Week", "Month").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(if (isSelected) Surface2 else Color.Transparent)
                                .clickable {
                                    HapticHelper.vibrate8ms(context)
                                    selectedFilter = filter
                                    if (filter != "Day") {
                                        selectedDayOffset = null
                                    }
                                }

                                .drawBehind {
                                    if (isSelected) {
                                        drawLine(
                                            color = SurfaceHighlight,
                                            start = Offset(0f, 0.5f),
                                            end = Offset(size.width, 0.5f),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                fontFamily = PlusJakartaSans,
                                fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Light,
                                fontSize = 14.sp,
                                color = if (isSelected) AccentGold else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Brief & Review quick access triggers in calm text
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BRIEF",
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            color = AccentGold,
                            letterSpacing = 0.14.em,
                            modifier = Modifier
                                .clickable { onOpenMorningBrief() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("morning_brief_button")
                        )
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "REVIEW",
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            letterSpacing = 0.14.em,
                            modifier = Modifier
                                .clickable { onOpenEveningReview() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("evening_review_button")
                        )
                    }
                }
            }
        }

        // Two-column bento grid of tinted category cards of varying heights
        if (bentoTasks.isEmpty()) {
            item {
                QuietEmptyState(message = "Nothing planned for today")
            }
        } else {
            // Group into 2-item rows for the two-column bento grid
            val pairs = bentoTasks.chunked(2)
            pairs.forEachIndexed { rowIndex, pair ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val leftItem = pair[0]
                        val leftTint = getCategoryTint(leftItem)
                        val leftHeight = if (rowIndex % 2 == 0) 170.dp else 140.dp

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
                            val rightHeight = if (rowIndex % 2 == 0) 140.dp else 170.dp

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
            Spacer(modifier = Modifier.height(180.dp))
        }
    }
}

private data class DateHeaderInfo(
    val date: Date,
    val monthName: String,
    val dayNumberStr: String,
    val tabLabel: String
)

