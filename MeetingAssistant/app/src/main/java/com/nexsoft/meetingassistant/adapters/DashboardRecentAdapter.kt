package com.nexsoft.meetingassistant.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexsoft.meetingassistant.databinding.ItemDashboardRecentBinding

class DashboardRecentAdapter(
    private var items: List<Pair<String, String>> = emptyList()
) : RecyclerView.Adapter<DashboardRecentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDashboardRecentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDashboardRecentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvRecentLabel.text = item.first
        holder.binding.tvRecentSub.text = item.second
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Pair<String, String>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
