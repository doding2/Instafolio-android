package com.instafolioo.instagramportfolio.view.common

import android.os.Build
import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.MobileAds
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.databinding.ActivityMainBinding
import com.instafolioo.instagramportfolio.extension.getNaviBarHeight
import com.instafolioo.instagramportfolio.extension.getStatusBarHeight
import com.instafolioo.instagramportfolio.view.home.HomeViewModel
import com.instafolioo.instagramportfolio.view.home.HomeViewModelFactory
import com.instafolioo.instagramportfolio.view.preview.PreviewViewModel


class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var previewViewModel: PreviewViewModel
    private lateinit var analyticsViewModel: FirebaseAnalyticsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val factory = HomeViewModelFactory(this)
        homeViewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
        previewViewModel = ViewModelProvider(this)[PreviewViewModel::class.java]
        val analyticsFactory = FirebaseAnalyticsViewModelFactory(this)
        analyticsViewModel = ViewModelProvider(this, analyticsFactory)[FirebaseAnalyticsViewModel::class.java]

        if(Build.VERSION.SDK_INT >= 25) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        MobileAds.initialize(this)
        previewViewModel.loadAd()

        // splash screen 동안 result slides 로딩
        binding.layoutRoot.viewTreeObserver.addOnPreDrawListener(
            object: ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (homeViewModel.isReady) {
                        binding.layoutRoot.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }

            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    // 프래그먼트들에서 onBackPressed()를 지원하기 위한 인터페이스
    interface OnBackPressedListener {
        fun onBackPressed()
    }

    override fun onBackPressed() {
        val fragmentList = supportFragmentManager.fragments.toMutableList()
        fragmentList += fragmentList[0].childFragmentManager.fragments

        for (fragment in fragmentList) {
            if (fragment is OnBackPressedListener) {
                fragment.onBackPressed()
                return
            }
        }

        super.onBackPressed()
    }
}