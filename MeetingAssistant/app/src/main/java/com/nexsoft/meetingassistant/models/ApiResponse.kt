package com.nexsoft.meetingassistant.models

data class ApiResponse<T>(
    val message: String? = null,
    val data: T? = null
)
