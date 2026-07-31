package com.nexsoft.meetingassistant.models

import com.google.gson.annotations.SerializedName

data class Notulis(
    @SerializedName("notulis_id") val notulisId: Int? = null,
    val username: String,
    val password: String? = null,
    val name: String
)
