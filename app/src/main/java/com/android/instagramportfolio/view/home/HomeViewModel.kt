package com.android.instagramportfolio.view.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.android.instagramportfolio.model.InstarFile

class HomeViewModel(context: Context): ViewModel() {

    val instarFiles = liveData {
        emit(
            arrayListOf(
                InstarFile("사과"),
                InstarFile("망고"),
                InstarFile("수박"),
                InstarFile("옥수수"),
                InstarFile("바나나"),
                InstarFile("라임"),
                InstarFile("오렌지"),
                InstarFile("배"),
                InstarFile("귤"),
                InstarFile("파인애플"),
                InstarFile("포도"),
                InstarFile("사과"),
                InstarFile("망고"),
                InstarFile("수박"),
                InstarFile("옥수수"),
                InstarFile("바나나"),
                InstarFile("라임"),
                InstarFile("오렌지"),
                InstarFile("배"),
                InstarFile("귤"),
                InstarFile("파인애플"),
                InstarFile("포도"),
                InstarFile("사과"),
                InstarFile("망고"),
                InstarFile("수박"),
                InstarFile("옥수수"),
                InstarFile("바나나"),
                InstarFile("라임"),
                InstarFile("오렌지"),
                InstarFile("배"),
                InstarFile("귤"),
                InstarFile("파인애플"),
                InstarFile("포도"),
            )
        )
    }

}