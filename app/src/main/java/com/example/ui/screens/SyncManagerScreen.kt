package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun SyncManagerScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val syncEngine = viewModel.syncEngine
    val profile = remember { syncEngine.getDeviceProfile() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var passkey by remember { mutableStateOf("JARVIS-SECURE-KEY") }
    var restorePayload by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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
                    modifier = Modifier.size(40.dp).testTag("sync_back_button")
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
                            text = "ENCRYPTED MULTI-DEVICE SYNC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "এনক্রিপ্টেড ব্যাকআপ ও ডিভাইস সিঙ্ক",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Device Info Card
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "নিবন্ধিত ডিভাইস আইডি", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = profile.deviceId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = JarvisTheme.config.primaryAccent, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = profile.deviceName, fontSize = 11.sp, color = TextMuted)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonEmerald.copy(alpha = 0.2f)
                    ) {
                        Text(text = "ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonEmerald, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            // Security Policy Card
            CyberCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "এন্ড-টু-এন্ড এনক্রিপশন নীতি", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "নোট, টাস্ক ও মেমোরি AES-256 দিয়ে এনক্রিপ্ট হয়। নিরাপত্তার জন্য আপনার ব্যক্তিগত AI API কী কখনোই সিঙ্কে অন্তর্ভুক্ত হয় না।",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            // Passkey Input
            CyberCard {
                Text(text = "ব্যাকআপ সুরক্ষার পাসকি (Passkey)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = passkey,
                    onValueChange = { passkey = it },
                    placeholder = { Text("একটি শক্তিশালী পাসকি লিখুন", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisTheme.config.primaryAccent,
                        unfocusedBorderColor = BorderGlow,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val encrypted = syncEngine.generateEncryptedBackup(passkey)
                            clipboardManager.setText(AnnotatedString(encrypted))
                            statusMessage = "এনক্রিপ্টেড ব্যাকআপ তৈরি ও ক্লিপবোর্ডে কপি করা হয়েছে!"
                            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.primaryAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = JarvisTheme.config.backgroundColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("এনক্রিপ্টেড ব্যাকআপ এক্সপোর্ট করুন", color = JarvisTheme.config.backgroundColor, fontWeight = FontWeight.Bold)
                }
            }

            // Restore Card
            CyberCard {
                Text(text = "ব্যাকআপ রিস্টোর করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = restorePayload,
                    onValueChange = { restorePayload = it },
                    placeholder = { Text("এক্সপোর্টকৃত ব্যাকআপ পেস্ট করুন", fontSize = 12.sp, color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisTheme.config.primaryAccent,
                        unfocusedBorderColor = BorderGlow,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val res = syncEngine.restoreEncryptedBackup(restorePayload.trim(), passkey)
                            statusMessage = res.message
                            Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = restorePayload.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.cardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, JarvisTheme.config.primaryAccent, RoundedCornerShape(10.dp)).testTag("restore_backup_button")
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = JarvisTheme.config.primaryAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("রিস্টোর প্রক্রিয়া শুরু করুন", color = JarvisTheme.config.primaryAccent, fontWeight = FontWeight.Bold)
                }
            }

            if (statusMessage != null) {
                Text(
                    text = statusMessage ?: "",
                    fontSize = 12.sp,
                    color = NeonEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
