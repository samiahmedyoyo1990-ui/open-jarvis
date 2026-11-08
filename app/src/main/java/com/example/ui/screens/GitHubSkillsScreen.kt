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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DevWorkspaceRepoEntity
import com.example.data.local.entity.InstalledSkillEntity
import com.example.github.AuditStatus
import com.example.github.GitHubRepoItem
import com.example.github.SecurityAuditReport
import com.example.ui.InspectionUiState
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CyanArcPrimary
import com.example.ui.theme.HologramBlue
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RedAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GitHubTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    EXPLORE("Explore", Icons.Default.Explore),
    SEARCH("Search", Icons.Default.Search),
    IMPORT("Import URL", Icons.Default.Download),
    INSTALLED("Installed", Icons.Default.CheckCircle),
    DEV_WORKSPACE("Dev Workspace", Icons.Default.Code),
    AUDIT_LOGS("Audit Logs", Icons.Default.History)
}

@Composable
fun GitHubSkillsScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(GitHubTab.EXPLORE) }

    val installedSkills by viewModel.installedSkills.collectAsStateWithLifecycle()
    val devRepos by viewModel.devWorkspaceRepos.collectAsStateWithLifecycle()
    val auditLogs by viewModel.recentAuditLogs.collectAsStateWithLifecycle()
    val searchResults by viewModel.gitHubSearchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearchingGitHub.collectAsStateWithLifecycle()
    val inspectionState by viewModel.inspectionState.collectAsStateWithLifecycle()

    var tokenInput by remember { mutableStateOf(viewModel.getGitHubToken().orEmpty()) }
    var showTokenConfig by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("github_skills_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanArcPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "GITHUB & SKILLS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanArcPrimary,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Universal Skill Installer & Dev Workspace",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = { showTokenConfig = !showTokenConfig },
                    modifier = Modifier.testTag("github_token_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "GitHub Token",
                        tint = if (tokenInput.isNotBlank()) NeonEmerald else TextMuted
                    )
                }
            }

            // GitHub PAT Token Card
            AnimatedVisibility(visible = showTokenConfig) {
                CyberCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "GitHub Personal Access Token (Optional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanArcPrimary
                    )
                    Text(
                        text = "প্রাইভেট রিপোজিটরি ও উচ্চ রেট-লিমিটের (৫,০০০ req/hr) জন্য টোকেন ব্যবহার করতে পারেন।",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            placeholder = { Text("ghp_xxxxxxxx...", color = TextMuted, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanArcPrimary,
                                unfocusedBorderColor = BorderGlow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Button(
                            onClick = {
                                viewModel.saveGitHubToken(tokenInput)
                                showTokenConfig = false
                                Toast.makeText(context, "টোকেন সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanArcPrimary)
                        ) {
                            Text("সেভ", color = Color(0xFF002229), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Horizontal Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GitHubTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyanArcPrimary.copy(alpha = 0.2f) else JarvisSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (isSelected) CyanArcPrimary else BorderGlow.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = tab }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) CyanArcPrimary else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyanArcPrimary else TextSecondary
                            )
                        }
                    }
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    GitHubTab.EXPLORE -> {
                        ExploreFeaturedSkillsView(
                            featuredSkills = viewModel.featuredGitHubSkills,
                            onInspect = { repo -> viewModel.inspectGitHubRepository(repo.htmlUrl) }
                        )
                    }
                    GitHubTab.SEARCH -> {
                        GitHubSearchView(
                            isSearching = isSearching,
                            searchResults = searchResults,
                            onSearch = { query -> viewModel.searchGitHub(query) },
                            onInspect = { repo -> viewModel.inspectGitHubRepository(repo.htmlUrl) }
                        )
                    }
                    GitHubTab.IMPORT -> {
                        ImportUrlView(
                            onInspectUrl = { url, branch -> viewModel.inspectGitHubRepository(url, branch) }
                        )
                    }
                    GitHubTab.INSTALLED -> {
                        InstalledSkillsManagerView(
                            installedSkills = installedSkills,
                            onToggle = { id, enabled -> viewModel.toggleInstalledSkill(id, enabled) },
                            onDelete = { id -> viewModel.deleteInstalledSkill(id) },
                            onUpdate = { skill -> viewModel.updateInstalledSkill(skill) },
                            onRollback = { skill -> viewModel.rollbackInstalledSkill(skill) }
                        )
                    }
                    GitHubTab.DEV_WORKSPACE -> {
                        DeveloperWorkspaceView(
                            devRepos = devRepos,
                            onDelete = { id -> viewModel.deleteDevRepo(id) }
                        )
                    }
                    GitHubTab.AUDIT_LOGS -> {
                        AuditLogsView(auditLogs = auditLogs)
                    }
                }
            }
        }

        // Active Inspection / Installation Modal Overlay
        if (inspectionState !is InspectionUiState.Idle) {
            InspectionAuditModal(
                state = inspectionState,
                onDismiss = { viewModel.clearInspection() },
                onInstall = { report, perms -> viewModel.installSkillFromInspection(report, perms) },
                onImportDev = { report -> viewModel.importDevRepoFromInspection(report) }
            )
        }
    }
}

@Composable
private fun ExploreFeaturedSkillsView(
    featuredSkills: List<GitHubRepoItem>,
    onInspect: (GitHubRepoItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "FEATURED JARVIS SKILLS & AGENTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanArcPrimary,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "যাচাইকৃত ওপেন-সোর্স এআই স্কিল যা ১-ট্যাপে ইন্সটল করা সম্ভব।",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(featuredSkills, key = { it.id }) { repo ->
            RepoCard(repo = repo, onInspect = { onInspect(repo) })
        }
    }
}

@Composable
private fun GitHubSearchView(
    isSearching: Boolean,
    searchResults: List<GitHubRepoItem>,
    onSearch: (String) -> Unit,
    onInspect: (GitHubRepoItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val topicChips = listOf("ai-assistant", "coding-agent", "bengali-nlp", "automation", "prompts")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("গিটহাবে স্কিল খুঁজুন...", color = TextMuted, fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("github_search_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanArcPrimary,
                    unfocusedBorderColor = BorderGlow,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            IconButton(
                onClick = { if (searchQuery.isNotBlank()) onSearch(searchQuery) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyanArcPrimary)
                    .testTag("github_search_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF002229))
            }
        }

        // Quick Topic Suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            topicChips.forEach { topic ->
                Surface(
                    onClick = {
                        searchQuery = topic
                        onSearch(topic)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = JarvisSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.border(1.dp, BorderGlow.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "#$topic",
                        fontSize = 10.sp,
                        color = CyanArcPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (isSearching) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanArcPrimary)
            }
        } else if (searchResults.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("কোনো সার্চ ফলাফল নেই। কিওয়ার্ড দিয়ে অনুসন্ধান করুন।", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(searchResults, key = { it.id }) { repo ->
                    RepoCard(repo = repo, onInspect = { onInspect(repo) })
                }
            }
        }
    }
}

@Composable
private fun ImportUrlView(
    onInspectUrl: (String, String?) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var branchInput by remember { mutableStateOf("main") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CyberCard {
            Text(
                text = "PASTE GITHUB URL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CyanArcPrimary,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "যেকোনো পাবলিক বা প্রাইভেট রিপোজিটরি URL পেস্ট করুন। JARVIS স্বয়ংক্রিয়ভাবে ফাইল কাঠামো ও স্কিল ম্যানিফেস্ট বিশ্লেষণ করবে।",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                placeholder = { Text("https://github.com/owner/repository", color = TextMuted, fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("github_url_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanArcPrimary,
                    unfocusedBorderColor = BorderGlow,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = branchInput,
                onValueChange = { branchInput = it },
                label = { Text("Branch / Tag", fontSize = 10.sp, color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanArcPrimary,
                    unfocusedBorderColor = BorderGlow,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (urlInput.isNotBlank()) {
                        onInspectUrl(urlInput.trim(), branchInput.trim().takeIf { it.isNotBlank() })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inspect_repo_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanArcPrimary)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF002229), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("পরীক্ষা ও নিরাপত্তা স্ক্যান করুন", color = Color(0xFF002229), fontWeight = FontWeight.Bold)
            }
        }

        CyberCard {
            Text(
                text = "সমর্থিত স্কিল ফরম্যাটসমূহ:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• jarvis.skill.json (JARVIS Native Skill Package)\n• SKILL.md / skill.md (AI Agent Instructions with YAML frontmatter)\n• prompts.json (Prompt Packs)\n• Developer Source Projects (Imported directly into Developer Workspace)",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun InstalledSkillsManagerView(
    installedSkills: List<InstalledSkillEntity>,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onUpdate: (InstalledSkillEntity) -> Unit,
    onRollback: (InstalledSkillEntity) -> Unit
) {
    if (installedSkills.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("কোনো এক্সটার্নাল স্কিল ইন্সটল করা নেই।", fontSize = 12.sp, color = TextMuted)
                Text("'Explore' বা 'Import URL' থেকে স্কিল যুক্ত করুন।", fontSize = 11.sp, color = TextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(installedSkills, key = { it.id }) { skill ->
                InstalledSkillCard(
                    skill = skill,
                    onToggle = { onToggle(skill.id, it) },
                    onDelete = { onDelete(skill.id) },
                    onUpdate = { onUpdate(skill) },
                    onRollback = { onRollback(skill) }
                )
            }
        }
    }
}

@Composable
private fun InstalledSkillCard(
    skill: InstalledSkillEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    onRollback: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(skill.installedAt))

    CyberCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = skill.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CyanArcPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.border(1.dp, CyanArcPrimary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "v${skill.version}",
                            fontSize = 10.sp,
                            color = CyanArcPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Author: ${skill.author} | Installed: $dateStr",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Switch(
                checked = skill.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF002229),
                    checkedTrackColor = CyanArcPrimary,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = JarvisSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = skill.description,
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action row: Update, Rollback, Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onUpdate,
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanArcPrimary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update", fontSize = 10.sp, color = CyanArcPrimary)
                }

                if (skill.previousVersionBackupJson != null) {
                    OutlinedButton(
                        onClick = onRollback,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, tint = HologramBlue, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rollback", fontSize = 10.sp, color = HologramBlue)
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = RedAlert, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun DeveloperWorkspaceView(
    devRepos: List<DevWorkspaceRepoEntity>,
    onDelete: (Long) -> Unit
) {
    if (devRepos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Code, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("ডেভেলপার ওয়ার্কস্পেসে কোনো সোর্স কোড রিপোজিটরি নেই।", fontSize = 12.sp, color = TextMuted)
                Text("সাধারণ গিটহাব সোর্স প্রোজেক্ট ইমপোর্ট করলে তা এখানে দেখা যাবে।", fontSize = 11.sp, color = TextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(devRepos, key = { it.id }) { repo ->
                CyberCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = repo.repoName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanArcPrimary
                            )
                            Text(
                                text = "${repo.owner} • ${repo.language} • ⭐ ${repo.stars}",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }

                        IconButton(onClick = { onDelete(repo.id) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedAlert, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = repo.description,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = JarvisSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "ফাইল তালিকা ও আর্কিটেকচার:", fontSize = 10.sp, color = CyanArcPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = parseFileTree(repo.fileTreeJson),
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 6
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogsView(auditLogs: List<com.example.data.local.entity.SkillAuditLogEntity>) {
    if (auditLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো অডিট লগ নেই।", fontSize = 12.sp, color = TextMuted)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(auditLogs, key = { it.id }) { log ->
                val dateStr = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(log.scannedAt))
                val isSafe = log.status == "COMPATIBLE" || log.status == "SAFE_TO_REVIEW"

                CyberCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.repoUrl.removePrefix("https://github.com/"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSafe) NeonEmerald.copy(alpha = 0.2f) else RedAlert.copy(alpha = 0.2f),
                            modifier = Modifier.border(1.dp, if (isSafe) NeonEmerald else RedAlert, RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = log.status,
                                fontSize = 9.sp,
                                color = if (isSafe) NeonEmerald else RedAlert,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(text = "Security Score: ${log.securityScore}/100 • $dateStr", fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun RepoCard(
    repo: GitHubRepoItem,
    onInspect: () -> Unit
) {
    CyberCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repo.fullName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanArcPrimary
                )
                Text(
                    text = "${repo.language} • ${repo.license}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                Text(text = "${repo.stars}", fontSize = 11.sp, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = repo.description,
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onInspect,
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanArcPrimary)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF002229), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Inspect & Install", fontSize = 10.sp, color = Color(0xFF002229), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InspectionAuditModal(
    state: InspectionUiState,
    onDismiss: () -> Unit,
    onInstall: (SecurityAuditReport, List<String>) -> Unit,
    onImportDev: (SecurityAuditReport) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = JarvisSurface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, CyanArcPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(16.dp)
        ) {
            when (state) {
                is InspectionUiState.Inspecting -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = CyanArcPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("গিটহাব রিপোজিটরি বিশ্লেষণ ও নিরাপত্তা স্ক্যান চলছে...", fontSize = 12.sp, color = CyanArcPrimary)
                    }
                }

                is InspectionUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = RedAlert, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.message, color = RedAlert, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDismiss) { Text("বন্ধ করুন") }
                    }
                }

                is InspectionUiState.Success -> {
                    val report = state.report
                    var approvedPermissions by remember { mutableStateOf(report.requestedPermissions) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SECURITY AUDIT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanArcPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            StatusPill(status = report.status)
                        }

                        Text(
                            text = report.repoFullName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "Compatibility: ${report.compatibilityType.banglaLabel}",
                            fontSize = 11.sp,
                            color = CyanArcPrimary
                        )

                        // SHA-256 Checksum Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = JarvisSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlow.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "SHA-256: ${report.integritySha256.take(24)}...",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(6.dp)
                            )
                        }

                        // Security Findings List
                        if (report.findings.isNotEmpty()) {
                            Text(text = "নিরাপত্তা পর্যবেক্ষণ:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            report.findings.forEach { finding ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = when (finding.level) {
                                            "CRITICAL" -> Icons.Default.Error
                                            "WARNING" -> Icons.Default.Warning
                                            else -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = when (finding.level) {
                                            "CRITICAL" -> RedAlert
                                            "WARNING" -> Color(0xFFFFB300)
                                            else -> NeonEmerald
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = finding.message,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        // Requested Permissions Checklist
                        if (report.requestedPermissions.isNotEmpty()) {
                            Text(text = "প্রয়োজনীয় পারমিশন অনুমোদন:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            report.requestedPermissions.forEach { perm ->
                                val isChecked = approvedPermissions.contains(perm)
                                Surface(
                                    onClick = {
                                        approvedPermissions = if (isChecked) {
                                            approvedPermissions - perm
                                        } else {
                                            approvedPermissions + perm
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChecked) CyanArcPrimary.copy(alpha = 0.15f) else JarvisSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().border(1.dp, if (isChecked) CyanArcPrimary else BorderGlow, RoundedCornerShape(8.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = perm, fontSize = 11.sp, color = TextPrimary)
                                        Text(text = if (isChecked) "অনুমোদিত ✓" else "বাতিল ✗", fontSize = 10.sp, color = if (isChecked) NeonEmerald else RedAlert)
                                    }
                                }
                            }
                        }

                        // Safety Disclaimer
                        Text(
                            text = "সতর্কতা: সমস্ত বহিরাগত গিটহাব প্যাকেজ নিয়ন্ত্রিত স্যান্ডবক্সে কার্যকর হয়। কোনো কোড ডিভাইস সিস্টেম ফাইল বা গোপনীয় API Key অ্যাক্সেস করতে পারে না।",
                            fontSize = 9.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("বাতিল", fontSize = 11.sp)
                            }

                            if (report.isInstallable) {
                                Button(
                                    onClick = { onInstall(report, approvedPermissions) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanArcPrimary)
                                ) {
                                    Text("অনুমোদন ও ইন্সটল", color = Color(0xFF002229), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (report.isDeveloperSource) {
                                Button(
                                    onClick = { onImportDev(report) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = HologramBlue)
                                ) {
                                    Text("ওয়ার্কস্পেসে ইমপোর্ট", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun StatusPill(status: AuditStatus) {
    val (color, label) = when (status) {
        AuditStatus.COMPATIBLE -> NeonEmerald to "সামঞ্জস্যপূর্ণ"
        AuditStatus.SAFE_TO_REVIEW -> HologramBlue to "পর্যালোচনাযোগ্য"
        AuditStatus.NEEDS_PERMISSION -> Color(0xFFFFB300) to "অনুমতি আবশ্যক"
        AuditStatus.WARNING -> Color(0xFFFF9800) to "সতর্কতা"
        AuditStatus.UNSUPPORTED -> Color(0xFF9E9E9E) to "অসমর্থিত"
        AuditStatus.BLOCKED -> RedAlert to "ব্লকড"
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.border(1.dp, color, RoundedCornerShape(6.dp))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun parseFileTree(jsonStr: String): String {
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<String>()
        for (i in 0 until minOf(array.length(), 10)) {
            list.add(array.getString(i))
        }
        list.joinToString("\n")
    } catch (_: Exception) {
        "কোনো ফাইল তালিকা উপলব্ধ নেই"
    }
}
