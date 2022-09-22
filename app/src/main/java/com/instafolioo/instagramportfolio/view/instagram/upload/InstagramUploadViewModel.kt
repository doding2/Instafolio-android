package com.instafolioo.instagramportfolio.view.instagram.upload

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InstagramUploadViewModel: ViewModel() {

    val content = MutableLiveData<String>().apply {
        value = ""
    }

}