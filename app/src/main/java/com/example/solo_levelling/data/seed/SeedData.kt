package com.example.solo_levelling.data.seed

import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.SystemDesignConceptEntity
import com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity
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
            verificationType = "COUNT",
            verificationTarget = 2f,
            priorityTags = "module_career",
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
            priorityTags = "module_workout",
        ),
        QuestTemplateEntity(
            key = "deep_work",
            type = "DAILY",
            title = "90 min deep work",
            description = "Distraction-free focused work",
            baseXp = 45,
            attributeRewardsJson = rewards(AttributeCode.FOC to 30, AttributeCode.DISC to 15),
            scheduleDaysCsv = "1,2,3,4,5",
            verificationType = "TIMER",
            verificationTarget = 90f,
        ),
        QuestTemplateEntity(
            key = "steps",
            type = "DAILY",
            title = "Hit step target",
            description = "Move your body today",
            baseXp = 25,
            attributeRewardsJson = rewards(AttributeCode.END to 20, AttributeCode.VIT to 5),
            scheduleDaysCsv = "1,2,3,4,5,6,7",
            verificationType = "METRIC_THRESHOLD",
            verificationTarget = 10000f,
            verificationUnit = "STEPS",
            priorityTags = "module_workout",
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
            key = "nutrition_daily",
            type = "DAILY",
            title = "Log nutrition",
            description = "Track calories and macros for the day",
            baseXp = 15,
            attributeRewardsJson = rewards(AttributeCode.VIT to 15),
            scheduleDaysCsv = "1,2,3,4,5,6,7",
            verificationType = "MANUAL",
            priorityTags = "module_diet,health,fitness",
        ),
        QuestTemplateEntity(
            key = "system_design",
            type = "WEEKLY",
            title = "System design study",
            description = "Review one system design topic or mock",
            baseXp = 80,
            attributeRewardsJson = rewards(AttributeCode.INT to 50, AttributeCode.WIS to 30),
            scheduleDaysCsv = "6",
            verificationType = "MANUAL",
            priorityTags = "module_career,career,system_design",
            difficulty = 2,
        ),
        QuestTemplateEntity(
            key = "weekly_review",
            type = "WEEKLY",
            title = "Weekly system review",
            description = "Review the week and plan next",
            baseXp = 100,
            attributeRewardsJson = rewards(AttributeCode.WIS to 60, AttributeCode.DISC to 40),
            scheduleDaysCsv = "7",
            verificationType = "AUTOMATIC",
        ),
        QuestTemplateEntity(
            key = "first_week_complete",
            type = "MILESTONE",
            title = "First week complete",
            description = "Complete your first full week of quests",
            baseXp = 150,
            attributeRewardsJson = rewards(AttributeCode.DISC to 80, AttributeCode.WIS to 70),
            scheduleDaysCsv = "",
            active = true,
        ),
        QuestTemplateEntity(
            key = "recovery",
            type = "RECOVERY",
            title = "Recovery quest",
            description = "Complete one primary quest to restore momentum",
            baseXp = 15,
            attributeRewardsJson = rewards(AttributeCode.DISC to 15),
            scheduleDaysCsv = "",
            active = true,
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

    fun careerNodes(): List<CareerNodeEntity> = listOf(
        CareerNodeEntity(track = "DSA", title = "Arrays & Hashing", orderIndex = 1, description = "Master fundamentals"),
        CareerNodeEntity(track = "DSA", title = "Trees & Graphs", orderIndex = 2, description = "Traversal and shortest paths"),
        CareerNodeEntity(track = "DSA", title = "Dynamic Programming", orderIndex = 3, description = "State transitions"),
        CareerNodeEntity(track = "Backend", title = "REST APIs", orderIndex = 1, description = "Design and implement APIs"),
        CareerNodeEntity(track = "Backend", title = "Databases", orderIndex = 2, description = "SQL, indexing, transactions"),
        CareerNodeEntity(track = "Backend", title = "Distributed Systems", orderIndex = 3, description = "Scaling and consistency"),
        CareerNodeEntity(track = "System Design", title = "Load Balancing", orderIndex = 1, description = "Traffic distribution"),
        CareerNodeEntity(track = "System Design", title = "Caching", orderIndex = 2, description = "Cache strategies"),
        CareerNodeEntity(track = "System Design", title = "Messaging Queues", orderIndex = 3, description = "Async processing"),
        CareerNodeEntity(track = "Interview", title = "Behavioral Prep", orderIndex = 1, description = "STAR stories"),
        CareerNodeEntity(track = "Interview", title = "Mock Interviews", orderIndex = 2, description = "Timed practice"),
        CareerNodeEntity(track = "Interview", title = "Offer Negotiation", orderIndex = 3, description = "Comp and leveling"),
    )

    fun dsaStarterProblems(): List<DsaProblemEntity> = listOf(
        DsaProblemEntity(title = "Two Sum", topic = "Arrays", difficulty = "EASY", externalId = "lc_1"),
        DsaProblemEntity(title = "Best Time to Buy and Sell Stock", topic = "Arrays", difficulty = "EASY", externalId = "lc_121"),
        DsaProblemEntity(title = "Valid Anagram", topic = "Strings", difficulty = "EASY", externalId = "lc_242"),
        DsaProblemEntity(title = "Longest Substring Without Repeating Characters", topic = "Strings", difficulty = "MEDIUM", externalId = "lc_3"),
        DsaProblemEntity(title = "Group Anagrams", topic = "Hashing", difficulty = "MEDIUM", externalId = "lc_49"),
        DsaProblemEntity(title = "Contains Duplicate", topic = "Hashing", difficulty = "EASY", externalId = "lc_217"),
        DsaProblemEntity(title = "Valid Palindrome", topic = "Two Pointers", difficulty = "EASY", externalId = "lc_125"),
        DsaProblemEntity(title = "3Sum", topic = "Two Pointers", difficulty = "MEDIUM", externalId = "lc_15"),
        DsaProblemEntity(title = "Maximum Average Subarray I", topic = "Sliding Window", difficulty = "EASY", externalId = "lc_643"),
        DsaProblemEntity(title = "Longest Repeating Character Replacement", topic = "Sliding Window", difficulty = "MEDIUM", externalId = "lc_424"),
        DsaProblemEntity(title = "Invert Binary Tree", topic = "Trees", difficulty = "EASY", externalId = "lc_226"),
        DsaProblemEntity(title = "Number of Islands", topic = "Graphs", difficulty = "MEDIUM", externalId = "lc_200"),
        DsaProblemEntity(title = "Climbing Stairs", topic = "DP", difficulty = "EASY", externalId = "lc_70"),
    )

    fun systemDesignTopics(): List<SystemDesignTopicEntity> = listOf(
        sdTopic("fundamentals", "Fundamentals", 1, listOf(
            "Requirements gathering", "Capacity estimation", "API basics", "High-level design",
        )),
        sdTopic("api_design", "API Design", 2, listOf(
            "REST vs RPC", "Versioning", "Pagination", "Rate limiting",
        )),
        sdTopic("databases", "Databases", 3, listOf(
            "SQL vs NoSQL", "Indexing", "Replication", "Sharding",
        )),
        sdTopic("caching", "Caching", 4, listOf(
            "Cache-aside", "Write-through", "TTL & eviction", "CDN caching",
        )),
        sdTopic("queues", "Queues", 5, listOf(
            "Message brokers", "At-least-once delivery", "Dead letter queues", "Backpressure",
        )),
        sdTopic("scaling", "Scaling", 6, listOf(
            "Vertical vs horizontal", "Load balancing", "Auto-scaling", "Stateless services",
        )),
        sdTopic("distributed", "Distributed Systems", 7, listOf(
            "CAP theorem", "Consistency models", "Leader election", "Distributed tracing",
        )),
    )

    private fun sdTopic(id: String, title: String, order: Int, concepts: List<String>): SystemDesignTopicEntity =
        SystemDesignTopicEntity(
            id = id,
            title = title,
            orderIndex = order,
            concepts = concepts.mapIndexed { idx, name ->
                SystemDesignConceptEntity(id = "${id}_c$idx", title = name)
            },
        )

    private fun rewards(vararg pairs: Pair<AttributeCode, Int>): String =
        pairs.joinToString(prefix = "{", postfix = "}") { "\"${it.first.name}\":${it.second}" }
}
