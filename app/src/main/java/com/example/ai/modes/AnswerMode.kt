package com.example.ai.modes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.vector.ImageVector

enum class AnswerMode(
    val id: String,
    val title: String,
    val banglaTitle: String,
    val description: String,
    val temperatureOffset: Float,
    val systemPromptSuffix: String
) {
    QUICK(
        id = "quick",
        title = "Quick Mode",
        banglaTitle = "কুইক মোড",
        description = "দ্রুত ও সংক্ষিপ্ত উত্তর, অপ্রয়োজনীয় ভূমিকা ছাড়া সরাসরি সমাধান।",
        temperatureOffset = -0.2f,
        systemPromptSuffix = "Mode: QUICK. Be extremely direct, concise, and fast. Provide immediate solutions without unnecessary conversational preamble."
    ),
    DEEP_THINK(
        id = "deep_think",
        title = "Deep Think",
        banglaTitle = "ডিপ থিঙ্ক মোড",
        description = "গভীর চিন্তাভাবনা, ধাপে ধাপে যুক্তি বিশ্লেষণ ও সম্ভাব্য সব দিকের বিশদ আলোচনা।",
        temperatureOffset = 0.0f,
        systemPromptSuffix = "Mode: DEEP_THINK. Think step-by-step through the problem. Break down underlying nuances, compare alternatives, and provide a thorough, structured, and insightful explanation."
    ),
    RESEARCH(
        id = "research",
        title = "Research Mode",
        banglaTitle = "রিসার্চ মোড",
        description = "গবেষণাধর্মী উত্তর, তথ্যসূত্র যাচাইকরণ ও বহুমুখী দৃষ্টিভঙ্গি উপস্থাপন।",
        temperatureOffset = -0.1f,
        systemPromptSuffix = "Mode: RESEARCH. Act as a senior academic researcher. Cite verifiable principles, structure arguments logically, distinguish settled facts from assumptions, and synthesize sources."
    ),
    CREATIVE(
        id = "creative",
        title = "Creative Mode",
        banglaTitle = "ক্রিয়েটিভ মোড",
        description = "সৃজনশীল গল্প, বাংলা কবিতা, চিত্রনাট্য ও চিত্তাকর্ষক বর্ণনা।",
        temperatureOffset = 0.4f,
        systemPromptSuffix = "Mode: CREATIVE. Use engaging, vivid storytelling, poetic metaphors, and high imagination while retaining JARVIS's polite elegance."
    ),
    CODING(
        id = "coding",
        title = "Coding Mode",
        banglaTitle = "কোডিং মোড",
        description = "প্রোডাকশন-গ্রেড কোড, বাগ ফিক্সিং, সঠিক আর্কিটেকচার ও ব্যাখ্যা।",
        temperatureOffset = -0.3f,
        systemPromptSuffix = "Mode: CODING. Provide clean, robust, modern, production-grade code snippets. Explain edge-case handling, performance considerations, and architectural rationale."
    ),
    PRECISION(
        id = "precision",
        title = "Precision Mode",
        banglaTitle = "প্রিসিশন মোড",
        description = "সর্বোচ্চ নির্ভুলতা, গাণিতিক স্পষ্টতা ও অতিরঞ্জনহীন সঠিক তথ্য।",
        temperatureOffset = -0.4f,
        systemPromptSuffix = "Mode: PRECISION. Adhere strictly to factual precision, rigorous calculations, and unambiguous statements. Avoid speculation or unverified claims."
    );

    fun getIcon(): ImageVector = when (this) {
        QUICK -> Icons.Default.Bolt
        DEEP_THINK -> Icons.Default.Psychology
        RESEARCH -> Icons.Default.Public
        CREATIVE -> Icons.Default.ColorLens
        CODING -> Icons.Default.Code
        PRECISION -> Icons.Default.Verified
    }

    val titleBn: String get() = banglaTitle
    val icon: String get() = when (this) {
        QUICK -> "⚡"
        DEEP_THINK -> "🧠"
        RESEARCH -> "🌐"
        CREATIVE -> "🎨"
        CODING -> "💻"
        PRECISION -> "🎯"
    }
    val systemInstructionOffset: String get() = systemPromptSuffix

    companion object {
        fun fromId(id: String): AnswerMode = entries.firstOrNull { it.id == id } ?: QUICK
    }
}
