package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.agents.AgentRole
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CyanArcPrimary
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AiAgentsScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val multiAgentEngine = viewModel.multiAgentEngine
    val session by multiAgentEngine.currentSession.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var taskPrompt by remember { mutableStateOf("") }
    var selectedRoles by remember {
        mutableStateOf(listOf(AgentRole.PLANNING, AgentRole.RESEARCH, AgentRole.REVIEWER))
    }

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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).testTag("agents_back_button")
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
                            text = "AI AGENTS COLLABORATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "বহুমুখী বিশেষায়িত এজেন্ট সমবায় ইঞ্জিন",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Task Input Card
            CyberCard {
                Text(
                    text = "টাস্ক বা জটিল সমস্যা লিখুন",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = taskPrompt,
                    onValueChange = { taskPrompt = it },
                    placeholder = {
                        Text("যেমন: 'একটি অ্যান্ড্রয়েড ফিটনেস অ্যাপ্লিকেশনের আর্কিটেকচার ও ডেটাবেজ ডিজাইন তৈরি করো'", fontSize = 12.sp, color = TextMuted)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("agent_task_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisTheme.config.primaryAccent,
                        unfocusedBorderColor = BorderGlow,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "অংশগ্রহণকারী বিশেষায়িত এজেন্ট নির্বাচন করুন:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Agent Role Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AgentRole.entries.chunked(2).forEach { rowRoles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowRoles.forEach { role ->
                                val isSelected = selectedRoles.contains(role)
                                Surface(
                                    onClick = {
                                        selectedRoles = if (isSelected) {
                                            if (selectedRoles.size > 1) selectedRoles - role else selectedRoles
                                        } else {
                                            selectedRoles + role
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) JarvisTheme.config.primaryAccent.copy(alpha = 0.2f) else JarvisTheme.config.cardBg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (isSelected) JarvisTheme.config.primaryAccent else BorderGlow,
                                            RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) JarvisTheme.config.primaryAccent else TextMuted)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = role.banglaTitle,
                                            fontSize = 11.sp,
                                            color = if (isSelected) JarvisTheme.config.primaryAccent else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (taskPrompt.isNotBlank() && session?.isRunning != true) {
                            scope.launch {
                                multiAgentEngine.executeCollaboration(taskPrompt, selectedRoles)
                            }
                        }
                    },
                    enabled = taskPrompt.isNotBlank() && session?.isRunning != true,
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.primaryAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("start_agents_button")
                ) {
                    if (session?.isRunning == true) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = JarvisTheme.config.backgroundColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("এজেন্টগণ কাজ করছেন...", color = JarvisTheme.config.backgroundColor, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = JarvisTheme.config.backgroundColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("এজেন্ট সমবায় শুরু করুন", color = JarvisTheme.config.backgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Results Section
            AnimatedVisibility(visible = session != null) {
                session?.let { activeSession ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "এজেন্টদের কাজের ধাপ ও অগ্রগতি",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent
                        )

                        activeSession.steps.forEach { step ->
                            CyberCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NeonEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = step.role.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Text(
                                        text = "${step.latencyMs} ms",
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = step.output,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        if (activeSession.finalSynthesis.isNotBlank()) {
                            CyberCard(
                                borderColor = JarvisTheme.config.primaryAccent
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = JarvisTheme.config.primaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "JARVIS MASTER SYNTHESIS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JarvisTheme.config.primaryAccent,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = activeSession.finalSynthesis,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
