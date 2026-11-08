package com.example.ai.skills

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val Context.skillsDataStore by preferencesDataStore(name = "jarvis_skills_registry")

data class JarvisSkill(
    val id: String,
    val name: String,
    val banglaName: String,
    val category: String,
    val description: String,
    val version: String = "1.0.0",
    val author: String = "JARVIS Labs",
    val permissions: List<String>,
    val isEnabled: Boolean = true,
    val iconName: String
)

class SkillsEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val defaultSkills = listOf(
        JarvisSkill(
            id = "skill_calc",
            name = "Calculator & Math Engine",
            banglaName = "গণিত ও হিসাব স্কিল",
            category = "Productivity",
            description = "জটিল গাণিতিক হিসাব, শতকরা, রূপান্তর ও পরিসংখ্যান বিশ্লেষণ সমাধান করে।",
            permissions = emptyList(),
            iconName = "Calculate"
        ),
        JarvisSkill(
            id = "skill_translate",
            name = "Bengali & English Linguistics",
            banglaName = "ভাষা ও অনুবাদ স্কিল",
            category = "Linguistics",
            description = "বাংলিশ (Romanized Bangla), প্রমিত বাংলা ও ইংরেজির মধ্যে নিখুঁত অনুবাদ ও ব্যাকরণ সংশোধন।",
            permissions = emptyList(),
            iconName = "Translate"
        ),
        JarvisSkill(
            id = "skill_research",
            name = "Live Web Research & Fact Search",
            banglaName = "ওয়েব রিসার্চ স্কিল",
            category = "Information",
            description = "ইন্টারনেট থেকে সরাসরি তথ্য অনুসন্ধান, খবরের সারসংক্ষেপ ও উৎস লিংক প্রদান।",
            permissions = listOf("INTERNET"),
            iconName = "Search"
        ),
        JarvisSkill(
            id = "skill_coding",
            name = "Android & Kotlin Architect",
            banglaName = "কোডিং ও অ্যাপ স্কিল",
            category = "Development",
            description = "জেটপ্যাক কম্পোজ, আধুনিক কোটলিন কোড ডিজাইন ও আর্কিটেকচার পরামর্শ।",
            permissions = emptyList(),
            iconName = "Code"
        ),
        JarvisSkill(
            id = "skill_weather",
            name = "Weather & Environmental Intel",
            banglaName = "আবহাওয়া বার্তা স্কিল",
            category = "Utilities",
            description = "তাপমাত্রা, আর্দ্রতা ও আবহাওয়ার পূর্বাভাস উপস্থাপন।",
            permissions = listOf("INTERNET"),
            iconName = "WbSunny"
        ),
        JarvisSkill(
            id = "skill_docs",
            name = "Document & File Reader",
            banglaName = "ডকুমেন্ট ও ফাইল স্কিল",
            category = "Productivity",
            description = "টেক্সট, পিডিএফ ও ডকুমেন্টের সারসংক্ষেপ তৈরি এবং প্রশ্নোত্তর।",
            permissions = listOf("STORAGE_ACCESS_FRAMEWORK"),
            iconName = "Description"
        ),
        JarvisSkill(
            id = "skill_automation",
            name = "Device Automation & Routines",
            banglaName = "অটোমেশন ও রুটিন স্কিল",
            category = "Automation",
            description = "ভয়েস কমান্ডের মাধ্যমে অ্যাপ লঞ্চ, রুটিন সক্রিয়করণ ও সিস্টেম নিয়ন্ত্রণ।",
            permissions = listOf("DEVICE_INTENTS"),
            iconName = "AutoFixHigh"
        )
    )

    private val _skills = MutableStateFlow<List<JarvisSkill>>(defaultSkills)
    val skills: StateFlow<List<JarvisSkill>> = _skills.asStateFlow()

    init {
        loadSkillsState()
    }

    private fun loadSkillsState() {
        scope.launch {
            context.skillsDataStore.data.collect { prefs ->
                val updated = defaultSkills.map { skill ->
                    val key = booleanPreferencesKey("skill_${skill.id}_enabled")
                    val isEnabled = prefs[key] ?: true
                    skill.copy(isEnabled = isEnabled)
                }
                _skills.value = updated
            }
        }
    }

    fun toggleSkill(skillId: String, enabled: Boolean) {
        scope.launch {
            context.skillsDataStore.edit { prefs ->
                prefs[booleanPreferencesKey("skill_${skillId}_enabled")] = enabled
            }
            _skills.value = _skills.value.map {
                if (it.id == skillId) it.copy(isEnabled = enabled) else it
            }
        }
    }

    fun isSkillActive(skillId: String): Boolean {
        return _skills.value.find { it.id == skillId }?.isEnabled ?: true
    }
}
