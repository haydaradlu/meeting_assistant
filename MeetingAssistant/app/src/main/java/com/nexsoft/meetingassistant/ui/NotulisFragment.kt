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
import com.nexsoft.meetingassistant.adapters.NotulisAdapter
import com.nexsoft.meetingassistant.api.ApiClient
import com.nexsoft.meetingassistant.databinding.FragmentNotulisBinding
import com.nexsoft.meetingassistant.models.Notulis
import com.nexsoft.meetingassistant.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotulisFragment : Fragment() {

    private var _binding: FragmentNotulisBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotulisAdapter
    private var allData: List<Notulis> = emptyList()

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotulisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        adapter = NotulisAdapter(
            onEdit = { notulis -> showEditDialog(notulis) },
            onDelete = { notulis -> showDeleteConfirmation(notulis) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnTambah.setOnClickListener { showAddDialog() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterData(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadData()
    }

    private fun loadData() {
        ApiClient.apiService.getAllNotulis().enqueue(object : Callback<List<Notulis>> {
            override fun onResponse(call: Call<List<Notulis>>, response: Response<List<Notulis>>) {
                if (response.isSuccessful) {
                    allData = response.body() ?: emptyList()
                    adapter.updateData(allData)
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat data notulis", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Notulis>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterData(query: String) {
        val filtered = allData.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.username.contains(query, ignoreCase = true)
        }
        adapter.updateData(filtered)
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_notulis, null)
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

            val notulis = Notulis(username = username, password = password, name = nama)
            ApiClient.apiService.createNotulis(notulis).enqueue(object : Callback<Notulis> {
                override fun onResponse(call: Call<Notulis>, response: Response<Notulis>) {
                    btnTambah.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Notulis berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadData()
                    } else {
                        Toast.makeText(requireContext(), "Gagal menambahkan notulis", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Notulis>, t: Throwable) {
                    btnTambah.isEnabled = true
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.show()
    }

    private fun showEditDialog(notulis: Notulis) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_notulis, null)
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

        tvTitle.text = "Edit Notulis"
        btnTambah.text = "Simpan"
        etUsername.setText(notulis.username)
        etNama.setText(notulis.name)
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

            val updatedNotulis = Notulis(
                notulisId = notulis.notulisId,
                username = username,
                password = if (password.isNotEmpty()) password else null,
                name = nama
            )

            ApiClient.apiService.updateNotulis(notulis.notulisId!!, updatedNotulis).enqueue(object : Callback<Notulis> {
                override fun onResponse(call: Call<Notulis>, response: Response<Notulis>) {
                    btnTambah.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Notulis berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadData()
                    } else {
                        Toast.makeText(requireContext(), "Gagal memperbarui notulis", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Notulis>, t: Throwable) {
                    btnTambah.isEnabled = true
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(notulis: Notulis) {
        if (sessionManager.getRole() == com.nexsoft.meetingassistant.utils.Constants.ROLE_NOTULIS && sessionManager.getUserId() == notulis.notulisId) {
            Toast.makeText(requireContext(), "Anda tidak dapat menghapus akun Anda sendiri", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                ApiClient.apiService.deleteNotulis(notulis.notulisId!!).enqueue(object : Callback<Map<String, String>> {
                    override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Notulis berhasil dihapus", Toast.LENGTH_SHORT).show()
                            loadData()
                        } else {
                            val errorMsg = parseErrorDetail(response.errorBody()?.string())
                            Toast.makeText(requireContext(), errorMsg ?: "Gagal menghapus notulis", Toast.LENGTH_SHORT).show()
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
