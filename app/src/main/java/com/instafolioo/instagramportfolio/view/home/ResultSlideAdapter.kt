package com.instafolioo.instagramportfolio.view.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.databinding.ItemResultSlideBinding
import com.instafolioo.instagramportfolio.model.ResultSlide

class ResultSlideAdapter(
    private val items: MutableList<ResultSlide>,
    private val clickListener: (ResultSlide, ItemResultSlideBinding) -> Unit,
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

    // 아이템 삭제
    fun removeItem(resultSlide: ResultSlide) {
        val index = items.indexOf(resultSlide)
        viewHolders.remove(resultSlide)
        items.removeAt(index)
        selectedItems.value?.remove(resultSlide)
        notifyItemRemoved(index)
    }

    // 편집모드 UI 보이도록 활성화
    fun disableEditMode() {
        for ((_, holder) in viewHolders) {
            holder.binding.imageChecked.visibility = View.GONE
        }
    }

    inner class ViewHolder(val binding: ItemResultSlideBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ResultSlide) {
            // 썸네일 지정
            binding.imageThumbnail.setImageBitmap(item.thumbnail)

            // 편집모드
            if (isEditMode.value == true) {
                // 얘가 선택됐다면 체크 활성화
                if (item in selectedItems.value!!) {
                    binding.imageChecked.visibility = View.VISIBLE
                }
                else {
                    binding.imageChecked.visibility = View.GONE
                }
            }
            else {
                binding.imageChecked.visibility = View.GONE
            }

            // 클릭
            binding.root.setOnClickListener {
                clickListener(item, binding)
            }

        }
    }

}