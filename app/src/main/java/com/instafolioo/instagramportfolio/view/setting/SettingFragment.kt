package com.instafolioo.instagramportfolio.view.setting

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.instafolioo.instagramportfolio.BuildConfig
import com.instafolioo.instagramportfolio.databinding.FragmentSettingBinding
import com.instafolioo.instagramportfolio.view.common.MainActivity
import com.instafolioo.instagramportfolio.view.common.delegates.ActivityLayoutSpecifier
import com.instafolioo.instagramportfolio.view.common.delegates.ActivityLayoutSpecifierDelegate

/**
 * Created by doding2 on 2023/11/21.
 */
class SettingFragment :
    Fragment(),
    MainActivity.OnBackPressedListener,
    ActivityLayoutSpecifier by ActivityLayoutSpecifierDelegate()
{
    private lateinit var binding: FragmentSettingBinding
    private lateinit var onBackCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        binding.apply {
            setStatusBarColor(activity, root, Color.WHITE, true)
            setNavigationBarColor(activity, root, Color.WHITE, true)
            setOrientationActions(
                activity = activity,
                onPortrait = {
                    root.setPadding(0, getStatusBarHeight(), 0, getNavigationBarHeight())
                },
                onLeftLandscape = {
                    root.setPadding(0, getStatusBarHeight(), getNavigationBarHeight(), 0)
                },
                onRightLandscape = {
                    root.setPadding(getNavigationBarHeight(), getStatusBarHeight(), 0, 0)
                }
            )
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.run {
            llPrivacyPolicy.setOnClickListener { showPrivacyPolicy() }
            tvVersion.text = BuildConfig.VERSION_NAME
        }
    }

    private fun showPrivacyPolicy() {
        val url = "https://fifth-silver-0cc.notion.site/d937ad4241174f1bbb8833136433b8d1?pvs=4"
        val intent = CustomTabsIntent.Builder()
            .build()
        intent.launchUrl(requireContext(), Uri.parse(url))
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackCallback)
    }

    override fun onDetach() {
        super.onDetach()
        onBackCallback.remove()
    }

    override fun onBackPressed() {
        findNavController().popBackStack()
    }
}