package com.nexsoft.meetingassistant.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val role: String,
    @SerializedName("user_id") val userId: Int,
    val name: String
)
