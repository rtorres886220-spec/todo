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
import com.example.ai.EveningReview
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Manrope
import com.example.ui.theme.QuietAccent
import com.example.ui.theme.QuietBackgroundLift
import com.example.ui.theme.QuietHairline
import com.example.ui.theme.QuietHighlight
import com.example.ui.theme.QuietTextPrimary
import com.example.ui.theme.QuietTextSecondary
import com.example.ui.theme.QuietTextTertiary
import com.example.ui.theme.QuietUrgency
import com.example.ui.util.HapticHelper

@Composable
fun EveningReviewDialog(
    review: EveningReview?,
    isLoading: Boolean,
    onRescheduleTomorrow: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
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
                .testTag("evening_review_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EVENING REVIEW",
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = QuietTextSecondary,
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

                if (isLoading || review == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = QuietTextSecondary,
                                strokeWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Summarizing today's achievements...",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = QuietTextSecondary,
                                letterSpacing = 0.14.em
                            )
                        }
                    }
                } else {
                    Text(
                        text = "${review.completedCount} ANCHORS COMPLETED",
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        color = QuietTextPrimary,
                        letterSpacing = 0.14.em
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = review.summary,
                        fontFamily = Manrope,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = QuietTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "\"${review.reflection}\"",
                        fontFamily = Manrope,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = QuietTextTertiary
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    QuietHairline()
                    Spacer(modifier = Modifier.height(16.dp))

                    if (review.slippedTaskIds.isNotEmpty()) {
                        Text(
                            text = "${review.slippedTaskIds.size} ITEMS SLIPPED",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = QuietUrgency,
                            letterSpacing = 0.14.em
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Momentum continues tomorrow with a single tap.",
                            fontFamily = Manrope,
                            fontSize = 15.sp,
                            color = QuietTextSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticHelper.vibrate8ms(context)
                                    onRescheduleTomorrow(review.slippedTaskIds)
                                }
                                .testTag("reschedule_slipped_button")
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "RESCHEDULE ALL TO TOMORROW",
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                color = QuietAccent,
                                letterSpacing = 0.14.em
                            )
                        }
                    } else {
                        Text(
                            text = "Zero slipped tasks. Everything is grounded.",
                            fontFamily = Manrope,
                            fontSize = 15.sp,
                            color = QuietTextPrimary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDismiss() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CLOSE REVIEW",
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                color = QuietTextSecondary,
                                letterSpacing = 0.14.em
                            )
                        }
                    }
                }
            }
        }
    }
}
