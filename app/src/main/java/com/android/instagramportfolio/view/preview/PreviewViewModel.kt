package com.android.instagramportfolio.view.preview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.instagramportfolio.model.PreviewSlide
import com.android.instagramportfolio.model.ResultSlide

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

}