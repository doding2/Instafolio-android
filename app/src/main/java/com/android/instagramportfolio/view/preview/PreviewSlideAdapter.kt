package com.android.instagramportfolio.view.preview

import android.content.res.Configuration
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.instagramportfolio.databinding.*
import com.android.instagramportfolio.model.PreviewSlide


class PreviewSlideAdapter(
    private val items: MutableList<PreviewSlide>,
    private val isInstarSize: Boolean,
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 얘가 있어야지 뷰 타입이 갈림
    override fun getItemViewType(position: Int): Int = when (items[position].viewType) {
        PreviewSlide.ORIGINAL -> PreviewSlide.ORIGINAL
        PreviewSlide.INSTAR_SIZE -> PreviewSlide.INSTAR_SIZE
        PreviewSlide.ORIGINAL_BINDING -> PreviewSlide.ORIGINAL_BINDING
        PreviewSlide.INSTAR_SIZE_BINDING -> PreviewSlide.INSTAR_SIZE_BINDING
        else -> PreviewSlide.ORIGINAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        Log.i("Adapter", "뷰 타입: ${viewType}")
        return when (viewType) {
            PreviewSlide.ORIGINAL -> {
                val binding: ItemPreviewSlideOriginalBinding =
                    DataBindingUtil.inflate(inflater, com.android.instagramportfolio.R.layout.item_preview_slide_original, parent, false)
                OriginalViewHolder(binding)
            }
            PreviewSlide.INSTAR_SIZE -> {
                val binding: ItemPreviewSlideInstarSizeBinding =
                    DataBindingUtil.inflate(inflater, com.android.instagramportfolio.R.layout.item_preview_slide_instar_size, parent, false)
                InstarSizeViewHolder(binding)
            }
            PreviewSlide.ORIGINAL_BINDING -> {
                val binding: ItemPreviewSlideOriginalBindingBinding =
                    DataBindingUtil.inflate(inflater, com.android.instagramportfolio.R.layout.item_preview_slide_original_binding, parent, false)
                OriginalBindingViewHolder(binding)
            }
            PreviewSlide.INSTAR_SIZE_BINDING -> {
                val binding: ItemPreviewSlideInstarSizeBindingBinding =
                    DataBindingUtil.inflate(inflater, com.android.instagramportfolio.R.layout.item_preview_slide_instar_size_binding, parent, false)
                InstarSizeBindingViewHolder(binding)
            }
            else -> throw IllegalStateException(
                "오리지널, 인스타 사이즈, 오리지널 바인딩, 인스타 사이즈 바인딩 이외의 타입은 존재하지 않습니다."
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is OriginalViewHolder -> holder.bind(item)
            is InstarSizeViewHolder -> holder.bind(item)
            is OriginalBindingViewHolder -> holder.bind(item)
            is InstarSizeBindingViewHolder -> holder.bind(item)
            else -> throw IllegalStateException(
                "오리지널, 인스타 사이즈, 오리지널 바인딩, 인스타 사이즈 바인딩 이외의 타입은 존재하지 않습니다."
            )
        }
    }

    override fun getItemCount(): Int = items.size

    fun replaceItems(items: List<PreviewSlide>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    // 원본 뷰홀더
    inner class OriginalViewHolder(
        val binding: ItemPreviewSlideOriginalBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.imagePreview.setImageBitmap(item.bitmap)
        }
    }

    // 인스타 사이즈 뷰홀더
    inner class InstarSizeViewHolder(
        val binding: ItemPreviewSlideInstarSizeBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.imagePreview.setImageBitmap(item.bitmap)

            val constraintParams = binding.layoutConstraint.layoutParams
            val previewParams = binding.imagePreview.layoutParams

            // 폰 누어있는 방향
            val displayMode: Int = binding.root.context.resources.configuration.orientation

            // 정방향
            if (displayMode == Configuration.ORIENTATION_PORTRAIT) {
                // width 길이에 정렬
                constraintParams.apply {
                    width = ConstraintLayout.LayoutParams.MATCH_PARENT
                    height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                }
                previewParams.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = 0
                }

                binding.layoutConstraint.layoutParams = constraintParams
                binding.imagePreview.layoutParams = previewParams
            }
            // 회전됨
            else {
                // height 길이에 정렬
                constraintParams.apply {
                    width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    height = ConstraintLayout.LayoutParams.MATCH_PARENT
                }
                previewParams.apply {
                    width = 0
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }

                binding.layoutConstraint.layoutParams = constraintParams
                binding.imagePreview.layoutParams = previewParams
            }
        }
    }

    // 오리지널 바인딩 뷰홀더
    inner class OriginalBindingViewHolder(
        val binding: ItemPreviewSlideOriginalBindingBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            // 오리지널 바인딩은
            // 프리뷰에 보여지는 예시와 실제 결과가 같게 하기 위해
            // 그냥 미리 합쳐버림
            binding.imagePreview.setImageBitmap(item.bitmap)

            // 밑의 코드는 item_preview_slide_original_binding2.xml에 대응함
//            // 이미지 넣기
//            binding.imagePreviewFirst.setImageBitmap(item.bitmap)
//            binding.imagePreviewSecond.setImageBitmap(item.bitmapSecond)
//
//            // 폰 누어있는 방향
//            val displayMode: Int = binding.root.context.resources.configuration.orientation
//
//            // 정방향
//            if (displayMode == Configuration.ORIENTATION_PORTRAIT) {
//                // 비트맵의 가로가 더 길면
//                // 얘가 오른쪽으로 쏠리게 보임
//                // 그거 보정
//                if (item.bitmap.height > item.bitmap.width) {
//                    binding.imagePreviewFirst.scaleType = ImageView.ScaleType.FIT_CENTER
//                } else {
//                    binding.imagePreviewFirst.scaleType = ImageView.ScaleType.FIT_END
//                }
//
//                // 얜 왼쪽으로 쏠려셔
//                // 그거 또 보정
//                if (item.bitmapSecond!!.height > item.bitmapSecond.width) {
//                    binding.imagePreviewSecond.scaleType = ImageView.ScaleType.FIT_CENTER
//                } else {
//                    binding.imagePreviewSecond.scaleType = ImageView.ScaleType.FIT_START
//                }
//            }
//            // 회전되어 있을 경우에는 보정을 그냥 전부 FIT_CENTER로 해줘야 됨
//            else {
//                binding.imagePreviewFirst.scaleType = ImageView.ScaleType.FIT_CENTER
//                binding.imagePreviewSecond.scaleType = ImageView.ScaleType.FIT_CENTER
//            }
        }
    }

    // 인스타 사이즈 바인딩 뷰홀더
    inner class InstarSizeBindingViewHolder(
        val binding: ItemPreviewSlideInstarSizeBindingBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.imagePreviewFirst.setImageBitmap(item.bitmap)
            binding.imagePreviewSecond.setImageBitmap(item.bitmapSecond)

            val constraintParams = binding.layoutConstraint.layoutParams
            val previewParams = binding.layoutPreview.layoutParams

            // 폰 누어있는 방향
            val displayMode: Int = binding.root.context.resources.configuration.orientation

            // 정방향
            if (displayMode == Configuration.ORIENTATION_PORTRAIT) {
                // width 길이에 정렬
                constraintParams.apply {
                    width = ConstraintLayout.LayoutParams.MATCH_PARENT
                    height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                }
                previewParams.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = 0
                }

                binding.layoutConstraint.layoutParams = constraintParams
                binding.layoutPreview.layoutParams = previewParams
            }
            // 회전됨
            else {
                // height 길이에 정렬
                constraintParams.apply {
                    width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    height = ConstraintLayout.LayoutParams.MATCH_PARENT
                }
                previewParams.apply {
                    width = 0
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }

                binding.layoutConstraint.layoutParams = constraintParams
                binding.layoutPreview.layoutParams = previewParams
            }
        }
    }
}