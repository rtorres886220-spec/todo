package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Manrope
import com.example.ui.theme.QuietAccent
import com.example.ui.theme.QuietGlassFill
import com.example.ui.theme.QuietHairline
import com.example.ui.theme.QuietHighlight
import com.example.ui.theme.QuietTextPrimary
import com.example.ui.theme.QuietTextSecondary
import com.example.ui.theme.QuietTextTertiary
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
    var isRecording by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // Breathing accent line animation on focus
    val infiniteTransition = rememberInfiniteTransition(label = "breathe_pulse")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "line_alpha"
    )

    // Mic orb glow pulsing animation
    val micGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_glow"
    )

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isRecording = false
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
            // Visibly lifted glass surface:
            // rgba(255,255,255,0.04) fill, 1px top highlight rgba(255,255,255,0.10), long soft shadow
            .drawBehind {
                // Long soft shadow cast upwards
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33000000),
                            Color(0x66000000)
                        ),
                        startY = -24.dp.toPx(),
                        endY = 0f
                    ),
                    topLeft = Offset(0f, -24.dp.toPx()),
                    size = Size(size.width, 24.dp.toPx())
                )

                // Glass surface background
                drawRect(color = Color(0xF207080B))
                // rgba(255,255,255,0.04) glass fill
                drawRect(color = QuietGlassFill)

                // 1px top highlight rgba(255,255,255,0.10)
                drawLine(
                    color = QuietHighlight,
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Clipboard detection banner
        AnimatedVisibility(
            visible = clipboardText != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (clipboardText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("clipboard_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLIPBOARD // ${clipboardText.take(24)}",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "ANCHOR THIS",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietAccent,
                        letterSpacing = 0.14.em,
                        modifier = Modifier
                            .clickable {
                                HapticHelper.vibrate8ms(context)
                                onAcceptClipboard()
                            }
                            .testTag("clipboard_add_button")
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "DISMISS",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextTertiary,
                        letterSpacing = 0.14.em,
                        modifier = Modifier
                            .clickable { onDismissClipboard() }
                            .padding(4.dp)
                    )
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
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
                        color = QuietTextPrimary,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Light,
                        fontSize = 15.sp,
                        letterSpacing = 0.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(QuietAccent),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitAndClear() }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Capture a thought or action...",
                                    color = QuietTextSecondary,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Breathing thin accent line under text on focus
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    if (isFocused) {
                        drawLine(
                            color = QuietAccent.copy(alpha = breathingAlpha),
                            start = Offset(0f, 1f),
                            end = Offset(size.width, 1f),
                            strokeWidth = 1.dp.toPx()
                        )
                    } else {
                        drawLine(
                            color = QuietHairline,
                            start = Offset(0f, 1f),
                            end = Offset(size.width, 1f),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (text.isNotBlank()) {
                // Submit trigger: clean instrument action
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { submitAndClear() }
                        .testTag("capture_send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        val stroke = 1.25.dp.toPx()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawLine(
                            color = QuietAccent,
                            start = Offset(center.x - 6.dp.toPx(), center.y),
                            end = Offset(center.x + 6.dp.toPx(), center.y),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = QuietAccent,
                            start = Offset(center.x + 2.dp.toPx(), center.y - 4.dp.toPx()),
                            end = Offset(center.x + 6.dp.toPx(), center.y),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = QuietAccent,
                            start = Offset(center.x + 2.dp.toPx(), center.y + 4.dp.toPx()),
                            end = Offset(center.x + 6.dp.toPx(), center.y),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                }
            } else {
                // Mic: 44px luminous orb with soft accent glow and a thin-stroke microphone glyph inside
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable {
                            try {
                                isRecording = true
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak anchor...")
                                }
                                voiceLauncher.launch(intent)
                            } catch (_: Exception) {
                                isRecording = false
                            }
                        }
                        .testTag("voice_capture_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(44.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val pulseMultiplier = if (isRecording) (micGlowPulse * 1.5f) else micGlowPulse

                        // Soft radial accent glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    QuietAccent.copy(alpha = 0.28f * pulseMultiplier),
                                    QuietAccent.copy(alpha = 0.08f * pulseMultiplier),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = 22.dp.toPx()
                            )
                        )

                        // Thin circular orb border
                        drawCircle(
                            color = QuietAccent.copy(alpha = 0.35f),
                            radius = 18.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Thin-stroke microphone glyph inside
                        val stroke = 1.25.dp.toPx()
                        val micW = 6.dp.toPx()
                        val micH = 10.dp.toPx()
                        val micTop = center.y - 6.5.dp.toPx()
                        val micLeft = center.x - micW / 2f

                        // Rounded mic capsule
                        drawRoundRect(
                            color = QuietAccent,
                            topLeft = Offset(micLeft, micTop),
                            size = Size(micW, micH),
                            cornerRadius = CornerRadius(micW / 2f, micW / 2f),
                            style = Stroke(width = stroke)
                        )

                        // Cradle arc around lower capsule
                        val cradleRadius = 5.5.dp.toPx()
                        drawArc(
                            color = QuietAccent,
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(center.x - cradleRadius, center.y - 2.5.dp.toPx()),
                            size = Size(cradleRadius * 2, cradleRadius * 2),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        // Stem line
                        val stemTop = center.y - 2.5.dp.toPx() + cradleRadius
                        drawLine(
                            color = QuietAccent,
                            start = Offset(center.x, stemTop),
                            end = Offset(center.x, stemTop + 3.5.dp.toPx()),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )

                        // Base horizontal foot
                        drawLine(
                            color = QuietAccent,
                            start = Offset(center.x - 3.5.dp.toPx(), stemTop + 3.5.dp.toPx()),
                            end = Offset(center.x + 3.5.dp.toPx(), stemTop + 3.5.dp.toPx()),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
