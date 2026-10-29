package com.example.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.theme.AnchorEmerald
import com.example.ui.theme.AnchorEmeraldDim
import com.example.ui.theme.AnchorOutline
import com.example.ui.theme.AnchorSurfaceHighlight
import com.example.ui.theme.AnchorSurfaceVariant
import com.example.ui.theme.AnchorTextPrimary
import com.example.ui.theme.AnchorTextSecondary
import com.example.ui.theme.AnchorTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DoneView(
    doneTasks: List<AnchorItem>,
    onRestore: (AnchorItem) -> Unit,
    onDelete: (AnchorItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AnchorSurfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AnchorEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AnchorEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${doneTasks.size} Anchors Completed",
                            color = AnchorTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Every finished item keeps your mind clutter-free.",
                            color = AnchorTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (doneTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎯", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No completed items yet",
                            color = AnchorTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the checkbox on any anchor to complete it.",
                            color = AnchorTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(doneTasks, key = { it.id }) { item ->
                val completedDateStr = item.completedAt?.let {
                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(it))
                } ?: ""

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AnchorSurfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AnchorOutline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .testTag("done_item_${item.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = AnchorEmeraldDim,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = AnchorTextTertiary,
                                fontSize = 14.sp,
                                textDecoration = TextDecoration.LineThrough
                            )
                            if (completedDateStr.isNotBlank()) {
                                Text(
                                    text = "Completed $completedDateStr",
                                    color = AnchorTextTertiary.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Restore to Inbox button
                        IconButton(
                            onClick = { onRestore(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Restore to Inbox",
                                tint = AnchorEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Delete permanently
                        IconButton(
                            onClick = { onDelete(item) },
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
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
