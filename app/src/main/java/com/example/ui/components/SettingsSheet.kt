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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AnchorCyan
import com.example.ui.theme.AnchorEmerald
import com.example.ui.theme.AnchorOutline
import com.example.ui.theme.AnchorSurface
import com.example.ui.theme.AnchorTextPrimary
import com.example.ui.theme.AnchorTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    morningTime: String,
    eveningTime: String,
    quietHours: String,
    escalationIntensity: String,
    deadAppHours: Int,
    onSave: (morning: String, evening: String, quiet: String, intensity: String, deadAppH: Int) -> Unit,
    onTestNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var morning by remember { mutableStateOf(morningTime) }
    var evening by remember { mutableStateOf(eveningTime) }
    var quiet by remember { mutableStateOf(quietHours) }
    var intensity by remember { mutableStateOf(escalationIntensity) }
    var deadApp by remember { mutableStateOf(deadAppHours) }

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
                    text = "Anchor Settings",
                    color = AnchorTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onDismiss) {
                    Text("Close", color = AnchorTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Morning & Evening Times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = morning,
                    onValueChange = { morning = it },
                    label = { Text("Morning Brief Time") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AnchorCyan,
                        unfocusedBorderColor = AnchorOutline,
                        focusedTextColor = AnchorTextPrimary,
                        unfocusedTextColor = AnchorTextPrimary
                    )
                )

                OutlinedTextField(
                    value = evening,
                    onValueChange = { evening = it },
                    label = { Text("Evening Review Time") },
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

            // Quiet Hours
            OutlinedTextField(
                value = quiet,
                onValueChange = { quiet = it },
                label = { Text("Quiet Hours (e.g. 22:00 - 07:00)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AnchorCyan,
                    unfocusedBorderColor = AnchorOutline,
                    focusedTextColor = AnchorTextPrimary,
                    unfocusedTextColor = AnchorTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Escalation Intensity
            Text(
                text = "ESCALATION LADDER INTENSITY",
                color = AnchorTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Gentle", "Standard", "Persistent").forEach { level ->
                    FilterChip(
                        selected = intensity == level,
                        onClick = { intensity = level },
                        label = { Text(level) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AnchorCyan.copy(alpha = 0.2f),
                            selectedLabelColor = AnchorCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dead-App Check-In
            Text(
                text = "DEAD-APP CHECK-IN NUDGE",
                color = AnchorTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Off", 6 to "6 Hours", 12 to "12 Hours", 24 to "24 Hours").forEach { (h, label) ->
                    FilterChip(
                        selected = deadApp == h,
                        onClick = { deadApp = h },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AnchorCyan.copy(alpha = 0.2f),
                            selectedLabelColor = AnchorCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Test Notification Button
            OutlinedButton(
                onClick = onTestNotification,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("test_notification_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = AnchorCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Push Notification & Escalation", color = AnchorCyan)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onSave(morning, evening, quiet, intensity, deadApp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_settings_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AnchorCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Settings", color = AnchorSurface, fontWeight = FontWeight.Bold)
            }
        }
    }
}
