package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import com.example.ui.theme.Surface1
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.components.CaptureBar
import com.example.ui.components.EditItemSheet
import com.example.ui.components.EveningReviewDialog
import com.example.ui.components.IntelligenceDialog
import com.example.ui.components.MorningBriefDialog
import com.example.ui.components.NoteDetailSheet
import com.example.ui.components.QuietNavigationBar
import com.example.ui.components.SettingsSheet
import com.example.ui.components.SnoozeCoachDialog
import com.example.ui.components.TaskDetailSheet
import com.example.ui.components.subtleFilmGrainOverlay
import com.example.ui.theme.GeistMono
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SoftDarkBackground
import com.example.ui.theme.SoftDarkRadialLift
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.HapticHelper
import com.example.ui.util.MotionUtil
import com.example.ui.views.DoneView
import com.example.ui.views.InboxView
import com.example.ui.views.NotesView
import com.example.ui.views.TodayView
import com.example.ui.views.UpcomingView
import com.example.viewmodel.AnchorTab
import com.example.viewmodel.AnchorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(viewModel: AnchorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val inboxTasks by viewModel.inboxItems.collectAsState()
    val todayTasks by viewModel.todayItems.collectAsState()
    val upcomingTasks by viewModel.upcomingItems.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val doneTasks by viewModel.doneItems.collectAsState()
    val overdueTasks by viewModel.overdueItems.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val isReducedMotion = remember { MotionUtil.isReducedMotion(context) }

    // Scroll tracking to hide/show navigation on scroll down/up
    var isNavVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) {
                    isNavVisible = false
                } else if (available.y > 12f) {
                    isNavVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // Task Detail Sheet & Note Detail Sheet state
    var selectedDetailItem by remember { mutableStateOf<AnchorItem?>(null) }
    var selectedNoteItem by remember { mutableStateOf<AnchorItem?>(null) }

    // System BackHandler: pop open sheets and dialogs gracefully
    val hasOpenOverlay = selectedDetailItem != null ||
        selectedNoteItem != null ||
        uiState.editingItem != null ||
        uiState.isSettingsOpen ||
        uiState.isMorningBriefOpen ||
        uiState.isEveningReviewOpen ||
        uiState.isSnoozeCoachOpen ||
        uiState.isIntelligenceOpen ||
        uiState.activeTab != AnchorTab.TODAY

    BackHandler(enabled = hasOpenOverlay) {
        when {
            selectedDetailItem != null -> selectedDetailItem = null
            selectedNoteItem != null -> selectedNoteItem = null
            uiState.editingItem != null -> viewModel.dismissEditing()
            uiState.isSettingsOpen -> viewModel.dismissSettings()
            uiState.isMorningBriefOpen -> viewModel.dismissMorningBrief()
            uiState.isEveningReviewOpen -> viewModel.dismissEveningReview()
            uiState.isSnoozeCoachOpen -> viewModel.dismissSnoozeCoach()
            uiState.isIntelligenceOpen -> viewModel.dismissIntelligence()
            uiState.activeTab != AnchorTab.TODAY -> viewModel.setTab(AnchorTab.TODAY)
        }
    }

    // Trigger Snackbar on Undo message
    LaunchedEffect(uiState.undoMessage) {
        val msg = uiState.undoMessage
        if (msg != null) {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                HapticHelper.vibrate8ms(context)
                viewModel.undo()
            }
            viewModel.clearUndo()
        }
    }

    // Weekday & date formatting
    val now = remember { Date() }
    val weekdayStr = remember(now) {
        SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
    }
    val dateMonoStr = remember(now) {
        SimpleDateFormat("MMMM d • yyyy", Locale.getDefault()).format(now).uppercase()
    }

    // Header title: Weekday ONLY on Today; screen name on every other tab in 300-weight type
    val headerTitle = when (uiState.activeTab) {
        AnchorTab.TODAY -> weekdayStr
        AnchorTab.INBOX -> "Inbox"
        AnchorTab.UPCOMING -> "Upcoming"
        AnchorTab.NOTES -> "Notes"
        AnchorTab.DONE -> "Done"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SoftDarkBackground,
        modifier = Modifier.nestedScroll(nestedScrollConnection)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Background: #08080A with a very faint radial lift (#101014) behind hero.
                // Add a subtle film-grain noise overlay at 3% opacity to avoid banding and add richness.
                .drawBehind {
                    drawRect(color = SoftDarkBackground)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SoftDarkRadialLift,
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.32f),
                            radius = size.width * 0.9f
                        )
                    )
                }
                .subtleFilmGrainOverlay()
        ) {
            // Main content with tab cross-fade and slight horizontal parallax
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 110.dp) // Space for floating header
            ) {
                AnimatedContent(
                    targetState = uiState.activeTab,
                    transitionSpec = {
                        val fadeSpring = spring<Float>(stiffness = 300f, dampingRatio = 0.85f)
                        val slideSpring = spring<IntOffset>(stiffness = 300f, dampingRatio = 0.85f)
                        if (isReducedMotion) {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                        } else {
                            (fadeIn(fadeSpring) + slideInHorizontally(
                                animationSpec = slideSpring,
                                initialOffsetX = { fullWidth -> (fullWidth * 0.08f).toInt() }
                            )) togetherWith (fadeOut(fadeSpring) + slideOutHorizontally(
                                animationSpec = slideSpring,
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.08f).toInt() }
                            ))
                        }
                    },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        AnchorTab.INBOX -> {
                            InboxView(
                                inboxItems = inboxTasks,
                                overdueItems = overdueTasks,
                                onMoveToToday = { viewModel.moveToToday(it) },
                                onSnooze = { viewModel.snoozeTask(it) },
                                onComplete = { viewModel.completeTask(it) },
                                onDelete = { viewModel.deleteTask(it) },
                                onEdit = { selectedDetailItem = it },
                                onSnoozeCoach = { viewModel.openSnoozeCoach(it) }
                            )
                        }
                        AnchorTab.TODAY -> {
                            TodayView(
                                todayTasks = todayTasks,
                                upcomingTasks = upcomingTasks,
                                overdueTasks = overdueTasks,
                                onMoveToToday = { viewModel.moveToToday(it) },
                                onSnooze = { viewModel.snoozeTask(it) },
                                onComplete = { viewModel.completeTask(it) },
                                onDelete = { viewModel.deleteTask(it) },
                                onEdit = { viewModel.startEditing(it) },
                                onTaskClick = { selectedDetailItem = it },
                                onSnoozeCoach = { viewModel.openSnoozeCoach(it) },
                                onOpenMorningBrief = { viewModel.openMorningBrief() },
                                onOpenEveningReview = { viewModel.openEveningReview() }
                            )
                        }
                        AnchorTab.UPCOMING -> {
                            UpcomingView(
                                upcomingTasks = upcomingTasks,
                                onMoveToToday = { viewModel.moveToToday(it) },
                                onSnooze = { viewModel.snoozeTask(it) },
                                onComplete = { viewModel.completeTask(it) },
                                onDelete = { viewModel.deleteTask(it) },
                                onEdit = { selectedDetailItem = it },
                                onSnoozeCoach = { viewModel.openSnoozeCoach(it) }
                            )
                        }
                        AnchorTab.NOTES -> {
                            NotesView(
                                notes = notes,
                                onDelete = { viewModel.deleteTask(it) },
                                onEdit = { selectedNoteItem = it }
                            )
                        }
                        AnchorTab.DONE -> {
                            DoneView(
                                doneTasks = doneTasks,
                                onRestore = { viewModel.uncompleteTask(it) },
                                onDelete = { viewModel.deleteTask(it) }
                            )
                        }
                    }
                }
            }

            // Top progressive gradient fade so scrolling content smoothly fades under header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SoftDarkBackground,
                                SoftDarkBackground.copy(alpha = 0.85f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Header: Display numeral/date with 3D tactile telemetry badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .clickable {
                            HapticHelper.vibrate8ms(context)
                            viewModel.openSettings()
                        }
                        .testTag("settings_button")
                ) {
                    Text(
                        text = headerTitle,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Light,
                        fontSize = 40.sp,
                        lineHeight = 44.sp,
                        color = TextPrimary,
                        letterSpacing = (-0.02).em
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateMonoStr,
                        fontFamily = GeistMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        letterSpacing = 0.14.em
                    )
                }

                // 3D Tactile Telemetry Capsule (triggers Intelligence dialog)
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = Color(0x120E182A),
                                topLeft = Offset(0f, 3.dp.toPx()),
                                size = size,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                            )
                        }
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(Surface1)
                        .drawBehind {
                            drawLine(
                                color = Color.White,
                                start = Offset(10.dp.toPx(), 1.dp.toPx()),
                                end = Offset(size.width - 10.dp.toPx(), 1.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .clickable {
                            HapticHelper.vibrate8ms(context)
                            viewModel.askIntelligence("What should I focus on next?")
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("intelligence_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(color = com.example.ui.theme.AccentGold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "3D CHRONO",
                            fontFamily = GeistMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = com.example.ui.theme.AccentGold,
                            letterSpacing = 0.12.em
                        )
                    }
                }
            }

            // Bottom progressive gradient fade so scrolling content fades smoothly above floating bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                SoftDarkBackground.copy(alpha = 0.75f),
                                SoftDarkBackground
                            )
                        )
                    )
            )

            // Floating Bottom Stack: Capture Bar + Lifted Floating Navigation Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // Floating glass capture bar
                CaptureBar(
                    onCapture = { text -> viewModel.capture(text) },
                    clipboardText = uiState.clipboardCandidate,
                    onAcceptClipboard = { viewModel.acceptClipboard() },
                    onDismissClipboard = { viewModel.dismissClipboard() }
                )

                // Lifted glass navigation bar floating 12px above bottom
                QuietNavigationBar(
                    activeTab = uiState.activeTab,
                    onTabSelected = { viewModel.setTab(it) },
                    inboxCount = inboxTasks.size,
                    todayCount = todayTasks.size,
                    hasOverdue = overdueTasks.isNotEmpty(),
                    isVisible = isNavVisible
                )
            }
        }

        // Shared-element / detail expansion sheet
        AnimatedVisibility(
            visible = selectedDetailItem != null,
            enter = fadeIn(tween(220)) + slideInVertically(
                animationSpec = spring(stiffness = 300f, dampingRatio = 0.85f),
                initialOffsetY = { fullHeight -> (fullHeight * 0.15f).toInt() }
            ),
            exit = fadeOut(tween(180)) + slideOutVertically(
                animationSpec = spring(stiffness = 300f, dampingRatio = 0.85f),
                targetOffsetY = { fullHeight -> (fullHeight * 0.15f).toInt() }
            )
        ) {
            selectedDetailItem?.let { item ->
                TaskDetailSheet(
                    item = item,
                    onEdit = {
                        val current = item
                        selectedDetailItem = null
                        viewModel.startEditing(current)
                    },
                    onComplete = {
                        viewModel.completeTask(item)
                        selectedDetailItem = null
                    },
                    onToggleSubtask = { index, completed ->
                        viewModel.toggleSubtask(item, index, completed) { updated ->
                            selectedDetailItem = updated
                        }
                    },
                    onAddSubtask = { text ->
                        viewModel.addSubtask(item, text) { updated ->
                            selectedDetailItem = updated
                        }
                    },
                    onDismiss = { selectedDetailItem = null }
                )
            }
        }

        // Note Detail & Reader Sheet
        if (selectedNoteItem != null) {
            NoteDetailSheet(
                item = selectedNoteItem!!,
                onSave = { updated ->
                    viewModel.updateItem(updated)
                    selectedNoteItem = null
                },
                onDelete = {
                    viewModel.deleteTask(it)
                    selectedNoteItem = null
                },
                onDismiss = { selectedNoteItem = null }
            )
        }

        // Modals & Sheets
        if (uiState.isMorningBriefOpen) {
            MorningBriefDialog(
                brief = uiState.morningBrief,
                allTasks = todayTasks + inboxTasks,
                isLoading = uiState.isGeneratingAi,
                onAccept = { taskIds -> viewModel.acceptMorningBriefTop3(taskIds) },
                onDismiss = { viewModel.dismissMorningBrief() }
            )
        }

        if (uiState.isEveningReviewOpen) {
            EveningReviewDialog(
                review = uiState.eveningReview,
                isLoading = uiState.isGeneratingAi,
                onRescheduleTomorrow = { ids -> viewModel.rescheduleSlippedTasks(ids) },
                onDismiss = { viewModel.dismissEveningReview() }
            )
        }

        if (uiState.isSnoozeCoachOpen) {
            SnoozeCoachDialog(
                item = uiState.snoozeCoachItem,
                breakdown = uiState.snoozeBreakdown,
                isLoading = uiState.isGeneratingAi,
                onApplySubtasks = { originalItem, subtasks -> viewModel.applySnoozeSubtasks(originalItem, subtasks) },
                onDismiss = { viewModel.dismissSnoozeCoach() }
            )
        }

        if (uiState.isIntelligenceOpen) {
            IntelligenceDialog(
                query = uiState.intelligenceQuery,
                answer = uiState.intelligenceAnswer,
                isLoading = uiState.isIntelligenceLoading,
                onItemClick = { item ->
                    if (item.type == AnchorItem.TYPE_NOTE) {
                        selectedNoteItem = item
                    } else {
                        selectedDetailItem = item
                    }
                },
                onDismiss = { viewModel.dismissIntelligence() }
            )
        }

        if (uiState.editingItem != null) {
            EditItemSheet(
                item = uiState.editingItem!!,
                onSave = { updated -> viewModel.updateItem(updated) },
                onDismiss = { viewModel.dismissEditing() }
            )
        }

        if (uiState.isSettingsOpen) {
            SettingsSheet(
                morningTime = uiState.morningTime,
                eveningTime = uiState.eveningTime,
                quietHours = uiState.quietHours,
                escalationIntensity = uiState.escalationIntensity,
                deadAppHours = uiState.deadAppHours,
                onSave = { morning, evening, quiet, intensity, deadAppH ->
                    viewModel.updateSettings(morning, evening, quiet, intensity, deadAppH)
                },
                onTestNotification = {
                    viewModel.triggerTestNotification(context)
                    Toast.makeText(context, "Notification sent", Toast.LENGTH_SHORT).show()
                },
                onLoadSampleData = {
                    viewModel.loadSampleData()
                    Toast.makeText(context, "Loaded 10 realistic sample anchors", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { viewModel.dismissSettings() }
            )
        }
    }
}
