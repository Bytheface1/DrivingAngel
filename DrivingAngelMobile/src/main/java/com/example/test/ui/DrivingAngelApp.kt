package com.example.test.ui

import android.app.Application
import com.example.test.data.DrivingAngelRepository
import com.example.test.data.database.DrivingAngelDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class DrivingAngelApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())
    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { DrivingAngelDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { DrivingAngelRepository(database.HeartRateDao()) }
}