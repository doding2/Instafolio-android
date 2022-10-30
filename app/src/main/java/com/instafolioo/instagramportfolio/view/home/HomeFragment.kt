package com.instafolioo.instagramportfolio.view.home

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.databinding.FragmentHomeBinding
import com.instafolioo.instagramportfolio.databinding.ItemResultSlideBinding
import com.instafolioo.instagramportfolio.extension.*
import com.instafolioo.instagramportfolio.model.ResultSlide
import com.instafolioo.instagramportfolio.model.getEmptyResultSlides
import com.instafolioo.instagramportfolio.view.common.FirebaseAnalyticsViewModel
import com.instafolioo.instagramportfolio.view.common.FirebaseAnalyticsViewModelFactory
import com.instafolioo.instagramportfolio.view.common.MainActivity
import com.instafolioo.instagramportfolio.view.common.delegates.ActivityLayoutSpecifier
import com.instafolioo.instagramportfolio.view.common.delegates.ActivityLayoutSpecifierDelegate
import com.instafolioo.instagramportfolio.view.slide.SlideViewModel

class HomeFragment : Fragment(),
    MainActivity.OnBackPressedListener,
    ActivityLayoutSpecifier by ActivityLayoutSpecifierDelegate()
{

    companion object {
        const val TAG = "HomeFragment"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var adapter: ResultSlideAdapter

    // bottom sheet 동작 설정
    private lateinit var behavior: BottomSheetBehavior<FrameLayout>

    private lateinit var slideViewModel: SlideViewModel
    private lateinit var analyticsViewModel: FirebaseAnalyticsViewModel

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, com.instafolioo.instagramportfolio.R.layout.fragment_home, container, false)
        val factory = HomeViewModelFactory(requireActivity())
        homeViewModel = ViewModelProvider(requireActivity(), factory)[HomeViewModel::class.java]
        slideViewModel= ViewModelProvider(requireActivity())[SlideViewModel::class.java]
        val analyticsFactory = FirebaseAnalyticsViewModelFactory(requireActivity())
        analyticsViewModel = ViewModelProvider(requireActivity(), analyticsFactory)[FirebaseAnalyticsViewModel::class.java]

        setRootPadding()

        // 리사이클러 뷰 설정
        adapter = ResultSlideAdapter(arrayListOf(), ::onItemClick,
            homeViewModel.selectedResultSlides, homeViewModel.isEditMode)
        binding.recyclerView.adapter = this.adapter


        // 인스타 파일들 재등록
        homeViewModel.resultSlides.observe(viewLifecycleOwner) {
            if (it != null) {
                showTooltip()
                checkInternetPermission()
            }

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


        // 편집모드
        enableEditButton()
        var isEditModeInit = true
        var initialLocation = 0f
        homeViewModel.isEditMode.observe(viewLifecycleOwner) {
            binding.layoutEditMode.root.run {
                post {
                    val collapsed = binding.layoutRoot.height.toFloat()
                    val expanded = initialLocation

                    if (isEditModeInit) {
                        isEditModeInit = false

                        if (initialLocation == 0f)
                            initialLocation = binding.layoutEditMode.root.y
                        if (!homeViewModel.selectedResultSlides.value.isNullOrEmpty())
                            return@post

                        animate()
                            .y(collapsed)
                            .setDuration(0)
                            .setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    binding.layoutEditMode.root.visibility = View.GONE
                                }
                            })
                        return@post
                    }

                    if (it) {
                        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        animate()
                            .y(expanded)
                            .setDuration(200)
                            .setListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationStart(animation: Animator?) {
                                    binding.layoutEditMode.root.visibility = View.VISIBLE
                                }
                            })
                    } else {
                        adapter.disableEditMode()
                        animate()
                            .y(collapsed)
                            .setDuration(200)
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
            if (homeViewModel.isEditMode()) {
                homeViewModel.selectedResultSlides.value?.let {
                    analyticsViewModel.logEventCancelResultSlides(it)
                }

                homeViewModel.isEditMode.value = false
            }
        }

        // 선택된 슬라이드들 열기
        binding.layoutEditMode.buttonEdit.setOnClickListener {
            if (homeViewModel.isEditMode()) {
                homeViewModel.selectedResultSlides.value?.let {
                    analyticsViewModel.logEventEditResultSlides(it)
                }

                openResultSlides()
            }
        }

        // 슬라이드 삭제
        binding.layoutEditMode.buttonDelete.setOnClickListener {
            if (homeViewModel.isEditMode()) {
                showDeleteDialog(
                    title = "프로젝트 삭제",
                    message = "선택한 파일을 삭제하시겠습니까?",
                    onOk = {
                        homeViewModel.selectedResultSlides.value?.let {
                            analyticsViewModel.logEventDeleteResultSlides(it)
                        }

                        deleteResultSlides()
                    }
                )
            }
        }

        return binding.root
    }

    private fun setRootPadding() {
        binding.apply {
            val layoutManager = GridLayoutManager(requireContext(), 3)
            recyclerView.layoutManager = layoutManager

            setStatusBarColor(activity, root, Color.WHITE, true)
            setNavigationBarColor(activity, root, Color.WHITE, true)
            setOrientationActions(
                activity = activity,
                onPortrait = {
                    layoutRoot.setPadding(0, getStatusBarHeight(), 0, getNavigationBarHeight())
                },
                onLeftLandscape = {
                    layoutRoot.setPadding(0, getStatusBarHeight(), getNavigationBarHeight(), 0)
                    recyclerView.layoutManager = layoutManager.apply {
                        spanCount = 6
                    }
                },
                onRightLandscape = {
                    layoutRoot.setPadding(getNavigationBarHeight(), getStatusBarHeight(), 0, 0)
                    recyclerView.layoutManager = layoutManager.apply {
                        spanCount = 6
                    }
                }
            )
        }
    }

    // 퍼미션 런처
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showMessageDialog(
                title = "권한요청",
                message = "인터넷 권한을 수락해주세요",
                onDismiss = requireActivity()::finish
            )
        }
    }

    private fun checkInternetPermission() {
        val pi = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
        val versionCode = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode else pi.versionCode.toLong()

        pi.versionCode

        if (versionCode < 4) {
            val isGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.INTERNET)
            if (isGranted != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.INTERNET)
            }
        }
    }

    // 앱 처음 실행이면 툴팁 보이기
    private fun showTooltip() {
        if (!homeViewModel.isFirstExecution) return

        homeViewModel.isFirstExecution = false
        analyticsViewModel.logEventTooltip()
        findNavController().navigate(R.id.action_homeFragment_to_tooltipFragment)
    }

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
            .navigate(R.id.action_homeFragment_to_slideFragment)

        // 이동후에는 편집모드 해제
        binding.root.postDelayed({
            homeViewModel.isEditMode.value = false
            homeViewModel.selectedResultSlides.value?.clear()
        }, 200)
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
            selectedResultSlides.value?.clear()
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
            analyticsViewModel.logEventSelectResultSlide(resultSlide)

            homeViewModel.isEditMode.value = true
            homeViewModel.selectedResultSlides.value?.clear()
            binding.imageChecked.visibility = View.VISIBLE
            homeViewModel.selectedResultSlides.value!!.add(resultSlide)

            enableEditButton()
            return
        }

        // 아이템 선택 해제
        if (resultSlide in homeViewModel.selectedResultSlides.value!!) {
            analyticsViewModel.logEventUnselectResultSlide(resultSlide)

            homeViewModel.selectedResultSlides.value!!.remove(resultSlide)
            binding.imageChecked.visibility = View.GONE

            enableEditButton()

            // 선택된 아이템이 아무것도 없게 된다면
            // 편집모드 해제
            if (homeViewModel.selectedResultSlides.value.isNullOrEmpty())
                homeViewModel.isEditMode.value = false
        }
        // 선택
        else {
            analyticsViewModel.logEventSelectResultSlide(resultSlide)

            homeViewModel.selectedResultSlides.value!!.add(resultSlide)
            binding.imageChecked.visibility = View.VISIBLE

            enableEditButton()
        }
    }

    // 선택된 슬라이드가 2개 이상이면 편집 버튼 안 보이게
    private fun enableEditButton() {
        binding.layoutEditMode.buttonEdit.apply {
            if (homeViewModel.selectedResultSlides.value!!.size <= 1) {
                isEnabled = true

                animate()
                    .alpha(1f)
                    .setDuration(150)
                    .setListener(object: AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            visibility = View.VISIBLE
                        }
                    })
            }
            else {
                isEnabled = false

                animate()
                    .alpha(0f)
                    .setDuration(150)
                    .setListener(object: AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            this@apply.visibility = View.GONE
                        }
                    })
            }
        }
    }

    // 뒤로가기 할 때 편집보드 켜져있으면 끔
    // sourceOfFiles bottom sheet이 확장되어 있으면 축소시킴
    override fun onBackPressed() {
        if (homeViewModel.isEditMode()) {
            homeViewModel.selectedResultSlides.value?.let {
                analyticsViewModel.logEventCancelResultSlides(it)
            }

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

        var imageUriCount = 0L
        var pdfUriCount = 0L
        val format = StringBuffer()

        // 만약을 대비해 이미지와 pdf만 존재하도록 필터링
        for (uri in uriList) {
            val fileName: String? = getFileName(uri)
            val formatted = fileName?.substringAfterLast(".")
            formatted?.let {
                if (format.contains(it)) return@let

                if (imageUriCount != 0L || pdfUriCount != 0L)
                    format.append("_")

                format.append(formatted)
            }

            // 이 uri가 이미지
            if (checkIsImage(uri)) {
                uriWithExtension.add(uri to "image")
                imageUriCount++
            }
            // 이 uri가 pdf
            else if (fileName != null && fileName.extension == "pdf") {
                uriWithExtension.add(uri to "pdf")
                pdfUriCount++
            }
        }

        if (imageUriCount > 0)
            analyticsViewModel.logEventLoadFromGallery(imageUriCount, format.toString())
        if (pdfUriCount > 0)
            analyticsViewModel.logEventLoadFromDrive(pdfUriCount, format.toString())

        if (uriWithExtension.isEmpty()) {
            showAlertDialog("지원하지 않는 형식의 파일입니다")
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