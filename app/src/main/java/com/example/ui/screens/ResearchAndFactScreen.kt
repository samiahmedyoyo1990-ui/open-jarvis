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
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.research.FactVerificationReport
import com.example.ai.research.ResearchResult
import com.example.ai.research.VerificationStatus
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RedAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ResearchAndFactScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val researchEngine = viewModel.webResearchAndFactEngine
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var researchQuery by remember { mutableStateOf("") }
    var factQuery by remember { mutableStateOf("") }

    var isResearching by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }

    var researchResult by remember { mutableStateOf<ResearchResult?>(null) }
    var factReport by remember { mutableStateOf<FactVerificationReport?>(null) }

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
                    modifier = Modifier.size(40.dp).testTag("research_back_button")
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
                            text = "LIVE RESEARCH & FACT CHECK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTheme.config.primaryAccent,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = JarvisTheme.config.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "ওয়েব গবেষণা ও সত্যতা যাচাইকরণ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = JarvisTheme.config.cardBg,
                contentColor = JarvisTheme.config.primaryAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = JarvisTheme.config.primaryAccent
                    )
                },
                modifier = Modifier.border(1.dp, BorderGlow, RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("ওয়েব রিসার্চ", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("ফ্যাক্ট চেকিং", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTabIndex == 0) {
                // Web Research Tab
                CyberCard {
                    Text(text = "লাইভ অনুসন্ধান কিউরি", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = researchQuery,
                        onValueChange = { researchQuery = it },
                        placeholder = { Text("যেমন: 'জেমস ওয়েব স্পেস টেলিস্কোপের সর্বশেষ আবিষ্কারসমূহ'", fontSize = 12.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("research_query_input"),
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
                            if (researchQuery.isNotBlank() && !isResearching) {
                                scope.launch {
                                    isResearching = true
                                    researchResult = researchEngine.performResearch(researchQuery)
                                    isResearching = false
                                }
                            }
                        },
                        enabled = researchQuery.isNotBlank() && !isResearching,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.primaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("start_research_button")
                    ) {
                        if (isResearching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = JarvisTheme.config.backgroundColor, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("তথ্য খোঁজা হচ্ছে...", color = JarvisTheme.config.backgroundColor, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = JarvisTheme.config.backgroundColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("গবেষণা ও তথ্য বিশ্লেষণ করুন", color = JarvisTheme.config.backgroundColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                researchResult?.let { res ->
                    CyberCard(borderColor = JarvisTheme.config.primaryAccent) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "RESEARCH SYNTHESIS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisTheme.config.primaryAccent, fontFamily = FontFamily.Monospace)
                            Text(text = "${res.executionTimeMs} ms", fontSize = 10.sp, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = res.synthesizedSummary, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)

                        if (res.sources.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "সংযুক্ত তথ্যসূত্রসমূহ:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            res.sources.forEach { src ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = JarvisTheme.config.primaryAccent, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = src.title, fontSize = 11.sp, color = JarvisTheme.config.primaryAccent, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            } else {
                // Fact Checking Tab
                CyberCard {
                    Text(text = "যাচাইযোগ্য দাবি বা বিবৃতি", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = factQuery,
                        onValueChange = { factQuery = it },
                        placeholder = { Text("দাবি পেস্ট করুন (যেমন: 'সূর্য পৃথিবীর চারিদিকে ঘোরে')", fontSize = 12.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("fact_query_input"),
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
                            if (factQuery.isNotBlank() && !isVerifying) {
                                scope.launch {
                                    isVerifying = true
                                    factReport = researchEngine.verifyFacts(factQuery)
                                    isVerifying = false
                                }
                            }
                        },
                        enabled = factQuery.isNotBlank() && !isVerifying,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.primaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("start_fact_check_button")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = JarvisTheme.config.backgroundColor, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সত্যতা যাচাই করা হচ্ছে...", color = JarvisTheme.config.backgroundColor, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.FactCheck, contentDescription = null, tint = JarvisTheme.config.backgroundColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ফ্যাক্ট চেক করুন", color = JarvisTheme.config.backgroundColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                factReport?.let { report ->
                    val badgeColor = when (report.overallStatus) {
                        VerificationStatus.VERIFIED -> NeonEmerald
                        VerificationStatus.PARTIALLY_VERIFIED -> Color(0xFFFFB300)
                        VerificationStatus.CONFLICTING_SOURCES -> RedAlert
                        VerificationStatus.UNCERTAIN -> TextMuted
                    }

                    CyberCard(borderColor = badgeColor) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = report.overallStatus.banglaLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = report.verifiedSummary, fontSize = 12.sp, color = TextPrimary, lineHeight = 17.sp)
                    }
                }
            }
        }
    }
}
