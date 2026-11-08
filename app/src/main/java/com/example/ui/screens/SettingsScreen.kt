package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CyanArcPrimary
import com.example.ui.theme.HologramBlue
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RedAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    onNavigateToAiProviders: () -> Unit = {},
    onNavigateToPersonalization: () -> Unit = {},
    onNavigateToAiAgents: () -> Unit = {},
    onNavigateToWorkspaces: () -> Unit = {},
    onNavigateToSkills: () -> Unit = {},
    onNavigateToRoutines: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToUsage: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToResearch: () -> Unit = {},
    onNavigateToGitHubSkills: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val preferredLang by viewModel.preferredLanguage.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val speechPitch by viewModel.speechPitch.collectAsStateWithLifecycle()
    val autoSpeak by viewModel.autoSpeak.collectAsStateWithLifecycle()
    val isContinuousMode by viewModel.isContinuousMode.collectAsStateWithLifecycle()
    val animationEnabled by viewModel.animationEnabled.collectAsStateWithLifecycle()
    val animationIntensity by viewModel.animationIntensity.collectAsStateWithLifecycle()
    val reducedMotion by viewModel.reducedMotion.collectAsStateWithLifecycle()
    val activeAiConfig by viewModel.activeAiConfig.collectAsStateWithLifecycle()

    var editingName by remember(userName) { mutableStateOf(userName) }
    var currentRate by remember(speechRate) { mutableFloatStateOf(speechRate) }
    var currentPitch by remember(speechPitch) { mutableFloatStateOf(speechPitch) }
    var currentIntensity by remember(animationIntensity) { mutableFloatStateOf(animationIntensity) }
    var showClearConfirm by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = CyanArcPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "JARVIS সেটিংস",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTheme.config.primaryAccent,
                    letterSpacing = 1.sp
                )
            }

            // PERSONALIZATION STUDIO SHOWCASE CARD
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPersonalization() }
                    .testTag("settings_personalization_button"),
                glowColor = JarvisTheme.config.primaryAccent,
                backgroundColor = JarvisTheme.config.cardBg,
                cornerRadius = 14.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = JarvisTheme.config.primaryAccent.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = JarvisTheme.config.primaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PERSONALIZATION STUDIO",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisTheme.config.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = JarvisTheme.config.primaryAccent
                                ) {
                                    Text(
                                        text = "NEW",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JarvisTheme.config.backgroundColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "7 AI Orb অ্যানিমেশন স্টাইল, 8 লাক্সারি থিম ও ফুল কাস্টমাইজেশন",
                                fontSize = 11.sp,
                                color = JarvisTheme.config.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Studio",
                        tint = JarvisTheme.config.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // AI Model & Core Engine (BYOK Management)
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAiProviders() }
                    .testTag("ai_providers_settings_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = JarvisTheme.config.cardBg.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, JarvisTheme.config.borderGlow, RoundedCornerShape(12.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = JarvisTheme.config.primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "AI কোর ইঞ্জিন (BYOK)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisTheme.config.textPrimary
                                )
                                val isKeyOrConfigured = activeAiConfig.isConfigured || viewModel.hasSavedApiKey(activeAiConfig.id)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isKeyOrConfigured) NeonEmerald.copy(alpha = 0.2f) else AmberAlert.copy(alpha = 0.2f),
                                    modifier = Modifier.border(
                                        1.dp,
                                        if (isKeyOrConfigured) NeonEmerald.copy(alpha = 0.6f) else AmberAlert.copy(alpha = 0.6f),
                                        RoundedCornerShape(4.dp)
                                    )
                                ) {
                                    Text(
                                        text = if (isKeyOrConfigured) "সক্রিয়" else "কী প্রয়োজন",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isKeyOrConfigured) NeonEmerald else AmberAlert,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${activeAiConfig.name} • ${activeAiConfig.selectedModel}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JarvisTheme.config.primaryAccent,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gemini, OpenRouter বা Firebase AI কনফিগারেশন ও API কী পরিচালনা করুন",
                                fontSize = 11.sp,
                                color = JarvisTheme.config.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open AI Manager",
                        tint = JarvisTheme.config.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // --- 23-SYSTEM JARVIS CORE EXPANSION ---
            Text(
                text = "উন্নত সহকারী মডিউলসমূহ (JARVIS Core)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanArcPrimary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            // System 14: Multi-Agent Collaboration
            SettingsNavTile(
                title = "AI Agents সমবায় ইঞ্জিন",
                subtitle = "প্ল্যানিং, রিসার্চ, অ্যানালাইসিস ও কোডিং এজেন্টের যৌথ কাজ",
                icon = Icons.Default.Group,
                onClick = onNavigateToAiAgents,
                tag = "settings_tile_agents"
            )

            // System 18: Project Workspaces
            SettingsNavTile(
                title = "প্রজেক্ট ওয়ার্কস্পেস",
                subtitle = "পার্সোনাল, বিজনেস, কোডিং ও স্টাডির স্বাধীন কনটেক্সট",
                icon = Icons.Default.Work,
                onClick = onNavigateToWorkspaces,
                tag = "settings_tile_workspaces"
            )

            // System 15: AI Skills & Marketplace
            SettingsNavTile(
                title = "AI স্কিল ও প্লাগইন মার্কেটপ্লেস",
                subtitle = "ক্যালকুলেটর, অনুবাদ, কোডিং, ওয়েদার ও ফাইল স্কিল পরিচালনা",
                icon = Icons.Default.Extension,
                onClick = onNavigateToSkills,
                tag = "settings_tile_skills"
            )

            // Universal GitHub & Skill Installer
            SettingsNavTile(
                title = "GitHub রিপোজিটরি ও স্কিল ইনস্টলার",
                subtitle = "গিটহাব সার্চ, URL ইম্পোর্ট, নিরাপত্তা অডিট ও ডেভেলপার ওয়ার্কস্পেস",
                icon = Icons.Default.Download,
                onClick = onNavigateToGitHubSkills,
                tag = "settings_tile_github_skills"
            )

            // System 07: Automation & Routines
            SettingsNavTile(
                title = "স্মার্ট অটোমেশন ও রুটিন",
                subtitle = "শুভ সকাল, স্টাডি মোড ও কাস্টম ভয়েস রুটিন ট্রিগার",
                icon = Icons.Default.AutoMode,
                onClick = onNavigateToRoutines,
                tag = "settings_tile_routines"
            )

            // System 08 & 16: Research & Fact Checking
            SettingsNavTile(
                title = "লাইভ ওয়েব রিসার্চ ও ফ্যাক্ট চেক",
                subtitle = "ইন্টারনেট অনুসন্ধান, তথ্য যাচাইকরণ ও বিশ্বস্ততার লেবেল",
                icon = Icons.Default.Public,
                onClick = onNavigateToResearch,
                tag = "settings_tile_research"
            )

            // System 06: Notification Intelligence
            SettingsNavTile(
                title = "স্মার্ট নোটিফিকেশন ইন্টেলিজেন্স",
                subtitle = "স্ট্যাটাস বার নোটিফিকেশন পড়া, ভিআইপি ফিল্টার ও এআই ডাইজেস্ট",
                icon = Icons.Default.Notifications,
                onClick = onNavigateToNotifications,
                tag = "settings_tile_notifications"
            )

            // System 19: Self-Diagnostics & Safe Repair
            SettingsNavTile(
                title = "সেলফ-ডায়াগনস্টিক ও নিরাপদ মেরামত",
                subtitle = "এআই কি, ডাটাবেজ, নেটওয়ার্ক ও কিস্টোর স্বাস্থ্য নিরীক্ষা",
                icon = Icons.Default.Build,
                onClick = onNavigateToDiagnostics,
                tag = "settings_tile_diagnostics"
            )

            // System 21: Usage & Cost Intelligence
            SettingsNavTile(
                title = "টোকেন ব্যবহার ও খরচ বিশ্লেষণ",
                subtitle = "রিকোয়েস্ট সংখ্যা, টোকেন ব্যালেন্স ও আনুমানিক খরচ ট্র্যাকিং",
                icon = Icons.Default.QueryStats,
                onClick = onNavigateToUsage,
                tag = "settings_tile_usage"
            )

            // System 22: Encrypted Multi-Device Sync
            SettingsNavTile(
                title = "এনক্রিপ্টেড ব্যাকআপ ও ডিভাইস সিঙ্ক",
                subtitle = "পাসকি দিয়ে AES-256 এনক্রিপ্ট করে নোট ও মেমরি ব্যাকআপ/রিস্টোর",
                icon = Icons.Default.Sync,
                onClick = onNavigateToSync,
                tag = "settings_tile_sync"
            )

            // User Profile
            CyberCard {
                Text(
                    text = "ব্যবহারকারী প্রোফাইল",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "JARVIS আপনাকে কী নামে সম্বোধন করবে?",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanArcPrimary,
                            unfocusedBorderColor = JarvisSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = { viewModel.updateUserName(editingName.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanArcPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "সেভ", color = Color(0xFF00242E), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Language & Voice Settings
            CyberCard {
                Text(
                    text = "ভয়েস ও ভাষা কনফিগারেশন",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "পছন্দের ভাষা:", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("bn" to "বাংলা (bn-BD)", "en" to "English (en-US)").forEach { (code, label) ->
                        Surface(
                            onClick = { viewModel.updateLanguage(code) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (preferredLang == code) CyanArcPrimary else JarvisSurfaceVariant,
                            modifier = Modifier.border(1.dp, CyanArcPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = label,
                                color = if (preferredLang == code) Color(0xFF00242E) else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "ভয়েসে উত্তর দেওয়া (Auto-Speak)", fontSize = 14.sp, color = TextPrimary)
                        Text(text = "প্রতিটি উত্তরের পর মুখে বলে শোনাবে", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = { viewModel.updateAutoSpeak(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanArcPrimary, checkedTrackColor = HologramBlue)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "কথা বলার গতি (Speed): ${"%.1f".format(currentRate)}x",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Slider(
                    value = currentRate,
                    onValueChange = { currentRate = it },
                    onValueChangeFinished = { viewModel.updateSpeechRate(currentRate) },
                    valueRange = 0.7f..1.6f,
                    colors = SliderDefaults.colors(thumbColor = CyanArcPrimary, activeTrackColor = CyanArcPrimary)
                )

                Text(
                    text = "কথা বলার স্বর (Pitch): ${"%.1f".format(currentPitch)}x",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Slider(
                    value = currentPitch,
                    onValueChange = { currentPitch = it },
                    onValueChangeFinished = { viewModel.updateSpeechPitch(currentPitch) },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = HologramBlue, activeTrackColor = HologramBlue)
                )

                Button(
                    onClick = {
                        viewModel.speakResponse("হ্যালো $userName, এটি JARVIS এর ভয়েস টেস্ট।")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant)
                ) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = CyanArcPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "ভয়েস টেস্ট করুন", color = CyanArcPrimary)
                }
            }

            // Hey JARVIS Background Wake Word System
            val isWakeWordEnabled by viewModel.isWakeWordEnabled.collectAsStateWithLifecycle()
            val isWakeWordServiceRunning by viewModel.isWakeWordServiceRunning.collectAsStateWithLifecycle()

            CyberCard(
                glowColor = if (isWakeWordServiceRunning) NeonEmerald else JarvisTheme.config.borderGlow,
                backgroundColor = JarvisTheme.config.cardBg
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "\"Hey JARVIS\" ওয়েক ওয়ার্ড",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisTheme.config.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = if (isWakeWordServiceRunning) NeonEmerald else JarvisTheme.config.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(8.dp)
                            ) {}
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isWakeWordServiceRunning) "সক্রিয় — ব্যাকগ্রাউন্ডে শুনছে" else "অ্যাপের বাইরে থেকে চালু করার জন্য সার্ভিস চালু করুন",
                            fontSize = 11.sp,
                            color = if (isWakeWordServiceRunning) NeonEmerald else JarvisTheme.config.textSecondary
                        )
                    }

                    Switch(
                        checked = isWakeWordEnabled,
                        onCheckedChange = { viewModel.toggleWakeWord(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisTheme.config.backgroundColor,
                            checkedTrackColor = JarvisTheme.config.primaryAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test Wake Word simulation button
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.handleWakeWordTriggered() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisTheme.config.primaryAccent.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = JarvisTheme.config.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "টেস্ট ওয়েক ওয়ার্ড সিমুলেশন (\"Hey JARVIS\")",
                        fontSize = 12.sp,
                        color = JarvisTheme.config.primaryAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Android OS & Battery optimization note
                Surface(
                    color = JarvisTheme.config.surfaceColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "ℹ️ অ্যান্ড্রয়েড ব্যাকগ্রাউন্ড নীতি ও ব্যাটারি অপ্টিমাইজেশন:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "• ব্যাকগ্রাউন্ড ওয়েক ওয়ার্ড সক্রিয় থাকলে স্ট্যাটাস বারে স্থায়ী নোটিফিকেশন থাকবে।\n" +
                                   "• সম্পূর্ণ অন-ডিভাইস কি-ওয়ার্ড সনাক্তকরণ (কোনো অডিও ক্লাউডে রেকর্ড বা আপলোড হয় না)।\n" +
                                   "• নিরবচ্ছিন্ন সেবার জন্য ফোনের 'Battery Optimization' থেকে JARVIS কে 'Unrestricted' নির্বাচন করুন।",
                            fontSize = 10.sp,
                            color = JarvisTheme.config.textSecondary,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Visualizer & Animation Controls (Section 14)
            CyberCard {
                Text(
                    text = "ভিজ্যুয়ালাইজার ও মোশন সেটিংস",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "অ্যানিমেটেড ব্যাকগ্রাউন্ড", fontSize = 14.sp, color = TextPrimary)
                        Text(text = "Siri-অনুপ্রাণিত ফ্লুইড ব্যাকগ্রাউন্ড লাইভ ইফেক্ট", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = animationEnabled,
                        onCheckedChange = { viewModel.updateAnimationEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanArcPrimary, checkedTrackColor = HologramBlue)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "ব্যাটারি ফ্রেন্ডলি মোড (Reduced Motion)", fontSize = 14.sp, color = TextPrimary)
                        Text(text = "মোশন ও প্রসেসিং কমিয়ে ব্যাটারি সাশ্রয় করে", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = reducedMotion,
                        onCheckedChange = { viewModel.updateReducedMotion(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanArcPrimary, checkedTrackColor = HologramBlue)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "অ্যানিমেশন গতি (Speed): ${"%.1f".format(currentIntensity)}x",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Slider(
                    value = currentIntensity,
                    onValueChange = { currentIntensity = it },
                    onValueChangeFinished = { viewModel.updateAnimationIntensity(currentIntensity) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = CyanArcPrimary, activeTrackColor = CyanArcPrimary)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "হ্যান্ডস-ফ্রি মোড (Continuous Conversation)", fontSize = 14.sp, color = TextPrimary)
                        Text(text = "JARVIS কথা শেষ করলে স্বয়ংক্রিয়ভাবে আবার শুনবে", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = isContinuousMode,
                        onCheckedChange = { viewModel.toggleContinuousMode() },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonEmerald, checkedTrackColor = CyanArcPrimary)
                    )
                }
            }

            // Navigation to Privacy & About
            CyberCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onNavigateToPrivacy,
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonEmerald)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "অনুমতি ও প্রাইভেসি", fontSize = 14.sp, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onNavigateToAbout,
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = CyanArcPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "JARVIS পরিচিতি ও আর্কিটেকচার", fontSize = 14.sp, color = TextPrimary)
                        }
                    }
                }
            }

            // Danger Zone: Clear History
            CyberCard(borderColor = RedAlert.copy(alpha = 0.4f)) {
                Text(text = "ডেটা ম্যানেজমেন্ট", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RedAlert)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "সমস্ত লোকাল কথোপকথন ইতিহাস সম্পূর্ণ মুছে ফেলুন।",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "কথোপকথনের মেমোরি মুছুন", color = RedAlert, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text(text = "ইতিহাস মুছে ফেলতে চান?", color = TextPrimary) },
                text = { Text(text = "সব কথোপকথনের রেকর্ড ফোন থেকে মুছে যাবে।", color = TextSecondary) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearConversations()
                            showClearConfirm = false
                        }
                    ) {
                        Text(text = "মুছে ফেলুন", color = RedAlert, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text(text = "বাতিল", color = TextSecondary)
                    }
                },
                containerColor = JarvisSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsNavTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = JarvisTheme.config.cardBg.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.dp, JarvisTheme.config.borderGlow, RoundedCornerShape(10.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = JarvisTheme.config.primaryAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

