package com.nexsoft.meetingassistant.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexsoft.meetingassistant.R
import com.nexsoft.meetingassistant.adapters.AdminAdapter
import com.nexsoft.meetingassistant.api.ApiClient
import com.nexsoft.meetingassistant.databinding.FragmentAdminBinding
import com.nexsoft.meetingassistant.models.Admin
import com.nexsoft.meetingassistant.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AdminAdapter
    private lateinit var sessionManager: SessionManager
    private var allAdmins: List<Admin> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        adapter = AdminAdapter(
            onEdit = { admin -> showEditDialog(admin) },
            onDelete = { admin -> showDeleteConfirmation(admin) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnTambah.setOnClickListener { showAddDialog() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAdmins(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadAdmins()
    }

    private fun loadAdmins() {
        ApiClient.apiService.getAllAdmins().enqueue(object : Callback<List<Admin>> {
            override fun onResponse(call: Call<List<Admin>>, response: Response<List<Admin>>) {
                if (response.isSuccessful) {
                    allAdmins = response.body() ?: emptyList()
                    adapter.updateData(allAdmins)
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat data admin", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Admin>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterAdmins(query: String) {
        val filtered = allAdmins.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.username.contains(query, ignoreCase = true)
        }
        adapter.updateData(filtered)
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_admin, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etNama = dialogView.findViewById<EditText>(R.id.etNama)
        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatal)
        val btnTambah = dialogView.findViewById<Button>(R.id.btnTambah)

        btnBatal.setOnClickListener { dialog.dismiss() }

        btnTambah.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val nama = etNama.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || nama.isEmpty()) {
                Toast.makeText(requireContext(), "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnTambah.isEnabled = false

            val admin = Admin(username = username, password = password, name = nama)
            ApiClient.apiService.createAdmin(admin).enqueue(object : Callback<Admin> {
                override fun onResponse(call: Call<Admin>, response: Response<Admin>) {
                    btnTambah.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Admin berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadAdmins()
                    } else {
                        Toast.makeText(requireContext(), "Gagal menambahkan admin", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Admin>, t: Throwable) {
                    btnTambah.isEnabled = true
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.show()
    }

    private fun showEditDialog(admin: Admin) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_admin, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etNama = dialogView.findViewById<EditText>(R.id.etNama)
        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatal)
        val btnTambah = dialogView.findViewById<Button>(R.id.btnTambah)

        tvTitle.text = "Edit Admin"
        btnTambah.text = "Simpan"
        etUsername.setText(admin.username)
        etNama.setText(admin.name)
        etPassword.hint = "Kosongkan jika tidak diubah"

        btnBatal.setOnClickListener { dialog.dismiss() }

        btnTambah.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val nama = etNama.text.toString().trim()

            if (username.isEmpty() || nama.isEmpty()) {
                Toast.makeText(requireContext(), "Username dan Nama harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnTambah.isEnabled = false

            val updatedAdmin = Admin(
                adminId = admin.adminId,
                username = username,
                password = if (password.isNotEmpty()) password else null,
                name = nama
            )

            ApiClient.apiService.updateAdmin(admin.adminId!!, updatedAdmin).enqueue(object : Callback<Admin> {
                override fun onResponse(call: Call<Admin>, response: Response<Admin>) {
                    btnTambah.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Admin berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadAdmins()
                    } else {
                        Toast.makeText(requireContext(), "Gagal memperbarui admin", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Admin>, t: Throwable) {
                    btnTambah.isEnabled = true
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(admin: Admin) {
        if (sessionManager.getRole() == com.nexsoft.meetingassistant.utils.Constants.ROLE_ADMIN && sessionManager.getUserId() == admin.adminId) {
            Toast.makeText(requireContext(), "Anda tidak dapat menghapus akun Anda sendiri", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                ApiClient.apiService.deleteAdmin(admin.adminId!!).enqueue(object : Callback<Map<String, String>> {
                    override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Admin berhasil dihapus", Toast.LENGTH_SHORT).show()
                            loadAdmins()
                        } else {
                            val errorMsg = parseErrorDetail(response.errorBody()?.string())
                            Toast.makeText(requireContext(), errorMsg ?: "Gagal menghapus admin", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                        Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun parseErrorDetail(jsonString: String?): String? {
        if (jsonString.isNullOrEmpty()) return null
        return try {
            org.json.JSONObject(jsonString).optString("detail", null)
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
