package com.instafolioo.instagramportfolio.view.tooltip

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.databinding.FragmentTooltipBinding
import com.instafolioo.instagramportfolio.extension.getNaviBarHeight
import com.instafolioo.instagramportfolio.extension.getStatusBarHeight
import com.instafolioo.instagramportfolio.view.common.FirebaseAnalyticsViewModel
import com.instafolioo.instagramportfolio.view.common.FirebaseAnalyticsViewModelFactory
import com.instafolioo.instagramportfolio.view.common.MainActivity

class TooltipFragment : Fragment(), MainActivity.OnBackPressedListener {

    private var _binding: FragmentTooltipBinding? = null
    private val binding get() = _binding!!

    private lateinit var tooltipTexts: ArrayList<String>


    private lateinit var analyticsViewModel: FirebaseAnalyticsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tooltip, container, false)
        val analyticsFactory = FirebaseAnalyticsViewModelFactory(requireActivity())
        analyticsViewModel = ViewModelProvider(requireActivity(), analyticsFactory)[FirebaseAnalyticsViewModel::class.java]

        setRootPadding()
        setTooltipAdapter()

        binding.buttonCancel.setOnClickListener { onBackPressed() }

        return binding.root
    }

    // dp to px
    private fun dpToPx(dp: Int): Int {
        val scale = requireContext().resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun setRootPadding() {
        activity?.window?.apply {
            statusBarColor = Color.WHITE
            navigationBarColor = Color.WHITE
            WindowInsetsControllerCompat(this, binding.root).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }

        if(Build.VERSION.SDK_INT < 24) {
            binding.textTooltip.layoutParams = (binding.textTooltip.layoutParams as RelativeLayout.LayoutParams).apply {
                topMargin = dpToPx(29)
            }
            return
        }

        when (requireActivity().display?.rotation) {
            Surface.ROTATION_90 -> {
                binding.layoutRoot.setPadding(0, getStatusBarHeight(), getNaviBarHeight(), 0)
                binding.textTooltip.layoutParams = (binding.textTooltip.layoutParams as RelativeLayout.LayoutParams).apply {
                    topMargin = 0
                }
            }
            Surface.ROTATION_270 -> {
                binding.layoutRoot.setPadding(getNaviBarHeight(), getStatusBarHeight(), 0, 0)
                binding.textTooltip.layoutParams = (binding.textTooltip.layoutParams as RelativeLayout.LayoutParams).apply {
                    topMargin = 0
                }
            }
            else -> {
                binding.layoutRoot.setPadding(0, getStatusBarHeight(), 0, getNaviBarHeight())
                binding.textTooltip.layoutParams = (binding.textTooltip.layoutParams as RelativeLayout.LayoutParams).apply {
                    topMargin = dpToPx(29)
                }
            }
        }
    }

    private fun setTooltipAdapter() {
        tooltipTexts = arrayListOf(
            getString(R.string.tooltip_text_1),
            getString(R.string.tooltip_text_2),
            getString(R.string.tooltip_text_3),
            getString(R.string.tooltip_text_4),
            getString(R.string.tooltip_text_5)
        )
        binding.textTooltip.text = tooltipTexts[0]

        binding.recyclerView.apply {
            adapter = TooltipAdapter(tooltipTexts)

            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)

            binding.scrollIndicator.attachToRecyclerView(this)

            addOnScrollListener(object: RecyclerView.OnScrollListener() {
                var currentPosition = 0

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val view = snapHelper.findSnapView(recyclerView.layoutManager ?: return)
                    view?.let {
                        val position = recyclerView.layoutManager!!.getPosition(it)

                        if (currentPosition != position) {
                            currentPosition = position
                            binding.textTooltip.text = tooltipTexts[position]
                        }
                    }
                }
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE ->
                            binding.recyclerView.smoothScrollToPosition(currentPosition)
                    }
                }
            })

            adjustScreen()
        }
    }

    private fun adjustScreen() {
        binding.recyclerView.apply {
            post {
                val position = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                val itemView = findViewHolderForAdapterPosition(position)?.itemView
                itemView?.let {
                    val height = binding.layoutRoot.height
                    val upperHeight = binding.run {
                        dpToPx(12) + buttonCancel.height + dpToPx(29) + textTooltip.height
                    }
                    val lowerHeight = binding.run {
                        scrollIndicator.height + it.height
                    }

                    var margin = height - upperHeight - lowerHeight
                    margin = when (requireActivity().display?.rotation) {
                        Surface.ROTATION_90, Surface.ROTATION_270 ->
                            if (margin <= 10) dpToPx(10) else margin
                        else -> margin
                    }
                    binding.scrollIndicator.layoutParams = (binding.scrollIndicator.layoutParams as RelativeLayout.LayoutParams).apply {
                        topMargin = margin
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        analyticsViewModel.logEventBackFromTooltip()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}