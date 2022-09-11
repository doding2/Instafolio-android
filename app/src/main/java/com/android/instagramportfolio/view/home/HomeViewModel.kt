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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class HomeViewModel(context: Context): ViewModel() {

    private val repository = ResultSlideRepository(ResultSlideDatabase(context).resultSlideDAO)

    val resultSlides: LiveData<MutableList<ResultSlide>> = repository.resultSlides

    // 저장 중에 도중에 나갈때 대비해서
    // DB에 등록된놈 삭제시킴
    val savingId = MutableLiveData<Long>().apply {
        value = null
    }

    // 편집모드인지 아닌지 저장
    val isEditMode = MutableLiveData<Boolean>().apply {
        value = false
    }

    // 편집모드에서 선택된 애들
    val selectedResultSlides = MutableLiveData<MutableList<ResultSlide>>().apply {
        value = mutableListOf()
    }

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
        CoroutineScope(Dispatchers.Main).launch {
            val resultSlide = repository.get(id)
            repository.delete(resultSlide)
        }
    }

    fun deleteResultSlide(resultSlide: ResultSlide) {
        CoroutineScope(Dispatchers.Main).launch {
            repository.delete(resultSlide)
        }
    }

}