package com.example.background

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatDao {
    @Query("SELECT * FROM cats")
    fun getAllCats(): Flow<List<CatItem>>

    @Insert
    suspend fun insertCat(cat: CatItem)

    // ✨ NEW: This lets us save the new position!
    @Update
    suspend fun updateCat(cat: CatItem)
}

@Database(entities = [CatItem::class], version = 2, exportSchema = false) // bumped version
abstract class CatDatabase : RoomDatabase() {
    abstract fun catDao(): CatDao

    companion object {
        @Volatile
        private var INSTANCE: CatDatabase? = null

        fun getDatabase(context: Context): CatDatabase {
            return INSTANCE ?: synchronized(this) {
                // fallbackToDestructiveMigration() wipes the db if schema changes (good for testing)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CatDatabase::class.java,
                    "cat_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}