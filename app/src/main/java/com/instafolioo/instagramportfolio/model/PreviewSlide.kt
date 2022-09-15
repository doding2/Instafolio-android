package com.instafolioo.instagramportfolio.model

import android.graphics.Bitmap

data class PreviewSlide(
    val bitmap: Bitmap,
    val bitmapSecond: Bitmap? = null,
    val viewType: Int
) {
    companion object {
        const val ORIGINAL = 0
        const val INSTAR_SIZE = 1
        const val ORIGINAL_BINDING = 2
        const val INSTAR_SIZE_BINDING = 3
    }
}
