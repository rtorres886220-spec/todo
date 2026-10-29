package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CaptureBar
import com.example.ui.components.EditItemSheet
import com.example.ui.components.EveningReviewDialog
import com.example.ui.components.MorningBriefDialog
import com.example.ui.components.SettingsSheet
import com.example.ui.components.SnoozeCoachDialog
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
import com.example.ui.views.DoneView
import com.example.ui.views.InboxView
import com.example.ui.views.NotesView
import com.example.ui.views.TodayView
import com.example.ui.views.UpcomingView
import com.example.viewmodel.AnchorTab
import com.example.viewmodel.AnchorViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    // Trigger Snackbar on Undo message
    LaunchedEffect(uiState.undoMessage) {
        val msg = uiState.undoMessage
        if (msg != null) {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo()
            }
            viewModel.clearUndo()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AnchorCyan.copy(alpha = 0.2f))
                        .border(1.dp, AnchorCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Anchor,
                        contentDescription = "Anchor Logo",
                        tint = AnchorCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anchor",
                        color = AnchorTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = when (uiState.activeTab) {
                            AnchorTab.INBOX -> "Zero-Friction Capture"
                            AnchorTab.TODAY -> "Max 5 Focus Anchors"
                            AnchorTab.UPCOMING -> "Scheduled Timelines"
                            AnchorTab.NOTES -> "Thoughts & Reference"
                            AnchorTab.DONE -> "Closed & Grounded"
                        },
                        color = AnchorTextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.openSettings() },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = AnchorTextSecondary
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Bottom Capture Bar: One-handed Thumb Reachable
                CaptureBar(
                    onCapture = { text -> viewModel.capture(text) },
                    clipboardText = uiState.clipboardCandidate,
                    onAcceptClipboard = { viewModel.acceptClipboard() },
                    onDismissClipboard = { viewModel.dismissClipboard() }
                )

                // 5-Tab Navigation Bar
                NavigationBar(
                    containerColor = AnchorSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                    // Inbox Tab
                    NavigationBarItem(
                        selected = uiState.activeTab == AnchorTab.INBOX,
                        onClick = { viewModel.setTab(AnchorTab.INBOX) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (inboxTasks.isNotEmpty()) {
                                        Badge(containerColor = AnchorCyan, contentColor = AnchorSurface) {
                                            Text("${inboxTasks.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Inbox, contentDescription = "Inbox")
                            }
                        },
                        label = { Text("Inbox", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AnchorCyan,
                            selectedTextColor = AnchorCyan,
                            unselectedIconColor = AnchorTextSecondary,
                            unselectedTextColor = AnchorTextSecondary,
                            indicatorColor = AnchorCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("tab_inbox")
                    )

                    // Today Tab
                    NavigationBarItem(
                        selected = uiState.activeTab == AnchorTab.TODAY,
                        onClick = { viewModel.setTab(AnchorTab.TODAY) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (todayTasks.isNotEmpty() || overdueTasks.isNotEmpty()) {
                                        Badge(containerColor = if (overdueTasks.isNotEmpty()) Color(0xFFF43F5E) else AnchorCyan) {
                                            Text("${todayTasks.size + overdueTasks.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Today, contentDescription = "Today")
                            }
                        },
                        label = { Text("Today", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AnchorCyan,
                            selectedTextColor = AnchorCyan,
                            unselectedIconColor = AnchorTextSecondary,
                            unselectedTextColor = AnchorTextSecondary,
                            indicatorColor = AnchorCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("tab_today")
                    )

                    // Upcoming Tab
                    NavigationBarItem(
                        selected = uiState.activeTab == AnchorTab.UPCOMING,
                        onClick = { viewModel.setTab(AnchorTab.UPCOMING) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Upcoming") },
                        label = { Text("Upcoming", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AnchorCyan,
                            selectedTextColor = AnchorCyan,
                            unselectedIconColor = AnchorTextSecondary,
                            unselectedTextColor = AnchorTextSecondary,
                            indicatorColor = AnchorCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("tab_upcoming")
                    )

                    // Notes Tab
                    NavigationBarItem(
                        selected = uiState.activeTab == AnchorTab.NOTES,
                        onClick = { viewModel.setTab(AnchorTab.NOTES) },
                        icon = { Icon(Icons.Default.Notes, contentDescription = "Notes") },
                        label = { Text("Notes", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AnchorCyan,
                            selectedTextColor = AnchorCyan,
                            unselectedIconColor = AnchorTextSecondary,
                            unselectedTextColor = AnchorTextSecondary,
                            indicatorColor = AnchorCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("tab_notes")
                    )

                    // Done Tab
                    NavigationBarItem(
                        selected = uiState.activeTab == AnchorTab.DONE,
                        onClick = { viewModel.setTab(AnchorTab.DONE) },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Done") },
                        label = { Text("Done", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AnchorCyan,
                            selectedTextColor = AnchorCyan,
                            unselectedIconColor = AnchorTextSecondary,
                            unselectedTextColor = AnchorTextSecondary,
                            indicatorColor = AnchorCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("tab_done")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeTab) {
                AnchorTab.INBOX -> {
                    InboxView(
                        inboxItems = inboxTasks,
                        overdueItems = overdueTasks,
                        onMoveToToday = { viewModel.moveToToday(it) },
                        onSnooze = { viewModel.snoozeTask(it) },
                        onComplete = { viewModel.completeTask(it) },
                        onDelete = { viewModel.deleteTask(it) },
                        onEdit = { viewModel.startEditing(it) },
                        onSnoozeCoach = { viewModel.openSnoozeCoach(it) }
                    )
                }
                AnchorTab.TODAY -> {
                    TodayView(
                        todayTasks = todayTasks,
                        overdueTasks = overdueTasks,
                        onMoveToToday = { viewModel.moveToToday(it) },
                        onSnooze = { viewModel.snoozeTask(it) },
                        onComplete = { viewModel.completeTask(it) },
                        onDelete = { viewModel.deleteTask(it) },
                        onEdit = { viewModel.startEditing(it) },
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
                        onEdit = { viewModel.startEditing(it) },
                        onSnoozeCoach = { viewModel.openSnoozeCoach(it) }
                    )
                }
                AnchorTab.NOTES -> {
                    NotesView(
                        notes = notes,
                        onDelete = { viewModel.deleteTask(it) },
                        onEdit = { viewModel.startEditing(it) }
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
                    Toast.makeText(context, "Notification sent! Check your notification shade.", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { viewModel.dismissSettings() }
            )
        }
    }
}
