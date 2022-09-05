package com.android.instagramportfolio.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.FragmentHomeBinding
import com.android.instagramportfolio.extension.getNaviBarHeight
import com.android.instagramportfolio.extension.getStatusBarHeight
import com.android.instagramportfolio.model.InstarFile
import com.android.instagramportfolio.view.MainActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior


class HomeFragment : Fragment(), MainActivity.OnBackPressedListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: InstarFileAdapter

    // bottom sheet 동작 설정
    private lateinit var behavior: BottomSheetBehavior<FrameLayout>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        val factory = HomeViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        //status bar와 navigation bar 모두 투명하게 만드는 코드
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // ui가 아랫부분 navitaion bar를 침범하지 않도록 패딩
        binding.layoutRoot.setPadding(0, 0, 0, requireContext().getNaviBarHeight())

        // files coordinator가 최대로 확장됐을 때 status bar 부분 침범하지 않도록 마진
        val params = binding.layoutFilesBottomSheet.layoutParams as CoordinatorLayout.LayoutParams
        params.setMargins(0, requireContext().getStatusBarHeight(), 0, 0)
        binding.layoutFilesBottomSheet.layoutParams = params

        // 앱 실행 화면이 ui xml과 똑같이 보이도록 패딩
        binding.layoutUser.setPadding(0, requireContext().getStatusBarHeight(), 0, 0)


        // 리사이클러 뷰 설정
        adapter = InstarFileAdapter(arrayListOf(), ::onItemClick)
        binding.recyclerView.adapter = this.adapter

        // 인스타 파일들 재등록
        viewModel.instarFiles.observe(viewLifecycleOwner) {
            adapter.replaceItems(it)
        }

        // bottom sheet 설정하기
        // 기본적으로 드래그 비활성화
        behavior = BottomSheetBehavior.from(binding.navigationAndBottomSheet)
        behavior.isDraggable = false
        setListOpacity(0)

        // 파일 종류 선택 bottom sheet 확장시키기
        binding.buttonShowSheet.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            setViewsExpanded()
        }

        // 검은 화면 클릭하면 bottom sheet 다시 줄어들음
        binding.layoutBlockScreen.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // bottom sheet가 줄어들었을 경우 다시 드래그 불능/검은 화면 제거
        behavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED ->  behavior.isDraggable = true
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        behavior.isDraggable = false
                        setViewsCollapsed()
                    }
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // 뷰 투명도 조절
                val opacity = (slideOffset * 255).toInt()
                binding.layoutBlockScreen.background.alpha = opacity
                binding.layoutShadowedPointedBackgroundImage.background.alpha = 255 - opacity

                // 파일 선택지들의 투명도 조절
                setListOpacity(opacity)
            }
        })

        return binding.root
    }


    private fun onItemClick(instarFile: InstarFile) {
        Toast.makeText(requireContext(), instarFile.fileName, Toast.LENGTH_SHORT).show()
    }

    // 뒤로가기 할 때 sourceOfFiles bottom sheet이 확장되어 있으면 축소시킴
    override fun onBackPressed() {
        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        else {
            requireActivity().finish()
        }
    }

    // navigation bottom sheet이 확장됐을 때 관련 뷰들도 활성화
    private fun setViewsExpanded() {
        // + 버튼 비활성화(애니메이션 없이)
        binding.buttonShowSheet.visibility = View.GONE
        binding.buttonShowSheet.isClickable = false

        // 블랙 스크린 활성화
        binding.layoutBlockScreen.visibility = View.VISIBLE

        // 맨 위 선택지 클릭 활성화
        binding.buttonDirectory.isClickable = true
        binding.buttonDirectory.isFocusable = true
    }

    // navigation bottom sheet이 줄어들었을 때 관련 뷰들도 비활성화
    private fun setViewsCollapsed() {
        // 맨 위 선택지 클릭 비활성화
        binding.buttonDirectory.isClickable = false
        binding.buttonDirectory.isFocusable = false

        // + 버튼 활성화(애니메이션과 같이)
        binding.buttonShowSheet.startAnimation(
            AlphaAnimation(0.0f, 1.0f).apply {
                duration = 150
                fillBefore = true
                setAnimationListener(object: Animation.AnimationListener {
                    override fun onAnimationEnd(p0: Animation?) {
                        binding.buttonShowSheet.visibility = View.VISIBLE
                        binding.buttonShowSheet.isClickable = true
                    }
                    override fun onAnimationStart(p0: Animation?) {}
                    override fun onAnimationRepeat(p0: Animation?) {}
                })
            })

        // 블랙 스크린 비활성화
        binding.layoutBlockScreen.visibility = View.GONE
    }

    // 파일 선택지들의 투명도 조절
    private fun setListOpacity(opacity: Int) {
        binding.buttonDirectory.background.alpha = opacity

        binding.imageDirectory.imageAlpha = opacity
        binding.imageCloud.imageAlpha = opacity
        binding.imageGallery.imageAlpha = opacity

        binding.textDirectory.setTextColor(binding.textDirectory.textColors.withAlpha(opacity))
        binding.textCloud.setTextColor(binding.textCloud.textColors.withAlpha(opacity))
        binding.textGallery.setTextColor(binding.textGallery.textColors.withAlpha(opacity))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}