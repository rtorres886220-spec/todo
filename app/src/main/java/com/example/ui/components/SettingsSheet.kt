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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.GeistMono
import com.example.ui.theme.HairlineColor
import com.example.ui.theme.Surface1
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.HapticHelper

import com.example.data.DiagnosticsReport
import com.example.ui.theme.Surface2
import com.example.ui.theme.TintSageFg
import com.example.ui.theme.TintApricotFg
import androidx.compose.material3.LinearProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    morningTime: String,
    eveningTime: String,
    quietHours: String,
    escalationIntensity: String,
    deadAppHours: Int,
    diagnosticsReport: DiagnosticsReport? = null,
    isRunningDiagnostics: Boolean = false,
    onRunDiagnostics: () -> Unit = {},
    onSave: (morning: String, evening: String, quiet: String, intensity: String, deadAppH: Int) -> Unit,
    onTestNotification: () -> Unit,
    onLoadSampleData: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var morning by remember { mutableStateOf(morningTime) }
    var evening by remember { mutableStateOf(eveningTime) }
    var quiet by remember { mutableStateOf(quietHours) }
    var intensity by remember { mutableStateOf(escalationIntensity) }
    var deadApp by remember { mutableStateOf(deadAppHours) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(3.dp)
                    .drawBehind { drawRect(color = HairlineColor) }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = SurfaceHighlight,
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
                    text = "SETTINGS",
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    letterSpacing = 0.14.em
                )

                Text(
                    text = "CLOSE",
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    letterSpacing = 0.14.em,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Morning & Evening Times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MORNING BRIEF",
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        letterSpacing = 0.14.em
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    QuietInputField(value = morning, onValueChange = { morning = it })
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EVENING REVIEW",
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        letterSpacing = 0.14.em
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    QuietInputField(value = evening, onValueChange = { evening = it })
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quiet Hours
            Text(
                text = "QUIET HOURS",
                fontFamily = GeistMono,
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 0.14.em
            )
            Spacer(modifier = Modifier.height(6.dp))
            QuietInputField(value = quiet, onValueChange = { quiet = it })

            Spacer(modifier = Modifier.height(22.dp))

            // Escalation Intensity
            Text(
                text = "ESCALATION INTENSITY",
                fontFamily = GeistMono,
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 0.14.em
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf("Gentle", "Standard", "Persistent").forEach { level ->
                    val isSelected = intensity.equals(level, ignoreCase = true)
                    Text(
                        text = level.uppercase(),
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        color = if (isSelected) AccentGold else TextTertiary,
                        letterSpacing = 0.14.em,
                        modifier = Modifier
                            .clickable {
                                HapticHelper.vibrate8ms(context)
                                intensity = level
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Dead-App Check-In
            Text(
                text = "INACTIVITY NUDGE",
                fontFamily = GeistMono,
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 0.14.em
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                listOf(0 to "OFF", 6 to "6H", 12 to "12H", 24 to "24H").forEach { (h, label) ->
                    val isSelected = deadApp == h
                    Text(
                        text = label,
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        color = if (isSelected) AccentGold else TextTertiary,
                        letterSpacing = 0.14.em,
                        modifier = Modifier
                            .clickable {
                                HapticHelper.vibrate8ms(context)
                                deadApp = h
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Subsystem Health & Diagnostics Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRoundRect(
                            color = Surface2,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                        )
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUBSYSTEM HEALTH",
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        letterSpacing = 0.14.em
                    )

                    Text(
                        text = if (isRunningDiagnostics) "PROBING..." else "RUN PROBE",
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        color = AccentGold,
                        letterSpacing = 0.14.em,
                        modifier = Modifier
                            .clickable(enabled = !isRunningDiagnostics) {
                                HapticHelper.vibrate8ms(context)
                                onRunDiagnostics()
                            }
                            .testTag("run_diagnostics_button")
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (isRunningDiagnostics) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = AccentGold,
                        trackColor = HairlineColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Verifying Room SQLite, AI engine, Alarms & Prefs...",
                        fontFamily = GeistMono,
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                } else if (diagnosticsReport != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (diagnosticsReport.allPassing) "● ALL SYSTEMS OPERATIONAL" else "▲ CHECK WARNING",
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            color = if (diagnosticsReport.allPassing) TintSageFg else TintApricotFg,
                            letterSpacing = 0.1.em
                        )
                        Text(
                            text = "${diagnosticsReport.totalLatencyMs}ms",
                            fontFamily = GeistMono,
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    diagnosticsReport.probes.forEach { probe ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = probe.name,
                                    fontFamily = GeistMono,
                                    fontSize = 10.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = probe.statusBadge,
                                    fontFamily = GeistMono,
                                    fontSize = 10.sp,
                                    color = if (probe.isHealthy) TintSageFg else TintApricotFg
                                )
                            }
                            Text(
                                text = "${probe.details} (${probe.latencyMs}ms)",
                                fontFamily = GeistMono,
                                fontSize = 9.sp,
                                color = TextTertiary
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap 'RUN PROBE' to verify live database, AI pipeline, alarms, and settings.",
                        fontFamily = GeistMono,
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Load Sample Data action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticHelper.vibrate8ms(context)
                        onLoadSampleData()
                    }
                    .testTag("load_sample_data_button")
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LOAD SAMPLE DATA",
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                    color = AccentGold,
                    letterSpacing = 0.14.em
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Test Notification action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticHelper.vibrate8ms(context)
                        onTestNotification()
                    }
                    .testTag("test_notification_button")
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DISPATCH TEST NUDGE",
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    letterSpacing = 0.14.em
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Save Settings action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticHelper.vibrate8ms(context)
                        onSave(morning, evening, quiet, intensity, deadApp)
                    }
                    .testTag("save_settings_button")
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE SETTINGS",
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                    color = AccentGold,
                    letterSpacing = 0.14.em
                )
            }
        }
    }
}

@Composable
private fun QuietInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = HairlineColor,
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
                fontFamily = GeistMono,
                fontSize = 13.sp,
                color = TextPrimary,
                letterSpacing = 0.14.em
            ),
            singleLine = true,
            cursorBrush = SolidColor(AccentGold),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
