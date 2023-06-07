package com.example.test.data

import androidx.room.TypeConverter
import java.util.*

/**
 * Class to convert Dates to Timestamp which the Room Database supports it
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}