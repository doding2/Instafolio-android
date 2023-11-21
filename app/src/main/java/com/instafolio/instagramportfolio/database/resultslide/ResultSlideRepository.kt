package com.instafolio.instagramportfolio.database.resultslide

import com.instafolio.instagramportfolio.model.ResultSlide


class ResultSlideRepository(private val dao: ResultSlideDAO) {

    val resultSlides = dao.getAllResultSlides()

    suspend fun add(resultSlide: ResultSlide): Long {
        return dao.insertResultSlide(resultSlide)
    }

    suspend fun delete(resultSlide: ResultSlide): Int {
        return dao.deleteResultSlide(resultSlide)
    }

    suspend fun get(id: Long): ResultSlide {
        return dao.getResultSlide(id)
    }
}