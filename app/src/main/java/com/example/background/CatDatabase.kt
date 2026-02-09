package com.example.background

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CatDao {
    @Query("SELECT * FROM cats")
    fun getAllCats(): Flow<List<CatItem>>

    @Insert
    suspend fun insertCat(cat: CatItem)

    @Update
    suspend fun updateCat(cat: CatItem)

    @Delete
    suspend fun deleteCat(cat: CatItem)
}

@Database(entities = [CatItem::class], version = 3, exportSchema = false)
abstract class CatDatabase : RoomDatabase() {
    abstract fun catDao(): CatDao

    companion object {
        @Volatile
        private var INSTANCE: CatDatabase? = null

        fun getDatabase(context: Context): CatDatabase {
            return INSTANCE ?: synchronized(this) {
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