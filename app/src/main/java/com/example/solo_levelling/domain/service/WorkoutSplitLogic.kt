package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.db.entity.PlannedExerciseEntity
import com.example.solo_levelling.data.db.entity.RepRangeEntity
import com.example.solo_levelling.data.db.entity.WorkoutDayPlanEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.data.seed.CatalogSplit
import com.example.solo_levelling.data.seed.WorkoutCatalog

data class SplitApplyResult(
    val routine: WorkoutRoutineEntity? = null,
    val error: String? = null,
    /** ISO weekdays (1=Mon…7=Sun) that have a workout, sorted. */
    val trainingIsoDays: List<Int> = emptyList(),
) {
    val isSuccess: Boolean get() = routine != null && error == null
}

object WorkoutSplitLogic {
    private val isoToKey = mapOf(
        1 to "monday",
        2 to "tuesday",
        3 to "wednesday",
        4 to "thursday",
        5 to "friday",
        6 to "saturday",
        7 to "sunday",
    )

    val weekdayLabels = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun",
    )

    fun parseRepRange(text: String): RepRangeEntity {
        val parts = text.trim().split("-")
        val min = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val max = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: min
        return RepRangeEntity(min = min, max = max)
    }

    fun parseScheduleDaysCsv(csv: String): List<Int> =
        csv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .distinct()
            .sorted()

    /**
     * Encodes split-slot → weekday as `"1:1,2:3,3:5"` (split.day : isoWeekday).
     */
    fun encodeDayMap(splitDayToIso: Map<Int, Int>): String =
        splitDayToIso.entries
            .sortedBy { it.key }
            .joinToString(",") { "${it.key}:${it.value}" }

    fun parseDayMap(csv: String): Map<Int, Int> {
        if (csv.isBlank()) return emptyMap()
        return csv.split(",")
            .mapNotNull { part ->
                val bits = part.trim().split(":")
                val slot = bits.getOrNull(0)?.trim()?.toIntOrNull() ?: return@mapNotNull null
                val iso = bits.getOrNull(1)?.trim()?.toIntOrNull() ?: return@mapNotNull null
                if (slot < 1 || iso !in 1..7) return@mapNotNull null
                slot to iso
            }
            .toMap()
    }

    /** Default: assign split days in order onto Mon… for daysPerWeek. */
    fun defaultDayMap(split: CatalogSplit): Map<Int, Int> {
        val ordered = split.schedule.sortedBy { it.day }
        return ordered.mapIndexed { index, slot ->
            slot.day to (index + 1).coerceAtMost(7)
        }.toMap()
    }

    fun workoutLabelForSlot(splitId: String, splitDay: Int): String {
        val split = WorkoutCatalog.findSplit(splitId) ?: return "Day $splitDay"
        val workoutId = split.schedule.firstOrNull { it.day == splitDay }?.workoutId ?: return "Day $splitDay"
        return WorkoutCatalog.resolveWorkout(workoutId)?.name ?: workoutId
    }

    /**
     * @param splitDayToIso maps each split schedule `day` (1..N) to an ISO weekday (1=Mon…7=Sun).
     */
    fun buildRoutine(splitId: String, splitDayToIso: Map<Int, Int>): SplitApplyResult {
        val split = WorkoutCatalog.findSplit(splitId)
            ?: return SplitApplyResult(error = "Unknown workout split")
        if (split.schedule.size != split.daysPerWeek) {
            return SplitApplyResult(error = "Split schedule is invalid")
        }
        val ordered = split.schedule.sortedBy { it.day }
        if (ordered.any { it.day !in splitDayToIso }) {
            return SplitApplyResult(error = "Assign a weekday for every workout in the split")
        }
        val isos = ordered.map { splitDayToIso.getValue(it.day) }
        if (isos.any { it !in 1..7 }) {
            return SplitApplyResult(error = "Pick a valid weekday for each workout")
        }
        if (isos.size != isos.distinct().size) {
            return SplitApplyResult(error = "Each weekday can only have one workout")
        }

        var routine = WorkoutRoutineEntity()
        for (iso in 1..7) {
            val key = isoToKey.getValue(iso)
            routine = routine.withDay(key, WorkoutDayPlanEntity(enabled = false, name = "Rest"))
        }
        ordered.forEachIndexed { index, slot ->
            val iso = splitDayToIso.getValue(slot.day)
            val key = isoToKey.getValue(iso)
            val workout = WorkoutCatalog.resolveWorkout(slot.workoutId)
                ?: return SplitApplyResult(error = "Unknown workout: ${slot.workoutId}")
            val exercises = workout.exercises.mapIndexed { exIndex, we ->
                val lib = WorkoutCatalog.exercises[we.exerciseId]
                PlannedExerciseEntity(
                    id = (index + 1) * 1000L + exIndex + 1,
                    name = lib?.name ?: we.exerciseId,
                    targetMuscle = lib?.muscleGroup ?: "",
                    sets = we.sets,
                    repRange = parseRepRange(we.repRange),
                )
            }
            routine = routine.withDay(
                key,
                WorkoutDayPlanEntity(
                    enabled = true,
                    name = workout.name,
                    exercises = exercises,
                ),
            )
        }
        return SplitApplyResult(
            routine = routine,
            trainingIsoDays = isos.distinct().sorted(),
        )
    }

    /** Ordered schedule_days fallback: slot i → sorted weekday i (legacy). */
    fun buildRoutineFromScheduleCsv(splitId: String, scheduleDaysCsv: String): SplitApplyResult {
        val split = WorkoutCatalog.findSplit(splitId)
            ?: return SplitApplyResult(error = "Unknown workout split")
        val days = parseScheduleDaysCsv(scheduleDaysCsv)
        if (days.isEmpty()) {
            return SplitApplyResult(error = "Select training days on your schedule")
        }
        if (days.size != split.daysPerWeek) {
            return SplitApplyResult(
                error = "Pick ${split.daysPerWeek} training days for ${split.name} (you have ${days.size})",
            )
        }
        val ordered = split.schedule.sortedBy { it.day }
        val map = ordered.mapIndexed { index, slot -> slot.day to days[index] }.toMap()
        return buildRoutine(splitId, map)
    }
}
