package com.nexsoft.meetingassistant.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.nexsoft.meetingassistant.api.ApiClient
import com.nexsoft.meetingassistant.databinding.FragmentHasilDetailBinding
import com.nexsoft.meetingassistant.models.HasilTranskripsi
import com.nexsoft.meetingassistant.models.Laporan
import com.nexsoft.meetingassistant.utils.Constants
import com.nexsoft.meetingassistant.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HasilTranskripsiDetailFragment : Fragment() {

    private var _binding: FragmentHasilDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private var hasilId: Int = 0
    private var currentHasil: HasilTranskripsi? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHasilDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        hasilId = arguments?.getInt("hasil_id", 0) ?: 0

        val role = sessionManager.getRole() ?: ""

        // Setup role-based UI
        setupRoleBasedUI(role)

        // Setup status spinner
        val statusItems = listOf("pending", "valid", "tidak_valid")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusItems)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = spinnerAdapter

        // Setup button listeners
        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSimpan.setOnClickListener {
            saveValidation()
        }

        binding.btnHapus.setOnClickListener {
            deleteHasil()
        }

        binding.btnKirim.setOnClickListener {
            kirimLaporan()
        }

        // Load data
        loadHasilDetail()
    }

    private fun setupRoleBasedUI(role: String) {
        when (role) {
            Constants.ROLE_ADMIN, Constants.ROLE_PEMIMPIN_RAPAT -> {
                binding.spinnerStatus.visibility = View.VISIBLE
                binding.tvStatus.visibility = View.GONE
                binding.btnHapus.visibility = View.VISIBLE
                binding.btnSimpan.visibility = View.VISIBLE
                binding.btnKirim.visibility = View.GONE
            }
            Constants.ROLE_NOTULIS -> {
                binding.spinnerStatus.visibility = View.GONE
                binding.tvStatus.visibility = View.VISIBLE
                binding.btnHapus.visibility = View.GONE
                binding.btnSimpan.visibility = View.GONE
                binding.btnKirim.visibility = View.VISIBLE
            }
        }
    }

    private fun loadHasilDetail() {
        ApiClient.apiService.getHasilById(hasilId).enqueue(object : Callback<HasilTranskripsi> {
            override fun onResponse(call: Call<HasilTranskripsi>, response: Response<HasilTranskripsi>) {
                if (response.isSuccessful) {
                    val hasil = response.body()
                    if (hasil != null) {
                        currentHasil = hasil
                        displayHasilDetail(hasil)
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat detail hasil transkripsi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<HasilTranskripsi>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayHasilDetail(hasil: HasilTranskripsi) {
        binding.tvNamaRekaman.text = hasil.namaRekaman ?: "-"
        binding.tvTanggal.text = hasil.tanggal ?: "-"
        binding.tvId.text = hasil.hasilId?.toString() ?: "-"
        binding.tvNotulis.text = hasil.notulisName ?: "-"

        val textTranskripsi = if (!hasil.hasilTranskripsi.isNullOrBlank()) hasil.hasilTranskripsi else "Sedang memproses audio..."
        binding.tvHasilTranskripsi.text = textTranskripsi

        val textRangkuman = if (!hasil.hasilRangkuman.isNullOrBlank()) hasil.hasilRangkuman else "Rangkuman sedang diproses..."
        binding.tvHasilRangkuman.text = textRangkuman

        // Set status
        val status = hasil.statusValidasi ?: "pending"
        binding.tvStatus.text = status

        // Set spinner position
        val statusItems = listOf("pending", "valid", "tidak_valid")
        val statusIndex = statusItems.indexOf(status)
        if (statusIndex >= 0) {
            binding.spinnerStatus.setSelection(statusIndex)
        }

        // Enable btnKirim only if status is valid
        if (status == "valid") {
            binding.btnKirim.isEnabled = true
            binding.btnKirim.alpha = 1.0f
        } else {
            binding.btnKirim.isEnabled = false
            binding.btnKirim.alpha = 0.5f
        }
    }

    private fun saveValidation() {
        val selectedStatus = binding.spinnerStatus.selectedItem?.toString() ?: "pending"
        val updatedHasil = HasilTranskripsi(
            hasilId = hasilId,
            statusValidasi = selectedStatus
        )

        ApiClient.apiService.validateHasil(hasilId, updatedHasil).enqueue(object : Callback<HasilTranskripsi> {
            override fun onResponse(call: Call<HasilTranskripsi>, response: Response<HasilTranskripsi>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Status berhasil diperbarui", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Gagal memperbarui status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<HasilTranskripsi>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteHasil() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                ApiClient.apiService.deleteHasil(hasilId).enqueue(object : Callback<Map<String, String>> {
                    override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Hasil transkripsi berhasil dihapus", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        } else {
                            Toast.makeText(requireContext(), "Gagal menghapus hasil transkripsi", Toast.LENGTH_SHORT).show()
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

    private fun kirimLaporan() {
        val hasil = currentHasil ?: return
        
        if (hasil.statusValidasi != "valid") {
            Toast.makeText(requireContext(), "Hanya hasil transkripsi dengan status 'valid' yang dapat dikirim", Toast.LENGTH_SHORT).show()
            return
        }
        
        val laporan = Laporan(
            hasilId = hasil.hasilId
        )

        ApiClient.apiService.createLaporan(laporan).enqueue(object : Callback<Laporan> {
            override fun onResponse(call: Call<Laporan>, response: Response<Laporan>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Laporan berhasil dikirim", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Gagal mengirim laporan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Laporan>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
