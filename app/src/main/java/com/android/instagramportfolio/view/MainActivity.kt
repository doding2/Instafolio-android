package com.android.instagramportfolio.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.databinding.adapters.ViewBindingAdapter.setPadding
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.ActivityMainBinding
import com.android.instagramportfolio.extension.getNaviBarHeight
import com.android.instagramportfolio.extension.getStatusBarHeight

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
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