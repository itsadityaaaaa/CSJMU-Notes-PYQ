package com.example.data.model

import androidx.room.TypeConverter
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.ResourceType

class Converters {
    @TypeConverter
    fun fromResourceType(value: ResourceType): String = value.name

    @TypeConverter
    fun toResourceType(value: String): ResourceType = runCatching {
        ResourceType.valueOf(value)
    }.getOrDefault(ResourceType.NOTES)

    @TypeConverter
    fun fromApprovalStatus(value: ApprovalStatus): String = value.name

    @TypeConverter
    fun toApprovalStatus(value: String): ApprovalStatus = runCatching {
        ApprovalStatus.valueOf(value)
    }.getOrDefault(ApprovalStatus.PENDING)

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(separator = "|||")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|||")
}
