package com.instafolioo.instagramportfolio.view.common.delegates

import android.app.Activity
import android.content.res.Resources
import android.os.Build
import android.view.Surface
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.view.WindowInsetsControllerCompat

class ActivityLayoutSpecifierDelegate : ActivityLayoutSpecifier {

    override fun getStatusBarHeight(): Int {
        val resourceId: Int = Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            Resources.getSystem().getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    override fun getNavigationBarHeight(): Int {
        val resourceId: Int = Resources.getSystem().getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            Resources.getSystem().getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    // activity의 view가 status와 navigation의 영역을 침범하도록 확장
    override fun extendRootViewLayout(window: Window?) {
        if (Build.VERSION.SDK_INT < 25) return

        window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    override fun setOrientationActions(
        activity: Activity?,
        onPortrait: () -> Unit,
        onLeftLandscape: () -> Unit,
        onRightLandscape: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT < 24) return

        val orientation = activity?.display?.rotation ?: return
        when (orientation) {
            Surface.ROTATION_0,
            Surface.ROTATION_180 -> onPortrait()
            Surface.ROTATION_90 -> onLeftLandscape()
            Surface.ROTATION_270 -> onRightLandscape()
        }
    }

    override fun setStatusBarColor(
        activity: Activity?,
        rootView: View?,
        @ColorInt color: Int,
        isLight: Boolean
    ) {
        activity?.window?.apply {
            statusBarColor = color
            rootView?.also {
                WindowInsetsControllerCompat(this, rootView).apply {
                    isAppearanceLightStatusBars = isLight
                }
            }
        }
    }

    override fun setNavigationBarColor(
        activity: Activity?,
        rootView: View?,
        @ColorInt color: Int,
        isLight: Boolean
    ) {
        activity?.window?.apply {
            navigationBarColor = color
            rootView?.also {
                WindowInsetsControllerCompat(this, rootView).apply {
                    isAppearanceLightNavigationBars = isLight
                }
            }
        }
    }
}