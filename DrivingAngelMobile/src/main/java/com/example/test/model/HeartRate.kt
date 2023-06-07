package com.example.test.model

import com.example.test.data.database.entities.HeartRateEntity
import java.util.*

data class HeartRate(
    val heartRate: Int,
    val timestamp: Date
    )

fun HeartRateEntity.toDomain() = HeartRate(heart_rate, timestamp)