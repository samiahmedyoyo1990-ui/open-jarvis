package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.components.AssistantState
import com.example.ui.components.CyberCard
import com.example.ui.components.JarvisArcOrb
import com.example.ui.theme.CyanArcPrimary
import com.example.ui.theme.HologramBlue
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    viewModel: JarvisViewModel,
    onComplete: () -> Unit
) {
    var inputName by remember { mutableStateOf("বস") }
    var micGranted by remember { mutableStateOf(false) }
    var notifGranted by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            JarvisArcOrb(
                state = AssistantState.IDLE,
                amplitude = 0.2f,
                size = 140.dp
            )

            Text(
                text = "JARVIS ইনিশিয়ালাইজেশন",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CyanArcPrimary
            )

            Text(
                text = "আমি আপনার স্মার্ট পার্সোনাল সহকারী। বাংলা ও ইংরেজিতে আপনার যেকোনো কথা শুনে ডিভাইস নিয়ন্ত্রণ, নোট রাখা ও কাজ গুছিয়ে দেব।",
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            // Name selection card
            CyberCard {
                Text(
                    text = "আপনাকে কীভাবে সম্বোধন করব?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    singleLine = true,
                    placeholder = { Text("যেমন: স্যার, বস বা আপনার নাম", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanArcPrimary,
                        unfocusedBorderColor = JarvisSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Permissions section
            CyberCard {
                Text(
                    text = "প্রয়োজনীয় অ্যাক্সেস ও অনুমতি",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                PermissionRow(
                    icon = Icons.Default.Mic,
                    title = "ভয়েস নির্দেশ (মাইক্রোফোন)",
                    desc = "কথা শুনে রিমাইন্ডার ও অ্যাকশন নেওয়ার জন্য",
                    isGranted = micGranted,
                    onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PermissionRow(
                        icon = Icons.Default.Notifications,
                        title = "রিমাইন্ডার নোটিফিকেশন",
                        desc = "জরুরি কাজের সময় সতর্কবার্তা পাঠানোর জন্য",
                        isGranted = notifGranted,
                        onRequest = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "প্রাইভেসি",
                        tint = NeonEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "আপনার কোনো তথ্য গোপনে রেকর্ড বা ক্লাউডে শেয়ার করা হয় না।",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (inputName.isNotBlank()) {
                        viewModel.updateUserName(inputName.trim())
                    }
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_complete_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanArcPrimary,
                    contentColor = Color(0xFF00242E)
                )
            ) {
                Text(
                    text = "JARVIS শুরু করুন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    desc: String,
    isGranted: Boolean,
    onRequest: () -> Unit
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) NeonEmerald.copy(alpha = 0.2f) else HologramBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (isGranted) NeonEmerald else CyanArcPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(text = desc, fontSize = 12.sp, color = TextMuted)
            }
        }

        if (!isGranted) {
            Surface(
                onClick = onRequest,
                shape = RoundedCornerShape(8.dp),
                color = JarvisSurfaceVariant,
                modifier = Modifier
                    .border(1.dp, CyanArcPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "অনুমতি দিন", fontSize = 12.sp, color = CyanArcPrimary, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text(text = "অনুমোদিত", fontSize = 12.sp, color = NeonEmerald, fontWeight = FontWeight.SemiBold)
        }
    }
}
