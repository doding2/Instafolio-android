package com.instafolio.instagramportfolio.view.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instafolio.instagramportfolio.R
import com.instafolio.instagramportfolio.database.resultslide.ResultSlideDatabase
import com.instafolio.instagramportfolio.database.resultslide.ResultSlideRepository
import com.instafolio.instagramportfolio.model.ResultSlide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class HomeViewModel(context: Context): ViewModel() {

    private val repository = ResultSlideRepository(ResultSlideDatabase(context).resultSlideDAO)

    val resultSlides = MutableLiveData<MutableList<ResultSlide>>().apply {
        value = repository.getAll().toMutableList()
    }

    // 편집모드인지 아닌지 저장
    val isEditMode = MutableLiveData<Boolean>().apply {
        value = false
    }

    // 편집모드에서 선택된 애들
    val selectedResultSlides = MutableLiveData<MutableList<ResultSlide>>().apply {
        value = mutableListOf()
    }

    // 툴팁 관련
    private var isFirstExecutionData = MutableLiveData<Boolean>().apply {
        val pref = context.getSharedPreferences(context.getString(R.string.pref_settings), Context.MODE_PRIVATE)
        value = pref.getBoolean("isFirstExecution", true)

        if (value == true) {
            with (pref.edit()) {
                putBoolean("isFirstExecution", false)
                apply()
            }
        }
    }
    var isFirstExecution
        get() = isFirstExecutionData.value ?: true
        set(value) {
            isFirstExecutionData.value = value
        }


    val isReady get() = (resultSlides.value != null)

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