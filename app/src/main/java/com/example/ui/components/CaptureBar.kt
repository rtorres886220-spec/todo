package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.GeistMono
import com.example.ui.theme.HairlineColor
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.ShadowAmbient
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.util.HapticHelper

@Composable
fun CaptureBar(
    onCapture: (String) -> Unit,
    clipboardText: String?,
    onAcceptClipboard: () -> Unit,
    onDismissClipboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                HapticHelper.vibrate8ms(context)
                onCapture(spokenText)
            }
        }
    }

    fun submitAndClear() {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            HapticHelper.vibrate8ms(context)
            onCapture(trimmed)
            text = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        // Clipboard detection pill
        AnimatedVisibility(
            visible = clipboardText != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (clipboardText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .border(1.dp, HairlineColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("clipboard_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "From Clipboard: \"${clipboardText.take(28)}\"",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "Add Task",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AccentGold,
                        modifier = Modifier
                            .clickable {
                                HapticHelper.vibrate8ms(context)
                                onAcceptClipboard()
                            }
                            .testTag("clipboard_add_button")
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "✕",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        modifier = Modifier
                            .clickable { onDismissClipboard() }
                            .padding(4.dp)
                    )
                }
            }
        }

        // Quick Intelligence suggestions on focus
        AnimatedVisibility(
            visible = isFocused && text.isEmpty() && clipboardText == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Quick 15m task",
                    "Tomorrow morning",
                    "High priority #urgent"
                ).forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface2)
                            .border(1.dp, HairlineColor, RoundedCornerShape(8.dp))
                            .clickable {
                                HapticHelper.vibrate8ms(context)
                                onCapture(prompt)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = prompt,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Clean Modern Capture Input Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = ShadowAmbient,
                        topLeft = Offset(0f, 2.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )
                }
                .clip(RoundedCornerShape(16.dp))
                .background(Surface1)
                .border(
                    width = 1.dp,
                    color = if (isFocused) AccentGold else HairlineColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                            .testTag("capture_input"),
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(AccentGold),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitAndClear() }),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                if (text.isEmpty()) {
                                    Text(
                                        text = "Add task, meeting, or note...",
                                        color = TextTertiary,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (text.isNotBlank()) {
                    // Send button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AccentGold)
                            .clickable { submitAndClear() }
                            .testTag("capture_submit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val stroke = 2.dp.toPx()
                            val w = size.width
                            val h = size.height
                            // Upward arrow
                            drawLine(color = Color.White, start = Offset(w * 0.5f, h * 0.8f), end = Offset(w * 0.5f, h * 0.2f), strokeWidth = stroke, cap = StrokeCap.Round)
                            drawLine(color = Color.White, start = Offset(w * 0.25f, h * 0.45f), end = Offset(w * 0.5f, h * 0.2f), strokeWidth = stroke, cap = StrokeCap.Round)
                            drawLine(color = Color.White, start = Offset(w * 0.75f, h * 0.45f), end = Offset(w * 0.5f, h * 0.2f), strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }
                } else {
                    // Voice input mic button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Surface2)
                            .clickable {
                                HapticHelper.vibrate8ms(context)
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak task...")
                                    }
                                    voiceLauncher.launch(intent)
                                } catch (_: Exception) {}
                            }
                            .testTag("voice_capture_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val stroke = 1.5.dp.toPx()
                            val w = size.width
                            val h = size.height
                            // Mic body
                            drawRoundRect(
                                color = TextSecondary,
                                topLeft = Offset(w * 0.35f, h * 0.15f),
                                size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.45f),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                style = Stroke(width = stroke)
                            )
                            // Cradle arc
                            drawArc(
                                color = TextSecondary,
                                startAngle = 0f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(w * 0.22f, h * 0.3f),
                                size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.4f),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            // Stem
                            drawLine(color = TextSecondary, start = Offset(w * 0.5f, h * 0.7f), end = Offset(w * 0.5f, h * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
}
