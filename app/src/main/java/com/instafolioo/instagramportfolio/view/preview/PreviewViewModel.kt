package com.instafolioo.instagramportfolio.view.preview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.ads.rewarded.RewardedAd
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
    val savingSlides = MutableLiveData<MutableList<ResultSlide>>().apply {
        value = mutableListOf()
    }

    // 분할 위치 지정
    val cutPositions = MutableLiveData<MutableList<Int>>().apply {
        value = mutableListOf()
    }


    private var isAdFinishedData = MutableLiveData<Boolean>()
    var isAdFinished: Boolean
        get() = isAdFinishedData.value ?: false
        set(value) {
            isAdFinishedData.value = value
        }

    private val isDownloadFinishedData = MutableLiveData<Boolean>()
    var isDownloadFinished: Boolean
        get() = isDownloadFinishedData.value ?: false
        set(value) {
            isDownloadFinishedData.value = value
        }


    // 광고
    private val mRewardedAdData = MutableLiveData<RewardedAd?>()
    var mRewardedAd: RewardedAd?
        get() = mRewardedAdData.value
        set(value) {
            mRewardedAdData.value = value
        }


    fun clearAd() {
        isAdFinished = false
        isDownloadFinished = false
        mRewardedAd = null
    }

    fun clear() {
        previewSlides.value?.clear()
        currentSlide.value = 1
        slidesSize.value = 0
        savingSlides.value = mutableListOf()
        cutPositions.value?.clear()
        isAdFinished = false
        isDownloadFinished = false
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