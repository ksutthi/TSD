package com.tsd.platform.persistence

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class JsonAttributeConverter : AttributeConverter<Map<String, Any>, String> {

    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, Any>?): String? {
        if (attribute == null) return null
        return try {
            objectMapper.writeValueAsString(attribute)
        } catch (e: Exception) {
            "{}" // Fallback to empty JSON
        }
    }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Any> {
        if (dbData.isNullOrBlank()) return mutableMapOf()
        return try {
            objectMapper.readValue(dbData, object : TypeReference<Map<String, Any>>() {})
        } catch (e: Exception) {
            mutableMapOf()
        }
    }
}