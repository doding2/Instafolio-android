package com.instafolioo.instagramportfolio.view.slide

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.instafolioo.instagramportfolio.model.ResultSlide
import com.instafolioo.instagramportfolio.model.Slide

class SlideViewModel: ViewModel() {

    // 기존 파일을 편집할 경우
    val resultSlideWithExtension = MutableLiveData<MutableList<Pair<ResultSlide, String>>>().apply {
        value = mutableListOf()
    }

    // 새로운 파일을 열었을 경우
    val uriWithExtension = MutableLiveData<MutableList<Pair<Uri, String>>>().apply {
        value = mutableListOf()
    }

    val slides = MutableLiveData<MutableList<Slide>>().apply {
        value = mutableListOf()
    }

    // 이거랑 아래거는 백업 용도로만 사용해야 됨
    // 실제로 사용해야 하는 것은 adapter에서 관리
    val bindingPairs = MutableLiveData<MutableList<Pair<Slide, Slide>>>().apply {
        value = mutableListOf()
    }
    val bindingFlattenSlides = MutableLiveData<List<Slide>>().apply {
        value = listOf()
    }

    val isInstarSize = MutableLiveData<Boolean>().apply {
        value = true
    }

    val enableBinding = MutableLiveData<Boolean>().apply {
        value = false
    }

    // Intent로 유저를 manage storage 퍼미션을 수정하라고 보냈는지 아닌지 체크
    val isAppPausedBecauseOfPermission = MutableLiveData<Boolean>().apply {
        value = false
    }

    // 기존 파일 열었을때 아무런 수정 안해으면 뒤로가기 다이얼로그 안뜨게 하기 위해
    val isSlideMoved = MutableLiveData<Boolean>().apply {
        value = false
    }

    fun clear() {
        resultSlideWithExtension.value = mutableListOf()
        uriWithExtension.value = mutableListOf()
        slides.value = mutableListOf()
        bindingPairs.value = mutableListOf()
        bindingFlattenSlides.value = listOf()
        isInstarSize.value = true
        enableBinding.value = false
        isAppPausedBecauseOfPermission.value = false
        isSlideMoved.value = false
    }
}