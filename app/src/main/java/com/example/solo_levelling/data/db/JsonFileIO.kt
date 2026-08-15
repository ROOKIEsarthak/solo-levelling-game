package com.example.solo_levelling.data.db

import java.io.File
import java.nio.charset.StandardCharsets

class JsonFileIO(private val root: File) {
    private val tasksDir: File
        get() = File(root, TASKS_DIR)

    fun ensureRoot() {
        root.mkdirs()
        tasksDir.mkdirs()
        File(root, WORKOUTS_LOGS_DIR).mkdirs()
        File(root, DIET_LOGS_DIR).mkdirs()
    }

    fun readText(name: String): String? {
        val file = File(root, name)
        if (!file.exists()) return null
        return file.readText(StandardCharsets.UTF_8)
    }

    fun writeText(name: String, content: String) {
        val file = File(root, name)
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(content, StandardCharsets.UTF_8)
        if (!temp.renameTo(file)) {
            file.writeText(content, StandardCharsets.UTF_8)
            temp.delete()
        }
    }

    fun delete(name: String) {
        File(root, name).delete()
    }

    fun listJsonFiles(relativeDir: String): List<File> {
        val dir = File(root, relativeDir)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    fun clearDir(relativeDir: String) {
        listJsonFiles(relativeDir).forEach { it.delete() }
    }

    fun listTasks(): List<File> =
        tasksDir.listFiles()?.filter { it.isFile && it.name.startsWith(TASK_PREFIX) && it.name.endsWith(".json") }
            ?.sortedBy { it.name }
            ?: emptyList()

    fun readTask(id: Long): String? {
        val file = taskFile(id)
        if (!file.exists()) return null
        return file.readText(StandardCharsets.UTF_8)
    }

    fun writeTask(id: Long, content: String) {
        tasksDir.mkdirs()
        val file = taskFile(id)
        val temp = File(tasksDir, "${file.name}.tmp")
        temp.writeText(content, StandardCharsets.UTF_8)
        if (!temp.renameTo(file)) {
            file.writeText(content, StandardCharsets.UTF_8)
            temp.delete()
        }
    }

    fun deleteTask(id: Long) {
        taskFile(id).delete()
    }

    fun clearTasks() {
        listTasks().forEach { it.delete() }
    }

    private fun taskFile(id: Long): File = File(tasksDir, "$TASK_PREFIX$id.json")

    companion object {
        const val TASKS_DIR = "tasks"
        const val TASK_PREFIX = "task-"
        const val WORKOUTS_LOGS_DIR = "workouts/logs"
        const val DIET_LOGS_DIR = "diet/logs"
        const val WORKOUT_ROUTINE_FILE = "workouts/routine.json"
    }
}
