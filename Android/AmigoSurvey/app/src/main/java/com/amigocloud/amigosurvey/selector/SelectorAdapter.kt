package com.amigocloud.amigosurvey.selector

import android.arch.paging.PagedListAdapter
import android.support.v7.recyclerview.extensions.DiffCallback
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.amigocloud.amigosurvey.databinding.ItemSelectorBinding

data class SelectorItem(val type: Type, val id: Long, val name: String, val imageUrl: String?) {
    enum class Type { PROJECT, DATASET, PLACEHOLDER }
}

class SelectorAdapter(private val onClick: (SelectorItem) -> Unit)
    : PagedListAdapter<SelectorItem, SelectorAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffCallback<SelectorItem>() {
            override fun areItemsTheSame(oldItem: SelectorItem, newItem: SelectorItem) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: SelectorItem, newItem: SelectorItem) = oldItem == newItem
        }

        val PLACEHOLDER_ITEM = SelectorItem(SelectorItem.Type.PLACEHOLDER, 0, "Loading...", null)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context).let {
            ViewHolder(ItemSelectorBinding.inflate(it, parent, false))
        }
    }

    override fun onBindViewHolder(holder: SelectorAdapter.ViewHolder, position: Int) {
        holder.bind(getItem(position) ?: PLACEHOLDER_ITEM, onClick)
    }

    class ViewHolder(private val binding: ItemSelectorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SelectorItem, onClick: (SelectorItem) -> Unit) {
            itemView.setOnClickListener { if (item.id > 0) onClick(item) }
            binding.item = item
            binding.executePendingBindings()
        }
    }
}
