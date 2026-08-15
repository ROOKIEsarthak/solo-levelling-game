package com.example.solo_levelling.domain.port

data class BusyBlock(
    val startEpochMs: Long,
    val endEpochMs: Long,
    val title: String,
)

interface CalendarPort {
    suspend fun suggestedBusyBlocks(date: String): List<BusyBlock>
}

class NoOpCalendarPort : CalendarPort {
    override suspend fun suggestedBusyBlocks(date: String): List<BusyBlock> = emptyList()
}
