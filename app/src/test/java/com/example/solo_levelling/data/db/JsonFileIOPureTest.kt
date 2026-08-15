package com.example.solo_levelling.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JsonFileIOPureTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun p_writeAndReadText_roundTrips() {
        val io = JsonFileIO(tmp.root)
        io.writeText("workouts.json", """[{"type":"Push"}]""")
        assertEquals("""[{"type":"Push"}]""", io.readText("workouts.json"))
        assertTrue(File(tmp.root, "workouts.json").exists())
    }

    @Test
    fun p_writeTask_createsTasksFolderFile() {
        val io = JsonFileIO(tmp.root)
        io.writeTask(7L, """{"id":7,"title":"DSA"}""")
        val content = io.readTask(7L)
        assertTrue(content!!.contains("DSA"))
        assertTrue(File(tmp.root, "tasks/task-7.json").exists())
    }

    @Test
    fun n_readMissing_returnsNull() {
        val io = JsonFileIO(tmp.root)
        assertNull(io.readText("missing.json"))
        assertNull(io.readTask(99L))
    }

    @Test
    fun e_clearTasks_removesAllTaskFilesOnly() {
        val io = JsonFileIO(tmp.root)
        io.writeText("user.json", """{"name":"Keep"}""")
        io.writeTask(1L, "{}")
        io.writeTask(2L, "{}")
        io.clearTasks()
        assertEquals(0, io.listTasks().size)
        assertEquals("""{"name":"Keep"}""", io.readText("user.json"))
        assertFalse(File(tmp.root, "tasks/task-1.json").exists())
    }
}
