package com.nexsoft.meetingassistant.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREF_NAME, Context.MODE_PRIVATE
    )

    fun saveAuthData(token: String, role: String, userId: Int, userName: String) {
        prefs.edit().apply {
            putString(Constants.KEY_TOKEN, token)
            putString(Constants.KEY_ROLE, role)
            putInt(Constants.KEY_USER_ID, userId)
            putString(Constants.KEY_USER_NAME, userName)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(Constants.KEY_TOKEN, null)

    fun getRole(): String? = prefs.getString(Constants.KEY_ROLE, null)

    fun getUserId(): Int = prefs.getInt(Constants.KEY_USER_ID, -1)

    fun getUserName(): String? = prefs.getString(Constants.KEY_USER_NAME, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
