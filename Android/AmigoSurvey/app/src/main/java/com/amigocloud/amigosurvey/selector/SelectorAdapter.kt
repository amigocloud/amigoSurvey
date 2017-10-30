package com.amigocloud.amigosurvey.selector

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.amigocloud.amigosurvey.databinding.ItemSelectorBinding


data class SelectorItem(val id: Long, val name: String, val imageUrl: String)

class SelectorAdapter : RecyclerView.Adapter<SelectorAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var data = mutableListOf<SelectorItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context).let {
            ViewHolder(ItemSelectorBinding.inflate(it, parent, false))
        }
    }

    override fun onBindViewHolder(holder: SelectorAdapter.ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount() = data.size

    override fun getItemId(position: Int) = data[position].id

    fun setData(data: List<SelectorItem>) {
        DiffUtil.calculateDiff(DiffCallback(this.data, data)).let {
            this.data.clear()
            this.data.addAll(data)
            it.dispatchUpdatesTo(this)
        }
    }

    class DiffCallback(private val oldItems: List<SelectorItem>,
                       private val newItems: List<SelectorItem>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition].id == newItems[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition] == newItems[newItemPosition]

    }

    class ViewHolder(private val binding: ItemSelectorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SelectorItem) {
            binding.item = item
            binding.executePendingBindings()
        }
    }
}
