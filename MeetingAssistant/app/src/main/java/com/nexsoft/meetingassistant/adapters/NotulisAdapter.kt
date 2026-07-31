package com.nexsoft.meetingassistant.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexsoft.meetingassistant.databinding.ItemNotulisRowBinding
import com.nexsoft.meetingassistant.models.Notulis

class NotulisAdapter(
    private var notulisList: MutableList<Notulis> = mutableListOf(),
    private val onEdit: (Notulis) -> Unit,
    private val onDelete: (Notulis) -> Unit
) : RecyclerView.Adapter<NotulisAdapter.NotulisViewHolder>() {

    inner class NotulisViewHolder(val binding: ItemNotulisRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotulisViewHolder {
        val binding = ItemNotulisRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotulisViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotulisViewHolder, position: Int) {
        val notulis = notulisList[position]
        with(holder.binding) {
            tvId.text = notulis.notulisId?.toString() ?: "-"
            tvUsername.text = notulis.username
            tvPassword.text = "••••••"
            tvNama.text = notulis.name
            btnEdit.setOnClickListener { onEdit(notulis) }
            btnDelete.setOnClickListener { onDelete(notulis) }
        }
    }

    override fun getItemCount(): Int = notulisList.size

    fun updateData(newList: List<Notulis>) {
        notulisList.clear()
        notulisList.addAll(newList)
        notifyDataSetChanged()
    }
}
