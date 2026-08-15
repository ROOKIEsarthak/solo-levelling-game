package com.example.solo_levelling.domain.port

import com.example.solo_levelling.data.db.entity.SyncOutboxEntity

interface SyncTransportPort {
    suspend fun push(entries: List<SyncOutboxEntity>): Int
}

class NoOpSyncTransport : SyncTransportPort {
    override suspend fun push(entries: List<SyncOutboxEntity>): Int = 0
}
