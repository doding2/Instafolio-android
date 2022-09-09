package com.android.instagramportfolio.view.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.android.instagramportfolio.model.SlideResult

class HomeViewModel(context: Context): ViewModel() {

    val instarFiles = liveData {
        emit(
            arrayListOf(
                SlideResult("사과"),
                SlideResult("망고"),
                SlideResult("수박"),
                SlideResult("옥수수"),
                SlideResult("바나나"),
                SlideResult("라임"),
                SlideResult("오렌지"),
                SlideResult("배"),
                SlideResult("귤"),
                SlideResult("파인애플"),
                SlideResult("포도"),
                SlideResult("사과"),
                SlideResult("망고"),
                SlideResult("수박"),
                SlideResult("옥수수"),
                SlideResult("바나나"),
                SlideResult("라임"),
                SlideResult("오렌지"),
                SlideResult("배"),
                SlideResult("귤"),
                SlideResult("파인애플"),
                SlideResult("포도"),
                SlideResult("사과"),
                SlideResult("망고"),
                SlideResult("수박"),
                SlideResult("옥수수"),
                SlideResult("바나나"),
                SlideResult("라임"),
                SlideResult("오렌지"),
                SlideResult("배"),
                SlideResult("귤"),
                SlideResult("파인애플"),
                SlideResult("포도"),
            )
        )
    }

}