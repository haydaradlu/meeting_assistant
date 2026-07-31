package com.nexsoft.meetingassistant.models

import com.google.gson.annotations.SerializedName

data class PemimpinRapat(
    @SerializedName("pr_id") val prId: Int? = null,
    val username: String,
    val password: String? = null,
    val name: String
)
