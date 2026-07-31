package com.nexsoft.meetingassistant.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexsoft.meetingassistant.R
import com.nexsoft.meetingassistant.adapters.HasilListAdapter
import com.nexsoft.meetingassistant.api.ApiClient
import com.nexsoft.meetingassistant.databinding.FragmentHasilListBinding
import com.nexsoft.meetingassistant.models.HasilTranskripsi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HasilTranskripsiListFragment : Fragment() {

    private var _binding: FragmentHasilListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HasilListAdapter
    private var allHasil: List<HasilTranskripsi> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHasilListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HasilListAdapter(
            onView = { hasil ->
                val fragment = HasilTranskripsiDetailFragment()
                val bundle = Bundle()
                bundle.putInt("hasil_id", hasil.hasilId ?: 0)
                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Setup filter spinner
        val filterItems = listOf("Semua", "pending", "valid", "tidak_valid", "Tanggal Terbaru", "Tanggal Terlama")
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

        loadHasil()
    }

    private fun loadHasil() {
        ApiClient.apiService.getAllHasil().enqueue(object : Callback<List<HasilTranskripsi>> {
            override fun onResponse(call: Call<List<HasilTranskripsi>>, response: Response<List<HasilTranskripsi>>) {
                if (response.isSuccessful) {
                    allHasil = response.body() ?: emptyList()
                    adapter.updateData(allHasil)
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat data hasil transkripsi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<HasilTranskripsi>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applyFilters() {
        val query = binding.etSearch.text.toString().trim()
        val statusFilter = binding.spinnerFilter.selectedItem?.toString() ?: "Semua"

        var filtered = allHasil

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                (it.namaRekaman ?: "").contains(query, ignoreCase = true) ||
                (it.hasilId?.toString() ?: "").contains(query, ignoreCase = true)
            }
        }

        if (statusFilter == "Tanggal Terbaru") {
            filtered = filtered.sortedByDescending { it.tanggal }
        } else if (statusFilter == "Tanggal Terlama") {
            filtered = filtered.sortedBy { it.tanggal }
        } else if (statusFilter != "Semua") {
            filtered = filtered.filter {
                it.statusValidasi == statusFilter
            }
        }

        adapter.updateData(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
