package com.nexsoft.meetingassistant.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexsoft.meetingassistant.databinding.ItemHasilRowBinding
import com.nexsoft.meetingassistant.models.HasilTranskripsi

class HasilListAdapter(
    private var hasilList: MutableList<HasilTranskripsi> = mutableListOf(),
    private val onView: (HasilTranskripsi) -> Unit
) : RecyclerView.Adapter<HasilListAdapter.HasilViewHolder>() {

    inner class HasilViewHolder(val binding: ItemHasilRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HasilViewHolder {
        val binding = ItemHasilRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HasilViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HasilViewHolder, position: Int) {
        val hasil = hasilList[position]
        with(holder.binding) {
            tvNo.text = (position + 1).toString()
            tvNamaRekaman.text = hasil.namaRekaman ?: "-"
            tvId.text = hasil.hasilId?.toString() ?: "-"
            tvTanggal.text = hasil.tanggal ?: "-"
            btnView.setOnClickListener { onView(hasil) }
        }
    }

    override fun getItemCount(): Int = hasilList.size

    fun updateData(newList: List<HasilTranskripsi>) {
        hasilList.clear()
        hasilList.addAll(newList)
        notifyDataSetChanged()
    }
}
