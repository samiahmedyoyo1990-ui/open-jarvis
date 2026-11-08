package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
fun SkillsMarketplaceScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val skillsEngine = viewModel.skillsEngine
    val skills by skillsEngine.skills.collectAsStateWithLifecycle()

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).testTag("skills_back_button")
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
                            text = "AI SKILLS & PLUGINS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "মডুলার স্কিল ও প্লাগইন মার্কেটপ্লেস",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Security disclaimer card
            CyberCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সিকিউর ক্যাপাবিলিটি সিস্টেম",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonEmerald
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "সব স্কিল সুরক্ষিত স্যান্ডবক্সে এক্সিকিউট হয়। কোনো বহিরাগত অপরীক্ষিত এক্সিকিউটেবল কোড বা DEX ডাউনলোড করা হয় না।",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(skills, key = { it.id }) { skill ->
                    CyberCard(
                        borderColor = if (skill.isEnabled) JarvisTheme.config.primaryAccent.copy(alpha = 0.5f) else BorderGlow
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = skill.banglaName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (skill.isEnabled) JarvisTheme.config.primaryAccent else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = JarvisTheme.config.primaryAccent.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = skill.category,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = JarvisTheme.config.primaryAccent,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = skill.description,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )

                                if (skill.permissions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        skill.permissions.forEach { perm ->
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = JarvisTheme.config.cardBg,
                                                modifier = Modifier.border(1.dp, BorderGlow, RoundedCornerShape(4.dp))
                                            ) {
                                                Text(
                                                    text = perm,
                                                    fontSize = 8.sp,
                                                    color = TextMuted,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Switch(
                                checked = skill.isEnabled,
                                onCheckedChange = { skillsEngine.toggleSkill(skill.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = JarvisTheme.config.backgroundColor,
                                    checkedTrackColor = JarvisTheme.config.primaryAccent
                                ),
                                modifier = Modifier.testTag("skill_switch_${skill.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}
