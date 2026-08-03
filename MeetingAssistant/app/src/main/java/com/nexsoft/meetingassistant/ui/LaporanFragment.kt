package com.nexsoft.meetingassistant.ui

import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexsoft.meetingassistant.adapters.LaporanAdapter
import com.nexsoft.meetingassistant.api.ApiClient
import com.nexsoft.meetingassistant.databinding.FragmentLaporanBinding
import com.nexsoft.meetingassistant.models.Laporan
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: LaporanAdapter
    private var allLaporan: List<Laporan> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = com.nexsoft.meetingassistant.utils.SessionManager(requireContext())
        val role = sessionManager.getRole() ?: ""

        adapter = LaporanAdapter(
            userRole = role,
            onDelete = { laporan -> showDeleteConfirmation(laporan) },
            onDownload = { laporan -> downloadLaporan(laporan) }
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

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadLaporan()
    }

    private fun loadLaporan() {
        ApiClient.apiService.getAllLaporan().enqueue(object : Callback<List<Laporan>> {
            override fun onResponse(call: Call<List<Laporan>>, response: Response<List<Laporan>>) {
                if (response.isSuccessful) {
                    allLaporan = response.body() ?: emptyList()
                    adapter.updateData(allLaporan)
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat data laporan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Laporan>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applyFilters() {
        val query = binding.etSearch.text.toString().trim()
        val filterOption = binding.spinnerFilter.selectedItem?.toString() ?: "Semua"

        var filtered = allLaporan.filter {
            (it.fileLaporan ?: "").contains(query, ignoreCase = true) ||
            (it.laporanId?.toString() ?: "").contains(query, ignoreCase = true)
        }

        filtered = when (filterOption) {
            "Tanggal Terbaru" -> filtered.sortedByDescending { it.tanggalKirim }
            "Tanggal Terlama" -> filtered.sortedBy { it.tanggalKirim }
            else -> filtered
        }

        adapter.updateData(filtered)
    }

    private fun showDeleteConfirmation(laporan: Laporan) {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                ApiClient.apiService.deleteLaporan(laporan.laporanId!!).enqueue(object : Callback<Map<String, String>> {
                    override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Laporan berhasil dihapus", Toast.LENGTH_SHORT).show()
                            loadLaporan()
                        } else {
                            Toast.makeText(requireContext(), "Gagal menghapus laporan", Toast.LENGTH_SHORT).show()
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

    private fun downloadLaporan(laporan: Laporan) {
        ApiClient.apiService.downloadLaporan(laporan.laporanId!!).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        try {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val rawName = laporan.fileLaporan ?: "laporan_${laporan.laporanId}.pdf"
                            val fileName = if (rawName.endsWith(".txt", true)) rawName.replace(".txt", ".pdf", true) else rawName
                            val file = File(downloadsDir, fileName)

                            val inputStream = body.byteStream()
                            val outputStream = FileOutputStream(file)
                            val buffer = ByteArray(4096)
                            var bytesRead: Int

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }

                            outputStream.flush()
                            outputStream.close()
                            inputStream.close()

                            Toast.makeText(requireContext(), "Laporan berhasil didownload ke: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                            showOpenFileDialog(file)
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Gagal menyimpan file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal mendownload laporan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showOpenFileDialog(file: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("Download Selesai")
            .setMessage("Laporan berhasil diunduh (${file.name}). Buka file sekarang?")
            .setPositiveButton("Buka") { _, _ ->
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    val mimeType = if (file.name.endsWith(".pdf", true)) "application/pdf" else "text/plain"
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e: android.content.ActivityNotFoundException) {
                    Toast.makeText(requireContext(), "Tidak ada aplikasi untuk membuka file PDF", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Gagal membuka file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
