package com.nexsoft.meetingassistant.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexsoft.meetingassistant.databinding.ItemLaporanRowBinding
import com.nexsoft.meetingassistant.models.Laporan
import com.nexsoft.meetingassistant.utils.toOnlyDate

class LaporanAdapter(
    private var laporanList: MutableList<Laporan> = mutableListOf(),
    private val userRole: String,
    private val onDelete: (Laporan) -> Unit,
    private val onDownload: (Laporan) -> Unit
) : RecyclerView.Adapter<LaporanAdapter.LaporanViewHolder>() {

    inner class LaporanViewHolder(val binding: ItemLaporanRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LaporanViewHolder {
        val binding = ItemLaporanRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LaporanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LaporanViewHolder, position: Int) {
        val laporan = laporanList[position]
        with(holder.binding) {
            tvNo.text = (position + 1).toString()
            tvLaporan.text = laporan.fileLaporan ?: "-"
            tvId.text = laporan.laporanId?.toString() ?: "-"
            tvTanggal.text = laporan.tanggalKirim.toOnlyDate()
            
            if (userRole == com.nexsoft.meetingassistant.utils.Constants.ROLE_NOTULIS) {
                btnDelete.visibility = android.view.View.GONE
            } else {
                btnDelete.visibility = android.view.View.VISIBLE
            }

            btnDelete.setOnClickListener { onDelete(laporan) }
            btnDownload.setOnClickListener { onDownload(laporan) }
        }
    }

    override fun getItemCount(): Int = laporanList.size

    fun updateData(newList: List<Laporan>) {
        laporanList.clear()
        laporanList.addAll(newList)
        notifyDataSetChanged()
    }
}
