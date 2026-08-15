package com.example.solo_levelling.domain.port

interface MetricIngestPort {
    suspend fun ingest(metricType: String, value: Float, date: String? = null)
}
