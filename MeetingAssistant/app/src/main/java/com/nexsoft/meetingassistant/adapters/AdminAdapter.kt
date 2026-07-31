package com.nexsoft.meetingassistant.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexsoft.meetingassistant.databinding.ItemAdminRowBinding
import com.nexsoft.meetingassistant.models.Admin

class AdminAdapter(
    private var adminList: MutableList<Admin> = mutableListOf(),
    private val onEdit: (Admin) -> Unit,
    private val onDelete: (Admin) -> Unit
) : RecyclerView.Adapter<AdminAdapter.AdminViewHolder>() {

    inner class AdminViewHolder(val binding: ItemAdminRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val binding = ItemAdminRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AdminViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val admin = adminList[position]
        with(holder.binding) {
            tvId.text = admin.adminId?.toString() ?: "-"
            tvUsername.text = admin.username
            tvPassword.text = "••••••"
            tvNama.text = admin.name
            btnEdit.setOnClickListener { onEdit(admin) }
            btnDelete.setOnClickListener { onDelete(admin) }
        }
    }

    override fun getItemCount(): Int = adminList.size

    fun updateData(newList: List<Admin>) {
        adminList.clear()
        adminList.addAll(newList)
        notifyDataSetChanged()
    }
}
