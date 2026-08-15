package com.example.solo_levelling.domain.service

import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.AttributeDelta

object AttributeRewardsParser {
    fun parse(json: String): List<AttributeDelta> {
        if (json.isBlank()) return emptyList()
        val deltas = mutableListOf<AttributeDelta>()
        for (code in AttributeCode.entries) {
            val regex = Regex("\"${code.name}\"\\s*:\\s*(-?\\d+)")
            val match = regex.find(json) ?: continue
            val amount = match.groupValues[1].toIntOrNull() ?: continue
            if (amount != 0) deltas.add(AttributeDelta(code, amount))
        }
        return deltas
    }

    fun toJson(deltas: List<AttributeDelta>): String =
        deltas.joinToString(prefix = "{", postfix = "}") { "\"${it.code.name}\":${it.amount}" }
}
