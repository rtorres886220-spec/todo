package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnchorItem
import com.example.ui.theme.AnchorCyan
import com.example.ui.theme.AnchorOutline
import com.example.ui.theme.AnchorSurface
import com.example.ui.theme.AnchorSurfaceVariant
import com.example.ui.theme.AnchorTextPrimary
import com.example.ui.theme.AnchorTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemSheet(
    item: AnchorItem,
    onSave: (AnchorItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        containerColor = AnchorSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Anchor",
                    color = AnchorTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AnchorTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Action") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AnchorCyan,
                    unfocusedBorderColor = AnchorOutline,
                    focusedTextColor = AnchorTextPrimary,
                    unfocusedTextColor = AnchorTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Type Chip Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == AnchorItem.TYPE_TASK,
                    onClick = { type = AnchorItem.TYPE_TASK },
                    label = { Text("Task") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AnchorCyan.copy(alpha = 0.2f),
                        selectedLabelColor = AnchorCyan
                    )
                )
                FilterChip(
                    selected = type == AnchorItem.TYPE_NOTE,
                    onClick = { type = AnchorItem.TYPE_NOTE },
                    label = { Text("Note") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AnchorCyan.copy(alpha = 0.2f),
                        selectedLabelColor = AnchorCyan
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Due Date & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AnchorCyan,
                        unfocusedBorderColor = AnchorOutline,
                        focusedTextColor = AnchorTextPrimary,
                        unfocusedTextColor = AnchorTextPrimary
                    )
                )

                OutlinedTextField(
                    value = dueTime,
                    onValueChange = { dueTime = it },
                    label = { Text("Time (HH:mm)") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AnchorCyan,
                        unfocusedBorderColor = AnchorOutline,
                        focusedTextColor = AnchorTextPrimary,
                        unfocusedTextColor = AnchorTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Priority & Estimated Minutes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text("Priority (high/med/low)") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AnchorCyan,
                        unfocusedBorderColor = AnchorOutline,
                        focusedTextColor = AnchorTextPrimary,
                        unfocusedTextColor = AnchorTextPrimary
                    )
                )

                OutlinedTextField(
                    value = estimatedMinutes,
                    onValueChange = { estimatedMinutes = it },
                    label = { Text("Est. Minutes") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AnchorCyan,
                        unfocusedBorderColor = AnchorOutline,
                        focusedTextColor = AnchorTextPrimary,
                        unfocusedTextColor = AnchorTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated: work, errands)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AnchorCyan,
                    unfocusedBorderColor = AnchorOutline,
                    focusedTextColor = AnchorTextPrimary,
                    unfocusedTextColor = AnchorTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_edit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AnchorCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes", color = AnchorSurface, fontWeight = FontWeight.Bold)
            }
        }
    }
}
