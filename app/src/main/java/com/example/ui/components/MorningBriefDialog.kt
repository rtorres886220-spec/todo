package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.MorningBrief
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

@Composable
fun MorningBriefDialog(
    brief: MorningBrief?,
    allTasks: List<AnchorItem>,
    isLoading: Boolean,
    onAccept: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .drawBehind {
                    drawRect(color = QuietBackgroundLift)
                    // 1px top highlight
                    drawLine(
                        color = QuietHighlight,
                        start = Offset(0f, 0.5f),
                        end = Offset(size.width, 0.5f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(24.dp)
                .testTag("morning_brief_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MORNING BRIEF",
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = QuietAccent,
                        letterSpacing = 0.14.em
                    )

                    Text(
                        text = "CLOSE",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isLoading || brief == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = QuietAccent,
                                strokeWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Distilling focus anchors...",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = QuietTextSecondary,
                                letterSpacing = 0.14.em
                            )
                        }
                    }
                } else {
                    // Headline and motivation
                    Text(
                        text = brief.focusHeadline,
                        fontFamily = Manrope,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = QuietTextPrimary,
                        letterSpacing = (-0.01).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"${brief.motivationalQuote}\"",
                        fontFamily = Manrope,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = QuietTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ESTIMATED FOCUS // ${brief.estimatedTotalMinutes}M",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietAccent,
                        letterSpacing = 0.14.em
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    QuietHairline()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "TOP 3 MUST-DO ANCHORS",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = QuietTextSecondary,
                        letterSpacing = 0.14.em
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val topTasks = allTasks.filter { brief.topTaskIds.contains(it.id) }
                        .ifEmpty { allTasks.take(3) }

                    topTasks.forEachIndexed { index, task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "0${index + 1}",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = QuietAccent,
                                letterSpacing = 0.14.em
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = task.title,
                                fontFamily = Manrope,
                                fontSize = 15.sp,
                                color = QuietTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (task.estimatedMinutes != null) {
                                Text(
                                    text = "${task.estimatedMinutes}M",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = QuietTextTertiary,
                                    letterSpacing = 0.14.em
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticHelper.vibrate8ms(context)
                                onAccept(brief.topTaskIds)
                            }
                            .testTag("accept_morning_plan_button")
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ACCEPT TODAY'S PLAN",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = QuietAccent,
                            letterSpacing = 0.14.em
                        )
                    }
                }
            }
        }
    }
}
