package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.model.AiModelInfo
import com.example.ai.model.AiProviderConfig
import com.example.ai.model.AiProviderType
import com.example.ai.model.ConnectionTestResult
import com.example.ai.security.SecureApiKeyStorage
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RedAlert
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProviderManagerScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val allConfigs by viewModel.allAiConfigs.collectAsStateWithLifecycle()
    val activeConfig by viewModel.activeAiConfig.collectAsStateWithLifecycle()
    val backupConfig by viewModel.backupAiConfig.collectAsStateWithLifecycle()
    val isBackupEnabled by viewModel.isAiBackupEnabled.collectAsStateWithLifecycle()

    // Testing state
    var isTestingActive by remember { mutableStateOf(false) }
    var activeTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }

    // Dialog state for adding/editing provider
    var showEditDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<AiProviderConfig?>(null) }

    // Delete confirmation dialog
    var configToDelete by remember { mutableStateOf<AiProviderConfig?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisTheme.config.backgroundColor)
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
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("ai_providers_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = JarvisTheme.config.primaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "UNIVERSAL AI ENGINE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisTheme.config.primaryAccent,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = JarvisTheme.config.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "BYOK (Bring Your Own Key) Management",
                            fontSize = 11.sp,
                            color = JarvisTheme.config.textSecondary
                        )
                    }
                }

                // Add Provider Button
                Button(
                    onClick = {
                        editingConfig = AiProviderConfig(
                            id = "provider_${UUID.randomUUID().toString().take(8)}",
                            type = AiProviderType.GOOGLE_GEMINI,
                            name = "My Custom Gemini Key"
                        )
                        showEditDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.primaryAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_provider_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = JarvisTheme.config.backgroundColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisTheme.config.backgroundColor
                    )
                }
            }

            // 1. ACTIVE AI PROVIDER HERO CARD
            CyberCard(
                glowColor = JarvisTheme.config.primaryAccent,
                backgroundColor = JarvisTheme.config.cardBg,
                cornerRadius = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE AI ENGINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (viewModel.hasSavedApiKey(activeConfig.id) || activeConfig.type == AiProviderType.FIREBASE_AI) {
                                NeonEmerald.copy(alpha = 0.2f)
                            } else {
                                RedAlert.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = if (viewModel.hasSavedApiKey(activeConfig.id) || activeConfig.type == AiProviderType.FIREBASE_AI) "ACTIVE & KEY SET" else "KEY MISSING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.hasSavedApiKey(activeConfig.id) || activeConfig.type == AiProviderType.FIREBASE_AI) NeonEmerald else RedAlert,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activeConfig.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisTheme.config.textPrimary
                    )

                    Text(
                        text = "Selected Model: ${activeConfig.selectedModel} (${activeConfig.type.displayName})",
                        fontSize = 12.sp,
                        color = JarvisTheme.config.textSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTestingActive = true
                                activeTestResult = null
                                coroutineScope.launch {
                                    val result = viewModel.testAiConnection(activeConfig.id)
                                    activeTestResult = result
                                    isTestingActive = false
                                }
                            },
                            enabled = !isTestingActive,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisTheme.config.primaryAccent.copy(alpha = 0.6f))
                        ) {
                            if (isTestingActive) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = JarvisTheme.config.primaryAccent,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 11.sp, color = JarvisTheme.config.primaryAccent)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = JarvisTheme.config.primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection", fontSize = 11.sp, color = JarvisTheme.config.primaryAccent)
                            }
                        }

                        Button(
                            onClick = {
                                editingConfig = activeConfig
                                showEditDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.surfaceColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = JarvisTheme.config.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Configure", fontSize = 11.sp, color = JarvisTheme.config.textPrimary)
                        }
                    }

                    // Active test result alert box
                    if (activeTestResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = if (activeTestResult!!.isSuccess) NeonEmerald.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeTestResult!!.message,
                                fontSize = 11.sp,
                                color = if (activeTestResult!!.isSuccess) NeonEmerald else RedAlert,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // 2. BACKUP FAILOVER ENGINE CARD
            CyberCard(
                glowColor = JarvisTheme.config.borderGlow,
                backgroundColor = JarvisTheme.config.cardBg,
                cornerRadius = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AUTOMATIC BACKUP FAILOVER",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisTheme.config.textPrimary
                            )
                            Text(
                                text = "মূল ইঞ্জিনের কোটা বা এরর হলে স্বয়ংক্রিয় ব্যাকআপ ইঞ্জিনে সুইচ করবে",
                                fontSize = 11.sp,
                                color = JarvisTheme.config.textSecondary
                            )
                        }
                        Switch(
                            checked = isBackupEnabled,
                            onCheckedChange = { viewModel.setAiBackupEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisTheme.config.backgroundColor,
                                checkedTrackColor = JarvisTheme.config.primaryAccent
                            )
                        )
                    }

                    if (isBackupEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ব্যাকআপ ইঞ্জিন নির্বাচন করুন:",
                            fontSize = 11.sp,
                            color = JarvisTheme.config.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allConfigs.filter { it.id != activeConfig.id }.forEach { config ->
                                val isSelected = backupConfig?.id == config.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setBackupAiProvider(config.id) },
                                    label = { Text(config.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = JarvisTheme.config.primaryAccent.copy(alpha = 0.25f),
                                        selectedLabelColor = JarvisTheme.config.primaryAccent,
                                        containerColor = JarvisTheme.config.surfaceColor,
                                        labelColor = JarvisTheme.config.textSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. CONFIGURED PROVIDERS DIRECTORY
            Text(
                text = "CONFIGURED PROVIDERS (${allConfigs.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = JarvisTheme.config.primaryAccent,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.Monospace
            )

            allConfigs.forEach { config ->
                val isActive = config.id == activeConfig.id
                val hasKey = viewModel.hasSavedApiKey(config.id) || config.type == AiProviderType.FIREBASE_AI
                val maskedKey = viewModel.getSavedApiKey(config.id)?.let { SecureApiKeyStorage.maskKey(it) } ?: "No Key Configured"

                CyberCard(
                    glowColor = if (isActive) JarvisTheme.config.primaryAccent else JarvisTheme.config.borderGlow,
                    backgroundColor = JarvisTheme.config.cardBg,
                    cornerRadius = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (config.type) {
                                        AiProviderType.GOOGLE_GEMINI -> Color(0xFF1A73E8)
                                        AiProviderType.OPEN_ROUTER -> Color(0xFF6B46C1)
                                        AiProviderType.FIREBASE_AI -> Color(0xFFFF9100)
                                    },
                                    modifier = Modifier.size(10.dp)
                                ) {}

                                Column {
                                    Text(
                                        text = config.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JarvisTheme.config.textPrimary
                                    )
                                    Text(
                                        text = "${config.type.displayName} • ${config.selectedModel}",
                                        fontSize = 11.sp,
                                        color = JarvisTheme.config.textSecondary
                                    )
                                }
                            }

                            if (isActive) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = JarvisTheme.config.primaryAccent
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JarvisTheme.config.backgroundColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.setActiveAiProvider(config.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisTheme.config.primaryAccent.copy(alpha = 0.5f))
                                ) {
                                    Text("Set Active", fontSize = 11.sp, color = JarvisTheme.config.primaryAccent)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Key Status & Action row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (hasKey) NeonEmerald else JarvisTheme.config.textSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (config.type == AiProviderType.FIREBASE_AI) "App Check Cloud Token" else maskedKey,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (hasKey) JarvisTheme.config.textSecondary else RedAlert
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        editingConfig = config
                                        showEditDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = JarvisTheme.config.primaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (config.type != AiProviderType.FIREBASE_AI && allConfigs.size > 1) {
                                    IconButton(
                                        onClick = { configToDelete = config },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = RedAlert.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. SECURITY & FREE API KEY INSTRUCTION BANNER
            Surface(
                color = JarvisTheme.config.surfaceColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisTheme.config.borderGlow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "হার্ডওয়্যার Keystore নিরাপত্তা ও ফ্রি API Key গাইড",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• সমস্ত API Key ডিভাইসের Android Keystore AES-256 GCM চিপ দ্বারা এনক্রিপ্ট হয়ে সংরক্ষিত থাকে।\n" +
                               "• গুগল জেমিনির সম্পূর্ণ ফ্রি API Key পাওয়ার ওয়েবসাইট: aistudio.google.com\n" +
                               "• OpenRouter এর মাল্টি-মডেল কি পাওয়ার ওয়েবসাইট: openrouter.ai/keys\n" +
                               "• একাধিক প্রোফাইল যোগ করে ইচ্ছামতো যেকোনো সময় সুইচ করতে পারেন।",
                        fontSize = 10.sp,
                        color = JarvisTheme.config.textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    // DIALOG: ADD / EDIT AI PROVIDER CONFIG
    if (showEditDialog && editingConfig != null) {
        var currentEditType by remember { mutableStateOf(editingConfig!!.type) }
        var profileName by remember { mutableStateOf(editingConfig!!.name) }
        var inputApiKey by remember { mutableStateOf(viewModel.getSavedApiKey(editingConfig!!.id).orEmpty()) }
        var baseUrl by remember { mutableStateOf(editingConfig!!.baseUrl) }
        var selectedModel by remember { mutableStateOf(editingConfig!!.selectedModel) }
        var temperature by remember { mutableFloatStateOf(editingConfig!!.temperature) }
        var showPassword by remember { mutableStateOf(false) }

        var isFetchingModels by remember { mutableStateOf(false) }
        var fetchedModels by remember { mutableStateOf<List<AiModelInfo>>(emptyList()) }
        var modelsExpanded by remember { mutableStateOf(false) }

        var isTestingInDialog by remember { mutableStateOf(false) }
        var testResultInDialog by remember { mutableStateOf<ConnectionTestResult?>(null) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "AI প্রোভাইডার কনফিগারেশন (BYOK)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTheme.config.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Provider Type Selector
                    Text(text = "প্রোভাইডার টাইপ:", fontSize = 11.sp, color = JarvisTheme.config.textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(AiProviderType.GOOGLE_GEMINI, AiProviderType.OPEN_ROUTER).forEach { type ->
                            val isSelected = currentEditType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    currentEditType = type
                                    baseUrl = type.defaultBaseUrl
                                    selectedModel = type.defaultModel
                                },
                                label = { Text(type.displayName, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisTheme.config.primaryAccent.copy(alpha = 0.25f),
                                    selectedLabelColor = JarvisTheme.config.primaryAccent
                                )
                            )
                        }
                    }

                    // Profile Name
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("প্রোফাইল নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisTheme.config.primaryAccent,
                            unfocusedBorderColor = JarvisTheme.config.borderGlow
                        )
                    )

                    // API Key Field with Show/Hide & Paste
                    OutlinedTextField(
                        value = inputApiKey,
                        onValueChange = { inputApiKey = it },
                        label = { Text("API Key (${currentEditType.displayName})") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Show/Hide Key",
                                        tint = JarvisTheme.config.primaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        inputApiKey = clip.trim()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = JarvisTheme.config.primaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisTheme.config.primaryAccent,
                            unfocusedBorderColor = JarvisTheme.config.borderGlow
                        )
                    )

                    // Model Selection & Discovery
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "মডেল নির্বাচন:", fontSize = 11.sp, color = JarvisTheme.config.textSecondary)
                        TextButton(
                            onClick = {
                                isFetchingModels = true
                                coroutineScope.launch {
                                    val models = viewModel.fetchModelsForProvider(editingConfig!!.id, inputApiKey)
                                    fetchedModels = models
                                    isFetchingModels = false
                                    if (models.isNotEmpty()) {
                                        modelsExpanded = true
                                    }
                                }
                            },
                            enabled = !isFetchingModels
                        ) {
                            if (isFetchingModels) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("মডেল খুঁজছি...", fontSize = 10.sp)
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ডিসকভার মডেল", fontSize = 10.sp)
                            }
                        }
                    }

                    // Model Dropdown or Manual input
                    ExposedDropdownMenuBox(
                        expanded = modelsExpanded,
                        onExpandedChange = { modelsExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = { selectedModel = it },
                            label = { Text("Model ID (ম্যানুয়াল এন্ট্রি বা সিলেক্ট)") },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelsExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisTheme.config.primaryAccent,
                                unfocusedBorderColor = JarvisTheme.config.borderGlow
                            )
                        )

                        if (fetchedModels.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = modelsExpanded,
                                onDismissRequest = { modelsExpanded = false }
                            ) {
                                fetchedModels.forEach { modelInfo ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(modelInfo.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text(modelInfo.id, fontSize = 10.sp, color = Color.Gray)
                                            }
                                        },
                                        onClick = {
                                            selectedModel = modelInfo.id
                                            modelsExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Base URL (Custom Endpoint)
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("API Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisTheme.config.primaryAccent,
                            unfocusedBorderColor = JarvisTheme.config.borderGlow
                        )
                    )

                    // Temperature Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Temperature (সৃজনশীলতা)", fontSize = 11.sp, color = JarvisTheme.config.textSecondary)
                            Text(String.format("%.1f", temperature), fontSize = 11.sp, color = JarvisTheme.config.primaryAccent)
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0.0f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = JarvisTheme.config.primaryAccent,
                                activeTrackColor = JarvisTheme.config.primaryAccent
                            )
                        )
                    }

                    // Test Connection Button in Dialog
                    OutlinedButton(
                        onClick = {
                            isTestingInDialog = true
                            testResultInDialog = null
                            coroutineScope.launch {
                                val tempConfig = editingConfig!!.copy(
                                    type = currentEditType,
                                    baseUrl = baseUrl.trim(),
                                    selectedModel = selectedModel.trim()
                                )
                                viewModel.saveAiProviderConfig(tempConfig, inputApiKey)
                                val res = viewModel.testAiConnection(tempConfig.id, inputApiKey)
                                testResultInDialog = res
                                isTestingInDialog = false
                            }
                        },
                        enabled = !isTestingInDialog,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisTheme.config.primaryAccent)
                    ) {
                        if (isTestingInDialog) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("যাচাই করছি...", fontSize = 11.sp)
                        } else {
                            Text("কানেকশন টেস্ট করুন", fontSize = 11.sp, color = JarvisTheme.config.primaryAccent)
                        }
                    }

                    // Dialog test result feedback
                    if (testResultInDialog != null) {
                        Surface(
                            color = if (testResultInDialog!!.isSuccess) NeonEmerald.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testResultInDialog!!.message,
                                fontSize = 10.sp,
                                color = if (testResultInDialog!!.isSuccess) NeonEmerald else RedAlert,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalConfig = editingConfig!!.copy(
                            type = currentEditType,
                            name = profileName.trim().ifBlank { currentEditType.displayName },
                            baseUrl = baseUrl.trim().ifBlank { currentEditType.defaultBaseUrl },
                            selectedModel = selectedModel.trim().ifBlank { currentEditType.defaultModel },
                            temperature = temperature
                        )
                        viewModel.saveAiProviderConfig(finalConfig, inputApiKey)
                        showEditDialog = false
                        Toast.makeText(context, "'${finalConfig.name}' সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.primaryAccent)
                ) {
                    Text("Save & Apply", color = JarvisTheme.config.backgroundColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = JarvisTheme.config.textSecondary)
                }
            },
            containerColor = JarvisTheme.config.surfaceColor
        )
    }

    // DIALOG: CONFIRM DELETE
    if (configToDelete != null) {
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            title = {
                Text("প্রোভাইডার ডিলিট করবেন?", fontWeight = FontWeight.Bold, color = JarvisTheme.config.textPrimary)
            },
            text = {
                Text(
                    "আপনি কি নিশ্চিত যে '${configToDelete!!.name}' এবং এর সংরক্ষিত কী ডিলিট করতে চান?",
                    color = JarvisTheme.config.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAiProviderConfig(configToDelete!!.id)
                        configToDelete = null
                        Toast.makeText(context, "প্রোভাইডার ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { configToDelete = null }) {
                    Text("Cancel", color = JarvisTheme.config.textSecondary)
                }
            },
            containerColor = JarvisTheme.config.surfaceColor
        )
    }
}
