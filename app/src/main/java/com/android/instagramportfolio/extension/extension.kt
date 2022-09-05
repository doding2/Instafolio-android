package com.android.instagramportfolio.extension

import android.content.Context

/** status bar와
 * navigation bar를
 * 투명하게 만든 후
 * 높이 조정을 위해 필요 */
// status bar의 높이 구하는 확장함수
fun Context.getStatusBarHeight(): Int {
    val resourceId = this.resources.getIdentifier("status_bar_height", "dimen", "android")

    return if (resourceId > 0) {
        this.resources.getDimensionPixelSize(resourceId)
    } else {
        0
    }
}

// navigation bar의 높이 구하는 확장함수
fun Context.getNaviBarHeight(): Int {
    val resourceId: Int = this.resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        this.resources.getDimensionPixelSize(resourceId)
    } else {
        0
    }
}