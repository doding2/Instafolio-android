package com.instafolioo.instagramportfolio.database.resultslide

import androidx.lifecycle.LiveData
import androidx.room.*
import com.instafolioo.instagramportfolio.model.ResultSlide

@Dao
interface ResultSlideDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertResultSlide(resultSlide: ResultSlide): Long

    @Delete
    suspend fun deleteResultSlide(resultSlide: ResultSlide): Int

    @Query("SELECT * FROM ResultSlideTable WHERE id == :id")
    suspend fun getResultSlide(id: Long): ResultSlide

    @Query("SELECT * FROM ResultSlideTable")
    fun getAllResultSlides(): LiveData<MutableList<ResultSlide>>
}