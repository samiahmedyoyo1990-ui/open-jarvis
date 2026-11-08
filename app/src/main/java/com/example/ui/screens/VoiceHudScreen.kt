package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.JarvisViewModel
import com.example.ui.components.AssistantState
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.CyberCard
import com.example.ui.components.JarvisArcOrb
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CyanArcDark
import com.example.ui.theme.CyanArcPrimary
import com.example.ui.theme.HologramBlue
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VoiceHudScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val lastAiResponse by viewModel.lastAiResponse.collectAsStateWithLifecycle()
    val lastActionFeedback by viewModel.lastActionFeedback.collectAsStateWithLifecycle()
    val visualizerAmplitude by viewModel.visualizerAmplitude.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        }
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("voice_hud_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = CyanArcPrimary
                    )
                }

                StatusBadge(state = assistantState)

                IconButton(
                    onClick = {
                        if (assistantState == AssistantState.SPEAKING) {
                            viewModel.stopSpeaking()
                        } else {
                            viewModel.speakResponse(lastAiResponse)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (assistantState == AssistantState.SPEAKING) Icons.Default.Stop else Icons.Default.VolumeUp,
                        contentDescription = "অডিও টগল",
                        tint = if (assistantState == AssistantState.SPEAKING) NeonEmerald else CyanArcPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Dynamic Arc Orb
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    JarvisArcOrb(
                        state = assistantState,
                        amplitude = visualizerAmplitude,
                        size = 230.dp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    AudioWaveformVisualizer(
                        isLive = assistantState == AssistantState.LISTENING || assistantState == AssistantState.SPEAKING,
                        amplitude = visualizerAmplitude,
                        barCount = 24,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            // Transcript & Live Communication Card
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = when (assistantState) {
                    AssistantState.LISTENING -> CyanArcPrimary
                    AssistantState.SPEAKING -> NeonEmerald
                    AssistantState.THINKING -> HologramBlue
                    else -> BorderGlow
                }
            ) {
                if (liveTranscript.isNotBlank()) {
                    Text(
                        text = "আপনি বলেছেন:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanArcDark,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "\"$liveTranscript\"",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "JARVIS:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanArcPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = lastAiResponse,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (!lastActionFeedback.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚡ $lastActionFeedback",
                        fontSize = 12.sp,
                        color = NeonEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Push-to-Talk Giant Voice Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
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
                            width = 3.dp,
                            color = if (assistantState == AssistantState.LISTENING) NeonEmerald else CyanArcPrimary,
                            shape = CircleShape
                        )
                        .clickable {
                            if (assistantState == AssistantState.LISTENING) {
                                viewModel.stopListening()
                            } else {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        .testTag("voice_hud_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (assistantState == AssistantState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "কথা বলুন",
                        tint = if (assistantState == AssistantState.LISTENING) Color.White else CyanArcPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = if (assistantState == AssistantState.LISTENING) "শুনছি... কথা বলা শেষ হলে ট্যাপ করুন" else "কথা বলতে ট্যাপ করুন (Push to Talk)",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
