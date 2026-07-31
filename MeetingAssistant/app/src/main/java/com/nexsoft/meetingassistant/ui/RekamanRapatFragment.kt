package com.nexsoft.meetingassistant.ui

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexsoft.meetingassistant.R
import com.nexsoft.meetingassistant.adapters.RekamanAdapter
import com.nexsoft.meetingassistant.api.ApiClient
import com.nexsoft.meetingassistant.databinding.FragmentRekamanRapatBinding
import com.nexsoft.meetingassistant.models.RekamanRapat
import com.nexsoft.meetingassistant.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class RekamanRapatFragment : Fragment() {

    private var _binding: FragmentRekamanRapatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RekamanAdapter
    private lateinit var sessionManager: SessionManager
    private var allRekaman: List<RekamanRapat> = emptyList()
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = getFileName(it)
            currentFileNameTextView?.text = selectedFileName
        }
    }

    private var currentFileNameTextView: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRekamanRapatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        val userRole = sessionManager.getRole() ?: ""

        adapter = RekamanAdapter(
            userRole = userRole,
            onDelete = { rekaman -> showDeleteConfirmation(rekaman) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Setup filter spinner
        val filterItems = listOf("Semua", "Tanggal Terbaru", "Tanggal Terlama")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterItems)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = spinnerAdapter

        binding.spinnerFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnTambah.setOnClickListener { showAddDialog() }

        loadRekaman()
    }

    private fun applyFilters() {
        val query = binding.etSearch.text.toString().trim()
        val filterOption = binding.spinnerFilter.selectedItem?.toString() ?: "Semua"

        var filtered = allRekaman.filter {
            (it.namaRekaman ?: "").contains(query, ignoreCase = true) ||
            (it.recId?.toString() ?: "").contains(query, ignoreCase = true)
        }

        filtered = when (filterOption) {
            "Tanggal Terbaru" -> filtered.sortedByDescending { it.tanggal }
            "Tanggal Terlama" -> filtered.sortedBy { it.tanggal }
            else -> filtered
        }

        adapter.updateData(filtered)
    }

    private fun loadRekaman() {
        ApiClient.apiService.getAllRekaman().enqueue(object : Callback<List<RekamanRapat>> {
            override fun onResponse(call: Call<List<RekamanRapat>>, response: Response<List<RekamanRapat>>) {
                if (response.isSuccessful) {
                    allRekaman = response.body() ?: emptyList()
                    adapter.updateData(allRekaman)
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat data rekaman", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<RekamanRapat>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddDialog() {
        selectedFileUri = null
        selectedFileName = ""

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_rekaman, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etNamaRekaman = dialogView.findViewById<EditText>(R.id.etNamaRekaman)
        val btnBrowse = dialogView.findViewById<Button>(R.id.btnBrowse)
        val tvFileName = dialogView.findViewById<TextView>(R.id.tvFileName)
        val etTanggal = dialogView.findViewById<EditText>(R.id.etTanggal)
        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatal)
        val btnTambah = dialogView.findViewById<Button>(R.id.btnTambah)

        currentFileNameTextView = tvFileName

        btnBrowse.setOnClickListener {
            filePickerLauncher.launch("audio/*")
        }

        etTanggal.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    etTanggal.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnBatal.setOnClickListener { dialog.dismiss() }

        btnTambah.setOnClickListener {
            val namaRekaman = etNamaRekaman.text.toString().trim()
            val tanggal = etTanggal.text.toString().trim()

            if (namaRekaman.isEmpty() || tanggal.isEmpty() || selectedFileUri == null) {
                Toast.makeText(requireContext(), "Semua field harus diisi dan file harus dipilih", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Nonaktifkan tombol untuk mencegah double-click
            btnTambah.isEnabled = false

            val namaRekamanBody = namaRekaman.toRequestBody("text/plain".toMediaTypeOrNull())
            val tanggalBody = tanggal.toRequestBody("text/plain".toMediaTypeOrNull())

            val inputStream = requireContext().contentResolver.openInputStream(selectedFileUri!!)
            val fileBytes = inputStream?.readBytes()
            if (fileBytes == null) {
                btnTambah.isEnabled = true
                return@setOnClickListener
            }
            inputStream.close()

            val fileBody = fileBytes.toRequestBody("audio/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", selectedFileName, fileBody)

            ApiClient.apiService.createRekaman(namaRekamanBody, filePart, tanggalBody)
                .enqueue(object : Callback<RekamanRapat> {
                    override fun onResponse(call: Call<RekamanRapat>, response: Response<RekamanRapat>) {
                        btnTambah.isEnabled = true
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Rekaman berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadRekaman()
                        } else {
                            Toast.makeText(requireContext(), "Gagal menambahkan rekaman", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<RekamanRapat>, t: Throwable) {
                        btnTambah.isEnabled = true
                        Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(rekaman: RekamanRapat) {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                ApiClient.apiService.deleteRekaman(rekaman.recId!!).enqueue(object : Callback<Map<String, String>> {
                    override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Rekaman berhasil dihapus", Toast.LENGTH_SHORT).show()
                            loadRekaman()
                        } else {
                            Toast.makeText(requireContext(), "Gagal menghapus rekaman", Toast.LENGTH_SHORT).show()
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

    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentFileNameTextView = null
        _binding = null
    }
}
