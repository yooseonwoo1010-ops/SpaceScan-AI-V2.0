package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        Project::class,
        ScanSegment::class,
        ScanImage::class,
        GeoAnchor::class,
        FloorPlan::class,
        AIRecommendation::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SpaceScanDatabase : RoomDatabase() {
    abstract fun spaceScanDao(): SpaceScanDao

    companion object {
        @Volatile
        private var INSTANCE: SpaceScanDatabase? = null

        fun getDatabase(context: Context): SpaceScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpaceScanDatabase::class.java,
                    "spacescan_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
