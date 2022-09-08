package com.android.instagramportfolio.view.home

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.FragmentHomeBinding
import com.android.instagramportfolio.extension.*
import com.android.instagramportfolio.model.InstarFile
import com.android.instagramportfolio.view.common.MainActivity
import com.android.instagramportfolio.view.slide.SlideViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior


class HomeFragment : Fragment(), MainActivity.OnBackPressedListener {


    companion object {
        const val TAG = "HomeFragment"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var adapter: InstarFileAdapter

    // bottom sheet 동작 설정
    private lateinit var behavior: BottomSheetBehavior<FrameLayout>

    // 선택된 bitmap들을 SlideFragment로 전달하기 위한 뷰 모델
    private lateinit var slideViewModel: SlideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        val factory = HomeViewModelFactory(requireActivity())
        homeViewModel = ViewModelProvider(requireActivity(), factory)[HomeViewModel::class.java]
        slideViewModel= ViewModelProvider(requireActivity())[SlideViewModel::class.java]

        //status bar와 navigation bar 모두 투명하게 만드는 코드
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // ui가 아랫부분 navitaion bar를 침범하지 않도록 패딩
        binding.layoutRoot.setPadding(0, 0, 0, getNaviBarHeight())
        // 앱 실행 화면이 ui xml과 똑같이 보이도록 패딩
        binding.layoutUser.setPadding(0, getStatusBarHeight(), 0, 0)

        // status bar가 밝은 색이라는 것을 알림
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightStatusBars = true

        // 리사이클러 뷰 설정
        adapter = InstarFileAdapter(arrayListOf(), ::onItemClick)
        binding.recyclerView.adapter = this.adapter

        // 인스타 파일들 재등록
        homeViewModel.instarFiles.observe(viewLifecycleOwner) {
            adapter.replaceItems(it)
        }

        // source of files bottom sheet 설정하기
        // bottom sheet가 최대로 확장됐을 때 status bar 부분 침범하지 않도록 마진
        val metrics = resources.displayMetrics
        val screenHeight = metrics.heightPixels
        val sourceOfFilesBehavior = BottomSheetBehavior.from(binding.layoutFilesBottomSheet)
        sourceOfFilesBehavior.maxHeight = screenHeight - requireContext().pxToDp(getStatusBarHeight())
        binding.layoutUser.post {
            sourceOfFilesBehavior.peekHeight = screenHeight - binding.layoutUser.height
        }


        //  Navigation bottom sheet 설정하기
        // 기본적으로 드래그 비활성화
        behavior = BottomSheetBehavior.from(binding.navigationAndBottomSheet)
        behavior.isDraggable = false
        // 맨 위 선택지 처음엔 비활성화
        binding.buttonDirectory.isEnabled = false
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

        // 내 디렉토리에서 파일 가져오기
        binding.buttonDirectory.setOnClickListener {
            requestToDirectory()
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        // 내 갤러리에서 파일 가져오기
        binding.buttonGallery.setOnClickListener {
            requestToGallery()
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

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

    // 내 디렉토리에서 파일들 가져오기
    private fun requestToDirectory() {
        val mimeTypes = arrayOf("application/pdf")

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        resultLauncher.launch(intent)
    }


    // 내 갤러리에서 파일들 가져오기
    private fun requestToGallery() {
        val mimeTypes = arrayOf("image/*")

        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        resultLauncher.launch(intent)
    }
    
    // activity result listener
    private val resultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) launcher@{
            if (it.resultCode != RESULT_OK) return@launcher
            val intent = it.data ?: return@launcher

            val requestCode = intent.getStringExtra("requestCode")

            // pdf 파일/이미지 가져옴
            if (requestCode == null) {
                // 리턴된 결과를 처리함
                handleResult(intent)
            }
            // 모든 파일 관리 허용 퍼미션 수락하고 옴
            else {
                Log.d(TAG, "퍼미션")
            }
        }

    // 리턴된 결과를 다루는 함수
    private fun handleResult(intent: Intent) {
        // 리턴된 인텐트를 가지고 파일 uri 리스트로 변환
        val uriList = getUriListFrom(intent)

        // 이미지와 pdf만 분별하여 넣을 리스트 (확장자별로 분류도 시킬 예정)
        val uriWithExtension = ArrayList<Pair<Uri, String>>()

        // 만약을 대비해 이미지와 pdf만 존재하도록 필터링
        for (uri in uriList) {
            val fileName: String? = getFileName(uri)

            // 이 uri가 이미지
            if (checkIsImage(uri)) {
                uriWithExtension.add(uri to "image")
            }
            // 이 uri가 pdf
            else if (fileName != null && fileName.extension == "pdf") {
                uriWithExtension.add(uri to "pdf")
            }
        }

        if (uriWithExtension.isEmpty()) {
            Toast.makeText(requireContext(), "이미지와 PDF 파일만을 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }


        // uri 리스트를 Slide Fragment로 전달
        slideViewModel.clear()
        slideViewModel.uriWithExtension.value = uriWithExtension

        // Slide Fragment로 이동
        findNavController()
            .navigate(R.id.action_homeFragment_to_slideFragment)
    }

    // 인텐트를 가지고 파일 uri 리스트로 변환
    private fun getUriListFrom(intent: Intent): MutableList<Uri> {
        val fileUriList = arrayListOf<Uri>()

        // 선택된 파일이 여러개인 경우
        if (intent.clipData != null) {
            for (i in 0 until intent.clipData!!.itemCount) {
                val uri = intent.clipData!!.getItemAt(i).uri
                fileUriList.add(uri)
            }
        }
        // 선택된 파일이 하나인 경우
        else if (intent.data != null) {
            val uri = intent.data!!
            fileUriList.add(uri)
        }
        // 파일을 아무것도 선택 안 했을 경우
        else {
            // Nothing to do
        }

        return fileUriList
    }

    // navigation bottom sheet이 확장됐을 때 관련 뷰들도 활성화
    private fun setViewsExpanded() {

        // + 버튼 비활성화(애니메이션 없이)
        binding.buttonShowSheet.visibility = View.GONE
        binding.buttonShowSheet.isClickable = false

        // 블랙 스크린 활성화
        binding.layoutBlockScreen.visibility = View.VISIBLE

        // 맨 위 선택지 클릭 활성화
        binding.buttonDirectory.isEnabled = true
        binding.buttonDirectory.isClickable = true
        binding.buttonDirectory.isFocusable = true
    }

    // navigation bottom sheet이 줄어들었을 때 관련 뷰들도 비활성화
    private fun setViewsCollapsed() {
        // 맨 위 선택지 클릭 비활성화
        binding.buttonDirectory.isEnabled = false
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