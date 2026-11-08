package com.example.data.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.preferences.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class OrbAnimationPreset(val displayName: String, val description: String) {
    SIRI("Siri-Inspired", "ফ্লুইড অর্গানিক এনার্জি ওভ"),
    PLASMA("Plasma Core", "উচ্চ শক্তির প্লাজমা ভর্টেক্স"),
    GALAXY("Galaxy Core", "ঘূর্ণায়মান গ্যালাক্সি ও তারা কণা"),
    CYBERNETIC("Cybernetic HUD", "ফিউচারিস্টিক সার্কিট ও টেক রিং"),
    MINIMAL("Minimal Pulse", "শান্ত ও পরিচ্ছন্ন ব্রিদিং পালস"),
    LIQUID("Liquid Gradient", "নমনীয় বায়ো-লিকুইড এনার্জি"),
    HOLOGRAPHIC("Holographic Arc", "বহুবর্ণিল হলোগ্রাফিক স্পেকট্রাম")
}

enum class BackgroundStylePreset(val displayName: String) {
    FLUID_MESH("Fluid Atmospheric Mesh"),
    GRADIENT("Dynamic Linear Gradient"),
    SOLID("Solid Luxury Matte")
}

enum class TypographyPreset(val displayName: String) {
    DEFAULT("Modern UI Standard"),
    MONOSPACE("Sci-Fi Monospace"),
    SANS_SERIF("Clean Sans-Serif")
}

data class ThemeConfig(
    val id: String = "jarvis_signature",
    val name: String = "JARVIS Signature",
    val backgroundColorHex: String = "#070A12",
    val surfaceColorHex: String = "#0D1422",
    val primaryAccentHex: String = "#00D9FF",
    val secondaryAccentHex: String = "#8B5CF6",
    val tertiaryAccentHex: String = "#A5F3FC",
    val textPrimaryHex: String = "#F5F7FA",
    val textSecondaryHex: String = "#8995A7",
    val cardBgHex: String = "#111A2D",
    val borderGlowHex: String = "#3300D9FF",
    val orbStyle: OrbAnimationPreset = OrbAnimationPreset.SIRI,
    val orbSizeDp: Int = 180,
    val orbGlowIntensity: Float = 1.0f,
    val animationSpeedMultiplier: Float = 1.0f,
    val particleIntensity: Float = 1.0f,
    val backgroundStyle: BackgroundStylePreset = BackgroundStylePreset.FLUID_MESH,
    val cardCornerRadiusDp: Int = 16,
    val fontScale: Float = 1.0f,
    val typographyPreset: TypographyPreset = TypographyPreset.DEFAULT,
    val ambientParticlesEnabled: Boolean = true
) {
    val backgroundColor: Color get() = parseColor(backgroundColorHex, Color(0xFF070A12))
    val surfaceColor: Color get() = parseColor(surfaceColorHex, Color(0xFF0D1422))
    val primaryAccent: Color get() = parseColor(primaryAccentHex, Color(0xFF00D9FF))
    val secondaryAccent: Color get() = parseColor(secondaryAccentHex, Color(0xFF8B5CF6))
    val tertiaryAccent: Color get() = parseColor(tertiaryAccentHex, Color(0xFFA5F3FC))
    val textPrimary: Color get() = parseColor(textPrimaryHex, Color(0xFFF5F7FA))
    val textSecondary: Color get() = parseColor(textSecondaryHex, Color(0xFF8995A7))
    val cardBg: Color get() = parseColor(cardBgHex, Color(0xCC111A2D))
    val borderGlow: Color get() = parseColor(borderGlowHex, Color(0x3300D9FF))

    fun toJsonString(): String {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("backgroundColorHex", backgroundColorHex)
        obj.put("surfaceColorHex", surfaceColorHex)
        obj.put("primaryAccentHex", primaryAccentHex)
        obj.put("secondaryAccentHex", secondaryAccentHex)
        obj.put("tertiaryAccentHex", tertiaryAccentHex)
        obj.put("textPrimaryHex", textPrimaryHex)
        obj.put("textSecondaryHex", textSecondaryHex)
        obj.put("cardBgHex", cardBgHex)
        obj.put("borderGlowHex", borderGlowHex)
        obj.put("orbStyle", orbStyle.name)
        obj.put("orbSizeDp", orbSizeDp)
        obj.put("orbGlowIntensity", orbGlowIntensity.toDouble())
        obj.put("animationSpeedMultiplier", animationSpeedMultiplier.toDouble())
        obj.put("particleIntensity", particleIntensity.toDouble())
        obj.put("backgroundStyle", backgroundStyle.name)
        obj.put("cardCornerRadiusDp", cardCornerRadiusDp)
        obj.put("fontScale", fontScale.toDouble())
        obj.put("typographyPreset", typographyPreset.name)
        obj.put("ambientParticlesEnabled", ambientParticlesEnabled)
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): ThemeConfig {
            return try {
                val obj = JSONObject(jsonStr)
                ThemeConfig(
                    id = obj.optString("id", "custom"),
                    name = obj.optString("name", "Custom Profile"),
                    backgroundColorHex = obj.optString("backgroundColorHex", "#070A12"),
                    surfaceColorHex = obj.optString("surfaceColorHex", "#0D1422"),
                    primaryAccentHex = obj.optString("primaryAccentHex", "#00D9FF"),
                    secondaryAccentHex = obj.optString("secondaryAccentHex", "#8B5CF6"),
                    tertiaryAccentHex = obj.optString("tertiaryAccentHex", "#A5F3FC"),
                    textPrimaryHex = obj.optString("textPrimaryHex", "#F5F7FA"),
                    textSecondaryHex = obj.optString("textSecondaryHex", "#8995A7"),
                    cardBgHex = obj.optString("cardBgHex", "#111A2D"),
                    borderGlowHex = obj.optString("borderGlowHex", "#3300D9FF"),
                    orbStyle = safeEnum(obj.optString("orbStyle"), OrbAnimationPreset.SIRI),
                    orbSizeDp = obj.optInt("orbSizeDp", 180),
                    orbGlowIntensity = obj.optDouble("orbGlowIntensity", 1.0).toFloat(),
                    animationSpeedMultiplier = obj.optDouble("animationSpeedMultiplier", 1.0).toFloat(),
                    particleIntensity = obj.optDouble("particleIntensity", 1.0).toFloat(),
                    backgroundStyle = safeEnum(obj.optString("backgroundStyle"), BackgroundStylePreset.FLUID_MESH),
                    cardCornerRadiusDp = obj.optInt("cardCornerRadiusDp", 16),
                    fontScale = obj.optDouble("fontScale", 1.0).toFloat(),
                    typographyPreset = safeEnum(obj.optString("typographyPreset"), TypographyPreset.DEFAULT),
                    ambientParticlesEnabled = obj.optBoolean("ambientParticlesEnabled", true)
                )
            } catch (e: Exception) {
                PresetThemes.JARVIS_SIGNATURE
            }
        }

        private inline fun <reified T : Enum<T>> safeEnum(value: String, default: T): T {
            return try {
                enumValueOf<T>(value)
            } catch (e: Exception) {
                default
            }
        }

        private fun parseColor(hex: String, fallback: Color): Color {
            return try {
                val cleanHex = hex.trim().replace("#", "")
                val longVal = when (cleanHex.length) {
                    6 -> ("FF$cleanHex").toLong(16)
                    8 -> cleanHex.toLong(16)
                    else -> return fallback
                }
                Color(longVal)
            } catch (e: Exception) {
                fallback
            }
        }
    }
}

object PresetThemes {
    val JARVIS_SIGNATURE = ThemeConfig(
        id = "jarvis_signature",
        name = "JARVIS Signature",
        backgroundColorHex = "#070A12",
        surfaceColorHex = "#0D1422",
        primaryAccentHex = "#00D9FF",
        secondaryAccentHex = "#8B5CF6",
        tertiaryAccentHex = "#A5F3FC",
        textPrimaryHex = "#F5F7FA",
        textSecondaryHex = "#8995A7",
        cardBgHex = "#121A2D",
        borderGlowHex = "#3300D9FF",
        orbStyle = OrbAnimationPreset.SIRI
    )

    val IRON_CORE = ThemeConfig(
        id = "iron_core",
        name = "Iron Core",
        backgroundColorHex = "#0A0A0C",
        surfaceColorHex = "#1C1712",
        primaryAccentHex = "#FFB300",
        secondaryAccentHex = "#FF5722",
        tertiaryAccentHex = "#FFE082",
        textPrimaryHex = "#FFF8E7",
        textSecondaryHex = "#BCAAA4",
        cardBgHex = "#261E16",
        borderGlowHex = "#40FFB300",
        orbStyle = OrbAnimationPreset.PLASMA
    )

    val CYBERPUNK = ThemeConfig(
        id = "cyberpunk",
        name = "Cyberpunk",
        backgroundColorHex = "#0B0314",
        surfaceColorHex = "#19082B",
        primaryAccentHex = "#00E5FF",
        secondaryAccentHex = "#E040FB",
        tertiaryAccentHex = "#FF007F",
        textPrimaryHex = "#FAEAFF",
        textSecondaryHex = "#BA96CA",
        cardBgHex = "#250E3E",
        borderGlowHex = "#4400E5FF",
        orbStyle = OrbAnimationPreset.CYBERNETIC
    )

    val MIDNIGHT_GALAXY = ThemeConfig(
        id = "midnight_galaxy",
        name = "Midnight Galaxy",
        backgroundColorHex = "#040510",
        surfaceColorHex = "#0C0E26",
        primaryAccentHex = "#7C4DFF",
        secondaryAccentHex = "#2979FF",
        tertiaryAccentHex = "#E040FB",
        textPrimaryHex = "#F0F2FF",
        textSecondaryHex = "#8C96C6",
        cardBgHex = "#141738",
        borderGlowHex = "#357C4DFF",
        orbStyle = OrbAnimationPreset.GALAXY
    )

    val ARCTIC_GLASS = ThemeConfig(
        id = "arctic_glass",
        name = "Arctic Glass",
        backgroundColorHex = "#050F1A",
        surfaceColorHex = "#0C2033",
        primaryAccentHex = "#00E5FF",
        secondaryAccentHex = "#80D8FF",
        tertiaryAccentHex = "#A5F3FC",
        textPrimaryHex = "#E0F7FA",
        textSecondaryHex = "#80B3C4",
        cardBgHex = "#122A42",
        borderGlowHex = "#4000E5FF",
        orbStyle = OrbAnimationPreset.HOLOGRAPHIC
    )

    val LUXURY_BLACK = ThemeConfig(
        id = "luxury_black",
        name = "Luxury Black",
        backgroundColorHex = "#030305",
        surfaceColorHex = "#0E0E12",
        primaryAccentHex = "#FFD700",
        secondaryAccentHex = "#CFD8DC",
        tertiaryAccentHex = "#FFF9C4",
        textPrimaryHex = "#FFFFFF",
        textSecondaryHex = "#9E9E9E",
        cardBgHex = "#16161D",
        borderGlowHex = "#30FFD700",
        orbStyle = OrbAnimationPreset.MINIMAL
    )

    val MINIMAL_AI = ThemeConfig(
        id = "minimal_ai",
        name = "Minimal AI",
        backgroundColorHex = "#0E1117",
        surfaceColorHex = "#161B22",
        primaryAccentHex = "#E6EDF3",
        secondaryAccentHex = "#58A6FF",
        tertiaryAccentHex = "#79C0FF",
        textPrimaryHex = "#F0F6FC",
        textSecondaryHex = "#8B949E",
        cardBgHex = "#1D232C",
        borderGlowHex = "#2558A6FF",
        orbStyle = OrbAnimationPreset.MINIMAL
    )

    val HOLOGRAPHIC = ThemeConfig(
        id = "holographic",
        name = "Holographic",
        backgroundColorHex = "#06081A",
        surfaceColorHex = "#0E1333",
        primaryAccentHex = "#00FFE0",
        secondaryAccentHex = "#FF4081",
        tertiaryAccentHex = "#7C4DFF",
        textPrimaryHex = "#F4F5FF",
        textSecondaryHex = "#9EA7D6",
        cardBgHex = "#161D4A",
        borderGlowHex = "#4400FFE0",
        orbStyle = OrbAnimationPreset.LIQUID
    )

    val ALL_PRESETS = listOf(
        JARVIS_SIGNATURE,
        IRON_CORE,
        CYBERPUNK,
        MIDNIGHT_GALAXY,
        ARCTIC_GLASS,
        LUXURY_BLACK,
        MINIMAL_AI,
        HOLOGRAPHIC
    )
}

class ThemeManager(private val context: Context) {

    private val KEY_ACTIVE_THEME = stringPreferencesKey("active_theme_config_json")
    private val KEY_CUSTOM_PROFILES = stringPreferencesKey("custom_profiles_json")

    private val _currentTheme = MutableStateFlow(PresetThemes.JARVIS_SIGNATURE)
    val currentTheme: StateFlow<ThemeConfig> = _currentTheme.asStateFlow()

    private val _customProfiles = MutableStateFlow<List<ThemeConfig>>(emptyList())
    val customProfiles: StateFlow<List<ThemeConfig>> = _customProfiles.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        loadSavedTheme()
    }

    private fun loadSavedTheme() {
        scope.launch {
            context.dataStore.data.collect { prefs ->
                val themeJson = prefs[KEY_ACTIVE_THEME]
                if (!themeJson.isNullOrBlank()) {
                    _currentTheme.value = ThemeConfig.fromJson(themeJson)
                }

                val profilesJson = prefs[KEY_CUSTOM_PROFILES]
                if (!profilesJson.isNullOrBlank()) {
                    _customProfiles.value = parseProfiles(profilesJson)
                }
            }
        }
    }

    fun applyTheme(theme: ThemeConfig) {
        _currentTheme.value = theme
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_ACTIVE_THEME] = theme.toJsonString()
            }
        }
    }

    fun saveCustomProfile(profile: ThemeConfig) {
        val updatedList = _customProfiles.value.filter { it.id != profile.id } + profile
        _customProfiles.value = updatedList
        _currentTheme.value = profile
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_ACTIVE_THEME] = profile.toJsonString()
                prefs[KEY_CUSTOM_PROFILES] = serializeProfiles(updatedList)
            }
        }
    }

    fun deleteProfile(profileId: String) {
        val updatedList = _customProfiles.value.filter { it.id != profileId }
        _customProfiles.value = updatedList
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_CUSTOM_PROFILES] = serializeProfiles(updatedList)
            }
        }
        if (_currentTheme.value.id == profileId) {
            applyTheme(PresetThemes.JARVIS_SIGNATURE)
        }
    }

    fun resetToDefault() {
        applyTheme(PresetThemes.JARVIS_SIGNATURE)
    }

    private fun serializeProfiles(list: List<ThemeConfig>): String {
        val array = JSONArray()
        list.forEach { array.put(JSONObject(it.toJsonString())) }
        return array.toString()
    }

    private fun parseProfiles(jsonStr: String): List<ThemeConfig> {
        val list = mutableListOf<ThemeConfig>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val itemObj = array.getJSONObject(i)
                list.add(ThemeConfig.fromJson(itemObj.toString()))
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }
}
