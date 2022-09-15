package com.instafolioo.instagramportfolio.database.resultslide

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.instafolioo.instagramportfolio.model.ResultSlide

@Database(entities = [ResultSlide::class], version = 1)
@TypeConverters(com.instafolioo.instagramportfolio.database.common.BitmapConverter::class)
abstract class ResultSlideDatabase : RoomDatabase() {

    abstract val resultSlideDAO: ResultSlideDAO

    companion object {
        @Volatile
        private var INSTANCE: ResultSlideDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = INSTANCE ?: synchronized(LOCK) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(context,
            ResultSlideDatabase::class.java, "ClosetDB")
            .build()
    }
}