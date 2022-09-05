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
}