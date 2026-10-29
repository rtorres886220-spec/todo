package com.example.ui.views

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.components.QuietEmptyState
import com.example.ui.components.QuietHairline
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Manrope
import com.example.ui.theme.QuietAccent
import com.example.ui.theme.QuietHairline
import com.example.ui.theme.QuietTextPrimary
import com.example.ui.theme.QuietTextSecondary
import com.example.ui.theme.QuietTextTertiary
import com.example.ui.util.HapticHelper
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
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Quiet Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DONE",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = QuietTextSecondary,
                    letterSpacing = 0.14.em
                )
                Text(
                    text = "${doneTasks.size} COMPLETED",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = QuietTextTertiary,
                    letterSpacing = 0.14.em
                )
            }
            QuietHairline(modifier = Modifier.padding(horizontal = 24.dp))
        }

        if (doneTasks.isEmpty()) {
            item {
                QuietEmptyState(message = "Nothing completed yet")
            }
        } else {
            items(doneTasks, key = { it.id }) { item ->
                val completedDateStr = item.completedAt?.let {
                    SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()).format(Date(it)).uppercase()
                } ?: ""

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("done_item_${item.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Completed thin ring check icon
                        Canvas(modifier = Modifier.size(18.dp)) {
                            val stroke = 1.25.dp.toPx()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(
                                color = QuietTextTertiary,
                                radius = size.minDimension / 2f - 2f,
                                style = Stroke(width = stroke)
                            )
                            drawLine(
                                color = QuietTextTertiary,
                                start = Offset(center.x - 3.5.dp.toPx(), center.y),
                                end = Offset(center.x - 1.dp.toPx(), center.y + 2.5.dp.toPx()),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = QuietTextTertiary,
                                start = Offset(center.x - 1.dp.toPx(), center.y + 2.5.dp.toPx()),
                                end = Offset(center.x + 3.5.dp.toPx(), center.y - 2.5.dp.toPx()),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontFamily = Manrope,
                                fontSize = 15.sp,
                                color = QuietTextTertiary,
                                textDecoration = TextDecoration.LineThrough,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (completedDateStr.isNotBlank()) {
                                Text(
                                    text = completedDateStr,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = QuietTextTertiary.copy(alpha = 0.8f),
                                    letterSpacing = 0.14.em
                                )
                            }
                        }

                        // Restore action
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    HapticHelper.vibrate8ms(context)
                                    onRestore(item)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "RESTORE",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = QuietAccent,
                                letterSpacing = 0.14.em
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Delete action
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    HapticHelper.vibrate8ms(context)
                                    onDelete(item)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "DEL",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = QuietTextTertiary,
                                letterSpacing = 0.14.em
                            )
                        }
                    }

                    QuietHairline(modifier = Modifier.padding(start = 24.dp))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(170.dp))
        }
    }
}
