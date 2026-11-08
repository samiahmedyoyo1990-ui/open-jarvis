package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.theme.BackgroundStylePreset
import com.example.data.theme.OrbAnimationPreset
import com.example.data.theme.PresetThemes
import com.example.data.theme.ThemeConfig
import com.example.data.theme.TypographyPreset
import com.example.ui.JarvisViewModel
import com.example.ui.components.AssistantState
import com.example.ui.components.CyberCard
import com.example.ui.components.JarvisArcOrb
import com.example.ui.components.SiriFluidVisualizerBackground
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RedAlert
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationStudioScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentSavedTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val customProfiles by viewModel.customProfiles.collectAsStateWithLifecycle()

    // Working copy for live editing
    var draftTheme by remember(currentSavedTheme) { mutableStateOf(currentSavedTheme) }

    // Live preview interaction states
    var previewState by remember { mutableStateOf(AssistantState.IDLE) }
    var previewAmplitude by remember { mutableFloatStateOf(0.2f) }

    // Navigation Studio Tabs: 0: PRESETS, 1: AI ORB, 2: COLORS, 3: ATMOSPHERE, 4: UI/TYPE, 5: PROFILES
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Presets", "AI Orb", "Colors", "Atmosphere", "Layout", "Profiles")

    // Dialogs
    var showSaveProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var showExportJsonDialog by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(draftTheme.backgroundColor)
    ) {
        // Dynamic Atmospheric Background Preview
        SiriFluidVisualizerBackground(
            state = previewState,
            amplitude = previewAmplitude,
            theme = draftTheme
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("personalization_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = draftTheme.primaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PERSONALIZATION",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = draftTheme.primaryAccent,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = draftTheme.secondaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "AI Orb & Luxury Visual Studio",
                            fontSize = 11.sp,
                            color = draftTheme.textSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Defaults",
                            tint = draftTheme.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Save / Apply Button
                    Button(
                        onClick = {
                            viewModel.applyTheme(draftTheme)
                            Toast.makeText(context, "থিম সফলভাবে প্রয়োগ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = draftTheme.primaryAccent,
                            contentColor = draftTheme.backgroundColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("apply_theme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Apply",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // SCROLLABLE CONTENT BODY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                // 1. INTERACTIVE LIVE ORB & THEME PREVIEW CARD
                CyberCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    glowColor = draftTheme.borderGlow,
                    backgroundColor = draftTheme.cardBg,
                    cornerRadius = draftTheme.cardCornerRadiusDp.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE INTERACTIVE PREVIEW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = draftTheme.primaryAccent,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = draftTheme.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = draftTheme.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live AI Orb with active draftTheme
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            JarvisArcOrb(
                                state = previewState,
                                amplitude = previewAmplitude,
                                size = draftTheme.orbSizeDp.dp,
                                theme = draftTheme
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Test State Chips (Simulates real assistant interaction states)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                AssistantState.IDLE to "Idle",
                                AssistantState.LISTENING to "Listening",
                                AssistantState.THINKING to "Thinking",
                                AssistantState.SPEAKING to "Speaking",
                                AssistantState.ERROR to "Alert"
                            ).forEach { (state, label) ->
                                val isSelected = previewState == state
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { previewState = state },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = draftTheme.primaryAccent.copy(alpha = 0.25f),
                                        selectedLabelColor = draftTheme.primaryAccent,
                                        containerColor = draftTheme.surfaceColor.copy(alpha = 0.6f),
                                        labelColor = draftTheme.textSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = draftTheme.borderGlow,
                                        selectedBorderColor = draftTheme.primaryAccent
                                    )
                                )
                            }
                        }

                        // Simulated Voice Waveform Amplitude Slider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pulse: ${(previewAmplitude * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = draftTheme.textSecondary,
                                modifier = Modifier.width(76.dp)
                            )
                            Slider(
                                value = previewAmplitude,
                                onValueChange = { previewAmplitude = it },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = draftTheme.primaryAccent,
                                    activeTrackColor = draftTheme.primaryAccent,
                                    inactiveTrackColor = draftTheme.surfaceColor
                                )
                            )
                        }
                    }
                }

                // 2. STUDIO TABS (Presets, AI Orb, Colors, Atmosphere, Layout, Profiles)
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = draftTheme.primaryAccent,
                    edgePadding = 14.dp,
                    divider = {},
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) draftTheme.primaryAccent else draftTheme.textSecondary
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. TAB CONTENT
                when (selectedTab) {
                    0 -> PresetsTab(
                        currentTheme = draftTheme,
                        onSelectPreset = { preset ->
                            draftTheme = preset
                        }
                    )
                    1 -> OrbStudioTab(
                        theme = draftTheme,
                        onUpdateTheme = { draftTheme = it }
                    )
                    2 -> ColorsTab(
                        theme = draftTheme,
                        onUpdateTheme = { draftTheme = it }
                    )
                    3 -> AtmosphereTab(
                        theme = draftTheme,
                        onUpdateTheme = { draftTheme = it }
                    )
                    4 -> LayoutAndTypeTab(
                        theme = draftTheme,
                        onUpdateTheme = { draftTheme = it }
                    )
                    5 -> ProfilesTab(
                        currentTheme = draftTheme,
                        savedProfiles = customProfiles,
                        onSelectProfile = { draftTheme = it },
                        onSaveNewProfile = { showSaveProfileDialog = true },
                        onDeleteProfile = { viewModel.deleteProfile(it.id) },
                        onExportJson = { showExportJsonDialog = true },
                        onImportJson = { showImportJsonDialog = true }
                    )
                }
            }
        }
    }

    // DIALOG: Save Custom Profile
    if (showSaveProfileDialog) {
        AlertDialog(
            onDismissRequest = { showSaveProfileDialog = false },
            title = {
                Text(
                    text = "সেভ কাস্টম প্রোফাইল",
                    color = draftTheme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "আপনার তৈরি থিমটি একটি নাম দিয়ে সংরক্ষণ করুন:",
                        fontSize = 13.sp,
                        color = draftTheme.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        placeholder = { Text("যেমন: Stark Obsidian HUD") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = draftTheme.textPrimary,
                            unfocusedTextColor = draftTheme.textPrimary,
                            focusedBorderColor = draftTheme.primaryAccent,
                            unfocusedBorderColor = draftTheme.borderGlow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newProfileName.trim().ifEmpty { "Custom Theme" }
                        val newProfile = draftTheme.copy(
                            id = "profile_${UUID.randomUUID().toString().take(8)}",
                            name = name
                        )
                        viewModel.saveCustomProfile(newProfile)
                        draftTheme = newProfile
                        newProfileName = ""
                        showSaveProfileDialog = false
                        Toast.makeText(context, "প্রোফাইল '$name' সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = draftTheme.primaryAccent)
                ) {
                    Text("Save Profile", color = draftTheme.backgroundColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveProfileDialog = false }) {
                    Text("Cancel", color = draftTheme.textSecondary)
                }
            },
            containerColor = draftTheme.surfaceColor
        )
    }

    // DIALOG: Export JSON
    if (showExportJsonDialog) {
        val jsonString = remember(draftTheme) { draftTheme.toJsonString() }
        AlertDialog(
            onDismissRequest = { showExportJsonDialog = false },
            title = {
                Text(
                    text = "JSON থিম এক্সপোর্ট",
                    color = draftTheme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "নিচের JSON কোডটি কপি করে অন্য ডিভাইসে বা বন্ধুদের সাথে শেয়ার করতে পারেন:",
                        fontSize = 12.sp,
                        color = draftTheme.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .verticalScroll(rememberScrollState()),
                        color = draftTheme.backgroundColor,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, draftTheme.borderGlow)
                    ) {
                        Text(
                            text = jsonString,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = draftTheme.primaryAccent,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(jsonString))
                        Toast.makeText(context, "JSON ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                        showExportJsonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = draftTheme.primaryAccent)
                ) {
                    Text("Copy to Clipboard", color = draftTheme.backgroundColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportJsonDialog = false }) {
                    Text("Close", color = draftTheme.textSecondary)
                }
            },
            containerColor = draftTheme.surfaceColor
        )
    }

    // DIALOG: Import JSON
    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = {
                Text(
                    text = "JSON থিম ইম্পোর্ট",
                    color = draftTheme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "এখানে JARVIS থিম JSON কোড পেস্ট করুন:",
                        fontSize = 12.sp,
                        color = draftTheme.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("{\"id\":\"custom\",\"name\":\"...\"}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = draftTheme.textPrimary,
                            unfocusedTextColor = draftTheme.textPrimary,
                            focusedBorderColor = draftTheme.primaryAccent,
                            unfocusedBorderColor = draftTheme.borderGlow
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            val parsed = ThemeConfig.fromJson(importJsonText)
                            draftTheme = parsed
                            showImportJsonDialog = false
                            importJsonText = ""
                            Toast.makeText(context, "থিম '${parsed.name}' সফলভাবে লোড হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = draftTheme.primaryAccent)
                ) {
                    Text("Load Theme", color = draftTheme.backgroundColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) {
                    Text("Cancel", color = draftTheme.textSecondary)
                }
            },
            containerColor = draftTheme.surfaceColor
        )
    }

    // DIALOG: Reset Defaults Confirmation
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "ডিফল্ট থিমে রিসেট করবেন?",
                    color = draftTheme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "আপনার সমস্ত কাস্টমাইজেশন রিসেট হয়ে JARVIS Signature থিমে ফিরে যাবে।",
                    fontSize = 13.sp,
                    color = draftTheme.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        draftTheme = PresetThemes.JARVIS_SIGNATURE
                        viewModel.resetTheme()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "ডিফল্ট থিমে রিসেট সম্পন্ন হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text("Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = draftTheme.textSecondary)
                }
            },
            containerColor = draftTheme.surfaceColor
        )
    }
}

// -------------------------------------------------------------
// TAB 1: 8 LUXURY PRESET THEMES CAROUSEL & GRID
// -------------------------------------------------------------
@Composable
private fun PresetsTab(
    currentTheme: ThemeConfig,
    onSelectPreset: (ThemeConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "8 LUXURY MASTER PRESETS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = currentTheme.primaryAccent,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )

        PresetThemes.ALL_PRESETS.forEach { preset ->
            val isSelected = currentTheme.id == preset.id
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPreset(preset) },
                glowColor = if (isSelected) preset.primaryAccent else preset.borderGlow,
                backgroundColor = preset.cardBg,
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Color Palette Swatch Trio
                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(preset.primaryAccent)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(preset.secondaryAccent)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(preset.surfaceColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                        }

                        Column {
                            Text(
                                text = preset.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = preset.textPrimary
                            )
                            Text(
                                text = "Orb: ${preset.orbStyle.displayName} | Bg: ${preset.backgroundColorHex}",
                                fontSize = 11.sp,
                                color = preset.textSecondary
                            )
                        }
                    }

                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = preset.primaryAccent,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = preset.backgroundColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelectPreset(preset) },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, preset.primaryAccent.copy(alpha = 0.5f))
                        ) {
                            Text("Select", fontSize = 11.sp, color = preset.primaryAccent)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: AI ORB STUDIO (7 PRESETS + SLIDERS)
// -------------------------------------------------------------
@Composable
private fun OrbStudioTab(
    theme: ThemeConfig,
    onUpdateTheme: (ThemeConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "AI ORB ENGINE (7 DISTINCT STYLES)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = theme.primaryAccent,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )

        // 7 Orb Styles List
        OrbAnimationPreset.values().forEach { style ->
            val isSelected = theme.orbStyle == style
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdateTheme(theme.copy(orbStyle = style)) },
                glowColor = if (isSelected) theme.primaryAccent else theme.borderGlow,
                backgroundColor = if (isSelected) theme.surfaceColor else theme.cardBg,
                cornerRadius = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = style.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) theme.primaryAccent else theme.textPrimary
                        )
                        Text(
                            text = style.description,
                            fontSize = 11.sp,
                            color = theme.textSecondary
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = theme.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Orb Dynamics Sliders
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = theme.borderGlow,
            backgroundColor = theme.cardBg,
            cornerRadius = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ORB PARAMETERS & DYNAMICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryAccent,
                    letterSpacing = 1.2.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Orb Size Slider (120dp to 240dp)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Orb Dimension", fontSize = 12.sp, color = theme.textPrimary)
                        Text("${theme.orbSizeDp} dp", fontSize = 12.sp, color = theme.primaryAccent)
                    }
                    Slider(
                        value = theme.orbSizeDp.toFloat(),
                        onValueChange = { onUpdateTheme(theme.copy(orbSizeDp = it.toInt())) },
                        valueRange = 120f..240f,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.primaryAccent,
                            activeTrackColor = theme.primaryAccent,
                            inactiveTrackColor = theme.surfaceColor
                        )
                    )
                }

                // Glow Intensity Slider (0.2 to 2.5)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Glow & Bloom Intensity", fontSize = 12.sp, color = theme.textPrimary)
                        Text("${String.format("%.1f", theme.orbGlowIntensity)}x", fontSize = 12.sp, color = theme.primaryAccent)
                    }
                    Slider(
                        value = theme.orbGlowIntensity,
                        onValueChange = { onUpdateTheme(theme.copy(orbGlowIntensity = it)) },
                        valueRange = 0.2f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.primaryAccent,
                            activeTrackColor = theme.primaryAccent,
                            inactiveTrackColor = theme.surfaceColor
                        )
                    )
                }

                // Animation Speed Multiplier (0.5 to 2.5)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rotation & Pulse Speed", fontSize = 12.sp, color = theme.textPrimary)
                        Text("${String.format("%.1f", theme.animationSpeedMultiplier)}x", fontSize = 12.sp, color = theme.primaryAccent)
                    }
                    Slider(
                        value = theme.animationSpeedMultiplier,
                        onValueChange = { onUpdateTheme(theme.copy(animationSpeedMultiplier = it)) },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.primaryAccent,
                            activeTrackColor = theme.primaryAccent,
                            inactiveTrackColor = theme.surfaceColor
                        )
                    )
                }

                // Particle Density (0.0 to 2.5)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Starlight Particle Density", fontSize = 12.sp, color = theme.textPrimary)
                        Text("${String.format("%.1f", theme.particleIntensity)}x", fontSize = 12.sp, color = theme.primaryAccent)
                    }
                    Slider(
                        value = theme.particleIntensity,
                        onValueChange = { onUpdateTheme(theme.copy(particleIntensity = it)) },
                        valueRange = 0.0f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.primaryAccent,
                            activeTrackColor = theme.primaryAccent,
                            inactiveTrackColor = theme.surfaceColor
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: COLORS & PALETTE CUSTOMIZER
// -------------------------------------------------------------
@Composable
private fun ColorsTab(
    theme: ThemeConfig,
    onUpdateTheme: (ThemeConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "CHROMATIC COLOR ENGINE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = theme.primaryAccent,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )

        // Palette Preset Swatches
        val popularAccents = listOf(
            "#00D9FF" to "Electric Cyan",
            "#00FFE0" to "Cyber Aqua",
            "#7C4DFF" to "Quantum Violet",
            "#E040FB" to "Neon Magenta",
            "#FFB300" to "Stark Gold",
            "#00E676" to "Neural Emerald",
            "#FF3D00" to "Fusion Orange",
            "#F5F7FA" to "Pure White"
        )

        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = theme.borderGlow,
            backgroundColor = theme.cardBg,
            cornerRadius = 14.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Quick Primary Accent Swatches",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    popularAccents.forEach { (hex, name) ->
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Cyan }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    onUpdateTheme(
                                        theme.copy(
                                            primaryAccentHex = hex,
                                            borderGlowHex = "#40" + hex.replace("#", "")
                                        )
                                    )
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        2.dp,
                                        if (theme.primaryAccentHex.equals(hex, ignoreCase = true)) Color.White else Color.Transparent,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(name.split(" ").last(), fontSize = 9.sp, color = theme.textSecondary)
                        }
                    }
                }
            }
        }

        // Color HEX Input Fields
        ColorHexEditor(
            label = "Primary Accent",
            description = "Orb core, active highlights, key action glow",
            currentHex = theme.primaryAccentHex,
            onHexChange = { onUpdateTheme(theme.copy(primaryAccentHex = it, borderGlowHex = "#40" + it.replace("#", ""))) },
            theme = theme
        )

        ColorHexEditor(
            label = "Secondary Accent",
            description = "Orb energy layer 2, gradients, secondary indicators",
            currentHex = theme.secondaryAccentHex,
            onHexChange = { onUpdateTheme(theme.copy(secondaryAccentHex = it)) },
            theme = theme
        )

        ColorHexEditor(
            label = "Tertiary Accent",
            description = "Highlight sparkles, soundwave resonance",
            currentHex = theme.tertiaryAccentHex,
            onHexChange = { onUpdateTheme(theme.copy(tertiaryAccentHex = it)) },
            theme = theme
        )

        ColorHexEditor(
            label = "Background Color",
            description = "Deep AMOLED canvas (#070A12, #000000, #0B0314)",
            currentHex = theme.backgroundColorHex,
            onHexChange = { onUpdateTheme(theme.copy(backgroundColorHex = it)) },
            theme = theme
        )

        ColorHexEditor(
            label = "Card & Glass Surface",
            description = "HUD container background (#111A2D, #1C1712)",
            currentHex = theme.cardBgHex,
            onHexChange = { onUpdateTheme(theme.copy(cardBgHex = it)) },
            theme = theme
        )
    }
}

@Composable
private fun ColorHexEditor(
    label: String,
    description: String,
    currentHex: String,
    onHexChange: (String) -> Unit,
    theme: ThemeConfig
) {
    var textValue by remember(currentHex) { mutableStateOf(currentHex) }
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(textValue.trim()))
    } catch (e: Exception) {
        Color.Gray
    }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = theme.borderGlow,
        backgroundColor = theme.cardBg,
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(parsedColor)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                )
                Column {
                    Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    Text(text = description, fontSize = 10.sp, color = theme.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input
                    if (input.startsWith("#") && (input.length == 7 || input.length == 9)) {
                        try {
                            android.graphics.Color.parseColor(input)
                            onHexChange(input)
                        } catch (e: Exception) {
                            // wait for full hex
                        }
                    }
                },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary,
                    focusedBorderColor = theme.primaryAccent,
                    unfocusedBorderColor = theme.borderGlow
                )
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 4: ATMOSPHERE & BACKGROUND STYLES
// -------------------------------------------------------------
@Composable
private fun AtmosphereTab(
    theme: ThemeConfig,
    onUpdateTheme: (ThemeConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "ATMOSPHERIC CANVAS & VISUALIZER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = theme.primaryAccent,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )

        // Background Style Presets
        BackgroundStylePreset.values().forEach { bgStyle ->
            val isSelected = theme.backgroundStyle == bgStyle
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdateTheme(theme.copy(backgroundStyle = bgStyle)) },
                glowColor = if (isSelected) theme.primaryAccent else theme.borderGlow,
                backgroundColor = if (isSelected) theme.surfaceColor else theme.cardBg,
                cornerRadius = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bgStyle.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) theme.primaryAccent else theme.textPrimary
                        )
                        Text(
                            text = when (bgStyle) {
                                BackgroundStylePreset.FLUID_MESH -> "অর্গানিক বায়ুমণ্ডলীয় এনার্জি মেশ যা ভয়েসের সাথে পালস করে"
                                BackgroundStylePreset.GRADIENT -> "মসৃণ ডায়নামিক লিনিয়ার গ্রেডিয়েন্ট ক্যানভাস"
                                BackgroundStylePreset.SOLID -> "ব্যাটারি সেভিং পিওর ম্যাট ডার্ক ব্যাকগ্রাউন্ড"
                            },
                            fontSize = 11.sp,
                            color = theme.textSecondary
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = theme.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Ambient Floating Particles Switch
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = theme.borderGlow,
            backgroundColor = theme.cardBg,
            cornerRadius = 14.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ambient Floating Particles",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "মহাজাগতিক ভাসমান নক্ষত্র কণা ব্যাকগ্রাউন্ডে প্রদর্শন করবে",
                        fontSize = 11.sp,
                        color = theme.textSecondary
                    )
                }
                Switch(
                    checked = theme.ambientParticlesEnabled,
                    onCheckedChange = { onUpdateTheme(theme.copy(ambientParticlesEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = theme.backgroundColor,
                        checkedTrackColor = theme.primaryAccent
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 5: LAYOUT & TYPOGRAPHY
// -------------------------------------------------------------
@Composable
private fun LayoutAndTypeTab(
    theme: ThemeConfig,
    onUpdateTheme: (ThemeConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "HUD GEOMETRY & TYPOGRAPHY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = theme.primaryAccent,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )

        // Card Corner Radius Slider
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = theme.borderGlow,
            backgroundColor = theme.cardBg,
            cornerRadius = theme.cardCornerRadiusDp.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Card Corner Curvature", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    Text("${theme.cardCornerRadiusDp} dp", fontSize = 13.sp, color = theme.primaryAccent)
                }
                Text("HUD কার্ড এবং ডায়ালগের কোণার রাউন্ডনেস", fontSize = 11.sp, color = theme.textSecondary)
                Slider(
                    value = theme.cardCornerRadiusDp.toFloat(),
                    onValueChange = { onUpdateTheme(theme.copy(cardCornerRadiusDp = it.toInt())) },
                    valueRange = 8f..28f,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.primaryAccent,
                        activeTrackColor = theme.primaryAccent,
                        inactiveTrackColor = theme.surfaceColor
                    )
                )
            }
        }

        // Typography Presets
        TypographyPreset.values().forEach { typePreset ->
            val isSelected = theme.typographyPreset == typePreset
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdateTheme(theme.copy(typographyPreset = typePreset)) },
                glowColor = if (isSelected) theme.primaryAccent else theme.borderGlow,
                backgroundColor = if (isSelected) theme.surfaceColor else theme.cardBg,
                cornerRadius = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = typePreset.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = when (typePreset) {
                                TypographyPreset.MONOSPACE -> FontFamily.Monospace
                                TypographyPreset.SANS_SERIF -> FontFamily.SansSerif
                                TypographyPreset.DEFAULT -> FontFamily.Default
                            },
                            color = if (isSelected) theme.primaryAccent else theme.textPrimary
                        )
                        Text(
                            text = when (typePreset) {
                                TypographyPreset.DEFAULT -> "আধুনিক মেটেরিয়াল ৩ টাইপোগ্রাফি ব্যালেন্স"
                                TypographyPreset.MONOSPACE -> "সাই-ফাই স্টার্ক এইচইউডি টার্মিনাল ফন্ট"
                                TypographyPreset.SANS_SERIF -> "মিনিমালিস্ট ও ক্রিস্প রিডাবিলিটি"
                            },
                            fontSize = 11.sp,
                            color = theme.textSecondary
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = theme.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Font Scaling Slider
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = theme.borderGlow,
            backgroundColor = theme.cardBg,
            cornerRadius = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Font Size Scaling", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    Text("${String.format("%.2f", theme.fontScale)}x", fontSize = 13.sp, color = theme.primaryAccent)
                }
                Text("টেক্সট ডিসপ্লে সাইজ সামঞ্জস্য করুন", fontSize = 11.sp, color = theme.textSecondary)
                Slider(
                    value = theme.fontScale,
                    onValueChange = { onUpdateTheme(theme.copy(fontScale = it)) },
                    valueRange = 0.85f..1.25f,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.primaryAccent,
                        activeTrackColor = theme.primaryAccent,
                        inactiveTrackColor = theme.surfaceColor
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 6: PROFILES & JSON CLOUD SYNC
// -------------------------------------------------------------
@Composable
private fun ProfilesTab(
    currentTheme: ThemeConfig,
    savedProfiles: List<ThemeConfig>,
    onSelectProfile: (ThemeConfig) -> Unit,
    onSaveNewProfile: () -> Unit,
    onDeleteProfile: (ThemeConfig) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "CUSTOM PROFILES & JSON PORTABILITY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = currentTheme.primaryAccent,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )

        // Actions: Save Profile, Export, Import
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSaveNewProfile,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryAccent)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = currentTheme.backgroundColor)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Profile", fontSize = 11.sp, color = currentTheme.backgroundColor, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onExportJson,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.borderGlow)
            ) {
                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = currentTheme.primaryAccent)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", fontSize = 11.sp, color = currentTheme.primaryAccent)
            }

            OutlinedButton(
                onClick = onImportJson,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.borderGlow)
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = currentTheme.secondaryAccent)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import", fontSize = 11.sp, color = currentTheme.secondaryAccent)
            }
        }

        // Saved Custom Profiles List
        if (savedProfiles.isEmpty()) {
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = currentTheme.borderGlow,
                backgroundColor = currentTheme.cardBg,
                cornerRadius = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = currentTheme.textSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "কোনো কাস্টম প্রোফাইল সংরক্ষিত নেই",
                        fontSize = 13.sp,
                        color = currentTheme.textSecondary
                    )
                    Text(
                        text = "আপনার পছন্দমতো রং ও Orb সাজিয়ে 'Save Profile' চাপুন",
                        fontSize = 11.sp,
                        color = currentTheme.textSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            savedProfiles.forEach { profile ->
                val isSelected = currentTheme.id == profile.id
                CyberCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProfile(profile) },
                    glowColor = if (isSelected) profile.primaryAccent else profile.borderGlow,
                    backgroundColor = profile.cardBg,
                    cornerRadius = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(profile.primaryAccent)
                            )
                            Column {
                                Text(
                                    text = profile.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = profile.textPrimary
                                )
                                Text(
                                    text = "Orb: ${profile.orbStyle.displayName}",
                                    fontSize = 11.sp,
                                    color = profile.textSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Text("Active", fontSize = 11.sp, color = profile.primaryAccent, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            IconButton(
                                onClick = { onDeleteProfile(profile) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = RedAlert.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
