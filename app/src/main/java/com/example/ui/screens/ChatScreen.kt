package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.modes.AnswerMode
import com.example.data.local.entity.ConversationEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.AssistantState
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CyanArcPrimary
import com.example.ui.theme.HologramBlue
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val currentMode by viewModel.currentAnswerMode.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var attachedDocUri by remember { mutableStateOf<Uri?>(null) }
    var attachedDocName by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedImageUri = uri
            attachedDocUri = null
            attachedDocName = null
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            attachedDocUri = uri
            attachedDocName = uri.lastPathSegment?.substringAfterLast('/') ?: "document.txt"
            attachedImageUri = null
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        }
    }

    LaunchedEffect(conversations.size) {
        if (conversations.isNotEmpty()) {
            listState.animateScrollToItem(conversations.size - 1)
        }
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
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = CyanArcPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "JARVIS CHAT",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanArcPrimary,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (assistantState == AssistantState.THINKING) "টাইপিং ও চিন্তা করছে..." else "অনলাইন ইন্টেলিজেন্স (${currentMode.titleBn})",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.clearConversations() },
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "ইতিহাস মুছুন",
                        tint = TextSecondary
                    )
                }
            }

            // Answer Modes Horizontal Scroll Row (System 23)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnswerMode.entries.forEach { mode ->
                    val isSelected = mode == currentMode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyanArcPrimary.copy(alpha = 0.2f) else JarvisSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (isSelected) CyanArcPrimary else BorderGlow.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setAnswerMode(mode) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = mode.icon, fontSize = 12.sp)
                            Text(
                                text = mode.titleBn,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyanArcPrimary else TextSecondary
                            )
                        }
                    }
                }
            }

            // Conversation Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (conversations.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚡ JARVIS এআই সক্রিয়",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanArcPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "যেকোনো প্রশ্ন করুন, ছবি আপলোড করুন, ফাইল বিশ্লেষণ করুন বা ভয়েস কমান্ড দিন।",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(conversations, key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                onSpeak = { viewModel.speakResponse(message.text) }
                            )
                        }

                        if (assistantState == AssistantState.THINKING) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = JarvisSurfaceVariant,
                                        modifier = Modifier.border(1.dp, BorderGlow, RoundedCornerShape(12.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = CyanArcPrimary,
                                                strokeWidth = 2.dp
                                            )
                                            Text(text = "JARVIS চিন্তা করছে...", fontSize = 12.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Attachment Banner (if image or document is picked)
            if (attachedImageUri != null || attachedDocUri != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HologramBlue.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .border(1.dp, HologramBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (attachedImageUri != null) "🖼️ ছবি সংযুক্ত করা হয়েছে" else "📄 ${attachedDocName ?: "ডকুমেন্ট"}",
                                fontSize = 12.sp,
                                color = CyanArcPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = {
                                attachedImageUri = null
                                attachedDocUri = null
                                attachedDocName = null
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "বাতিল",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderGlow, RoundedCornerShape(16.dp)),
                color = JarvisSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Document Attachment Button
                    IconButton(
                        onClick = { docPickerLauncher.launch(arrayOf("text/*", "application/pdf")) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("attach_doc_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "ডকুমেন্ট সংযুক্ত করুন",
                            tint = if (attachedDocUri != null) CyanArcPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Photo Attachment Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("attach_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "ছবি সংযুক্ত করুন",
                            tint = if (attachedImageUri != null) CyanArcPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (attachedImageUri != null) "ছবি সম্পর্কে প্রশ্ন করুন..." else if (attachedDocUri != null) "ডকুমেন্ট সম্পর্কে প্রশ্ন করুন..." else "এখানে লিখুন...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = false,
                        maxLines = 3
                    )

                    IconButton(
                        onClick = {
                            if (assistantState == AssistantState.LISTENING) {
                                viewModel.stopListening()
                            } else {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (assistantState == AssistantState.LISTENING) NeonEmerald else JarvisSurfaceVariant)
                            .testTag("chat_mic_button")
                    ) {
                        Icon(
                            imageVector = if (assistantState == AssistantState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "কথা বলুন",
                            tint = if (assistantState == AssistantState.LISTENING) Color.White else CyanArcPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            val imgUri = attachedImageUri
                            val docUri = attachedDocUri
                            val text = inputText.trim()

                            if (imgUri != null) {
                                viewModel.analyzeImageAttachment(imgUri, if (text.isNotBlank()) text else "এই ছবিটি বিশ্লেষণ করুন")
                                attachedImageUri = null
                                inputText = ""
                            } else if (docUri != null) {
                                viewModel.analyzeDocument(docUri, attachedDocName ?: "document", if (text.isNotBlank()) text else null)
                                attachedDocUri = null
                                attachedDocName = null
                                inputText = ""
                            } else if (text.isNotBlank()) {
                                viewModel.processUserQuery(text)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanArcPrimary)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "পাঠান",
                            tint = Color(0xFF002229),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ConversationEntity,
    onSpeak: () -> Unit
) {
    val isUser = message.isUser
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) HologramBlue.copy(alpha = 0.25f) else JarvisSurfaceVariant,
            modifier = Modifier
                .border(
                    1.dp,
                    if (isUser) HologramBlue.copy(alpha = 0.5f) else BorderGlow,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "আপনি" else "JARVIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) HologramBlue else CyanArcPrimary,
                        fontFamily = FontFamily.Monospace
                    )

                    if (!isUser) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "শুনুন",
                                tint = CyanArcPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )

                if (!message.actionExecuted.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonEmerald.copy(alpha = 0.15f),
                        modifier = Modifier.border(1.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = "অ্যাকশন: ${message.actionExecuted}",
                            fontSize = 11.sp,
                            color = NeonEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
