package com.instafolioo.instagramportfolio.view.instagram.log_in

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.instagram4j.instagram4j.IGClient
import com.github.instagram4j.instagram4j.requests.media.MediaConfigureSidecarRequest
import com.github.instagram4j.instagram4j.requests.upload.RuploadPhotoRequest
import com.github.instagram4j.instagram4j.utils.IGUtils
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.databinding.FragmentInstagramLogInBinding
import com.instafolioo.instagramportfolio.extension.*
import com.instafolioo.instagramportfolio.model.PreviewSlide
import com.instafolioo.instagramportfolio.model.ResultSlide
import com.instafolioo.instagramportfolio.view.home.HomeViewModel
import com.instafolioo.instagramportfolio.view.home.HomeViewModelFactory
import com.instafolioo.instagramportfolio.view.preview.PreviewViewModel
import com.instafolioo.instagramportfolio.view.slide.SlideViewModel
import kotlinx.coroutines.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.nio.file.Files

class InstagramLogInFragment : Fragment() {

    companion object {
        const val TAG = "InstagramLogInFragment"
    }

    private var _binding: FragmentInstagramLogInBinding? = null
    private val binding get() = _binding!!

    private lateinit var logInViewModel: InstagramLogInViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_instagram_log_in, container, false)
        logInViewModel = ViewModelProvider(this)[InstagramLogInViewModel::class.java]
        binding.viewModel = logInViewModel
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

        // status bar, navigation bar가 밝은 색이라는 것을 알림
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightStatusBars = true
        WindowInsetsControllerCompat(requireActivity().window, binding.root).isAppearanceLightNavigationBars = true

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // 로그인
        binding.buttonLogIn.setOnClickListener {
            binding.editTextNickname.clearFocus()
            binding.editTextPassword.clearFocus()
            imm.hideSoftInputFromWindow(binding.editTextPassword.windowToken, 0)
            logIn()
        }

        return binding.root
    }

    private val interceptor = HttpLoggingInterceptor {
        Log.d(TAG, it)
    }.setLevel(HttpLoggingInterceptor.Level.BODY)

    // 로그인 하기
    private fun logIn() {
        binding.layoutLoading.root.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching {
                val client = IGClient.builder()
                    .username(logInViewModel.nickname.value?.toString())
                    .password(logInViewModel.password.value?.toString())
                    .client(IGUtils.defaultHttpClientBuilder().addInterceptor(interceptor).build())
                    .login()

                serialize(client)
            }

            withContext(Dispatchers.Main) {
                // 로그인 성공
                result.onSuccess {
                    findNavController().navigate(R.id.action_instagramLogInFragment_to_instagramUploadFragment)
                    binding.layoutLoading.root.visibility = View.GONE
                }
                // 실패
                result.onFailure {
                    binding.layoutLoading.root.visibility = View.GONE
                    showMessageDialog("로그인에 실패했습니다.", "${result.exceptionOrNull()?.message}")
                }

            }
        }
    }

    // 로그인 정보 저장
    private fun serialize(client: IGClient) {
        val cw = ContextWrapper(context)
        var directory = cw.getDir("instagram", Context.MODE_PRIVATE)
        directory = File(directory, "log_in")
        directory.mkdirs()

        val clientFile = File(directory, "client.ser")
        val cookieFile = File(directory, "cookie.ser")

        client.serialize(clientFile, cookieFile)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}