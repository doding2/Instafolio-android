package com.android.instagramportfolio.view.preview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.instagramportfolio.model.PreviewSlide

class PreviewViewModel: ViewModel() {

    val previewSlides = MutableLiveData<MutableList<PreviewSlide>>().apply {
        value = mutableListOf()
    }

}