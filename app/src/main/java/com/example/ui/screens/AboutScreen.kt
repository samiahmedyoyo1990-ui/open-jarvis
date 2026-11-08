package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun AboutScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val githubUrl = "https://github.com/samiahmedyoyo1990-ui/open-jarvis"

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
            horizontalAlignment = Alignment.CenterHorizontally,
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
                    modifier = Modifier.testTag("about_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = CyanArcPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "JARVIS পরিচিতি",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanArcPrimary,
                    letterSpacing = 1.sp
                )
            }

            JarvisArcOrb(
                state = AssistantState.IDLE,
                amplitude = 0.25f,
                size = 130.dp
            )

            Text(
                text = "JARVIS AI ASSISTANT",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CyanArcPrimary,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "সংস্করণ: 1.0.0 (Production Release)",
                fontSize = 12.sp,
                color = TextSecondary
            )

            // GitHub Fork Reference Card
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = CyanArcPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "গিটহাব প্রজেক্ট ও সোর্স কোড",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "GitHub Fork: samiahmedyoyo1990-ui/open-jarvis",
                    fontSize = 13.sp,
                    color = HologramBlue
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, CyanArcPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = CyanArcPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "GitHub রিপোজিটরি দেখুন", color = CyanArcPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            // Architecture Breakdown
            CyberCard {
                Text(
                    text = "সিস্টেম আর্কিটেকচার ও প্রযুক্তি",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                TechSpecItem(title = "AI ব্রেন", value = "Google Gemini 3.8 Flash (Firebase AI Logic)")
                TechSpecItem(title = "সিকিউরিটি", value = "Firebase App Check Attestation")
                TechSpecItem(title = "ভয়েস ইঞ্জিন", value = "Android SpeechRecognizer + TextToSpeech (bn-BD)")
                TechSpecItem(title = "লোকাল মেমোরি", value = "Android Room SQLite + DataStore Preferences")
                TechSpecItem(title = "ইউজার ইন্টারফেস", value = "Jetpack Compose + Material 3 Cyber-HUD")
                TechSpecItem(title = "অ্যাকশন ইঞ্জিন", value = "Intent-based App & System Controller")
            }

            // Developer Transfer & GitHub Info
            CyberCard {
                Text(
                    text = "গিটহাব বা লোকাল ডেভেলপমেন্ট গাইড",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonEmerald
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "AI Studio প্রজেক্টের সম্পূর্ণ সোর্স কোড ডাউনলোড করে অথবা সরাসরি GitHub রিপোজিটরিতে git remote হিসেবে কানেক্ট করে Android Studio-তে ওপেন করা যাবে।",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TechSpecItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 12.sp, color = TextMuted)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
