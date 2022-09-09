package com.android.instagramportfolio.view.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.ItemInstarFileBinding
import com.android.instagramportfolio.model.SlideResult

class InstarFileAdapter(
    private val items: MutableList<SlideResult>,
    private val clickListener: (SlideResult) -> Unit
): RecyclerView.Adapter<InstarFileAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ItemInstarFileBinding = DataBindingUtil.inflate(inflater, R.layout.item_instar_file, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun replaceItems(items: List<SlideResult>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemInstarFileBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SlideResult) {
            // TODO 나중에 아이템 꾸며야됨

            binding.root.setOnClickListener {
                clickListener(item)
            }
        }
    }

}