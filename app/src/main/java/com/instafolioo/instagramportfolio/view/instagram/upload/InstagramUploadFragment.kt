package com.instafolioo.instagramportfolio.view.instagram.upload

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.instagram4j.instagram4j.IGClient
import com.github.instagram4j.instagram4j.requests.media.MediaConfigureSidecarRequest
import com.github.instagram4j.instagram4j.requests.upload.RuploadPhotoRequest
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.databinding.FragmentInstagramUploadBinding
import com.instafolioo.instagramportfolio.extension.*
import com.instafolioo.instagramportfolio.model.PreviewSlide
import com.instafolioo.instagramportfolio.model.ResultSlide
import com.instafolioo.instagramportfolio.view.common.MainActivity
import com.instafolioo.instagramportfolio.view.home.HomeViewModel
import com.instafolioo.instagramportfolio.view.home.HomeViewModelFactory
import com.instafolioo.instagramportfolio.view.preview.PreviewViewModel
import com.instafolioo.instagramportfolio.view.slide.SlideViewModel
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files


class InstagramUploadFragment: Fragment(), MainActivity.OnBackPressedListener {

    companion object {
        const val TAG = "InstagramUploadFragment"
    }

    private var _binding: FragmentInstagramUploadBinding? = null
    private val binding get() = _binding!!

    private lateinit var uploadViewModel: InstagramUploadViewModel
    private lateinit var slideViewModel: SlideViewModel
    private lateinit var previewViewModel: PreviewViewModel
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_instagram_upload, container, false)
        slideViewModel = ViewModelProvider(requireActivity())[SlideViewModel::class.java]
        previewViewModel = ViewModelProvider(requireActivity())[PreviewViewModel::class.java]
        val homeFactory = HomeViewModelFactory(requireActivity())
        homeViewModel = ViewModelProvider(requireActivity(), homeFactory)[HomeViewModel::class.java]
        uploadViewModel = ViewModelProvider(this)[InstagramUploadViewModel::class.java]
        binding.viewModel = uploadViewModel
        binding.lifecycleOwner = this

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
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightStatusBars = true
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightNavigationBars = true

        val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        // 인스타그램에 업로드
        binding.buttonUpload.setOnClickListener {
            binding.editTextContent.clearFocus()
            imm.hideSoftInputFromWindow(binding.editTextContent.windowToken, 0)
            upload()
        }

        return binding.root
    }

    // 업로드 하기
    private fun upload() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.layoutLoading.root.visibility = View.VISIBLE
            
            // 클라이언트 불러오기
            val client = getClient()

            // 이미지 저장
            val files = savePreviewImages()

            val result = runCatching {
                // 1개 이상일때만 앨범으로서 전송
                if (files.size > 1) uploadAlbum(client, files)
                // 포토로 전송
                else if (files.size == 1) uploadPhoto(client, files.first())
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
                deleteInstagramImageCache()

                binding.layoutLoading.root.visibility = View.GONE
                showAlertDialog("업로드에 성공했습니다.",
                    onDismiss = {
                        previewViewModel.savingSlide.value = null
                        findNavController().navigate(R.id.action_instagramUploadFragment_to_homeFragment)
                    }
                )
            }

        }
    }

    // 내부저장소에서 클라이언트 가져오기
    private fun getClient(): IGClient {
        val result = runCatching {
            val cw = ContextWrapper(context)
            var directory = cw.getDir("instagram", Context.MODE_PRIVATE)
            directory = File(directory, "log_in")

            val clientFile = File(directory, "client.ser")
            val cookieFile = File(directory, "cookie.ser")

            val client = IGClient.deserialize(clientFile, cookieFile)

            return@runCatching client
        }

        // 실패
        if (result.isFailure) {
            showAlertDialog(
                "인스타그램에 로그인 되어있지 않습니다.",
                onDismiss = {
                    findNavController().navigate(R.id.action_instagramUploadFragment_to_previewFragment)
                })
            throw FileNotFoundException("There is no instagram client files.")
        }
        // 성공
        else {
            return result.getOrThrow()
        }
    }


    private suspend fun uploadAlbum(client: IGClient, files: List<File>) {
        coroutineScope {
            launch(Dispatchers.IO) {
                val metadata = arrayListOf<MediaConfigureSidecarRequest.SidecarChildrenMetadata>()

                for (file in files) {
                    // load image
                    val firstImgData = Files.readAllBytes(file.toPath())

                    // upload
                    val uploadFirstPhoto = RuploadPhotoRequest(
                        firstImgData,
                        "1",
                        System.currentTimeMillis().toString(),
                        true
                    )

                    val e = client.sendRequest(uploadFirstPhoto).join()
                    if (e.statusCode != 200) {
                        // 200이 아니면 이미지 업로드 안된거
                        continue
                    }

                    val firstId = e.upload_id
                    metadata.add(MediaConfigureSidecarRequest.SidecarChildrenMetadata(firstId))
                    delay(7)
                }

                val configReq = MediaConfigureSidecarRequest(
                    MediaConfigureSidecarRequest
                        .MediaConfigureSidecarPayload()
                        .caption(uploadViewModel.content.value?.toString())
                        .children_metadata(metadata)
//                        .location()
                )

                val response = client.sendRequest(configReq).join()
                Log.d(TAG, "result: ${response.status}")
            }
        }
    }

    private suspend fun uploadPhoto(client: IGClient, file: File) {
        coroutineScope {
            launch(Dispatchers.IO) {
                client.actions().timeline()
                    .uploadPhoto(file, uploadViewModel.content.value?.toString())
                    .thenAccept {
                        Log.d(TAG, "upload succeed: ${it.status}")
                    }
                    .join()
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
                        PreviewSlide.ORIGINAL ->
                            bitmaps.add(it.getAsOriginal().setInstagramRatio())
                        PreviewSlide.INSTAR_SIZE ->
                            bitmaps.add(it.getAsInstarSize())
                        PreviewSlide.ORIGINAL_BINDING ->
                            bitmaps.add(it.getAsOriginalBinding().setInstagramRatio())
                        PreviewSlide.INSTAR_SIZE_BINDING ->
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
        var directory = cw.getDir("instagram", Context.MODE_PRIVATE)
        directory = File(directory, "instagram_temp")
        directory.deleteRecursively()
    }

    override fun onBackPressed() {
        showConfirmDialog(
            "저장되지 않았습니다.",
            "정말 뒤로가시겠습니까?",
            onOk = {
                findNavController().navigate(R.id.action_instagramUploadFragment_to_previewFragment)
            }
        )
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