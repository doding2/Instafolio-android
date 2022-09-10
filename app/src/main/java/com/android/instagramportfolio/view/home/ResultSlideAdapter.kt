package com.android.instagramportfolio.view.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.ItemResultSlideBinding
import com.android.instagramportfolio.model.ResultSlide

class ResultSlideAdapter(
    private val items: MutableList<ResultSlide>,
    private val clickListener: (ResultSlide) -> Unit
): RecyclerView.Adapter<ResultSlideAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ItemResultSlideBinding = DataBindingUtil.inflate(inflater, R.layout.item_result_slide, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun replaceItems(items: List<ResultSlide>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemResultSlideBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ResultSlide) {
            // 썸네일 지정
            binding.imageThumbnail.setImageBitmap(item.thumbnail)

            // 체크박스 비활성화
            binding.checkbox.visibility = View.GONE

            binding.root.setOnClickListener {
                clickListener(item)
            }
        }
    }

}