package com.example.ui.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
fun NotesView(
    notes: List<AnchorItem>,
    onDelete: (AnchorItem) -> Unit,
    onEdit: (AnchorItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTag by remember { mutableStateOf<String?>(null) }

    val allTags = notes.flatMap { it.tagList }.distinct()
    val filteredNotes = if (selectedTag == null) notes else notes.filter { it.tagList.contains(selectedTag) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Tag filters (Quiet text filters, no pills)
        if (allTags.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "ALL",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = if (selectedTag == null) QuietAccent else QuietTextTertiary,
                            letterSpacing = 0.14.em,
                            modifier = Modifier
                                .clickable { selectedTag = null }
                                .padding(vertical = 4.dp)
                        )
                    }
                    items(allTags) { tag ->
                        val isSelected = selectedTag == tag
                        Text(
                            text = "#${tag.uppercase()}",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = if (isSelected) QuietAccent else QuietTextTertiary,
                            letterSpacing = 0.14.em,
                            modifier = Modifier
                                .clickable { selectedTag = if (isSelected) null else tag }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                QuietHairline(modifier = Modifier.padding(horizontal = 24.dp))
            }
        }

        if (filteredNotes.isEmpty()) {
            item {
                QuietEmptyState(message = "No notes yet")
            }
        } else {
            items(filteredNotes, key = { it.id }) { item ->
                val dateStr = SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()).format(Date(item.createdAt)).uppercase()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(item) }
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .testTag("note_card_${item.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateStr,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = QuietTextTertiary,
                            letterSpacing = 0.14.em
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Copy action
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        HapticHelper.vibrate8ms(context)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Anchor Note", item.title)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Note copied", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "COPY",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = QuietTextSecondary,
                                    letterSpacing = 0.14.em
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

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
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        fontFamily = Manrope,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = QuietTextPrimary,
                        letterSpacing = 0.sp
                    )

                    if (item.tagList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.tagList.joinToString("  ") { "#$it" },
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

        item {
            Spacer(modifier = Modifier.height(170.dp))
        }
    }
}
