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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TaskEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyanArcPrimary
import com.example.ui.theme.HologramBlue
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RedAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasksScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    val pendingTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("tasks_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = CyanArcPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "টাস্ক ও রিমাইন্ডার",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanArcPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${pendingTasks.size} টি চলমান কাজ বাকি",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "কোনো কাজ সংরক্ষিত নেই।",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pendingTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "চলমান কাজ (${pendingTasks.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanArcPrimary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(pendingTasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggle = { viewModel.toggleTask(task) },
                                onDelete = { viewModel.deleteTask(task) }
                            )
                        }
                    }

                    if (completedTasks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "সম্পন্ন কাজ (${completedTasks.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(completedTasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggle = { viewModel.toggleTask(task) },
                                onDelete = { viewModel.deleteTask(task) }
                            )
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_task_fab"),
            containerColor = CyanArcPrimary,
            contentColor = Color(0xFF002229)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "নতুন কাজ")
        }

        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, priority, reminderMin ->
                    viewModel.addTask(title, priority, reminderMin)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.isCompleted
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dueDateStr = task.dueDate?.let { dateFormat.format(Date(it)) }

    CyberCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonEmerald,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = Color(0xFF00242E)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCompleted) TextMuted else TextPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Badge
                    val priorityColor = when (task.priority.lowercase()) {
                        "high", "জরুরি" -> RedAlert
                        "low", "কম" -> TextMuted
                        else -> HologramBlue
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priorityColor.copy(alpha = 0.15f),
                        modifier = Modifier.border(1.dp, priorityColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = task.priority,
                            fontSize = 10.sp,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (dueDateStr != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = CyanArcPrimary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = dueDateStr, fontSize = 11.sp, color = CyanArcPrimary)
                        }
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "মুছুন",
                    tint = RedAlert.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var reminderOption by remember { mutableIntStateOf(0) } // 0: None, 1: 15m, 2: 1h, 3: 3h

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "নতুন কাজ যোগ করুন", color = CyanArcPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("কাজের বিবরণ") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanArcPrimary,
                        unfocusedBorderColor = JarvisSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Text(text = "গুরুত্ব (Priority):", fontSize = 12.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Normal", "High").forEach { p ->
                        Surface(
                            onClick = { priority = p },
                            shape = RoundedCornerShape(8.dp),
                            color = if (priority == p) CyanArcPrimary else JarvisSurfaceVariant,
                            modifier = Modifier.border(1.dp, CyanArcPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = p,
                                color = if (priority == p) Color(0xFF00242E) else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(text = "রিমাইন্ডার সেট করুন:", fontSize = 12.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("না" to 0, "১৫ মিনিট" to 15, "১ ঘণ্টা" to 60, "৩ ঘণ্টা" to 180).forEach { (label, min) ->
                        Surface(
                            onClick = { reminderOption = min },
                            shape = RoundedCornerShape(8.dp),
                            color = if (reminderOption == min) NeonEmerald else JarvisSurfaceVariant,
                            modifier = Modifier.border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = label,
                                color = if (reminderOption == min) Color(0xFF00242E) else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val reminder = if (reminderOption > 0) reminderOption else null
                        onAdd(title.trim(), priority, reminder)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanArcPrimary)
            ) {
                Text(text = "যোগ করুন", color = Color(0xFF00242E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "বাতিল", color = TextSecondary)
            }
        },
        containerColor = JarvisSurfaceVariant
    )
}
