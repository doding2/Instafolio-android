package com.instafolioo.instagramportfolio.view.preview

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.instagram4j.instagram4j.IGClient
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.databinding.FragmentPreviewBinding
import com.instafolioo.instagramportfolio.extension.*
import com.instafolioo.instagramportfolio.model.PreviewSlide
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.INSTAR_SIZE
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.INSTAR_SIZE_BINDING
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.ORIGINAL
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.ORIGINAL_BINDING
import com.instafolioo.instagramportfolio.model.ResultSlide
import com.instafolioo.instagramportfolio.view.common.MainActivity
import com.instafolioo.instagramportfolio.view.home.HomeViewModel
import com.instafolioo.instagramportfolio.view.home.HomeViewModelFactory
import com.instafolioo.instagramportfolio.view.slide.SlideViewModel
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

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


    // 분할 디바이더 움직이는 위치
    private var cutStartX: Float = 0f
    private var cutEndX: Float = 0f
    private var dividerWidth: Float = 0f
    private var widthOfCutItem = 0f

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

        // 프리뷰 초기화
        previewViewModel.clear()
        
        // 뷰를 status bar와 navigation bar의 위치에서 떨어진 원래 위치로 복구(회전 방향에 따라 달라짐)
        when (requireActivity().display?.rotation) {
            // 폰이 왼쪽으로 누움
            Surface.ROTATION_90 -> {
                binding.layoutRoot.setPadding(0, getStatusBarHeight(), getNaviBarHeight(), 0)
            }
            // 폰이 오른쪽으로 누움
            Surface.ROTATION_270 -> {
                binding.layoutRoot.setPadding(getNaviBarHeight(), getStatusBarHeight(), 0, 0)
            }
            // 그 외는 그냥 정방향으으로 처리함
            else -> {
                binding.layoutRoot.setPadding(0, getStatusBarHeight(), 0, getNaviBarHeight())
            }
        }

        // status bar, navigation bar가 밝은 색이 아니라는 것을 알림
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightNavigationBars = false

        // 리사이클러뷰에 어답터 추가
        slideAdapter = PreviewSlideAdapter(previewViewModel.previewSlides.value!!)
        binding.recyclerView.adapter = slideAdapter

        // Slide들을 PreviewSlide로 변환
        processSlidesIntoPreviewSlides()

        // Recycler view를 Pager로 설정
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerView)

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

        // 현재 보여지고 있는 아이템 포지션 화면에 띄우기
        binding.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val position = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    previewViewModel.currentSlide.value = position + 1

                    val rawX = (position + 1) * widthOfCutItem + cutStartX
                    moveDivider(rawX, true)
                }
            }
        })


        // 분할 화면 설정
        cutAdapter = PreviewSlideCutAdapter(previewViewModel.previewSlides.value!!, previewViewModel.cutPositions)
        binding.recyclerViewCut.adapter = cutAdapter
        val layoutManager = FlexboxLayoutManager(requireContext()).apply {
            flexWrap = FlexWrap.NOWRAP
            justifyContent = JustifyContent.CENTER
        }
        binding.recyclerViewCut.layoutManager = layoutManager
        binding.recyclerViewCut.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> moveDivider(event.rawX)
            }

            return@setOnTouchListener false
        }

        // 디바이더 너비 알아내기
        binding.divider.post {
            dividerWidth = binding.divider.width.toFloat()
        }

        // 분할 버튼 클릭
        binding.buttonCut.setOnClickListener {
            previewViewModel.apply {
                if (isAlreadyCut) {
                    removeCutPosition()
                    cutAdapter.connectSlide(currentSlide.value!! - 1)
                }
                else {
                    addCutPosition()
                    cutAdapter.cutSlide(currentSlide.value!! - 1)
                }
            }
        }


        return binding.root
    }

    // 분할 디바이더 조작
    private fun moveDivider(rawX: Float, isFromSlide: Boolean = false) {
        val x =
            if (rawX < cutStartX) cutStartX
            else if (rawX > cutEndX) cutEndX
            else rawX

        binding.divider.animate()
            .x(x - dividerWidth / 2)
            .setDuration(0)
            .start()

        // 슬라이드로부터 왔으면 여기까지
        if (isFromSlide) return

        // 선택한 지점으로 이동하기
        var position: Int = floor((x - cutStartX) / widthOfCutItem).toInt()
        if (position >= 0 && position < previewViewModel.slidesSize.value!!) {
            binding.recyclerView.scrollToPosition(position)
            previewViewModel.currentSlide.value = position + 1
        }

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
            when(format) {
                // 이미지로 저장
                "png", "jpg" -> {
                    saveSlidesAsImage(format)
                }
                // pdf로 저장
                "pdf" -> {
                    saveSlidesAsPdf()
                }
                // 인스타그램에 업로드
                "instagram" -> {
                    sendToInstagram()
                }
                else -> throw IllegalStateException("존재하지 않는 선택지 입니다")
            }
            binding.layoutLoading.root.visibility = View.VISIBLE
        }
    }
    
    // 인스타그램으로 사진 전달
    private fun navigateToInstagram() {
        try {
            val cw = ContextWrapper(context)
            var directory = cw.getDir("instagram", Context.MODE_PRIVATE)
            directory = File(directory, "log_in")

            val clientFile = File(directory, "client.ser")
            val cookieFile = File(directory, "cookie.ser")

            val client = IGClient.deserialize(clientFile, cookieFile)

            if (client.isLoggedIn)
                findNavController().navigate(R.id.action_previewFragment_to_instagramUploadFragment)
            else
                throw Exception("Not Logged In")
        } catch (e: Exception) {
            findNavController().navigate(R.id.action_previewFragment_to_instagramLogInFragment)
        }
    }


    // 업로드 하기
    private fun sendToInstagram() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.layoutLoading.root.visibility = View.VISIBLE

            // 이미지 저장
            val files = savePreviewImages()

            val result = runCatching {
                sendIntent(files)
            }

            // 업로드 실패
            result.onFailure {
                // 기록 삭제
                previewViewModel.savingSlide.value?.let {
                    homeViewModel.deleteResultSlide(it)
                    it.deleteCache(requireContext())
                }
                previewViewModel.savingSlide.value = null
                deleteInstagramImageCache()

                binding.layoutLoading.root.visibility = View.GONE
                showMessageDialog("업로드에 실패했습니다.", "${it.message}")
            }

            // 업로드 성공
            result.onSuccess {
                binding.layoutLoading.root.visibility = View.GONE
                showAlertDialog("인스타그램에 전달했습니다.",
                    onDismiss = {
                        deleteInstagramImageCache()
                        previewViewModel.savingSlide.value = null
                        findNavController().navigate(R.id.action_previewFragment_to_homeFragment)
                    }
                )
            }

        }
    }

    private suspend fun sendIntent(files: List<File>) {
        coroutineScope {
            launch(Dispatchers.IO) {

                val result = runCatching {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        `package` = "com.instagram.android"
                        type = "image/jpg"

                        val uriList = arrayListOf<Uri>()
                        files.forEach {
                            val uri = FileProvider.getUriForFile(
                                requireContext(),
                                requireContext().applicationContext.packageName + ".provider",
                                it
                            )
                            uriList.add(uri)
                        }

                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                    }
                    startActivity(shareIntent)
                }

                result.onFailure {
                    var intent = requireActivity().packageManager.getLaunchIntentForPackage("com.instagram.android")

                    if (intent == null) {
                        intent = Intent(Intent.ACTION_VIEW).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            data = Uri.parse("market://details?id="+"com.instagram.android")
                        }
                        startActivity(intent)
                    }
                    else {
                        throw Exception("인스타그램 앱을 찾을 수 없습니다.")
                    }
                }
            }
        }
    }

    private suspend fun savePreviewImages(): List<File> {
        var files: List<File>? = null

        coroutineScope {
            launch(Dispatchers.IO) {
                val bitmaps = arrayListOf<Bitmap>()

                previewViewModel.previewSlides.value?.forEach {
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
                val resultSlide = ResultSlide(0, "png", slideViewModel.slides.value!!.size, thumbnail)
                val id = homeViewModel.addResultSlide(resultSlide)
                resultSlide.id = id

                // 저장중
                withContext(Dispatchers.Main) {
                    previewViewModel.savingSlide.value = resultSlide
                }

                // 내부저장소에 원본과 상태 저장 (무조건 png)
                val savingOriginalImages = async {
                    slideViewModel.run {
                        val originalBitmaps = slides.value!!.map { it.bitmap }
                        val isInstarSize = isInstarSize.value!!
                        val bindingIndices = bindingPairs.value!!.map { slides.value!!.indexOf(it.first) to slides.value!!.indexOf(it.second) }

                        saveOriginalSlidesWithState(requireContext(), originalBitmaps, isInstarSize, bindingIndices,
                            "slides", "id_${id}", "png", previewViewModel.savingSlide)
                    }
                }

                // 슬라이드를 내부저장소에 프리뷰 그대로 저장
                val savingPreviewImages = async {
                    savePreviewSlides(requireContext(), bitmaps,
                        "instagram", "instagram_temp", "jpg",
                        previewViewModel.savingSlide
                    )
                }

                // 저장 기다리기
                savingOriginalImages.await()
                files = savingPreviewImages.await()
            }
        }

        return files
            ?: throw Exception("Saving images failed.")
    }

    private fun deleteInstagramImageCache() {
        val cw = ContextWrapper(context)
        var cacheDir = File(cw.cacheDir, "instagram")
        cacheDir = File(cacheDir, "instagram_temp")
        cacheDir.deleteRecursively()
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


    // 슬라이드를 프리뷰 슬라이드로 변환
    private fun processSlidesIntoPreviewSlides() {
        binding.textIndicator.visibility = View.GONE
        binding.layoutLoading.root.visibility = View.VISIBLE

        // 이미 해놓은거 있으면 패스
        if (slideAdapter.itemCount > 0) {
            binding.layoutLoading.root.visibility = View.GONE
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

                // 컷 에리어 위치 알아내기
                binding.recyclerViewCut.post {
                    cutStartX = binding.recyclerViewCut.x
                    cutEndX = cutStartX + binding.recyclerViewCut.width
                    widthOfCutItem = ((cutEndX - cutStartX) / previewViewModel.previewSlides.value!!.size)
                    cutAdapter.itemWidth = widthOfCutItem
                    val rawX =  widthOfCutItem + cutStartX - 0.1f
                    moveDivider(rawX)
                }
                
                // 로딩 끄기
                binding.layoutLoading.root.visibility = View.GONE

                // 현재 페이지 표시
                previewViewModel.slidesSize.value = previewViewModel.previewSlides.value?.size
                binding.textIndicator.visibility = View.VISIBLE
            }
        }
    }

    // 이미지를 알맞은 형태로 저장
    private fun saveSlidesAsImage(format: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmaps = arrayListOf<Bitmap>()

            previewViewModel.previewSlides.value?.forEach {
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
            val resultSlide = ResultSlide(0, format, slideViewModel.slides.value!!.size, thumbnail)
            val id = homeViewModel.addResultSlide(resultSlide)
            resultSlide.id = id

            withContext(Dispatchers.Main) {
                previewViewModel.savingSlide.value = resultSlide
            }

            // 내부저장소에 원본과 상태 저장 (무조건 png)
            val inInnerStorage = async {
                slideViewModel.run {
                    val originalBitmaps = slides.value!!.map { it.bitmap }
                    val isInstarSize = isInstarSize.value!!
                    val bindingIndices = bindingPairs.value!!.map { slides.value!!.indexOf(it.first) to slides.value!!.indexOf(it.second) }

                    saveOriginalSlidesWithState(requireContext(), originalBitmaps, isInstarSize, bindingIndices,
                        "slides", "id_${id}", "png", previewViewModel.savingSlide)
                }
            }
            // 슬라이드를 외부저장소에 이미지로 저장
            val inExternalStorage = async {
                saveBitmapsAsImageInExternalStorage(
                    bitmaps,
                    "포트폴리오 ${getTimeStamp()}",
                    format,
                    previewViewModel.savingSlide
                )
            }

            // 저장 기다리기
            inInnerStorage.await()
            inExternalStorage.await()

            // 홈 화면으로 돌아가기
            withContext(Dispatchers.Main) {
                showAlertDialog("저장에 성공했습니다.",
                    onDismiss = {
                        previewViewModel.savingSlide.value = null
                        findNavController().navigate(R.id.action_previewFragment_to_homeFragment)
                    }
                )
            }
        }
    }

    // 이미지들을 pdf 형태로 저장
    private fun saveSlidesAsPdf() {
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmaps = arrayListOf<Bitmap>()

            previewViewModel.previewSlides.value?.forEach {
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
            val resultSlide = ResultSlide(0, "pdf", slideViewModel.slides.value!!.size, thumbnail)
            val id = homeViewModel.addResultSlide(resultSlide)
            resultSlide.id = id

            withContext(Dispatchers.Main) {
                previewViewModel.savingSlide.value = resultSlide
            }

            // 내부저장소에 원본과 상태 저장 (무조건 png)
            val inInnerStorage = async {
                slideViewModel.run {
                    val originalBitmaps = slides.value!!.map { it.bitmap }
                    val isInstarSize = isInstarSize.value!!
                    val bindingIndices = bindingPairs.value!!.map { slides.value!!.indexOf(it.first) to slides.value!!.indexOf(it.second) }

                    saveOriginalSlidesWithState(requireContext(), originalBitmaps, isInstarSize, bindingIndices,
                        "slides", "id_${id}", "png", previewViewModel.savingSlide)
                }
            }
            // 슬라이드를 외부저장소에 pdf로 저장(포맷은 jpg)
            val inExternalStorage = async {
                saveBitmapsAsPdfInExternalStorage(
                    bitmaps,
                    name="포트폴리오 ${getTimeStamp()}",
                    isSavingSlide=previewViewModel.savingSlide
                )
            }

            // 저장 기다리기
            inInnerStorage.await()
            inExternalStorage.await()

            // 홈 화면으로 돌아가기
            withContext(Dispatchers.Main) {
                showAlertDialog("저장에 성공했습니다.",
                    onDismiss = {
                        previewViewModel.savingSlide.value = null
                        findNavController().navigate(R.id.action_previewFragment_to_homeFragment)
                    }
                )
            }
        }


    }

    // 뒤로가기
    override fun onBackPressed() {
        // 저장중
        if (previewViewModel.savingSlide.value != null) {
            showConfirmDialog(
                "아직 저장중입니다.",
                "저장을 중지하시겠습니까?",
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
        previewViewModel.savingSlide.value?.let {
            homeViewModel.deleteResultSlide(it)
            it.deleteCache(requireContext())
        }
        previewViewModel.savingSlide.value = null
    }

}