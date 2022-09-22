package com.instafolioo.instagramportfolio.view.instagram.log_in

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InstagramLogInViewModel: ViewModel() {

    val nickname = MutableLiveData<String>().apply {
        value = ""
    }

    val password = MutableLiveData<String>().apply {
        value = ""
    }

}