package com.example.solo_levelling.data.seed

data class CatalogExercise(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
)

data class CatalogWorkoutExercise(
    val exerciseId: String,
    val sets: Int,
    val repRange: String,
    val restSeconds: Int,
)

data class CatalogWorkout(
    val id: String,
    val name: String,
    val targetMuscles: List<String>,
    val exercises: List<CatalogWorkoutExercise>,
)

data class CatalogSplitDay(
    val day: Int,
    val workoutId: String,
)

data class CatalogSplit(
    val id: String,
    val name: String,
    val daysPerWeek: Int,
    val schedule: List<CatalogSplitDay>,
)

object WorkoutCatalog {
    const val version = "1.0.0"

    val splits: List<CatalogSplit> = listOf(
        CatalogSplit(
            id = "ppl_ul",
            name = "Push Pull Legs Upper Lower",
            daysPerWeek = 5,
            schedule = listOf(
                CatalogSplitDay(1, "push"),
                CatalogSplitDay(2, "pull"),
                CatalogSplitDay(3, "legs"),
                CatalogSplitDay(4, "upper"),
                CatalogSplitDay(5, "lower"),
            ),
        ),
        CatalogSplit(
            id = "ul_ppl",
            name = "Upper Lower Push Pull Legs",
            daysPerWeek = 5,
            schedule = listOf(
                CatalogSplitDay(1, "upper"),
                CatalogSplitDay(2, "lower"),
                CatalogSplitDay(3, "push"),
                CatalogSplitDay(4, "pull"),
                CatalogSplitDay(5, "legs"),
            ),
        ),
        CatalogSplit(
            id = "upper_lower",
            name = "Upper Lower",
            daysPerWeek = 4,
            schedule = listOf(
                CatalogSplitDay(1, "upper"),
                CatalogSplitDay(2, "lower"),
                CatalogSplitDay(3, "upper"),
                CatalogSplitDay(4, "lower"),
            ),
        ),
        CatalogSplit(
            id = "ppl",
            name = "Push Pull Legs",
            daysPerWeek = 6,
            schedule = listOf(
                CatalogSplitDay(1, "push"),
                CatalogSplitDay(2, "pull"),
                CatalogSplitDay(3, "legs"),
                CatalogSplitDay(4, "push"),
                CatalogSplitDay(5, "pull"),
                CatalogSplitDay(6, "legs"),
            ),
        ),
        CatalogSplit(
            id = "full_body_3",
            name = "Full Body",
            daysPerWeek = 3,
            schedule = listOf(
                CatalogSplitDay(1, "full_body"),
                CatalogSplitDay(2, "full_body"),
                CatalogSplitDay(3, "full_body"),
            ),
        ),
        CatalogSplit(
            id = "full_body_4",
            name = "Full Body 4 Days",
            daysPerWeek = 4,
            schedule = listOf(
                CatalogSplitDay(1, "full_body"),
                CatalogSplitDay(2, "full_body"),
                CatalogSplitDay(3, "full_body"),
                CatalogSplitDay(4, "full_body"),
            ),
        ),
        CatalogSplit(
            id = "arnold",
            name = "Arnold Split",
            daysPerWeek = 6,
            schedule = listOf(
                CatalogSplitDay(1, "chest_back"),
                CatalogSplitDay(2, "shoulders_arms"),
                CatalogSplitDay(3, "legs"),
                CatalogSplitDay(4, "chest_back"),
                CatalogSplitDay(5, "shoulders_arms"),
                CatalogSplitDay(6, "legs"),
            ),
        ),
        CatalogSplit(
            id = "bro_split",
            name = "Bro Split",
            daysPerWeek = 5,
            schedule = listOf(
                CatalogSplitDay(1, "chest"),
                CatalogSplitDay(2, "back"),
                CatalogSplitDay(3, "shoulders"),
                CatalogSplitDay(4, "arms"),
                CatalogSplitDay(5, "legs"),
            ),
        ),
    )

    val workouts: Map<String, CatalogWorkout> = mapOf(
        "push" to CatalogWorkout(
            id = "push",
            name = "Push",
            targetMuscles = listOf("chest", "front_delts", "side_delts", "triceps"),
            exercises = listOf(
                CatalogWorkoutExercise("bench_press", 3, "6-10", 150),
                CatalogWorkoutExercise("incline_dumbbell_press", 3, "8-12", 120),
                CatalogWorkoutExercise("shoulder_press", 3, "8-12", 120),
                CatalogWorkoutExercise("cable_lateral_raise", 3, "12-20", 60),
                CatalogWorkoutExercise("triceps_pushdown", 3, "10-15", 60),
                CatalogWorkoutExercise("overhead_triceps_extension", 2, "10-15", 60),
            ),
        ),
        "pull" to CatalogWorkout(
            id = "pull",
            name = "Pull",
            targetMuscles = listOf("lats", "upper_back", "rear_delts", "biceps", "forearms"),
            exercises = listOf(
                CatalogWorkoutExercise("lat_pulldown", 3, "8-12", 120),
                CatalogWorkoutExercise("barbell_row", 3, "6-10", 150),
                CatalogWorkoutExercise("single_arm_dumbbell_row", 3, "8-12", 120),
                CatalogWorkoutExercise("cable_pullover", 2, "10-15", 60),
                CatalogWorkoutExercise("face_pull", 3, "12-20", 60),
                CatalogWorkoutExercise("barbell_curl", 3, "8-12", 90),
                CatalogWorkoutExercise("hammer_curl", 2, "10-15", 60),
            ),
        ),
        "legs" to CatalogWorkout(
            id = "legs",
            name = "Legs",
            targetMuscles = listOf("quadriceps", "hamstrings", "glutes", "calves"),
            exercises = listOf(
                CatalogWorkoutExercise("squat", 3, "6-10", 150),
                CatalogWorkoutExercise("leg_press", 3, "8-12", 120),
                CatalogWorkoutExercise("romanian_deadlift", 3, "8-12", 150),
                CatalogWorkoutExercise("leg_extension", 3, "10-15", 60),
                CatalogWorkoutExercise("leg_curl", 3, "10-15", 60),
                CatalogWorkoutExercise("calf_raise", 3, "10-20", 60),
            ),
        ),
        "upper" to CatalogWorkout(
            id = "upper",
            name = "Upper",
            targetMuscles = listOf("chest", "back", "shoulders", "biceps", "triceps"),
            exercises = listOf(
                CatalogWorkoutExercise("bench_press", 3, "6-10", 150),
                CatalogWorkoutExercise("lat_pulldown", 3, "8-12", 120),
                CatalogWorkoutExercise("seated_cable_row", 3, "8-12", 120),
                CatalogWorkoutExercise("incline_dumbbell_press", 2, "8-12", 120),
                CatalogWorkoutExercise("cable_lateral_raise", 3, "12-20", 60),
                CatalogWorkoutExercise("barbell_curl", 2, "8-12", 90),
                CatalogWorkoutExercise("triceps_pushdown", 2, "10-15", 60),
            ),
        ),
        "lower" to CatalogWorkout(
            id = "lower",
            name = "Lower",
            targetMuscles = listOf("quadriceps", "hamstrings", "glutes", "calves"),
            exercises = listOf(
                CatalogWorkoutExercise("leg_press", 3, "8-12", 120),
                CatalogWorkoutExercise("romanian_deadlift", 3, "8-12", 150),
                CatalogWorkoutExercise("leg_extension", 3, "10-15", 60),
                CatalogWorkoutExercise("leg_curl", 3, "10-15", 60),
                CatalogWorkoutExercise("calf_raise", 4, "10-20", 60),
            ),
        ),
        "full_body" to CatalogWorkout(
            id = "full_body",
            name = "Full Body",
            targetMuscles = listOf("chest", "back", "shoulders", "arms", "legs"),
            exercises = listOf(
                CatalogWorkoutExercise("bench_press", 3, "6-10", 150),
                CatalogWorkoutExercise("lat_pulldown", 3, "8-12", 120),
                CatalogWorkoutExercise("leg_press", 3, "8-12", 120),
                CatalogWorkoutExercise("romanian_deadlift", 2, "8-12", 120),
                CatalogWorkoutExercise("cable_lateral_raise", 2, "12-20", 60),
                CatalogWorkoutExercise("barbell_curl", 2, "10-15", 60),
                CatalogWorkoutExercise("triceps_pushdown", 2, "10-15", 60),
            ),
        ),
        "chest_back" to CatalogWorkout(
            id = "chest_back",
            name = "Chest + Back",
            targetMuscles = listOf("chest", "back"),
            exercises = listOf(
                CatalogWorkoutExercise("bench_press", 3, "6-10", 150),
                CatalogWorkoutExercise("lat_pulldown", 3, "8-12", 120),
                CatalogWorkoutExercise("incline_dumbbell_press", 3, "8-12", 120),
                CatalogWorkoutExercise("barbell_row", 3, "6-10", 150),
                CatalogWorkoutExercise("cable_pullover", 2, "10-15", 60),
            ),
        ),
        "shoulders_arms" to CatalogWorkout(
            id = "shoulders_arms",
            name = "Shoulders + Arms",
            targetMuscles = listOf("shoulders", "biceps", "triceps"),
            exercises = listOf(
                CatalogWorkoutExercise("shoulder_press", 3, "8-12", 120),
                CatalogWorkoutExercise("cable_lateral_raise", 4, "12-20", 60),
                CatalogWorkoutExercise("rear_delt_fly", 3, "12-20", 60),
                CatalogWorkoutExercise("barbell_curl", 3, "8-12", 90),
                CatalogWorkoutExercise("hammer_curl", 2, "10-15", 60),
                CatalogWorkoutExercise("triceps_pushdown", 3, "10-15", 60),
                CatalogWorkoutExercise("overhead_triceps_extension", 2, "10-15", 60),
            ),
        ),
        "chest" to CatalogWorkout(
            id = "chest",
            name = "Chest",
            targetMuscles = listOf("chest"),
            exercises = listOf(
                CatalogWorkoutExercise("bench_press", 4, "6-10", 150),
                CatalogWorkoutExercise("incline_dumbbell_press", 3, "8-12", 120),
                CatalogWorkoutExercise("cable_fly", 3, "10-15", 60),
            ),
        ),
        "back" to CatalogWorkout(
            id = "back",
            name = "Back",
            targetMuscles = listOf("lats", "upper_back"),
            exercises = listOf(
                CatalogWorkoutExercise("lat_pulldown", 4, "8-12", 120),
                CatalogWorkoutExercise("barbell_row", 3, "6-10", 150),
                CatalogWorkoutExercise("single_arm_dumbbell_row", 3, "8-12", 120),
                CatalogWorkoutExercise("cable_pullover", 3, "10-15", 60),
            ),
        ),
        "shoulders" to CatalogWorkout(
            id = "shoulders",
            name = "Shoulders",
            targetMuscles = listOf("front_delts", "side_delts", "rear_delts"),
            exercises = listOf(
                CatalogWorkoutExercise("shoulder_press", 3, "8-12", 120),
                CatalogWorkoutExercise("cable_lateral_raise", 4, "12-20", 60),
                CatalogWorkoutExercise("rear_delt_fly", 3, "12-20", 60),
            ),
        ),
        "arms" to CatalogWorkout(
            id = "arms",
            name = "Arms",
            targetMuscles = listOf("biceps", "triceps", "forearms"),
            exercises = listOf(
                CatalogWorkoutExercise("barbell_curl", 3, "8-12", 90),
                CatalogWorkoutExercise("hammer_curl", 3, "10-15", 60),
                CatalogWorkoutExercise("triceps_pushdown", 3, "10-15", 60),
                CatalogWorkoutExercise("overhead_triceps_extension", 3, "10-15", 60),
            ),
        ),
    )

    val exercises: Map<String, CatalogExercise> = mapOf(
        "bench_press" to CatalogExercise("bench_press", "Barbell Bench Press", "chest", "barbell"),
        "incline_dumbbell_press" to CatalogExercise("incline_dumbbell_press", "Incline Dumbbell Press", "chest", "dumbbell"),
        "cable_fly" to CatalogExercise("cable_fly", "Cable Fly", "chest", "cable"),
        "lat_pulldown" to CatalogExercise("lat_pulldown", "Lat Pulldown", "back", "machine"),
        "barbell_row" to CatalogExercise("barbell_row", "Barbell Row", "back", "barbell"),
        "single_arm_dumbbell_row" to CatalogExercise("single_arm_dumbbell_row", "Single Arm Dumbbell Row", "back", "dumbbell"),
        "seated_cable_row" to CatalogExercise("seated_cable_row", "Seated Cable Row", "back", "cable"),
        "cable_pullover" to CatalogExercise("cable_pullover", "Cable Pullover", "back", "cable"),
        "shoulder_press" to CatalogExercise("shoulder_press", "Shoulder Press", "shoulders", "machine"),
        "cable_lateral_raise" to CatalogExercise("cable_lateral_raise", "Cable Lateral Raise", "side_delts", "cable"),
        "rear_delt_fly" to CatalogExercise("rear_delt_fly", "Rear Delt Fly", "rear_delts", "machine"),
        "face_pull" to CatalogExercise("face_pull", "Face Pull", "rear_delts", "cable"),
        "triceps_pushdown" to CatalogExercise("triceps_pushdown", "Triceps Pushdown", "triceps", "cable"),
        "overhead_triceps_extension" to CatalogExercise("overhead_triceps_extension", "Overhead Triceps Extension", "triceps", "cable"),
        "barbell_curl" to CatalogExercise("barbell_curl", "Barbell Curl", "biceps", "barbell"),
        "hammer_curl" to CatalogExercise("hammer_curl", "Hammer Curl", "biceps", "dumbbell"),
        "squat" to CatalogExercise("squat", "Barbell Squat", "quadriceps", "barbell"),
        "leg_press" to CatalogExercise("leg_press", "Leg Press", "quadriceps", "machine"),
        "romanian_deadlift" to CatalogExercise("romanian_deadlift", "Romanian Deadlift", "hamstrings", "barbell"),
        "leg_extension" to CatalogExercise("leg_extension", "Leg Extension", "quadriceps", "machine"),
        "leg_curl" to CatalogExercise("leg_curl", "Leg Curl", "hamstrings", "machine"),
        "calf_raise" to CatalogExercise("calf_raise", "Calf Raise", "calves", "machine"),
    )

    fun findSplit(id: String): CatalogSplit? = splits.firstOrNull { it.id == id }

    fun resolveWorkout(workoutId: String): CatalogWorkout? = workouts[workoutId]

    fun exerciseName(id: String): String = exercises[id]?.name ?: id
}
