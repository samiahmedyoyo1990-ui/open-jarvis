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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.entity.WorkspaceEntity
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

@Composable
fun WorkspacesScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val currentSlug by viewModel.currentWorkspaceSlug.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newInstructions by remember { mutableStateOf("") }

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
                        modifier = Modifier.size(40.dp).testTag("workspaces_back_button")
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
                                text = "PROJECT WORKSPACES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisTheme.config.primaryAccent,
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = null,
                                tint = JarvisTheme.config.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "স্বাধীন কনটেক্সট ও প্রজেক্ট স্পেস",
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
                    modifier = Modifier.testTag("create_workspace_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = JarvisTheme.config.backgroundColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisTheme.config.backgroundColor)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "প্রতিটি ওয়ার্কস্পেসে স্বতন্ত্র মেমরি, চ্যাট কনটেক্সট এবং বিশেষায়িত সিস্টেম ইন্সট্রাকশন সংরক্ষিত থাকে।",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(workspaces, key = { it.id }) { ws ->
                    val isActive = ws.slug == currentSlug

                    CyberCard(
                        borderColor = if (isActive) JarvisTheme.config.primaryAccent else BorderGlow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectWorkspace(ws.slug) }
                            .testTag("workspace_item_${ws.slug}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isActive) JarvisTheme.config.primaryAccent.copy(alpha = 0.2f) else JarvisTheme.config.cardBg,
                                    modifier = Modifier.size(42.dp).border(1.dp, if (isActive) JarvisTheme.config.primaryAccent else BorderGlow, RoundedCornerShape(10.dp))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (isActive) JarvisTheme.config.primaryAccent else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ws.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) JarvisTheme.config.primaryAccent else TextPrimary
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = NeonEmerald.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeonEmerald,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ws.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 2
                                    )
                                }
                            }

                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                Text("নতুন ওয়ার্কস্পেস তৈরি করুন", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("ওয়ার্কস্পেস নাম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("সংক্ষিপ্ত বিবরণ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newInstructions,
                        onValueChange = { newInstructions = it },
                        label = { Text("সিস্টেম ইন্সট্রাকশন / গাইডলাইন") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val slug = "ws_${System.currentTimeMillis()}"
                            viewModel.saveWorkspace(
                                WorkspaceEntity(
                                    slug = slug,
                                    name = newName.trim(),
                                    description = newDesc.trim().ifBlank { "কাস্টম ওয়ার্কস্পেস" },
                                    systemInstructions = newInstructions.trim()
                                )
                            )
                            viewModel.selectWorkspace(slug)
                            showCreateDialog = false
                            newName = ""
                            newDesc = ""
                            newInstructions = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.primaryAccent)
                ) {
                    Text("তৈরি করুন", color = JarvisTheme.config.backgroundColor)
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
