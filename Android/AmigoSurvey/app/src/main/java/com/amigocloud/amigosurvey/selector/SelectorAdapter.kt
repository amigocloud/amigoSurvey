package com.amigocloud.amigosurvey.selector

import android.arch.paging.PagedListAdapter
import android.support.v7.recyclerview.extensions.DiffCallback
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.amigocloud.amigosurvey.databinding.ItemSelectorBinding
import com.amigocloud.amigosurvey.util.layoutInflater

data class SelectorItem(val type: Type, val id: Long, val name: String, val imageUrl: String?) {
    enum class Type { PROJECT, DATASET }
}

class SelectorAdapter(private val onClick: (SelectorItem) -> Unit)
    : PagedListAdapter<SelectorItem, SelectorAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffCallback<SelectorItem>() {
            override fun areItemsTheSame(oldItem: SelectorItem, newItem: SelectorItem) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: SelectorItem, newItem: SelectorItem) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemSelectorBinding.inflate(parent.context.layoutInflater, parent, false))

    override fun onBindViewHolder(holder: SelectorAdapter.ViewHolder, position: Int) =
            holder.bind(getItem(position), onClick)

    class ViewHolder(private val binding: ItemSelectorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SelectorItem?, onClick: (SelectorItem) -> Unit) {
            if (item != null) {
                itemView.setOnClickListener { if (item.id > 0) onClick(item) }
                binding.item = item
                binding.executePendingBindings()
            }
        }
    }
}
