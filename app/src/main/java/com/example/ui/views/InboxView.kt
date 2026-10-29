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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.components.AnchorCard
import com.example.ui.theme.AnchorAmber
import com.example.ui.theme.AnchorCyan
import com.example.ui.theme.AnchorTextPrimary
import com.example.ui.theme.AnchorTextSecondary
import com.example.ui.theme.AnchorTextTertiary

@Composable
fun InboxView(
    inboxItems: List<AnchorItem>,
    overdueItems: List<AnchorItem>,
    onMoveToToday: (AnchorItem) -> Unit,
    onSnooze: (AnchorItem) -> Unit,
    onComplete: (AnchorItem) -> Unit,
    onDelete: (AnchorItem) -> Unit,
    onEdit: (AnchorItem) -> Unit,
    onSnoozeCoach: (AnchorItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Overdue Items pinned to the top
        if (overdueItems.isNotEmpty()) {
            item {
                Text(
                    text = "🚨 OVERDUE ANCHORS (PINNED)",
                    color = AnchorAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(overdueItems, key = { "inbox_overdue_${it.id}" }) { item ->
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

        // Inbox Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UNSORTED INBOX (${inboxItems.size})",
                    color = AnchorTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Swipe right to send to Today",
                    color = AnchorTextTertiary,
                    fontSize = 11.sp
                )
            }
        }

        if (inboxItems.isEmpty() && overdueItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📥", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Inbox Zero",
                            color = AnchorTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Every thought is anchored or planned. Type below anytime.",
                            color = AnchorTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(inboxItems, key = { it.id }) { item ->
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

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
