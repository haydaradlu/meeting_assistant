package com.nexsoft.meetingassistant.models

import com.google.gson.annotations.SerializedName

data class RekamanRapat(
    @SerializedName("rec_id") val recId: Int? = null,
    @SerializedName("notulis_id") val notulisId: Int? = null,
    @SerializedName("notulis_name") val notulisName: String? = null,
    @SerializedName("file_audio") val fileAudio: String? = null,
    val tanggal: String? = null,
    @SerializedName("nama_rekaman") val namaRekaman: String? = null
)
