package com.rentease.app.data.local

import androidx.room.TypeConverter
import com.rentease.app.data.local.entity.OutboxAction
import com.rentease.app.data.local.entity.OutboxStatus

class RentEaseTypeConverters {
    @TypeConverter
    fun fromOutboxAction(value: OutboxAction): String = value.name

    @TypeConverter
    fun toOutboxAction(value: String): OutboxAction = OutboxAction.valueOf(value)

    @TypeConverter
    fun fromOutboxStatus(value: OutboxStatus): String = value.name

    @TypeConverter
    fun toOutboxStatus(value: String): OutboxStatus = OutboxStatus.valueOf(value)
}
