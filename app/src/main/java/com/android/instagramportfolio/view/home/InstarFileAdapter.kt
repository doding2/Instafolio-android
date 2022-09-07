package com.android.instagramportfolio.view.home

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.ItemInstarFileBinding
import com.android.instagramportfolio.model.InstarFile

class InstarFileAdapter(
    private val items: MutableList<InstarFile>,
    private val clickListener: (InstarFile) -> Unit
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

    fun replaceItems(items: List<InstarFile>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemInstarFileBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InstarFile) {
            // TODO 나중에 아이템 꾸며야됨

            binding.root.setOnClickListener {
                clickListener(item)
            }
        }
    }

}