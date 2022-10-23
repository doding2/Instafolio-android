package com.instafolioo.instagramportfolio.view.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instafolioo.instagramportfolio.database.resultslide.ResultSlideDatabase
import com.instafolioo.instagramportfolio.database.resultslide.ResultSlideRepository
import com.instafolioo.instagramportfolio.model.ResultSlide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class HomeViewModel(context: Context): ViewModel() {

    private val repository = ResultSlideRepository(ResultSlideDatabase(context).resultSlideDAO)

    val resultSlides: LiveData<MutableList<ResultSlide>> = repository.resultSlides

    // 편집모드인지 아닌지 저장
    val isEditMode = MutableLiveData<Boolean>().apply {
        value = false
    }

    // 편집모드에서 선택된 애들
    val selectedResultSlides = MutableLiveData<MutableList<ResultSlide>>().apply {
        value = mutableListOf()
    }

    val isReady get() = resultSlides.value != null

    fun isEditMode(): Boolean {
        return isEditMode.value == true
    }

    suspend fun addResultSlide(resultSlide: ResultSlide): Long {
        var id = 0L

        coroutineScope {
            launch(Dispatchers.Main) {
                id = repository.add(resultSlide)
                resultSlide.id = id
                resultSlides.value?.add(resultSlide)
            }
        }

        return id
    }

    fun deleteResultSlideOf(id: Long) {
        viewModelScope.launch(Dispatchers.Main) {
            val resultSlide = repository.get(id)
            repository.delete(resultSlide)
        }
    }

    fun deleteResultSlide(resultSlide: ResultSlide) {
        viewModelScope.launch(Dispatchers.Main) {
            repository.delete(resultSlide)
        }
    }

}