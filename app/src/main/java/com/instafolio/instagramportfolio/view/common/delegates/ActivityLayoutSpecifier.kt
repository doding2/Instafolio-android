package com.instafolio.instagramportfolio.view.common.delegates

import android.app.Activity
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt

/**
 * status bar와 navigation bar를 포함한
 * Activity 화면 세팅을 담당함
 */
interface ActivityLayoutSpecifier {

    fun getStatusBarHeight(): Int

    fun getNavigationBarHeight(): Int

    /* activity의 view가 status와 navigation의 영역을 침범하도록 확장 */
    fun extendRootViewLayout(window: Window?)

    fun setOrientationActions(
        activity: Activity?,
        onPortrait: () -> Unit,
        onLeftLandscape: () -> Unit,
        onRightLandscape: () -> Unit)

    fun setStatusBarColor(
        activity: Activity?,
        rootView: View?,
        @ColorInt color: Int,
        isLight: Boolean)

    fun setNavigationBarColor(
        activity: Activity?,
        rootView: View?,
        @ColorInt color: Int,
        isLight: Boolean)

}