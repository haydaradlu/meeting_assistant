package com.nexsoft.meetingassistant.models

import com.google.gson.annotations.SerializedName

data class Laporan(
    @SerializedName("laporan_id") val laporanId: Int? = null,
    @SerializedName("hasil_id") val hasilId: Int? = null,
    @SerializedName("pr_id") val prId: Int? = null,
    @SerializedName("admin_id") val adminId: Int? = null,
    @SerializedName("file_laporan") val fileLaporan: String? = null,
    @SerializedName("tanggal_kirim") val tanggalKirim: String? = null
)
