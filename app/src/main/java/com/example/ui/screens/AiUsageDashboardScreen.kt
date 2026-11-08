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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import com.example.ui.theme.CyanArcPrimary
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RedAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AiUsageDashboardScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val data by viewModel.usageDashboardData.collectAsStateWithLifecycle()

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
                    modifier = Modifier.size(40.dp).testTag("usage_back_button")
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
                            text = "AI USAGE & COST INTEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "টোকেন ব্যবহার ও খরচ বিশ্লেষণ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Summary Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "মোট রিকোয়েস্ট",
                    value = "${data.totalRequests}",
                    sub = "সফল কিউরি",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "ব্যবহৃত টোকেন",
                    value = "${data.totalTokens}",
                    sub = "ইনপুট + আউটপুট",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "আনুমানিক খরচ",
                    value = String.format("$%.4f", data.totalEstimatedCostUsd),
                    sub = "BYOK রেট অনুযায়ী",
                    modifier = Modifier.weight(1f),
                    accentColor = NeonEmerald
                )
                MetricTile(
                    title = "গড় লেটেন্সি",
                    value = "${data.averageLatencyMs} ms",
                    sub = "নেটওয়ার্ক রেসপন্স",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Provider Breakdown
            Text(
                text = "প্রোভাইডারভিত্তিক বিশ্লেষণ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JarvisTheme.config.primaryAccent
            )

            if (data.providerSummaries.isEmpty()) {
                CyberCard {
                    Text(
                        text = "এখনও কোনো AI কিউরি রেকর্ড করা হয়নি। কথা বলুন বা চ্যাট করুন।",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            } else {
                data.providerSummaries.forEach { summary ->
                    CyberCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = summary.providerId,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${summary.requestCount} Requests • ${summary.totalTokens} Tokens",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = String.format("$%.4f", summary.totalCostUsd),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Clear Log Button
            Button(
                onClick = { viewModel.clearUsageLogs() },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.cardBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlow, RoundedCornerShape(10.dp))
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = RedAlert, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ব্যবহারের ইতিহাস মুছে ফেলুন", color = RedAlert, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    accentColor: androidx.compose.ui.graphics.Color = CyanArcPrimary
) {
    CyberCard(modifier = modifier) {
        Text(text = title, fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = sub, fontSize = 10.sp, color = TextMuted)
    }
}
