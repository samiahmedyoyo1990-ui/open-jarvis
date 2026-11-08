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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diagnostic.DiagnosticStatus
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
fun DiagnosticScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val report by viewModel.diagnosticReport.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.runDiagnostics()
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        modifier = Modifier.size(40.dp).testTag("diagnostic_back_button")
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
                                text = "SELF-DIAGNOSTIC & REPAIR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisTheme.config.primaryAccent,
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = JarvisTheme.config.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "সিস্টেম ডায়াগনস্টিক ও নিরাপদ মেরামত",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.runDiagnostics() },
                    modifier = Modifier.size(40.dp).testTag("refresh_diagnostics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = JarvisTheme.config.primaryAccent
                    )
                }
            }

            // Overall Status Banner
            report?.let { rep ->
                val statusColor: Color = when (rep.overallStatus) {
                    DiagnosticStatus.HEALTHY -> NeonEmerald
                    DiagnosticStatus.WARNING -> Color(0xFFFFB300)
                    DiagnosticStatus.CRITICAL -> RedAlert
                }
                val statusText: String = when (rep.overallStatus) {
                    DiagnosticStatus.HEALTHY -> "সবগুলো সিস্টেম স্বাভাবিক ও কার্যক্ষম"
                    DiagnosticStatus.WARNING -> "সতর্কতা: কিছু কনফিগারেশন পর্যবেক্ষণ প্রয়োজন"
                    DiagnosticStatus.CRITICAL -> "সিস্টেম ত্রুটি শনাক্ত: সমাধান আবশ্যক"
                }

                CyberCard(borderColor = statusColor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (rep.overallStatus) {
                                DiagnosticStatus.HEALTHY -> Icons.Default.CheckCircle
                                DiagnosticStatus.WARNING -> Icons.Default.Warning
                                DiagnosticStatus.CRITICAL -> Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "সামগ্রিক স্বাস্থ্য অবস্থা",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                            Text(
                                text = statusText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Diagnostic items
                rep.items.forEach { item ->
                    val itemColor = when (item.status) {
                        DiagnosticStatus.HEALTHY -> NeonEmerald
                        DiagnosticStatus.WARNING -> Color(0xFFFFB300)
                        DiagnosticStatus.CRITICAL -> RedAlert
                    }

                    CyberCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(itemColor, RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.description,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.details,
                                    fontSize = 11.sp,
                                    color = itemColor,
                                    lineHeight = 15.sp
                                )
                            }

                            if (item.canAutoRepair && item.repairActionName != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val res = viewModel.performSafeRepair(item)
                                            Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
                                            viewModel.runDiagnostics()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("repair_${item.title.take(6)}")
                                ) {
                                    Text(item.repairActionName, fontSize = 10.sp, color = JarvisTheme.config.primaryAccent)
                                }
                            }
                        }
                    }
                }

                // Export Report Button
                Button(
                    onClick = {
                        val reportJson = viewModel.exportDiagnosticReport()
                        clipboardManager.setText(AnnotatedString(reportJson))
                        Toast.makeText(context, "ডায়াগনস্টিক রিপোর্ট ক্লিপবোর্ডে কপি করা হয়েছে।", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisTheme.config.cardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderGlow, RoundedCornerShape(10.dp))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = JarvisTheme.config.primaryAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("রেড্যাক্টেড ডায়াগনস্টিক রিপোর্ট কপি করুন", color = JarvisTheme.config.primaryAccent, fontSize = 12.sp)
                }
            }
        }
    }
}
