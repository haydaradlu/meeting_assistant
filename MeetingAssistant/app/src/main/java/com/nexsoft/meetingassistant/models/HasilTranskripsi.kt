package com.nexsoft.meetingassistant.models

import com.google.gson.annotations.SerializedName

data class HasilTranskripsi(
    @SerializedName("hasil_id") val hasilId: Int? = null,
    @SerializedName("rec_id") val recId: Int? = null,
    @SerializedName("pr_id") val prId: Int? = null,
    @SerializedName("notulis_id") val notulisId: Int? = null,
    @SerializedName("hasil_transkripsi") val hasilTranskripsi: String? = null,
    @SerializedName("hasil_rangkuman") val hasilRangkuman: String? = null,
    @SerializedName("summary_percentage") val summaryPercentage: Double? = null,
    val tanggal: String? = null,
    @SerializedName("status_validasi") val statusValidasi: String? = null,
    @SerializedName("nama_rekaman") val namaRekaman: String? = null,
    @SerializedName("notulis_name") val notulisName: String? = null
)

