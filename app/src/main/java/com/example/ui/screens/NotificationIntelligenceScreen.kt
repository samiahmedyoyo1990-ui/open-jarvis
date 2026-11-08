package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.JarvisNotificationListenerService
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun NotificationIntelligenceScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notifications by JarvisNotificationListenerService.notifications.collectAsStateWithLifecycle()
    val isConnected by JarvisNotificationListenerService.isServiceConnected.collectAsStateWithLifecycle()
    val isPermissionGranted = remember { JarvisNotificationListenerService.isNotificationAccessGranted(context) }

    var summaryResult by remember { mutableStateOf<String?>(null) }
    var isSummarizing by remember { mutableStateOf(false) }

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
                    modifier = Modifier.size(40.dp).testTag("notifs_back_button")
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
                            text = "NOTIFICATION INTELLIGENCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "স্মার্ট নোটিফিকেশন ফিল্টার ও সারসংক্ষেপ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Permission / Access Card
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = if (isPermissionGranted) NeonEmerald else JarvisTheme.config.primaryAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPermissionGranted) "নোটিফিকেশন পারমিশন সক্রিয়" else "নোটিফিকেশন পারমিশন প্রয়োজন",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPermissionGranted) "JARVIS স্ট্যাটাস বারের নোটিফিকেশন বুদ্ধিমত্তার সাথে বিশ্লেষণ করতে প্রস্তুত।" else "Android Settings এ নোটিফিকেশন এক্সেস চালু করুন।",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    if (!isPermissionGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("অনুমতি দিন", fontSize = 11.sp, color = JarvisTheme.config.primaryAccent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Summarize Action
            if (notifications.isNotEmpty()) {
                Button(
                    onClick = {
                        scope.launch {
                            isSummarizing = true
                            val combined = notifications.take(5).joinToString("\n") { "${it.appTitle}: ${it.title} - ${it.text}" }
                            val prompt = "You are JARVIS Notification Intelligence. Summarize these status bar notifications into a brief Bengali digest, highlighting any urgent or important message:\n\n$combined"
                            val res = viewModel.universalAiEngine.executeAiQuery(prompt, emptyList())
                            summaryResult = res.replyText
                            isSummarizing = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.primaryAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("summarize_notifs_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = JarvisTheme.config.backgroundColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSummarizing) "সারসংক্ষেপ তৈরি হচ্ছে..." else "সব নোটিফিকেশনের AI সারসংক্ষেপ", color = JarvisTheme.config.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }

            if (summaryResult != null) {
                Spacer(modifier = Modifier.height(10.dp))
                CyberCard(borderColor = JarvisTheme.config.primaryAccent) {
                    Text(text = "AI NOTIFICATION DIGEST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisTheme.config.primaryAccent, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = summaryResult ?: "", fontSize = 12.sp, color = TextPrimary, lineHeight = 17.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Notification List
            Text(text = "সর্বশেষ নোটিফিকেশনসমূহ (${notifications.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))

            if (notifications.isEmpty()) {
                CyberCard {
                    Text(text = "কোনো নতুন নোটিফিকেশন নেই।", fontSize = 12.sp, color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.key }) { notif ->
                        CyberCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = notif.appTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisTheme.config.primaryAccent)
                                        if (notif.isPriority) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = NeonEmerald.copy(alpha = 0.2f)
                                            ) {
                                                Text(text = "VIP / URGENT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonEmerald, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                            }
                                        }
                                    }
                                    if (notif.title.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = notif.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = notif.text, fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
