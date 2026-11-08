package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.RoutineEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RoutinesScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val routines by viewModel.routines.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newRoutineName by remember { mutableStateOf("") }
    var newTriggerWord by remember { mutableStateOf("") }
    var newActionSpeech by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp).testTag("routines_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = JarvisTheme.config.primaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AUTOMATION & ROUTINES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisTheme.config.primaryAccent,
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.AutoMode,
                                contentDescription = null,
                                tint = JarvisTheme.config.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "স্মার্ট অটোমেশন ও ভয়েস রুটিন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.primaryAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_routine_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = JarvisTheme.config.backgroundColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisTheme.config.backgroundColor)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "নির্দিষ্ট ভয়েস ট্রিগার শব্দ বললে JARVIS সরাসরি এই রুটিনগুলো কার্যকর করে।",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(routines, key = { it.id }) { routine ->
                    CyberCard(
                        borderColor = if (routine.isEnabled) JarvisTheme.config.primaryAccent.copy(alpha = 0.5f) else BorderGlow
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = routine.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (routine.isEnabled) JarvisTheme.config.primaryAccent else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ট্রিগার: \"${routine.triggerValue}\"",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "অ্যাকশন: ${routine.actionPayload}",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    maxLines = 2
                                )
                            }

                            Switch(
                                checked = routine.isEnabled,
                                onCheckedChange = { viewModel.toggleRoutine(routine.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = JarvisTheme.config.backgroundColor,
                                    checkedTrackColor = JarvisTheme.config.primaryAccent
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text("নতুন রুটিন তৈরি করুন", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newRoutineName,
                        onValueChange = { newRoutineName = it },
                        label = { Text("রুটিনের নাম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTriggerWord,
                        onValueChange = { newTriggerWord = it },
                        label = { Text("ভয়েস ট্রিগার শব্দ (যেমন: 'কাজ শেষ')") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newActionSpeech,
                        onValueChange = { newActionSpeech = it },
                        label = { Text("JARVIS এর প্রতিক্রিয়া / অ্যাকশন বার্তা") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRoutineName.isNotBlank() && newTriggerWord.isNotBlank()) {
                            viewModel.saveRoutine(
                                RoutineEntity(
                                    name = newRoutineName.trim(),
                                    triggerType = "VOICE",
                                    triggerValue = newTriggerWord.trim(),
                                    actionType = "SPEAK",
                                    actionPayload = newActionSpeech.trim().ifBlank { "রুটিন সম্পন্ন হয়েছে।" }
                                )
                            )
                            showCreateDialog = false
                            newRoutineName = ""
                            newTriggerWord = ""
                            newActionSpeech = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.primaryAccent)
                ) {
                    Text("সংরক্ষণ", color = JarvisTheme.config.backgroundColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("বাতিল", color = TextSecondary)
                }
            },
            containerColor = JarvisTheme.config.cardBg
        )
    }
}
