package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AnchorCyan
import com.example.ui.theme.AnchorCyanGlow
import com.example.ui.theme.AnchorEmerald
import com.example.ui.theme.AnchorOutline
import com.example.ui.theme.AnchorSurface
import com.example.ui.theme.AnchorSurfaceHighlight
import com.example.ui.theme.AnchorSurfaceVariant
import com.example.ui.theme.AnchorTextPrimary
import com.example.ui.theme.AnchorTextSecondary

@Composable
fun CaptureBar(
    onCapture: (String) -> Unit,
    clipboardText: String?,
    onAcceptClipboard: () -> Unit,
    onDismissClipboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // Voice recognition launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onCapture(spokenText)
            }
        }
    }

    fun submitAndClear() {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            onCapture(trimmed)
            text = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .background(AnchorSurface.copy(alpha = 0.95f))
            .border(width = 1.dp, color = AnchorOutline.copy(alpha = 0.4f), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Clipboard detection banner
        AnimatedVisibility(
            visible = clipboardText != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (clipboardText != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AnchorSurfaceHighlight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("clipboard_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Clipboard",
                            tint = AnchorCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clipboard: \"${clipboardText.take(28)}${if (clipboardText.length > 28) "..." else ""}\"",
                            color = AnchorTextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onAcceptClipboard,
                            modifier = Modifier.testTag("clipboard_add_button")
                        ) {
                            Text("Add this?", color = AnchorCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        IconButton(
                            onClick = onDismissClipboard,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = AnchorTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Full-width Zero-Friction Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("capture_input"),
                placeholder = {
                    Text(
                        text = "Anchor a task or note... (Enter saves instantly)",
                        color = AnchorTextSecondary.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitAndClear() }
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AnchorSurfaceVariant,
                    unfocusedContainerColor = AnchorSurfaceVariant,
                    focusedBorderColor = AnchorCyan,
                    unfocusedBorderColor = AnchorOutline,
                    focusedTextColor = AnchorTextPrimary,
                    unfocusedTextColor = AnchorTextPrimary,
                    cursorColor = AnchorCyan
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (text.isNotBlank()) {
                // Send / Save Button
                IconButton(
                    onClick = { submitAndClear() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(AnchorCyan, AnchorCyanGlow)),
                            shape = CircleShape
                        )
                        .testTag("capture_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Save Anchor",
                        tint = Color(0xFF042F2E),
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                // Large Microphone Button for Voice Capture
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your task or note...")
                            }
                            voiceLauncher.launch(intent)
                        } catch (e: Exception) {
                            // If speech recognition activity not available
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(AnchorCyan.copy(alpha = 0.25f), AnchorSurfaceVariant)
                            ),
                            shape = CircleShape
                        )
                        .border(1.dp, AnchorCyan.copy(alpha = 0.4f), CircleShape)
                        .testTag("voice_capture_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Capture",
                        tint = AnchorCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
