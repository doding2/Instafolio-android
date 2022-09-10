package com.android.instagramportfolio.view.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.android.instagramportfolio.database.resultslide.ResultSlideDatabase
import com.android.instagramportfolio.database.resultslide.ResultSlideRepository
import com.android.instagramportfolio.model.ResultSlide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(context: Context): ViewModel() {

    private val repository = ResultSlideRepository(ResultSlideDatabase(context).resultSlideDAO)

    val resultSlides: LiveData<MutableList<ResultSlide>> = repository.resultSlides

    val nextDirectory get() = "id_" + resultSlides.value!!.size

    fun addResultSlide(resultSlide: ResultSlide) {
        CoroutineScope(Dispatchers.Main).launch {
            val id = repository.add(resultSlide)
            resultSlide.id = id
            resultSlides.value?.add(resultSlide)
        }
    }

}