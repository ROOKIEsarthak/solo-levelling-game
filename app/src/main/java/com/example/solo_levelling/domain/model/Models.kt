package com.example.solo_levelling.domain.model

enum class AttributeCode {
    STR, END, INT, VIT, DISC, FOC, WIS
}

enum class QuestStatus {
    AVAILABLE, IN_PROGRESS, COMPLETED, MISSED, LOCKED
}

enum class QuestType {
    DAILY, WEEKLY, MILESTONE, BOSS, RECOVERY
}

enum class VerificationType {
    MANUAL, TIMER, COUNT, METRIC_THRESHOLD, AUTOMATIC
}

data class AttributeDelta(
    val code: AttributeCode,
    val amount: Int,
)
