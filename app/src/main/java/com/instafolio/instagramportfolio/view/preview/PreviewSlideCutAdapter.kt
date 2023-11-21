package com.instafolio.instagramportfolio.view.preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.instafolio.instagramportfolio.R
import com.instafolio.instagramportfolio.databinding.ItemCutInstaSizeBinding
import com.instafolio.instagramportfolio.databinding.ItemCutInstaSizeBindingBinding
import com.instafolio.instagramportfolio.databinding.ItemCutOriginalBinding
import com.instafolio.instagramportfolio.databinding.ItemCutOriginalBindingBinding
import com.instafolio.instagramportfolio.model.PreviewSlide
import com.instafolio.instagramportfolio.model.PreviewSlide.Companion.INSTAR_SIZE
import com.instafolio.instagramportfolio.model.PreviewSlide.Companion.INSTAR_SIZE_BINDING
import com.instafolio.instagramportfolio.model.PreviewSlide.Companion.ORIGINAL
import com.instafolio.instagramportfolio.model.PreviewSlide.Companion.ORIGINAL_BINDING

class PreviewSlideCutAdapter(
    private val items: MutableList<PreviewSlide>,
    private val cutPositions: MutableLiveData<MutableList<Int>>,
    private val onItemClick: (PreviewSlide, Int) -> Unit,
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
                    DataBindingUtil.inflate(inflater, R.layout.item_cut_original, parent, false)
                OriginalViewHolder(binding)
            }
            INSTAR_SIZE -> {
                val binding: ItemCutInstaSizeBinding =
                    DataBindingUtil.inflate(inflater, R.layout.item_cut_insta_size, parent, false)
                InstarSizeViewHolder(binding)
            }
            ORIGINAL_BINDING -> {
                val binding: ItemCutOriginalBindingBinding =
                    DataBindingUtil.inflate(inflater, R.layout.item_cut_original_binding, parent, false)
                OriginalBindingViewHolder(binding)
            }
            INSTAR_SIZE_BINDING -> {
                val binding: ItemCutInstaSizeBindingBinding =
                    DataBindingUtil.inflate(inflater, R.layout.item_cut_insta_size_binding, parent, false)
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

    fun getItemWidth() = viewHolders.values.firstOrNull()?.root?.getMarginedWidth()
        ?: 0

    private fun View.getMarginedWidth(): Int {
        val params = layoutParams as RecyclerView.LayoutParams
        val hMargin = params.marginStart + params.marginEnd
        return measuredWidth + hMargin
    }

    fun getItemViewAt(position: Int) = viewHolders[position]?.root
        ?: throw IllegalStateException(
            "오리지널, 인스타 사이즈, 오리지널 바인딩, 인스타 사이즈 바인딩 이외의 타입은 존재하지 않습니다."
        )

    fun replaceItems(items: List<PreviewSlide>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    fun cut(position: Int) {
        viewHolders[position]?.cut()
    }

    fun paste(position: Int) {
        viewHolders[position]?.paste()
    }

    open inner class CutViewHolder(val root: View): RecyclerView.ViewHolder(root) {
        val divider: View = root.findViewById(R.id.divider)

        fun cut() {
            divider.visibility = View.VISIBLE
        }

        fun paste() {
            divider.visibility = View.GONE
        }
    }

    // 원본 뷰홀더
    inner class OriginalViewHolder(
        val binding: ItemCutOriginalBinding
    ): CutViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.image.setImageBitmap(item.bitmap)
            binding.root.setOnClickListener { onItemClick(item, items.indexOf(item)) }

            if (items.indexOf(item) in cutPositions.value!!) {
                cut()
            }
            else {
                paste()
            }
        }
    }

    // 인스타 사이즈 뷰홀더
    inner class InstarSizeViewHolder(
        val binding: ItemCutInstaSizeBinding
    ): CutViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.image.setImageBitmap(item.bitmap)
            binding.root.setOnClickListener { onItemClick(item, items.indexOf(item)) }

            if (items.indexOf(item) in cutPositions.value!!) {
                cut()
            }
            else {
                paste()
            }
        }
    }

    // 오리지널 바인딩 뷰홀더
    inner class OriginalBindingViewHolder(
        val binding: ItemCutOriginalBindingBinding
    ): CutViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.image.setImageBitmap(item.bitmap)
            binding.root.setOnClickListener { onItemClick(item, items.indexOf(item)) }

            if (items.indexOf(item) in cutPositions.value!!) {
                cut()
            }
            else {
                paste()
            }
        }
    }

    // 인스타 사이즈 바인딩 뷰홀더
    inner class InstarSizeBindingViewHolder(
        val binding: ItemCutInstaSizeBindingBinding
    ): CutViewHolder(binding.root) {

        fun bind(item: PreviewSlide) {
            binding.imagePreviewFirst.setImageBitmap(item.bitmap)
            binding.imagePreviewSecond.setImageBitmap(item.bitmapSecond)
            binding.root.setOnClickListener { onItemClick(item, items.indexOf(item)) }

            if (items.indexOf(item) in cutPositions.value!!) {
                cut()
            }
            else {
                paste()
            }
        }
    }
}