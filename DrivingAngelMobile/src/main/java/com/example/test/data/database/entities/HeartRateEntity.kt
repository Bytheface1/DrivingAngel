package com.example.test.data.database.entities

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "heart_rate_table")
data class HeartRateEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "heart_rate") val heart_rate: Int,
    @NonNull @ColumnInfo(name = "timestamp") val timestamp: Date,
    )