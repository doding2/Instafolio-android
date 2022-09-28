package com.instafolioo.instagramportfolio.view.preview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.instafolioo.instagramportfolio.model.PreviewSlide
import com.instafolioo.instagramportfolio.model.ResultSlide

class PreviewViewModel: ViewModel() {

    val previewSlides = MutableLiveData<MutableList<PreviewSlide>>().apply {
        value = mutableListOf()
    }
    // 현재 보여지고 있는 슬라이드의 포지션
    val currentSlide = MutableLiveData<Int>().apply {
        value = 1
    }
    val slidesSize = MutableLiveData<Int>().apply {
        value = 0
    }

    // 저장 중에 도중에 나갈때 대비해서
    // DB에 등록된놈 삭제시킴
    val savingSlide = MutableLiveData<ResultSlide>().apply {
        value = null
    }

    // 분할 위치 지정
    val cutPositions = MutableLiveData<MutableList<Int>>().apply {
        value = mutableListOf()
    }

    fun clear() {
        previewSlides.value?.clear()
        currentSlide.value = 1
        slidesSize.value = 0
        savingSlide.value = null
        cutPositions.value?.clear()
    }

    // 분할
    val isAlreadyCut get() = cutPositions.value!!.contains(currentSlide.value!! - 1)

    fun addCutPosition() {
        cutPositions.value?.add(currentSlide.value!! - 1)
    }

    fun removeCutPosition() {
        cutPositions.value?.remove(currentSlide.value!! - 1)
    }
}