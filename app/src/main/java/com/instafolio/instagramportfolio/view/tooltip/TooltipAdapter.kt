package com.instafolio.instagramportfolio.view.tooltip

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.instafolio.instagramportfolio.R
import com.instafolio.instagramportfolio.databinding.ItemTooltipBinding

class TooltipAdapter(
    private val items: MutableList<String>
): RecyclerView.Adapter<TooltipAdapter.MainViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ItemTooltipBinding = DataBindingUtil.inflate(inflater, R.layout.item_tooltip, parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holderSelector: MainViewHolder, position: Int) {
        val item = items[position]
        holderSelector.bind(item, position)
    }

    override fun getItemCount(): Int = items.size

    inner class MainViewHolder(val binding: ItemTooltipBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: String, position: Int) {
            when (position) {
                0 -> binding.imageTooltip.setImageResource(R.drawable.tooltip_1)
                1 -> binding.imageTooltip.setImageResource(R.drawable.tooltip_2)
                2 -> binding.imageTooltip.setImageResource(R.drawable.tooltip_3)
                3 -> binding.imageTooltip.setImageResource(R.drawable.tooltip_4)
                4 -> binding.imageTooltip.setImageResource(R.drawable.tooltip_5)
                else -> binding.imageTooltip.setImageResource(R.drawable.tooltip_1)
            }
        }
    }
}