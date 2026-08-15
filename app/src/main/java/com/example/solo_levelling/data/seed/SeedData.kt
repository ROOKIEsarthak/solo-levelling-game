package com.example.solo_levelling.data.seed

import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.domain.model.AttributeCode

object SeedData {
    fun defaultTemplates(): List<QuestTemplateEntity> = listOf(
        QuestTemplateEntity(
            key = "dsa_daily",
            type = "DAILY",
            title = "Solve 2 DSA problems",
            description = "Real problem solving on any platform",
            baseXp = 40,
            attributeRewardsJson = rewards(AttributeCode.INT to 30, AttributeCode.DISC to 10),
            scheduleDaysCsv = "1,2,3,4,5",
        ),
        QuestTemplateEntity(
            key = "workout_daily",
            type = "DAILY",
            title = "Complete workout",
            description = "Strength or cardio session",
            baseXp = 50,
            attributeRewardsJson = rewards(
                AttributeCode.STR to 35,
                AttributeCode.DISC to 10,
                AttributeCode.VIT to 5,
            ),
            scheduleDaysCsv = "1,2,3,4,5,6",
        ),
        QuestTemplateEntity(
            key = "deep_work",
            type = "DAILY",
            title = "90 min deep work",
            description = "Distraction-free focused work",
            baseXp = 45,
            attributeRewardsJson = rewards(AttributeCode.FOC to 30, AttributeCode.DISC to 15),
            scheduleDaysCsv = "1,2,3,4,5",
        ),
        QuestTemplateEntity(
            key = "steps",
            type = "DAILY",
            title = "Hit step target",
            description = "Move your body today",
            baseXp = 25,
            attributeRewardsJson = rewards(AttributeCode.END to 20, AttributeCode.VIT to 5),
            scheduleDaysCsv = "1,2,3,4,5,6,7",
        ),
        QuestTemplateEntity(
            key = "journal",
            type = "DAILY",
            title = "Write a short journal",
            description = "Reflect on mistakes and wins",
            baseXp = 20,
            attributeRewardsJson = rewards(AttributeCode.WIS to 15, AttributeCode.DISC to 5),
            scheduleDaysCsv = "1,2,3,4,5,6,7",
        ),
        QuestTemplateEntity(
            key = "weekly_review",
            type = "WEEKLY",
            title = "Weekly system review",
            description = "Review the week and plan next",
            baseXp = 100,
            attributeRewardsJson = rewards(AttributeCode.WIS to 60, AttributeCode.DISC to 40),
            scheduleDaysCsv = "7",
        ),
        QuestTemplateEntity(
            key = "recovery",
            type = "RECOVERY",
            title = "Recovery quest",
            description = "Complete one primary quest to restore momentum",
            baseXp = 15,
            attributeRewardsJson = rewards(AttributeCode.DISC to 15),
            scheduleDaysCsv = "",
            active = false,
        ),
    )

    fun achievements(): List<AchievementDefEntity> = listOf(
        AchievementDefEntity("FIRST_QUEST", "First Quest", "Complete your first quest", "QUESTS_COMPLETED", 1, 10),
        AchievementDefEntity("SEVEN_DAY_HUNTER", "7 Day Hunter", "Reach a 7-day streak", "STREAK", 7, 50),
        AchievementDefEntity("THIRTY_DAY_HUNTER", "30 Day Hunter", "Reach a 30-day streak", "STREAK", 30, 150),
        AchievementDefEntity("PROBLEM_SLAYER", "Problem Slayer", "Solve 50 DSA problems", "DSA_SOLVED", 50, 100),
        AchievementDefEntity("IRON_WILL", "Iron Will", "Complete 50 quests", "QUESTS_COMPLETED", 50, 100),
        AchievementDefEntity("PERFECT_WEEK", "Perfect Week", "Complete all quests in a week", "PERFECT_WEEK", 1, 80),
        AchievementDefEntity("BOSS_SLAYER", "Boss Slayer", "Clear a boss quest", "BOSS_CLEARED", 1, 120),
        AchievementDefEntity("S_RANK", "S Rank", "Reach level 51", "LEVEL", 51, 200),
    )

    private fun rewards(vararg pairs: Pair<AttributeCode, Int>): String =
        pairs.joinToString(prefix = "{", postfix = "}") { "\"${it.first.name}\":${it.second}" }
}
