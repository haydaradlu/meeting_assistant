package com.nexsoft.meetingassistant.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexsoft.meetingassistant.databinding.ItemRekamanRowBinding
import com.nexsoft.meetingassistant.models.RekamanRapat

class RekamanAdapter(
    private var rekamanList: MutableList<RekamanRapat> = mutableListOf(),
    private val userRole: String,
    private val onDelete: (RekamanRapat) -> Unit
) : RecyclerView.Adapter<RekamanAdapter.RekamanViewHolder>() {

    inner class RekamanViewHolder(val binding: ItemRekamanRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RekamanViewHolder {
        val binding = ItemRekamanRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RekamanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RekamanViewHolder, position: Int) {
        val rekaman = rekamanList[position]
        with(holder.binding) {
            tvNo.text = (position + 1).toString()
            tvId.text = rekaman.recId?.toString() ?: "-"
            tvNotulis.text = rekaman.notulisName ?: "-"
            tvRekaman.text = rekaman.namaRekaman ?: "-"
            tvTanggal.text = rekaman.tanggal ?: "-"

            // Hide delete button for notulis role
            if (userRole == "notulis") {
                layoutAksi.visibility = View.GONE
            } else {
                layoutAksi.visibility = View.VISIBLE
                btnDelete.setOnClickListener { onDelete(rekaman) }
            }
        }
    }

    override fun getItemCount(): Int = rekamanList.size

    fun updateData(newList: List<RekamanRapat>) {
        rekamanList.clear()
        rekamanList.addAll(newList)
        notifyDataSetChanged()
    }
}
