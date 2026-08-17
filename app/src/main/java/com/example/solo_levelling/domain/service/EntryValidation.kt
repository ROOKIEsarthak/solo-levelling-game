package com.example.solo_levelling.domain.service

object EntryValidation {
    fun requireNonBlank(value: String?, fieldLabel: String): String? {
        if (value.isNullOrBlank()) return "Enter $fieldLabel"
        return null
    }

    fun requirePositiveFloat(text: String?, fieldLabel: String): String? {
        val value = text?.trim()?.toFloatOrNull()
        if (value == null) return "Enter a valid $fieldLabel"
        if (value <= 0f) return "$fieldLabel must be greater than 0"
        return null
    }

    fun requirePositiveInt(text: String?, fieldLabel: String): String? {
        val value = text?.trim()?.toIntOrNull()
        if (value == null) return "Enter a valid $fieldLabel"
        if (value <= 0) return "$fieldLabel must be greater than 0"
        return null
    }

    fun requireNonNegativeInt(text: String?, fieldLabel: String): String? {
        val value = text?.trim()?.toIntOrNull()
        if (value == null) return "Enter a valid $fieldLabel"
        if (value < 0) return "$fieldLabel cannot be negative"
        return null
    }

    fun firstError(vararg errors: String?): String? = errors.firstOrNull { it != null }
}
