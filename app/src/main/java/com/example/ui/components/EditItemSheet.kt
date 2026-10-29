package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Manrope
import com.example.ui.theme.QuietAccent
import com.example.ui.theme.QuietBackgroundLift
import com.example.ui.theme.QuietHairline
import com.example.ui.theme.QuietHighlight
import com.example.ui.theme.QuietTextPrimary
import com.example.ui.theme.QuietTextSecondary
import com.example.ui.theme.QuietTextTertiary
import com.example.ui.util.HapticHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemSheet(
    item: AnchorItem,
    onSave: (AnchorItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var title by remember { mutableStateOf(item.title) }
    var type by remember { mutableStateOf(item.type) }
    var dueDate by remember { mutableStateOf(item.dueDate ?: "") }
    var dueTime by remember { mutableStateOf(item.dueTime ?: "") }
    var priority by remember { mutableStateOf(item.priority) }
    var tags by remember { mutableStateOf(item.tags) }
    var estimatedMinutes by remember { mutableStateOf(item.estimatedMinutes?.toString() ?: "25") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuietBackgroundLift,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(1.dp)
                    .drawBehind { drawRect(color = QuietHairline) }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // 1px top highlight
                    drawLine(
                        color = QuietHighlight,
                        start = Offset(0f, 0.5f),
                        end = Offset(size.width, 0.5f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EDIT ANCHOR",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = QuietTextPrimary,
                    letterSpacing = 0.14.em
                )

                Text(
                    text = "CANCEL",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = QuietTextSecondary,
                    letterSpacing = 0.14.em,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title input
            Text(
                text = "CONTENT",
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = QuietTextSecondary,
                letterSpacing = 0.14.em
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = QuietHairline,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                    .padding(vertical = 6.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Light,
                        fontSize = 16.sp,
                        color = QuietTextPrimary
                    ),
                    cursorBrush = SolidColor(QuietAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_title_input")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Type Toggle (Quiet mono)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = "TASK",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = if (type == AnchorItem.TYPE_TASK) QuietAccent else QuietTextTertiary,
                    letterSpacing = 0.14.em,
                    modifier = Modifier
                        .clickable { type = AnchorItem.TYPE_TASK }
                        .padding(vertical = 4.dp)
                )
                Text(
                    text = "NOTE",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = if (type == AnchorItem.TYPE_NOTE) QuietAccent else QuietTextTertiary,
                    letterSpacing = 0.14.em,
                    modifier = Modifier
                        .clickable { type = AnchorItem.TYPE_NOTE }
                        .padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Due Date & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DUE DATE (YYYY-MM-DD)",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    QuietEditField(value = dueDate, onValueChange = { dueDate = it })
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TIME (HH:MM)",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    QuietEditField(value = dueTime, onValueChange = { dueTime = it })
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Priority & Estimated Minutes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PRIORITY",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    QuietEditField(value = priority, onValueChange = { priority = it })
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EST. MINUTES",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    QuietEditField(value = estimatedMinutes, onValueChange = { estimatedMinutes = it })
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tags
            Text(
                text = "TAGS",
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = QuietTextSecondary,
                letterSpacing = 0.14.em
            )
            Spacer(modifier = Modifier.height(6.dp))
            QuietEditField(value = tags, onValueChange = { tags = it })

            Spacer(modifier = Modifier.height(28.dp))

            // Save action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticHelper.vibrate8ms(context)
                        onSave(
                            item.copy(
                                title = title.trim(),
                                type = type,
                                dueDate = dueDate.ifBlank { null },
                                dueTime = dueTime.ifBlank { null },
                                priority = priority.lowercase().ifBlank { AnchorItem.PRIORITY_MEDIUM },
                                tags = tags.trim(),
                                estimatedMinutes = estimatedMinutes.toIntOrNull()
                            )
                        )
                    }
                    .testTag("save_edit_button")
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE CHANGES",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = QuietAccent,
                    letterSpacing = 0.14.em
                )
            }
        }
    }
}

@Composable
private fun QuietEditField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = QuietHairline,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .padding(vertical = 6.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                color = QuietTextPrimary,
                letterSpacing = 0.14.em
            ),
            singleLine = true,
            cursorBrush = SolidColor(QuietAccent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
