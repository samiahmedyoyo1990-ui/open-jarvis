package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ConversationEntity
import com.example.ui.DashboardOverlay
import com.example.ui.JarvisViewModel
import com.example.ui.components.AssistantState
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.CyberCard
import com.example.ui.components.JarvisArcOrb
import com.example.ui.components.SiriFluidVisualizerBackground
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.screens.PersonalizationStudioScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: JarvisViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToVoiceHud: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPersonalization: () -> Unit = {},
    onNavigateToAiProviders: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {}
) {
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val lastAiResponse by viewModel.lastAiResponse.collectAsStateWithLifecycle()
    val lastActionFeedback by viewModel.lastActionFeedback.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val visualizerAmplitude by viewModel.visualizerAmplitude.collectAsStateWithLifecycle()
    val isContinuousMode by viewModel.isContinuousMode.collectAsStateWithLifecycle()
    val animationEnabled by viewModel.animationEnabled.collectAsStateWithLifecycle()
    val animationIntensity by viewModel.animationIntensity.collectAsStateWithLifecycle()
    val reducedMotion by viewModel.reducedMotion.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeOverlay by viewModel.activeOverlay.collectAsStateWithLifecycle()
    val isWakeWordEnabled by viewModel.isWakeWordEnabled.collectAsStateWithLifecycle()
    val isWakeWordServiceRunning by viewModel.isWakeWordServiceRunning.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleListening()
        }
    }

    LaunchedEffect(conversations.size) {
        if (conversations.isNotEmpty()) {
            chatListState.animateScrollToItem(conversations.size - 1)
        }
    }

    val quickActionSuggestions = listOf(
        "🤖 এআই এজেন্টস" to "এআই এজেন্ট টিম সক্রিয় করো",
        "📁 ওয়ার্কস্পেস" to "ওয়ার্কস্পেস তালিকা দেখাও",
        "📦 গিটহাব স্কিলস" to "ইন্সটল করা স্কিলগুলো দেখাও",
        "⚡ স্কিলস মার্কেট" to "প্লাগইন ও স্কিলস দেখাও",
        "🔍 ওয়েব রিসার্চ" to "অনলাইন থেকে সর্বশেষ তথ্য অনুসন্ধান করো",
        "ইউটিউব খোলো" to "YouTube open koro",
        "ব্রাউজার খোলো" to "Open Chrome",
        "নোট সেভ করো" to "আমার জন্য একটা নোট লিখে রাখো",
        "আজকের কাজ" to "আমার আজকের কাজগুলো বলো",
        "ক্যালকুলেটর" to "Calculator open koro",
        "ক্যামেরা চালু করো" to "Camera open koro",
        "ডিভাইস তথ্য" to "ব্যাটারি ও ডিভাইসের তথ্য দাও",
        "ওয়াইফাই সেটিংস" to "Wi-Fi settings open koro",
        "ফোন ডায়ালার" to "ডায়ালার ওপেন করো"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        // 1. Siri-inspired Fluid Atmospheric Animated Visualizer (Stays Behind All Interface Elements)
        if (animationEnabled) {
            SiriFluidVisualizerBackground(
                state = assistantState,
                amplitude = visualizerAmplitude,
                theme = JarvisTheme.config,
                reducedMotion = reducedMotion
            )
        }

        // 2. Main Integrated Foreground Interface (Non-obstructive, fluid, and responsive)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP HEADER: Brand Identity, Continuous Mode Toggle, and Quick Nav
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JARVIS",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent,
                            letterSpacing = 3.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "শুভদিন, $userName",
                        fontSize = 12.sp,
                        color = JarvisTheme.config.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Continuous Hands-Free Mode Toggle Pill
                    Surface(
                        onClick = { viewModel.toggleContinuousMode() },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isContinuousMode) NeonEmerald.copy(alpha = 0.2f) else JarvisTheme.config.cardBg.copy(alpha = 0.7f),
                        modifier = Modifier.border(
                            1.dp,
                            if (isContinuousMode) NeonEmerald.copy(alpha = 0.6f) else JarvisTheme.config.borderGlow,
                            RoundedCornerShape(16.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isContinuousMode) Icons.Default.Headset else Icons.Default.HeadsetOff,
                                contentDescription = "হ্যান্ডস-ফ্রি মোড",
                                tint = if (isContinuousMode) NeonEmerald else JarvisTheme.config.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isContinuousMode) "Hands-Free" else "Manual",
                                fontSize = 11.sp,
                                color = if (isContinuousMode) NeonEmerald else JarvisTheme.config.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Hey JARVIS Background Wake Word Pill
                    Surface(
                        onClick = { viewModel.toggleWakeWord(!isWakeWordEnabled) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isWakeWordServiceRunning) NeonEmerald.copy(alpha = 0.2f) else JarvisTheme.config.cardBg.copy(alpha = 0.7f),
                        modifier = Modifier.border(
                            1.dp,
                            if (isWakeWordServiceRunning) NeonEmerald.copy(alpha = 0.6f) else JarvisTheme.config.borderGlow,
                            RoundedCornerShape(16.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isWakeWordServiceRunning) NeonEmerald else JarvisTheme.config.textSecondary.copy(alpha = 0.4f))
                            )
                            Text(
                                text = "Hey JARVIS",
                                fontSize = 11.sp,
                                color = if (isWakeWordServiceRunning) NeonEmerald else JarvisTheme.config.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Personalization Studio Button
                    IconButton(
                        onClick = onNavigateToPersonalization,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("dashboard_personalization_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "পার্সোনালাইজেশন স্টুডিও",
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // AI Engine & BYOK Management Button
                    IconButton(
                        onClick = onNavigateToAiProviders,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("dashboard_ai_providers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = "AI কোর ইঞ্জিন ও কী ম্যানেজার",
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "সেটিংস",
                            tint = JarvisTheme.config.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Status Badge with active animation and retry support
            StatusBadge(
                state = assistantState,
                onRetry = { viewModel.retryLastQuery() },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Center Holographic Arc Orb (Subtle and reactive in idle, pulsing during interaction)
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clickable {
                        // Tapping orb triggers speech recognition or stops speech
                        if (assistantState == AssistantState.SPEAKING) {
                            viewModel.stopSpeaking()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                JarvisArcOrb(
                    state = assistantState,
                    amplitude = visualizerAmplitude,
                    size = if (assistantState == AssistantState.IDLE) 140.dp else 165.dp
                )
            }

            // Real-Time Audio Waveform (Visible during listening or speaking)
            AnimatedVisibility(
                visible = assistantState == AssistantState.LISTENING || assistantState == AssistantState.SPEAKING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AudioWaveformVisualizer(
                    isLive = true,
                    amplitude = visualizerAmplitude,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            // Live speech transcript preview
            if (liveTranscript.isNotBlank() && assistantState == AssistantState.LISTENING) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, CyanArcDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    color = JarvisSurfaceVariant.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "\"$liveTranscript\"",
                        color = CyanArcPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Error Card with Retry Button
            if (assistantState == AssistantState.ERROR && !errorMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, RedAlert.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    color = RedAlert.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage ?: "একটি ত্রুটি হয়েছে।",
                            fontSize = 12.sp,
                            color = RedAlert,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.retryLastQuery() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "পুনরায় চেষ্টা", tint = CyanArcPrimary)
                        }
                    }
                }
            }

            // Middle Scrollable Conversation Stream (Integrated directly into the dashboard)
            CyberCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                borderColor = when (assistantState) {
                    AssistantState.SPEAKING -> NeonEmerald.copy(alpha = 0.5f)
                    AssistantState.THINKING -> HologramBlue.copy(alpha = 0.5f)
                    AssistantState.EXECUTING -> Color(0xFF00FFE0).copy(alpha = 0.6f)
                    else -> BorderGlow
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONVERSATION & ACTIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanArcPrimary,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (assistantState == AssistantState.SPEAKING) {
                            // Interrupt / Stop speaking button
                            IconButton(
                                onClick = { viewModel.stopSpeaking() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "কথা থামান",
                                    tint = RedAlert,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.speakResponse(lastAiResponse) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "শুনুন",
                                    tint = CyanArcPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable mini conversation feed
                if (conversations.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = lastAiResponse,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "কথা বলতে নিচের মাইকে চাপ দিন বা দ্রুত কমান্ড বেছে নিন।",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(conversations.takeLast(12), key = { it.id }) { msg ->
                            DashboardMessageRow(
                                message = msg,
                                onSpeak = { viewModel.speakResponse(msg.text) }
                            )
                        }

                        if (assistantState == AssistantState.THINKING) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = CyanArcPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "JARVIS ভাবছে...",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Action execution indicator tag
                if (!lastActionFeedback.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HologramBlue.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HologramBlue.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CyanArcPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = lastActionFeedback ?: "",
                                fontSize = 11.sp,
                                color = CyanArcPrimary
                            )
                        }
                    }
                }
            }

            // Quick Command Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(quickActionSuggestions) { (label, command) ->
                    Surface(
                        onClick = { viewModel.processUserQuery(command) },
                        shape = RoundedCornerShape(8.dp),
                        color = JarvisSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.border(1.dp, BorderGlow, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // BOTTOM CONTROL BAR: Text Field + Giant Push-to-Talk Mic
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("এখানে লিখুন বা নির্দেশ দিন...", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_text_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanArcPrimary,
                            unfocusedBorderColor = JarvisSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Send text button
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.processUserQuery(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanArcPrimary)
                            .testTag("dashboard_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "পাঠান",
                            tint = Color(0xFF002229),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Push-to-Talk Mic Button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (assistantState == AssistantState.LISTENING) {
                                        listOf(CyanArcPrimary, HologramBlue)
                                    } else {
                                        listOf(JarvisSurfaceVariant, Color(0xFF0F1B2E))
                                    }
                                )
                            )
                            .border(
                                width = 2.dp,
                                color = if (assistantState == AssistantState.LISTENING) NeonEmerald else CyanArcPrimary,
                                shape = CircleShape
                            )
                            .clickable {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                            .testTag("dashboard_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (assistantState == AssistantState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "কথা বলুন",
                            tint = if (assistantState == AssistantState.LISTENING) Color.White else CyanArcPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Bottom Navigation Shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickNavTab(icon = Icons.Default.Note, label = "নোটস", onClick = { viewModel.setOverlay(DashboardOverlay.NOTES) })
                QuickNavTab(icon = Icons.Default.CheckCircle, label = "টাস্কস", onClick = { viewModel.setOverlay(DashboardOverlay.TASKS) })
                QuickNavTab(icon = Icons.Default.Palette, label = "থিম", onClick = onNavigateToPersonalization)
                QuickNavTab(icon = Icons.Default.AutoAwesome, label = "ফুল চ্যাট", onClick = onNavigateToChat)
                QuickNavTab(icon = Icons.Default.Settings, label = "সেটিংস", onClick = { viewModel.setOverlay(DashboardOverlay.SETTINGS) })
            }
        }

        // 3. Integrated Modal Bottom Sheets for Notes, Tasks, and Settings
        // Keeps user directly on the dashboard without losing continuous voice session!
        if (activeOverlay != DashboardOverlay.NONE) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeOverlay() },
                sheetState = sheetState,
                containerColor = JarvisTheme.config.surfaceColor,
                dragHandle = null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp)
                ) {
                    when (activeOverlay) {
                        DashboardOverlay.NOTES -> {
                            NotesScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.closeOverlay() }
                            )
                        }
                        DashboardOverlay.TASKS -> {
                            TasksScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.closeOverlay() }
                            )
                        }
                        DashboardOverlay.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToPersonalization = onNavigateToPersonalization,
                                onNavigateToPrivacy = onNavigateToPrivacy,
                                onNavigateToAbout = onNavigateToAbout,
                                onBack = { viewModel.closeOverlay() }
                            )
                        }
                        DashboardOverlay.PERSONALIZATION -> {
                            PersonalizationStudioScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.closeOverlay() }
                            )
                        }
                        DashboardOverlay.GITHUB_SKILLS -> {
                            GitHubSkillsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.closeOverlay() }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMessageRow(
    message: ConversationEntity,
    onSpeak: () -> Unit
) {
    val isUser = message.isUser
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 12.dp
            ),
            color = if (isUser) HologramBlue.copy(alpha = 0.22f) else JarvisSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier
                .border(
                    1.dp,
                    if (isUser) HologramBlue.copy(alpha = 0.45f) else BorderGlow,
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "আপনি" else "JARVIS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) HologramBlue else CyanArcPrimary,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = formattedTime, fontSize = 9.sp, color = TextMuted)
                        if (!isUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onSpeak, modifier = Modifier.size(18.dp)) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "শুনুন",
                                    tint = CyanArcPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )

                if (!message.actionExecuted.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚡ ${message.actionExecuted}",
                        fontSize = 10.sp,
                        color = NeonEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickNavTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = JarvisSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.border(1.dp, BorderGlow.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CyanArcPrimary, modifier = Modifier.size(14.dp))
            Text(text = label, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}
