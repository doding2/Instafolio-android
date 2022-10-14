package com.instafolioo.instagramportfolio.view.preview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.databinding.FragmentPreviewBinding
import com.instafolioo.instagramportfolio.extension.*
import com.instafolioo.instagramportfolio.model.PreviewSlide
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.INSTAR_SIZE
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.INSTAR_SIZE_BINDING
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.ORIGINAL
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.ORIGINAL_BINDING
import com.instafolioo.instagramportfolio.model.ResultSlide
import com.instafolioo.instagramportfolio.model.Slide
import com.instafolioo.instagramportfolio.view.common.MainActivity
import com.instafolioo.instagramportfolio.view.home.HomeViewModel
import com.instafolioo.instagramportfolio.view.home.HomeViewModelFactory
import com.instafolioo.instagramportfolio.view.slide.SlideViewModel
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round


class PreviewFragment : Fragment(), MainActivity.OnBackPressedListener {

    companion object {
        const val TAG = "PreviewFragment"
    }

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var previewViewModel: PreviewViewModel
    private lateinit var slideViewModel: SlideViewModel
    private lateinit var homeViewModel: HomeViewModel

    private lateinit var slideAdapter: PreviewSlideAdapter
    private lateinit var cutAdapter: PreviewSlideCutAdapter

    private var cutWidth = 0

    private lateinit var scroller: LinearSmoothScroller
    private var cutAreaIsScrolling = false
    private var previewAreaIsScrolling = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_preview, container, false)
        val homeFactory = HomeViewModelFactory(requireActivity())
        homeViewModel = ViewModelProvider(requireActivity(), homeFactory)[HomeViewModel::class.java]
        slideViewModel = ViewModelProvider(requireActivity())[SlideViewModel::class.java]
        previewViewModel = ViewModelProvider(requireActivity())[PreviewViewModel::class.java]
        binding.viewModel = previewViewModel
        binding.lifecycleOwner = this

        // status bar, navigation bar가 밝은 색이 아니라는 것을 알림
        requireActivity().window.statusBarColor = Color.BLACK
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightNavigationBars = false

        // 리사이클러뷰에 어답터 추가
        slideAdapter = PreviewSlideAdapter(previewViewModel.previewSlides.value!!)
        binding.recyclerView.adapter = slideAdapter

        // Recycler view를 Pager로 설정
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var currentPosition = RecyclerView.NO_POSITION

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (cutAreaIsScrolling) return

                val view = snapHelper.findSnapView(recyclerView.layoutManager ?: return)!!
                val position = recyclerView.layoutManager!!.getPosition(view)

                _binding?.root?.post {
                    val screenWidth = binding.root.width
                    val cutItemWidth = cutAdapter.getItemWidth()
                    val ddx = round(dx * cutItemWidth.toFloat() / screenWidth.toFloat()).toInt()
                    binding.recyclerViewCut.scrollBy(ddx, 0)
                }

                if (currentPosition != position) {
                    currentPosition = position
                    previewViewModel.currentSlide.value = position + 1

                    binding.textCut.text =
                        if (previewViewModel.isAlreadyCut)
                            "분할해제"
                        else
                            "분할"
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        previewAreaIsScrolling = false

                        if (currentPosition == previewViewModel.currentSlide.value!! - 1) {
                            scroller.targetPosition = currentPosition
                            binding.recyclerViewCut.layoutManager?.startSmoothScroll(scroller)
                        }
                    }
                    else -> previewAreaIsScrolling = true
                }
            }
        })


        // 분할 화면 설정
        cutAdapter = PreviewSlideCutAdapter(previewViewModel.previewSlides.value!!, previewViewModel.cutPositions, ::onCutItemClick)
        binding.recyclerViewCut.adapter = cutAdapter

        val snapHelper2 = LinearSnapHelper()
        snapHelper2.attachToRecyclerView(binding.recyclerViewCut)
        binding.recyclerViewCut.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var currentPosition = RecyclerView.NO_POSITION

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (previewAreaIsScrolling) return

                val view = snapHelper2.findSnapView(recyclerView.layoutManager ?: return)!!
                val position = recyclerView.layoutManager!!.getPosition(view)

                if (currentPosition != position) {
                    currentPosition = position
                    previewViewModel.currentSlide.value = position + 1
                    binding.recyclerView.scrollToPosition(position)

                    binding.textCut.text =
                        if (previewViewModel.isAlreadyCut)
                            "분할해제"
                        else
                            "분할"
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                cutAreaIsScrolling = when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> false
                    else -> true
                }
            }
        })

        scroller = object: LinearSmoothScroller(requireContext()) {
            override fun getHorizontalSnapPreference(): Int {
                return SNAP_TO_END
            }
        }



        // Slide들을 PreviewSlide로 변환
        processSlidesIntoPreviewSlides()

        // 다운로드 버튼
        binding.buttonDownload.setOnClickListener {
            // write external storage 퍼미션 체크
            val isGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (isGranted == PackageManager.PERMISSION_GRANTED) {
                showDialog()
            } else {
                // write storage 퍼미션 요청
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }


        // 분할 버튼 클릭
        binding.buttonCut.setOnClickListener {
            previewViewModel.apply {
                if (isAlreadyCut) {
                    removeCutPosition()
                    cutAdapter.paste(previewViewModel.currentSlide.value!! - 1)
                    binding.textCut.text = "분할"
                }
                else if (previewSlides.value!!.size != currentSlide.value){
                    addCutPosition()
                    cutAdapter.cut(previewViewModel.currentSlide.value!! - 1)
                    binding.textCut.text = "분할해제"
                }
            }
        }


        return binding.root
    }

    private fun onCutItemClick(previewSlide: PreviewSlide, position: Int) {
        binding.recyclerView.scrollToPosition(position)
        scroller.targetPosition = position
        binding.recyclerViewCut.layoutManager?.startSmoothScroll(scroller)
    }

    // 외부저장소에 다운받을 때 폴더 이름으로 사용할거임
    private fun getTimeStamp(): String {
        val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        val date = Calendar.getInstance().time
        return format.format(date)
    }

    // 어떤 형식으로 다운 받을 것인지 선택하는 다이얼로그
    private fun showDialog() {
        showSelectFormatDialog { format ->

            binding.layoutLoading.root.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.Main) {
                when(format) {
                    // 이미지로 저장
                    "png", "jpg" -> {
                        saveImageWithCutting(format)
                    }
                    // pdf로 저장
                    "pdf" -> {
                        savePdfWithCutting()
                    }
                    // 인스타그램으로 전달
                    "instagram" -> {
                        // 일단 저장 후
                        saveImageWithCutting("jpg")
                        // 인스타그램에 전달
                        openInstagram()

                        previewViewModel.savingSlides.value?.clear()
                        findNavController().navigate(R.id.action_previewFragment_to_homeFragment)
                    }
                    else -> throw IllegalStateException("존재하지 않는 선택지 입니다")
                }


                // 홈 화면으로 돌아가기
                showAlertDialog("저장에 성공했습니다.",
                    onDismiss = {
                        previewViewModel.savingSlides.value?.clear()
                        findNavController().navigate(R.id.action_previewFragment_to_homeFragment)
                    }
                )
            }
        }
    }

    // 퍼미션 런처
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 승인 됐으면 다운 시작
        if (isGranted) {
            showDialog()
        }
        // 아니면 빠꾸
        else {
            showMessageDialog("권한요청", "다운로드하기 위해서는\n사진 및 미디어 액세스 권한이 필요합니다.")
        }
    }

    private fun openInstagram() {
        // 인스타그램 앱 열기
        try {
            val uri = Uri.parse("instagram://share")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.instagram.android")
            startActivity(intent)
        }
        // 마켓 인스타그램 페이지로 전송
        catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = Uri.parse("market://details?id="+"com.instagram.android")
            }
            startActivity(intent)
        }
    }

    // 슬라이드를 프리뷰 슬라이드로 변환
    private fun processSlidesIntoPreviewSlides() {
        binding.textIndicator.visibility = View.GONE
        binding.layoutLoading.root.visibility = View.VISIBLE

        // 이미 해놓은거 있으면 패스
        if (slideAdapter.itemCount > 0) {
            binding.recyclerViewCut.post {
                cutWidth = binding.recyclerViewCut.width
                val itemWidth = cutAdapter.getItemWidth()
                val padding = (cutWidth - itemWidth) / 2

                binding.recyclerViewCut.setPadding(padding, 0, padding, 0)
                scroller.targetPosition = 0
                binding.recyclerViewCut.layoutManager?.startSmoothScroll(scroller)
            }
            binding.layoutLoading.root.visibility = View.GONE

            // 현재 페이지 표시
            previewViewModel.slidesSize.value = previewViewModel.previewSlides.value?.size
            binding.textIndicator.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val previewSlideList = arrayListOf<PreviewSlide>()

            slideViewModel.slides.value?.forEach {
                var previewSlide: PreviewSlide? = null

                // 바인딩 되어있는 놈
                if (it in slideViewModel.bindingFlattenSlides.value!!) {
                    for ((first, second) in slideViewModel.bindingPairs.value!!) {
                        if (it.bitmap == first.bitmap) {
                            // 첫번째 놈
                            // 인스타 사이즈냐 아니냐에 따라서도 달라짐
                            previewSlide = if (slideViewModel.isInstarSize.value == true) {
                                PreviewSlide(first.bitmap, second.bitmap, viewType=INSTAR_SIZE_BINDING)
                            } else {
                                // 오리지널 바인딩은
                                // 프리뷰에 보여지는 예시와 실제 결과가 같게 하기 위해
                                // 그냥 미리 합쳐버림
                                PreviewSlide(bindSlide(first.bitmap, second.bitmap), viewType=ORIGINAL_BINDING)
                            }
                            break
                        }
                        // 두번째 놈이면, 얘는 이 전에 이미 등록 했으니 패스
                    }
                }
                // 바인딩 안 되어 있는 놈
                else {
                    previewSlide = if (slideViewModel.isInstarSize.value == true) {
                        PreviewSlide(it.bitmap, viewType=INSTAR_SIZE)
                    } else {
                        PreviewSlide(it.bitmap, viewType=ORIGINAL)
                    }
                }

                // 프리뷰 슬라이드 목록에 추가
                if (previewSlide != null) {
                    previewSlideList.add(previewSlide)
                }
            }

            withContext(Dispatchers.Main) {
                // 리사이클러 뷰에 반영
                slideAdapter.replaceItems(previewSlideList)
                cutAdapter.replaceItems(previewSlideList)

                binding.recyclerViewCut.post {
                    cutWidth = binding.recyclerViewCut.width
                    val itemWidth = cutAdapter.getItemWidth()
                    val padding = (cutWidth - itemWidth) / 2

                    binding.recyclerViewCut.setPadding(padding, 0, padding, 0)
                    scroller.targetPosition = 0
                    binding.recyclerViewCut.layoutManager?.startSmoothScroll(scroller)
                    binding.recyclerView.layoutManager?.scrollToPosition(0)
                    previewViewModel.currentSlide.value = 1
                }

                // 로딩 끄기
                binding.layoutLoading.root.visibility = View.GONE

                // 현재 페이지 표시
                previewViewModel.slidesSize.value = previewViewModel.previewSlides.value?.size
                binding.textIndicator.visibility = View.VISIBLE
            }
        }
    }

    // 이미지를 분할해서 포맷에 맞게 저장
    private suspend fun saveImageWithCutting(format: String) {
        // 범위로 나눠서 분할
        previewViewModel.apply {

            val time = getTimeStamp()

            // 분할 안 했을 경우
            if (cutPositions.value.isNullOrEmpty()) {
                val title = "$time 0 ~ ${previewSlides.value!!.size - 1}"
                saveSlidesAsImage(format, title, previewSlides.value!!, slideViewModel.slides.value!!)
                return@apply
            }

            val bindingIndices = slideViewModel.bindingFlattenSlides.value!!.map {
                slideViewModel.slides.value!!.indexOf(it)
            }

            // 얘 꼭 필요함
            cutPositions.value?.add(previewSlides.value!!.size - 1)

            cutPositions.value?.forEachIndexed { index, position ->
                val prevRangedPreviewSlides: List<PreviewSlide>
                val rangedPreviewSlides: List<PreviewSlide>
                val rangedOriginalSlides: List<Slide>
                var title: String

                rangedPreviewSlides = when (index) {
                    0 -> {
                        title = "$time 0 ~ ${position}"
                        previewSlides.value!!.subList(0, position + 1)
                    }
                    else -> {
                        title = "$time ${cutPositions.value!![index - 1] + 1} ~ ${position}"
                        previewSlides.value!!.subList(cutPositions.value!![index - 1] + 1, position + 1)
                    }
                }

                if (rangedPreviewSlides.size == 1) {
                    title = "$time $position"
                }

                prevRangedPreviewSlides = if (index >= 1) {
                    previewSlides.value!!.subList(0, cutPositions.value!![index - 1] + 1)
                } else {
                    listOf()
                }


                // 오리지널 슬라이드 뽑아내기 (바인딩 안 했을 경우 오프셋은 0)
                var prevBindingOffset = 0
                var currentBindingOffset = 0

                // 바인딩 했을 경우
                if (bindingIndices.isNotEmpty()) {
                    prevBindingOffset = prevRangedPreviewSlides.count { (it.viewType == ORIGINAL_BINDING) || (it.viewType == INSTAR_SIZE_BINDING) }
                    currentBindingOffset = rangedPreviewSlides.count { (it.viewType == ORIGINAL_BINDING) || (it.viewType == INSTAR_SIZE_BINDING) }
                }


                rangedOriginalSlides = when (index) {
                    0 -> {
                        slideViewModel.slides.value!!.subList(0, position + currentBindingOffset + 1)
                    }
                    else -> {
                        slideViewModel.slides.value!!.subList(cutPositions.value!![index - 1] + prevBindingOffset + 1, position + prevBindingOffset + currentBindingOffset + 1)
                    }
                }

                saveSlidesAsImage(format, title, rangedPreviewSlides, rangedOriginalSlides)
            }
        }
    }
    
    // 이미지를 알맞은 형태로 저장
    private suspend fun saveSlidesAsImage(format: String, title: String, previews: List<PreviewSlide>, originals: List<Slide>) {
        coroutineScope {
            launch(Dispatchers.IO) {
                val bitmaps = arrayListOf<Bitmap>()

                previews.forEach {
                    when (it.viewType) {
                        ORIGINAL ->
                            bitmaps.add(it.getAsOriginal())
                        INSTAR_SIZE ->
                            bitmaps.add(it.getAsInstarSize())
                        ORIGINAL_BINDING ->
                            bitmaps.add(it.getAsOriginalBinding())
                        INSTAR_SIZE_BINDING ->
                            bitmaps.add(it.getAsInstarSizeBinding())
                        else -> throw IllegalStateException("있을 수 없는 뷰 타입")
                    }
                }


                // result slide를 뷰 모델에 추가시켜 저장
                val thumbnail = getResized(bitmaps.first(), 200, 200)
                val resultSlide = ResultSlide(0, format, originals.size, thumbnail)
                val id = homeViewModel.addResultSlide(resultSlide)
                resultSlide.id = id

                withContext(Dispatchers.Main) {
                    previewViewModel.savingSlides.value?.add(resultSlide)
                }

                // 내부저장소에 원본과 상태 저장 (무조건 png)
                val inInnerStorage = async {
                    slideViewModel.run {
                        val originalBitmaps = originals.map { it.bitmap }
                        val isInstarSize = isInstarSize.value!!
                        val bindingIndices = bindingPairs.value!!
                            .filter { it.first in originals && it.second in originals }
                            .map { originals.indexOf(it.first) to originals.indexOf(it.second) }

                        saveOriginalSlidesWithState(requireContext(), originalBitmaps, isInstarSize, bindingIndices,
                            "slides", "id_${id}", "png", previewViewModel.savingSlides)
                    }
                }
                // 슬라이드를 외부저장소에 이미지로 저장
                val inExternalStorage = async {
                    val externalFile = saveBitmapsAsImageInExternalStorage(
                        bitmaps,
                        "포트폴리오 $title",
                        format,
                        previewViewModel.savingSlides
                    )

                    refreshFile(externalFile)
                }

                // 저장 기다리기
                inInnerStorage.await()
                inExternalStorage.await()
            }
        }
    }

    // 이미지를 분할해서 pdf로 저장
    private suspend fun savePdfWithCutting() {
        // 범위로 나눠서 분할
        previewViewModel.apply {

            val time = getTimeStamp()

            // 분할 안 했을 경우
            if (cutPositions.value.isNullOrEmpty()) {
                val title = "$time 0 ~ ${previewSlides.value!!.size - 1}"
                saveSlidesAsPdf(title, previewSlides.value!!, slideViewModel.slides.value!!)
                return@apply
            }

            val bindingIndices = slideViewModel.bindingFlattenSlides.value!!.map {
                slideViewModel.slides.value!!.indexOf(it)
            }

            // 얘 꼭 필요함
            cutPositions.value?.add(previewSlides.value!!.size - 1)

            cutPositions.value?.forEachIndexed { index, position ->
                val prevRangedPreviewSlides: List<PreviewSlide>
                val rangedPreviewSlides: List<PreviewSlide>
                val rangedOriginalSlides: List<Slide>
                var title: String

                rangedPreviewSlides = when (index) {
                    0 -> {
                        title = "$time 0 ~ ${position}"
                        previewSlides.value!!.subList(0, position + 1)
                    }
                    else -> {
                        title = "$time ${cutPositions.value!![index - 1] + 1} ~ ${position}"
                        previewSlides.value!!.subList(cutPositions.value!![index - 1] + 1, position + 1)
                    }
                }


                if (rangedPreviewSlides.size == 1) {
                    title = "$time $position"
                }

                prevRangedPreviewSlides = if (index >= 1) {
                    previewSlides.value!!.subList(0, cutPositions.value!![index - 1] + 1)
                } else {
                    listOf()
                }


                // 오리지널 슬라이드 뽑아내기 (바인딩 안 했을 경우 오프셋은 0)
                var prevBindingOffset = 0
                var currentBindingOffset = 0

                // 바인딩 했을 경우
                if (bindingIndices.isNotEmpty()) {
                    prevBindingOffset = prevRangedPreviewSlides.count { (it.viewType == ORIGINAL_BINDING) || (it.viewType == INSTAR_SIZE_BINDING) }
                    currentBindingOffset = rangedPreviewSlides.count { (it.viewType == ORIGINAL_BINDING) || (it.viewType == INSTAR_SIZE_BINDING) }
                }


                rangedOriginalSlides = when (index) {
                    0 -> {
                        slideViewModel.slides.value!!.subList(0, position + currentBindingOffset + 1)
                    }
                    else -> {
                        slideViewModel.slides.value!!.subList(cutPositions.value!![index - 1] + prevBindingOffset + 1, position + prevBindingOffset + currentBindingOffset + 1)
                    }
                }

                saveSlidesAsPdf(title, rangedPreviewSlides, rangedOriginalSlides)
            }
        }
    }

    // 이미지들을 pdf 형태로 저장
    private suspend fun saveSlidesAsPdf(title: String, previews: List<PreviewSlide>, originals: List<Slide>) {
        coroutineScope {
            launch(Dispatchers.IO) {
                val bitmaps = arrayListOf<Bitmap>()

                previews.forEach {
                    when (it.viewType) {
                        ORIGINAL ->
                            bitmaps.add(it.getAsOriginal())
                        INSTAR_SIZE ->
                            bitmaps.add(it.getAsInstarSize())
                        ORIGINAL_BINDING ->
                            bitmaps.add(it.getAsOriginalBinding())
                        INSTAR_SIZE_BINDING ->
                            bitmaps.add(it.getAsInstarSizeBinding())
                        else -> throw IllegalStateException("있을 수 없는 뷰 타입")
                    }
                }

                // result slide를 뷰 모델에 추가시켜 저장
                val thumbnail = getResized(bitmaps.first(), 200, 200)
                val resultSlide = ResultSlide(0, "pdf", originals.size, thumbnail)
                val id = homeViewModel.addResultSlide(resultSlide)
                resultSlide.id = id

                withContext(Dispatchers.Main) {
                    previewViewModel.savingSlides.value?.add(resultSlide)
                }

                // 내부저장소에 원본과 상태 저장 (무조건 png)
                val inInnerStorage = async {
                    slideViewModel.run {
                        val originalBitmaps = originals.map { it.bitmap }
                        val isInstarSize = isInstarSize.value!!
                        val bindingIndices = bindingPairs.value!!
                            .filter { it.first in originals && it.second in originals }
                            .map { originals.indexOf(it.first) to originals.indexOf(it.second) }

                        saveOriginalSlidesWithState(
                            requireContext(), originalBitmaps, isInstarSize, bindingIndices,
                            "slides", "id_${id}", "png", previewViewModel.savingSlides
                        )
                    }
                }
                // 슬라이드를 외부저장소에 pdf로 저장(포맷은 jpg)
                val inExternalStorage = async {
                    val externalFile = saveBitmapsAsPdfInExternalStorage(
                        bitmaps,
                        name = "포트폴리오 $title",
                        isSavingSlide = previewViewModel.savingSlides
                    )

                    refreshFile(externalFile)
                }

                // 저장 기다리기
                inInnerStorage.await()
                inExternalStorage.await()
            }
        }
    }

    // 파일이 다운되도 안드로이드 시스템에서 인지 못하는 에러 수정
    private fun refreshFile(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(file)
            scanIntent.data = contentUri
            requireActivity(). sendBroadcast(scanIntent)
        } else {
            val intent = Intent(
                Intent.ACTION_MEDIA_MOUNTED,
                Uri.fromFile(file)
            )
            requireActivity().sendBroadcast(intent)
        }
    }

    // 뒤로가기
    override fun onBackPressed() {
        // 저장중
        if (!previewViewModel.savingSlides.value.isNullOrEmpty()) {
            showConfirmDialog(
                "아직 저장중입니다.",
                "저장을 중지하시겠습니까?",
                onOk = {
                    findNavController().popBackStack()
                }
            )
        }
        else if (!previewViewModel.cutPositions.value.isNullOrEmpty()) {
            showConfirmDialog(
                "저장되지 않았습니다.",
                "정말 뒤로가시겠습니까?",
                onOk = {
                    findNavController().popBackStack()
                }
            )
        }
        // 저장중이 아님
        else {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // 저장중에 나가면
        // 기록 삭제시킴
        if (!previewViewModel.savingSlides.value.isNullOrEmpty()) {
            previewViewModel.savingSlides.value!!.forEach {
                homeViewModel.deleteResultSlide(it)
                it.deleteCache(requireContext())
            }
        }
        previewViewModel.savingSlides.value?.clear()
    }

}