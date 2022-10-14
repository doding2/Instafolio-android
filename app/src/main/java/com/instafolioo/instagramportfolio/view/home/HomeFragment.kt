package com.instafolioo.instagramportfolio.view.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.instafolioo.instagramportfolio.databinding.FragmentHomeBinding
import com.instafolioo.instagramportfolio.databinding.ItemResultSlideBinding
import com.instafolioo.instagramportfolio.extension.*
import com.instafolioo.instagramportfolio.model.ResultSlide
import com.instafolioo.instagramportfolio.model.getEmptyResultSlides
import com.instafolioo.instagramportfolio.view.common.MainActivity
import com.instafolioo.instagramportfolio.view.slide.SlideViewModel


class HomeFragment : Fragment(), MainActivity.OnBackPressedListener {


    companion object {
        const val TAG = "HomeFragment"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var adapter: ResultSlideAdapter

    // bottom sheet 동작 설정
    private lateinit var behavior: BottomSheetBehavior<FrameLayout>

    // 선택된 bitmap들을 SlideFragment로 전달하기 위한 뷰 모델
    private lateinit var slideViewModel: SlideViewModel

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, com.instafolioo.instagramportfolio.R.layout.fragment_home, container, false)
        val factory = HomeViewModelFactory(requireActivity())
        homeViewModel = ViewModelProvider(requireActivity(), factory)[HomeViewModel::class.java]
        slideViewModel= ViewModelProvider(requireActivity())[SlideViewModel::class.java]

        when (requireActivity().display?.rotation) {
            // 폰이 왼쪽으로 누움
            Surface.ROTATION_90 -> {
                binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 6)
            }
            // 폰이 오른쪽으로 누움
            Surface.ROTATION_270 -> {
                binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 6)
            }
            // 그 외는 그냥 정방향으으로 처리함
            else -> {
                // 리사이클러뷰 한 줄에 아이템 3개씩 오도록 조정
                binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
            }
        }


        // status bar, navigation bar가 밝은 색이라는 것을 알림
        requireActivity().window.statusBarColor = Color.WHITE
        WindowInsetsControllerCompat(requireActivity().window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }


        // 리사이클러 뷰 설정
        adapter = ResultSlideAdapter(arrayListOf(), ::onItemClick,
            homeViewModel.selectedResultSlides, homeViewModel.isEditMode)
        binding.recyclerView.adapter = this.adapter



        // 인스타 파일들 재등록
        homeViewModel.resultSlides.observe(viewLifecycleOwner) {

            binding.textNoResultSlide.visibility =
                if (it.isEmpty()) View.VISIBLE else View.GONE

            if (it.isNullOrEmpty()) {
                adapter.replaceItems(getEmptyResultSlides())
                binding.recyclerView.setOnTouchListener { _, _ -> true }
                return@observe
            }

            binding.recyclerView.setOnTouchListener(null)
            adapter.replaceItems(it)
        }

        //  Navigation bottom sheet 설정하기
        // 기본적으로 드래그 비활성화
        behavior = BottomSheetBehavior.from(binding.navigationAndBottomSheet)
        behavior.isDraggable = false
        // 맨 위 선택지 처음엔 비활성화
        binding.buttonDirectory.isEnabled = false
        binding.buttonGallery.isEnabled = false
        setListOpacity(0)

        // 파일 종류 선택 bottom sheet 확장시키기
        binding.buttonShowSheet.setOnClickListener {
            // 편집모드가 아니라면 파일 선택
            if (!homeViewModel.isEditMode()) {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                setViewsExpanded()
            }
            // 편집모드라면 선택된 파일 열기
            else {
                openResultSlides()
            }
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

        var isEditModeInit = true

        // 편집모드 변동 관찰
        homeViewModel.isEditMode.observe(viewLifecycleOwner) {
            if (isEditModeInit) {
                isEditModeInit = false
                binding.layoutEditMode.root.run {
                    post {
                        animate()
                            .translationYBy(height.toFloat())
                            .setDuration(0)
                            .setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    binding.layoutEditMode.root.visibility = View.GONE
                                }
                            })
                    }
                }
                return@observe
            }

            if (it) {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                adapter.enableEditMode(true)

                binding.layoutEditMode.root.run {
                    post {
                        animate()
                            .translationYBy(-height.toFloat())
                            .setDuration(150)
                            .setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationStart(animation: Animator?) {
                                    binding.layoutEditMode.root.visibility = View.VISIBLE
                                }
                            })
                    }
                }
            } else {
                adapter.enableEditMode(false)
                binding.layoutEditMode.root.run {
                    post {
                        animate()
                            .translationYBy(height.toFloat())
                            .setDuration(150)
                            .setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    binding.layoutEditMode.root.visibility = View.GONE
                                }
                            })
                    }
                }
            }
        }

        // 편집모드 취소
        binding.layoutEditMode.buttonCancel.setOnClickListener {
            if (homeViewModel.isEditMode())
                homeViewModel.isEditMode.value = false
        }

        // 선택된 슬라이드들 열기
        binding.layoutEditMode.buttonEdit.setOnClickListener {
            if (homeViewModel.isEditMode())
                openResultSlides()
        }

        // 슬라이드 삭제
        binding.layoutEditMode.buttonDelete.setOnClickListener {
            if (homeViewModel.isEditMode()) {
                showDeleteDialog(
                    title = "프로젝트 삭제",
                    message = "선택한 파일을 삭제하시겠습니까?",
                    onOk = ::deleteResultSlides
                )
            }
        }

        return binding.root
    }

    // 폰트

    // 편집모드에서 여러 파일 동시에 열기
    private fun openResultSlides() {
        if (homeViewModel.selectedResultSlides.value.isNullOrEmpty())
            return

        // 한번 초기화
        slideViewModel.clear()

        for (resultSlide in homeViewModel.selectedResultSlides.value!!) {

            val pair = if (resultSlide.format == "pdf") {
                resultSlide to resultSlide.format
            } else {
                resultSlide to "image"
            }

            slideViewModel.resultSlideWithExtension.value!!.add(pair)
        }

        // Slide Fragment로 이동
        findNavController()
            .navigate(com.instafolioo.instagramportfolio.R.id.action_homeFragment_to_slideFragment)

        // 이동후에는 편집모드 해제
        homeViewModel.isEditMode.value = false
        homeViewModel.selectedResultSlides.value = mutableListOf()
    }


    // 편집모드에서 여러 슬라이드 삭제시키기
    private fun deleteResultSlides() {
        homeViewModel.run {
            if (selectedResultSlides.value.isNullOrEmpty())
                return

            // ConcurrentModificationException 피하기 위해
            val copyList = selectedResultSlides.value!!.toList()

            for (resultSlide in copyList) {
                deleteResultSlide(resultSlide)
                adapter.removeItem(resultSlide)
                // 이미지도 삭제
                resultSlide.deleteCache(requireContext())
            }

            // 편집모드 해제
            isEditMode.value = false

            // 선택된 놈들 초기화
            selectedResultSlides.value = mutableListOf()
        }
    }


    // result slide 클릭하면 편집 화면으로 이동
    private fun onItemClick(resultSlide: ResultSlide, binding: ItemResultSlideBinding) {
        // 슬라이드가 하나도 없을 때
        if (resultSlide.format == "empty") {
            return
        }

        // 편집모드가 아닐때
        if (!homeViewModel.isEditMode()) {
            homeViewModel.isEditMode.value = true
            homeViewModel.selectedResultSlides.value = mutableListOf()
            binding.imageChecked.visibility = View.VISIBLE
            homeViewModel.selectedResultSlides.value!!.add(resultSlide)
            return
        }

        // 아이템 선택 해제
        if (resultSlide in homeViewModel.selectedResultSlides.value!!) {
            homeViewModel.selectedResultSlides.value!!.remove(resultSlide)
            binding.imageChecked.visibility = View.GONE

            // 선택된 아이템이 아무것도 없게 된다면
            // 편집모드 해제
            if (homeViewModel.selectedResultSlides.value.isNullOrEmpty())
                homeViewModel.isEditMode.value = false
        }
        // 선택
        else {
            homeViewModel.selectedResultSlides.value!!.add(resultSlide)
            binding.imageChecked.visibility = View.VISIBLE
        }
    }

    // 뒤로가기 할 때 편집보드 켜져있으면 끔
    // sourceOfFiles bottom sheet이 확장되어 있으면 축소시킴
    override fun onBackPressed() {
        if (homeViewModel.isEditMode()) {
            homeViewModel.isEditMode.value = false
        }
        else if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
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
                Log.d(TAG, "그 이외")
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
            showAlertDialog("지원하지 않는 파일입니다.")
            return
        }


        // uri 리스트를 Slide Fragment로 전달
        slideViewModel.clear()
        slideViewModel.uriWithExtension.value = uriWithExtension

        // Slide Fragment로 이동
        findNavController()
            .navigate(com.instafolioo.instagramportfolio.R.id.action_homeFragment_to_slideFragment)
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

        binding.buttonGallery.isEnabled = true
    }

    // navigation bottom sheet이 줄어들었을 때 관련 뷰들도 비활성화
    private fun setViewsCollapsed() {
        // 맨 위 선택지 클릭 비활성화
        binding.buttonDirectory.isEnabled = false
        binding.buttonDirectory.isClickable = false
        binding.buttonDirectory.isFocusable = false

        binding.buttonGallery.isEnabled = false

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
        binding.buttonDirectory.background?.alpha = opacity
        binding.divider.background?.alpha = opacity
        binding.buttonGallery.background?.alpha = opacity

        binding.divider.background?.alpha = opacity

        binding.imageDirectory.imageAlpha = opacity
        binding.imageGallery.imageAlpha = opacity

        binding.textSheetTitle.setTextColor(binding.textSheetTitle.textColors.withAlpha(opacity))
        binding.textDirectory.setTextColor(binding.textDirectory.textColors.withAlpha(opacity))
        binding.textGallery.setTextColor(binding.textGallery.textColors.withAlpha(opacity))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}