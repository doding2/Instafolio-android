package com.instafolioo.instagramportfolio.view.preview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.rewarded.RewardedAd
import com.instafolioo.instagramportfolio.model.PreviewSlide
import com.instafolioo.instagramportfolio.model.ResultSlide

class PreviewViewModel(application: Application): AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext

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

    fun loadAd() {
        return

//        viewModelScope.launch(Dispatchers.Main) {
//            val adRequest = AdRequest.Builder().build()
//
//            val callback = object: RewardedAdLoadCallback() {
//                override fun onAdFailedToLoad(adError: LoadAdError) {
//                    // 로딩 실패
//                    Log.d("Fragment", "광고 로딩 실패")
//                }
//                override fun onAdLoaded(rewardedAd: RewardedAd) {
//                    mRewardedAd = rewardedAd
//                    Log.d("Fragment", "광고 로딩 성공")
//                }
//            }
//
//            RewardedAd.load(context, context.resources.getString(R.string.test_admob_id_rewarded_ad), adRequest, callback)
//        }
    }

    fun clear() {
        previewSlides.value?.clear()
        currentSlide.value = 1
        slidesSize.value = 0
        savingSlides.value = mutableListOf()
        cutPositions.value?.clear()
        isDownloadFinished = false
        isAdFinished = false
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