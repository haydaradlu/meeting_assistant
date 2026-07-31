package com.nexsoft.meetingassistant.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexsoft.meetingassistant.adapters.DashboardRecentAdapter
import com.nexsoft.meetingassistant.api.ApiClient
import com.nexsoft.meetingassistant.databinding.FragmentHomeBinding
import com.nexsoft.meetingassistant.models.HasilTranskripsi
import com.nexsoft.meetingassistant.models.Laporan
import com.nexsoft.meetingassistant.models.RekamanRapat
import com.nexsoft.meetingassistant.utils.Constants
import com.nexsoft.meetingassistant.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var recentAdapter: DashboardRecentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        val role = sessionManager.getRole() ?: ""
        val name = sessionManager.getUserName() ?: "User"

        // Setup welcome
        binding.tvWelcomeTitle.text = "Selamat Datang, $name!"
        binding.tvWelcomeSub.text = when (role) {
            Constants.ROLE_ADMIN -> "Kelola dan pantau seluruh data aplikasi"
            Constants.ROLE_PEMIMPIN_RAPAT -> "Pantau dan validasi hasil transkripsi rapat"
            Constants.ROLE_NOTULIS -> "Rekam dan kelola transkripsi rapat Anda"
            else -> "Ringkasan data hari ini"
        }

        // Setup recent adapter
        recentAdapter = DashboardRecentAdapter()
        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = recentAdapter
        binding.rvRecent.isNestedScrollingEnabled = false

        // Load data based on role
        when (role) {
            Constants.ROLE_ADMIN -> loadAdminDashboard()
            Constants.ROLE_PEMIMPIN_RAPAT -> loadPemimpinRapatDashboard()
            Constants.ROLE_NOTULIS -> loadNotulisDashboard()
        }
    }

    // ====================================================
    // ADMIN DASHBOARD
    // Statistik: Admin, Pemimpin Rapat, Notulis, Rekaman
    // Status Transkripsi: Pending, Valid, Tidak Valid
    // Aktivitas Terbaru: 5 laporan terbaru
    // ====================================================
    private fun loadAdminDashboard() {
        binding.sectionStatus.visibility = View.VISIBLE
        binding.tvRecentTitle.text = "Laporan Terbaru"

        // Card labels
        binding.tvStat1Label.text = "Total Admin"
        binding.tvStat2Label.text = "Pemimpin Rapat"
        binding.tvStat3Label.text = "Total Notulis"
        binding.tvStat4Label.text = "Rekaman Rapat"

        // Load Admin count
        ApiClient.apiService.getAllAdmins().enqueue(object : Callback<List<com.nexsoft.meetingassistant.models.Admin>> {
            override fun onResponse(call: Call<List<com.nexsoft.meetingassistant.models.Admin>>, response: Response<List<com.nexsoft.meetingassistant.models.Admin>>) {
                if (_binding == null) return
                binding.tvStat1Value.text = (response.body()?.size ?: 0).toString()
            }
            override fun onFailure(call: Call<List<com.nexsoft.meetingassistant.models.Admin>>, t: Throwable) {}
        })

        // Load Pemimpin Rapat count
        ApiClient.apiService.getAllPemimpinRapat().enqueue(object : Callback<List<com.nexsoft.meetingassistant.models.PemimpinRapat>> {
            override fun onResponse(call: Call<List<com.nexsoft.meetingassistant.models.PemimpinRapat>>, response: Response<List<com.nexsoft.meetingassistant.models.PemimpinRapat>>) {
                if (_binding == null) return
                binding.tvStat2Value.text = (response.body()?.size ?: 0).toString()
            }
            override fun onFailure(call: Call<List<com.nexsoft.meetingassistant.models.PemimpinRapat>>, t: Throwable) {}
        })

        // Load Notulis count
        ApiClient.apiService.getAllNotulis().enqueue(object : Callback<List<com.nexsoft.meetingassistant.models.Notulis>> {
            override fun onResponse(call: Call<List<com.nexsoft.meetingassistant.models.Notulis>>, response: Response<List<com.nexsoft.meetingassistant.models.Notulis>>) {
                if (_binding == null) return
                binding.tvStat3Value.text = (response.body()?.size ?: 0).toString()
            }
            override fun onFailure(call: Call<List<com.nexsoft.meetingassistant.models.Notulis>>, t: Throwable) {}
        })

        // Load Rekaman count
        ApiClient.apiService.getAllRekaman().enqueue(object : Callback<List<RekamanRapat>> {
            override fun onResponse(call: Call<List<RekamanRapat>>, response: Response<List<RekamanRapat>>) {
                if (_binding == null) return
                binding.tvStat4Value.text = (response.body()?.size ?: 0).toString()
            }
            override fun onFailure(call: Call<List<RekamanRapat>>, t: Throwable) {}
        })

        // Load Hasil Transkripsi for status breakdown
        ApiClient.apiService.getAllHasil().enqueue(object : Callback<List<HasilTranskripsi>> {
            override fun onResponse(call: Call<List<HasilTranskripsi>>, response: Response<List<HasilTranskripsi>>) {
                if (_binding == null) return
                val list = response.body() ?: emptyList()
                val pending = list.count { it.statusValidasi == "pending" }
                val valid = list.count { it.statusValidasi == "valid" }
                val tidakValid = list.count { it.statusValidasi == "tidak_valid" }
                binding.tvStatusPending.text = pending.toString()
                binding.tvStatusValid.text = valid.toString()
                binding.tvStatusTidakValid.text = tidakValid.toString()
            }
            override fun onFailure(call: Call<List<HasilTranskripsi>>, t: Throwable) {}
        })

        // Load recent laporan
        ApiClient.apiService.getAllLaporan().enqueue(object : Callback<List<Laporan>> {
            override fun onResponse(call: Call<List<Laporan>>, response: Response<List<Laporan>>) {
                if (_binding == null) return
                val list = response.body() ?: emptyList()
                val recent = list.takeLast(5).reversed()
                if (recent.isEmpty()) {
                    binding.tvRecentEmpty.visibility = View.VISIBLE
                } else {
                    binding.tvRecentEmpty.visibility = View.GONE
                    val items = recent.map {
                        Pair(
                            "Laporan #${it.laporanId}",
                            "File: ${it.fileLaporan ?: "-"}  •  ${it.tanggalKirim ?: "-"}"
                        )
                    }
                    recentAdapter.updateData(items)
                }
            }
            override fun onFailure(call: Call<List<Laporan>>, t: Throwable) {}
        })
    }

    // ====================================================
    // PEMIMPIN RAPAT DASHBOARD
    // Statistik: Total Transkripsi, Pending, Valid, Tidak Valid
    // Status Transkripsi: rincian
    // Aktivitas Terbaru: 5 transkripsi terbaru
    // ====================================================
    private fun loadPemimpinRapatDashboard() {
        binding.sectionStatus.visibility = View.VISIBLE
        binding.tvRecentTitle.text = "Transkripsi Terbaru"

        binding.tvStat1Label.text = "Total Transkripsi"
        binding.tvStat2Label.text = "Menunggu Validasi"
        binding.tvStat3Label.text = "Sudah Valid"
        binding.tvStat4Label.text = "Total Laporan"

        ApiClient.apiService.getAllHasil().enqueue(object : Callback<List<HasilTranskripsi>> {
            override fun onResponse(call: Call<List<HasilTranskripsi>>, response: Response<List<HasilTranskripsi>>) {
                if (_binding == null) return
                val list = response.body() ?: emptyList()
                val pending = list.count { it.statusValidasi == "pending" }
                val valid = list.count { it.statusValidasi == "valid" }
                val tidakValid = list.count { it.statusValidasi == "tidak_valid" }

                binding.tvStat1Value.text = list.size.toString()
                binding.tvStat2Value.text = pending.toString()
                binding.tvStat3Value.text = valid.toString()

                binding.tvStatusPending.text = pending.toString()
                binding.tvStatusValid.text = valid.toString()
                binding.tvStatusTidakValid.text = tidakValid.toString()

                // Recent 5
                val recent = list.takeLast(5).reversed()
                if (recent.isEmpty()) {
                    binding.tvRecentEmpty.visibility = View.VISIBLE
                } else {
                    binding.tvRecentEmpty.visibility = View.GONE
                    val items = recent.map {
                        Pair(
                            it.namaRekaman ?: "Rekaman #${it.recId}",
                            "Status: ${it.statusValidasi ?: "pending"}  •  ${it.tanggal ?: "-"}"
                        )
                    }
                    recentAdapter.updateData(items)
                }
            }
            override fun onFailure(call: Call<List<HasilTranskripsi>>, t: Throwable) {}
        })

        ApiClient.apiService.getAllLaporan().enqueue(object : Callback<List<Laporan>> {
            override fun onResponse(call: Call<List<Laporan>>, response: Response<List<Laporan>>) {
                if (_binding == null) return
                binding.tvStat4Value.text = (response.body()?.size ?: 0).toString()
            }
            override fun onFailure(call: Call<List<Laporan>>, t: Throwable) {}
        })
    }

    // ====================================================
    // NOTULIS DASHBOARD
    // Statistik: Rekaman, Transkripsi, Valid, Laporan Dikirim
    // Tidak ada section status (notulis tidak bisa validasi)
    // Aktivitas Terbaru: 5 rekaman terbaru
    // ====================================================
    private fun loadNotulisDashboard() {
        binding.sectionStatus.visibility = View.GONE
        binding.tvRecentTitle.text = "Rekaman Terbaru"

        binding.tvStat1Label.text = "Rekaman Saya"
        binding.tvStat2Label.text = "Transkripsi"
        binding.tvStat3Label.text = "Sudah Valid"
        binding.tvStat4Label.text = "Laporan Dikirim"

        ApiClient.apiService.getAllRekaman().enqueue(object : Callback<List<RekamanRapat>> {
            override fun onResponse(call: Call<List<RekamanRapat>>, response: Response<List<RekamanRapat>>) {
                if (_binding == null) return
                val list = response.body() ?: emptyList()
                binding.tvStat1Value.text = list.size.toString()

                // Recent 5 rekaman
                val recent = list.takeLast(5).reversed()
                if (recent.isEmpty()) {
                    binding.tvRecentEmpty.visibility = View.VISIBLE
                } else {
                    binding.tvRecentEmpty.visibility = View.GONE
                    val items = recent.map {
                        Pair(
                            it.namaRekaman ?: "Rekaman #${it.recId}",
                            "Tanggal: ${it.tanggal ?: "-"}"
                        )
                    }
                    recentAdapter.updateData(items)
                }
            }
            override fun onFailure(call: Call<List<RekamanRapat>>, t: Throwable) {}
        })

        ApiClient.apiService.getAllHasil().enqueue(object : Callback<List<HasilTranskripsi>> {
            override fun onResponse(call: Call<List<HasilTranskripsi>>, response: Response<List<HasilTranskripsi>>) {
                if (_binding == null) return
                val list = response.body() ?: emptyList()
                val valid = list.count { it.statusValidasi == "valid" }
                binding.tvStat2Value.text = list.size.toString()
                binding.tvStat3Value.text = valid.toString()
            }
            override fun onFailure(call: Call<List<HasilTranskripsi>>, t: Throwable) {}
        })

        ApiClient.apiService.getAllLaporan().enqueue(object : Callback<List<Laporan>> {
            override fun onResponse(call: Call<List<Laporan>>, response: Response<List<Laporan>>) {
                if (_binding == null) return
                binding.tvStat4Value.text = (response.body()?.size ?: 0).toString()
            }
            override fun onFailure(call: Call<List<Laporan>>, t: Throwable) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
