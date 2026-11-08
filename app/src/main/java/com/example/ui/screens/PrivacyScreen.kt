package com.example.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyanArcPrimary
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RedAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PrivacyScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val hasMic = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

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
                    modifier = Modifier.testTag("privacy_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = CyanArcPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "অনুমতি ও নিরাপত্তা নীতি",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanArcPrimary
                )
            }

            // Privacy Declaration
            CyberCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "ব্যক্তিগত তথ্যের সর্বোচ্চ নিরাপত্তা", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "JARVIS আপনার কোনো কথোপকথন বা ব্যক্তিগত অডিও গোপনে রেকর্ড করে না। কেবলমাত্র ব্যবহারকারী সরাসরি পুশ-টু-টক (Push-to-Talk) বোতাম চাপলে তবেই মাইক্রোফোন সক্রিয় হয়।\n\nআপনার সমস্ত নোট, টাস্ক এবং মেমোরি আপনার ডিভাইসের সুরক্ষিত Room ডাটাবেসে লোকালভাবে সংরক্ষিত থাকে।",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }

            // Permissions Status
            Text(text = "সিস্টেম পারমিশন স্ট্যাটাস", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyanArcPrimary)

            CyberCard {
                PermissionStatusItem(
                    icon = Icons.Default.Mic,
                    name = "মাইক্রোফোন (RECORD_AUDIO)",
                    purpose = "কথা শুনে রিমাইন্ডার ও নির্দেশ বোঝার জন্য",
                    isGranted = hasMic
                )
                Spacer(modifier = Modifier.height(12.dp))
                PermissionStatusItem(
                    icon = Icons.Default.Notifications,
                    name = "নোটিফিকেশন (POST_NOTIFICATIONS)",
                    purpose = "নির্ধারিত সময়ে রিমাইন্ডার পাঠানোর জন্য",
                    isGranted = hasNotif
                )
                Spacer(modifier = Modifier.height(12.dp))
                PermissionStatusItem(
                    icon = Icons.Default.Wifi,
                    name = "ইন্টারনেট (INTERNET)",
                    purpose = "Gemini ক্লাউড ইন্টেলিজেন্স ও অনলাইন কোয়েরির জন্য",
                    isGranted = true
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusItem(
    icon: ImageVector,
    name: String,
    purpose: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) NeonEmerald.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) NeonEmerald else RedAlert,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = purpose, fontSize = 11.sp, color = TextMuted)
            }
        }

        Text(
            text = if (isGranted) "অনুমোদিত" else "অনুমোদন নেই",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isGranted) NeonEmerald else RedAlert
        )
    }
}
