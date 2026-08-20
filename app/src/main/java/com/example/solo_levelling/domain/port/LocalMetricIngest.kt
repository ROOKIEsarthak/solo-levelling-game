package com.example.solo_levelling.domain.port

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.domain.logic.ActivityDatePolicy
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LocalMetricIngest(
    private val db: JsonDatabase,
    private val clock: AppClock,
    private val onIngested: suspend () -> Unit = {},
) : MetricIngestPort {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    override suspend fun ingest(metricType: String, value: Float, date: String?) {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val zone = runCatching { ZoneId.of(profile?.timezone ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        val dateStr = date ?: today.format(dateFmt)
        val parsed = runCatching { LocalDate.parse(dateStr, dateFmt) }.getOrNull() ?: return
        if (!ActivityDatePolicy.canWriteRecord(today, parsed)) return
        val now = clock.nowEpochMs()
        db.moduleDao().insertMetric(
            MetricLogEntity(
                metricType = metricType,
                value = value,
                recordedAtEpochMs = now,
                date = dateStr,
            ),
        )
        onIngested()
    }
}
