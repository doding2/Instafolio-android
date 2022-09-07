package com.android.instagramportfolio.view.common

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.ActivityMainBinding

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

    // 요청한 권한 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // This method call must be launched here to delegate permission granted results To [PermissionRequester].
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}