package com.instafolioo.instagramportfolio.view.slide

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.extension.*
import com.instafolioo.instagramportfolio.model.Slide
import com.instafolioo.instagramportfolio.view.common.MainActivity
import com.instafolioo.instagramportfolio.view.home.HomeViewModel
import com.instafolioo.instagramportfolio.view.home.HomeViewModelFactory
import com.instafolioo.instagramportfolio.view.preview.PreviewViewModel
import kotlinx.coroutines.*
import java.io.File

class SlideFragment : Fragment(), MainActivity.OnBackPressedListener {

    companion object {
        const val TAG = "SlideFragment"
    }

    private var _binding: com.instafolioo.instagramportfolio.databinding.FragmentSlideBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SlideViewModel
    private lateinit var adapter: SlideAdapter

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var previewViewModel: PreviewViewModel

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        when (throwable) {
            // image exception
            is NoReadStoragePermissionException -> {
                // read storage 퍼미션 요청
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            // pdf exception
            is NoManageStoragePermissionException -> {
                showAlertDialog("지원하지 않는 경로로부터의 파일입니다",
                    onDismiss = {
                        findNavController().popBackStack()
                })
            }
            else -> {
                showMessageDialog(
                    "에러 발생",
                    "${throwable.message}",
                    onDismiss = {
                        findNavController().popBackStack()
                    }
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_slide, container, false)
        viewModel= ViewModelProvider(requireActivity())[SlideViewModel::class.java]
        val factory = HomeViewModelFactory(requireActivity())
        homeViewModel = ViewModelProvider(requireActivity(), factory)[HomeViewModel::class.java]
        previewViewModel = ViewModelProvider(requireActivity())[PreviewViewModel::class.java]

        // 뷰를 status bar와 navigation bar의 위치에서 떨어진 원래 위치로 복구(회전 방향에 따라 달라짐)
        when (requireActivity().display?.rotation) {
            // 폰이 왼쪽으로 누움
            Surface.ROTATION_90 -> {
                // 뷰가 화면에 너무 크게 차지하지 않게 조절
                binding.layoutRoot.post {
                    val params = binding.layoutPreviewBackground.layoutParams
                    params.width = (binding.layoutRoot.height / 3.0).toInt()
                    binding.layoutPreviewBackground.layoutParams = params
                }
            }
            // 폰이 오른쪽으로 누움
            Surface.ROTATION_270 -> {
                // 뷰가 화면에 너무 크게 차지하지 않게 조절
                binding.layoutRoot.post {
                    val params = binding.layoutPreviewBackground.layoutParams
                    params.width = (binding.layoutRoot.height / 3.0).toInt()
                    binding.layoutPreviewBackground.layoutParams = params
                }
            }
            // 그 외는 그냥 정방향으으로 처리함
            else -> {
                // 정상 상태
                val params = binding.layoutPreviewBackground.layoutParams
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                binding.layoutPreviewBackground.layoutParams = params
            }
        }

        requireActivity().window.apply {
            statusBarColor = Color.BLACK
            navigationBarColor = Color.BLACK

            WindowInsetsControllerCompat(this, binding.root).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }


        // 로딩 화면 표시
        enableLoading(true)

        // 리사이클러 뷰 설정
        adapter = SlideAdapter(requireContext(), arrayListOf(), binding.recyclerViewSlide, ::onItemClick, viewModel)
        adjustSlideSize()

        binding.recyclerViewSlide.adapter = adapter
        val layoutManager = FlexboxLayoutManager(requireContext()).apply {
            flexWrap = FlexWrap.WRAP
        }
        binding.recyclerViewSlide.layoutManager = layoutManager

        // Drag and Drop 구현
        val callback = ItemTouchHelperCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerViewSlide)
        adapter.startDrag(object: SlideAdapter.OnStartDragListener {
            override fun onStartDrag(viewHolder: SlideAdapter.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }
        })

        // 기존의 파일을 편집할 경우
        if (!viewModel.resultSlideWithExtension.value.isNullOrEmpty()) {
            handleResultSlide()
        }
        // 새로운 파일을 열었을 경우
        else if (!viewModel.uriWithExtension.value.isNullOrEmpty()) {
            handleUri(viewModel.uriWithExtension.value!!)
        }
        // 상정하지 않은 상황, 에러
        else {
            findNavController().popBackStack()
        }

        // 이미지 사이즈 조절
        initImagesScale()
        binding.buttonInstaSize.setOnClickListener {
            viewModel.isSlideChanged.value = true
            scaleImages()
        }

        // 이미지 두 개를 하나로 묶기 활성화 버튼
        initBinding()
        binding.buttonBinding.setOnClickListener {
            if (adapter.itemCount > 1) {
                initBinding()
                viewModel.enableBinding.value = viewModel.enableBinding.value != true

                // 바인딩 상태가 풀렸을 때, 바인딩된 슬라이드가 존재하지 않는다면
                // 그냥 싱글 프리뷰 화면 보이기
                if (viewModel.enableBinding.value == false && adapter.bindingFlattenSlides.isEmpty()) {
                    binding.layoutBinding.visibility = View.GONE
                    binding.imagePreview.visibility = View.VISIBLE
                }
            }
            else {
                showAlertDialog("적어도 두 개의 이미지가 필요합니다")
            }
        }

        // 바인딩 되어있으면 버튼 배경이 생김
        viewModel.enableBinding.observe(viewLifecycleOwner) {
            if (it == true) {
                binding.buttonBinding.setEnabledColor(true)
                showFirstBindingToPreview()
            } else {
                binding.buttonBinding.setEnabledColor(false)
            }
        }
        // 확장 되어있으면 버튼 배경이 생김
        viewModel.isInstarSize.observe(viewLifecycleOwner) {
            binding.apply {
                imageInstaSizeUpper.setEnabledColor(it)
                imageInstaSize11.setEnabledColor(it)
                imageInstaSizeLower.setEnabledColor(it)
            }
        }

        // 프리뷰 화면으로 이동 버튼
        binding.buttonNavigatePreview.setOnClickListener {
            // 슬라이드 로딩이 끝났을 때만 이동가능
            if (viewModel.slides.value != null && viewModel.slides.value!!.size > 0) {
                // Preview Fragment에 정보 전달 하기 위해 백업
                viewModel.slides.value = adapter.items
                viewModel.bindingPairs.value = adapter.bindingPairs
                viewModel.bindingFlattenSlides.value = adapter.bindingFlattenSlides
                previewViewModel.clear()
                findNavController().navigate(R.id.action_slideFragment_to_previewFragment)
            }
        }

        // 뒤로가기
        binding.buttonBack.setOnClickListener { onBackPressed() }

        return binding.root
    }

    // 기존 파일을 편집할 경우
    private fun handleResultSlide() {
        lifecycleScope.launch(Dispatchers.Main) {

            // 이미 로딩 끝났었으면 패스
            if (!viewModel.slides.value.isNullOrEmpty()) {
                binding.imagePreview.setImageBitmap(viewModel.slides.value!![0].bitmap)
                adapter.replaceItems(viewModel.slides.value!!, viewModel.bindingPairs.value!!)
                adapter.bindingPairs = viewModel.bindingPairs.value!!

                Log.d(TAG, "${adapter.items.size}")

                if (viewModel.enableBinding.value == true) {
                    showFirstBindingToPreview()
                }

                // 로딩 화면 끄기
                enableLoading(false, 0L)
                return@launch
            }

            val pairList = viewModel.resultSlideWithExtension.value ?: return@launch
            val slides = mutableListOf<Slide>()

            for (pair in pairList) {

                val resultSlide = pair.first
                val format = pair.second

                // 백그라운드 스레드에서 처리
                val waitingSlides = async(Dispatchers.IO) {
                    try {
                        val imageBitmaps = resultSlide.getFileAsImages(requireContext())
                        val slides = imageBitmaps.map { Slide(it) }

                        // 인스타 사이즈 상태 불러오기
                        val isInstarSize = resultSlide.getInstarSizeState(requireContext())

                        // 바인딩 페어 정보 불러오기
                        val bindingIndices = resultSlide.getBindingIndicesState(requireContext())
                        val bindingPairs = bindingIndices.map { slides[it.first] to slides[it.second] }.toMutableList()

                        // 뷰 모델에 정보 저장
                        withContext(Dispatchers.Main) {
                            if (!isInstarSize) {
                                viewModel.isInstarSize.value = isInstarSize
                                initImagesScale()
                            }
                            viewModel.bindingPairs.value?.addAll(bindingPairs)
                        }

                        return@async slides
                    } catch (e: Exception) {
                        throw CancellationException()
                    }
                }

                // 이미지 불러오는걸 기다림
                val loadedSlide = waitingSlides.await().toMutableList()

                // 손상된 파일이라
                // 삭제시키기
                if (resultSlide.size != loadedSlide.size) {
                    showAlertDialog(
                        "파일을 열 수 없습니다",
                        onDismiss = {
                            homeViewModel.deleteResultSlide(resultSlide)
                            findNavController().popBackStack()
                        }
                    )
                }

                // 슬라이드 리스트에 변환 완료된 것들 추가
                slides.addAll(loadedSlide)
            }


            // 가공 완료된 slide들을 view model에 전달
            if (viewModel.slides.value == null) {
                viewModel.slides.value = slides
            } else {
                viewModel.slides.value!!.clear()
                viewModel.slides.value!!.addAll(slides)
            }

            // slide 리스트가 변할때마다 리사이클러뷰에 반영
            if (viewModel.slides.value!!.size > 0) {
                binding.imagePreview.setImageBitmap(viewModel.slides.value!![0].bitmap)
                adapter.replaceItems(viewModel.slides.value!!, viewModel.bindingPairs.value!!)
            } else {
                showAlertDialog(
                    "파일을 열 수 없습니다",
                    onDismiss =  {
                        findNavController().popBackStack()
                    }
                )
            }

            viewModel.bindingPairs.value = adapter.bindingPairs

            // 로딩 끄기
            enableLoading(false)
        }
    }

    // 로딩창 활성화/비활성화
    private fun enableLoading(enabled: Boolean, duration: Long = 300L) {
        binding.layoutLoading.root.apply {
            val alpha = if (enabled) 1f else 0f
            val duration1 = if (enabled) 0L else duration
            val visibility = if(enabled) View.VISIBLE else View.GONE

            animate()
                .alpha(alpha)
                .setDuration(duration1)
                .setListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        _binding?.layoutLoading?.root?.visibility = visibility
                    }
                })
        }
    }

    // 버튼 이미지 틴트 색 바꾸기
    private fun ImageView.setEnabledColor(enabled: Boolean) {
        val color = if (enabled) Color.WHITE
        else ContextCompat.getColor(requireContext(), R.color.button_disabled_slide_color)

        setColorFilter(color)
    }
    
    // 리사이클러 뷰의 column 개수가 4개 (또는 8개)가 되도록 아이템 슬라이드 조절
    private fun adjustSlideSize() {

        val isRotated = when (requireActivity().display?.rotation) {
            Surface.ROTATION_90, Surface.ROTATION_270 -> true
            else -> false
        }

        binding.recyclerViewSlide.run {
            post {
                val itemSize = if (isRotated) width / 8
                else width / 4

                this@SlideFragment.adapter.setSlidesSize(itemSize)
            }
        }

    }

    // dp to px
    private val Int.dp: Int
        get() {
            val scale = requireContext().resources.displayMetrics.density
            return (this * scale + 0.5f).toInt()
        }


    // 새로운 파일을 열었을 경우
    private fun handleUri(uris: MutableList<Pair<Uri, String>>) {
        if (uris.isNullOrEmpty()) return

        lifecycleScope.launch(exceptionHandler) {

            // 이미 로딩 끝났었으면 패스
            if (!viewModel.slides.value.isNullOrEmpty()) {
                binding.imagePreview.setImageBitmap(viewModel.slides.value!![0].bitmap)
                adapter.replaceItems(viewModel.slides.value!!, viewModel.bindingPairs.value!!)
                adapter.bindingPairs = viewModel.bindingPairs.value!!

                // 로딩 화면 끄기
                enableLoading(false, 0L)
                return@launch
            }

            val slides: MutableList<Slide> = processIntoSlides(uris)

            // 가공 완료된 slide들을 view model에 전달
            if (viewModel.slides.value == null) {
                viewModel.slides.value = slides
            } else {
                viewModel.slides.value!!.clear()
                viewModel.slides.value!!.addAll(slides)
            }

            // slide 리스트가 변할때마다 리사이클러뷰에 반영
            if (viewModel.slides.value!!.size > 0) {
                binding.imagePreview.setImageBitmap(viewModel.slides.value!![0].bitmap)
                adapter.replaceItems(viewModel.slides.value!!, viewModel.bindingPairs.value!!)
            } else {
                // 변환된 비트맵 이미지가 0개
                showAlertDialog(
                    "파일을 열 수 없습니다",
                    onDismiss =  {
                        findNavController().popBackStack()
                    }
                )
            }

            viewModel.bindingPairs.value = adapter.bindingPairs

            // 로딩 화면 끄기
            enableLoading(false)
        }
    }

    // 슬라이드 클릭하면 프리뷰에 띄워줌
    private fun onItemClick(slide: Slide) {
        if (viewModel.enableBinding.value == true) {
            if (viewModel.slides.value?.size!! <= 1) {
                return
            }
            viewModel.isSlideChanged.value = true

            var firstIdx = adapter.getIndexOf(slide)
            val secondIdx = firstIdx + 1
            
            // 클릭한 아이템이 마지막 아이템일 때
            if (firstIdx >= adapter.itemCount - 1) {
                val firstSlide = adapter.getSlideAt(firstIdx)
                
                // 바인딩 되어있는 놈이면 바인딩 취소
                if (adapter.isBindingContains(firstSlide)) {
                    adapter.cancelBinding(firstSlide)
                    showFirstBindingToPreview()
                }
                
                return
            }

            
            val firstSlide = adapter.getSlideAt(firstIdx)
            val secondSlide = adapter.getSlideAt(secondIdx)

            // 두 슬라이드가 바인딩 안 되어있다면, 등록
            if (!adapter.isBindingContains(firstSlide) && !adapter.isBindingContains(secondSlide)) {
                adapter.registerBinding(firstSlide, secondSlide)

                binding.imagePreview.visibility = View.GONE
                binding.layoutBinding.visibility = View.VISIBLE
                binding.imageBindingPreviewFirst.setImageBitmap(firstSlide.bitmap)
                binding.imageBindingPreviewSecond.setImageBitmap(secondSlide.bitmap)
            }
            // 그 외는 모두 취소
            // 처음 꺼를 클릭한 거니까 그냥 취소
            else if (adapter.isBindingContains(firstSlide)) {
                adapter.cancelBinding(firstSlide)
                showFirstBindingToPreview()
            }
            // 클릭한 놈의 다음 놈이 바인딩 되어있다면 그냥 패스
            else if (adapter.isBindingContains(secondSlide)) {
                return
            }
            else {
                showAlertDialog("예상치 못한 에러가 발생했습니다")
            }

        } else {
            // 만약 클릭한 이 슬라이드가 바인딩 된 놈일 때
            if (adapter.isBindingContains(slide)) {
                binding.imagePreview.visibility = View.GONE
                binding.layoutBinding.visibility = View.VISIBLE

                for ((first, second) in adapter.bindingPairs) {
                    if (first == slide || second == slide) {
                        binding.imageBindingPreviewFirst.setImageBitmap(first.bitmap)
                        binding.imageBindingPreviewSecond.setImageBitmap(second.bitmap)
                        break
                    }
                }
            }
            // 아닐 때
            else {
                binding.imagePreview.visibility = View.VISIBLE
                binding.layoutBinding.visibility = View.GONE
                binding.imagePreview.setImageBitmap(slide.bitmap)
            }
        }
    }

    // 바인딩 초기화
    private fun initBinding() {
        if (viewModel.enableBinding.value == true) {
            binding.layoutBinding.visibility = View.VISIBLE
            binding.imagePreview.visibility = View.GONE
        }
        else {
            binding.layoutBinding.visibility = View.GONE
            binding.imagePreview.visibility = View.VISIBLE
        }
    }

    // 가장 위에 존재하는 바인딩을 프리뷰에 띄우기
    private fun showFirstBindingToPreview() {

        binding.imagePreview.visibility = View.GONE
        binding.layoutBinding.visibility = View.VISIBLE

        if (adapter.items.isEmpty()) return

        // 등록된 바인딩 페어가 없으면
        // 1, 2번 놈 바인딩 된 샘플 이미지 보이기
        if (adapter.bindingPairs.isEmpty()) {
            binding.imageBindingPreviewFirst.setImageResource(android.R.color.transparent)
            binding.imageBindingPreviewSecond.setImageResource(android.R.color.transparent)

            binding.imageBindingPreviewFirst.setImageBitmap(adapter.items[0].bitmap)
            binding.imageBindingPreviewSecond.setImageBitmap(adapter.items[1].bitmap)
        }
        else {
            val pair = adapter.bindingPairs.minBy { adapter.getIndexOf(it.first) + adapter.getIndexOf(it.second) }
            binding.imageBindingPreviewFirst.setImageBitmap(pair.first.bitmap)
            binding.imageBindingPreviewSecond.setImageBitmap(pair.second.bitmap)
        }

    }


    // 이미지 크기 초기화
    private fun initImagesScale() {
        if (viewModel.isInstarSize.value == true) {
            setInstarSize()
        } else {
            setOriginalImage()
        }
    }

    // 이미지 크기 조절
    private fun scaleImages() {
        if (viewModel.isInstarSize.value == true) {
            setOriginalImage()
            viewModel.isInstarSize.value = false
        } else {
            setInstarSize()
            viewModel.isInstarSize.value = true
        }
    }

    // 이미지들을 화면 꽉차게 키우기
    private fun setOriginalImage() {
        adapter.setOriginalImage()
        binding.layoutPreviewBackground.setBackgroundResource(android.R.color.transparent)
        binding.layoutBinding.setBackgroundResource(android.R.color.transparent)
    }

    // 이미지들을 인스타 사이즈로 줄이기
    private fun setInstarSize() {
        adapter.setImagesInstarSize()
        binding.layoutPreviewBackground.setBackgroundResource(R.color.white)
        binding.layoutBinding.setBackgroundResource(R.color.white)
    }

    // uri들을 slide(bitmap 이미지)로 가공
    private suspend fun processIntoSlides(uriList: List<Pair<Uri, String>>): MutableList<Slide> {
        val slides = ArrayList<Slide>()

        // pdf를 여러장의 Slide(bitmap)들로 변환
        // 이미지이면 그냥 비트맵으로 변환
        coroutineScope {
            launch(Dispatchers.IO) {
                for ((uri, extension) in uriList) {
                    if (extension == "image") {
                        val slide = Slide(convertImageUriToBitmap(uri))
                        slides.add(slide)
                    } else if (extension == "pdf") {
                        val returnedSlides = convertPdfUriToBitmaps(uri).map { Slide(it) }
                        slides.addAll(returnedSlides)
                    }
                }
            }
        }

        return slides
    }

    // 퍼미션 런처
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            handleUri(viewModel.uriWithExtension.value!!)
        }
        else {
            showMessageDialog(
                "권한요청",
                "파일을 열기 위해서는\n사진 및 미디어 액세스 권한이 필요합니다",
                onDismiss = {
                    findNavController().popBackStack()
                }
            )
        }
    }

    // image의 uri를 가지고 bitmap으로 변환
    private suspend fun convertImageUriToBitmap(imageUri: Uri): Bitmap {
        var bitmap: Bitmap? = null

        coroutineScope {
                launch(exceptionHandler) {
                    withContext(Dispatchers.IO) {
                        bitmap = imageToBitmap(imageUri)
                        bitmap = getResized(bitmap!!)
                    }
                }
        }

        return bitmap
            ?: throw Exception("Converting Image File to Image Failed")
    }

    // pdf의 uri를 가지고 bitmap으로 변환
    private suspend fun convertPdfUriToBitmaps(pdfUri: Uri): MutableList<Bitmap> {
        var bitmaps: MutableList<Bitmap>? = null

        coroutineScope {
                launch(exceptionHandler) {
                    withContext(Dispatchers.IO) {
                        val realPath = getFilePathFromURI(this@SlideFragment, pdfUri)
                        // pdf 파일의 path가 null인 경우 빈 리스트를 반환
                        val pdfFile = File(realPath!!)
                        bitmaps = pdfToBitmaps(pdfFile)
                    }
                }
        }
        return bitmaps
            ?: throw Exception("Converting PDF to Image Failed")
    }

    // 뒤로가기
    override fun onBackPressed() {
        // 기존 파일 열었을때
        // 수정된게 없으면
        // 확인 다이얼로그 안 띄워도 됨
        viewModel.run {
            if (!resultSlideWithExtension.value.isNullOrEmpty()
                && isSlideChanged.value == false
            ) {
                slides.value = null
                findNavController().popBackStack()
                return
            }

            showConfirmDialog(
                "저장되지 않았습니다",
                "저장하지 않고 뒤로가시겠습니까?",
                onOk = {
                    slides.value = null
                    findNavController().popBackStack()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // pdf 파일 열때마다 생기는 이상한 놈들 없애기
        requireContext().optimizeDocumentCache()
    }
}