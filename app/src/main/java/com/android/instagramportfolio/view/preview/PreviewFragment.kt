package com.android.instagramportfolio.view.preview

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.PagerSnapHelper
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.FragmentPreviewBinding
import com.android.instagramportfolio.extension.*
import com.android.instagramportfolio.model.PreviewSlide
import com.android.instagramportfolio.model.PreviewSlide.Companion.INSTAR_SIZE
import com.android.instagramportfolio.model.PreviewSlide.Companion.INSTAR_SIZE_BINDING
import com.android.instagramportfolio.model.PreviewSlide.Companion.ORIGINAL
import com.android.instagramportfolio.model.PreviewSlide.Companion.ORIGINAL_BINDING
import com.android.instagramportfolio.model.ResultSlide
import com.android.instagramportfolio.view.common.MainActivity
import com.android.instagramportfolio.view.home.HomeViewModel
import com.android.instagramportfolio.view.home.HomeViewModelFactory
import com.android.instagramportfolio.view.slide.SlideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PreviewFragment : Fragment(), MainActivity.OnBackPressedListener {

    companion object {
        const val TAG = "PreviewFragment"
    }

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var previewViewModel: PreviewViewModel
    private lateinit var slideViewModel: SlideViewModel
    private lateinit var homeViewModel: HomeViewModel

    private lateinit var adapter: PreviewSlideAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_preview, container, false)
        previewViewModel = ViewModelProvider(this)[PreviewViewModel::class.java]
        slideViewModel = ViewModelProvider(requireActivity())[SlideViewModel::class.java]
        val homeFactory = HomeViewModelFactory(requireActivity())
        homeViewModel = ViewModelProvider(requireActivity(), homeFactory)[HomeViewModel::class.java]

        // 뷰를 status bar와 navigation bar의 위치에서 떨어진 원래 위치로 복구(회전 방향에 따라 달라짐)
        when (requireActivity().display?.rotation) {
            // 폰이 왼쪽으로 누움
            Surface.ROTATION_90 -> {
                binding.root.setPadding(0, getStatusBarHeight(), getNaviBarHeight(), 0)
            }
            // 폰이 오른쪽으로 누움
            Surface.ROTATION_270 -> {
                binding.root.setPadding(getNaviBarHeight(), getStatusBarHeight(), 0, 0)
            }
            // 그 외는 그냥 정방향으으로 처리함
            else -> {
                binding.root.setPadding(0, getStatusBarHeight(), 0, getNaviBarHeight())
            }
        }

        // status bar, navigation bar가 밝은 색이 아니라는 것을 알림
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightNavigationBars = false

        // 리사이클러뷰에 어답터 추가
        adapter = PreviewSlideAdapter(previewViewModel.previewSlides.value!!, slideViewModel.isInstarSize.value!!)
        binding.recyclerView.adapter = adapter

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


        Log.d(TAG, getTimeStamp())

        return binding.root
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
                    binding.layoutLoading.root.visibility = View.VISIBLE
                    saveSlidesAsImage(format)
                }
                // pdf로 저장
                "pdf" -> {
                    binding.layoutLoading.root.visibility = View.VISIBLE
                    saveSlidesAsPdf()
                }
                else -> throw IllegalStateException("존재하지 않는 선택지 입니다")
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


    // 슬라이드를 프리뷰 슬라이드로 변환
    private fun processSlidesIntoPreviewSlides() {
        binding.layoutLoading.root.visibility = View.VISIBLE

        // 이미 해놓은거 있으면 패스
        if (adapter.itemCount > 0) {
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
                adapter.replaceItems(previewSlideList)
                // 로딩 끄기
                binding.layoutLoading.root.visibility = View.GONE
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
            val resultSlide = ResultSlide(0, format, bitmaps.size, thumbnail)
            val id = homeViewModel.addResultSlide(resultSlide)
            withContext(Dispatchers.Main) {
                homeViewModel.savingId.value = id
            }

            // 슬라이드를 내부저장소에 이미지로 저장
            val inInnerStorage = async {
                saveBitmapsAsImage(requireContext(), bitmaps, "slides", "id_${id}", format)
            }
            // 슬라이드를 외부저장소에 이미지로 저장
            val inExternalStorage = async {
                saveBitmapsAsImageInExternalStorage(
                    bitmaps,
                    "포트폴리오 ${getTimeStamp()}",
                    format
                )
            }

            // 저장 기다리기
            inInnerStorage.await()
            inExternalStorage.await()

            // 홈 화면으로 돌아가기
            withContext(Dispatchers.Main) {
                homeViewModel.savingId.value = null
                showAlertDialog("저장 완료",
                    onDismiss = {
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
            val resultSlide = ResultSlide(0, "pdf", bitmaps.size, thumbnail)
            val id = homeViewModel.addResultSlide(resultSlide)
            withContext(Dispatchers.Main) {
                homeViewModel.savingId.value = id
            }


            // 슬라이드를 내부저장소에 pdf로 저장
            val inInnerStorage = async {
                saveBitmapsAsPdf(requireContext(), bitmaps, "slides", "id_$id", "0")
            }
            // 슬라이드를 외부저장소에 pdf로 저장
            val inExternalStorage = async {
                saveBitmapsAsPdfInExternalStorage(
                    bitmaps,
                    name="포트폴리오 ${getTimeStamp()}"
                )
            }

            // 저장 기다리기
            inInnerStorage.await()
            inExternalStorage.await()

            // 홈 화면으로 돌아가기
            withContext(Dispatchers.Main) {
                previewViewModel.previewSlides.value = null
                homeViewModel.savingId.value = null
                findNavController().navigate(R.id.action_previewFragment_to_homeFragment)
            }
        }
    }

    // 뒤로가기
    override fun onBackPressed() {
        // 저장중
        if (homeViewModel.savingId.value != null) {
            showConfirmDialog(
                "아직 저장중입니다.",
                "저장을 중지하시겠습니까?",
                onOk = {
                    previewViewModel.previewSlides.value = null
                    homeViewModel.deleteResultSlide(homeViewModel.savingId.value!!)
                    homeViewModel.savingId.value = null
                    findNavController().popBackStack()
                }
            )
        }
        // 저장중이 아님
        else {
            previewViewModel.previewSlides.value = null
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (homeViewModel.savingId.value != null) {
            homeViewModel.deleteResultSlide(homeViewModel.savingId.value!!)
            homeViewModel.savingId.value = null
        }
    }

}