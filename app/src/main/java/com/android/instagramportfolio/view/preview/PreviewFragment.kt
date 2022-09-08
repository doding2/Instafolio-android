package com.android.instagramportfolio.view.preview

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.FragmentPreviewBinding
import com.android.instagramportfolio.view.slide.SlideViewModel

class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var previewViewModel: PreviewViewModel
    private lateinit var slideViewModel: SlideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_preview, container, false)
        previewViewModel = ViewModelProvider(this)[PreviewViewModel::class.java]
        slideViewModel = ViewModelProvider(requireActivity())[SlideViewModel::class.java]





        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}