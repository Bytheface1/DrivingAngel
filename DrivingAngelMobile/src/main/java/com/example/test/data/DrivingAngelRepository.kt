package com.example.test.data

import androidx.annotation.WorkerThread
import com.example.test.data.database.dao.HeartRateDao
import com.example.test.data.database.entities.HeartRateEntity
import kotlinx.coroutines.flow.Flow

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class DrivingAngelRepository(private val heartRateDao: HeartRateDao) {

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allHeartRates: Flow<List<HeartRateEntity>> = heartRateDao.getAllHeartRates()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(heartRate: HeartRateEntity) {
        heartRateDao.insert(heartRate)
    }

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun deleteAll() {
        heartRateDao.deleteAll()
    }


}