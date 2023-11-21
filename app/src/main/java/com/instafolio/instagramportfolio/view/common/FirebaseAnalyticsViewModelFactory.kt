package com.instafolio.instagramportfolio.view.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FirebaseAnalyticsViewModelFactory(private val context: Context): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FirebaseAnalyticsViewModel::class.java)) {
            return FirebaseAnalyticsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown View Model Class")
    }

}