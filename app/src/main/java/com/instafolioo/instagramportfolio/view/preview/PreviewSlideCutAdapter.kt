package com.instafolioo.instagramportfolio.view.preview

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.databinding.*
import com.instafolioo.instagramportfolio.model.PreviewSlide
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.INSTAR_SIZE
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.INSTAR_SIZE_BINDING
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.ORIGINAL
import com.instafolioo.instagramportfolio.model.PreviewSlide.Companion.ORIGINAL_BINDING

class PreviewSlideCutAdapter(
    private val items: MutableList<PreviewSlide>,
    private val cutPositions: MutableLiveData<MutableList<Int>>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val viewHolders = mutableMapOf<Int, CutViewHolder>()
    var itemWidth: Float = 0f

    // 얘가 있어야지 뷰 타입이 갈림
    override fun getItemViewType(position: Int): Int = when (items[position].viewType) {
        ORIGINAL -> ORIGINAL
        INSTAR_SIZE -> INSTAR_SIZE
        ORIGINAL_BINDING -> ORIGINAL_BINDING
        INSTAR_SIZE_BINDING -> INSTAR_SIZE_BINDING
        else -> ORIGINAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            ORIGINAL -> {
                val binding: ItemCutOriginalBinding =
                    DataBindingUtil.inflate(inflater, com.instafolioo.instagramportfolio.R.layout.item_cut_original, parent, false)
                OriginalViewHolder(binding)
            }
            INSTAR_SIZE -> {
                val binding: ItemCutInstaSizeBinding =
                    DataBindingUtil.inflate(inflater, com.instafolioo.instagramportfolio.R.layout.item_cut_insta_size, parent, false)
                InstarSizeViewHolder(binding)
            }
            ORIGINAL_BINDING -> {
                val binding: ItemCutOriginalBindingBinding =
                    DataBindingUtil.inflate(inflater, com.instafolioo.instagramportfolio.R.layout.item_cut_original_binding, parent, false)
                OriginalBindingViewHolder(binding)
            }
            INSTAR_SIZE_BINDING -> {
                val binding: ItemCutInstaSizeBindingBinding =
                    DataBindingUtil.inflate(inflater, com.instafolioo.instagramportfolio.R.layout.item_cut_insta_size_binding, parent, false)
                InstarSizeBindingViewHolder(binding)
            }
            else -> throw IllegalStateException(
                "오리지널, 인스타 사이즈, 오리지널 바인딩, 인스타 사이즈 바인딩 이외의 타입은 존재하지 않습니다."
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        viewHolders[position] = holder as CutViewHolder

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

    // 슬라이드 분할시키기
    fun cutSlide(position: Int) {
        if (position != items.size - 1) {
            val viewHolder = viewHolders[position]
            viewHolder?.cutSlide()
        }
    }

    // 분할된 슬라이드 이어붙이기
    fun connectSlide(position: Int) {
        if (position != items.size - 1) {
            val viewHolder = viewHolders[position]
            viewHolder?.connectSlide()
        }
    }

    fun replaceItems(items: List<PreviewSlide>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    // 분할 함수를 모든 뷰 홀더가 가지게 하기 위한 클래스
    open inner class CutViewHolder(val root: View): RecyclerView.ViewHolder(root) {

        fun cutSlide() {
            val image = root.findViewById<View>(R.id.image)
            val param = image.layoutParams as FrameLayout.LayoutParams
            param.marginEnd = (itemWidth / 2).toInt()
            image.layoutParams = param
        }

        fun connectSlide() {
            val image = root.findViewById<View>(R.id.image)
            val param = image.layoutParams as FrameLayout.LayoutParams
            param.marginEnd = 0
            image.layoutParams = param
        }
    }

    // 원본 뷰홀더
    inner class OriginalViewHolder(
        val binding: ItemCutOriginalBinding
    ): CutViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.image.setImageBitmap(item.bitmap)

            if (items.indexOf(item) in cutPositions.value!!) {
                cutSlide()
            }
            else {
                connectSlide()
            }
        }
    }

    // 인스타 사이즈 뷰홀더
    inner class InstarSizeViewHolder(
        val binding: ItemCutInstaSizeBinding
    ): CutViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.image.setImageBitmap(item.bitmap)

            if (items.indexOf(item) in cutPositions.value!!) {
                cutSlide()
            }
            else {
                connectSlide()
            }
        }
    }

    // 오리지널 바인딩 뷰홀더
    inner class OriginalBindingViewHolder(
        val binding: ItemCutOriginalBindingBinding
    ): CutViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.image.setImageBitmap(item.bitmap)

            if (items.indexOf(item) in cutPositions.value!!) {
                cutSlide()
            }
            else {
                connectSlide()
            }
        }
    }

    // 인스타 사이즈 바인딩 뷰홀더
    inner class InstarSizeBindingViewHolder(
        val binding: ItemCutInstaSizeBindingBinding
    ): CutViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.imageFirst.setImageBitmap(item.bitmap)
            binding.imageSecond.setImageBitmap(item.bitmapSecond)

            if (items.indexOf(item) in cutPositions.value!!) {
                cutSlide()
            }
            else {
                connectSlide()
            }
        }
    }
}