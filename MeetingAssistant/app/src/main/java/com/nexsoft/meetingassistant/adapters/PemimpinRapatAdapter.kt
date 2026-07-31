package com.nexsoft.meetingassistant.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexsoft.meetingassistant.databinding.ItemPemimpinRapatRowBinding
import com.nexsoft.meetingassistant.models.PemimpinRapat

class PemimpinRapatAdapter(
    private var pemimpinRapatList: MutableList<PemimpinRapat> = mutableListOf(),
    private val onEdit: (PemimpinRapat) -> Unit,
    private val onDelete: (PemimpinRapat) -> Unit
) : RecyclerView.Adapter<PemimpinRapatAdapter.PemimpinRapatViewHolder>() {

    inner class PemimpinRapatViewHolder(val binding: ItemPemimpinRapatRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PemimpinRapatViewHolder {
        val binding = ItemPemimpinRapatRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PemimpinRapatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PemimpinRapatViewHolder, position: Int) {
        val pemimpinRapat = pemimpinRapatList[position]
        with(holder.binding) {
            tvId.text = pemimpinRapat.prId?.toString() ?: "-"
            tvUsername.text = pemimpinRapat.username
            tvPassword.text = "••••••"
            tvNama.text = pemimpinRapat.name
            btnEdit.setOnClickListener { onEdit(pemimpinRapat) }
            btnDelete.setOnClickListener { onDelete(pemimpinRapat) }
        }
    }

    override fun getItemCount(): Int = pemimpinRapatList.size

    fun updateData(newList: List<PemimpinRapat>) {
        pemimpinRapatList.clear()
        pemimpinRapatList.addAll(newList)
        notifyDataSetChanged()
    }
}
