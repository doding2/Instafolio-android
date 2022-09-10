package com.android.instagramportfolio.database.resultslide

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.instagramportfolio.model.ResultSlide

@Dao
interface ResultSlideDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertResultSlide(resultSlide: ResultSlide): Long

    @Delete
    suspend fun deleteResultSlide(resultSlide: ResultSlide): Int

    @Query("SELECT * FROM ResultSlideTable")
    fun getAllResultSlides(): LiveData<MutableList<ResultSlide>>
}