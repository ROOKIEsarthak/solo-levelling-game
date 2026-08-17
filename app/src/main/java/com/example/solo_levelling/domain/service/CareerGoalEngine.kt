package com.example.solo_levelling.domain.service

data class CareerGoalAssessment(
    val nextGoalTitle: String,
    val reason: String,
    val mandatoryAreas: List<String>,
    val recommendedAreas: List<String>,
    val currentLevelLabel: String,
    val targetLevelLabel: String,
)

object CareerGoalEngine {
    fun assess(
        experienceBand: String,
        currentRole: String,
        targetRole: String,
        yearsExperience: Double,
        dsaConfidence: Int,
        sdConfidence: Int,
    ): CareerGoalAssessment {
        val band = normalizeBand(experienceBand)
        val current = normalizeRole(currentRole, band, yearsExperience)
        val target = normalizeRole(targetRole, band, yearsExperience)
        val gap = roleRank(target) - roleRank(current)

        return when {
            band == ExperienceBand.STUDENT -> studentAssessment(target)
            gap <= 0 -> maintenanceAssessment(current, target, dsaConfidence, sdConfidence)
            target >= RoleLevel.SENIOR -> seniorTrackAssessment(current, target, dsaConfidence, sdConfidence)
            target == RoleLevel.SDE2 -> sde2ReadinessAssessment(current, dsaConfidence, sdConfidence)
            target == RoleLevel.SDE1 -> sde1ReadinessAssessment(band, dsaConfidence, sdConfidence)
            else -> defaultProgressAssessment(current, target, dsaConfidence, sdConfidence)
        }
    }

    private fun studentAssessment(target: RoleLevel): CareerGoalAssessment {
        val targetLabel = roleLabel(target)
        return CareerGoalAssessment(
            nextGoalTitle = "SDE1 Interview Readiness",
            reason = "Build fundamentals before targeting $targetLabel roles.",
            mandatoryAreas = listOf("DSA", "Backend Basics"),
            recommendedAreas = listOf("Behavioral", "System Design"),
            currentLevelLabel = "Student / Beginner",
            targetLevelLabel = targetLabel,
        )
    }

    private fun sde1ReadinessAssessment(
        band: String,
        dsaConfidence: Int,
        sdConfidence: Int,
    ): CareerGoalAssessment {
        val mandatory = mutableListOf("DSA", "Backend")
        val recommended = mutableListOf("Behavioral")
        if (sdConfidence < 40) recommended.add(0, "System Design")
        if (dsaConfidence < 50) mandatory.add(0, "DSA Fundamentals")

        return CareerGoalAssessment(
            nextGoalTitle = "SDE1 Interview Readiness",
            reason = "Close skill gaps for your first software engineering role.",
            mandatoryAreas = mandatory.distinct(),
            recommendedAreas = recommended.distinct(),
            currentLevelLabel = bandLabel(band),
            targetLevelLabel = "SDE1",
        )
    }

    private fun sde2ReadinessAssessment(
        current: RoleLevel,
        dsaConfidence: Int,
        sdConfidence: Int,
    ): CareerGoalAssessment {
        val mandatory = listOf("DSA", "System Design", "Backend")
        val recommended = buildList {
            add("Behavioral")
            if (dsaConfidence >= 70 && sdConfidence < 60) add("System Design Deep Dive")
            if (sdConfidence >= 60 && dsaConfidence < 60) add("DSA Patterns")
        }

        return CareerGoalAssessment(
            nextGoalTitle = "SDE2 Interview Readiness",
            reason = "Mid-level interviews expect strong DSA, system design, and backend depth.",
            mandatoryAreas = mandatory,
            recommendedAreas = recommended,
            currentLevelLabel = roleLabel(current),
            targetLevelLabel = "SDE2",
        )
    }

    private fun seniorTrackAssessment(
        current: RoleLevel,
        target: RoleLevel,
        dsaConfidence: Int,
        sdConfidence: Int,
    ): CareerGoalAssessment {
        val mandatory = listOf("System Design", "Behavioral", "Leadership")
        val recommended = buildList {
            add("Architecture")
            if (dsaConfidence < 75) add("DSA")
            if (sdConfidence < 70) add("Large-Scale Design")
            if (target >= RoleLevel.STAFF) add("Cross-Team Influence")
        }

        return CareerGoalAssessment(
            nextGoalTitle = "${roleLabel(target)} Readiness",
            reason = "Senior+ loops emphasize design ownership, trade-offs, and leadership signals.",
            mandatoryAreas = mandatory,
            recommendedAreas = recommended.distinct(),
            currentLevelLabel = roleLabel(current),
            targetLevelLabel = roleLabel(target),
        )
    }

    private fun maintenanceAssessment(
        current: RoleLevel,
        target: RoleLevel,
        dsaConfidence: Int,
        sdConfidence: Int,
    ): CareerGoalAssessment {
        val weakest = minOf(dsaConfidence, sdConfidence)
        val focus = when {
            weakest == dsaConfidence -> "DSA"
            else -> "System Design"
        }
        return CareerGoalAssessment(
            nextGoalTitle = "Maintain ${roleLabel(current)} Edge",
            reason = "You are at or above your stated target; sharpen $focus to stay interview-ready.",
            mandatoryAreas = listOf(focus),
            recommendedAreas = listOf("Behavioral", "Backend"),
            currentLevelLabel = roleLabel(current),
            targetLevelLabel = roleLabel(target),
        )
    }

    private fun defaultProgressAssessment(
        current: RoleLevel,
        target: RoleLevel,
        dsaConfidence: Int,
        sdConfidence: Int,
    ): CareerGoalAssessment {
        val mandatory = buildList {
            add("DSA")
            add("Backend")
            if (roleRank(target) >= roleRank(RoleLevel.SDE2)) add("System Design")
        }
        val recommended = buildList {
            add("Behavioral")
            if (dsaConfidence < 60) add("DSA")
            if (sdConfidence < 50) add("System Design")
        }

        return CareerGoalAssessment(
            nextGoalTitle = "${roleLabel(target)} Progress",
            reason = "Steady progress toward ${roleLabel(target)} from ${roleLabel(current)}.",
            mandatoryAreas = mandatory.distinct(),
            recommendedAreas = recommended.distinct(),
            currentLevelLabel = roleLabel(current),
            targetLevelLabel = roleLabel(target),
        )
    }

    private enum class RoleLevel {
        STUDENT,
        SDE1,
        SDE2,
        SDE3,
        SENIOR,
        STAFF,
        PRINCIPAL,
    }

    private object ExperienceBand {
        const val STUDENT = "student"
        const val ZERO_TO_ONE = "0-1"
        const val ONE_TO_TWO = "1-2"
        const val TWO_TO_THREE = "2-3"
        const val THREE_TO_FIVE = "3-5"
        const val FIVE_PLUS = "5+"
    }

    private fun normalizeBand(raw: String): String {
        val key = raw.trim().lowercase().replace("_", " ").replace("/", " ")
        return when {
            key.contains("student") || key.contains("beginner") -> ExperienceBand.STUDENT
            key.contains("0") && key.contains("1") -> ExperienceBand.ZERO_TO_ONE
            key.contains("1") && key.contains("2") -> ExperienceBand.ONE_TO_TWO
            key.contains("2") && key.contains("3") -> ExperienceBand.TWO_TO_THREE
            key.contains("3") && key.contains("5") -> ExperienceBand.THREE_TO_FIVE
            key.contains("5+") || key.contains("5 plus") || key.contains("5 or more") -> ExperienceBand.FIVE_PLUS
            else -> key
        }
    }

    private fun normalizeRole(raw: String, band: String, yearsExperience: Double): RoleLevel {
        val key = raw.trim().lowercase()
        return when {
            key.isBlank() || key.contains("student") || key.contains("intern") -> RoleLevel.STUDENT
            key.contains("principal") -> RoleLevel.PRINCIPAL
            key.contains("staff") -> RoleLevel.STAFF
            key.contains("senior") || key == "sde3" || key.contains("sde 3") -> RoleLevel.SENIOR
            key.contains("sde2") || key.contains("sde 2") || key.contains("mid") -> RoleLevel.SDE2
            key.contains("sde1") || key.contains("sde 1") || key.contains("junior") || key.contains("entry") -> RoleLevel.SDE1
            band == ExperienceBand.FIVE_PLUS || yearsExperience >= 5 -> RoleLevel.SENIOR
            band == ExperienceBand.THREE_TO_FIVE || yearsExperience >= 3 -> RoleLevel.SDE2
            band == ExperienceBand.TWO_TO_THREE || yearsExperience >= 2 -> RoleLevel.SDE1
            else -> RoleLevel.STUDENT
        }
    }

    private fun roleRank(role: RoleLevel): Int = when (role) {
        RoleLevel.STUDENT -> 0
        RoleLevel.SDE1 -> 1
        RoleLevel.SDE2 -> 2
        RoleLevel.SDE3 -> 3
        RoleLevel.SENIOR -> 4
        RoleLevel.STAFF -> 5
        RoleLevel.PRINCIPAL -> 6
    }

    private fun roleLabel(role: RoleLevel): String = when (role) {
        RoleLevel.STUDENT -> "Student"
        RoleLevel.SDE1 -> "SDE1"
        RoleLevel.SDE2 -> "SDE2"
        RoleLevel.SDE3 -> "SDE3"
        RoleLevel.SENIOR -> "Senior"
        RoleLevel.STAFF -> "Staff"
        RoleLevel.PRINCIPAL -> "Principal"
    }

    private fun bandLabel(band: String): String = when (band) {
        ExperienceBand.STUDENT -> "Student / Beginner"
        ExperienceBand.ZERO_TO_ONE -> "0-1 years"
        ExperienceBand.ONE_TO_TWO -> "1-2 years"
        ExperienceBand.TWO_TO_THREE -> "2-3 years"
        ExperienceBand.THREE_TO_FIVE -> "3-5 years"
        ExperienceBand.FIVE_PLUS -> "5+ years"
        else -> band
    }
}
