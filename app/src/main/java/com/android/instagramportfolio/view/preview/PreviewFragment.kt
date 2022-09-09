package com.android.instagramportfolio.view.preview

import android.os.Bundle
import android.view.*
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
import com.android.instagramportfolio.view.slide.SlideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var previewViewModel: PreviewViewModel
    private lateinit var slideViewModel: SlideViewModel

    private lateinit var adapter: PreviewSlideAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_preview, container, false)
        previewViewModel = ViewModelProvider(this)[PreviewViewModel::class.java]
        slideViewModel = ViewModelProvider(requireActivity())[SlideViewModel::class.java]

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
            showSelectFormatDialog { format ->
                when(format) {
                    "png", "jpg", "pdf" -> {
                        binding.layoutLoading.root.visibility = View.VISIBLE
                        saveSlides(format)
                    }
                    else -> throw IllegalStateException("존재하지 않는 선택지 입니다")
                }
            }
        }




        return binding.root
    }

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
    private fun saveSlides(format: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            previewViewModel.previewSlides.value?.forEachIndexed { index, previewSlide ->
                when (previewSlide.viewType) {
                    ORIGINAL -> {
                        saveSlideAsOriginal(requireContext(), previewSlide,
                            "slides", "slide list 0", "$index", format)
                    }
                    INSTAR_SIZE -> {

                    }
                    ORIGINAL_BINDING -> {
                        saveSlideAsOriginalBinding(requireContext(), previewSlide,
                        "slides", "slide list 1", "$index", format)
                    }
                    INSTAR_SIZE_BINDING -> {

                    }
                    else -> {

                    }
                }
            }

            // 홈 화면으로 돌아가기
            withContext(Dispatchers.Main) {
                findNavController().navigate(R.id.action_previewFragment_to_homeFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}