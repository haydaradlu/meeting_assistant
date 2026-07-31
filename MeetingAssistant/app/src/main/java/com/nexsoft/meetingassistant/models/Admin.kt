package com.nexsoft.meetingassistant.models

import com.google.gson.annotations.SerializedName

data class Admin(
    @SerializedName("admin_id") val adminId: Int? = null,
    val username: String,
    val password: String? = null,
    val name: String
)
