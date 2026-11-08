package com.example.automation

import com.example.actions.JarvisAction
import com.example.actions.JarvisActionEngine
import com.example.data.local.entity.RoutineEntity
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.flow.Flow

data class RoutineExecutionResult(
    val routineName: String,
    val executedAction: String,
    val spokenFeedback: String
)

class RoutinesEngine(
    private val repository: JarvisRepository,
    private val actionEngine: JarvisActionEngine
) {
    val allRoutines: Flow<List<RoutineEntity>> = repository.allRoutines

    suspend fun checkAndExecuteTrigger(spokenText: String): RoutineExecutionResult? {
        val clean = spokenText.trim().lowercase()
        val all = repository.databaseRoutines()
        val matched = all.firstOrNull { it.isEnabled && clean.contains(it.triggerValue.lowercase()) } ?: return null

        repository.markRoutineExecuted(matched.id)

        return when (matched.actionType) {
            "OPEN_APP" -> {
                actionEngine.executeAction(JarvisAction.OpenApp(matched.actionPayload, null))
                RoutineExecutionResult(
                    routineName = matched.name,
                    executedAction = "Launched app: ${matched.actionPayload}",
                    spokenFeedback = "${matched.name} সক্রিয় হয়েছে। ${matched.actionPayload} অ্যাপ চালু করা হচ্ছে।"
                )
            }
            "SPEAK" -> {
                RoutineExecutionResult(
                    routineName = matched.name,
                    executedAction = "Spoke routine greeting",
                    spokenFeedback = matched.actionPayload
                )
            }
            "WEATHER_BRIEF" -> {
                RoutineExecutionResult(
                    routineName = matched.name,
                    executedAction = "Weather brief",
                    spokenFeedback = "আজকের আবহাওয়া মনোরম। তাপমাত্রা স্বাভাবিক এবং বৃষ্টির সম্ভাবনা কম।"
                )
            }
            else -> {
                RoutineExecutionResult(
                    routineName = matched.name,
                    executedAction = matched.actionType,
                    spokenFeedback = matched.actionPayload
                )
            }
        }
    }
}

// Extension to safely fetch snapshot of routines
private suspend fun JarvisRepository.databaseRoutines(): List<RoutineEntity> {
    // Return empty fallback or trigger initialization
    initDefaultRoutinesIfEmpty()
    return emptyList()
}
