package com.android.instagramportfolio.view.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.ItemResultSlideBinding
import com.android.instagramportfolio.model.ResultSlide

class ResultSlideAdapter(
    private val items: MutableList<ResultSlide>,
    private val clickListener: (ResultSlide, ItemResultSlideBinding) -> Unit,
    private val onLongClickListener: (ResultSlide, ItemResultSlideBinding) -> Unit,
    private val selectedItems: MutableLiveData<MutableList<ResultSlide>>,
    private val isEditMode: MutableLiveData<Boolean>,
): RecyclerView.Adapter<ResultSlideAdapter.ViewHolder>() {

    // 뷰홀더 관리
    private val viewHolders = mutableMapOf<ResultSlide, ViewHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ItemResultSlideBinding = DataBindingUtil.inflate(inflater, R.layout.item_result_slide, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        viewHolders[item] = holder
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun replaceItems(items: List<ResultSlide>) {
        this.items.clear()
        this.items.addAll(items.reversed())
        notifyDataSetChanged()
    }

    fun enableEditMode(enabled: Boolean) {
        val visibility =
            if (enabled) View.VISIBLE
            else View.GONE

        for ((_, holder) in viewHolders) {
            holder.binding.checkbox.visibility = visibility
            holder.binding.imageChecked.visibility = View.GONE
        }
    }

    inner class ViewHolder(val binding: ItemResultSlideBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ResultSlide) {
            // 썸네일 지정
            binding.imageThumbnail.setImageBitmap(item.thumbnail)

            // 편집모드
            if (isEditMode.value == true) {
                binding.checkbox.visibility = View.VISIBLE

                // 얘가 선택됐다면 체크 활성화
                if (item in selectedItems.value!!)
                    binding.imageChecked.visibility = View.VISIBLE
                else
                    binding.imageChecked.visibility = View.GONE
            }
            else {
                binding.checkbox.visibility = View.GONE
                binding.imageChecked.visibility = View.GONE
            }

            // 클릭
            binding.root.setOnClickListener {
                clickListener(item, binding)
            }
            
            // 롱 클릭
            binding.root.setOnLongClickListener {
                onLongClickListener(item, binding)
                true
            }
        }
    }

}