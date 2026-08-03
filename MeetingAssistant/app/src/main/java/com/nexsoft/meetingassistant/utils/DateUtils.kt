package com.nexsoft.meetingassistant.utils

object DateUtils {
    fun formatOnlyDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty() || dateStr == "-") return "-"
        return dateStr.substringBefore("T").substringBefore(" ")
    }
}

fun String?.toOnlyDate(): String {
    if (this.isNullOrEmpty() || this == "-") return "-"
    return this.substringBefore("T").substringBefore(" ")
}
