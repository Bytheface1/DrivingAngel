package com.example.test.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.test.data.database.entities.HeartRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HeartRateDao {

    @Query("SELECT * FROM heart_rate_table ORDER BY timestamp ASC")
    fun getAllHeartRates(): Flow<List<HeartRateEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(heartRate: HeartRateEntity)

    @Query("DELETE FROM heart_rate_table")
    suspend fun deleteAll()
}