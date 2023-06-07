package com.example.test.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.test.data.DateConverter
import com.example.test.data.database.dao.HeartRateDao
import com.example.test.data.database.entities.HeartRateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*

/**
 * This is the backend. The database.
 * Annotates class to be a Room Database with a table (entity) of the HeartRate class
 */
@Database(entities = [HeartRateEntity::class], version = 1)
@TypeConverters(DateConverter::class)
abstract class DrivingAngelDatabase : RoomDatabase() {

    abstract fun HeartRateDao(): HeartRateDao

    companion object {
        @Volatile
        private var INSTANCE: DrivingAngelDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): DrivingAngelDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DrivingAngelDatabase::class.java,
                    "word_database"
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    .fallbackToDestructiveMigration()
                    .addCallback(DrivingAngelDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }

        private class DrivingAngelDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            /**
             * Override the onCreate method to populate the database.
             */
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // If you want to keep the data through app restarts,
                // comment out the following line.
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        deleteDatabase(database.HeartRateDao())
                        //populateDatabase(database.HeartRateDao())
                    }
                }
            }
        }

        /**
         * Populate the database in a new coroutine.
         * If you want to start with more words, just add them.
         */
        suspend fun populateDatabase(heartRateDao: HeartRateDao) {
            // Start the app with a clean database every time.
            // Not needed if you only populate on creation.
            heartRateDao.deleteAll()
            var heartRateEntity = HeartRateEntity(0, 70, Date())
            heartRateDao.insert(heartRateEntity)
            for (i in 0..30) {
                heartRateEntity = HeartRateEntity(0, kotlin.random.Random.nextInt(50, 175), Date())
                heartRateDao.insert(heartRateEntity)
                delay(5000)
            }
        }

        /**
         * Delete the database in a new coroutine.
         * If you want to start with more words, just add them.
         */
        suspend fun deleteDatabase(heartRateDao: HeartRateDao) {
            // Start the app with a clean database every time.
            heartRateDao.deleteAll()
        }
    }
}